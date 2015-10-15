/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifndef TI_USE_AUTOLAYOUT

#import "LayoutConstraint.h"
#import "QuartzCore/QuartzCore.h"
#import "TiUtils.h"
#import "TiUIView.h"
#import "TiViewProxy.h"

/* BEGIN PSEUDOCODE

First try width.

Width is constant or percent: create a width value appropriate. Consult view if it's a valid width.
Width is auto: Consult view on preferred width. If so, use it. If not, act as if it's undefined.

Okay, see if we have a width. If so, look to see if we have x. If so, we're done for horizontal.

If width is valid:
	if x is constant or percent:
		create a valid x
	else if left and right are defined:
		Balance springily.
	else if left is defined
		x = left + width*anchorpoint
	else if right is defined
		x = superviewwidth - right - width*anchorpoint
	else (left and right are undefined)
		x = superviewwidth/2 - width*anchorpoint
else (width is invalid)
	(Same as before)

*/

CGSize minmaxSize(LayoutConstraint * constraint, CGSize size, CGSize parentSize)
{
    CGSize result = size;
    if (constraint->sizeRatio > 0) {
        if (TiDimensionIsUndefined(constraint->width)) {
            result.width = constraint->sizeRatio * result.height;
        } else if (TiDimensionIsUndefined(constraint->height)) {
            result.height = constraint->sizeRatio * result.width;
        }
    }
    result.width = MAX(TiDimensionCalculateValueDef(constraint->minimumWidth, parentSize.width, result.width),result.width);
    result.height = MAX(TiDimensionCalculateValueDef(constraint->minimumHeight, parentSize.height, result.height),result.height);
    result.width = MIN(TiDimensionCalculateValueDef(constraint->maximumWidth, parentSize.width, result.width),result.width);
    result.height = MIN(TiDimensionCalculateValueDef(constraint->maximumHeight, parentSize.height, result.height),result.height);
    return result;
}

CGSize SizeConstraintViewWithSizeAddingResizing(LayoutConstraint * constraint, NSObject<LayoutAutosizing> * autoSizer, CGSize referenceSize, UIViewAutoresizing * resultResizing)
{
    __block CGFloat width;
    __block CGFloat height;
    BOOL ignoreWPercent = NO;
    BOOL ignoreHPercent = NO;
    BOOL needsWidthAutoCompute = NO;
    BOOL needsHeightAutoCompute = NO;
    BOOL parentCanGrow = NO;
    CGSize parentSize = CGSizeZero;
    
    BOOL flexibleWidth = NO;
    BOOL flexibleHeight = NO;
    BOOL autoSizeComputed = NO;
    
    __block CGFloat offsetx = TiDimensionCalculateValue(constraint->left, referenceSize.width)
    + TiDimensionCalculateValue(constraint->right, referenceSize.width);
    
    __block CGFloat offsety = TiDimensionCalculateValue(constraint->top, referenceSize.height)
    + TiDimensionCalculateValue(constraint->bottom, referenceSize.height);
    
    
    __block BOOL followsFillWBehavior = TiDimensionIsAutoFill([autoSizer defaultAutoWidthBehavior:nil]);
    __block BOOL followsFillHBehavior = TiDimensionIsAutoFill([autoSizer defaultAutoHeightBehavior:nil]);
    
    __block CGFloat boundingWidth = referenceSize.width;
    __block CGFloat boundingHeight = referenceSize.height;
    
    CGSize (^completion)() = ^() {
        
        CGSize result = minmaxSize(constraint, CGSizeMake(width, height), CGSizeMake(boundingWidth, boundingHeight));
        //Should we always do this or only for auto
        if ([autoSizer respondsToSelector:@selector(verifySize:)])
        {
            result = [(id)autoSizer verifySize:result];
        }
        
        if ((resultResizing != NULL) && [autoSizer respondsToSelector:@selector(verifyAutoresizing:)])
        {
            *resultResizing = [autoSizer verifyAutoresizing:*resultResizing];
        }
        return result;
    };
    
    if(resultResizing != NULL)
    {
        *resultResizing &= ~(UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
    }
    
    
    if ([autoSizer isKindOfClass:[TiViewProxy class]]) {
        TiViewProxy* parent = [(TiViewProxy*)autoSizer viewParent];
        LayoutConstraint* parentConstraints = [parent layoutProperties];
        if (parent != nil && (!TiLayoutRuleIsAbsolute(parentConstraints->layoutStyle))) {
            //Sandbox with percent values is garbage
            //            ignorePercent = YES;
            ignoreWPercent = TiLayoutRuleIsHorizontal(parentConstraints->layoutStyle) || TiLayoutRuleIsVertical(parentConstraints->layoutStyle);
            ignoreHPercent = TiLayoutRuleIsVertical(parentConstraints->layoutStyle) || (ignoreWPercent && TiLayoutFlagsHasHorizontalWrap(parentConstraints));
            UIView *parentView = [parent parentViewForChild:(TiViewProxy*)autoSizer];
            parentSize = (parentView != nil) ? parentView.bounds.size : CGSizeZero;
            parentCanGrow = TiDimensionIsAutoSize(parentConstraints->height);
        } else if (!parent) {
            parentCanGrow = TiDimensionIsAutoSize(constraint->height);
        }
    }
    
    
    
    if (constraint->fullscreen == YES) {
        *resultResizing |= (UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
        width = boundingWidth ;
        height = boundingHeight;
        return completion();
    }
    
    TiDimension dimension = constraint->width;
    if (TiDimensionIsDip(dimension))
    {
        followsFillWBehavior = NO;
        width = roundf(TiDimensionCalculateValue(dimension, boundingWidth));
    }
    else if (TiDimensionIsPercent(dimension)) {
        followsFillWBehavior = NO;
        flexibleWidth = YES;
        if (ignoreWPercent) {
            boundingWidth = parentSize.width;
        }
        width = roundf(TiDimensionCalculateValue(dimension, boundingWidth));
    }
    else if (TiDimensionIsUndefined(dimension))
    {
        flexibleWidth = YES;
        if (!TiDimensionIsUndefined(constraint->left) && !TiDimensionIsUndefined(constraint->centerX) ) {
            followsFillWBehavior = NO;
            width = 2 * ( TiDimensionCalculateValue(constraint->centerX, boundingWidth) - TiDimensionCalculateValue(constraint->left, boundingWidth) );
        }
        //        else if (!TiDimensionIsUndefined(constraint->left) && !TiDimensionIsUndefined(constraint->right) ) {
        ////            followsFillWBehavior = YES;
        //            width = boundingWidth - offsetx;
        //        }
        else if (!TiDimensionIsUndefined(constraint->centerX) && !TiDimensionIsUndefined(constraint->right) ) {
            followsFillWBehavior = NO;
            width = 2 * ( boundingWidth - TiDimensionCalculateValue(constraint->right, boundingWidth) - TiDimensionCalculateValue(constraint->centerX, boundingWidth));
        }
        else if (followsFillWBehavior){
            width = boundingWidth - offsetx;
        } else {
            //This block takes care of auto,SIZE and FILL. If it is size ensure followsFillBehavior is set to false
            needsWidthAutoCompute = YES;
            followsFillWBehavior = NO;
            width = boundingWidth - offsetx;
        }
    }
    else if(TiDimensionIsAutoFill(dimension) || (TiDimensionIsAuto(dimension) && followsFillWBehavior)){
        flexibleWidth = YES;
        followsFillWBehavior = YES;
        width = boundingWidth - offsetx;
    }
    else if(constraint->sizeRatio > 0){
        followsFillWBehavior = NO;
        width = 0.0f;
    }
    else {
        //This block takes care of auto,SIZE and FILL. If it is size ensure followsFillBehavior is set to false
        needsWidthAutoCompute = YES;
        followsFillWBehavior = NO;
        width = boundingWidth - offsetx;
    }
    
    
    
    dimension = constraint->height;
    
    if (TiDimensionIsDip(dimension))
    {
        followsFillHBehavior = NO;
        height = roundf(TiDimensionCalculateValue(dimension, boundingHeight));
    }
    else if (TiDimensionIsPercent(dimension)) {
        followsFillHBehavior = NO;
        flexibleHeight = YES;
        if (ignoreHPercent) {
            boundingHeight = parentSize.height;
        }
        height = roundf(TiDimensionCalculateValue(dimension, boundingHeight));
    }
    else if (TiDimensionIsUndefined(dimension))
    {
        flexibleHeight = YES;
        if (!TiDimensionIsUndefined(constraint->top) && !TiDimensionIsUndefined(constraint->centerY) ) {
            followsFillHBehavior = NO;
            height = 2 * ( TiDimensionCalculateValue(constraint->centerY, boundingHeight) - TiDimensionCalculateValue(constraint->top, boundingHeight) );
        }
        //        else if (!TiDimensionIsUndefined(constraint->top) && !TiDimensionIsUndefined(constraint->bottom) ) {
        ////            followsFillHBehavior = YES;
        //            height = boundingHeight - offsety;
        //        }
        else if (!TiDimensionIsUndefined(constraint->centerY) && !TiDimensionIsUndefined(constraint->bottom) ) {
            followsFillHBehavior = NO;
            height = 2 * ( boundingHeight - TiDimensionCalculateValue(constraint->bottom, boundingHeight) - TiDimensionCalculateValue(constraint->centerY, boundingHeight));
        }
        else if (followsFillHBehavior){
            height = boundingHeight - offsety;
        } else {
            //This block takes care of auto,SIZE and FILL. If it is size ensure followsFillBehavior is set to false
            needsHeightAutoCompute = YES;
            followsFillHBehavior = NO;
            height = boundingHeight - offsety;
        }
    }
    else if(TiDimensionIsAutoFill(dimension) || (TiDimensionIsAuto(dimension) && followsFillHBehavior)){
        flexibleHeight = YES;
        followsFillHBehavior = YES;
        height = boundingHeight - offsety;
    }
    else if(constraint->sizeRatio > 0){
        followsFillHBehavior = NO;
        height = 0.0f;
    }
    else {
        //This block takes care of auto,SIZE and FILL. If it is size ensure followsFillBehavior is set to false
        needsHeightAutoCompute = YES;
        followsFillHBehavior = NO;
        height = boundingHeight - offsety;
    }
    
    
    CGSize autoSize;
    
    //Undefined falls to auto behavior
    if (!flexibleWidth && TiDimensionIsUndefined(constraint->width) || TiDimensionIsAuto(constraint->width) )
    {
        //Check if default auto behavior is fill
        if ([autoSizer respondsToSelector:@selector(defaultAutoWidthBehavior:)]) {
            flexibleWidth = TiDimensionIsAutoFill([autoSizer defaultAutoWidthBehavior:nil]);
            needsWidthAutoCompute = !flexibleWidth;
        }
    }
    
    if (needsWidthAutoCompute && [autoSizer respondsToSelector:@selector(autoSizeForSize:)]) {
        //If it comes here it has to follow size behavior
        if (autoSizeComputed == NO) {
            autoSize = [autoSizer autoSizeForSize:CGSizeMake(width, height)];
            autoSizeComputed = YES;
        }
        CGFloat desiredWidth = autoSize.width;
        width = width < desiredWidth?width:desiredWidth;
    }
    else if(flexibleWidth && resultResizing != NULL){
        *resultResizing |= UIViewAutoresizingFlexibleWidth;
    }
    
    //Undefined falls to auto behavior
    if (!flexibleHeight && TiDimensionIsUndefined(constraint->height) || TiDimensionIsAuto(constraint->height) )
    {
        //Check if default auto behavior is fill
        if ([autoSizer respondsToSelector:@selector(defaultAutoHeightBehavior:)]) {
            flexibleHeight = TiDimensionIsAutoFill([autoSizer defaultAutoHeightBehavior:nil]);
            needsHeightAutoCompute = !flexibleHeight;
        }
    }
    
    if (needsHeightAutoCompute && [autoSizer respondsToSelector:@selector(autoSizeForSize:)]) {
        //If it comes here it has to follow size behavior
        if (autoSizeComputed == NO) {
            autoSize = [autoSizer autoSizeForSize:CGSizeMake(width, height)];
            autoSizeComputed = YES;
        }
        height = parentCanGrow?autoSize.height:(height < autoSize.height?height:autoSize.height);
    }
    else if(flexibleHeight && resultResizing != NULL){
        *resultResizing |= UIViewAutoresizingFlexibleHeight;
    }
    
    return completion();
}



CGPoint PositionConstraintGivenSizeBoundsAddingResizing(LayoutConstraint * constraint, LayoutConstraint * parentConstraint, TiViewProxy* viewProxy, CGSize viewSize, CGPoint anchorPoint, CGSize referenceSize, CGSize sandboxSize, UIViewAutoresizing * resultResizing)
{
    BOOL flexibleSize = *resultResizing & UIViewAutoresizingFlexibleWidth;
    
    *resultResizing &= ~(UIViewAutoresizingFlexibleLeftMargin | UIViewAutoresizingFlexibleRightMargin | UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleBottomMargin);
    
    CGFloat centerX = 0.0f;
    
    BOOL horizontal = parentConstraint && TiLayoutRuleIsHorizontal(parentConstraint->layoutStyle);
//    BOOL horizontalWrap = horizontal && TiLayoutFlagsHasHorizontalWrap(parentConstraint);
    BOOL horizontalNoWrap = horizontal && !TiLayoutFlagsHasHorizontalWrap(parentConstraint);
    BOOL vertical = parentConstraint && TiLayoutRuleIsVertical(parentConstraint->layoutStyle);
    
    BOOL ignoreMargins = NO;
    BOOL isSizeUndefined = TiDimensionIsUndefined(constraint->width);
    
    CGSize parentSize = sandboxSize;
    if (!horizontal) parentSize = referenceSize;
    
    if (constraint->fullscreen == YES) {
		return CGPointMake(parentSize.width/2.0, parentSize.height/2.0);
    }
    
    CGFloat frameLeft = 0.0;
    if (!flexibleSize) {
        if (TiDimensionIsUndefined(constraint->width)) {
            ignoreMargins = TiDimensionDidCalculateValue(constraint->centerX, referenceSize.width, &centerX);
        }
        else if(!TiDimensionDidCalculateValue(constraint->left, referenceSize.width, &frameLeft))
        {
            ignoreMargins = TiDimensionDidCalculateValue(constraint->centerX, referenceSize.width, &centerX);
        }
    }
	
    if (!ignoreMargins)
    {
        //Either the view has flexible width or pins were not defined for positioning
        int marginSuggestions=0;
        
        if(TiDimensionDidCalculateValue(constraint->left, referenceSize.width, &frameLeft))
        {
            marginSuggestions++;
        }
        else if (!flexibleSize)
        {
            *resultResizing |= UIViewAutoresizingFlexibleLeftMargin;
        }
        
        if (isSizeUndefined || (marginSuggestions == 0) || flexibleSize) {
            
            CGFloat frameRight;
            if(TiDimensionDidCalculateValue(constraint->right, referenceSize.width, &frameRight))
            {
                marginSuggestions++;
                frameLeft += parentSize.width - viewSize.width - frameRight;
            }
            else if (!horizontal && !flexibleSize)
            {
                *resultResizing |= UIViewAutoresizingFlexibleRightMargin;
            }
        }
        if (marginSuggestions < 1)
        {
            centerX = parentSize.width/2.0 + viewSize.width*(anchorPoint.x-0.5);
        }
        else
        {
            centerX = frameLeft/marginSuggestions + viewSize.width*anchorPoint.x;
        }
    }
	
    flexibleSize = *resultResizing & UIViewAutoresizingFlexibleHeight;
    CGFloat centerY = 0.0f;
    
    
    isSizeUndefined = TiDimensionIsUndefined(constraint->height);
    ignoreMargins = NO;
    CGFloat frameTop = 0.0;
    parentSize = sandboxSize;
    if ((!horizontal && !vertical) || horizontalNoWrap) {
        parentSize = referenceSize;
    }
    if(!flexibleSize) {
        if (TiDimensionIsUndefined(constraint->height)) {
            ignoreMargins = TiDimensionDidCalculateValue(constraint->centerY, referenceSize.height, &centerY);
        }
        else if(!TiDimensionDidCalculateValue(constraint->top, referenceSize.height, &frameTop))
        {
            ignoreMargins = TiDimensionDidCalculateValue(constraint->centerY, referenceSize.height, &centerY);;
        }
    }
 
	
    if (!ignoreMargins)
    {
        //Either the view has flexible height or pins were not defined for positioning
        int marginSuggestions=0;
        
        if((!horizontal || TiDimensionIsUndefined(constraint->bottom) || flexibleSize) && TiDimensionDidCalculateValue(constraint->top, referenceSize.height, &frameTop))
        {
            marginSuggestions++;
        }
        else if (!vertical && !flexibleSize)
        {
            *resultResizing |= UIViewAutoresizingFlexibleTopMargin;
        }
        if (isSizeUndefined || (marginSuggestions == 0) || flexibleSize) {
            CGFloat frameBottom;
            if(TiDimensionDidCalculateValue(constraint->bottom, referenceSize.height, &frameBottom) && TiDimensionIsUndefined(constraint->top))
            {
                marginSuggestions++;
                frameTop += parentSize.height - viewSize.height - frameBottom;
            }
            else if (!vertical && !flexibleSize)
            {
                *resultResizing |= UIViewAutoresizingFlexibleBottomMargin;
            }
        }
        if (marginSuggestions < 1)
        {
            centerY = parentSize.height/2.0 + viewSize.height*(anchorPoint.y-0.5);
        }
        else
        {
            centerY = frameTop/marginSuggestions + viewSize.height*anchorPoint.y;
        }
    }
    
    return CGPointMake(centerX, centerY);
}


void ApplyConstraintToViewWithBounds(LayoutConstraint * constraint, LayoutConstraint * parentConstraint, TiUIView * subView, CGRect viewBounds)
{
	if(constraint == NULL)
	{
		DebugLog(@"[ERROR] No constraints available for view %@.", subView);
		return;
	}

	UIViewAutoresizing resultMask = UIViewAutoresizingNone;
	CGRect resultBounds;
	resultBounds.origin = CGPointZero;
	resultBounds.size = SizeConstraintViewWithSizeAddingResizing(constraint,(TiViewProxy *)[subView proxy], viewBounds.size, &resultMask);
	
	CGPoint resultCenter = PositionConstraintGivenSizeBoundsAddingResizing(constraint, parentConstraint, (TiViewProxy *)[subView proxy], resultBounds.size,
			[[subView layer] anchorPoint], viewBounds.size, viewBounds.size, &resultMask);
	
	resultCenter.x += resultBounds.origin.x + viewBounds.origin.x;
	resultCenter.y += resultBounds.origin.y + viewBounds.origin.y;
	
	[subView setAutoresizingMask:resultMask];
	[subView setCenter:resultCenter];
	[subView setBounds:resultBounds];
}

CGFloat WidthFromConstraintGivenWidth(LayoutConstraint * constraint, CGFloat viewWidth)
{
    if (constraint->fullscreen == YES) return viewWidth;
	switch (constraint->width.type)
	{
		case TiDimensionTypeDip:
		{
			return constraint->width.value;
		}
		case TiDimensionTypePercent:
		{
			return constraint->width.value * viewWidth;
		}
		default: {
			break;
		}
	}

	return viewWidth - (TiDimensionCalculateValue(constraint->left, viewWidth) + TiDimensionCalculateValue(constraint->right, viewWidth));
}

BOOL IsLayoutUndefined(LayoutConstraint *constraint)
{
	// if all values are undefined, the layout is considered undefined.
	return TiDimensionIsUndefined(constraint->top)&&
		   TiDimensionIsUndefined(constraint->left)&&
		   TiDimensionIsUndefined(constraint->right)&&
		   TiDimensionIsUndefined(constraint->bottom)&&
		   TiDimensionIsUndefined(constraint->width)&&
		   TiDimensionIsUndefined(constraint->height);
}
#endif