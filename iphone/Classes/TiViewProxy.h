/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiProxy.h"
#import "TiUIView.h"
#import "TiRect.h"
#import "TiProxyTemplate.h"
#import <pthread.h>
#import "TiAnimatableProxy.h"
#import "TiViewController.h"
/*
 This Protocol will be implemented by objects that want to
 monitor views not in the normal view heirarchy. 
*/
@protocol TiProxyObserver
@optional
-(void)proxyDidRelayout:(id)sender;

@end

#pragma mark dirtyflags used by TiViewProxy
#define NEEDS_LAYOUT_CHILDREN	1
//Set this flag to true to disable instant updates
static const BOOL ENFORCE_BATCH_UPDATE = NO;


enum
{
	TiRefreshViewPosition = 2,
	TiRefreshViewChildrenPosition,
    TiRefreshViewZIndex,
    TiRefreshViewSize,

	TiRefreshViewEnqueued,
};

@class TiAction, TiBlob, TiViewAnimationStep, TiViewController, TiWindowProxy;
//For TableRows, we need to have minimumParentHeightForWidth:

/**
 The class represents a proxy that is attached to a view.
 The class is not intended to be overriden.
 */
@interface TiViewProxy : TiAnimatableProxy<LayoutAutosizing>
{
@protected
//TODO: Actually have a rhyme and reason on keeping things @protected vs @private.
//For now, for sake of proper value grouping, we're all under one roof.

#ifndef TI_USE_AUTOLAYOUT
#pragma mark Layout properties
	LayoutConstraint layoutProperties;
#endif
    NSInteger vzIndex;
	BOOL hidden;	//This is the boolean version of ![TiUtils boolValue:visible def:yes]
		//And has nothing to do with whether or not it's onscreen or
    
    BOOL readyToCreateView;
    BOOL defaultReadyToCreateView;


#pragma mark Visual components
	TiUIView *view;
	UIBarButtonItem * barButtonItem;

#pragma mark Layout caches that can be recomputed
	CGFloat verticalLayoutBoundary;
	CGFloat horizontalLayoutBoundary;
	CGFloat horizontalLayoutRowHeight;	//Note, this has nothing to do with table views.
	int lastChildArranged;

	CGRect sandboxBounds;
	CGPoint positionCache;	//Recomputed and stored when position changes.
	CGRect sizeCache;	//Recomputed and stored when size changes.
	UIViewAutoresizing autoresizeCache;	//Changed by repositioning or resizing.

	BOOL parentVisible;
	//In most cases, this is the same as [parent parentVisible] && ![parent hidden]
	//However, in the case of windows attached to the root view, the parent is ALWAYS visible.
	//That is, will be true if and only if all parents are visible or are the root controller.
	//Use parentWillShow and parentWillHide to set this.

#pragma mark Housecleaning that is set and used
	NSRecursiveLock *destroyLock;

	BOOL windowOpened;
	BOOL windowOpening;

	int dirtyflags;	//For atomic actions, best to be explicit about the 32 bitness.
	BOOL viewInitialized;
	BOOL repositioning;
	BOOL isUsingBarButtonItem;
    //This flag is set to true on startLayout() call and false on finishLayout() call
    BOOL updateStarted;
    BOOL allowLayoutUpdate;
    
    NSMutableDictionary *layoutPropDictionary;
    
    id observer;
    TiViewController* controller;
}

#pragma mark public API
@property(nonatomic, readwrite, retain) TiViewController* controller;

@property(nonatomic,readonly) TiRect * size;
@property(nonatomic,readonly) TiRect * rect;
/*
 Provides access to z-index value.
*/
@property(nonatomic,readwrite,assign) NSInteger vzIndex;

/**
 Provides access to visibility of parent view proxy.
 */
@property(nonatomic,readwrite,assign) BOOL parentVisible; // For tableview magic ONLY
@property(nonatomic,readwrite,assign) BOOL preventListViewSelection; // For listview
@property(nonatomic,readwrite,assign) BOOL canBeResizedByFrame;
@property(nonatomic,readwrite,assign) BOOL canRepositionItself;
@property(nonatomic,readwrite,assign) BOOL canResizeItself;


@property(nonatomic,readwrite,assign) BOOL hiddenForLayout;

-(void)startLayout:(id)arg;//Deprecated since 3.0.0
-(void)finishLayout:(id)arg;//Deprecated since 3.0.0
-(void)updateLayout:(id)arg;//Deprecated since 3.0.0
-(void)setTempProperty:(id)propVal forKey:(id)propName;
-(void)processTempProperties:(NSDictionary*)arg;
-(void)setProxyObserver:(id)arg;

/**
 Tells the view proxy to set visibility on a child proxy to _YES_.
 @param arg A single proxy to show.
 */
-(void)show:(id)arg;

/**
 Tells the view proxy to set visibility on a child proxy to _NO_.
 @param arg A single proxy to hide.
 */
-(void)hide:(id)arg;

#ifndef TI_USE_AUTOLAYOUT
-(void)setTop:(id)value;
-(void)setBottom:(id)value;
-(void)setLeft:(id)value;
-(void)setRight:(id)value;
-(void)setWidth:(id)value;
-(void)setHeight:(id)value;
#endif
-(void)setZIndex:(id)value;
-(id)zIndex;

// See the code for setValue:forUndefinedKey: for why we can't have this
#ifndef TI_USE_AUTOLAYOUT
-(void)setMinWidth:(id)value;
-(void)setMinHeight:(id)value;
-(void)setCenter:(id)value;
#endif
-(NSMutableDictionary*)center;
-(id)animatedCenter;

-(void)setBackgroundGradient:(id)arg;
-(TiBlob*)toImage:(id)args;
-(UIImage*)toImageWithScale:(CGFloat)scale;

-(void)setDefaultReadyToCreateView:(BOOL)ready;
//this is for the tableview magic. For any other view, will be set when
//added to a parent which is already ready
-(void)setReadyToCreateView:(BOOL)ready;
-(void)setReadyToCreateView:(BOOL)ready recursive:(BOOL)recursive;
-(void)clearView:(BOOL)recurse;
-(TiUIView*)getOrCreateView;
-(void)fakeOpening;

-(TiViewProxy*)viewParent;
//-(void)setParent:(TiParentingProxy*)parent_ checkForOpen:(BOOL)check;
-(void)runBlock:(void (^)(TiViewProxy* proxy))block onlyVisible:(BOOL)onlyVisible recursive:(BOOL)recursive;
-(void)runBlockOnMainThread:(void (^)(TiViewProxy* proxy))block onlyVisible:(BOOL)onlyVisible recursive:(BOOL)recursive;

#pragma mark nonpublic accessors not related to Housecleaning


#ifndef TI_USE_AUTOLAYOUT
/**
 Provides access to layout properties of the underlying view.
 */
@property(nonatomic,readonly,assign) LayoutConstraint * layoutProperties;
#endif

/**
 Provides access to sandbox bounds of the underlying view.
 */
@property(nonatomic,readwrite,assign) CGRect sandboxBounds;
	//This is unaffected by parentVisible. So if something is truely visible, it'd be [self visible] && parentVisible.
-(void)setHidden:(BOOL)newHidden withArgs:(id)args;
-(BOOL)isHidden;

@property(nonatomic,retain) UIBarButtonItem * barButtonItem;
-(TiUIView *)barButtonViewForSize:(CGSize)bounds;
-(TiUIView *)barButtonViewForRect:(CGRect)bounds;
-(UIBarButtonItem*)barButtonItemForRect:(CGRect)bounds;

//NOTE: DO NOT SET VIEW UNLESS IN A TABLE VIEW, AND EVEN THEN.
@property(nonatomic,readwrite,retain)TiUIView * view;

/**
 Returns language conversion table.
 
 Subclasses may override.
 @return The dictionary 
 */
-(NSMutableDictionary*)langConversionTable;

#pragma mark Methods subclasses should override for behavior changes

/**
 Whether or not the view proxy can have non Ti-Views which have to be pushed to the bottom when adding children.
 **This method is only meant for legacy classes. New classes must implement the proper wrapperView code**
 Subclasses may override.
 @return _NO_ if the view proxy can have non Ti-Views in its view heirarchy
 */
-(BOOL)optimizeSubviewInsertion;

/**
 Whether or not the view proxy needs to suppress relayout.
 
 Subclasses may override.
 @return _YES_ if relayout should be suppressed, _NO_ otherwise.
 */
-(BOOL)suppressesRelayout;

/**
 Whether or not the view proxy supports navigation bar positioning.
 
 Subclasses may override.
 @return _YES_ if navigation bar positioning is supported, _NO_ otherwise.
 */
-(BOOL)supportsNavBarPositioning;

/**
 Whether or not the view proxy can have a UIController object in its parent view.
 
 Subclasses may override.
 @return _YES_ if the view proxy can have a UIController object in its parent view
 */
-(BOOL)canHaveControllerParent;

/**
 Whether or not the view proxy should detach its view on unload.
 
 Subclasses may override.
 @return _YES_ if the view should be detached, _NO_ otherwise.
 */
-(BOOL)shouldDetachViewOnUnload;

/**
 Returns parent view for child proxy.
 
 The method is used in cases when proxies heirarchy is different from views hierarchy.
 Subclasses may override.
 @param child The child view proxy for which return the parent view.
 @return The parent view
 */
-(UIView *)parentViewForChild:(TiViewProxy *)child;
-(TiWindowProxy*)getParentWindow;

#pragma mark Event trigger methods

/**
 Tells the view proxy that the attached window will open.
 @see windowDidOpen
 */
-(void)windowWillOpen;

/**
 Tells the view proxy that the attached window did open.
 @see windowWillOpen
 */
-(void)windowDidOpen;

/**
 Tells the view proxy that the attached window will close.
 @see windowDidClose
 */
-(void)windowWillClose;

/**
 Tells the view proxy that the attached window did close.
 @see windowWillClose
 */
-(void)windowDidClose;

/**
 Tells the view proxy that its properties are about to change.
 @see didFirePropertyChanges
 */
-(void)willFirePropertyChanges;

/**
 Tells the view proxy that its properties are changed.
 @see willFirePropertyChanges
 */
-(void)didFirePropertyChanges;

/**
 Tells the view proxy that a view will be attached to it.
 @see viewDidInitialize
 */
-(void)viewWillInitialize; // Need this for video player & possibly other classes which override newView

/**
 Tells the view proxy that a view was initialized.
 @see viewWillInitialize
 */
-(void)viewDidInitialize;

/**
 Tells the view proxy that a view was attached to it.
 @see viewWillInitialize
 */
-(void)viewDidAttach;

/**
 Tells the view proxy that a view will be detached from it.
 @see viewDidDetach
 */
-(void)viewWillDetach;

/**
 Tells the view proxy that a view was detached from it.
 @see viewWillDetach
 */
-(void)viewDidDetach;

-(void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration;
-(void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration;
-(void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation;

-(void)viewWillAppear:(BOOL)animated;
-(void)viewWillDisappear:(BOOL)animated;

#pragma mark Housecleaning state accessors
//TODO: Sounds like the redundancy department of redundancy was here.
/**
 Whether or not a view is attached to the view proxy.
 @return _YES_ if the view proxy has a view attached to it, _NO_ otherwise.
 */
-(BOOL)viewAttached;

/**
 Whether or not the view proxy has been initialized.
 @return _YES_ if the view proxy has been initialized, _NO_ otherwise.
 */
-(BOOL)viewInitialized;

/**
 Whether or not the view proxy has been completely set up.
 @return _YES_ if the view proxy has been initialized and its view has a superview and non-empty bounds, _NO_ otherwise.
 */
-(BOOL)viewReady;

-(BOOL)viewLayedOut;

/**
 Whether or not a window attached to the view proxy has been opened.
 @return _YES_ if the view proxy's window has been opened, _NO_ otherwise.
 */
-(BOOL)windowHasOpened;

/**
 Whether or not a window attached to the view proxy is currently being opened.
 @return _YES_ if the view proxy's window is being opened, _NO_ otherwise.
 */
-(BOOL)windowIsOpening;

/**
 Whether or not the view proxy is using a bar button item.
 @return _YES_ if a bar button item is used, _NO_ otherwise.
 */
-(BOOL)isUsingBarButtonItem;

#pragma mark Building up and tearing down
-(void)firePropertyChanges;

/**
 Returns a ne view corresponding to the view proxy.
 @return The created view.
 */
-(TiUIView*)newView;

/**
 Tells the view proxy to detach its view.
 */
-(void)detachView;
-(void)detachView:(BOOL)recursive;

-(void)destroy;


/**
 Tells the view proxy to remove its bar button item.
 */
-(void)removeBarButtonView;

#pragma mark Callbacks

/**
 Tells the view attached to the view proxy to perform a selector with given arguments.
 @param selector The selector to perform.
 @param object The argument for the method performed.
 @param create The flag to create the view if the one is not attached.
 @param wait The flag to wait till the operation completes.
 */
-(void)makeViewPerformSelector:(SEL)selector withObject:(id)object createIfNeeded:(BOOL)create waitUntilDone:(BOOL)wait;

#pragma mark Layout events, internal and external

/**
 Tells the view proxy that the attached view size will change.
 */
-(void)willChangeSize;

/**
 Tells the view proxy that the attached view position will change.
 */
-(void)willChangePosition;

/**
 Tells the view proxy that the attached view z-index will change.
*/
-(void)willChangeZIndex;

/** Tells the view proxy that the attached view layout will change.
 */
-(void)willChangeLayout;

/**
 Tells the view proxy that the attached view will show.
 */
-(void)willShow;

/**
 Tells the view proxy that the attached view will hide.
 */
-(void)willHide;

/**
 Tells the view proxy that the attached view contents will change.
 */
-(void)contentsWillChange;

/**
 Tells the view proxy that the attached view contents will change and
 that it should layout immediately
 */
-(void)contentsWillChangeImmediate;
-(void)contentsWillChangeAnimated:(NSTimeInterval)duration;

/**
 Tells the view proxy that the attached view's parent size will change.
 */
-(void)parentSizeWillChange;

/**
 Tells the view proxy that the attached view's parent will change position and size.
 */
-(void)parentWillRelay;

/**
 Tells the view proxy that the attached view's parent will show.
 */
-(void)parentWillShow;

/**
 Tells the view proxy that the attached view's parent will hide.
 */
-(void)parentWillHide;

#pragma mark Layout actions

-(void)refreshView:(TiUIView *)transferView;

/**
 Tells the view proxy to force size refresh of the attached view.
 */
-(void)refreshSize;

/**
 Tells the view proxy to force position refresh of the attached view.
 */
-(void)refreshPosition;

/**
 Puts the view in the layout queue for rendering.
 */
-(void)willEnqueue;

//Unlike the other layout actions, this one is done by the parent of the one called by refreshView.
//This is the effect of refreshing the Z index via careful view placement.
-(void)insertSubview:(UIView *)childView forProxy:(TiViewProxy *)childProxy;

-(NSArray*)viewChildren;
-(NSArray*)visibleChildren;
#pragma mark Layout commands that need refactoring out

-(void)determineSandboxBounds;

/**
 Tells the view to layout its children.
 @param optimize Internal use only. Always specify _NO_.
 */
-(void)layoutChildren:(BOOL)optimize;

/**
 Tells the view to layout its children only if there were any layout changes.
 */
-(void)layoutChildrenIfNeeded;

-(void)layoutChild:(TiViewProxy*)child optimize:(BOOL)optimize withMeasuredBounds:(CGRect)bounds;
-(NSArray*)measureChildren:(NSArray*)childArray;
-(CGRect)computeChildSandbox:(TiViewProxy*)child withBounds:(CGRect)bounds;
-(CGRect)computeBoundsForParentBounds:(CGRect)parentBounds;

/**
 Tells the view to adjust its size and position according to the current layout constraints.
 */
-(BOOL)relayout;

-(void)reposition;	//Todo: Replace
-(void)repositionWithinAnimation:(TiViewAnimationStep*)animation;
-(void)repositionWithinAnimation;
/**
 Tells if the view is enqueued in the LayoutQueue
 */
-(BOOL)willBeRelaying;

-(BOOL) widthIsAutoFill;
-(BOOL) widthIsAutoSize;
-(BOOL) heightIsAutoFill;
-(BOOL) heightIsAutoSize;
-(BOOL) belongsToContext:(id<TiEvaluator>) context;

-(CGSize)autoSizeForSize:(CGSize)size;
-(CGSize)autoSizeForSize:(CGSize)size ignoreMinMax:(BOOL)ignoreMinMaxComputation;

/**
 Tells the view that its child view size will change.
 @param child The child view
 */
-(void)childWillResize:(TiViewProxy *)child;	//Todo: Replace
-(void)childWillResize:(TiViewProxy *)child withinAnimation:(TiViewAnimationStep*)animation;
-(void)aboutToBeAnimated;

/**
 The current running animation
 */
-(TiViewAnimationStep*)runningAnimation;
-(void)setRunningAnimation:(TiViewAnimationStep*)animation;

- (void)prepareForReuse;

//+ (TiViewProxy *)unarchiveFromTemplate:(id)viewTemplate inContext:(id<TiEvaluator>)context;

/**
 Performs view's configuration procedure. Used during proxy creation and listview item update
 */
-(void)configurationStart;
-(void)configurationStart:(BOOL)recursive;
-(void)configurationSet;
-(void)configurationSet:(BOOL)recursive;
-(BOOL)isConfigurationSet;

/**
 foucs methods
 */
- (void)focus:(id)args;
- (void)blur:(id)args;
- (BOOL)focused:(id)unused;
- (BOOL)focussed;

/**
 Method to simulate the layout of child even if not really a child
 */
-(void)layoutNonRealChild:(TiViewProxy*)child withParent:(UIView*)parentView;

/**
 Verify size
 */
-(CGSize)verifySize:(CGSize)size;

/**
 Set a fake animation (used by windows during rotation)
 */
-(void)setFakeAnimationOfDuration:(NSTimeInterval)duration andCurve:(CAMediaTimingFunction*)curve;
/**
 remove the fake animation
 */
-(void)removeFakeAnimation;


-(void)performBlock:(void (^)(void))block withinOurAnimationOnProxy:(TiViewProxy*)viewProxy;

/**
 Update the view if necessary
 */
-(void)refreshViewIfNeeded;
-(void)refreshViewIfNeeded:(BOOL)recursive;
-(void)refreshViewOrParent;
-(void)refreshView;

/**
 Perform a block while preventing relayout
 */
-(void)performBlockWithoutLayout:(void (^)(void))block;
-(void)performLayoutBlockAndRefresh:(void (^)(void))block;

/**
 Make the view dirty so that it will get refreshed on the next run
 */
-(void)dirtyItAll;

/**
Set the animation on its view and all it's children
 */
-(void)setRunningAnimationRecursive:(TiViewAnimationStep*)animation;

/**
 Tells if the view is currently in the process of being rotated
 */
-(BOOL)isRotating;

/**
 Create or access a managing controller. Only call if you want a controller!
 */
-(UIViewController*) hostingController;
-(UIViewController*) controller;
-(TiUIView*) getAndPrepareViewForOpening:(CGRect)bounds;
-(TiUIView*) getAndPrepareViewForOpening;
+(void)reorderViewsInParent:(UIView*)parentView;
@end


#define USE_VIEW_FOR_METHOD(resultType,methodname,inputType)	\
-(resultType) methodname: (inputType)value	\
{	\
    return [[self view] methodname:value];	\
}

#define USE_VIEW_FOR_VERIFY_WIDTH	USE_VIEW_FOR_METHOD(CGFloat,verifyWidth,CGFloat)
#define USE_VIEW_FOR_VERIFY_HEIGHT	USE_VIEW_FOR_METHOD(CGFloat,verifyHeight,CGFloat)
#define USE_VIEW_FOR_CONTENT_SIZE	USE_VIEW_FOR_METHOD(CGSize,contentSizeForSize,CGSize)

#define DECLARE_VIEW_CLASS_FOR_NEWVIEW(viewClass)	\
-(TiUIView*)newView	\
{	\
	return [[viewClass alloc] init];	\
}

