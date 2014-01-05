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
#import "TiSVGImage.h"
#import "TiImageHelper.h"
#import "TiTransition.h"
#import "TiViewAnimationStep.h"
#import "TiBorderLayer.h"

@interface CALayer (Additions)
- (void)bringToFront;
- (void)sendToBack;
@end

@implementation CALayer (Additions)

- (void)bringToFront {
    CALayer *superlayer = self.superlayer;
    [self removeFromSuperlayer];
    [superlayer addSublayer:self];
}

- (void)sendToBack {
    CALayer *superlayer = self.superlayer;
    [self removeFromSuperlayer];
    [superlayer insertSublayer:self atIndex:0];
}
@end


@interface UntouchableView : UIView

@end

@implementation UntouchableView

- (UIView *)hitTest:(CGPoint) point withEvent:(UIEvent *)event
{
    UIView* result = [super hitTest:point withEvent:event];
    if (result == self)
        return nil;
    return result;
}

@end


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
    UntouchableView* _childrenHolder;
    TiBorderLayer* _borderLayer;
    BOOL _shouldHandleSelection;
    BOOL _customUserInteractionEnabled;
    BOOL _touchEnabled;
    BOOL _dispatchPressed;
    
    BOOL needsToSetBackgroundImage;
	BOOL needsToSetBackgroundSelectedImage;
	BOOL needsToSetBackgroundDisabledImage;
    BOOL needsUpdateBackgroundImageFrame;
    UIEdgeInsets _backgroundPadding;
    UIEdgeInsets _borderPadding;
    CGFloat* radii;
    BOOL usePathAsBorder;
}
-(void)setBackgroundDisabledImage_:(id)value;
-(void)setBackgroundSelectedImage_:(id)value;
-(void)sanitycheckListeners;
@end

@interface TiUIView(Private)
-(void)renderRepeatedBackground:(id)image;
@end

@implementation TiUIView

DEFINE_EXCEPTIONS

#define kTOUCH_MAX_DIST 70

@synthesize proxy,touchDelegate,oldSize, backgroundLayer = _bgLayer, shouldHandleSelection = _shouldHandleSelection, animateBgdTransition, runningAnimation;

#pragma mark Internal Methods

//+(Class)layerClass
//{
//    return [CAShapeLayer class];
//}

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
    [_childrenHolder release];
    [childViews release];
    [transferLock release];
	[transformMatrix release];
	[_bgLayer release];
	[_borderLayer release];
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
    if (radii != NULL) {
        free(radii);
        radii = NULL;
    }
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
        [self cancelAllAnimations];
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
    self.clipsToBounds = self.layer.masksToBounds = clipChildren = YES;
    self.userInteractionEnabled = YES;
    self.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    backgroundOpacity = 1.0f;
    _customUserInteractionEnabled = YES;
    _touchEnabled = YES;
    _dispatchPressed = NO;
    animateBgdTransition = NO;
    _backgroundPadding = _borderPadding = UIEdgeInsetsZero;
    viewState = -1;
    radii = NULL;
    usePathAsBorder = NO;
}


- (id) init
{
	self = [super init];
	if (self != nil)
	{
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

-(void)ensureGestureListeners
{
    if ([(TiViewProxy*)proxy _hasListeners:@"swipe"]) {
        [[self gestureRecognizerForEvent:@"uswipe"] setEnabled:YES];
        [[self gestureRecognizerForEvent:@"dswipe"] setEnabled:YES];
        [[self gestureRecognizerForEvent:@"rswipe"] setEnabled:YES];
        [[self gestureRecognizerForEvent:@"lswipe"] setEnabled:YES];
    }
    if ([(TiViewProxy*)proxy _hasListeners:@"pinch"]) {
         [[self gestureRecognizerForEvent:@"pinch"] setEnabled:YES];
    }
    if ([(TiViewProxy*)proxy _hasListeners:@"longpress"]) {
        [[self gestureRecognizerForEvent:@"longpress"] setEnabled:YES];
    }
}

-(BOOL)proxyHasTapListener
{
	return [proxy _hasAnyListeners:[NSArray arrayWithObjects:@"singletap", @"doubletap", @"twofingertap", nil]];
}

-(BOOL)proxyHasTouchListener
{
	return [proxy _hasAnyListeners:[NSArray arrayWithObjects:@"touchstart", @"touchcancel", @"touchend", @"touchmove", @"click", @"dblclick", nil]];
}

-(BOOL) proxyHasGestureListeners
{
	return [proxy _hasAnyListeners:[NSArray arrayWithObjects:@"swipe", @"pinch", @"longpress", nil]];
}

-(void)updateTouchHandling
{
    BOOL touchEventsSupported = [self viewSupportsBaseTouchEvents];
    handlesTouches = touchEventsSupported && (
                [self proxyHasTouchListener]
                || [self proxyHasTapListener]
                || [self proxyHasGestureListeners]);
    [self ensureGestureListeners];
    // If a user has not explicitly set whether or not the view interacts, base it on whether or
    // not it handles events, and if not, set it to the interaction default.
    if (!changedInteraction) {
        _customUserInteractionEnabled = handlesTouches || [self interactionDefault];
    }
}

-(void)initializeState
{
	
	[self updateTouchHandling];
	 
	super.backgroundColor = nil;
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
    if (_bgLayer) {
        _bgLayer.readyToCreateDrawables = YES;
    }
    if (_borderLayer) {
        _borderLayer.readyToCreateDrawables = YES;
    }
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
    self.accessibilityElementsHidden = [TiUtils boolValue:accessibilityHidden def:NO];
}

#pragma mark Layout

-(void)applyPathToLayersMask:(CALayer*)layer path:(CGPathRef)path
{
    if (layer == nil) return;
    if (path == nil) {
        layer.mask = nil;
    }
    else {
        if (layer.mask == nil) {
            layer.mask = [CAShapeLayer layer];
        }
        if (runningAnimation) {
            CABasicAnimation *pathAnimation = [CABasicAnimation animationWithKeyPath:@"path"];
            pathAnimation.fromValue = (id)((CAShapeLayer*)layer.mask).path;
            pathAnimation.duration = [runningAnimation duration];
            pathAnimation.timingFunction = [runningAnimation curve];
            pathAnimation.fillMode = kCAFillModeBoth;
            pathAnimation.toValue = (id)path;
            [layer.mask addAnimation:pathAnimation forKey:@"clippingpath"];
        }
        ((CAShapeLayer*)layer.mask).path = path;
    }
}

CGPathRef CGPathCreateRoundiiRect( const CGRect rect, const CGFloat* radii)
{
    if (radii == NULL) {
        return nil;
    }
    // create a mutable path
    CGMutablePathRef path = CGPathCreateMutable();
    
    // get the 4 corners of the rect
    CGPoint topLeft = CGPointMake(rect.origin.x, rect.origin.y);
    CGPoint topRight = CGPointMake(rect.origin.x + rect.size.width, rect.origin.y);
    CGPoint bottomRight = CGPointMake(rect.origin.x + rect.size.width, rect.origin.y + rect.size.height);
    CGPoint bottomLeft = CGPointMake(rect.origin.x, rect.origin.y + rect.size.height);
    
    // move to top left
    CGPathMoveToPoint(path, NULL, topLeft.x + radii[0], topLeft.y);
    
    if (radii[2] == radii[3]) {
        CGFloat radius = radii[2];
        CGPathAddRelativeArc(path, NULL, topRight.x - radius, topRight.y + radius, radius, -M_PI_2, M_PI_2);
    }
    else {
        // add top line
        CGPathAddLineToPoint(path, NULL, topRight.x - radii[2], topRight.y);
        // add top right curve
        CGPathAddQuadCurveToPoint(path, NULL, topRight.x, topRight.y, topRight.x, topRight.y + radii[3]);
    }
    
    
    if (radii[4] == radii[5]) {
        CGFloat radius = radii[4];
        CGPathAddRelativeArc(path, NULL, bottomRight.x - radius, bottomRight.y - radius, radius, 0, M_PI_2);
    }
    else {
        // add right line
        CGPathAddLineToPoint(path, NULL, bottomRight.x, bottomRight.y - radii[4]);
        
        // add bottom right curve
        CGPathAddQuadCurveToPoint(path, NULL, bottomRight.x, bottomRight.y, bottomRight.x - radii[5], bottomRight.y);
    }
    
    if (radii[6] == radii[7]) {
        CGFloat radius = radii[6];
        CGPathAddRelativeArc(path, NULL, bottomLeft.x + radius, bottomLeft.y - radius, radius, M_PI_2, M_PI_2);
    }
    else {
        // add bottom line
        CGPathAddLineToPoint(path, NULL, bottomLeft.x + radii[6], bottomLeft.y);
        
        // add bottom left curve
        CGPathAddQuadCurveToPoint(path, NULL, bottomLeft.x, bottomLeft.y, bottomLeft.x, bottomLeft.y - radii[7]);
    }
    if (radii[0] == radii[1]) {
        CGFloat radius = radii[0];
        CGPathAddRelativeArc(path, NULL, topLeft.x + radius, topLeft.y + radius, radius, M_PI, M_PI_2);
    }
    else {
        // add left line
        CGPathAddLineToPoint(path, NULL, topLeft.x, topLeft.y + radii[0]);
        
        // add top left curve
        CGPathAddQuadCurveToPoint(path, NULL, topLeft.x, topLeft.y, topLeft.x + radii[1], topLeft.y);
    }
    
    // return the path
    return path;
}

-(void)updatePathForClipping:(CGRect)bounds
{
    //the 0.5f is there to have a clean border where you don't see the background
    CGPathRef path = self.layer.shadowPath = CGPathCreateRoundiiRect(bounds, radii);
    if (clipChildren && usePathAsBorder && (!self.layer.mask || [self.layer.mask isKindOfClass:[CAShapeLayer class]]))
    {
        [self applyPathToLayersMask:self.layer path:path];
        
    }
    else if (!usePathAsBorder) {
        CGFloat radius = radii[0];
        self.layer.cornerRadius = radius;
        if (_bgLayer) _bgLayer.cornerRadius = radius;
    }
    if (_bgLayer)
    {
        _bgLayer.shadowPath = path;
    }
    CGPathRelease(path);
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
    if (radii != NULL)
    {
        [self updatePathForClipping:bounds];
    }
    if (_borderLayer) {
        [_borderLayer updateBorderPath:radii inBounds:bounds];
        _borderLayer.frame = UIEdgeInsetsInsetRect(bounds, _borderPadding);
    }
    if (_bgLayer) {
        _bgLayer.frame = UIEdgeInsetsInsetRect(bounds, _backgroundPadding);
    }

    if (self.layer.mask != nil) {
        [self.layer.mask setFrame:bounds];
    }
    [self updateTransform];
}


-(void)setFrame:(CGRect)frame
{
	[super setFrame:frame];
	
	// this happens when a view is added to another view but not
	// through the framework (such as a tableview header) and it
	// means we need to force the layout of our children
//	if (childrenInitialized==NO && 
//		CGRectIsEmpty(frame)==NO &&
//		[self.proxy isKindOfClass:[TiViewProxy class]])
//	{
//		childrenInitialized=YES;
//		[(TiViewProxy*)self.proxy layoutChildren:NO];
//	}
}


-(void)updateBounds:(CGRect)newBounds
{
    //TIMOB-11197, TC-1264
    [CATransaction begin];
    if (runningAnimation == nil) {
        [CATransaction setValue:(id)kCFBooleanTrue forKey:kCATransactionDisableActions];
    }
    else {
        [CATransaction setAnimationDuration:[runningAnimation duration]];
        [CATransaction setAnimationTimingFunction:[runningAnimation curve]];
    }
    
    [self frameSizeChanged:[TiUtils viewPositionRect:self] bounds:newBounds];
    [CATransaction commit];
}


-(void)checkBounds
{
    CGRect newBounds = [self bounds];
    if(!CGRectIsEmpty(newBounds) && !CGSizeEqualToSize(oldSize, newBounds.size)) {
        [self updateBounds:newBounds];
        oldSize = newBounds.size;
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


- (void)didMoveToSuperview
{
	[self updateTransform];
	[super didMoveToSuperview];
}

-(void)updateTransform
{
#ifdef USE_TI_UI2DMATRIX	
	if ([transformMatrix isKindOfClass:[Ti2DMatrix class]] && self.superview != nil)
	{
        CGSize size = self.bounds.size;
        CGSize parentSize = self.superview.bounds.size;
		self.transform = [(Ti2DMatrix*)transformMatrix matrixInViewSize:size andParentSize:parentSize];
		return;
	}
#endif
#if defined(USE_TI_UIIOS3DMATRIX) || defined(USE_TI_UI3DMATRIX)
	if ([transformMatrix isKindOfClass:[Ti3DMatrix class]])
	{
		self.layer.transform = [(Ti3DMatrix*)transformMatrix matrix];
		return;
	}
#endif
	self.transform = CGAffineTransformIdentity;
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

-(void)setTintColor_:(id)color
{
    if ([TiUtils isIOS7OrGreater]) {
        TiColor *ticolor = [TiUtils colorValue:color];
        [self performSelector:@selector(setTintColor:) withObject:[ticolor _color]];
    }
}

-(UIView*)parentViewForChildren
{
//    if (_childrenHolder == nil) {
//        _childrenHolder = [[UntouchableView alloc] initWithFrame:self.bounds];
//        _childrenHolder.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
//        _childrenHolder.layer.masksToBounds = YES;
//        _childrenHolder.layer.cornerRadius = self.layer.cornerRadius;
//
//        [self addSubview:_childrenHolder];
//    }
    return self;
}

-(void)onCreateCustomBackground
{
    
}

-(TiSelectableBackgroundLayer*)getOrCreateCustomBackgroundLayer
{
    if (_bgLayer != nil) {
        return _bgLayer;
    }
    
    _bgLayer = [[TiSelectableBackgroundLayer alloc] init];
    
    [[[self backgroundWrapperView] layer] insertSublayer:_bgLayer atIndex:0];
    _bgLayer.frame = UIEdgeInsetsInsetRect([[self backgroundWrapperView] layer].bounds, _backgroundPadding);
    _bgLayer.opacity = backgroundOpacity;
    _bgLayer.shadowPath = self.layer.shadowPath;
    _bgLayer.readyToCreateDrawables = configurationSet;
    _bgLayer.animateTransition = animateBgdTransition;
    [self onCreateCustomBackground];
    return _bgLayer;
}


-(TiBorderLayer*)getOrCreateBorderLayer
{
    if (_borderLayer != nil) {
        return _borderLayer;
    }
    
    _borderLayer = [[TiBorderLayer alloc] init];
    _borderLayer.cornerRadius = self.layer.cornerRadius;
   [_borderLayer setRadii:radii];
    [[[self backgroundWrapperView] layer] addSublayer:_borderLayer];
    CGRect bounds = UIEdgeInsetsInsetRect([[self backgroundWrapperView] layer].bounds, _borderPadding);
    if (!CGRectIsEmpty(bounds)) {
        _borderLayer.frame = bounds;
    }
    
    _borderLayer.opacity = backgroundOpacity;
    return _borderLayer;
}

-(CALayer*)backgroundLayer
{
    return _bgLayer;
}

-(void) setBackgroundGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [[self getOrCreateCustomBackgroundLayer] setGradient:newGradient forState:UIControlStateNormal];
}

-(void) setBackgroundSelectedGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [[self getOrCreateCustomBackgroundLayer] setGradient:newGradient forState:UIControlStateSelected];
    [[self getOrCreateCustomBackgroundLayer] setGradient:newGradient forState:UIControlStateHighlighted];
}

-(void) setBackgroundHighlightedGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [[self getOrCreateCustomBackgroundLayer] setGradient:newGradient forState:UIControlStateHighlighted];
}

-(void) setBackgroundDisabledGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [[self getOrCreateCustomBackgroundLayer] setGradient:newGradient forState:UIControlStateDisabled];
}
-(void)setBackgroundColor:(UIColor*)color
{
    // this trick is to prevent tableviewcell from changing our color. When we want
    // to actually change our color, lets call super!
}
-(void) setBackgroundColor_:(id)color
{
    UIColor* uicolor;
	if ([color isKindOfClass:[UIColor class]])
	{
        uicolor = (UIColor*)color;
	}
	else
	{
		uicolor = [[TiUtils colorValue:color] _color];
	}
    if (backgroundOpacity < 1.0f) {
        const CGFloat* components = CGColorGetComponents(uicolor.CGColor);
        float alpha = CGColorGetAlpha(uicolor.CGColor) * backgroundOpacity;
        uicolor = [UIColor colorWithRed:components[0] green:components[1] blue:components[2] alpha:alpha];
    }
    if (clipChildren || radii == nil)
    {
        super.backgroundColor = uicolor;
    }
    else
    {
        [[self getOrCreateCustomBackgroundLayer] setColor:uicolor forState:UIControlStateNormal];
    }
}

-(void) setBackgroundSelectedColor_:(id)color
{
    UIColor* uiColor = [TiUtils colorValue:color].color;
    [[self getOrCreateCustomBackgroundLayer] setColor:uiColor forState:UIControlStateSelected];
    [[self getOrCreateCustomBackgroundLayer] setColor:uiColor forState:UIControlStateHighlighted];
}

-(void) setBackgroundHighlightedColor_:(id)color
{
    UIColor* uiColor = [TiUtils colorValue:color].color;
    [[self getOrCreateCustomBackgroundLayer] setColor:uiColor forState:UIControlStateHighlighted];
}

-(void) setBackgroundDisabledColor_:(id)color
{
    UIColor* uiColor = [TiUtils colorValue:color].color;
    [[self getOrCreateCustomBackgroundLayer] setColor:uiColor forState:UIControlStateDisabled];
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


-(id)loadImageOrSVG:(id)arg
{
    if (arg==nil) return nil;
	if (TiDimensionIsUndefined(leftCap) && TiDimensionIsUndefined(topCap) &&
        TiDimensionIsUndefined(rightCap) && TiDimensionIsUndefined(bottomCap)) {
        return [TiUtils loadBackgroundImage:arg forProxy:proxy];
    }
    else {
        return [TiUtils loadBackgroundImage:arg forProxy:proxy withLeftCap:leftCap topCap:topCap rightCap:rightCap bottomCap:bottomCap];
    }
	return nil;
}
-(UIImage*)loadImage:(id)arg
{
    if (arg==nil) return nil;
    id result = nil;
	if (TiDimensionIsUndefined(leftCap) && TiDimensionIsUndefined(topCap) &&
        TiDimensionIsUndefined(rightCap) && TiDimensionIsUndefined(bottomCap)) {
        result =  [TiUtils loadBackgroundImage:arg forProxy:proxy];
    }
    else {
        result =  [TiUtils loadBackgroundImage:arg forProxy:proxy withLeftCap:leftCap topCap:topCap rightCap:rightCap bottomCap:bottomCap];
    }
    if ([result isKindOfClass:[UIImage class]]) return result;
    else if ([result isKindOfClass:[TiSVGImage class]]) return [((TiSVGImage*)result) fullImage];
	return nil;
}

-(void) setBackgroundImage_:(id)image
{
    if (!configurationSet) {
        needsToSetBackgroundImage = YES;
        return;
    }
    [[self getOrCreateCustomBackgroundLayer] setImage:[self loadImageOrSVG:image] forState:UIControlStateNormal];
}

-(void) setBackgroundSelectedImage_:(id)arg
{
    if (!configurationSet) {
        needsToSetBackgroundSelectedImage = YES;
        return;
    }
    id image = [self loadImageOrSVG:arg];
    [[self getOrCreateCustomBackgroundLayer] setImage:image forState:UIControlStateHighlighted];
    [[self getOrCreateCustomBackgroundLayer] setImage:image forState:UIControlStateSelected];
}

-(void) setBackgroundHighlightedImage_:(id)image
{
    if (!configurationSet) {
        needsToSetBackgroundSelectedImage = YES;
        return;
    }
    [[self getOrCreateCustomBackgroundLayer] setImage:[self loadImageOrSVG:image] forState:UIControlStateHighlighted];
}

-(void) setBackgroundDisabledImage_:(id)image
{
    if (!configurationSet) {
        needsToSetBackgroundDisabledImage = YES;
        return;
    }
    [[self getOrCreateCustomBackgroundLayer] setImage:[self loadImageOrSVG:image] forState:UIControlStateSelected];
}

-(void) setBackgroundInnerShadows_:(id)value
{
    ENSURE_TYPE_OR_NIL(value, NSArray);
    NSArray* result = nil;
    if ([value count] >0) {
        result = [NSMutableArray arrayWithCapacity:[value count]];
        [value enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
            [(id)result addObject:[TiUIHelper getShadow:obj]];
        }];
    }
    
    [[self getOrCreateCustomBackgroundLayer] setInnerShadows:result forState:UIControlStateNormal];
}

-(void) setBackgroundSelectedInnerShadows_:(id)value
{
    ENSURE_TYPE_OR_NIL(value, NSArray);
    NSArray* result = nil;
    if ([value count] >0) {
        result = [NSMutableArray arrayWithCapacity:[value count]];
        [value enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
            [(id)result addObject:[TiUIHelper getShadow:obj]];
        }];
    }
    
    [[self getOrCreateCustomBackgroundLayer] setInnerShadows:result forState:UIControlStateSelected];
    [[self getOrCreateCustomBackgroundLayer] setInnerShadows:result forState:UIControlStateHighlighted];
}

-(void) setBackgroundHighlightedInnerShadows_:(id)value
{
    ENSURE_TYPE_OR_NIL(value, NSArray);
    NSArray* result = nil;
    if ([value count] >0) {
        result = [NSMutableArray arrayWithCapacity:[value count]];
        [value enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
            [(id)result addObject:[TiUIHelper getShadow:obj]];
        }];
    }
    
    [[self getOrCreateCustomBackgroundLayer] setInnerShadows:result forState:UIControlStateHighlighted];
}

-(void) setBackgroundDisabledInnerShadows_:(id)value
{
    ENSURE_TYPE_OR_NIL(value, NSArray);
    NSArray* result = nil;
    if ([value count] >0) {
        result = [NSMutableArray arrayWithCapacity:[value count]];
        [value enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
            [(id)result addObject:[TiUIHelper getShadow:obj]];
        }];
    }
    
    [[self getOrCreateCustomBackgroundLayer] setInnerShadows:result forState:UIControlStateDisabled];
}


-(void)setOpacity_:(id)opacity
{
 	ENSURE_UI_THREAD_1_ARG(opacity);
	self.alpha = [TiUtils floatValue:opacity];
}

-(void)setBackgroundRepeat_:(id)repeat
{
    [self getOrCreateCustomBackgroundLayer].imageRepeat = [TiUtils boolValue:repeat def:NO];
}

-(void)setBackgroundOpacity_:(id)opacity
{
    backgroundOpacity = [TiUtils floatValue:opacity def:1.0f];
    
    id value = [proxy valueForKey:@"backgroundColor"];
    if (value!=nil) {
        [self setBackgroundColor_:value];
    }

    if (_bgLayer) {
        _bgLayer.opacity = backgroundOpacity;
    }
}


//-(void)setBackgroundImageLayerBounds:(CGRect)bounds
//{
//    if ([self backgroundLayer] != nil)
//    {
//        CGRect backgroundFrame = CGRectMake(bounds.origin.x - padding.origin.x,
//                                            bounds.origin.y - padding.origin.y,
//                                            bounds.size.width + padding.origin.x + padding.size.width,
//                                            bounds.size.height + padding.origin.y + padding.size.height);
//        [self backgroundLayer].frame = backgroundFrame;
//    }
//}

//-(void) updateBackgroundImageFrameWithPadding
//{
//    if (!configurationSet){
//        needsUpdateBackgroundImageFrame = YES;
//        return; // lazy init
//    }
//    [self setBackgroundImageLayerBounds:self.bounds];
//}

//-(void)setBackgroundImage_:(id)url
//{
//    [super setBackgroundImage_:url];
//    //if using padding we must not mask to bounds.
//    [self backgroundLayer].masksToBounds = CGRectEqualToRect(padding, CGRectZero) ;
////    [self updateBackgroundImageFrameWithPadding];
//}
//
//-(void)setBackgroundPaddingLeft_:(id)left
//{
//    padding.origin.x = [TiUtils floatValue:left];
//    [self updateBackgroundImageFrameWithPadding];
//}
//
//-(void)setBackgroundPaddingRight_:(id)right
//{
//    padding.size.width = [TiUtils floatValue:right];
//    [self updateBackgroundImageFrameWithPadding];
//}
//
//-(void)setBackgroundPaddingTop_:(id)top
//{
//    padding.origin.y = [TiUtils floatValue:top];
//    [self updateBackgroundImageFrameWithPadding];
//}
//
//-(void)setBackgroundPaddingBottom_:(id)bottom
//{
//    padding.size.height = [TiUtils floatValue:bottom];
//    [self updateBackgroundImageFrameWithPadding];
//}


-(void)setBackgroundPadding_:(id)value
{
    _backgroundPadding = [TiUtils insetValue:value];
    if (_bgLayer) {
        _bgLayer.frame = UIEdgeInsetsInsetRect(self.bounds, _backgroundPadding);
    }
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

-(void)setusePathAsBorder:(BOOL)value
{
    if (value != usePathAsBorder)
    {
        usePathAsBorder = value;
        if (usePathAsBorder) {
            [_borderLayer swithToContentBorder];
            self.layer.cornerRadius = 0;
            if (_bgLayer) _bgLayer.cornerRadius = 0;
            if (_borderLayer) _borderLayer.cornerRadius = 0;
            if (self.backgroundColor)
            {
                [[self getOrCreateCustomBackgroundLayer] setColor:super.backgroundColor forState:UIControlStateNormal];
                super.backgroundColor = nil;
            }
        }
        else {
            CGFloat radius = radii[0];
            self.layer.cornerRadius = radius;
            if (_bgLayer) _bgLayer.cornerRadius = radius;
            if (_borderLayer) _borderLayer.cornerRadius = radius;
        }
    }
    else if(!usePathAsBorder)
    {
        CGFloat radius = radii[0];
        self.layer.cornerRadius = radius;
        if (_bgLayer) _bgLayer.cornerRadius = radius;
        if (_borderLayer) _borderLayer.cornerRadius = radius;
    }
}

-(void)setBorderRadius_:(id)value
{
    if ([value isKindOfClass:[NSArray class]]) {
        radii =(CGFloat*)malloc(8*sizeof(CGFloat));
        NSArray* array = (NSArray*)value;
        int count = [array count];
        if (count == 4)
        {
            for (int i = 0; i < count; ++i){
                radii[2*i] = radii[2*i+1] = [TiUtils floatValue:[array objectAtIndex:i] def:0.0f];
            }
        } else  if (count == 8)
        {
            for (int i = 0; i < count; ++i){
                radii[i] = [TiUtils floatValue:[array objectAtIndex:i] def:0.0f];
            }
        }
        [self setusePathAsBorder:YES];
    }
    else
    {
        radii = (CGFloat*)malloc(8*sizeof(CGFloat));
        CGFloat radius = [TiUtils floatValue:value def:0.0f];
        for (int i = 0; i < 8; ++i){
            radii[i] = radius;
        }
        [self setusePathAsBorder:!clipChildren];
    }
    if (_borderLayer)
    {
        [_borderLayer setRadii:radii];
    }
}

-(void)setBorderColor_:(id)color
{
    UIColor* uiColor = [TiUtils colorValue:color].color;
    [[self getOrCreateBorderLayer] setColor:uiColor forState:UIControlStateNormal];
}

-(void) setBorderSelectedColor_:(id)color
{
    UIColor* uiColor = [TiUtils colorValue:color].color;
    [[self getOrCreateBorderLayer] setColor:uiColor forState:UIControlStateSelected];
    [[self getOrCreateBorderLayer] setColor:uiColor forState:UIControlStateHighlighted];
}

-(void) setBorderHighlightedColor_:(id)color
{
    UIColor* uiColor = [TiUtils colorValue:color].color;
    [[self getOrCreateBorderLayer] setColor:uiColor forState:UIControlStateHighlighted];
}

-(void) setBorderDisabledColor_:(id)color
{
    UIColor* uiColor = [TiUtils colorValue:color].color;
    [[self getOrCreateBorderLayer] setColor:uiColor forState:UIControlStateDisabled];
}

-(void) setBorderGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [[self getOrCreateBorderLayer] setGradient:newGradient forState:UIControlStateNormal];
}

-(void) setBorderSelectedGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [[self getOrCreateBorderLayer] setGradient:newGradient forState:UIControlStateSelected];
    [[self getOrCreateBorderLayer] setGradient:newGradient forState:UIControlStateHighlighted];
}

-(void) setBorderHighlightedGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [[self getOrCreateBorderLayer] setGradient:newGradient forState:UIControlStateHighlighted];
}

-(void) setBorderDisabledGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [[self getOrCreateBorderLayer] setGradient:newGradient forState:UIControlStateDisabled];
}

-(void)setBorderWidth_:(id)w
{
	[[self getOrCreateBorderLayer] setClipWidth:TiDimensionCalculateValueFromString([TiUtils stringValue:w])];
}

-(void)setBorderPadding_:(id)value
{
    _borderPadding = [TiUtils insetValue:value];
    if (_borderLayer) {
        CGRect bounds = UIEdgeInsetsInsetRect([[self backgroundWrapperView] layer].bounds, _borderPadding);
        if (!CGRectIsEmpty(bounds)) {
            _borderLayer.frame = bounds;
        }
    }
}

-(void)setAnchorPoint_:(id)point
{
	CGPoint anchorPoint = [TiUtils pointValue:point];
	CGPoint newPoint = CGPointMake(self.bounds.size.width * anchorPoint.x, self.bounds.size.height * anchorPoint.y);
    CGPoint oldPoint = CGPointMake(self.bounds.size.width * self.layer.anchorPoint.x, self.bounds.size.height * self.layer.anchorPoint.y);

    CGPoint position = self.layer.position;
    
    position.x -= oldPoint.x;
    position.x += newPoint.x;
    
    position.y -= oldPoint.y;
    position.y += newPoint.y;
    
    self.layer.position = position;
    self.layer.anchorPoint = anchorPoint;
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
    state = [self realStateForState:state];
    if (_bgLayer != nil) {
        [_bgLayer setState:state];
    }
    if (_borderLayer != nil) {
        [_borderLayer setState:state];
    }
}

-(UIControlState)getBgState
{
    if (_bgLayer != nil) {
        return [_bgLayer getState];
    }
    else if(_borderLayer != nil) {
        return [_borderLayer getState];
    }
    return UIControlStateNormal;
}

-(void)setTouchEnabled_:(id)arg
{
	_touchEnabled = [TiUtils boolValue:arg def:_touchEnabled];
}

-(void)setEnabled_:(id)arg
{
	_customUserInteractionEnabled = [TiUtils boolValue:arg def:[self interactionDefault]];
    [self setBgState:UIControlStateNormal];
    changedInteraction = YES;
}

-(void)setDispatchPressed_:(id)arg
{
	_dispatchPressed = [TiUtils boolValue:arg def:_dispatchPressed];
}

-(BOOL) touchEnabled {
	return _touchEnabled;
}

-(void)setTouchPassThrough_:(id)arg
{
	touchPassThrough = [TiUtils boolValue:arg];
}


-(void)setExclusiveTouch_:(id)arg
{
	self.exclusiveTouch = [TiUtils boolValue:arg];
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
//    if ([[self shadowLayer] shadowOpacity] > 0.0f)
//    {
//        [self shadowLayer].shadowPath = [_borderLayer clippingPath];
//    }
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
    [_borderLayer bringToFront];
    [_bgLayer sendToBack];
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
    [CATransaction begin];
	[[self layer] removeAllAnimations];
    for (CALayer* layer in [[self layer] sublayers]) {
        [layer removeAllAnimations];
    }
	[CATransaction commit];
}

-(BOOL)animating
{
	return [(TiAnimatableProxy*)self.proxy animating];
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
		[proxy fireEvent:@"twofingertap" withObject:event checkForListener:NO];
	}
    else
        [proxy fireEvent:@"singletap" withObject:event checkForListener:NO];
}

-(void)recognizedDoubleTap:(UITapGestureRecognizer*)recognizer
{
	NSDictionary *event = [TiUtils dictionaryFromGesture:recognizer inView:self];
    //Because double-tap suppresses touchStart and double-click, we must do this:
    if ([proxy _hasListeners:@"touchstart"])
    {
        [proxy fireEvent:@"touchstart" withObject:event checkForListener:NO];
    }
    if ([proxy _hasListeners:@"dblclick"]) {
        [proxy fireEvent:@"dblclick" withObject:event checkForListener:NO];
    }
    [proxy fireEvent:@"doubletap" withObject:event checkForListener:NO];
}

-(void)recognizedPinch:(UIPinchGestureRecognizer*)recognizer 
{ 
    NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:
                           NUMDOUBLE(recognizer.scale), @"scale", 
                           NUMDOUBLE(recognizer.velocity), @"velocity", 
                           nil]; 
    [self.proxy fireEvent:@"pinch" withObject:event checkForListener:NO];
}

-(void)recognizedLongPress:(UILongPressGestureRecognizer*)recognizer 
{ 
    if ([recognizer state] == UIGestureRecognizerStateBegan) {
        CGPoint p = [recognizer locationInView:self];
        NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:
                               NUMFLOAT(p.x), @"x",
                               NUMFLOAT(p.y), @"y",
                               nil];
        [self.proxy fireEvent:@"longpress" withObject:event checkForListener:NO];
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
	[proxy fireEvent:@"swipe" withObject:event checkForListener:NO];
	[event release];

}

#pragma mark Touch Events


- (BOOL)interactionDefault
{
	return YES;
}

- (BOOL)interactionEnabled
{
	return self.userInteractionEnabled && _customUserInteractionEnabled;
}

- (BOOL)hasTouchableListener
{
	return handlesTouches;
}

-(UIView*)viewForHitTest
{
    return self;
}

- (UIView *)hitTest:(CGPoint) point withEvent:(UIEvent *)event
{
	BOOL hasTouchListeners = [self hasTouchableListener];
	UIView *hitView = [super hitTest:point withEvent:event];
	// if we don't have any touch listeners, see if interaction should
	// be handled at all.. NOTE: we don't turn off the views interactionEnabled
	// property since we need special handling ourselves and if we turn it off
	// on the view, we'd never get this event
	if ((touchPassThrough || (hasTouchListeners == NO && _touchEnabled==NO)))
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

-(BOOL) enabledForBgState
{
    return [self interactionEnabled];
}

-(void)setViewState:(UIControlState)state
{
    BOOL needsUpdate = viewState != state;
    viewState = state;
    if (needsUpdate)
    {
        [self setBgState:UIControlStateNormal];
    }
}

-(UIControlState)realStateForState:(UIControlState)state
{
    if ([self enabledForBgState]) {
        if (viewState != -1)
            state = viewState;
        return state;
    }
    return UIControlStateDisabled;
}

-(void)touchSetHighlighted:(BOOL)highlighted
{
    [self setHighlighted:highlighted];
}

-(void)handleTouchEvent:(NSString*)event forTouch:(UITouch*)touch
{
    if ([self interactionEnabled])
	{
		if ([proxy _hasListeners:event])
		{
            NSDictionary *evt = [TiUtils dictionaryFromTouch:touch inView:self];
			[proxy fireEvent:event withObject:evt checkForListener:NO];
//			[self handleControlEvents:UIControlEventTouchDown];
		}
	}
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
    
    if (_shouldHandleSelection) {
        [self touchSetHighlighted:YES];
    }
	[self handleTouchEvent:@"touchstart" forTouch:[touches anyObject]];
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
        [self touchSetHighlighted:!outside];
    }
	[self handleTouchEvent:@"touchmove" forTouch:touch];
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
        [self touchSetHighlighted:NO];
    }
	if ([self interactionEnabled])
	{
		UITouch *touch = [touches anyObject];
        BOOL hasTouchEnd = [proxy _hasListeners:@"touchend"];
        BOOL hasDblclick = [proxy _hasListeners:@"dblclick"];
        BOOL hasClick = [proxy _hasListeners:@"click"];
		if (hasTouchEnd || hasDblclick || hasClick)
		{
            NSDictionary *evt = [TiUtils dictionaryFromTouch:touch inView:self];
            if (hasTouchEnd) {
                [proxy fireEvent:@"touchend" withObject:evt checkForListener:NO];
                [self handleControlEvents:UIControlEventTouchCancel];
            }
            
            // Click handling is special; don't propagate if we have a delegate,
            // but DO invoke the touch delegate.
            // clicks should also be handled by any control the view is embedded in.
            if (hasDblclick && [touch tapCount] == 2) {
                [proxy fireEvent:@"dblclick" withObject:evt checkForListener:NO];
                return;
            }
            if (hasClick)
            {
                CGPoint localPoint = [touch locationInView:self];
                BOOL outside = (localPoint.x < -kTOUCH_MAX_DIST || (localPoint.x - self.frame.size.width)  > kTOUCH_MAX_DIST ||
                                localPoint.y < -kTOUCH_MAX_DIST || (localPoint.y - self.frame.size.height)  > kTOUCH_MAX_DIST);
                if (!outside && touchDelegate == nil) {
                    [proxy fireEvent:@"click" withObject:evt checkForListener:NO];
                    return;
                } 
            }
		}
	}
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event 
{
    [self setBgState:UIControlStateNormal];
    if ([[event touchesForView:self] count] > 0 || [self touchedContentViewWithEvent:event]) {
        [self processTouchesCancelled:touches withEvent:event];
    }
    [super touchesCancelled:touches withEvent:event];
}

- (void)processTouchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
    if (_shouldHandleSelection) {
        [self touchSetHighlighted:NO];
    }
	[self handleTouchEvent:@"touchcancel" forTouch:[touches anyObject]];
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
        [self updateTouchHandling];
	}
}

-(void)listenerRemoved:(NSString*)event count:(int)count
{
	if (count == 0)
	{
		[self handleListenerRemovedWithEvent:event];
        [self updateTouchHandling];
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
        if (self.layer.mask == nil || [self.layer.mask isKindOfClass:[CAShapeLayer class]]) {
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

-(void)setHighlighted:(BOOL)isHiglighted
{
    [self setBgState:isHiglighted?UIControlStateHighlighted:UIControlStateNormal];
    if (!_dispatchPressed) return;
	for (TiUIView * thisView in [self childViews])
	{
        if ([thisView.subviews count] > 0) {
            id firstChild = [thisView.subviews objectAtIndex:0];
            if ([firstChild isKindOfClass:[UIControl class]] && [firstChild respondsToSelector:@selector(setHighlighted:)])
            {
                [firstChild setHighlighted:isHiglighted];//swizzle will call setHighlighted on the view
            }
            else {
                [(id)thisView setHighlighted:isHiglighted];
            }
        }
        else {
			[(id)thisView setHighlighted:isHiglighted];
		}
	}
}

-(void)setSelected:(BOOL)isSelected
{
    [self setBgState:isSelected?UIControlStateSelected:UIControlStateNormal];
    if (!_dispatchPressed) return;
	for (TiUIView * thisView in [self childViews])
	{
        if ([thisView.subviews count] > 0) {
            id firstChild = [thisView.subviews objectAtIndex:0];
            if ([firstChild isKindOfClass:[UIControl class]] && [firstChild respondsToSelector:@selector(setSelected:)])
            {
                [firstChild setSelected:isSelected]; //swizzle will call setSelected on the view
            }
            else {
                [(id)thisView setSelected:isSelected];
            }
        }
        else {
			[(id)thisView setSelected:isSelected];
		}
	}
}

- (void)transitionfromView:(TiUIView *)viewOut toView:(TiUIView *)viewIn withTransition:(TiTransition *)transition completionBlock:(void (^)(void))block{
    [(TiAnimatableProxy*)viewOut.proxy addRunningAnimation:transition];
    [(TiAnimatableProxy*)viewIn.proxy addRunningAnimation:transition];
    
    [TiTransitionHelper transitionfromView:viewOut toView:viewIn insideView:self withTransition:transition completionBlock:^{
        [(TiAnimatableProxy*)viewOut.proxy removeRunningAnimation:transition];
        [(TiAnimatableProxy*)viewIn.proxy removeRunningAnimation:transition];
        if (block != nil) {
            block();
        }
    }];
}

- (void)blurBackground:(id)args
{
    //get the visible rect
    CGRect visibleRect = [self.superview convertRect:self.frame toView:self];
    if (CGRectIsEmpty(visibleRect)) return;
    visibleRect.origin.y += self.frame.origin.y;
    visibleRect.origin.x += self.frame.origin.x;
    
    //hide all the blurred views from the superview before taking a screenshot
    CGFloat alpha = self.alpha;
    CGFloat superviewAlpha = self.superview.alpha;
    self.alpha = 0.0f;
    if (superviewAlpha == 0) {
        self.superview.alpha = 1.0f;
    }
    //Render the layer in the image context
    //Render the layer in the image context
    UIGraphicsBeginImageContextWithOptions(visibleRect.size, NO, 1.0);
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextTranslateCTM(context, -visibleRect.origin.x, -visibleRect.origin.y);
    CALayer *layer = self.superview.layer;
    [layer renderInContext:context];
    
    //show all the blurred views from the superview before taking a screenshot
    self.alpha = alpha;
    self.superview.alpha = superviewAlpha;
   
    __block UIImage *image = [UIGraphicsGetImageFromCurrentImageContext() retain];
    UIGraphicsEndImageContext();
    
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_LOW, 0), ^{
        NSArray* properties;
        id firstArg = [args objectAtIndex:0];
        if ([firstArg isKindOfClass:[NSArray class]]) {
            properties = firstArg;
        }
        else {
            properties = [NSArray arrayWithObject:[TiUtils stringValue:firstArg]];
        }
        NSDictionary* options;
        ENSURE_ARG_AT_INDEX(options, args, 1, NSDictionary)
        [options setValue:[NSArray arrayWithObject:[NSNumber numberWithInt:TiImageHelperFilterBoxBlur]] forKey:@"filters"];
        UIImage* result = [[TiImageHelper imageFiltered:[image autorelease] withOptions:options] retain];
        if ([options objectForKey:@"callback"]) {
            id callback = [options objectForKey:@"callback"];
            ENSURE_TYPE(callback, KrollCallback)
            if (callback != nil) {
                TiThreadPerformOnMainThread(^{
                    TiBlob* blob = [[TiBlob alloc] initWithImage:result];
                    NSDictionary *event = [NSDictionary dictionaryWithObject:blob forKey:@"image"];
                    [self.proxy _fireEventToListener:@"blurBackground" withObject:event listener:callback thisObject:nil];

                    [blob release];
                }, NO);
            }
        }
            for (NSString* property in properties) {
                [self.proxy setValue:result forKey:property];
            }
        [result release];
    });
}

@end
