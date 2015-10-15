/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiProxy.h"
#import "TiGradient.h"
#import "LayoutConstraint.h"
//#import "TiSelectableBackgroundLayer.h"
#import "TouchDelegate_Views.h"

@class TiTransition;
#ifdef TI_USE_AUTOLAYOUT
#import "TiLayoutView.h"
#endif
//By declaring a scrollView protocol, TiUITextWidget can access 
@class TiUIView;
@class TiSelectableBackgroundLayer;
@class TiBorderLayer;

@interface UntouchableView : UIView

@end
/**
 The protocol for scrolling.
 */
@protocol TiScrolling

/**
 Tells the scroll view that keyboard did show.
 @param keyboardTop The keyboard height.
 */
-(void)keyboardDidShowAtHeight:(CGFloat)keyboardTop;

/**
 Tells the scroll view to scroll to make the specified view visible.
 @param firstResponderView The view to make visible.
 @param keyboardTop The keyboard height.
 */
-(void)scrollToShowView:(TiUIView *)firstResponderView withKeyboardHeight:(CGFloat)keyboardTop;

@end

void InsetScrollViewForKeyboard(UIScrollView * scrollView,CGFloat keyboardTop,CGFloat minimumContentHeight);
void OffsetScrollViewForRect(UIScrollView * scrollView,CGFloat keyboardTop,CGFloat minimumContentHeight,CGRect responderRect);

void ModifyScrollViewForKeyboardHeightAndContentHeightWithResponderRect(UIScrollView * scrollView,CGFloat keyboardTop,CGFloat minimumContentHeight,CGRect responderRect);

@class TiViewProxy;
@class ADTransition;
@class TiViewAnimationStep;
@class TiRect;
@class DirectionPanGestureRecognizer;
/**
 Base class for all Titanium views.
 @see TiViewProxy
 */
#ifdef TI_USE_AUTOLAYOUT
@interface TiUIView : TiLayoutView<TiProxyDelegate, TouchDelegate>
#else
@interface TiUIView : UIView<TiProxyDelegate, TouchDelegate, LayoutAutosizing>
#endif
{
@protected
    BOOL configurationSet;
    NSMutableArray* childViews;
    UIControlState viewState;
	BOOL _tintColorImage;
    TiCap imageCap;
//@private
	TiProxy *proxy;
		
	id transformMatrix;
	BOOL childrenInitialized;
	
	// Touch detection
    BOOL changedInteraction;
	BOOL handlesTouches;
	UIView *touchDelegate;		 // used for touch delegate forwarding
	BOOL animating;

	BOOL touchPassThrough;
	BOOL clipChildren;
	
	UITapGestureRecognizer*			singleTapRecognizer;
	UITapGestureRecognizer*			doubleTapRecognizer;
	UITapGestureRecognizer*			twoFingerTapRecognizer;
	UIPinchGestureRecognizer*		pinchRecognizer;
	UISwipeGestureRecognizer*		leftSwipeRecognizer;
	UISwipeGestureRecognizer*		rightSwipeRecognizer;
	UISwipeGestureRecognizer*		upSwipeRecognizer;
	UISwipeGestureRecognizer*		downSwipeRecognizer;
    UILongPressGestureRecognizer*	longPressRecognizer;
    DirectionPanGestureRecognizer*         panRecognizer;
    DirectionPanGestureRecognizer*         shoveRecognizer;
    UIRotationGestureRecognizer*    rotationRecognizer;
	
	//Resizing handling
	CGSize oldSize;
    
    float backgroundOpacity;
	NSRecursiveLock *transferLock;
}

/**
 Returns current status of the view animation.
 @return _YES_ if view is being animated, _NO_ otherwise.
 */
-(BOOL)animating;
-(void)cancelAllAnimations;

/**
 Provides access to a proxy object of the view. 
 */
@property(nonatomic,readwrite,assign)	TiProxy *proxy;

/**
 Provides access to touch delegate of the view.
 
 Touch delegate is the control that receives all touch events.
 */
@property(nonatomic,readwrite,assign)	UIView *touchDelegate;

/**
 Returns view's transformation matrix.
 */
@property(nonatomic,readonly)			id transformMatrix;


@property(nonatomic,readwrite,retain)	TiViewAnimationStep *runningAnimation;

/**
 Provides access to background image of the view.
 */
//@property(nonatomic,readwrite,retain) id backgroundImage;

/**
 Returns enablement of touch events.
 @see updateTouchHandling
 */
@property(nonatomic,readonly) BOOL touchEnabled;
@property(nonatomic,readonly) CGSize oldSize;

@property(nonatomic,readonly)	UITapGestureRecognizer*			singleTapRecognizer;
@property(nonatomic,readonly)	UITapGestureRecognizer*			doubleTapRecognizer;
@property(nonatomic,readonly)	UITapGestureRecognizer*			twoFingerTapRecognizer;
@property(nonatomic,readonly)	UIPinchGestureRecognizer*		pinchRecognizer;
@property(nonatomic,readonly)	UISwipeGestureRecognizer*		leftSwipeRecognizer;
@property(nonatomic,readonly)	UISwipeGestureRecognizer*		rightSwipeRecognizer;
@property(nonatomic,readonly)	UILongPressGestureRecognizer*	longPressRecognizer;

-(void)configureGestureRecognizer:(UIGestureRecognizer*)gestureRecognizer;
- (UIGestureRecognizer *)gestureRecognizerForEvent:(NSString *)event;
-(void)handleListenerRemovedWithEvent:(NSString *)event;
-(void)handleListenerAddedWithEvent:(NSString *)event;
-(BOOL)proxyHasGestureListeners;
-(void)ensureGestureListeners;

#pragma mark Framework
-(TiViewProxy*)viewProxy;

/**
 Performs view's initialization procedure.
 */
-(void)initializeState;
- (void) initialize;

/**
 Performs view's configuration procedure.
 */
-(void)configurationStart;
-(void)configurationSet;
-(BOOL)isConfigurationSet;

-(void)setTransform_:(id)matrix;
-(void) setBackgroundColor_:(id)color;
-(void)setEnabled_:(id)arg;

/*
 Tells the view to load an image.
 @param image The string referring the image.
 @return The loaded image.
 */
-(UIImage*)loadImage:(id)image;
-(id)loadImageOrSVG:(id)arg;

-(id)proxyValueForKey:(NSString *)key;
-(void)readProxyValuesWithKeys:(id<NSFastEnumeration>)keys;

-(NSArray*) childViews;


/**
 Tells the view to update its touch handling state.
 @see touchEnabled
 */
-(void)updateTouchHandling;

/**
 Tells the view that its frame and/or bounds has chnaged.
 @param frame The frame rect
 @param bounds The bounds rect
 */
-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds;

/**
 Tells the view to make its root view a first responder.
 */
-(void)makeRootViewFirstResponder;

/**
 The convenience method to raise an exception for the view.
 @param reason The exception reason.
 @param subreason The exception subreason.
 @param location The exception location.
 */
+(void)throwException:(NSString *) reason subreason:(NSString*)subreason location:(NSString *)location;

-(void)throwException:(NSString *) reason subreason:(NSString*)subreason location:(NSString *)location;

/**
 Returns default enablement for interactions.
 
 Subclasses may override.
 @return _YES_ if the control has interactions enabled by default, _NO_ otherwise.
 */
-(BOOL)interactionDefault; 

-(BOOL)interactionEnabled;
-(void)setCustomUserInteractionEnabled:(BOOL)value;

/**
 Whether or not the view has any touchable listeners attached.
 @return _YES_ if the control has any touchable listener attached, _NO_ otherwise.
 */
-(BOOL)hasTouchableListener;

//-(void)handleControlEvents:(UIControlEvents)events;

-(void)setVisible_:(id)visible;
-(void)setSelected_:(id)arg;
-(void)setTintColor_:(id)color;

-(void)setBackgroundImage_:(id)value;

@property(nonatomic,readwrite,assign)	BOOL alwaysUseBackgroundLayer;
-(UIView *)backgroundWrapperView;
-(void)setBgState:(UIControlState)state;
@property (nonatomic, readonly) TiSelectableBackgroundLayer* backgroundLayer;
@property (nonatomic, readonly) TiBorderLayer* borderLayer;
@property(nonatomic,assign) BOOL shouldHandleSelection;
@property(nonatomic,assign) BOOL animateBgdTransition;
@property(nonatomic,assign) BOOL canKeepBackgroundColor;

-(void)checkBounds;
-(void)updateBounds:(CGRect)newBounds;

-(BOOL)clipChildren;

@property (nonatomic, readonly) id accessibilityElement;

- (void)setAccessibilityLabel_:(id)accessibilityLabel;
- (void)setAccessibilityValue_:(id)accessibilityValue;
- (void)setAccessibilityHint_:(id)accessibilityHint;
- (void)setAccessibilityHidden_:(id)accessibilityHidden;

/**
 Whether or not a view not normally picked up by the Titanium view hierarchy (such as wrapped iOS UIViews) was touched.
 @return _YES_ if the view contains specialized content (such as a system view) which should register as a touch for this view, _NO_ otherwise.
 */
-(BOOL)touchedContentViewWithEvent:(UIEvent*)event;

- (void)processTouchesBegan:(NSSet *)touches withEvent:(UIEvent *)event;
- (void)processTouchesMoved:(NSSet *)touches withEvent:(UIEvent *)event;
- (void)processTouchesEnded:(NSSet *)touches withEvent:(UIEvent *)event;
- (void)processTouchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event;

-(void)recognizedLongPress:(UILongPressGestureRecognizer*)recognizer;
-(void)recognizedPinch:(UIPinchGestureRecognizer*)recognizer;
-(void)recognizedSwipe:(UISwipeGestureRecognizer *)recognizer;
-(void)recognizedPan:(UIPanGestureRecognizer *)recognizer;
-(void)recognizedRotation:(UIPanGestureRecognizer *)recognizer;
-(void)recognizedShove:(UIRotationGestureRecognizer *)recognizer;

-(void)detach;

-(void)setHighlighted:(BOOL)isHiglighted;
-(void)setHighlighted:(BOOL)isHiglighted animated:(BOOL)animated;
-(void)setSelected:(BOOL)isSelected;
-(void)setSelected:(BOOL)isSelected animated:(BOOL)animated;
- (void)transitionFromView:(UIView *)viewOut toView:(UIView *)viewIn withTransition:(TiTransition *)transition animationBlock:(void (^)(void))animationBlock completionBlock:(void (^)(void))block;
- (void)blurBackground:(id)args;
-(UIControlState)realStateForState:(UIControlState)state;
-(BOOL) enabledForBgState;
-(void)setViewState:(UIControlState)state;
-(void)touchSetHighlighted:(BOOL)highlighted;
-(UIView*)parentViewForChildren;
-(void)onCreateCustomBackground;

-(NSMutableDictionary*)dictionaryFromTouch:(UITouch*)touch;

-(NSMutableDictionary*)dictionaryFromGesture:(UIGestureRecognizer*)gesture;
-(UIViewController*)getContentController;
-(void)fillBoundsToRect:(TiRect*)rect;

@end

#pragma mark TO REMOVE, used only during transition.

#define USE_PROXY_FOR_METHOD(resultType,methodname,inputType)	\
-(resultType)methodname:(inputType)value	\
{	\
	DeveloperLog(@"[DEBUG] Using view proxy via redirection instead of directly for %@.",self);	\
	return [(TiViewProxy *)[self proxy] methodname:value];	\
}

#define USE_PROXY_FOR_VERIFY_AUTORESIZING	USE_PROXY_FOR_METHOD(UIViewAutoresizing,verifyAutoresizing,UIViewAutoresizing)



