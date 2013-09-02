/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiBase.h"
#import "TiUIView.h"
#import "TiColor.h"
#import "TiRect.h"
#import "TiUtils.h"
#import "ImageLoader.h"
#ifdef USE_TI_UI2DMATRIX	
	#import "Ti2DMatrix.h"
#endif
#if defined(USE_TI_UIIOS3DMATRIX) || defined(USE_TI_UI3DMATRIX)
	#import "Ti3DMatrix.h"
#endif
#import "TiViewProxy.h"
#import "TiApp.h"
#import "UIImage+Resize.h"
#import "TiUIHelper.h"


void InsetScrollViewForKeyboard(UIScrollView * scrollView,CGFloat keyboardTop,CGFloat minimumContentHeight)
{
	VerboseLog(@"ScrollView:%@, keyboardTop:%f minimumContentHeight:%f",scrollView,keyboardTop,minimumContentHeight);

	CGRect scrollVisibleRect = [scrollView convertRect:[scrollView bounds] toView:[[TiApp app] topMostView]];
	//First, find out how much we have to compensate.

	CGFloat obscuredHeight = scrollVisibleRect.origin.y + scrollVisibleRect.size.height - keyboardTop;	
	//ObscuredHeight is how many vertical pixels the keyboard obscures of the scroll view. Some of this may be acceptable.

	CGFloat unimportantArea = MAX(scrollVisibleRect.size.height - minimumContentHeight,0);
	//It's possible that some of the covered area doesn't matter. If it all matters, unimportant is 0.

	//As such, obscuredHeight is now how much actually matters of scrollVisibleRect.

	CGFloat bottomInset = MAX(0,obscuredHeight-unimportantArea);
	[scrollView setContentInset:UIEdgeInsetsMake(0, 0, bottomInset, 0)];

	CGPoint offset = [scrollView contentOffset];

	if(offset.y + bottomInset < 0 )
	{
		offset.y = -bottomInset;
		[scrollView setContentOffset:offset animated:YES];
	}

	VerboseLog(@"ScrollVisibleRect(%f,%f),%fx%f; obscuredHeight:%f; unimportantArea:%f",
			scrollVisibleRect.origin.x,scrollVisibleRect.origin.y,scrollVisibleRect.size.width,scrollVisibleRect.size.height,
			obscuredHeight,unimportantArea);
}

void OffsetScrollViewForRect(UIScrollView * scrollView,CGFloat keyboardTop,CGFloat minimumContentHeight,CGRect responderRect)
{
	VerboseLog(@"ScrollView:%@, keyboardTop:%f minimumContentHeight:%f responderRect:(%f,%f),%fx%f;",
			scrollView,keyboardTop,minimumContentHeight,
			responderRect.origin.x,responderRect.origin.y,responderRect.size.width,responderRect.size.height);

	CGRect scrollVisibleRect = [scrollView convertRect:[scrollView bounds] toView:[[TiApp app] topMostView]];
	//First, find out how much we have to compensate.

	CGFloat obscuredHeight = scrollVisibleRect.origin.y + scrollVisibleRect.size.height - keyboardTop;	
	//ObscuredHeight is how many vertical pixels the keyboard obscures of the scroll view. Some of this may be acceptable.

	//It's possible that some of the covered area doesn't matter. If it all matters, unimportant is 0.

	//As such, obscuredHeight is now how much actually matters of scrollVisibleRect.

	VerboseLog(@"ScrollVisibleRect(%f,%f),%fx%f; obscuredHeight:%f;",
			scrollVisibleRect.origin.x,scrollVisibleRect.origin.y,scrollVisibleRect.size.width,scrollVisibleRect.size.height,
			obscuredHeight);

	scrollVisibleRect.size.height -= MAX(0,obscuredHeight);

	//Okay, the scrollVisibleRect.size now represents the actually visible area.

	CGPoint offsetPoint = [scrollView contentOffset];

	CGPoint offsetForBottomRight;
	offsetForBottomRight.x = responderRect.origin.x + responderRect.size.width - scrollVisibleRect.size.width;
	offsetForBottomRight.y = responderRect.origin.y + responderRect.size.height - scrollVisibleRect.size.height;

	offsetPoint.x = MIN(responderRect.origin.x,MAX(offsetPoint.x,offsetForBottomRight.x));
	offsetPoint.y = MIN(responderRect.origin.y,MAX(offsetPoint.y,offsetForBottomRight.y));
	VerboseLog(@"OffsetForBottomright:(%f,%f) OffsetPoint:(%f,%f)",
			offsetForBottomRight.x, offsetForBottomRight.y, offsetPoint.x, offsetPoint.y);

	CGFloat maxOffset = [scrollView contentInset].bottom + [scrollView contentSize].height - scrollVisibleRect.size.height;
	
	if(maxOffset < offsetPoint.y)
	{
		offsetPoint.y = MAX(0,maxOffset);
	}

	[scrollView setContentOffset:offsetPoint animated:YES];
}

void ModifyScrollViewForKeyboardHeightAndContentHeightWithResponderRect(UIScrollView * scrollView,CGFloat keyboardTop,CGFloat minimumContentHeight,CGRect responderRect)
{
	VerboseLog(@"ScrollView:%@, keyboardTop:%f minimumContentHeight:%f responderRect:(%f,%f),%fx%f;",
			scrollView,keyboardTop,minimumContentHeight,
			responderRect.origin.x,responderRect.origin.y,responderRect.size.width,responderRect.size.height);

	CGRect scrollVisibleRect = [scrollView convertRect:[scrollView bounds] toView:[[TiApp app] topMostView]];
	//First, find out how much we have to compensate.

	CGFloat obscuredHeight = scrollVisibleRect.origin.y + scrollVisibleRect.size.height - keyboardTop;	
	//ObscuredHeight is how many vertical pixels the keyboard obscures of the scroll view. Some of this may be acceptable.

	CGFloat unimportantArea = MAX(scrollVisibleRect.size.height - minimumContentHeight,0);
	//It's possible that some of the covered area doesn't matter. If it all matters, unimportant is 0.

	//As such, obscuredHeight is now how much actually matters of scrollVisibleRect.

	[scrollView setContentInset:UIEdgeInsetsMake(0, 0, MAX(0,obscuredHeight-unimportantArea), 0)];

	VerboseLog(@"ScrollVisibleRect(%f,%f),%fx%f; obscuredHeight:%f; unimportantArea:%f",
			scrollVisibleRect.origin.x,scrollVisibleRect.origin.y,scrollVisibleRect.size.width,scrollVisibleRect.size.height,
			obscuredHeight,unimportantArea);

	scrollVisibleRect.size.height -= MAX(0,obscuredHeight);

	//Okay, the scrollVisibleRect.size now represents the actually visible area.

	CGPoint offsetPoint = [scrollView contentOffset];

	if(!CGRectIsEmpty(responderRect))
	{
		CGPoint offsetForBottomRight;
		offsetForBottomRight.x = responderRect.origin.x + responderRect.size.width - scrollVisibleRect.size.width;
		offsetForBottomRight.y = responderRect.origin.y + responderRect.size.height - scrollVisibleRect.size.height;
	
		offsetPoint.x = MIN(responderRect.origin.x,MAX(offsetPoint.x,offsetForBottomRight.x));
		offsetPoint.y = MIN(responderRect.origin.y,MAX(offsetPoint.y,offsetForBottomRight.y));
		VerboseLog(@"OffsetForBottomright:(%f,%f) OffsetPoint:(%f,%f)",
				offsetForBottomRight.x, offsetForBottomRight.y, offsetPoint.x, offsetPoint.y);
	}
	else
	{
		offsetPoint.x = MAX(0,offsetPoint.x);
		offsetPoint.y = MAX(0,offsetPoint.y);
		VerboseLog(@"OffsetPoint:(%f,%f)",offsetPoint.x, offsetPoint.y);
	}

	[scrollView setContentOffset:offsetPoint animated:YES];
}

NSArray* listenerArray = nil;

@interface TiUIView () {
    TiSelectableBackgroundLayer* _bgLayer;
    BOOL _shouldHandleSelection;
}
-(void)setBackgroundDisabledImage_:(id)value;
-(void)setBackgroundSelectedImage_:(id)value;
-(void)sanitycheckListeners;
@end

@interface TiUIView(Private)
-(void)renderRepeatedBackground:(id)image;
@end

@implementation TiUIView

+ (Class)layerClass {
    return [TiSelectableBackgroundLayer class];
}

DEFINE_EXCEPTIONS

#define kTOUCH_MAX_DIST 70

@synthesize proxy,touchDelegate,oldSize, shouldHandleSelection = _shouldHandleSelection;

#pragma mark Internal Methods

#if VIEW_DEBUG
-(id)retain
{
	[super retain];
	NSLog(@"[VIEW %@] RETAIN: %d", self, [self retainCount]);
}

-(oneway void)release
{
	NSLog(@"[VIEW %@] RELEASE: %d", self, [self retainCount]-1);
	[super release];
}
#endif

-(void)dealloc
{
    [childViews release];
    [transferLock release];
	[transformMatrix release];
	[animation release];
	[singleTapRecognizer release];
	[doubleTapRecognizer release];
	[twoFingerTapRecognizer release];
	[pinchRecognizer release];
	[leftSwipeRecognizer release];
	[rightSwipeRecognizer release];
	[upSwipeRecognizer release];
	[downSwipeRecognizer release];
	[longPressRecognizer release];
	proxy = nil;
	touchDelegate = nil;
	childViews = nil;
	[super dealloc];
}

-(void)detach
{
    if (proxy != nil && [(TiViewProxy*)proxy view] == self)
    {
        [(TiViewProxy*)proxy detachView];
    }
    else {
        NSArray* subviews = [self subviews];
        for (UIView* subview in subviews) {
            if([subview isKindOfClass:[TiUIView class]])
            {
                [(TiUIView*)subview detach];
                
            }
            else {
                [subview removeFromSuperview];
            }
        }
        [self removeFromSuperview];
        self.proxy = nil;
        self.touchDelegate = nil;
    }
}

-(void)removeFromSuperview
{
	if ([NSThread isMainThread])
	{
		[super removeFromSuperview];
	}
	else 
	{
		TiThreadPerformOnMainThread(^{[super removeFromSuperview];}, YES);
	}
}

- (void) initialize
{
    childViews  =[[NSMutableArray alloc] init];
    transferLock = [[NSRecursiveLock alloc] init];
    touchPassThrough = NO;
    _shouldHandleSelection = YES;
    self.clipsToBounds = clipChildren = YES;
    self.userInteractionEnabled = YES;
    backgroundOpacity = 1.0f;
    _bgLayer = (TiSelectableBackgroundLayer*)self.layer;
}

- (id) init
{
	self = [super init];
	if (self != nil)
	{
        [self initialize];
	}
	return self;
}

- (id)initWithFrame:(CGRect)frame
{
	self = [super initWithFrame:frame];
	if (self != nil)
	{
        [self initialize];
	}
	return self;
}

-(BOOL)viewSupportsBaseTouchEvents
{
	// give the ability for the subclass to turn off our event handling
	// if it wants too
	return YES;
}


-(BOOL)proxyHasTapListener
{
	return [proxy _hasListeners:@"singletap"] ||
			[proxy _hasListeners:@"doubletap"] ||
			[proxy _hasListeners:@"twofingertap"];
}

-(BOOL)proxyHasTouchListener
{
	return [proxy _hasListeners:@"touchstart"] ||
			[proxy _hasListeners:@"touchcancel"] ||
			[proxy _hasListeners:@"touchend"] ||
			[proxy _hasListeners:@"touchmove"] ||
			[proxy _hasListeners:@"click"] ||
			[proxy _hasListeners:@"dblclick"];
} 

-(void)updateTouchHandling
{
	BOOL touchEventsSupported = [self viewSupportsBaseTouchEvents];
	handlesTouches = touchEventsSupported && (
                [self proxyHasTouchListener]
                || [self proxyHasTapListener]
                || [proxy _hasListeners:@"swipe"]
                || [proxy _hasListeners:@"pinch"]
                || [proxy _hasListeners:@"longpress"]);

    // If a user has not explicitly set whether or not the view interacts, base it on whether or
    // not it handles events, and if not, set it to the interaction default.
    if (!changedInteraction) {
        self.userInteractionEnabled = handlesTouches || [self interactionDefault];
    }
}

-(void)initializeState
{
	virtualParentTransform = CGAffineTransformIdentity;
	
	[self updateTouchHandling];
	 
	self.backgroundColor = [UIColor clearColor]; 
	self.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
}

-(void)configurationStart
{
    configurationSet = needsToSetBackgroundImage = needsToSetBackgroundDisabledImage = needsToSetBackgroundSelectedImage = NO;
}

-(void)configurationSet
{
	// can be used to trigger things after all properties are set
    configurationSet = YES;
    if (needsToSetBackgroundImage)
        [self setBackgroundImage_:[[self proxy] valueForKey:@"backgroundImage"]];
    if (needsToSetBackgroundDisabledImage)
        [self setBackgroundDisabledImage_:[[self proxy] valueForKey:@"backgroundDisabledImage"]];
    if (needsToSetBackgroundSelectedImage)
        [self setBackgroundSelectedImage_:[[self proxy] valueForKey:@"backgroundSelectedImage"]];
    _bgLayer.readyToCreateDrawables = YES;
}

-(void)setProxy:(TiProxy *)p
{
	proxy = p;
	[proxy setModelDelegate:self];
	[self sanitycheckListeners];
}

-(id)transformMatrix
{
	return transformMatrix;
}

- (id)accessibilityElement
{
	return self;
}

#pragma mark - Accessibility API

- (void)setAccessibilityLabel_:(id)accessibilityLabel
{
	id accessibilityElement = self.accessibilityElement;
	if (accessibilityElement != nil) {
		[accessibilityElement setIsAccessibilityElement:YES];
		[accessibilityElement setAccessibilityLabel:[TiUtils stringValue:accessibilityLabel]];
	}
}

- (void)setAccessibilityValue_:(id)accessibilityValue
{
	id accessibilityElement = self.accessibilityElement;
	if (accessibilityElement != nil) {
		[accessibilityElement setIsAccessibilityElement:YES];
		[accessibilityElement setAccessibilityValue:[TiUtils stringValue:accessibilityValue]];
	}
}

- (void)setAccessibilityHint_:(id)accessibilityHint
{
	id accessibilityElement = self.accessibilityElement;
	if (accessibilityElement != nil) {
		[accessibilityElement setIsAccessibilityElement:YES];
		[accessibilityElement setAccessibilityHint:[TiUtils stringValue:accessibilityHint]];
	}
}

- (void)setAccessibilityHidden_:(id)accessibilityHidden
{
	if ([TiUtils isIOS5OrGreater]) {
		self.accessibilityElementsHidden = [TiUtils boolValue:accessibilityHidden def:NO];
	}
}

#pragma mark Layout 

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
    [self updateViewShadowPath];
}


-(void)setFrame:(CGRect)frame
{
	[super setFrame:frame];
	
	// this happens when a view is added to another view but not
	// through the framework (such as a tableview header) and it
	// means we need to force the layout of our children
	if (childrenInitialized==NO && 
		CGRectIsEmpty(frame)==NO &&
		[self.proxy isKindOfClass:[TiViewProxy class]])
	{
		childrenInitialized=YES;
		[(TiViewProxy*)self.proxy layoutChildren:NO];
	}
}


-(void)checkBounds
{
    CGRect newBounds = [self bounds];
    if(!CGSizeEqualToSize(oldSize, newBounds.size)) {
        oldSize = newBounds.size;
        //TIMOB-11197, TC-1264
        if (!animating) {
            [CATransaction begin];
            [CATransaction setDisableActions:YES];
        }
        if (self.layer.mask != nil) {
            [self.layer.mask setFrame:newBounds];
        }
        
//        [self.layer setFrame:newBounds]; // not needed and doesnt work
        
        [self frameSizeChanged:[TiUtils viewPositionRect:self] bounds:newBounds];
        if (!animating) {
            [CATransaction commit];
        }
    }
}



-(void)setBounds:(CGRect)bounds
{
	[super setBounds:bounds];
	[self checkBounds];
}

-(void)layoutSubviews
{
	[super layoutSubviews];
	[self checkBounds];
}

-(void)updateTransform
{
#ifdef USE_TI_UI2DMATRIX	
	if ([transformMatrix isKindOfClass:[Ti2DMatrix class]])
	{
		self.transform = CGAffineTransformConcat(virtualParentTransform, [(Ti2DMatrix*)transformMatrix matrix]);
		return;
	}
#endif
#if defined(USE_TI_UIIOS3DMATRIX) || defined(USE_TI_UI3DMATRIX)
	if ([transformMatrix isKindOfClass:[Ti3DMatrix class]])
	{
		self.layer.transform = CATransform3DConcat(CATransform3DMakeAffineTransform(virtualParentTransform),[(Ti3DMatrix*)transformMatrix matrix]);
		return;
	}
#endif
	self.transform = virtualParentTransform;
}


-(void)setVirtualParentTransform:(CGAffineTransform)newTransform
{
	virtualParentTransform = newTransform;
	[self updateTransform];
}

-(void)fillBoundsToRect:(TiRect*)rect
{
	CGRect r = [self bounds];
	[rect setRect:r];
}

-(void)fillFrameToRect:(TiRect*)rect
{
	CGRect r = [self frame];
	[rect setRect:r];
}

#pragma mark Public APIs

-(void) setBackgroundGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [_bgLayer setGradient:newGradient forState:UIControlStateNormal];
}

-(void) setBackgroundSelectedGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [_bgLayer setGradient:newGradient forState:UIControlStateSelected];
}

-(void) setBackgroundDisabledGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [_bgLayer setGradient:newGradient forState:UIControlStateDisabled];
}

-(UIColor*) colorValue:(id)color_
{
    UIColor* uicolor;
	if ([color_ isKindOfClass:[UIColor class]])
	{
        uicolor = (UIColor*)color_;
	}
	else
	{
		uicolor = [[TiUtils colorValue:color_] _color];
	}
    return uicolor;
}

-(void) setBackgroundColor_:(id)color_
{
    [_bgLayer setColor:[self colorValue:color_] forState:UIControlStateNormal];
}

-(void) setBackgroundSelectedColor_:(id)color_
{
    [_bgLayer setColor:[self colorValue:color_] forState:UIControlStateSelected];
}

-(void) setBackgroundDisabledColor_:(id)color_
{
    [_bgLayer setColor:[self colorValue:color_] forState:UIControlStateDisabled];
}

-(UIImage*)convertToUIImage:(id)arg
{
	if (arg==nil) return nil;
    UIImage *image = nil;
	
    if ([arg isKindOfClass:[TiBlob class]]) {
        TiBlob *blob = (TiBlob*)arg;
        image = [blob image];
    }
    else if ([arg isKindOfClass:[TiFile class]]) {
        NSURL *url = [TiUtils toURL:arg proxy:proxy];
        image = [[ImageLoader sharedLoader] loadImmediateImage:url];
    }
    else if ([arg isKindOfClass:[UIImage class]]) {
		// called within this class
        image = (UIImage*)arg;
    }
    return image;
}



-(UIImage*)loadImage:(id)arg
{
    if (arg==nil) return nil;
    UIImage *image = nil;
	
    if ([arg isKindOfClass:[TiBlob class]]) {
        TiBlob *blob = (TiBlob*)arg;
        image = [blob image];
    }
    else if ([arg isKindOfClass:[UIImage class]]) {
		// called within this class
        image = (UIImage*)arg;
    }
    else {
        NSURL *url;
        if ([arg isKindOfClass:[TiFile class]]) {
            TiFile *file = (TiFile*)arg;
            url = [NSURL fileURLWithPath:[file path]];
        }
        else {
            url = [TiUtils toURL:arg proxy:proxy];
        }
        if (TiDimensionIsUndefined(leftCap) && TiDimensionIsUndefined(topCap) &&
            TiDimensionIsUndefined(rightCap) && TiDimensionIsUndefined(bottomCap)) {
            image =  [[ImageLoader sharedLoader]loadImmediateImage:url];
        }
        else {
            image = [[ImageLoader sharedLoader] loadImmediateStretchableImage:url withLeftCap:leftCap topCap:topCap rightCap:rightCap bottomCap:bottomCap];
        }
    }
	return image;
}

-(void) setBackgroundImage_:(id)image
{
    if (!configurationSet) {
        needsToSetBackgroundImage = YES;
        return;
    }
    UIImage* bgImage = [self loadImage:image];
    [_bgLayer setImage:bgImage forState:UIControlStateNormal];
}

-(void) setBackgroundSelectedImage_:(id)image
{
    if (!configurationSet) {
        needsToSetBackgroundSelectedImage = YES;
        return;
    }
    UIImage* bgImage = [self loadImage:image];
    [_bgLayer setImage:bgImage forState:UIControlStateSelected];
}

-(void) setBackgroundDisabledImage_:(id)image
{
    if (!configurationSet) {
        needsToSetBackgroundDisabledImage = YES;
        return;
    }
    UIImage* bgImage = [self loadImage:image];
    [_bgLayer setImage:bgImage forState:UIControlStateSelected];
}

-(void)setOpacity_:(id)opacity
{
 	ENSURE_UI_THREAD_1_ARG(opacity);
	self.alpha = [TiUtils floatValue:opacity];
}

-(void)setBackgroundRepeat_:(id)repeat
{
    _bgLayer.imageRepeat = [TiUtils boolValue:repeat def:NO];
}

-(void)setBackgroundOpacity_:(id)opacity
{
    self.layer.opacity = [TiUtils floatValue:opacity def:1.0f];
}

-(void)setImageCap_:(id)arg
{
    ENSURE_SINGLE_ARG(arg,NSDictionary);
    NSDictionary* dict = (NSDictionary*)arg;
    if ([dict objectForKey:@"left"]) {
        leftCap = TiDimensionFromObject([dict objectForKey:@"left"]);
    }
    if ([dict objectForKey:@"right"]) {
        rightCap = TiDimensionFromObject([dict objectForKey:@"right"]);
    }
    if ([dict objectForKey:@"top"]) {
        topCap = TiDimensionFromObject([dict objectForKey:@"top"]);
    }
    if ([dict objectForKey:@"bottom"]) {
        bottomCap = TiDimensionFromObject([dict objectForKey:@"bottom"]);
    }
}

-(void)setBorderRadius_:(id)radius
{
	self.layer.cornerRadius = [TiUtils floatValue:radius];
    [self updateViewShadowPath];
}


-(void)setBorderColor_:(id)color
{
	TiColor *ticolor = [TiUtils colorValue:color];
	self.layer.borderWidth = MAX(self.layer.borderWidth,1);
	self.layer.borderColor = [ticolor _color].CGColor;
}

-(void)setBorderWidth_:(id)w
{
	self.layer.borderWidth = TiDimensionCalculateValueFromString([TiUtils stringValue:w]);
}

-(void)setAnchorPoint_:(id)point
{
	self.layer.anchorPoint = [TiUtils pointValue:point];
}

-(void)setTransform_:(id)transform_
{
	RELEASE_TO_NIL(transformMatrix);
	transformMatrix = [transform_ retain];
	[self updateTransform];
}

-(void)setCenter_:(id)point
{
	self.center = [TiUtils pointValue:point];
}

-(void)setVisible_:(id)visible
{
  	ENSURE_UI_THREAD_1_ARG(visible);
    BOOL oldVal = self.hidden;
    BOOL newVal = ![TiUtils boolValue:visible];
    
    
    if (newVal == oldVal) return;
    
    self.hidden = newVal;
	//TODO: If we have an animated show, hide, or setVisible, here's the spot for it.
    TiViewProxy* viewProxy = (TiViewProxy*)[self proxy];
	
	if(viewProxy.parentVisible)
	{
		if (newVal)
		{
			[viewProxy willHide];
            [viewProxy refreshView:nil];
		}
		else
		{
            [viewProxy refreshView:nil];
			[viewProxy willShow];
            //Redraw ourselves if changing from invisible to visible, to handle any changes made
		}
	}
    
//    //Redraw ourselves if changing from invisible to visible, to handle any changes made
//	if (!self.hidden && oldVal) {
//        [viewProxy willEnqueue];
//    }
}

-(void)setBgState:(UIControlState)state
{
    [_bgLayer setState:state];
}

-(UIControlState)getBgState
{
    return [_bgLayer getState];
}

-(void)setTouchEnabled_:(id)arg
{
	self.userInteractionEnabled = [TiUtils boolValue:arg];
    [self setBgState:self.userInteractionEnabled?UIControlStateNormal:UIControlStateDisabled];
    changedInteraction = YES;
}

-(BOOL) touchEnabled {
	return touchEnabled;
}

-(void)setTouchPassThrough_:(id)arg
{
	touchPassThrough = [TiUtils boolValue:arg];
}

-(BOOL)touchPassThrough {
    return touchPassThrough;
}

-(UIView *)backgroundWrapperView
{
	return self;
}


-(void)setClipChildren_:(id)arg
{
    clipChildren = [TiUtils boolValue:arg];
    self.clipsToBounds = [self clipChildren];
}

-(BOOL)clipChildren
{
    return (clipChildren && ([[self shadowLayer] shadowOpacity] == 0));

}


-(CALayer *)shadowLayer
{
	return [self layer];
}


-(void)setViewShadow_:(id)arg
{
    ENSURE_SINGLE_ARG(arg,NSDictionary);
    [TiUIHelper applyShadow:arg toLayer:[self shadowLayer]];
    [self updateViewShadowPath];
}

-(void)updateViewShadowPath
{
    if ([[self shadowLayer] shadowOpacity] > 0.0f)
    {
        //to speedup things
        [self shadowLayer].shadowPath =[UIBezierPath bezierPathWithRoundedRect:[self bounds] cornerRadius:self.layer.cornerRadius].CGPath;
    }
}

-(NSArray*) childViews
{
    return [NSArray arrayWithArray:childViews];
}

-(void)didAddSubview:(UIView*)view
{
    if ([view isKindOfClass:[TiUIView class]])
    {
        [childViews addObject:view];
    }
	// So, it turns out that adding a subview places it beneath the gradient layer.
	// Every time we add a new subview, we have to make sure the gradient stays where it belongs..
//    if (_bgLayer != nil) {
//		[[[self backgroundWrapperView] layer] insertSublayer:_bgLayer atIndex:0];
//	}
}

- (void)willRemoveSubview:(UIView *)subview
{
    
    if ([subview isKindOfClass:[TiUIView class]] && childViews)
    {
        [childViews removeObject:subview];
    }
}

-(void)cancelAllAnimations
{
    if (animation != nil) {
        [animation cancel:nil];
        RELEASE_TO_NIL(animation);
    }
    [CATransaction begin];
	[[self layer] removeAllAnimations];
	[CATransaction commit];
}

-(void)animate:(TiAnimation *)newAnimation
{
	RELEASE_TO_NIL(animation);
	
	if ([self.proxy isKindOfClass:[TiViewProxy class]] && [(TiViewProxy*)self.proxy viewReady]==NO)
	{
		DebugLog(@"[DEBUG] Ti.View.animate() called before view %@ was ready: Will re-attempt", self);
		if (animationDelayGuard++ > 5)
		{
			DebugLog(@"[DEBUG] Animation guard triggered, exceeded timeout to perform animation.");
            animationDelayGuard = 0;
			return;
		}
		[self performSelector:@selector(animate:) withObject:newAnimation afterDelay:0.01];
		return;
	}
	
	animationDelayGuard = 0;
    //TIMOB-13237. Wait for layout to finish before animating.
    //TODO. This is a hack. When we implement the polynomial layout for iOS we will be able to do
    //a full layout of this view and associated views in the animation block.
    if ([self.proxy isKindOfClass:[TiViewProxy class]] && [(TiViewProxy*)self.proxy willBeRelaying]) {
		DebugLog(@"[DEBUG] Ti.View.animate() called while view waiting to relayout: Will re-attempt", self);
		if (animationDelayGuardForLayout++ > 2) {
            DebugLog(@"[DEBUG] Animation guard triggered, exceeded timeout for layout to occur. Continuing.");
        } else {
            [self performSelector:@selector(animate:) withObject:newAnimation afterDelay:0.02];
            return;
        }
    }
    animationDelayGuardForLayout = 0;    

	if (newAnimation != nil)
	{
		RELEASE_TO_NIL(animation);
		animation = [newAnimation retain];
		[animation animate:self];
	}	
	else
	{
		DebugLog(@"[WARN] Ti.View.animate() (view %@) could not make animation from: %@", self, newAnimation);
	}
}
-(void)animationStarted
{
    animating = YES;
}
-(void)animationCompleted
{
	animating = NO;
}

-(BOOL)animating
{
	return animating;
}

#pragma mark Property Change Support

-(SEL)selectorForProperty:(NSString*)key
{
	NSString *method = [NSString stringWithFormat:@"set%@%@_:", [[key substringToIndex:1] uppercaseString], [key substringFromIndex:1]];
	return NSSelectorFromString(method);
}

-(SEL)selectorForlayoutProperty:(NSString*)key
{
	NSString *method = [NSString stringWithFormat:@"set%@%@:", [[key substringToIndex:1] uppercaseString], [key substringFromIndex:1]];
	return NSSelectorFromString(method);
}

-(void)readProxyValuesWithKeys:(id<NSFastEnumeration>)keys
{
	DoProxyDelegateReadValuesWithKeysFromProxy(self, keys, proxy);
}

-(void)propertyChanged:(NSString*)key oldValue:(id)oldValue newValue:(id)newValue proxy:(TiProxy*)proxy_
{
	DoProxyDelegateChangedValuesWithProxy(self, key, oldValue, newValue, proxy_);
}


//Todo: Generalize.
-(void)setKrollValue:(id)value forKey:(NSString *)key withObject:(id)props
{
	if(value == [NSNull null])
	{
		value = nil;
	}

	NSString *method = SetterStringForKrollProperty(key);
    
	SEL methodSel = NSSelectorFromString([method stringByAppendingString:@"withObject:"]);
	if([self respondsToSelector:methodSel])
	{
		[self performSelector:methodSel withObject:value withObject:props];
		return;
	}		

	methodSel = NSSelectorFromString(method);
	if([self respondsToSelector:methodSel])
	{
		[self performSelector:methodSel withObject:value];
	}
}

- (void)detachViewProxy {
    if(!proxy) return;
    self.proxy = nil;
    for (UIView *subview in self.subviews) {
        if ([subview isKindOfClass:[TiUIView class]])
            [(TiUIView*)subview detachViewProxy];
    }
}

-(void)transferProxy:(TiViewProxy*)newProxy
{
    [self transferProxy:newProxy deep:NO];
}

-(void)transferProxy:(TiViewProxy*)newProxy deep:(BOOL)deep
{
    [self transferProxy:newProxy withBlockBefore:nil withBlockAfter:nil deep:deep];
}

-(void)transferProxy:(TiViewProxy*)newProxy withBlockBefore:(void (^)(TiViewProxy* proxy))blockBefore
                withBlockAfter:(void (^)(TiViewProxy* proxy))blockAfter deep:(BOOL)deep
{
	TiViewProxy * oldProxy = (TiViewProxy *)[self proxy];
	
	// We can safely skip everything if we're transferring to ourself.
	if (oldProxy != newProxy) {
        
        if(blockBefore)
        {
            blockBefore(newProxy);
        }
        
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        [transferLock lock];
        
        if (deep) {
			NSArray *subProxies = [newProxy children];
			[[oldProxy children] enumerateObjectsUsingBlock:^(TiViewProxy *oldSubProxy, NSUInteger idx, BOOL *stop) {
				TiViewProxy *newSubProxy = idx < [subProxies count] ? [subProxies objectAtIndex:idx] : nil;
				[[oldSubProxy view] transferProxy:newSubProxy withBlockBefore:blockBefore withBlockAfter:blockAfter deep:deep];
			}];
		}
        
        NSSet* transferableProperties = [[oldProxy class] transferableProperties];
        NSMutableSet* oldProperties = [NSMutableSet setWithArray:(NSArray *)[oldProxy allKeys]];
        NSMutableSet* newProperties = [NSMutableSet setWithArray:(NSArray *)[newProxy allKeys]];
        NSMutableSet* keySequence = [NSMutableSet setWithArray:[newProxy keySequence]];
        NSMutableSet* layoutProps = [NSMutableSet setWithArray:[TiViewProxy layoutProperties]];
        [oldProperties minusSet:newProperties];
        [oldProperties minusSet:layoutProps];
        [newProperties minusSet:keySequence];
        [layoutProps intersectSet:newProperties];
        [newProperties intersectSet:transferableProperties];
        [oldProperties intersectSet:transferableProperties];
        
        id<NSFastEnumeration> keySeq = keySequence;
        id<NSFastEnumeration> oldProps = oldProperties;
        id<NSFastEnumeration> newProps = newProperties;
        id<NSFastEnumeration> fastLayoutProps = layoutProps;
        
		[oldProxy retain];
		
        [self configurationStart];
		[newProxy setReproxying:YES];
        
		[oldProxy setView:nil];
		[newProxy setView:self];
        
		[self setProxy:newProxy];

        //The important sequence first:
		for (NSString * thisKey in keySeq)
		{
			id newValue = [newProxy valueForKey:thisKey];
			id oldValue = [oldProxy valueForKey:thisKey];
			if ((oldValue != newValue) && ![oldValue isEqual:newValue]) {
				[self setKrollValue:newValue forKey:thisKey withObject:nil];
			}
		}
        
		for (NSString * thisKey in fastLayoutProps)
		{
			id newValue = [newProxy valueForKey:thisKey];
			id oldValue = [oldProxy valueForKey:thisKey];
			if ((oldValue != newValue) && ![oldValue isEqual:newValue]) {
                SEL selector = [self selectorForlayoutProperty:thisKey];
				if([[self proxy] respondsToSelector:selector])
                {
                    [[self proxy] performSelector:selector withObject:newValue];
                }
			}
		}

		for (NSString * thisKey in oldProps)
		{
			[self setKrollValue:nil forKey:thisKey withObject:nil];
		}

		for (NSString * thisKey in newProps)
		{
			id newValue = [newProxy valueForKey:thisKey];
			id oldValue = [oldProxy valueForKey:thisKey];
			if ((oldValue != newValue) && ![oldValue isEqual:newValue]) {
				[self setKrollValue:newValue forKey:thisKey withObject:nil];
			}
		}
        
        [pool release];
        pool = nil;

        [self configurationSet];
        
		[oldProxy release];
		
		[newProxy setReproxying:NO];

 
        if(blockAfter)
        {
          blockAfter(newProxy);  
        }

        [transferLock unlock];
        
	}
    
}

-(BOOL)validateTransferToProxy:(TiViewProxy*)newProxy deep:(BOOL)deep
{
	TiViewProxy * oldProxy = (TiViewProxy *)[self proxy];
	
	if (oldProxy == newProxy) {
		return YES;
	}    
	if (![newProxy isMemberOfClass:[oldProxy class]]) {
        DebugLog(@"[ERROR] Cannot reproxy not same proxy class");
		return NO;
	}
    
    UIView * ourView = [[oldProxy parent] parentViewForChild:oldProxy];
    UIView *parentView = [self superview];
    if (parentView!=ourView)
    {
        DebugLog(@"[ERROR] Cannot reproxy not same parent view");
        return NO;
    }
	
	__block BOOL result = YES;
	if (deep) {
		NSArray *subProxies = [newProxy children];
		NSArray *oldSubProxies = [oldProxy children];
		if ([subProxies count] != [oldSubProxies count]) {
            DebugLog(@"[ERROR] Cannot reproxy not same number of subproxies");
			return NO;
		}
		[oldSubProxies enumerateObjectsUsingBlock:^(TiViewProxy *oldSubProxy, NSUInteger idx, BOOL *stop) {
			TiViewProxy *newSubProxy = [subProxies objectAtIndex:idx];
            TiUIView* view = [oldSubProxy view];
            if (!view){
                DebugLog(@"[ERROR] Cannot reproxy no subproxy view");
                result = NO;
                *stop = YES;
            }
            else
                result = [view validateTransferToProxy:newSubProxy deep:YES]; //we assume that the view is already created
			if (!result) {
				*stop = YES;
			}
		}];
	}
	return result;
}

-(id)proxyValueForKey:(NSString *)key
{
	return [proxy valueForKey:key];
}

#pragma mark First Responder delegation

-(void)makeRootViewFirstResponder
{
	[[[TiApp controller] view] becomeFirstResponder];
}

#pragma mark Recognizers

-(UITapGestureRecognizer*)singleTapRecognizer;
{
	if (singleTapRecognizer == nil) {
		singleTapRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedSingleTap:)];
		[self configureGestureRecognizer:singleTapRecognizer];
		[self addGestureRecognizer:singleTapRecognizer];

		if (doubleTapRecognizer != nil) {
			[singleTapRecognizer requireGestureRecognizerToFail:doubleTapRecognizer];
		}
	}
	return singleTapRecognizer;
}

-(UITapGestureRecognizer*)doubleTapRecognizer;
{
	if (doubleTapRecognizer == nil) {
		doubleTapRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedDoubleTap:)];
		[doubleTapRecognizer setNumberOfTapsRequired:2];
		[self configureGestureRecognizer:doubleTapRecognizer];
		[self addGestureRecognizer:doubleTapRecognizer];
		
		if (singleTapRecognizer != nil) {
			[singleTapRecognizer requireGestureRecognizerToFail:doubleTapRecognizer];
		}		
	}
	return doubleTapRecognizer;
}

-(UITapGestureRecognizer*)twoFingerTapRecognizer;
{
	if (twoFingerTapRecognizer == nil) {
		twoFingerTapRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedSingleTap:)];
		[twoFingerTapRecognizer setNumberOfTouchesRequired:2];
		[self configureGestureRecognizer:twoFingerTapRecognizer];
		[self addGestureRecognizer:twoFingerTapRecognizer];
	}
	return twoFingerTapRecognizer;
}

-(UIPinchGestureRecognizer*)pinchRecognizer;
{
	if (pinchRecognizer == nil) {
		pinchRecognizer = [[UIPinchGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedPinch:)];
		[self configureGestureRecognizer:pinchRecognizer];
		[self addGestureRecognizer:pinchRecognizer];
	}
	return pinchRecognizer;
}

-(UISwipeGestureRecognizer*)leftSwipeRecognizer;
{
	if (leftSwipeRecognizer == nil) {
		leftSwipeRecognizer = [[UISwipeGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedSwipe:)];
		[leftSwipeRecognizer setDirection:UISwipeGestureRecognizerDirectionLeft];
		[self configureGestureRecognizer:leftSwipeRecognizer];
		[self addGestureRecognizer:leftSwipeRecognizer];
	}
	return leftSwipeRecognizer;
}

-(UISwipeGestureRecognizer*)rightSwipeRecognizer;
{
	if (rightSwipeRecognizer == nil) {
		rightSwipeRecognizer = [[UISwipeGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedSwipe:)];
		[rightSwipeRecognizer setDirection:UISwipeGestureRecognizerDirectionRight];
		[self configureGestureRecognizer:rightSwipeRecognizer];
		[self addGestureRecognizer:rightSwipeRecognizer];
	}
	return rightSwipeRecognizer;
}
-(UISwipeGestureRecognizer*)upSwipeRecognizer;
{
	if (upSwipeRecognizer == nil) {
		upSwipeRecognizer = [[UISwipeGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedSwipe:)];
		[upSwipeRecognizer setDirection:UISwipeGestureRecognizerDirectionUp];
		[self configureGestureRecognizer:upSwipeRecognizer];
		[self addGestureRecognizer:upSwipeRecognizer];
	}
	return upSwipeRecognizer;
}
-(UISwipeGestureRecognizer*)downSwipeRecognizer;
{
	if (downSwipeRecognizer == nil) {
		downSwipeRecognizer = [[UISwipeGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedSwipe:)];
		[downSwipeRecognizer setDirection:UISwipeGestureRecognizerDirectionDown];
		[self configureGestureRecognizer:downSwipeRecognizer];
		[self addGestureRecognizer:downSwipeRecognizer];
	}
	return downSwipeRecognizer;
}

-(UILongPressGestureRecognizer*)longPressRecognizer;
{
	if (longPressRecognizer == nil) {
		longPressRecognizer = [[UILongPressGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedLongPress:)];
		[self configureGestureRecognizer:longPressRecognizer];
		[self addGestureRecognizer:longPressRecognizer];
	}
	return longPressRecognizer;
}

-(void)recognizedSingleTap:(UITapGestureRecognizer*)recognizer
{
	NSDictionary *event = [TiUtils dictionaryFromGesture:recognizer inView:self];
    if ([recognizer numberOfTouchesRequired] == 2) {
		[proxy fireEvent:@"twofingertap" withObject:event];
	}
    else
        [proxy fireEvent:@"singletap" withObject:event];
}

-(void)recognizedDoubleTap:(UITapGestureRecognizer*)recognizer
{
	NSDictionary *event = [TiUtils dictionaryFromGesture:recognizer inView:self];
    //Because double-tap suppresses touchStart and double-click, we must do this:
    if ([proxy _hasListeners:@"touchstart"])
    {
        [proxy fireEvent:@"touchstart" withObject:event propagate:YES];
    }
    if ([proxy _hasListeners:@"dblclick"]) {
        [proxy fireEvent:@"dblclick" withObject:event propagate:YES];
    }
    [proxy fireEvent:@"doubletap" withObject:event];
}

-(void)recognizedPinch:(UIPinchGestureRecognizer*)recognizer 
{ 
    NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:
                           NUMDOUBLE(recognizer.scale), @"scale", 
                           NUMDOUBLE(recognizer.velocity), @"velocity", 
                           nil]; 
    [self.proxy fireEvent:@"pinch" withObject:event]; 
}

-(void)recognizedLongPress:(UILongPressGestureRecognizer*)recognizer 
{ 
    if ([recognizer state] == UIGestureRecognizerStateBegan) {
        CGPoint p = [recognizer locationInView:self];
        NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:
                               NUMFLOAT(p.x), @"x",
                               NUMFLOAT(p.y), @"y",
                               nil];
        [self.proxy fireEvent:@"longpress" withObject:event]; 
    }
}

-(NSString*) swipeStringFromGesture:(UISwipeGestureRecognizer *)recognizer
{
    NSString* swipeString;
	switch ([recognizer direction]) {
		case UISwipeGestureRecognizerDirectionUp:
			swipeString = @"up";
			break;
		case UISwipeGestureRecognizerDirectionDown:
			swipeString = @"down";
			break;
		case UISwipeGestureRecognizerDirectionLeft:
			swipeString = @"left";
			break;
		case UISwipeGestureRecognizerDirectionRight:
			swipeString = @"right";
			break;
		default:
			swipeString = @"unknown";
			break;
	}
    return swipeString;
}

-(void)recognizedSwipe:(UISwipeGestureRecognizer *)recognizer
{
	NSMutableDictionary *event = [[TiUtils dictionaryFromGesture:recognizer inView:self] mutableCopy];
	[event setValue:[self swipeStringFromGesture:recognizer] forKey:@"direction"];
	[proxy fireEvent:@"swipe" withObject:event];
	[event release];

}

#pragma mark Touch Events


- (BOOL)interactionDefault
{
	return YES;
}

- (BOOL)interactionEnabled
{
	return self.userInteractionEnabled;
}

- (BOOL)hasTouchableListener
{
	return handlesTouches;
}

- (UIView *)hitTest:(CGPoint) point withEvent:(UIEvent *)event 
{
	BOOL hasTouchListeners = [self hasTouchableListener];

	// if we don't have any touch listeners, see if interaction should
	// be handled at all.. NOTE: we don't turn off the views interactionEnabled
	// property since we need special handling ourselves and if we turn it off
	// on the view, we'd never get this event
	if (hasTouchListeners == NO && [self interactionEnabled]==NO)
	{
		return nil;
	}
	
    // OK, this is problematic because of the situation where:
    // touchDelegate --> view --> button
    // The touch never reaches the button, because the touchDelegate is as deep as the touch goes.
    
    /*
	// delegate to our touch delegate if we're hit but it's not for us
	if (hasTouchListeners==NO && touchDelegate!=nil)
	{
		return touchDelegate;
	}
     */
	
	UIView *hitView = [super hitTest:point withEvent:event];
	if (touchPassThrough)
	{
		if (hitView != self) 
			return hitView;
		return nil;
	}
	return hitView;
}

// TODO: Revisit this design decision in post-1.3.0
-(void)handleControlEvents:(UIControlEvents)events
{
	// For subclasses (esp. buttons) to override when they have event handlers.
	TiViewProxy* parentProxy = [(TiViewProxy*)proxy parent];
	if ([parentProxy viewAttached] && [parentProxy canHaveControllerParent]) {
		[[parentProxy view] handleControlEvents:events];
	}
}

// For subclasses
-(BOOL)touchedContentViewWithEvent:(UIEvent *)event
{
    return NO;
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    if ([[event touchesForView:self] count] > 0 || [self touchedContentViewWithEvent:event]) {
        [self processTouchesBegan:touches withEvent:event];
    }
    [super touchesBegan:touches withEvent:event];
}

- (void)processTouchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    
    UITouch *touch = [touches anyObject];
    if (_shouldHandleSelection) {
        [self setBgState:UIControlStateSelected];
    }
	
	if (handlesTouches)
	{
		if ([proxy _hasListeners:@"touchstart"])
		{
            NSDictionary *evt = [TiUtils dictionaryFromTouch:touch inView:self];
			[proxy fireEvent:@"touchstart" withObject:evt propagate:YES];
			[self handleControlEvents:UIControlEventTouchDown];
		}
	}
}

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
    

    if ([[event touchesForView:self] count] > 0 || [self touchedContentViewWithEvent:event]) {
        [self processTouchesMoved:touches withEvent:event];
    }
    [super touchesMoved:touches withEvent:event];
}

- (void)processTouchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
	UITouch *touch = [touches anyObject];
    CGPoint localPoint = [touch locationInView:self];
    BOOL outside = (localPoint.x < -kTOUCH_MAX_DIST || (localPoint.x - self.frame.size.width)  > kTOUCH_MAX_DIST ||
                    localPoint.y < -kTOUCH_MAX_DIST || (localPoint.y - self.frame.size.height)  > kTOUCH_MAX_DIST);
    if (_shouldHandleSelection) {
        [self setBgState:outside?UIControlStateNormal:UIControlStateSelected];
    }
	if (handlesTouches)
	{
		if ([proxy _hasListeners:@"touchmove"])
		{
            NSDictionary *evt = [TiUtils dictionaryFromTouch:touch inView:self];
			[proxy fireEvent:@"touchmove" withObject:evt propagate:YES];
		}
	}
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event 
{
    if ([[event touchesForView:self] count] > 0 || [self touchedContentViewWithEvent:event]) {
        [self processTouchesEnded:touches withEvent:event];
    }
    [super touchesEnded:touches withEvent:event];
}

- (void)processTouchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    if (_shouldHandleSelection) {
        [self setBgState:self.userInteractionEnabled?UIControlStateNormal:UIControlStateDisabled];
    }
	if (handlesTouches)
	{
		UITouch *touch = [touches anyObject];
        BOOL hasTouchEnd = [proxy _hasListeners:@"touchend"];
        BOOL hasDblclick = [proxy _hasListeners:@"dblclick"];
        BOOL hasClick = [proxy _hasListeners:@"click"];
		if (hasTouchEnd || hasDblclick || hasClick)
		{
            NSDictionary *evt = [TiUtils dictionaryFromTouch:touch inView:self];
            if (hasTouchEnd) {
                [proxy fireEvent:@"touchend" withObject:evt propagate:YES];
                [self handleControlEvents:UIControlEventTouchCancel];
            }
            
            // Click handling is special; don't propagate if we have a delegate,
            // but DO invoke the touch delegate.
            // clicks should also be handled by any control the view is embedded in.
            if (hasDblclick && [touch tapCount] == 2) {
                [proxy fireEvent:@"dblclick" withObject:evt propagate:YES];
                return;
            }
            if (hasClick)
            {
                CGPoint localPoint = [touch locationInView:self];
                BOOL outside = (localPoint.x < -kTOUCH_MAX_DIST || (localPoint.x - self.frame.size.width)  > kTOUCH_MAX_DIST ||
                                localPoint.y < -kTOUCH_MAX_DIST || (localPoint.y - self.frame.size.height)  > kTOUCH_MAX_DIST);
                if (!outside && touchDelegate == nil) {
                    [proxy fireEvent:@"click" withObject:evt propagate:YES];
                    return;
                } 
            }
		}
	}
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event 
{
    [self setBgState:self.userInteractionEnabled?UIControlStateNormal:UIControlStateDisabled];
    if ([[event touchesForView:self] count] > 0 || [self touchedContentViewWithEvent:event]) {
        [self processTouchesCancelled:touches withEvent:event];
    }
    [super touchesCancelled:touches withEvent:event];
}

- (void)processTouchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
	if (handlesTouches)
	{
		UITouch *touch = [touches anyObject];
		NSDictionary *evt = [TiUtils dictionaryFromTouch:touch inView:self];
		if ([proxy _hasListeners:@"touchcancel"])
		{
			[proxy fireEvent:@"touchcancel" withObject:evt propagate:YES];
		}
	}
}

#pragma mark Listener management

-(void)removeGestureRecognizerOfClass:(Class)c
{
    for (UIGestureRecognizer* r in [self gestureRecognizers]) {
        if ([r isKindOfClass:c]) {
            [self removeGestureRecognizer:r];
            break;
        }
    }
}

-(void)configureGestureRecognizer:(UIGestureRecognizer*)gestureRecognizer
{
    [gestureRecognizer setDelaysTouchesBegan:NO];
    [gestureRecognizer setDelaysTouchesEnded:NO];
    [gestureRecognizer setCancelsTouchesInView:NO];
}

- (UIGestureRecognizer *)gestureRecognizerForEvent:(NSString *)event
{
    if ([event isEqualToString:@"singletap"]) {
        return [self singleTapRecognizer];
    }
    if ([event isEqualToString:@"doubletap"]) {
        return [self doubleTapRecognizer];
    }
    if ([event isEqualToString:@"twofingertap"]) {
        return [self twoFingerTapRecognizer];
    }
    if ([event isEqualToString:@"lswipe"]) {
        return [self leftSwipeRecognizer];
    }
    if ([event isEqualToString:@"rswipe"]) {
        return [self rightSwipeRecognizer];
    }
    if ([event isEqualToString:@"uswipe"]) {
        return [self upSwipeRecognizer];
    }
    if ([event isEqualToString:@"dswipe"]) {
        return [self downSwipeRecognizer];
    }
    if ([event isEqualToString:@"pinch"]) {
        return [self pinchRecognizer];
    }
    if ([event isEqualToString:@"longpress"]) {
        return [self longPressRecognizer];
    }
    return nil;
}

-(void)handleListenerAddedWithEvent:(NSString *)event
{
	ENSURE_UI_THREAD_1_ARG(event);
    [self updateTouchHandling];
    if ([event isEqualToString:@"swipe"]) {
        [[self gestureRecognizerForEvent:@"uswipe"] setEnabled:YES];
        [[self gestureRecognizerForEvent:@"dswipe"] setEnabled:YES];
        [[self gestureRecognizerForEvent:@"rswipe"] setEnabled:YES];
        [[self gestureRecognizerForEvent:@"lswipe"] setEnabled:YES];
    }
    else {
        [[self gestureRecognizerForEvent:event] setEnabled:YES];
    }
}

-(void)handleListenerRemovedWithEvent:(NSString *)event
{
	ENSURE_UI_THREAD_1_ARG(event);
	// unfortunately on a remove, we have to check all of them
	// since we might be removing one but we still have others

	[self updateTouchHandling];
    if ([event isEqualToString:@"swipe"]) {
        [[self gestureRecognizerForEvent:@"uswipe"] setEnabled:NO];
        [[self gestureRecognizerForEvent:@"dswipe"] setEnabled:NO];
        [[self gestureRecognizerForEvent:@"rswipe"] setEnabled:NO];
        [[self gestureRecognizerForEvent:@"lswipe"] setEnabled:NO];
    }
    else {
        [[self gestureRecognizerForEvent:event] setEnabled:NO];
    }
}

-(void)listenerAdded:(NSString*)event count:(int)count
{
	if (count == 1 && [self viewSupportsBaseTouchEvents])
	{
		[self handleListenerAddedWithEvent:event];
	}
}

-(void)listenerRemoved:(NSString*)event count:(int)count
{
	if (count == 0)
	{
		[self handleListenerRemovedWithEvent:event];
	}
}

-(void)sanitycheckListeners	//TODO: This can be optimized and unwound later.
{
	if(listenerArray == nil){
		listenerArray = [[NSArray alloc] initWithObjects: @"singletap",
						 @"doubletap",@"twofingertap",@"swipe",@"pinch",@"longpress",nil];
	}
	for (NSString * eventName in listenerArray) {
		if ([proxy _hasListeners:eventName]) {
			[self handleListenerAddedWithEvent:eventName];
		}
	}
}

-(void)setViewMask_:(id)arg
{
    UIImage* image = [self loadImage:arg];
    if (image == nil) {
        self.layer.mask = nil;
    }
    else {
        if (self.layer.mask == nil) {
            self.layer.mask = [CALayer layer];
            self.layer.mask.frame = self.layer.bounds;
        }
        self.layer.opaque = NO;
        self.layer.mask.contentsScale = [image scale];
        self.layer.mask.contentsCenter = TiDimensionLayerContentCenter(topCap, leftCap, topCap, leftCap, [image size]);
        if (!CGPointEqualToPoint(self.layer.mask.contentsCenter.origin,CGPointZero)) {
            self.layer.mask.magnificationFilter = @"nearest";
        } else {
            self.layer.mask.magnificationFilter = @"linear";
        }
        self.layer.mask.contents = (id)image.CGImage;
    }
    
    [self.layer setNeedsDisplay];
}


@end
