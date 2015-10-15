/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiBase.h"
#import "TiUIView.h"
#import "TiColor.h"
#import "TiRect.h"
#import "TiUtils.h"
#import "ImageLoader.h"
#import "Ti2DMatrix.h"
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

#import <objc/runtime.h>
#import "UIGestureRecognizer+Ti.h"

#import "DirectionPanGestureRecognizer.h"

static NSString * const kTiViewShapeMaskKey = @"kTiViewShapeMask";


@interface TiViewProxy()
-(UIViewController*)getContentController;
@end

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
    CGFloat bottomInset = 0;
    if (keyboardTop != 0) {
        CGRect scrollVisibleRect = [scrollView convertRect:[scrollView bounds] toView:[[TiApp app] viewForKeyboardAccessory]];
        //First, find out how much we have to compensate.
        
        CGFloat obscuredHeight = scrollVisibleRect.origin.y + scrollVisibleRect.size.height - keyboardTop;
        //ObscuredHeight is how many vertical pixels the keyboard obscures of the scroll view. Some of this may be acceptable.
        
        CGFloat unimportantArea = MAX(scrollVisibleRect.size.height - minimumContentHeight,0);
        //It's possible that some of the covered area doesn't matter. If it all matters, unimportant is 0.
        
        //As such, obscuredHeight is now how much actually matters of scrollVisibleRect.
        
        bottomInset = MAX(0,obscuredHeight-unimportantArea);
        
    }
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
    if (keyboardTop == 0) return;
	VerboseLog(@"ScrollView:%@, keyboardTop:%f minimumContentHeight:%f responderRect:(%f,%f),%fx%f;",
			scrollView,keyboardTop,minimumContentHeight,
			responderRect.origin.x,responderRect.origin.y,responderRect.size.width,responderRect.size.height);

	CGRect scrollVisibleRect = [scrollView convertRect:[scrollView bounds] toView:[[TiApp app] viewForKeyboardAccessory]];
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
    CGPoint currentOffset = scrollView.contentOffset;
    if (!CGPointEqualToPoint(currentOffset, offsetPoint)) {
        [scrollView setContentOffset:offsetPoint animated:YES];
    }
}

void ModifyScrollViewForKeyboardHeightAndContentHeightWithResponderRect(UIScrollView * scrollView,CGFloat keyboardTop,CGFloat minimumContentHeight,CGRect responderRect)
{
	VerboseLog(@"ScrollView:%@, keyboardTop:%f minimumContentHeight:%f responderRect:(%f,%f),%fx%f;",
			scrollView,keyboardTop,minimumContentHeight,
			responderRect.origin.x,responderRect.origin.y,responderRect.size.width,responderRect.size.height);

	CGRect scrollVisibleRect = [scrollView convertRect:[scrollView bounds] toView:[[TiApp app] viewForKeyboardAccessory]];
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


@interface TiUIView () {
    TiSelectableBackgroundLayer* _bgLayer;
    UntouchableView* _childrenHolder;
    TiBorderLayer* _borderLayer;
    BOOL _shouldHandleSelection;
    BOOL _customUserInteractionEnabled;
    BOOL _propagateParentEnabled;
    BOOL _setEnabledFromParent;
    BOOL _dispatchPressed;
    
    BOOL needsToSetBackgroundImage;
	BOOL needsToSetBackgroundSelectedImage;
	BOOL needsToSetBackgroundDisabledImage;
    BOOL needsUpdateBackgroundImageFrame;
    UIEdgeInsets _backgroundPadding;
    UIEdgeInsets _borderPadding;
    CGFloat* radii;
    BOOL usePathAsBorder;
    BOOL _nonRetina;
    BOOL _selected;
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

@synthesize proxy,touchDelegate,oldSize, backgroundLayer = _bgLayer, borderLayer = _borderLayer, shouldHandleSelection = _shouldHandleSelection, animateBgdTransition, runningAnimation;

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
    [panRecognizer release];
    [shoveRecognizer release];
    [rotationRecognizer release];
	[runningAnimation release];
    [proxy setModelDelegate:nil];
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
    if (proxy != nil && [[self viewProxy] view] == self)
    {
        [[self viewProxy] detachView];
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

-(TiViewProxy*)viewProxy {
    return (TiViewProxy*)proxy;
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
    configurationSet = NO;
    _alwaysUseBackgroundLayer = NO;
    childViews  =[[NSMutableArray alloc] init];
    transferLock = [[NSRecursiveLock alloc] init];
    touchPassThrough = NO;
    _shouldHandleSelection = YES;
    self.clipsToBounds = self.layer.masksToBounds = clipChildren = YES;
    self.userInteractionEnabled = YES;
    self.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
//    self.layer.shouldRasterize = YES;
    self.layer.rasterizationScale = [[UIScreen mainScreen] scale];
    backgroundOpacity = 1.0f;
    _customUserInteractionEnabled = YES;
    _canKeepBackgroundColor = NO;
    _dispatchPressed = NO;
    animateBgdTransition = NO;
    _backgroundPadding = _borderPadding = UIEdgeInsetsZero;
    viewState = -1;
    radii = NULL;
    usePathAsBorder = NO;
    _propagateParentEnabled = YES;
    _setEnabledFromParent = YES;
    _nonRetina = NO;
    _tintColorImage = NO;
    _selected = NO;
}


//- (id) init
//{
//	self = [super init];
//	if (self != nil)
//	{
//	}
//	return self;
//}

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
    if ([[self viewProxy] _hasListenersIgnoreBubble:@"swipe"]) {
        [[self gestureRecognizerForEvent:@"uswipe"] setEnabled:YES];
        [[self gestureRecognizerForEvent:@"dswipe"] setEnabled:YES];
        [[self gestureRecognizerForEvent:@"rswipe"] setEnabled:YES];
        [[self gestureRecognizerForEvent:@"lswipe"] setEnabled:YES];
    }
    if ([[self viewProxy] _hasListenersIgnoreBubble:@"pinch"]) {
         [[self gestureRecognizerForEvent:@"pinch"] setEnabled:YES];
    }
    if ([[self viewProxy] _hasListenersIgnoreBubble:@"longpress"]) {
        [[self gestureRecognizerForEvent:@"longpress"] setEnabled:YES];
    }
    if ([[self viewProxy] _hasListenersIgnoreBubble:@"pan"]) {
        [[self gestureRecognizerForEvent:@"pan"] setEnabled:YES];
    }
    if ([[self viewProxy] _hasListenersIgnoreBubble:@"rotate"]) {
        [[self gestureRecognizerForEvent:@"rotate"] setEnabled:YES];
    }
    if ([[self viewProxy] _hasListenersIgnoreBubble:@"shove"]) {
        [[self gestureRecognizerForEvent:@"shove"] setEnabled:YES];
    }
}

-(NSArray*)gestureListenersArray {
    static NSArray* gestureListenersArray = nil;
    if (gestureListenersArray == nil) {
        gestureListenersArray = [[NSArray alloc] initWithObjects:@"singletap", @"doubletap", @"twofingertap", @"swipe", @"pinch", @"longpress", @"pan", @"rotate", @"shove", nil];
    }
    return gestureListenersArray;
}

-(BOOL)proxyHasTapListener
{
    static NSArray* tapListeners = nil;
    if (tapListeners == nil) {
        tapListeners = [[NSArray alloc] initWithObjects:@"singletap", @"doubletap", @"twofingertap", nil];
    }
	return [proxy _hasAnyListeners:tapListeners];
}

-(BOOL)proxyHasTouchListener
{
    static NSArray* touchListeners = nil;
    if (touchListeners == nil) {
        touchListeners = [[NSArray alloc] initWithObjects:@"touchstart", @"touchcancel", @"touchend", @"touchmove", @"click", @"dblclick", nil];
    }
	return [proxy _hasAnyListeners:touchListeners];
}

-(BOOL) proxyHasGestureListeners
{
    static NSArray* gestureListeners = nil;
    if (gestureListeners == nil) {
        gestureListeners = [[NSArray alloc] initWithObjects:@"swipe", @"pinch", @"longpress", @"pan", @"shove", @"rotate", nil];
    }
	return [proxy _hasAnyListeners:gestureListeners];
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
	 
	super.backgroundColor = [UIColor clearColor]; //carefull it seems that nil is different from clear :s
	self.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
}

-(void)configurationStart
{
    configurationSet = needsToSetBackgroundImage = needsToSetBackgroundDisabledImage = needsToSetBackgroundSelectedImage = NO;
    if (_bgLayer) {
        _bgLayer.readyToCreateDrawables = configurationSet;
    }
    if (_borderLayer) {
        _borderLayer.readyToCreateDrawables = configurationSet;
    }
}

-(BOOL)isConfigurationSet
{
    return configurationSet;
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
        _bgLayer.readyToCreateDrawables = configurationSet;
    }
    if (_borderLayer) {
        _borderLayer.readyToCreateDrawables = configurationSet;
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
            [layer.mask addAnimation:pathAnimation forKey:@"clippingPath"];
        }
        ((CAShapeLayer*)layer.mask).path = path;
    }
}

-(void)applyPathToLayersShadow:(CALayer*)layer path:(CGPathRef)path
{
    if (layer == nil) return;
    if (path == nil) {
        layer.shadowPath = nil;
    }
    else {
        if (runningAnimation) {
            CABasicAnimation *pathAnimation = [CABasicAnimation animationWithKeyPath:@"shadowPath"];
            pathAnimation.fromValue = (id)layer.shadowPath;
            pathAnimation.duration = [runningAnimation duration];
            pathAnimation.timingFunction = [runningAnimation curve];
            pathAnimation.fillMode = kCAFillModeBoth;
            pathAnimation.toValue = (id)path;
            [layer addAnimation:pathAnimation forKey:@"shadowPath"];
        }
        layer.shadowPath = path;
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
    CGPathRef path =  CGPathCreateRoundiiRect(bounds, radii);
    if (runningAnimation) {
        CABasicAnimation *pathAnimation = [CABasicAnimation animationWithKeyPath:@"shadowPath"];
        pathAnimation.fromValue = (id)self.layer.shadowPath;
        pathAnimation.duration = [runningAnimation duration];
        pathAnimation.timingFunction = [runningAnimation curve];
        pathAnimation.fillMode = kCAFillModeBoth;
        pathAnimation.toValue = (id)path;
        [self.layer addAnimation:pathAnimation forKey:@"shadowPath"];
    }
    self.layer.shadowPath = path;
    if (clipChildren && usePathAsBorder && (!self.layer.mask || [self.layer.mask isKindOfClass:[CAShapeLayer class]]))
    {
        [self applyPathToLayersMask:self.layer path:path];
        if (_bgLayer)
        {
            [self applyPathToLayersShadow:_bgLayer path:path];
        }
        
    }
    else if (!usePathAsBorder) {
        CGFloat radius = radii[0];
        self.layer.cornerRadius = radius;
        if (_bgLayer) _bgLayer.cornerRadius = radius;
    }
    else {
        if (_bgLayer)
        {
            [self applyPathToLayersShadow:_bgLayer path:path];
        }
    }
    

    CGPathRelease(path);
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
    if (![[self viewProxy] viewLayedOut]) return;
    if (radii != NULL)
    {
        [self updatePathForClipping:bounds];
    }
    if (_borderLayer) {
        [_borderLayer setFrame:[self backgroundWrapperView].bounds withinAnimation:runningAnimation];
    }
    if (_bgLayer) {
        _bgLayer.frame = UIEdgeInsetsInsetRect([self backgroundWrapperView].bounds, _backgroundPadding);
    }
    if (self.layer.mask != nil) {
        [self.layer.mask setFrame:bounds];
    }
    [self updateTransform];
    TiProxy* maskProxy = [[self viewProxy] holdedProxyForKey:kTiViewShapeMaskKey];
    if (maskProxy) {
        if (IS_OF_CLASS(maskProxy, TiViewProxy)) {
            [(TiViewProxy*)maskProxy setSandboxBounds:bounds];
            [(TiViewProxy*)maskProxy refreshView];
        }
    }
}


-(void)setFrame:(CGRect)frame
{
    
#ifdef TI_USE_AUTOLAYOUT
        [super setFrame:frame];
#else
   if ([[self viewProxy] canBeResizedByFrame]) {
        [super setFrame:frame];
        
        [self checkBounds];
        //        [[self viewProxy] performBlock:^{
        //            [[self viewProxy] performBlockWithoutLayout:^{
        //                [[self viewProxy] willChangeSize];
        ////                [self willChangePosition];
        //            }];
        //
        //            [[self viewProxy] refreshViewOrParent];
        //        } withinAnimation:runningAnimation];
//        [[self viewProxy] repositionWithinAnimation];
    }
//    if (![self viewProxy].canBeResizedByFrame) return;
	// this happens when a view is added to another view but not
	// through the framework (such as a tableview header) and it
	// means we need to force the layout of our children
//	if (childrenInitialized==NO &&
//		CGRectIsEmpty(frame)==NO &&
//		[self.proxy isKindOfClass:[TiViewProxy class]])
//	{
//		childrenInitialized=YES;
//		[(TiViewProxy*)self.proxy layoutChildren:NO];
//    } else if (CGRectIsEmpty(frame)) {
//        childrenInitialized=NO;
//    }
#endif
}

-(void)updateBounds:(CGRect)newBounds
{
    //TIMOB-11197, TC-1264
    [CATransaction begin];
    
    if (self.runningAnimation == nil) {
        [CATransaction setValue:(id)kCFBooleanTrue forKey:kCATransactionDisableActions];
    }
    else {
        [CATransaction setAnimationDuration:[self.runningAnimation duration]];
        [CATransaction setAnimationTimingFunction:[self.runningAnimation curve]];
    }
    [self frameSizeChanged:[TiUtils viewPositionRect:self] bounds:newBounds];
//    if ([[self viewProxy] canBeResizedByFrame]) {
//        [[self viewProxy] refreshViewIfNeeded];
//    }
    [CATransaction commit];
}


-(void)checkBounds
{
#ifndef TI_USE_AUTOLAYOUT
    CGRect newBounds = [self bounds];
    if(!CGSizeEqualToSize(oldSize, newBounds.size)) {
        //make sure to change old first to prevent setBounds in relayout to call us again
        oldSize = newBounds.size;
        if ([[self viewProxy] canBeResizedByFrame]) {
            [[self viewProxy] performBlockWithoutLayout:^{
                [[self viewProxy] willChangeSize];
            }];
        }
        [self updateBounds:newBounds];
    }
#endif
}



-(void)setBounds:(CGRect)bounds
{
	[super setBounds:bounds];
	[self checkBounds];
}

-(void)layoutSubviews
{
//	[self checkBounds];
#ifndef TI_USE_AUTOLAYOUT

    if ([[self viewProxy] canBeResizedByFrame]) {
//        [[self viewProxy] performBlock:^{
//            [[self viewProxy] performBlockWithoutLayout:^{
//                [[self viewProxy] willChangeSize];
////                [self willChangePosition];
//            }];
//            
//            [[self viewProxy] refreshViewOrParent];
//        } withinAnimation:runningAnimation];
        [[self viewProxy] refreshViewIfNeeded];
    }
#endif
    [super layoutSubviews];
}


- (void)didMoveToSuperview
{
	[self updateTransform];
	[super didMoveToSuperview];
    if ([self.superview isKindOfClass:[TiUIView class]])
    {
        BOOL parentEnabled = [(TiUIView*)self.superview customUserInteractionEnabled];
        if (!parentEnabled && _customUserInteractionEnabled) {
            [self setEnabled_:@(parentEnabled && _customUserInteractionEnabled)];
        }
    }
}

-(void)updateTransform
{
	if ([transformMatrix isKindOfClass:[Ti2DMatrix class]] && self.superview != nil)
	{
        CGSize size = self.bounds.size;
        CGSize parentSize = self.superview.bounds.size;
		self.transform = [(Ti2DMatrix*)transformMatrix matrixInViewSize:size andParentSize:parentSize];
		return;
	}
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

- (NSComparisonResult)compare:(TiUIView *)otherView {
    NSInteger val1 = ((TiViewProxy*)self.proxy).vzIndex;
    NSInteger val2 = ((TiViewProxy*)otherView.proxy).vzIndex;
    if (val1 < val2) {
        return NSOrderedAscending;
    } else if(val1 > val2) {
        return NSOrderedDescending;
    }
    return NSOrderedSame;
}

#pragma mark Public APIs

-(void)setTintColor_:(id)color
{
    TiColor *ticolor = [TiUtils colorValue:color];
    [self setTintColor:[ticolor _color]];
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
    if (_nonRetina){
        [_bgLayer setNonRetina:_nonRetina];
    }
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
    if (usePathAsBorder) {
        [_borderLayer swithToContentBorder];
    }
    else {
        _borderLayer.cornerRadius = self.layer.cornerRadius;
    }
    [_borderLayer setRadii:radii];
    [_borderLayer setBorderPadding:_borderPadding];
    [[[self backgroundWrapperView] layer] addSublayer:_borderLayer];
    CGRect bounds = [[self backgroundWrapperView] layer].bounds;
    if (!CGRectIsEmpty(bounds)) {
        _borderLayer.frame = bounds;
    }
    if (_nonRetina){
        [_borderLayer setNonRetina:_nonRetina];
    }
    _borderLayer.readyToCreateDrawables = configurationSet;
    _borderLayer.opacity = backgroundOpacity;
    return _borderLayer;
}


-(void)setBackgroundGradient:(TiGradient*)gradient forState:(UIControlState)state
{
    if (!gradient) {
        [_bgLayer setGradient:gradient forState:state];
    }
    else {
        [[self getOrCreateCustomBackgroundLayer] setGradient:gradient forState:state];
    }
}

-(void)setBackgroundImage:(id)image forState:(UIControlState)state
{
    if (!image) {
        [_bgLayer setImage:image forState:state];
    }
    else {
        [[self getOrCreateCustomBackgroundLayer] setImage:image forState:state];
    }
}

-(void)setBackgroundColor:(UIColor*)color forState:(UIControlState)state
{
    if (!color) {
        [_bgLayer setColor:color forState:state];
    }
    else {
        if (!_canKeepBackgroundColor && self.backgroundColor)
        {
            [[self getOrCreateCustomBackgroundLayer] setColor:super.backgroundColor forState:UIControlStateNormal];
            super.backgroundColor = nil;
        }
        [[self getOrCreateCustomBackgroundLayer] setColor:color forState:state];
    }
}

-(void)setBackgroundInnerShadows:(NSArray*)shadow forState:(UIControlState)state
{
    if (!shadow) {
        [_bgLayer setInnerShadows:shadow forState:state];
    }
    else {
        [[self getOrCreateCustomBackgroundLayer] setInnerShadows:shadow forState:state];
    }
}

-(void) setBackgroundGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [self setBackgroundGradient:newGradient forState:UIControlStateNormal];
}

-(void) setBackgroundSelectedGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [self setBackgroundGradient:newGradient forState:UIControlStateHighlighted];
}

-(void) setBackgroundHighlightedGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [self setBackgroundGradient:newGradient forState:UIControlStateHighlighted];
}

-(void) setBackgroundDisabledGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [self setBackgroundGradient:newGradient forState:UIControlStateDisabled];
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
    
    if (!_alwaysUseBackgroundLayer && !_bgLayer && (clipChildren || radii == nil))
    {
        if (backgroundOpacity < 1.0f) {
            const CGFloat* components = CGColorGetComponents(uicolor.CGColor);
            float alpha = CGColorGetAlpha(uicolor.CGColor) * backgroundOpacity;
            uicolor = [uicolor colorWithAlphaComponent:alpha];
        }
        super.backgroundColor = uicolor;
    }
    else
    {
        [self setBackgroundColor:[TiUtils colorValue:color].color forState:UIControlStateNormal];
    }
}

-(void) setBackgroundSelectedColor_:(id)color
{
    [self setBackgroundColor:[TiUtils colorValue:color].color forState:UIControlStateHighlighted];
}

-(void) setBackgroundHighlightedColor_:(id)color
{
    [self setBackgroundColor:[TiUtils colorValue:color].color forState:UIControlStateHighlighted];
}

-(void) setBackgroundDisabledColor_:(id)color
{
    [self setBackgroundColor:[TiUtils colorValue:color].color forState:UIControlStateDisabled];
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
	if (TiCapIsUndefined(imageCap)) {
        return [TiUtils loadBackgroundImage:arg forProxy:proxy];
    }
    else {
        return [TiUtils loadBackgroundImage:arg forProxy:proxy withCap:imageCap];
    }
	return nil;
}
-(UIImage*)loadImage:(id)arg
{
    if (arg==nil) return nil;
    id result = nil;
    if (TiCapIsUndefined(imageCap)) {
        result =  [TiUtils loadBackgroundImage:arg forProxy:proxy];
    }
    else {
        result =  [TiUtils loadBackgroundImage:arg forProxy:proxy withCap:imageCap];
    }
    if ([result isKindOfClass:[UIImage class]]) return result;
    else if ([result isKindOfClass:[TiSVGImage class]]) return [((TiSVGImage*)result) fullImage];
	return nil;
}

-(void) setBackgroundImage_:(id)arg
{
    if (!configurationSet) {
        needsToSetBackgroundImage = YES;
        return;
    }
    id image = [self loadImageOrSVG:arg];
    [self setBackgroundImage:image forState:UIControlStateNormal];
}

-(void) setBackgroundSelectedImage_:(id)arg
{
    if (!configurationSet) {
        needsToSetBackgroundSelectedImage = YES;
        return;
    }
    id image = [self loadImageOrSVG:arg];
    [self setBackgroundImage:image forState:UIControlStateHighlighted];
}

-(void) setBackgroundHighlightedImage_:(id)arg
{
    if (!configurationSet) {
        needsToSetBackgroundSelectedImage = YES;
        return;
    }
    id image = [self loadImageOrSVG:arg];
    [self setBackgroundImage:image forState:UIControlStateHighlighted];
}

-(void) setBackgroundDisabledImage_:(id)arg
{
    if (!configurationSet) {
        needsToSetBackgroundDisabledImage = YES;
        return;
    }
    id image = [self loadImageOrSVG:arg];
    [self setBackgroundImage:image forState:UIControlStateDisabled];
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
    
    [self setBackgroundInnerShadows:result forState:UIControlStateNormal];
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
    
    [self setBackgroundInnerShadows:result forState:UIControlStateHighlighted];
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
    [self setBackgroundInnerShadows:result forState:UIControlStateHighlighted];
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
    
    [self setBackgroundInnerShadows:result forState:UIControlStateDisabled];
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
    if (backgroundOpacity != 1 && self.backgroundColor)
    {
        [[self getOrCreateCustomBackgroundLayer] setColor:super.backgroundColor forState:UIControlStateNormal];
        super.backgroundColor = nil;
    }
    if (_bgLayer) {
        _bgLayer.opacity = backgroundOpacity;
    }
    if (_borderLayer) {
        _borderLayer.opacity = backgroundOpacity;
    }
}

-(void)setBackgroundPadding_:(id)value
{
    _backgroundPadding = [TiUtils insetValue:value];
    if (_bgLayer) {
        _bgLayer.frame = UIEdgeInsetsInsetRect([self backgroundWrapperView].bounds, _backgroundPadding);
    }
}


-(void)setImageCap_:(id)arg
{
    imageCap = [TiUtils capValue:arg def:TiCapUndefined];
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
        }
    }
    else if(!usePathAsBorder)
    {
        CGFloat radius = radii[0];
        self.layer.cornerRadius = radius;
        if (_bgLayer) _bgLayer.cornerRadius = radius;
    }
}

-(void)setBorderRadius_:(id)value
{
    if ([value isKindOfClass:[NSArray class]]) {
        radii =(CGFloat*)malloc(8*sizeof(CGFloat));
        NSArray* array = (NSArray*)value;
        NSUInteger count = [array count];
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


-(void)setBorderGradient:(TiGradient*)gradient forState:(UIControlState)state
{
    if (!gradient) {
        [_borderLayer setGradient:gradient forState:state];
    }
    else {
        [[self getOrCreateBorderLayer] setGradient:gradient forState:state];
    }
}

-(void)setBorderImage:(id)image forState:(UIControlState)state
{
    if (!image) {
        [_borderLayer setImage:image forState:state];
    }
    else {
        [[self getOrCreateBorderLayer] setImage:image forState:state];
    }
}

-(void)setBorderColor:(UIColor*)color forState:(UIControlState)state
{
    if (!color) {
        [_borderLayer setColor:color forState:state];
    }
    else {
        [[self getOrCreateBorderLayer] setColor:color forState:state];
    }
}

-(void)setBorderInnerShadows:(NSArray*)shadow forState:(UIControlState)state
{
    if (!shadow) {
        [_borderLayer setInnerShadows:shadow forState:state];
    }
    else {
        [[self getOrCreateBorderLayer] setInnerShadows:shadow forState:state];
    }
}


-(void)setBorderColor_:(id)color
{
    UIColor* uiColor = [TiUtils colorValue:color].color;
    [self setBorderColor:uiColor forState:UIControlStateNormal];
}

-(void) setBorderSelectedColor_:(id)color
{
    UIColor* uiColor = [TiUtils colorValue:color].color;
    [self setBorderColor:uiColor forState:UIControlStateHighlighted];
}

-(void) setBorderHighlightedColor_:(id)color
{
    UIColor* uiColor = [TiUtils colorValue:color].color;
    [self setBorderColor:uiColor forState:UIControlStateHighlighted];
}

-(void) setBorderDisabledColor_:(id)color
{
    UIColor* uiColor = [TiUtils colorValue:color].color;
    [self setBorderColor:uiColor forState:UIControlStateDisabled];
}

-(void) setBorderGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [self setBorderGradient:newGradient forState:UIControlStateNormal];
}

-(void) setBorderSelectedGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [self setBorderGradient:newGradient forState:UIControlStateHighlighted];
}

-(void) setBorderHighlightedGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [self setBorderGradient:newGradient forState:UIControlStateHighlighted];
}

-(void) setBorderDisabledGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [self setBorderGradient:newGradient forState:UIControlStateDisabled];
}

-(void)setBorderWidth_:(id)w
{
	[[self getOrCreateBorderLayer] setClipWidth:TiDimensionCalculateValueFromString([TiUtils stringValue:w])];
}

-(void)setBorderPadding_:(id)value
{
    _borderPadding = [TiUtils insetValue:value];
    if (_borderLayer) {
        [_borderLayer setBorderPadding:_borderPadding];
    }
}

-(void)setRetina_:(id)value
{
    _nonRetina = ![TiUtils boolValue:value];
    if (_bgLayer) {
        [_bgLayer setNonRetina:_nonRetina];
    }
    if (_borderLayer) {
        [_borderLayer setNonRetina:_nonRetina];
    }
}


-(void)setAnchorPoint_:(id)point
{
	CGPoint anchorPoint = [TiUtils pointValue:point];
    [[self viewProxy] performLayoutBlockAndRefresh:^{
        //setting the anchorPoint will immediately call for a layout
        self.layer.anchorPoint = anchorPoint;
        [(TiViewProxy*)[self proxy] willChangePosition];
    }];
}

-(void)setTransform_:(id)transform_
{
	RELEASE_TO_NIL(transformMatrix);
#if defined(USE_TI_UIIOS3DMATRIX) || defined(USE_TI_UI3DMATRIX)
	if ([transform_ isKindOfClass:[Ti3DMatrix class]]) {
        transformMatrix = [transform_ retain];
    }
    else 
#endif
    transformMatrix = [[TiUtils matrixValue:transform_] retain];
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
    
    TiViewProxy* viewProxy = (TiViewProxy*)[self proxy];
	if(viewProxy.parentVisible)
	{
		if (newVal)
		{
			[viewProxy willHide];
		}
		else
		{
            [viewProxy performBlockWithoutLayout:^{
                [viewProxy willShow];
            }];
            if (configurationSet) {
                [viewProxy refreshViewOrParent];

            }
        }
	}
}

-(void)setAnimatedTransition:(BOOL)animated
{
    if (_bgLayer != nil) {
        [_bgLayer setAnimateTransition:animated];
    }
    if (_borderLayer != nil) {
        [_borderLayer setAnimateTransition:animated];
    }
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
	self.userInteractionEnabled  = [TiUtils boolValue:arg def:self.userInteractionEnabled];
    changedInteraction = YES;
}

-(BOOL)customUserInteractionEnabled {
    return _customUserInteractionEnabled;
}

-(void)setPropagateParentEnabled_:(id)arg
{
	_propagateParentEnabled  = [TiUtils boolValue:arg def:YES];
}

-(void)setEnabledFromParent_:(id)arg
{
	_setEnabledFromParent  = [TiUtils boolValue:arg def:YES];
}

-(void)setCustomUserInteractionEnabled:(BOOL)value
{
    _customUserInteractionEnabled = value;
}

-(void)setEnabled:(id)arg calledFromParent:(BOOL)calledFromParent
{
    BOOL newValue = [TiUtils boolValue:arg def:[self interactionDefault]];
    if (!calledFromParent || _setEnabledFromParent) {
        if (newValue != _customUserInteractionEnabled) {
            [self setCustomUserInteractionEnabled:newValue];
            [self setBgState:UIControlStateNormal];
            changedInteraction = YES;
        }
    }
    if (changedInteraction || (calledFromParent && _propagateParentEnabled)) {
        for (TiUIView * thisView in [self childViews])
        {
            if ([thisView isKindOfClass:[TiUIView class]])
            {
                BOOL originalValue = [[((TiUIView*)thisView).proxy valueForUndefinedKey:@"enabled"] boolValue];
                [thisView setEnabled:arg calledFromParent:YES];
            }
        }
    }
    
}

-(void)setEnabled_:(id)arg
{
    [self setEnabled:arg calledFromParent:NO];
    [self setBgState:UIControlStateNormal];
}

-(void)setSelected_:(id)arg
{
    _selected = [TiUtils boolValue:arg def:NO];
    [self setViewState:_selected?UIControlStateHighlighted:-1];
}

-(void)setDispatchPressed_:(id)arg
{
	_dispatchPressed = [TiUtils boolValue:arg def:_dispatchPressed];
}

-(id)dispatchPressed_
{
	return @(_dispatchPressed);
}

-(id) touchEnabled_ {
	return @(self.userInteractionEnabled);
}

-(void)setTouchPassThrough_:(id)arg
{
	touchPassThrough = [TiUtils boolValue:arg];
}


-(void)setExclusiveTouch_:(id)arg
{
	self.exclusiveTouch = [TiUtils boolValue:arg];
    [self viewForHitTest].exclusiveTouch = self.exclusiveTouch;
}


-(id)touchPassThrough_ {
    return @(touchPassThrough);
}

-(UIView *)backgroundWrapperView
{
	return self;
}


-(void)setClipChildren_:(id)arg
{
    clipChildren = [TiUtils boolValue:arg];
    self.clipsToBounds = [self clipChildren];
    if ([self parentViewForChildren] != self) {
        [self parentViewForChildren].clipsToBounds = self.clipsToBounds;
    }
}

-(void)setMasksToBounds_:(id)arg
{
    self.layer.masksToBounds = [TiUtils boolValue:arg];
    if ([self parentViewForChildren] != self) {
        [self parentViewForChildren].layer.masksToBounds = self.layer.masksToBounds;
    }
}


-(void)setRasterize_:(id)arg
{
    self.layer.shouldRasterize = [TiUtils boolValue:arg def:self.layer.shouldRasterize];
}

-(BOOL)clipChildren
{
    return clipChildren && ([[self shadowLayer] shadowOpacity] == 0);
}

-(id)clipChildren_
{
    return @([self clipChildren]);
}

-(void)setTintColorImage_:(id)arg
{
    _tintColorImage = [TiUtils boolValue:arg def:NO];
}


-(CALayer *)shadowLayer
{
	return [self layer];
}


-(void)setViewShadow_:(id)arg
{
    ENSURE_SINGLE_ARG(arg,NSDictionary);
    [TiUIHelper applyShadow:arg toLayer:[self shadowLayer]];
}

-(NSArray*) childViews
{
    return [NSArray arrayWithArray:childViews];
}

-(void)verifyZOrder
{
    [_bgLayer sendToBack];
    [_borderLayer bringToFront];
}

-(void)didAddSubview:(UIView*)view
{
    if ([view isKindOfClass:[TiUIView class]])
    {
        [childViews addObject:view];
    }
	// So, it turns out that adding a subview places it beneath the gradient layer.
	// Every time we add a new subview, we have to make sure the gradient stays where it belongs..
//    [self verifyZOrder];
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

//-(SEL)selectorForProperty:(NSString*)key
//{
//	NSString *method = [NSString stringWithFormat:@"set%@%@_:", [[key substringToIndex:1] uppercaseString], [key substringFromIndex:1]];
//	return NSSelectorFromString(method);
//}

-(void)readProxyValuesWithKeys:(id<NSFastEnumeration>)keys
{
	DoProxyDelegateReadValuesWithKeysFromProxy(self, keys, proxy);
}

-(void)propertyChanged:(NSString*)key oldValue:(id)oldValue newValue:(id)newValue proxy:(TiProxy*)proxy_
{
	DoProxyDelegateChangedValuesWithProxy(self, key, oldValue, newValue, proxy_);
}



- (void)detachViewProxy {
    if(!proxy) return;
    self.proxy = nil;
    for (UIView *subview in self.subviews) {
        if ([subview isKindOfClass:[TiUIView class]])
            [(TiUIView*)subview detachViewProxy];
    }
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


-(UIView*)viewForGestures
{
    return self;
}

-(UITapGestureRecognizer*)singleTapRecognizer
{
    if (singleTapRecognizer == nil) {
        singleTapRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedSingleTap:)];
        [self configureGestureRecognizer:singleTapRecognizer];
        [[self viewForGestures] addGestureRecognizer:singleTapRecognizer];
        
        if (doubleTapRecognizer != nil) {
            [singleTapRecognizer requireGestureRecognizerToFail:doubleTapRecognizer];
        }
        if (longPressRecognizer != nil) {
            [singleTapRecognizer requireGestureRecognizerToFail:longPressRecognizer];
        }
    }
    return singleTapRecognizer;
}

-(UITapGestureRecognizer*)doubleTapRecognizer
{
    if (doubleTapRecognizer == nil) {
        doubleTapRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedDoubleTap:)];
        [doubleTapRecognizer setNumberOfTapsRequired:2];
        [self configureGestureRecognizer:doubleTapRecognizer];
        [[self viewForGestures] addGestureRecognizer:doubleTapRecognizer];
        
        if (singleTapRecognizer != nil) {
            [singleTapRecognizer requireGestureRecognizerToFail:doubleTapRecognizer];
        }
        if (longPressRecognizer != nil) {
            [doubleTapRecognizer requireGestureRecognizerToFail:longPressRecognizer];
        }
    }
    return doubleTapRecognizer;
}

-(UITapGestureRecognizer*)twoFingerTapRecognizer
{
    if (twoFingerTapRecognizer == nil) {
        twoFingerTapRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedSingleTap:)];
        [twoFingerTapRecognizer setNumberOfTouchesRequired:2];
        [self configureGestureRecognizer:twoFingerTapRecognizer];
        [[self viewForGestures] addGestureRecognizer:twoFingerTapRecognizer];
    }
    return twoFingerTapRecognizer;
}

-(UIPinchGestureRecognizer*)pinchRecognizer
{
    if (pinchRecognizer == nil) {
        pinchRecognizer = [[UIPinchGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedPinch:)];
        [self configureGestureRecognizer:pinchRecognizer];
        [[self viewForGestures] addGestureRecognizer:pinchRecognizer];
    }
    return pinchRecognizer;
}

-(UISwipeGestureRecognizer*)leftSwipeRecognizer
{
    if (leftSwipeRecognizer == nil) {
        leftSwipeRecognizer = [[UISwipeGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedSwipe:)];
        [leftSwipeRecognizer setDirection:UISwipeGestureRecognizerDirectionLeft];
        [self configureGestureRecognizer:leftSwipeRecognizer];
        [[self viewForGestures] addGestureRecognizer:leftSwipeRecognizer];
    }
    return leftSwipeRecognizer;
}

-(UISwipeGestureRecognizer*)rightSwipeRecognizer
{
    if (rightSwipeRecognizer == nil) {
        rightSwipeRecognizer = [[UISwipeGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedSwipe:)];
        [rightSwipeRecognizer setDirection:UISwipeGestureRecognizerDirectionRight];
        [self configureGestureRecognizer:rightSwipeRecognizer];
        [[self viewForGestures] addGestureRecognizer:rightSwipeRecognizer];
    }
    return rightSwipeRecognizer;
}
-(UISwipeGestureRecognizer*)upSwipeRecognizer
{
    if (upSwipeRecognizer == nil) {
        upSwipeRecognizer = [[UISwipeGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedSwipe:)];
        [upSwipeRecognizer setDirection:UISwipeGestureRecognizerDirectionUp];
        [self configureGestureRecognizer:upSwipeRecognizer];
        [[self viewForGestures] addGestureRecognizer:upSwipeRecognizer];
    }
    return upSwipeRecognizer;
}
-(UISwipeGestureRecognizer*)downSwipeRecognizer
{
    if (downSwipeRecognizer == nil) {
        downSwipeRecognizer = [[UISwipeGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedSwipe:)];
        [downSwipeRecognizer setDirection:UISwipeGestureRecognizerDirectionDown];
        [self configureGestureRecognizer:downSwipeRecognizer];
        [[self viewForGestures] addGestureRecognizer:downSwipeRecognizer];
    }
    return downSwipeRecognizer;
}

-(UILongPressGestureRecognizer*)longPressRecognizer
{
    if (longPressRecognizer == nil) {
        longPressRecognizer = [[UILongPressGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedLongPress:)];
        [self configureGestureRecognizer:longPressRecognizer];
        [[self viewForGestures] addGestureRecognizer:longPressRecognizer];
        if (singleTapRecognizer != nil) {
            [singleTapRecognizer requireGestureRecognizerToFail:longPressRecognizer];
        }
        if (doubleTapRecognizer != nil) {
            [doubleTapRecognizer requireGestureRecognizerToFail:longPressRecognizer];
        }
    }
    return longPressRecognizer;
}

-(UIPanGestureRecognizer*)panRecognizer
{
    if (panRecognizer == nil) {
        panRecognizer = [[DirectionPanGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedPan:)];
        [self configureGestureRecognizer:panRecognizer];
        id direction = [[self proxy] valueForKey:@"panDirection"];
        if (direction) {
            panRecognizer.direction = PanDirectionFromObject(direction);
        }
        [[self viewForGestures] addGestureRecognizer:panRecognizer];
    }
    return panRecognizer;
}

-(UIPanGestureRecognizer*)shoveRecognizer
{
    if (shoveRecognizer == nil) {
        shoveRecognizer = [[DirectionPanGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedShove:)];
        [self configureGestureRecognizer:shoveRecognizer];
        shoveRecognizer.minimumNumberOfTouches = 2;
        shoveRecognizer.maximumNumberOfTouches = 2;
        shoveRecognizer.direction = DirectionPangestureRecognizerVertical;
        id direction = [[self proxy] valueForKey:@"panDirection"];
        if (direction) {
            shoveRecognizer.direction = PanDirectionFromObject(direction);
        }
        [[self viewForGestures] addGestureRecognizer:shoveRecognizer];
    }
    return shoveRecognizer;
}

-(UIRotationGestureRecognizer*)rotateRecognizer
{
    if (rotationRecognizer == nil) {
        rotationRecognizer = [[UIRotationGestureRecognizer alloc] initWithTarget:self action:@selector(recognizedRotation:)];
        [self configureGestureRecognizer:rotationRecognizer];
        [[self viewForGestures] addGestureRecognizer:rotationRecognizer];
    }
    return rotationRecognizer;
}

-(NSMutableDictionary*)dictionaryFromGesture:(UIGestureRecognizer*)recognizer {    
    UIView* theView = [recognizer startTouchedView];
    UIGestureRecognizerState state = [recognizer state];
    if ([recognizer state] == UIGestureRecognizerStateBegan || !theView) {
        UIView* view = recognizer.view;
        CGPoint loc = [recognizer locationInView:view];
        theView = [view hitTest:loc withEvent:nil];
        while (theView && !IS_OF_CLASS(theView, TiUIView)) {
            theView = [theView superview];
        }
        if ([recognizer state] == UIGestureRecognizerStateBegan) {
            [recognizer setStartTouchedView:(TiUIView*)theView];
        }
    } else if (state == UIGestureRecognizerStateEnded ||
               state == UIGestureRecognizerStateCancelled) {
        [recognizer setStartTouchedView:nil];
    }
    if (theView && theView != self) {
        return [(TiUIView*)theView dictionaryFromGesture:recognizer];
    }
    return [TiUtils dictionaryFromGesture:recognizer inView:theView];
}

-(void)recognizedSingleTap:(UITapGestureRecognizer*)recognizer
{
	NSDictionary *event = [self dictionaryFromGesture:recognizer];
    if ([recognizer numberOfTouchesRequired] == 2) {
		[proxy fireEvent:@"twofingertap" withObject:event checkForListener:NO];
	}
    else
        [proxy fireEvent:@"singletap" withObject:event propagate:NO checkForListener:NO];
}

-(void)recognizedDoubleTap:(UITapGestureRecognizer*)recognizer
{
	NSDictionary *event = [self dictionaryFromGesture:recognizer];
    //Because double-tap suppresses touchStart and double-click, we must do this:
    if ([proxy _hasListeners:@"touchstart"])
    {
        [proxy fireEvent:@"touchstart" withObject:event checkForListener:NO];
    }
    if ([proxy _hasListeners:@"dblclick"]) {
        [proxy fireEvent:@"dblclick" withObject:event checkForListener:NO];
    }
    [proxy fireEvent:@"doubletap" withObject:event propagate:NO checkForListener:NO];
}

-(void)recognizedPinch:(UIPinchGestureRecognizer*)recognizer 
{
    [self fireGestureEvent:recognizer ofType:@"pinch"];
}

-(void)recognizedLongPress:(UILongPressGestureRecognizer*)recognizer 
{ 
    if ([recognizer state] == UIGestureRecognizerStateBegan) {
        [self.proxy fireEvent:@"longpress" withObject:[self dictionaryFromGesture:recognizer] propagate:NO checkForListener:NO];
    }
}

-(void)fireGestureEvent:(UIGestureRecognizer*)recognizer ofType:(NSString*)type {
    UIGestureRecognizerState state = [recognizer state];
    NSDictionary* data = [self dictionaryFromGesture:recognizer];
    if (state == UIGestureRecognizerStateBegan) {
        [self.proxy fireEvent:[NSString stringWithFormat:@"%@start", type] withObject:data propagate:NO checkForListener:YES];
    } else if (state == UIGestureRecognizerStateEnded ||
               state == UIGestureRecognizerStateCancelled) {
        [self.proxy fireEvent:[NSString stringWithFormat:@"%@end", type] withObject:data propagate:NO checkForListener:YES];
    } else {
        [self.proxy fireEvent:type withObject:data propagate:NO checkForListener:NO];
    }
}

-(void)recognizedPan:(UIPanGestureRecognizer*)recognizer
{
    [self fireGestureEvent:recognizer ofType:@"pan"];
}

-(void)recognizedSwipe:(UISwipeGestureRecognizer *)recognizer
{
    NSDictionary *event = [self dictionaryFromGesture:recognizer];
    [proxy fireEvent:@"swipe" withObject:event propagate:NO checkForListener:NO];
}

-(void)recognizedRotation:(UIRotationGestureRecognizer*)recognizer
{
    [self fireGestureEvent:recognizer ofType:@"rotate"];
}

-(void)recognizedShove:(UIPanGestureRecognizer*)recognizer
{
    [self fireGestureEvent:recognizer ofType:@"shove"];
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
	if (hitView == [self viewForHitTest] && (touchPassThrough || (hasTouchListeners == NO && self.userInteractionEnabled==NO)))
	{
        if (touchPassThrough) {
            //still send touchstart as it might be usefull
            [self handleTouchEvent:@"touchstart" forTouch:[[event allTouches] anyObject]];
        }
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
//-(void)handleControlEvents:(UIControlEvents)events
//{
//	// For subclasses (esp. buttons) to override when they have event handlers.
//	TiViewProxy* parentProxy = (TiViewProxy*)[[self viewProxy] parent];
//	if ([parentProxy viewAttached] && [parentProxy canHaveControllerParent]) {
//		[[parentProxy view] handleControlEvents:events];
//	}
//}

// For subclasses
-(BOOL)touchedContentViewWithEvent:(UIEvent *)event
{
    return NO;
}

-(BOOL) enabledForBgState
{
    return _customUserInteractionEnabled;
}

-(void)setViewState:(UIControlState)state
{
    BOOL needsUpdate = viewState != state;
    viewState = state;
    if (needsUpdate)
    {
        [self setHighlighted:NO];
    }
}

-(UIControlState)realStateForState:(UIControlState)state
{
    if ([self enabledForBgState]) {
//        TiUIView * parentView = [[[self viewProxy] parent] view];
//        if (parentView && [parentView dispatchPressed]) {
//            return [parentView realStateForState:state];
//        }
        if (viewState != -1) {
            state = viewState;
        }
        return state;
    }
    return UIControlStateDisabled;
}

-(void)touchSetHighlighted:(BOOL)highlighted
{
    [self setHighlighted:highlighted];
}

-(NSMutableDictionary*)dictionaryFromTouch:(UITouch*)touch
{
    return [TiUtils dictionaryFromTouch:touch inView:self];
}

-(void)handleTouchEvent:(NSString*)event forTouch:(UITouch*)touch
{
    if ([self interactionEnabled])
	{
        if ([self.viewProxy _hasListeners:event checkParent:YES])
		{
            NSDictionary *evt = [self dictionaryFromTouch:touch];
            [proxy fireEvent:event withObject:evt propagate:YES checkForListener:NO];
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
            CGPoint localPoint = [touch locationInView:self];
            BOOL outside = (localPoint.x < -kTOUCH_MAX_DIST || (localPoint.x - self.frame.size.width)  > kTOUCH_MAX_DIST ||
                            localPoint.y < -kTOUCH_MAX_DIST || (localPoint.y - self.frame.size.height)  > kTOUCH_MAX_DIST);
            NSDictionary *evt = [self dictionaryFromTouch:touch];
            if (hasTouchEnd) {
                [proxy fireEvent:@"touchend" withObject:evt checkForListener:NO];
//                [self handleControlEvents:UIControlEventTouchCancel];
            }
            
            // Click handling is special; don't propagate if we have a delegate,
            // but DO invoke the touch delegate.
            // clicks should also be handled by any control the view is embedded in.
            if (!outside && hasDblclick && [touch tapCount] == 2) {
                [proxy fireEvent:@"dblclick" withObject:evt checkForListener:NO];
                return;
            }
            if (!outside && hasClick)
            {
                if (touchDelegate == nil) {
                    [proxy fireEvent:@"click" withObject:evt checkForListener:NO];
                }
            }
		}
	}
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event 
{
//    [self setBgState:UIControlStateNormal];
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

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer
{
    //tryout should it be possible?
    if (IS_OF_CLASS(gestureRecognizer, UISwipeGestureRecognizer) &&
        IS_OF_CLASS(otherGestureRecognizer, UIPanGestureRecognizer)) {
        return NO;
    }
    return YES;
//    return gestureRecognizer.delegate &&
//    otherGestureRecognizer.delegate &&
//    gestureRecognizer.delegate == otherGestureRecognizer.delegate;
}

-(void)configureGestureRecognizer:(UIGestureRecognizer*)gestureRecognizer
{
    [gestureRecognizer setTiGesture:YES];
    [gestureRecognizer setDelaysTouchesBegan:NO];
    [gestureRecognizer setDelaysTouchesEnded:NO];
    [gestureRecognizer setCancelsTouchesInView:NO];
    [gestureRecognizer setDelegate:(id<UIGestureRecognizerDelegate>)self];
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
    if ([event isEqualToString:@"rotate"]) {
        return [self rotateRecognizer];
    }
    if ([event isEqualToString:@"shove"]) {
        return [self shoveRecognizer];
    }
    if ([event isEqualToString:@"longpress"]) {
        return [self longPressRecognizer];
    }
    if ([event isEqualToString:@"pan"]) {
        return [self panRecognizer];
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

-(void)listenerAdded:(NSString*)event count:(NSInteger)count
{
	if (count == 1 && [self viewSupportsBaseTouchEvents])
	{
		[self handleListenerAddedWithEvent:event];
        [self updateTouchHandling];
	}
}

-(void)listenerRemoved:(NSString*)event count:(NSInteger)count
{
	if (count == 0)
	{
		[self handleListenerRemovedWithEvent:event];
        [self updateTouchHandling];
	}
}

-(void)sanitycheckListeners	//TODO: This can be optimized and unwound later.
{
	for (NSString * eventName in [self gestureListenersArray]) {
		if ([[self viewProxy] _hasListenersIgnoreBubble:eventName]) {
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
        self.layer.mask.contentsCenter = TiDimensionLayerContentCenter(imageCap.topCap, imageCap.leftCap, imageCap.topCap, imageCap.leftCap, [image size]);
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
    isHiglighted = (_selected || isHiglighted);
    [self setHighlighted:isHiglighted animated:NO];
}

-(void)setHighlighted:(BOOL)isHiglighted animated:(BOOL)animated
{
    [self setAnimatedTransition:animated];
    [self setBgState:isHiglighted?UIControlStateHighlighted:UIControlStateNormal];
    if (!_dispatchPressed) return;
	for (TiUIView * thisView in [self childViews])
	{
        if ([thisView.subviews count] > 0) {
            id firstChild = [thisView.subviews objectAtIndex:0];
            if ([firstChild isKindOfClass:[UIControl class]] && [firstChild respondsToSelector:@selector(setHighlighted:animated:)])
            {
                [firstChild setHighlighted:isHiglighted animated:animated];//swizzle will call setHighlighted on the view
            }
            else {
                [(id)thisView setHighlighted:isHiglighted animated:animated];
            }
        }
        else {
			[(id)thisView setHighlighted:isHiglighted animated:animated];
		}
	}
    [self setAnimatedTransition:NO];
}

-(void)setSelected:(BOOL)isSelected
{
    isSelected = (_selected || isSelected);
    [self setSelected:isSelected animated:NO];
}

-(void)setSelected:(BOOL)isSelected animated:(BOOL)animated
{
    [self setAnimatedTransition:animated];
    
    //we dont really support Selected for background as it is not necessary
    [self setBgState:isSelected?UIControlStateHighlighted:UIControlStateNormal];
    if (!_dispatchPressed) return;
	for (TiUIView * thisView in [self childViews])
	{
        if ([thisView.subviews count] > 0) {
            id firstChild = [thisView.subviews objectAtIndex:0];
            if ([firstChild isKindOfClass:[UIControl class]] && [firstChild respondsToSelector:@selector(setSelected:animated:)])
            {
                [firstChild setSelected:isSelected animated:animated]; //swizzle will call setSelected on the view
            }
            else {
                [(id)thisView setSelected:isSelected animated:animated];
            }
        }
        else {
			[(id)thisView setSelected:isSelected animated:animated];
		}
	}
    [self setAnimatedTransition:NO];
}

- (void)transitionFromView:(TiUIView *)viewOut toView:(TiUIView *)viewIn withTransition:(TiTransition *)transition animationBlock:(void (^)(void))animBlock completionBlock:(void (^)(void))block{
    [(TiAnimatableProxy*)viewOut.proxy addRunningAnimation:transition];
    [(TiAnimatableProxy*)viewIn.proxy addRunningAnimation:transition];
    
    [TiTransitionHelper transitionFromView:viewOut toView:viewIn insideView:self withTransition:transition prepareBlock:NULL animationBlock:animBlock completionBlock:^{
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
        NSDictionary* options = nil;
        ENSURE_ARG_AT_INDEX(options, args, 1, NSDictionary)
        if (![options objectForKey:@"filters"]) {
            [options setValue:[NSArray arrayWithObject:[NSNumber numberWithInt:TiImageHelperFilterIOSBlur]] forKey:@"filters"];
        }
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

-(UIViewController*)getContentController
{
    return ([[self viewProxy] getContentController]);
}

-(void)setMaskFromView_:(id)arg
{
    TiViewProxy* viewProxy = [self viewProxy];
    TiProxy* vp = [viewProxy createChildFromObject:arg];
    TiProxy* current = [[self viewProxy] holdedProxyForKey:kTiViewShapeMaskKey];
    if (IS_OF_CLASS(current, TiViewProxy)) {
        [[[(TiViewProxy*)current view] layer] removeFromSuperlayer];
        [[self viewProxy] removeHoldedProxyForKey:kTiViewShapeMaskKey];
    }
    if (IS_OF_CLASS(vp, TiViewProxy)) {
        [[self viewProxy] addProxyToHold:vp forKey:kTiViewShapeMaskKey];
        self.layer.mask = [[(TiViewProxy*)vp getAndPrepareViewForOpening] layer];
    }
    [self.layer setNeedsDisplay];
}

@end
