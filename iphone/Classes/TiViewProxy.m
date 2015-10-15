/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiViewProxy.h"
#import "LayoutConstraint.h"
#import "TiApp.h"
#import "TiBlob.h"
#import "TiLayoutQueue.h"
#import "TiAction.h"
#import "TiStylesheet.h"
#import "TiLocale.h"
#import "TiUIView.h"
#import "TiTransition.h"
#import "TiWindowProxy.h"
#import "TiApp.h"
#import "TiViewAnimation+Friend.h"
#import "TiViewAnimationStep.h"
#import "TiTransitionAnimation+Friend.h"
#import "TiTransitionAnimationStep.h"

#import "TiUIiOSPreviewContextProxy.h"
#import "TiPreviewingDelegate.h"
#import <QuartzCore/QuartzCore.h>
#import <libkern/OSAtomic.h>
#import <pthread.h>
#import "TiViewController.h"
#import "TiWindowProxy.h"

BOOL TiCGRectIsEmpty(CGRect rect)
{
    return (rect.size.width == 0 && rect.size.height == 0);
}

@interface TiFakeAnimation : TiViewAnimationStep

@end

@implementation TiFakeAnimation

@end

@interface TiViewProxy()
{
    BOOL needsContentChange;
    BOOL allowContentChange;
    BOOL instantUpdates;
	unsigned int animationDelayGuard;
    BOOL _transitioning;
    id _pendingTransition;
    BOOL needsFocusOnAttach;
}
@end

#define IGNORE_IF_NOT_OPENED if ([self viewAttached]==NO) {dirtyflags=0;return;}

@implementation TiViewProxy

@synthesize controller = controller;



#pragma mark public API

@synthesize vzIndex, parentVisible, preventListViewSelection;
-(void)setVzIndex:(NSInteger)newZindex
{
    if(newZindex == vzIndex)
    {
        return;
    }
    
    vzIndex = newZindex;
    [self replaceValue:@(vzIndex) forKey:@"vzIndex" notification:NO];
    [self willChangeZIndex];
}

-(void)setInstantUpdates:(BOOL)value
{
    if(value == instantUpdates)
    {
        return;
    }
    
    instantUpdates = value;
    [self replaceValue:NUMBOOL(instantUpdates) forKey:@"instantUpdates" notification:NO];
}




-(NSString*)apiName
{
    return @"Ti.View";
}

-(void)runBlockOnMainThread:(void (^)(TiViewProxy* proxy))block onlyVisible:(BOOL)onlyVisible recursive:(BOOL)recursive
{
    if ([NSThread isMainThread])
	{
        [self runBlock:block onlyVisible:onlyVisible recursive:recursive];
    }
    else
    {
        TiThreadPerformOnMainThread(^{
            [self runBlock:block onlyVisible:onlyVisible recursive:recursive];
        }, NO);
    }
}

-(void)runBlock:(void (^)(TiViewProxy* proxy))block onlyVisible:(BOOL)onlyVisible recursive:(BOOL)recursive
{
    if (recursive)
    {
        pthread_rwlock_rdlock(&childrenLock);
        [children enumerateObjectsUsingBlock:^(TiProxy* child, NSUInteger idx, BOOL * _Nonnull stop) {
            if(IS_OF_CLASS(child, TiViewProxy) && (!onlyVisible || !((TiViewProxy*)child).isHidden)) {
                block((TiViewProxy*)child);
                [(TiViewProxy*)child runBlock:block onlyVisible:onlyVisible recursive:recursive];
            }
        }];
//        NSArray* subproxies = onlyVisible?[self visibleChildren]:[self viewChildren];
        pthread_rwlock_unlock(&childrenLock);
//        for (TiViewProxy * thisChildProxy in subproxies)
//        {
//            block(thisChildProxy);
//            [thisChildProxy runBlock:block onlyVisible:onlyVisible recursive:recursive];
//        }
    }
}

-(void)makeChildrenPerformSelector:(SEL)selector withObject:(id)object
{
    [[self viewChildren] makeObjectsPerformSelector:selector withObject:object];
}

//-(void)makeVisibleChildrenPerformSelector:(SEL)selector withObject:(id)object
//{
//    [[self visibleChildren] makeObjectsPerformSelector:selector withObject:object];
//}

-(void)setVisible:(NSNumber *)newVisible
{
	[self setHidden:![TiUtils boolValue:newVisible def:YES] withArgs:nil];
	[self replaceValue:newVisible forKey:@"visible" notification:YES];
}

-(void)setTempProperty:(id)propVal forKey:(id)propName {
    if (layoutPropDictionary == nil) {
        layoutPropDictionary = [[NSMutableDictionary alloc] init];
    }
    
    if (propVal != nil && propName != nil) {
        [layoutPropDictionary setObject:propVal forKey:propName];
    }
}

-(void)setProxyObserver:(id)arg
{
    observer = arg;
}

-(void)processTempProperties:(NSDictionary*)arg
{
    //arg will be non nil when called from updateLayout
    if (arg != nil) {
        NSEnumerator *enumerator = [arg keyEnumerator];
        id key;
        while ((key = [enumerator nextObject])) {
            [self setTempProperty:[arg objectForKey:key] forKey:key];
        }
    }
    
    if (layoutPropDictionary != nil) {
        [self setValuesForKeysWithDictionary:layoutPropDictionary];
        RELEASE_TO_NIL(layoutPropDictionary);
    }
}

-(void)applyProperties:(id)args
{
    id data = nil;
    BOOL wait = NO;
    if (IS_OF_CLASS(args, NSDictionary)) {
        data = args;
    } else if (args){
        NSNumber* waitArg = nil;
        ENSURE_ARG_AT_INDEX(data, args, 0, NSObject);
        ENSURE_ARG_OR_NIL_AT_INDEX(waitArg, args, 1, NSNumber);
        if (waitArg != nil) {
            wait = [waitArg boolValue];
        }
        
    }
    
    TiThreadPerformBlockOnMainThread(^{
        [self configurationStart];
        [super applyProperties:data?@[data]:nil];
        [self configurationSet];
        [self refreshViewOrParent];
        [self handlePendingAnimation];
    }, wait);
}

-(void)startLayout:(id)arg
{
    DebugLog(@"startLayout() method is deprecated since 3.0.0 .");
    updateStarted = YES;
    allowLayoutUpdate = NO;
}
-(void)finishLayout:(id)arg
{
    DebugLog(@"finishLayout() method is deprecated since 3.0.0 .");
    updateStarted = NO;
    allowLayoutUpdate = YES;
    [self processTempProperties:nil];
    allowLayoutUpdate = NO;
}
-(void)updateLayout:(id)arg
{
    DebugLog(@"updateLayout() method is deprecated since 3.0.0, use applyProperties() instead.");
    id val = nil;
    if ([arg isKindOfClass:[NSArray class]]) {
        val = [arg objectAtIndex:0];
    }
    else
    {
        val = arg;
    }
    updateStarted = NO;
    allowLayoutUpdate = YES;
    ENSURE_TYPE_OR_NIL(val, NSDictionary);
    [self processTempProperties:val];
    allowLayoutUpdate = NO;
    
}

-(BOOL) belongsToContext:(id<TiEvaluator>) context
{
    id<TiEvaluator> myContext = ([self executionContext]==nil)?[self pageContext]:[self executionContext];
    return (context == myContext);
}

-(void)show:(id)arg
{
	TiThreadPerformOnMainThread(^{
        [self setHidden:NO withArgs:arg];
        [self replaceValue:NUMBOOL(YES) forKey:@"visible" notification:YES];
    }, NO);
}
 
-(void)hide:(id)arg
{
    TiThreadPerformOnMainThread(^{
        [self setHidden:YES withArgs:arg];
        [self replaceValue:NUMBOOL(NO) forKey:@"visible" notification:YES];
    }, NO);
}

#pragma Animations

-(id)animationDelegate
{
    if (parent)
        return [[self viewParent] animationDelegate];
    return nil;
}

-(BOOL)readyToAnimate
{
    return [self viewInitialized] && allowContentChange;
}

-(void)handlePendingAnimation:(TiAnimation*)pendingAnimation
{
    if ([self viewReady]==NO &&  ![pendingAnimation isTransitionAnimation])
	{
		DebugLog(@"[DEBUG] Ti.UI.View.animate() called before view %@ was ready: Will re-attempt", self);
		if (animationDelayGuard++ > 5)
		{
			DebugLog(@"[DEBUG] Animation guard triggered, exceeded timeout to perform animation.");
            [pendingAnimation simulateFinish:self];
            [self handlePendingAnimation];
            animationDelayGuard = 0;
			return;
		}
        dispatch_async(dispatch_get_main_queue(), ^{
            [self performSelector:@selector(handlePendingAnimation:) withObject:pendingAnimation afterDelay:0.01];
        });
		return;
	}
	animationDelayGuard = 0;
    [super handlePendingAnimation:pendingAnimation];
}

-(void)aboutToBeAnimated
{
    if ([view superview]==nil)
    {
        VerboseLog(@"Entering animation without a superview Parent is %@, props are %@",parent,dynprops);
        [self windowWillOpen]; // we need to manually attach the window if you're animating
        [[self viewParent] childWillResize:self];
    }
    
}

-(void)setFakeApplyProperties:(BOOL)newValue
{
    _fakeApplyProperties = newValue;
    if (!_fakeApplyProperties) {
        [self refreshViewOrParent];
    }
}

-(HLSAnimation*)animationForAnimation:(TiAnimation*)animation
{
    TiHLSAnimationStep* step;
    if (animation.isTransitionAnimation) {
        TiTransitionAnimation * hlsAnimation = [TiTransitionAnimation animation];
        hlsAnimation.animatedProxy = self;
        hlsAnimation.animationProxy = animation;
        hlsAnimation.transition = animation.transition;
        hlsAnimation.transitionViewProxy = animation.view;
        step = [TiTransitionAnimationStep animationStep];
        step.duration = [animation getAnimationDuration];
        step.curve = [animation curve];
        [(TiTransitionAnimationStep*)step addTransitionAnimation:hlsAnimation insideHolder:[self getOrCreateView]];
    }
    else {
        TiViewAnimation * hlsAnimation = [TiViewAnimation animation];
        hlsAnimation.animatedProxy = self;
        hlsAnimation.tiViewProxy = self;
        hlsAnimation.animationProxy = animation;
        step = [TiViewAnimationStep animationStep];
        step.duration = [animation getAnimationDuration];
        step.curve = [animation curve];
       [(TiViewAnimationStep*)step addViewAnimation:hlsAnimation forView:self.view];
    }
    
    return [HLSAnimation animationWithAnimationStep:step];
}

-(void)handleAnimation:(TiAnimation*)animation witDelegate:(id)delegate
{
    TiThreadPerformBlockOnMainThread(^{
        [super handleAnimation:animation witDelegate:delegate];
    }, NO);
}
-(void)playAnimation:(HLSAnimation*)animation withRepeatCount:(NSUInteger)repeatCount afterDelay:(double)delay
{
//    TiThreadPerformBlockOnMainThread(^{
        [self refreshViewOrParent];
        [self aboutToBeAnimated];
        [animation playWithRepeatCount:repeatCount afterDelay:delay];
//	}, YES);
}

//override
-(void)animationDidComplete:(TiAnimation *)animation
{
    OSAtomicTestAndClearBarrier(TiRefreshViewEnqueued, &dirtyflags);
    [self willEnqueue];
    [super animationDidComplete:animation];
}

-(void)resetProxyPropertiesForAnimation:(TiAnimation*)animation
{
    TiThreadPerformBlockOnMainThread(^{
        [super resetProxyPropertiesForAnimation:animation];
		[[self viewParent] layoutChildren:NO];
    }, YES);
}

#ifndef TI_USE_AUTOLAYOUT

#define CHECK_LAYOUT_UPDATE(layoutName,value) \
if (ENFORCE_BATCH_UPDATE) { \
    if (updateStarted) { \
        [self setTempProperty:value forKey:@#layoutName]; \
        return; \
    } \
    else if(!allowLayoutUpdate){ \
        return; \
    } \
}

#define LAYOUTPROPERTIES_SETTER_IGNORES_AUTO(methodName,layoutName,converter,postaction)	\
-(void)methodName:(id)value	\
{	\
    CHECK_LAYOUT_UPDATE(layoutName,value) \
    TiDimension result = converter(value);\
    if ( TiDimensionIsDip(result) || TiDimensionIsPercent(result) ) {\
        layoutProperties.layoutName = result;\
    }\
    else {\
        if (!TiDimensionIsUndefined(result)) {\
            DebugLog(@"[WARN] Invalid value %@ specified for property %@",[TiUtils stringValue:value],@#layoutName); \
        } \
        layoutProperties.layoutName = TiDimensionUndefined;\
    }\
    [self replaceValue:value forKey:@#layoutName notification:YES];	\
    postaction; \
}

#define LAYOUTPROPERTIES_SETTER(methodName,layoutName,converter,postaction)	\
-(void)methodName:(id)value	\
{	\
    CHECK_LAYOUT_UPDATE(layoutName,value) \
    layoutProperties.layoutName = converter(value);	\
    [self replaceValue:value forKey:@#layoutName notification:YES];	\
    postaction; \
}

#define TO_BOOL(value) [TiUtils boolValue:value];
#define TO_FLOAT(value) [TiUtils floatValue:value];

#define LAYOUTFLAGS_SETTER(methodName,layoutName,flagName,postaction)	\
-(void)methodName:(id)value	\
{	\
	CHECK_LAYOUT_UPDATE(layoutName,value) \
	layoutProperties.layoutFlags.flagName = [TiUtils boolValue:value];	\
	[self replaceValue:value forKey:@#layoutName notification:NO];	\
	postaction; \
}

LAYOUTPROPERTIES_SETTER_IGNORES_AUTO(setTop,top,TiDimensionFromObject,[self willChangePosition])
LAYOUTPROPERTIES_SETTER_IGNORES_AUTO(setBottom,bottom,TiDimensionFromObject,[self willChangePosition])

LAYOUTPROPERTIES_SETTER_IGNORES_AUTO(setLeft,left,TiDimensionFromObject,[self willChangePosition])
LAYOUTPROPERTIES_SETTER_IGNORES_AUTO(setRight,right,TiDimensionFromObject,[self willChangePosition])

LAYOUTPROPERTIES_SETTER(setWidth,width,TiDimensionFromObject,[self willChangeSize])
LAYOUTPROPERTIES_SETTER(setHeight,height,TiDimensionFromObject,[self willChangeSize])

//-(void)setFullscreen:(id)value
//{
//    CHECK_LAYOUT_UPDATE(layoutName,value)
//    layoutProperties.fullscreen = [TiUtils boolValue:value def:NO];
//    [self replaceValue:value forKey:@"fullscreen" notification:YES];
//    [self willChangeSize];
//    [self willChangePosition];
//}


//-(id)getFullscreen
//{
//    return NUMBOOL(layoutProperties.fullscreen);
//}
//

-(NSArray *)keySequence
{
	static NSArray *keySequence = nil;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		keySequence = [@[@"visible", @"clipChildren", @"backgroundOpacity", @"imageCap"] retain];
	});
	return keySequence;
}

// See below for how we handle setLayout
//LAYOUTPROPERTIES_SETTER(setLayout,layoutStyle,TiLayoutRuleFromObject,[self willChangeLayout])

LAYOUTPROPERTIES_SETTER(setMinWidth,minimumWidth,TiDimensionFromObject,[self willChangeSize])
LAYOUTPROPERTIES_SETTER(setMinHeight,minimumHeight,TiDimensionFromObject,[self willChangeSize])
LAYOUTPROPERTIES_SETTER(setMaxWidth,maximumWidth,TiDimensionFromObject,[self willChangeSize])
LAYOUTPROPERTIES_SETTER(setMaxHeight,maximumHeight,TiDimensionFromObject,[self willChangeSize])
LAYOUTPROPERTIES_SETTER(setLayoutFullscreen,fullscreen,TO_BOOL,[self willChange])
LAYOUTPROPERTIES_SETTER(setSizeRatio,sizeRatio,TO_FLOAT,[self willChangeSize])
LAYOUTPROPERTIES_SETTER(setWeight,weight,TO_FLOAT,[self willChangeSize])

LAYOUTFLAGS_SETTER(setHorizontalWrap,horizontalWrap,horizontalWrap,[self willChangeLayout])

// Special handling to try and avoid Apple's detection of private API 'layout'
-(void)setValue:(id)value forUndefinedKey:(NSString *)key
{
    if ([key isEqualToString:[@"lay" stringByAppendingString:@"out"]]) {
        //CAN NOT USE THE MACRO 
        if (ENFORCE_BATCH_UPDATE) {
            if (updateStarted) {
                [self setTempProperty:value forKey:key]; \
                return;
            }
            else if(!allowLayoutUpdate){
                return;
            }
        }
        layoutProperties.layoutStyle = TiLayoutRuleFromObject(value);
        [self replaceValue:value forKey:[@"lay" stringByAppendingString:@"out"] notification:YES];
        
        [self willChangeLayout];
        return;
    }
    [super setValue:value forUndefinedKey:key];
}

#endif
NSString * GetterStringForKrollProperty(NSString * key)
{
    return [NSString stringWithFormat:@"%@_", key];
}

SEL GetterForKrollProperty(NSString * key)
{
	NSString *method = GetterStringForKrollProperty(key);
	return NSSelectorFromString(method);
}

- (id) valueForKey: (NSString *) key
{
    SEL sel = GetterForKrollProperty(key);
	if ([view respondsToSelector:sel])
	{
		return [view performSelector:sel];
	}
    return [super valueForKey:key];
}

-(id)size
{
	TiRect *rect = [[TiRect alloc] init];
    if ([self viewAttached]) {
        [self makeViewPerformSelector:@selector(fillBoundsToRect:) withObject:rect createIfNeeded:YES waitUntilDone:YES];
        id defaultUnit = [TiApp defaultUnit];
        if ([defaultUnit isKindOfClass:[NSString class]]) {
            [rect convertToUnit:defaultUnit];
        }
    }
    else {
        [rect setRect:CGRectZero];
    }
    NSDictionary* result = [rect toJSON];
    [rect release];
    return result;
}

-(id)rect
{
    __block TiRect *rect = [[TiRect alloc] init];
    TiThreadPerformBlockOnMainThread(^{
        if ([self viewLayedOut]) {
            //        CGPoint viewAnchor = [[ourView layer] anchorPoint];
            CGRect viewRect = self.view.frame;
            //        __block CGRect viewRect;
            //        __block CGPoint viewPosition;
            //        __block CGAffineTransform viewTransform;
            //        __block CGPoint viewAnchor;
            //            TiUIView * ourView = [self view];
            //            viewRect = [ourView bounds];
            //            viewPosition = [ourView center];
            //            viewTransform = [ourView transform];
            //            viewAnchor = [[ourView layer] anchorPoint];
            //        viewRect.origin = CGPointMake(-viewAnchor.x*viewRect.size.width, -viewAnchor.y*viewRect.size.height);
            //        viewRect = CGRectApplyAffineTransform(viewRect, viewTransform);
            //        viewRect.origin.x += viewPosition.x;
            //        viewRect.origin.y += viewPosition.y;
            [rect setRect:viewRect];
            
            id defaultUnit = [TiApp defaultUnit];
            if ([defaultUnit isKindOfClass:[NSString class]]) {
                [rect convertToUnit:defaultUnit];
            }       
        }
        else {
            [rect setRect:CGRectZero];
        }
    }, YES);
    NSDictionary* result = [rect toJSON];
    [rect release];
    return result;
}

-(id)absoluteRect
{
    TiRect *rect = [[TiRect alloc] init];
	if ([self viewAttached]) {
        __block CGRect viewRect;
        __block CGPoint viewPosition;
        __block CGAffineTransform viewTransform;
        __block CGPoint viewAnchor;
        TiThreadPerformOnMainThread(^{
            TiUIView * ourView = [self view];
            viewRect = [ourView bounds];
            viewPosition = [ourView center];
            viewTransform = [ourView transform];
            viewAnchor = [[ourView layer] anchorPoint];
            viewRect.origin = CGPointMake(-viewAnchor.x*viewRect.size.width, -viewAnchor.y*viewRect.size.height);
            viewRect = CGRectApplyAffineTransform(viewRect, viewTransform);
            viewRect.origin.x += viewPosition.x;
            viewRect.origin.y += viewPosition.y;
            viewRect.origin = [ourView convertPoint:CGPointZero toView:nil];
            if (![[UIApplication sharedApplication] isStatusBarHidden])
            {
                CGRect statusFrame = [[UIApplication sharedApplication] statusBarFrame];
                viewRect.origin.y -= statusFrame.size.height;
            }
            
        }, YES);
        [rect setRect:viewRect];
        
        id defaultUnit = [TiApp defaultUnit];
        if ([defaultUnit isKindOfClass:[NSString class]]) {
            [rect convertToUnit:defaultUnit];
        }
    }
    else {
        [rect setRect:CGRectZero];
    }
    NSDictionary* result = [rect toJSON];
    [rect release];
    return result;
}

-(id)zIndex
{
    return [self valueForUndefinedKey:@"vzIndex"];
}

-(void)setZIndex:(id)value
{
    [self setVzIndex:[TiUtils intValue:value def:0]];
//    }
}


-(NSMutableDictionary*)center
{
    NSMutableDictionary* result = [[[NSMutableDictionary alloc] init] autorelease];
    id xVal = [self valueForUndefinedKey:@"centerX_"];
    if (xVal != nil) {
        [result setObject:xVal forKey:@"x"];
    }
    id yVal = [self valueForUndefinedKey:@"centerY_"];
    if (yVal != nil) {
        [result setObject:yVal forKey:@"y"];
    }
    
    if ([[result allKeys] count] > 0) {
        return result;
    }
    return nil;
}

#ifndef TI_USE_AUTOLAYOUT
-(void)setCenter:(id)value
{
    CHECK_LAYOUT_UPDATE(center, value);
    TiPoint* p = [TiUtils tiPointValue:value];
    layoutProperties.centerX = p.xDimension;
    layoutProperties.centerY = p.yDimension;
    [self willChangePosition];
}
#endif

-(id)animatedCenter
{
	if (![self viewAttached])
	{
		return nil;
	}
	__block CGPoint result;
	TiThreadPerformOnMainThread(^{
		UIView * ourView = view;
		CALayer * ourLayer = [ourView layer];
		CALayer * animatedLayer = [ourLayer presentationLayer];
	
		if (animatedLayer !=nil) {
			result = [animatedLayer position];
		}
		else {
			result = [ourLayer position];
		}
	}, YES);
	//TODO: Should this be a TiPoint? If so, the accessor fetcher might try to
	//hold onto the point, which is undesired.
	return [NSDictionary dictionaryWithObjectsAndKeys:NUMFLOAT(result.x),@"x",NUMFLOAT(result.y),@"y", nil];
}

-(void)setBackgroundGradient:(id)arg
{
	TiGradient * newGradient = [TiGradient gradientFromObject:arg proxy:self];
	[self replaceValue:newGradient forKey:@"backgroundGradient" notification:YES];
}

-(UIImage*)toImageWithScale:(CGFloat)scale
{
    TiUIView *myview = [self getAndPrepareViewForOpening];
    CGSize size = myview.bounds.size;
   
    if (CGSizeEqualToSize(size, CGSizeZero) || size.width==0 || size.height==0)
    {
        CGSize size = [self autoSizeForSize:CGSizeMake(1000,1000)];
        if (size.width==0 || size.height == 0)
        {
            size = [UIScreen mainScreen].bounds.size;
        }
        CGRect rect = CGRectMake(0, 0, size.width, size.height);
        [TiUtils setView:myview positionRect:rect];
    }
    scale *= [TiUtils screenScale];
    UIGraphicsBeginImageContextWithOptions(size, [myview.layer isOpaque], scale);
    float oldOpacity = myview.alpha;
    myview.alpha = 1;
    [myview.layer renderInContext:UIGraphicsGetCurrentContext()];
    myview.alpha = oldOpacity;
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return image;
}

-(TiBlob*)toImage:(id)args
{
    KrollCallback *callback = nil;
    float scale = 1.0f;
    
    id obj = nil;
    if( [args count] > 0) {
        obj = [args objectAtIndex:0];
        
        if (obj == [NSNull null]) {
            obj = nil;
        }
        
        if( [args count] > 1) {
            scale = [TiUtils floatValue:[args objectAtIndex:1] def:1.0f];
        }
    }
	ENSURE_SINGLE_ARG_OR_NIL(obj,KrollCallback);
    callback = (KrollCallback*)obj;
	TiBlob *blob = [[[TiBlob alloc] init] autorelease];
	// we spin on the UI thread and have him convert and then add back to the blob
	// if you pass a callback function, we'll run the render asynchronously, if you
	// don't, we'll do it synchronously
	TiThreadPerformOnMainThread(^{
		UIImage *image = [self toImageWithScale:scale];
		[blob setImage:image];
        [blob setMimeType:@"image/png" type:TiBlobTypeImage];
		if (callback != nil)
		{
            NSDictionary *event = [NSDictionary dictionaryWithObject:blob forKey:@"image"];
            [self _fireEventToListener:@"toimage" withObject:event listener:callback thisObject:nil];
		}
	}, (callback==nil));
	
	return blob;
}

-(id)convertPointToView:(id)args
{
    id arg1 = nil;
    TiViewProxy* arg2 = nil;
    ENSURE_ARG_AT_INDEX(arg1, args, 0, NSObject);
    ENSURE_ARG_AT_INDEX(arg2, args, 1, TiViewProxy);
    BOOL validPoint;
    CGPoint oldPoint = [TiUtils pointValue:arg1 valid:&validPoint];
    if (!validPoint) {
        [self throwException:TiExceptionInvalidType subreason:@"Parameter is not convertable to a TiPoint" location:CODELOCATION];
    }
    
    __block BOOL validView = NO;
    __block CGPoint p;
    TiThreadPerformBlockOnMainThread(^{
        if ([self viewLayedOut] && [arg2 viewLayedOut]) {
            validView = YES;
            p = [self.view convertPoint:oldPoint toView:arg2.view];
        }
    }, YES);
    if (!validView) {
        return nil;
    }
    return @{
             @"x":@(p.x),
             @"y":@(p.y)
             };
}


#pragma mark parenting

-(void)childAdded:(TiProxy*)child atIndex:(NSInteger)position shouldRelayout:(BOOL)shouldRelayout
{
    [super childAdded:child atIndex:position shouldRelayout:shouldRelayout];
    if (!IS_OF_CLASS(child, TiViewProxy)){
        return;
    }
    TiThreadPerformBlockOnMainThread(^{
        TiViewProxy* childViewProxy = (TiViewProxy*)child;
        [childViewProxy setReadyToCreateView:YES]; //tableview magic not to create view on proxy creation
        if (!windowOpened || !shouldRelayout || !readyToCreateView)  {
            return;
        }
        if ([childViewProxy isHidden]) {
            return;
        }
        [childViewProxy performBlockWithoutLayout:^{
            [childViewProxy windowWillOpen];
            [childViewProxy getOrCreateView];
            [childViewProxy windowDidOpen];
        }];
        
        //        [self contentsWillChange];
//        if(parentVisible && !hidden)
//        {
//            [childViewProxy parentWillShow];
//        }
        
        //If layout is non absolute push this into the layout queue
        //else just layout the child with current bounds
//        if (allowContentChange) {
            if (![self absoluteLayout]) {
                [self contentsWillChange];
                if (parentVisible && allowContentChange) {
                    [childViewProxy refreshViewOrParent];
                }
            }
            else {
                if (parentVisible && allowContentChange) {
                    [self layoutChild:childViewProxy optimize:NO withMeasuredBounds:[[self view] bounds]];
                } else {
                    [self contentsWillChange];
                }
            }
//        } else {
//            [self parentContentWillChange];
//        }

    }, NO);
}

-(void)childRemoved:(TiProxy*)child wasChild:(BOOL)wasChild shouldDetach:(BOOL)shouldDetach
{
    [super childRemoved:child wasChild:wasChild shouldDetach:shouldDetach];
    if ([self destroyed] || ![child isKindOfClass:[TiViewProxy class]]){
        return;
    }
//    ENSURE_UI_THREAD_WAIT_1_ARG(child);
    TiViewProxy* childViewProxy = (TiViewProxy*)child;
    
    if (shouldDetach) {
        [childViewProxy setProxyObserver:nil];
        [childViewProxy windowWillClose];
        [childViewProxy setParentVisible:NO];
        [childViewProxy windowDidClose]; //will call detach view
    } else {
        [childViewProxy setParentVisible:NO];
    }
    if (!wasChild) return;
    [self parentContentWillChange];
//    BOOL layoutNeedsRearranging = ![self absoluteLayout];
//    if (layoutNeedsRearranging)
//    {
//        [self willChangeLayout];
//    } else {
//        
//    }
}

-(void)removeAllChildren:(id)arg
{
    ENSURE_UI_THREAD_1_ARG(arg);
    [self performBlockWithoutLayout:^{
        [super removeAllChildren:arg];
        [self refreshViewIfNeeded];
    }];
}

//-(void)setParent:(TiParentingProxy*)parent_ checkForOpen:(BOOL)check
//{
//    [super setParent:parent_];
	
//	if (check && parent!=nil && ([parent isKindOfClass:[TiViewProxy class]]) && [[self viewParent] windowHasOpened])
//	{
//		[self windowWillOpen];
//	}
//}

//-(void)setParent:(TiParentingProxy*)parent_
//{
//    [self setParent:parent_ checkForOpen:YES];
//}


-(TiViewProxy*)viewParent
{
    if (IS_OF_CLASS(parent, TiViewProxy) && !isUsingBarButtonItem) {
        return (TiViewProxy*)parent;
    }
    return nil;
}

-(NSArray*)viewChildren
{
    if (childrenCount == 0) return nil;
//    if (![NSThread isMainThread]) {
//        __block NSArray* result = nil;
//        TiThreadPerformOnMainThread(^{
//            result = [[self viewChildren] retain];
//        }, YES);
//        return [result autorelease];
//    }
//    
	pthread_rwlock_rdlock(&childrenLock);
    NSArray* copy = [[children filteredArrayUsingPredicate:[NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) {
        return [object isKindOfClass:[TiViewProxy class]];
    }]] retain];
	pthread_rwlock_unlock(&childrenLock);
	return [copy autorelease];
}
-(void)makeViewChildrenPerformSelector:(SEL)selector withObject:(id)object
{
    [self performBlockOnViewChildren:^(TiViewProxy * child) {
        [child performSelector:selector withObject:object];
    }];
}

-(void)performBlockOnViewChildren:(void (^)(TiViewProxy* object))block
{
    pthread_rwlock_rdlock(&childrenLock);
    [children enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        if (IS_OF_CLASS(obj, TiViewProxy)) {
            block(obj);
        }
    }];
    pthread_rwlock_unlock(&childrenLock);
}


-(NSArray*)visibleChildren
{
    if (childrenCount == 0) return nil;
//    if (![NSThread isMainThread]) {
//        __block NSArray* result = nil;
//        TiThreadPerformOnMainThread(^{
//            result = [[self visibleChildren] retain];
//        }, YES);
//        return [result autorelease];
//    }
	pthread_rwlock_rdlock(&childrenLock);
    NSArray* copy = [[children filteredArrayUsingPredicate:[NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) {
        if (IS_OF_CLASS(object, TiViewProxy)) {
            TiViewProxy* viewProxy = object;
            return !viewProxy.isHidden && !viewProxy.hiddenForLayout;
        }
        return NO;
    }]] retain];
    pthread_rwlock_unlock(&childrenLock);
	return [copy autorelease];
}


#pragma mark nonpublic accessors not related to Housecleaning

@synthesize barButtonItem;

#ifndef TI_USE_AUTOLAYOUT
-(LayoutConstraint *)layoutProperties
{
	return &layoutProperties;
}
#endif

@synthesize sandboxBounds = sandboxBounds;

-(void)setSandboxBounds:(CGRect)rect
{
    if (!TiCGRectIsEmpty(rect) && !CGRectEqualToRect(rect, sandboxBounds))
    {
        sandboxBounds = rect;
//        [self dirtyItAll];
    }
}

-(void)setHidden:(BOOL)newHidden withArgs:(id)args
{
    if (hidden != newHidden) {
        hidden = newHidden;
//            if (!hidden && !view && parent) {
//                [parent childAdded:self atIndex:0 shouldRelayout:YES];
//            }
    }
//	hidden = newHidden;

}

-(BOOL)isHidden
{
    return hidden;
}

//-(CGSize)contentSizeForSize:(CGSize)size
//{
//    return CGSizeZero;
//}

-(CGSize)verifySize:(CGSize)size
{
    CGSize result = size;
    if([self respondsToSelector:@selector(verifyWidth:)])
	{
		result.width = [self verifyWidth:result.width];
	}
    if([self respondsToSelector:@selector(verifyHeight:)])
	{
		result.height = [self verifyHeight:result.height];
	}

    return result;
}
#ifndef TI_USE_AUTOLAYOUT
-(CGSize)autoSizeForSize:(CGSize)size
{
    return [self autoSizeForSize:size ignoreMinMax:NO];
}
-(CGSize)autoSizeForSize:(CGSize)size ignoreMinMax:(BOOL)ignoreMinMaxComputation
{
    if (!ignoreMinMaxComputation) {
        size = minmaxSize(&layoutProperties, size, size);
    }
    CGSize contentSize = CGSizeMake(-1, -1);
    if ([self respondsToSelector:@selector(contentSizeForSize:)]) {
        contentSize = [self contentSizeForSize:size];
    }
    BOOL isAbsolute = [self absoluteLayout];
    CGSize result = CGSizeZero;
    
    CGRect bounds = CGRectZero;
    if (!isAbsolute) {
        bounds.size.width = size.width;
        bounds.size.height = size.height;
        verticalLayoutBoundary = 0;
        horizontalLayoutBoundary = 0;
        horizontalLayoutRowHeight = 0;
    }
	CGRect sandBox = CGRectZero;
    CGSize thisSize = CGSizeZero;
    
    if (childrenCount > 0)
    {
        NSArray* childArray = [self visibleChildren];
        if (isAbsolute)
        {
            for (TiViewProxy* thisChildProxy in childArray)
            {
                thisSize = [thisChildProxy minimumParentSizeForSize:size];
                if(result.width<thisSize.width)
                {
                    result.width = thisSize.width;
                }
                if(result.height<thisSize.height)
                {
                    result.height = thisSize.height;
                }
            }
        }
        else {
            BOOL horizontal =  TiLayoutRuleIsHorizontal(layoutProperties.layoutStyle);
            BOOL vertical =  TiLayoutRuleIsVertical(layoutProperties.layoutStyle);
//            BOOL horizontalNoWrap = horizontal && !TiLayoutFlagsHasHorizontalWrap(&layoutProperties);
            BOOL horizontalWrap = horizontal && TiLayoutFlagsHasHorizontalWrap(&layoutProperties);
            
            NSMutableArray * widthFillChildren = horizontal?[NSMutableArray array]:nil;
            NSMutableArray * heightFillChildren = (vertical || horizontalWrap)?[NSMutableArray array]:nil;
            CGFloat widthNonFill = 0;
            CGFloat heightNonFill = 0;
            
            //First measure the sandbox bounds
            for (TiViewProxy* thisChildProxy in childArray)
            {
                BOOL horizontalFill = [thisChildProxy wantsToFillHorizontalLayout];
                BOOL verticalFill = [thisChildProxy wantsToFillVerticalLayout];
                if (!horizontalWrap)
                {
                    if (widthFillChildren && horizontalFill)
                    {
                        [widthFillChildren addObject:thisChildProxy];
                        continue;
                    }
                    else if (heightFillChildren && verticalFill)
                    {
                        [heightFillChildren addObject:thisChildProxy];
                        continue;
                    }
                }
                sandBox = [self computeChildSandbox:thisChildProxy withBounds:bounds];
                thisSize = CGSizeMake(sandBox.origin.x + sandBox.size.width, sandBox.origin.y + sandBox.size.height);
                if(result.width<thisSize.width)
                {
                    result.width = thisSize.width;
                }
                if(result.height<thisSize.height)
                {
                    result.height = thisSize.height;
                }
            }
            
            NSUInteger nbWidthAutoFill = [widthFillChildren count];
            if (nbWidthAutoFill > 0) {
                CGFloat usableWidth = floorf((size.width - result.width) / nbWidthAutoFill);
                CGRect usableRect = CGRectMake(0,0,usableWidth, size.height);
                for (TiViewProxy* thisChildProxy in widthFillChildren) {
                    sandBox = [self computeChildSandbox:thisChildProxy withBounds:usableRect];
                    thisSize = CGSizeMake(sandBox.origin.x + sandBox.size.width, sandBox.origin.y + sandBox.size.height);
                    if(result.width<thisSize.width)
                    {
                        result.width = thisSize.width;
                    }
                    if(result.height<thisSize.height)
                    {
                        result.height = thisSize.height;
                    }
                }
            }
            
            NSUInteger nbHeightAutoFill = [heightFillChildren count];
            if (nbHeightAutoFill > 0) {
                CGFloat usableHeight = floorf((size.height - result.height) / nbHeightAutoFill);
                CGRect usableRect = CGRectMake(0,0,size.width, usableHeight);
                for (TiViewProxy* thisChildProxy in heightFillChildren) {
                    sandBox = [self computeChildSandbox:thisChildProxy withBounds:usableRect];
                    thisSize = CGSizeMake(sandBox.origin.x + sandBox.size.width, sandBox.origin.y + sandBox.size.height);
                    if(result.width<thisSize.width)
                    {
                        result.width = thisSize.width;
                    }
                    if(result.height<thisSize.height)
                    {
                        result.height = thisSize.height;
                    }
                }
            }
        }
    }
	
    if (result.width < contentSize.width) {
        result.width = contentSize.width;
    }
    if (result.height < contentSize.height) {
        result.height = contentSize.height;
    }
//    if (!ignoreMinMaxComputation) {
//        result = minmaxSize(&layoutProperties, result, size);
//    }

	return [self verifySize:result];
}
#endif

#ifndef TI_USE_AUTOLAYOUT
-(CGSize)sizeForAutoSize:(CGSize)size
{
    if (layoutProperties.fullscreen == YES) return size;
    
    CGFloat suggestedWidth = size.width;
    BOOL followsFillHBehavior = TiDimensionIsAutoFill([self defaultAutoWidthBehavior:nil]);
    CGFloat suggestedHeight = size.height;
    BOOL followsFillWBehavior = TiDimensionIsAutoFill([self defaultAutoHeightBehavior:nil]);
    
    CGFloat offsetx = TiDimensionCalculateValue(layoutProperties.left, size.width)
    + TiDimensionCalculateValue(layoutProperties.right, size.width);
    
    CGFloat offsety = TiDimensionCalculateValue(layoutProperties.top, size.height)
    + TiDimensionCalculateValue(layoutProperties.bottom, size.height);
    
    CGSize result = CGSizeZero;
    
    if (TiDimensionIsDip(layoutProperties.width) || TiDimensionIsPercent(layoutProperties.width))
    {
        result.width =  TiDimensionCalculateValue(layoutProperties.width, suggestedWidth);
    }
    else if (TiDimensionIsAutoFill(layoutProperties.width))
    {
        result.width = size.width;
        result.width -= offsetx;
    }
    else if (TiDimensionIsUndefined(layoutProperties.width))
    {
        if (!TiDimensionIsUndefined(layoutProperties.left) && !TiDimensionIsUndefined(layoutProperties.centerX) ) {
            result.width = 2 * ( TiDimensionCalculateValue(layoutProperties.centerX, suggestedWidth) - TiDimensionCalculateValue(layoutProperties.left, suggestedWidth) );
        }
        else if (!TiDimensionIsUndefined(layoutProperties.left) && !TiDimensionIsUndefined(layoutProperties.right) ) {
            result.width = TiDimensionCalculateMargins(layoutProperties.left, layoutProperties.right, suggestedWidth);
        }
        else if (!TiDimensionIsUndefined(layoutProperties.centerX) && !TiDimensionIsUndefined(layoutProperties.right) ) {
            result.width = 2 * ( size.width - TiDimensionCalculateValue(layoutProperties.right, suggestedWidth) - TiDimensionCalculateValue(layoutProperties.centerX, suggestedWidth));
        }
        else {
            result.width = size.width;
            result.width -= offsetx;
        }
    } else {
        result.width = size.width;
        result.width -= offsetx;
    }
    
    if (TiDimensionIsDip(layoutProperties.height) || TiDimensionIsPercent(layoutProperties.height))        {
        result.height = TiDimensionCalculateValue(layoutProperties.height, suggestedHeight);
    }
    else if (TiDimensionIsAutoFill(layoutProperties.height))
    {
        result.height = size.height;
        result.height -= offsety;
    }
    else if (TiDimensionIsUndefined(layoutProperties.height))
    {
        if (!TiDimensionIsUndefined(layoutProperties.top) && !TiDimensionIsUndefined(layoutProperties.centerY) ) {
            result.height = 2 * ( TiDimensionCalculateValue(layoutProperties.centerY, suggestedHeight) - TiDimensionCalculateValue(layoutProperties.top, suggestedHeight) );
        }
        else if (!TiDimensionIsUndefined(layoutProperties.top) && !TiDimensionIsUndefined(layoutProperties.bottom) ) {
            result.height = TiDimensionCalculateMargins(layoutProperties.top, layoutProperties.bottom, suggestedHeight);
        }
        else if (!TiDimensionIsUndefined(layoutProperties.centerY) && !TiDimensionIsUndefined(layoutProperties.bottom) ) {
            result.height = 2 * ( suggestedHeight - TiDimensionCalculateValue(layoutProperties.bottom, suggestedHeight) - TiDimensionCalculateValue(layoutProperties.centerY, suggestedHeight));
        }
        else{
            result.height = size.height;
            result.height -= offsety;
        }
    } else {
        result.height = size.height;
        result.height -= offsety;
    }
    result = minmaxSize(&layoutProperties, result, size);
    return result;
}

-(CGSize)minimumParentSizeForSize:(CGSize)size
{

    CGFloat offsetx = TiDimensionCalculateValue(layoutProperties.left, size.width)
    + TiDimensionCalculateValue(layoutProperties.right, size.width);
    
    CGFloat offsety = TiDimensionCalculateValue(layoutProperties.top, size.height)
    + TiDimensionCalculateValue(layoutProperties.bottom, size.height);
    
    CGSize result = [self minimumParentSizeForSizeNoPadding:size];
    result.width += offsetx;
    result.height += offsety;
    
    return result;
}
#endif

#ifndef TI_USE_AUTOLAYOUT
-(CGSize)minimumParentSizeForSizeNoPadding:(CGSize)size
{
    if (layoutProperties.fullscreen == YES) return size;
    
    CGSize suggestedSize = size;
    BOOL followsFillWidthBehavior = TiDimensionIsAutoFill([self defaultAutoWidthBehavior:nil]);
    BOOL followsFillHeightBehavior = TiDimensionIsAutoFill([self defaultAutoHeightBehavior:nil]);
    BOOL recheckForFillW = NO, recheckForFillH = NO;
    
    BOOL autoComputed = NO;
    CGSize autoSize = [self sizeForAutoSize:size];
    //    //Ensure that autoHeightForSize is called with the lowest limiting bound
    //    CGFloat desiredWidth = MIN([self minimumParentWidthForSize:size],size.width);
    
    CGSize result = CGSizeMake(0, 0);

	if (TiDimensionIsDip(layoutProperties.width) || TiDimensionIsPercent(layoutProperties.width))
	{
		result.width += TiDimensionCalculateValue(layoutProperties.width, suggestedSize.width);
	}
	else if (TiDimensionIsAutoFill(layoutProperties.width) || (TiDimensionIsAuto(layoutProperties.width) && followsFillWidthBehavior) )
	{
		result.width = suggestedSize.width;
	}
    else if (followsFillWidthBehavior && TiDimensionIsUndefined(layoutProperties.width))
    {
        if (!TiDimensionIsUndefined(layoutProperties.left) && !TiDimensionIsUndefined(layoutProperties.centerX) ) {
            result.width += 2 * ( TiDimensionCalculateValue(layoutProperties.centerX, suggestedSize.width) - TiDimensionCalculateValue(layoutProperties.left, suggestedSize.width) );
        }
        else if (!TiDimensionIsUndefined(layoutProperties.left) && !TiDimensionIsUndefined(layoutProperties.right) ) {
            result.width += TiDimensionCalculateMargins(layoutProperties.left, layoutProperties.right, suggestedSize.width);
        }
        else if (!TiDimensionIsUndefined(layoutProperties.centerX) && !TiDimensionIsUndefined(layoutProperties.right) ) {
            result.width += 2 * ( size.width - TiDimensionCalculateValue(layoutProperties.right, suggestedSize.width) - TiDimensionCalculateValue(layoutProperties.centerX, suggestedSize.width));
        }
        else {
            recheckForFillW = followsFillWidthBehavior;
//            autoComputed = YES;
//            autoSize = [self autoSizeForSize:autoSize];
            result.width = suggestedSize.width;
        }
    }
	else
	{
		autoComputed = YES;
        autoSize = [self autoSizeForSize:autoSize];
        result.width += autoSize.width;
	}
    if (recheckForFillW && (result.width < suggestedSize.width) ) {
        result.width = suggestedSize.width;
    }
    
    
    if (TiDimensionIsDip(layoutProperties.height) || TiDimensionIsPercent(layoutProperties.height))	{
		result.height += TiDimensionCalculateValue(layoutProperties.height, suggestedSize.height);
	}
    else if (TiDimensionIsAutoFill(layoutProperties.height) || (TiDimensionIsAuto(layoutProperties.height) && followsFillHeightBehavior) )
	{
		recheckForFillH = YES;
        if (autoComputed == NO) {
            autoComputed = YES;
            autoSize = [self autoSizeForSize:autoSize];
        }
		result.height += autoSize.height;
	}
    else if (followsFillHeightBehavior && TiDimensionIsUndefined(layoutProperties.height))
    {
        if (!TiDimensionIsUndefined(layoutProperties.top) && !TiDimensionIsUndefined(layoutProperties.centerY) ) {
            result.height += 2 * ( TiDimensionCalculateValue(layoutProperties.centerY, suggestedSize.height) - TiDimensionCalculateValue(layoutProperties.top, suggestedSize.height) );
        }
        else if (!TiDimensionIsUndefined(layoutProperties.top) && !TiDimensionIsUndefined(layoutProperties.bottom) ) {
            result.height += TiDimensionCalculateMargins(layoutProperties.top, layoutProperties.bottom, suggestedSize.height);
        }
        else if (!TiDimensionIsUndefined(layoutProperties.centerY) && !TiDimensionIsUndefined(layoutProperties.bottom) ) {
            result.height += 2 * ( suggestedSize.height - TiDimensionCalculateValue(layoutProperties.bottom, suggestedSize.height) - TiDimensionCalculateValue(layoutProperties.centerY, suggestedSize.height));
        }
        else {
            recheckForFillH = followsFillHeightBehavior;
//            if (autoComputed == NO) {
//                autoComputed = YES;
//                autoSize = [self autoSizeForSize:autoSize];
//            }
            result.height = suggestedSize.height;
//            result.height += autoSize.height;
        }
    }
	else
	{
		if (autoComputed == NO) {
            autoComputed = YES;
            autoSize = [self autoSizeForSize:autoSize];
        }
		result.height += autoSize.height;
	}
    if (recheckForFillH && (result.height < suggestedSize.height) ) {
        result.height = suggestedSize.height;
    }
    result = minmaxSize(&layoutProperties, result, size);
    
	return result;
}
#endif


-(UIBarButtonItem*)barButtonItemForController:(UINavigationController*)navController
{
	if (barButtonItem == nil)
	{
		isUsingBarButtonItem = YES;
		barButtonItem = [[UIBarButtonItem alloc] initWithCustomView:[self barButtonViewForRect:navController.navigationBar.bounds]];
	}
	return barButtonItem;
}

-(UIBarButtonItem*)barButtonItem
{
	return [self barButtonItemForRect:CGRectZero];
}

-(UIBarButtonItem*)barButtonItemForRect:(CGRect)bounds
{
	if (barButtonItem == nil)
	{
		isUsingBarButtonItem = YES;
		barButtonItem = [[UIBarButtonItem alloc] initWithCustomView:[self barButtonViewForRect:bounds]];
	}
	return barButtonItem;
}

- (TiUIView *)barButtonViewForRect:(CGRect)bounds
{
#ifndef TI_USE_AUTOLAYOUT
    self.canBeResizedByFrame = YES;
    isUsingBarButtonItem = YES;
    //TODO: This logic should have a good place in case that refreshLayout is used.
//	LayoutConstraint barButtonLayout = layoutProperties;
	if (TiDimensionIsUndefined(layoutProperties.width))
	{
		layoutProperties.width = TiDimensionAutoSize;
        
	}
	if (TiDimensionIsUndefined(layoutProperties.height))
	{
		layoutProperties.height = TiDimensionAutoSize;
	}
#endif
    return [self getAndPrepareViewForOpening:bounds];
}

- (TiUIView *)barButtonViewForSize:(CGSize)size
{
    return [self barButtonViewForRect:CGRectMake(0, 0, size.width, size.height)];
}


#pragma mark Recognizers

//supposed to be called on init
-(void)setDefaultReadyToCreateView:(BOOL)ready
{
    defaultReadyToCreateView = readyToCreateView = ready;
}


-(void)setReadyToCreateViewNSNumber:(NSNumber*)ready
{
    [self setReadyToCreateView:[ready boolValue]];
}

-(void)setReadyToCreateView:(BOOL)ready
{
    [self setReadyToCreateView:YES recursive:YES];
}

-(void)setReadyToCreateView:(BOOL)ready recursive:(BOOL)recursive
{
    readyToCreateView = ready;
    if (!recursive) return;
    [self makeViewChildrenPerformSelector:@selector(setReadyToCreateViewNSNumber:) withObject:@(ready)];
}

-(TiUIView*)getOrCreateView
{
    readyToCreateView = YES;
    return [self view];
}

-(TiUIView*) getAndPrepareViewForOpening:(CGRect)bounds
{
    if([self viewAttached]) {
        if (!CGRectEqualToRect(bounds, self.sandboxBounds)) {
            [self setSandboxBounds:bounds];
            if (!TiCGRectIsEmpty(sandboxBounds))
            {
                [self refreshView];
                [self handlePendingAnimation];
            }
        }
        
        return view;
    }
    [self setSandboxBounds:bounds];
    [self parentWillShow];
    [self windowWillOpen];
    if (view) {
        [self setSandboxBounds:bounds];
        if (!TiCGRectIsEmpty(sandboxBounds))
        {
            [self refreshView];
            [self handlePendingAnimation];
        }
    } else {
        [self getOrCreateView];
        [self windowDidOpen];
    }
    return view;
}


-(TiUIView*) getAndPrepareViewForOpening
{
    if([self viewAttached]) return view;
    [self determineSandboxBoundsForce];
    [self parentWillShow];
    [self windowWillOpen];
    TiUIView* tiview = [self getOrCreateView];
    [self windowDidOpen];
    return tiview;
}


-(void)determineSandboxBoundsForce
{
    if(!TiCGRectIsEmpty(sandboxBounds)) return;
    if(!TiCGRectIsEmpty(view.bounds)){
        [self setSandboxBounds:view.bounds];
    }
    else if (!TiCGRectIsEmpty(sizeCache)) {
        [self setSandboxBounds:sizeCache];
    }
    else if (parent != nil) {
        CGRect bounds = [[[self viewParent] view] bounds];
        if (!CGRectIsEmpty(bounds)){
            [self setSandboxBounds:bounds];
        }
        else [self setSandboxBounds:([self viewParent]).sandboxBounds];
    } else {
        [self computeBoundsForParentBounds:CGRectZero];
        [self setSandboxBounds:sizeCache];
    }
}

-(TiUIView*)view
{
	if (view == nil && readyToCreateView)
	{
		WARN_IF_BACKGROUND_THREAD_OBJ
#ifdef VERBOSE
		if(![NSThread isMainThread])
		{
			NSLog(@"[WARN] Break here");
		}
#endif		
		// on open we need to create a new view
		[self viewWillInitialize];
		view = [self newView];

#ifdef TI_USE_AUTOLAYOUT
        if ([self respondsToSelector:@selector(defaultAutoWidthBehavior:)]) {
            [view setDefaultWidth:[self defaultAutoWidthBehavior:nil]];
        }
        if ([self respondsToSelector:@selector(defaultAutoHeightBehavior:)]) {
            [view setDefaultHeight:[self defaultAutoHeightBehavior:nil]];
        }
#endif
        view.proxy = self;
		view.layer.transform = CATransform3DIdentity;
		view.transform = CGAffineTransformIdentity;
        view.hidden = hidden;

		[view initializeState];

        [self configurationStart];
		// fire property changes for all properties to our delegate
		[self firePropertyChanges];

		[self configurationSet];
        
        [self performBlockOnViewChildren:^(TiViewProxy *child) {
            TiUIView *childView = [(TiViewProxy*)child getOrCreateView];
            [self insertSubview:childView forProxy:child];
        }];

		viewInitialized = YES;
		[self viewDidInitialize];
		// If parent has a non absolute layout signal the parent that
		//contents will change else just lay ourselves out
//		if (parent != nil && ![parent absoluteLayout]) {
//			[parent contentsWillChange];
//		}
//		else {
#ifndef TI_USE_AUTOLAYOUT
			if(TiCGRectIsEmpty(sandboxBounds) && !TiCGRectIsEmpty(view.bounds)){
                [self setSandboxBounds:view.bounds];
			}
#endif
//            [self dirtyItAll];
//            [self refreshViewIfNeeded];
//		}
        if (!TiCGRectIsEmpty(sandboxBounds))
        {
            [self refreshView];
            [self handlePendingAnimation];
        }
        
        if (windowOpening || windowOpened) {
            [self viewDidAttach];
        }
	}

	CGRect bounds = [view bounds];
	if (!CGPointEqualToPoint(bounds.origin, CGPointZero))
	{
		[view setBounds:CGRectMake(0, 0, bounds.size.width, bounds.size.height)];
	}
	
	return view;
}

- (void)prepareForReuse
{
    [self makeViewChildrenPerformSelector:@selector(prepareForReuse) withObject:nil];
    [self makeChildrenPerformSelector:@selector(prepareForReuse) withObject:nil];
}

-(void)clearViewNSNumber:(NSNumber*)recurse
{
    [self clearView:[recurse boolValue]];
}

//CAUTION: TO BE USED ONLY WITH TABLEVIEW MAGIC
-(void)clearView:(BOOL)recurse
{
    [self setView:nil];
    if (recurse) {
        [self makeViewChildrenPerformSelector:@selector(clearViewNSNumber:) withObject:@(recurse)];
    }
}

//CAUTION: TO BE USED ONLY WITH TABLEVIEW MAGIC
-(void)setView:(TiUIView *)newView
{
    if (view == newView) return;
    
    RELEASE_TO_NIL(view)
    
    if (self.modelDelegate!=nil)
    {
        if ([self.modelDelegate respondsToSelector:@selector(detachProxy)])
            [self.modelDelegate detachProxy];
        self.modelDelegate = nil;
    }
    
    if (newView == nil)
        readyToCreateView = defaultReadyToCreateView;
    else {
        view = [newView retain];
        self.modelDelegate = newView;
    }
}

//USED WITH TABLEVIEW MAGIC
//-(void)processPendingAdds
//{
//    pthread_rwlock_rdlock(&childrenLock);
//    for (TiViewProxy* child in [self children]) {
//        [child processPendingAdds];
//    }
//    
//    pthread_rwlock_unlock(&childrenLock);
//    if (pendingAdds != nil)
//    {
//        for (id child in pendingAdds)
//        {
//            [(TiViewProxy*)child processPendingAdds];
//            [self add:child];
//        }
//		RELEASE_TO_NIL(pendingAdds);
//    }
//}

//CAUTION: TO BE USED ONLY WITH TABLEVIEW MAGIC
-(void)fakeOpening
{
    windowOpened = parentVisible = YES;
}

-(NSMutableDictionary*)langConversionTable
{
    return nil;
}

#pragma mark Methods subclasses should override for behavior changes
-(BOOL)optimizeSubviewInsertion
{
    //Return YES for any view that implements a wrapperView that is a TiUIView (Button and ScrollView currently) and a basic view
    return ( [view isMemberOfClass:[TiUIView class]] ) ;
}

-(BOOL)suppressesRelayout
{
    if (controller != nil) {
        //If controller view is not loaded, sandbox bounds will become zero.
        //In that case we do not want to mess up our sandbox, which is by default
        //mainscreen bounds. It will adjust when view loads.
        return [controller isViewLoaded];
    }
	return NO;
}

-(BOOL)supportsNavBarPositioning
{
	return YES;
}

// TODO: Re-evaluate this along with the other controller propagation mechanisms, post 1.3.0.
// Returns YES for anything that can have a UIController object in its parent view
-(BOOL)canHaveControllerParent
{
	return YES;
}

-(BOOL)shouldDetachViewOnUnload
{
	return YES;
}

-(UIView *)parentViewForChild:(TiViewProxy *)child
{
	return [view parentViewForChildren];
}

-(TiWindowProxy*)getParentWindow
{
    if (parent) {
        if ([parent isKindOfClass:[TiWindowProxy class]])
        {
            return (TiWindowProxy*)parent;
        }
        else {
            return [[self viewParent] getParentWindow];
        }
    }
    return nil;
}

-(UIViewController*)getContentController
{
    if (controller) {
        return controller;
    }
    if (parent) {
        return [[self viewParent] getContentController];
    }
    return nil;
}

#pragma mark Event trigger methods

-(void)windowWillOpen
{

	
	// this method is called just before the top level window
	// that this proxy is part of will open and is ready for
	// the views to be attached
	
	if (windowOpened==YES)
	{
		return;
	}
	
	windowOpened = YES;
	windowOpening = YES;
    
    if (view != nil) {
        [self viewDidAttach];
    }
	// If the window was previously opened, it may need to have
	// its existing children redrawn
	// Maybe need to call layout children instead for non absolute layout
    [self makeViewChildrenPerformSelector:@selector(windowWillOpen) withObject:nil];


    //TODO: This should be properly handled and moved, but for now, let's force it (Redundantly, I know.)
	if (parent != nil) {
		[self parentWillShow];
	}
}

-(void)windowDidOpen
{
	windowOpening = NO;
    [self makeViewChildrenPerformSelector:@selector(windowDidOpen) withObject:nil];
}

-(void)windowWillClose
{
    [self makeViewChildrenPerformSelector:@selector(windowWillClose) withObject:nil];
}

-(void)windowDidClose
{
    if (controller) {
        [controller removeFromParentViewController];
        RELEASE_TO_NIL_AUTORELEASE(controller);
    }
    [self makeViewChildrenPerformSelector:@selector(windowDidClose) withObject:nil];
	[self detachView:NO];
	windowOpened=NO;
}


-(void)willFirePropertyChanges
{
	// for subclasses
	if ([view respondsToSelector:@selector(willFirePropertyChanges)])
	{
		[view performSelector:@selector(willFirePropertyChanges)];
	}
}

-(void)didFirePropertyChanges
{
	// for subclasses
	if ([view respondsToSelector:@selector(didFirePropertyChanges)])
	{
		[view performSelector:@selector(didFirePropertyChanges)];
	}
}

-(void)viewWillInitialize
{
	// for subclasses
}

-(void)viewDidInitialize
{
	// for subclasses
}

-(void)viewDidAttach
{
    if (needsFocusOnAttach) {
        [self focus:nil];
        needsFocusOnAttach = NO;
    }
	// for subclasses
}


-(void)viewWillDetach
{
	// for subclasses
}

-(void)viewDidDetach
{
	// for subclasses
}

-(void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    //For various views (scrollableView, NavGroup etc this info neeeds to be forwarded)
    NSArray* childProxies = [self viewChildren];
	for (TiViewProxy * thisProxy in childProxies)
	{
		if ([thisProxy respondsToSelector:@selector(willAnimateRotationToInterfaceOrientation:duration:)])
		{
			[(id)thisProxy willAnimateRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];
		}
	}
}

-(void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    //For various views (scrollableView, NavGroup etc this info neeeds to be forwarded)
    NSArray* childProxies = [self viewChildren];
	for (TiViewProxy * thisProxy in childProxies)
	{
		if ([thisProxy respondsToSelector:@selector(willRotateToInterfaceOrientation:duration:)])
		{
			[(id)thisProxy willRotateToInterfaceOrientation:toInterfaceOrientation duration:duration];
		}
	}
}

-(void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation
{
    //For various views (scrollableView, NavGroup etc this info neeeds to be forwarded)
    NSArray* childProxies = [self viewChildren];
	for (TiViewProxy * thisProxy in childProxies)
	{
		if ([thisProxy respondsToSelector:@selector(didRotateFromInterfaceOrientation:)])
		{
			[(id)thisProxy didRotateFromInterfaceOrientation:fromInterfaceOrientation];
		}
	}
}

#pragma mark Housecleaning state accessors

-(BOOL)viewHasSuperview:(UIView *)superview
{
	return [(UIView *)view superview] == superview;
}

-(BOOL)viewAttached
{
	return view!=nil && windowOpened;
}

-(BOOL)viewLayedOut
{
    CGRect rectToTest = parent?sizeCache:[[self view] bounds];
    return (rectToTest.size.width != 0 || rectToTest.size.height != 0);
}

//TODO: When swapping about proxies, views are uninitialized, aren't they?
-(BOOL)viewInitialized
{
	return viewInitialized && (view != nil);
}

-(BOOL)viewReady
{
	return view!=nil &&
			CGRectIsNull([view layer].bounds)==NO &&
			[[view layer] superlayer] != nil;
}

-(BOOL)windowHasOpened
{
	return windowOpened;
}

-(void)setPreviewContext:(id)context
{
#if IS_XCODE_7
#ifdef USE_TI_UIIOSPREVIEWCONTEXT
    
    if ([TiUtils forceTouchSupported] == NO) {
        NSLog(@"[WARN] 3DTouch is not available on this device.");
        return;
    }
    
    ENSURE_TYPE(context, TiUIiOSPreviewContextProxy);
    
    [context setSourceView:self];
    [context connectToDelegate];

#endif
#endif
}

-(BOOL)windowIsOpening
{
	return windowOpening;
}

- (BOOL) isUsingBarButtonItem
{
	return isUsingBarButtonItem;
}

#pragma mark Building up and Tearing down

-(void)resetDefaultValues
{
    autoresizeCache = UIViewAutoresizingNone;
    sizeCache = CGRectZero;
    sandboxBounds = CGRectZero;
    positionCache = CGPointZero;
    repositioning = NO;
    parentVisible = NO;
    preventListViewSelection = NO;
    viewInitialized = NO;
    readyToCreateView = defaultReadyToCreateView;
    windowOpened = NO;
    windowOpening = NO;
    dirtyflags = 0;
    allowContentChange = YES;
    needsContentChange = NO;
}

-(id)init
{
	if ((self = [super init]))
	{
		destroyLock = [[NSRecursiveLock alloc] init];
		_bubbleParent = YES;
        defaultReadyToCreateView = NO;
        hidden = NO;
        _hiddenForLayout = NO;
        [self resetDefaultValues];
        _transitioning = NO;
        vzIndex = 0;
        instantUpdates = NO;
        _canBeResizedByFrame = NO;
        _canRepositionItself = YES;
        _canResizeItself = YES;
        needsFocusOnAttach = NO;
	}
	return self;
}

-(void)_configure
{
    [self replaceValue:@(YES) forKey:@"enabled" notification:NO];
    [self replaceValue:@(NO) forKey:@"fullscreen" notification:NO];
    [self replaceValue:@(YES) forKey:@"visible" notification:NO];
    [self replaceValue:@(FALSE) forKey:@"opaque" notification:NO];
    [self replaceValue:@(1.0f) forKey:@"opacity" notification:NO];
}

-(void)_initWithProperties:(NSDictionary*)properties
{
    updateStarted = YES;
    allowLayoutUpdate = NO;
	// Set horizontal layout wrap:true as default 
#ifndef TI_USE_AUTOLAYOUT
	layoutProperties.layoutFlags.horizontalWrap = NO;
    layoutProperties.fullscreen = NO;
    layoutProperties.weight = 1.0f;
    layoutProperties.sizeRatio = 0.0f;
#endif
	[self initializeProperty:@"visible" defaultValue:NUMBOOL(YES)];

	if (properties!=nil)
	{
        NSNumber* isVisible = [properties objectForKey:@"visible"];
        hidden = ![TiUtils boolValue:isVisible def:YES];
        
		NSString *objectId = [properties objectForKey:@"id"];
		NSString* className = [properties objectForKey:@"className"];
		NSMutableArray* classNames = [properties objectForKey:@"classNames"];
		
		NSString *type = [NSStringFromClass([self class]) stringByReplacingOccurrencesOfString:@"TiUI" withString:@""];
		type = [[type stringByReplacingOccurrencesOfString:@"Proxy" withString:@""] lowercaseString];

		TiStylesheet *stylesheet = [[[self pageContext] host] stylesheet];
		NSString *basename = [[self pageContext] basename];
		NSString *density = [TiUtils isRetinaDisplay] ? @"high" : @"medium";

		if (objectId!=nil || className != nil || classNames != nil || [stylesheet basename:basename density:density hasTag:type])
		{
			// get classes from proxy
			NSString *className = [properties objectForKey:@"className"];
			NSMutableArray *classNames = [properties objectForKey:@"classNames"];
			if (classNames==nil)
			{
				classNames = [NSMutableArray arrayWithCapacity:1];
			}
			if (className!=nil)
			{
				[classNames addObject:className];
			}

		    
		    NSDictionary *merge = [stylesheet stylesheet:objectId density:density basename:basename classes:classNames tags:[NSArray arrayWithObject:type]];
			if (merge!=nil)
			{
				// incoming keys take precendence over existing stylesheet keys
				NSMutableDictionary *dict = [NSMutableDictionary dictionaryWithDictionary:merge];
				[dict addEntriesFromDictionary:properties];
                
				properties = dict;
			}
		}
		// do a translation of language driven keys to their converted counterparts
		// for example titleid should look up the title in the Locale
		NSMutableDictionary *table = [self langConversionTable];
		if (table!=nil)
		{
			for (id key in table)
			{
				// determine which key in the lang table we need to use
				// from the lang property conversion key
				id langKey = [properties objectForKey:key];
				if (langKey!=nil)
				{
					// eg. titleid -> title
					id convertKey = [table objectForKey:key];
					// check and make sure we don't already have that key
					// since you can't override it if already present
					if ([properties objectForKey:convertKey]==nil)
					{
						id newValue = [TiLocale getString:langKey comment:nil];
						if (newValue!=nil)
						{
							[(NSMutableDictionary*)properties setObject:newValue forKey:convertKey];
						}
					}
				}
			}
		}
	}
	[super _initWithProperties:properties];
    updateStarted = NO;
    allowLayoutUpdate = YES;
    [self processTempProperties:nil];
    allowLayoutUpdate = NO;

}

-(void)dealloc
{
    if (controller != nil) {
        [controller detachProxy]; //make the controller knows we are done
#ifdef TI_USE_KROLL_THREAD
        TiThreadReleaseOnMainThread(controller, NO);
        controller = nil;
#else
        TiThreadPerformOnMainThread(^{
            RELEASE_TO_NIL(controller);
        }, YES);
#endif
    }
	RELEASE_TO_NIL(destroyLock);
	
	[super dealloc];
}


-(void)viewWillAppear:(BOOL)animated
{
    [self willShow];
//    [self refreshViewIfNeeded];
//    [self runBlock:^(TiViewProxy *proxy) {
//        [proxy viewWillAppear:animated];
//    } onlyVisible:NO recursive:YES];
}

-(void)viewDidAppear:(BOOL)animated
{
//    [self runBlock:^(TiViewProxy *proxy) {
//        [proxy viewDidAppear:animated];
//    } onlyVisible:NO recursive:YES];
}

-(void)viewWillDisappear:(BOOL)animated
{
    [self willHide];
//    [self runBlock:^(TiViewProxy *proxy) {
//        [proxy viewWillDisappear:animated];
//    } onlyVisible:NO recursive:YES];
}

-(void)viewDidDisappear:(BOOL)animated
{
//    [self runBlock:^(TiViewProxy *proxy) {
//        [proxy viewDidDisappear:animated];
//    } onlyVisible:NO recursive:YES];
}

-(UIViewController*)hostingController;
{
    if (controller == nil) {
        controller = [[TiViewController alloc] initWithViewProxy:self];
    }
    return controller;
}

-(BOOL)retainsJsObjectForKey:(NSString *)key
{
	return ![key isEqualToString:@"animation"];
}

-(void)firePropertyChanges
{
	[self willFirePropertyChanges];
	
	if ([view respondsToSelector:@selector(readProxyValuesWithKeys:)]) {
		id<NSFastEnumeration> values = [self allKeys];
		[view readProxyValuesWithKeys:values];
	}

	[self didFirePropertyChanges];
}

-(TiUIView*)newView
{
    TiUIView* newview = nil;
	NSString * proxyName = NSStringFromClass([self class]);
	if ([proxyName hasSuffix:@"Proxy"]) 
	{
		Class viewClass = nil;
		NSString * className = [proxyName substringToIndex:[proxyName length]-5];
		viewClass = NSClassFromString(className);
		if (viewClass != nil)
		{
			return [[viewClass alloc] init];
		}
	}
	else
	{
		DeveloperLog(@"[WARN] No TiView for Proxy: %@, couldn't find class: %@",self,proxyName);
	}
    return [[TiUIView alloc] init];
}


-(void)detachView
{
	[self detachView:YES];
}

-(void)detachView:(BOOL)recursive
{
	[destroyLock lock];
    [self clearAnimations];
    
    if(recursive)
    {
        [self makeViewChildrenPerformSelector:@selector(detachView) withObject:nil];
    }
    
    [self removeBarButtonView];
    
	if (view!=nil)
	{
		[self viewWillDetach];
//        [self cancelAllAnimations:nil];
		[view removeFromSuperview];
		view.proxy = nil;
        view.touchDelegate = nil;
		RELEASE_TO_NIL(view);
		[self viewDidDetach];
	}
    if (self.modelDelegate!=nil)
    {
        if ([self.modelDelegate respondsToSelector:@selector(detachProxy)])
            [self.modelDelegate detachProxy];
        self.modelDelegate = nil;
    }
	[destroyLock unlock];
    [self resetDefaultValues];

}

-(void)_destroy
{
	[destroyLock lock];
	if ([self destroyed])
	{
		// not safe to do multiple times given rwlock
		[destroyLock unlock];
		return;
	}

	//Part of super's _destroy is to release the modelDelegate, which in our case is ALSO the view.
	//As such, we need to have the super happen before we release the view, so that we can insure that the
	//release that triggers the dealloc happens on the main thread.
	
	if (barButtonItem != nil)
	{
		if ([NSThread isMainThread])
		{
			RELEASE_TO_NIL(barButtonItem);
		}
		else
		{
#ifdef TI_USE_KROLL_THREAD
			TiThreadReleaseOnMainThread(barButtonItem, NO);
			barButtonItem = nil;
#else
            TiThreadPerformOnMainThread(^{
                RELEASE_TO_NIL(barButtonItem);
            }, NO);
#endif
		}
	}

	if (view!=nil)
	{
		if ([NSThread isMainThread])
		{
			[self detachView];
		}
		else
		{
			view.proxy = nil;
#ifdef TI_USE_KROLL_THREAD
			TiThreadReleaseOnMainThread(view, NO);
			view = nil;
#else
            TiThreadPerformOnMainThread(^{
                RELEASE_TO_NIL(view);
            }, YES);
#endif
		}
	}
    
    // _destroy is called during a JS context shutdown, to inform the object to
    // release all its memory and references.  this will then cause dealloc
    // on objects that it contains (assuming we don't have circular references)
    // since some of these objects are registered in the context and thus still
    // reachable, we need _destroy to help us start the unreferencing part
    [super _destroy];
    
	[destroyLock unlock];
}

-(void)destroy
{
	//FIXME- me already have a _destroy, refactor this
	[self _destroy];
}

-(void)removeBarButtonView
{
    self.canBeResizedByFrame = NO;
	isUsingBarButtonItem = NO;
	[self setBarButtonItem:nil];
}

#pragma mark Callbacks

-(void)didReceiveMemoryWarning:(NSNotification*)notification
{
	// Only release a view if we're the only living reference for it
	// WARNING: do not call [self view] here as that will create the
	// view if it doesn't yet exist (thus defeating the purpose of
	// this method)
	
	//NOTE: for now, we're going to have to turn this off until post
	//1.4 where we can figure out why the drawing is screwed up since
	//the views aren't reattaching.  
	/*
	if (view!=nil && [view retainCount]==1)
	{
		[self detachView];
	}*/
	[super didReceiveMemoryWarning:notification];
}

-(void)makeViewPerformSelector:(SEL)selector withObject:(id)object createIfNeeded:(BOOL)create waitUntilDone:(BOOL)wait
{
	BOOL isAttached = [self viewAttached];
	
	if(!isAttached && !create)
	{
		return;
	}
	TiThreadPerformBlockOnMainThread(^{
		[[self getOrCreateView] performSelector:selector withObject:object];
	}, wait);
}

#pragma mark Listener Management

-(void)parentListenersChanged
{
    TiThreadPerformOnMainThread(^{
        if (view != nil && [view respondsToSelector:@selector(updateTouchHandling)]) {
            [view updateTouchHandling];
        }
    }, NO);
}

-(void)_listenerAdded:(NSString*)type count:(NSInteger)count
{
	if (self.modelDelegate!=nil && [(NSObject*)self.modelDelegate respondsToSelector:@selector(listenerAdded:count:)])
	{
		[self.modelDelegate listenerAdded:type count:count];
	}
	else if(view!=nil) // don't create the view if not already realized
	{
		if ([self.view respondsToSelector:@selector(listenerAdded:count:)]) {
			[self.view listenerAdded:type count:count];
		}
	}
    
    [super _listenerAdded:type count:count];
}

-(void)_listenerRemoved:(NSString*)type count:(NSInteger)count
{
	if (self.modelDelegate!=nil && [(NSObject*)self.modelDelegate respondsToSelector:@selector(listenerRemoved:count:)])
	{
		[self.modelDelegate listenerRemoved:type count:count];
	}
	else if(view!=nil) // don't create the view if not already realized
	{
		if ([self.view respondsToSelector:@selector(listenerRemoved:count:)]) {
			[self.view listenerRemoved:type count:count];
		}
	}
    [super _listenerRemoved:type count:count];

}

#pragma mark Layout events, internal and external

#define SET_AND_PERFORM(flagBit,action)	\
if (!viewInitialized || !parentVisible || OSAtomicTestAndSetBarrier(flagBit, &dirtyflags)) \
{	\
	action;	\
}


-(void)willEnqueue
{
#ifndef TI_USE_AUTOLAYOUT
	SET_AND_PERFORM(TiRefreshViewEnqueued,return);
    if (parentVisible && !hidden && (!allowContentChange || instantUpdates)) return;
	[TiLayoutQueue addViewProxy:self];
#endif
}

-(void)willEnqueueIfVisible
{
//	if(parentVisible && !hidden)
//	{
		[self willEnqueue];
//	}
//    SET_AND_PERFORM(TiRefreshViewEnqueued,return);
//    if (parentVisible && !hidden && (!allowContentChange || instantUpdates)) return;
//    [TiLayoutQueue addViewProxy:self];
}


-(void)performBlockWithoutLayout:(void (^)(void))block
{
    if (!allowContentChange) {
        block();
    } else {
        allowContentChange = NO;
        block();
        allowContentChange = YES;
    }
}

-(void)performBlock:(void (^)(void))block withinAnimation:(TiViewAnimationStep*)animation
{
    if (animation) {
        [self setRunningAnimation:animation];
        block();
        [self setRunningAnimation:nil];
    }
    else {
        block();
    }
}

-(void)performBlock:(void (^)(void))block withinOurAnimationOnProxy:(TiViewProxy*)viewProxy
{
    [viewProxy performBlock:block withinAnimation:[self runningAnimation]];
}

-(void)parentContentWillChange
{
    if (allowContentChange == NO && [[self viewParent] allowContentChange])
    {
        [[self viewParent] performBlockWithoutLayout:^{
            [[self viewParent] contentsWillChange];
        }];
    }
    else {
        if ([self viewParent]) {
            [[self viewParent] contentsWillChange];
        } else {
            [self contentsWillChange];
        }
    }
}

-(void)willChangeSize
{
 #ifndef TI_USE_AUTOLAYOUT
   if (!_canResizeItself) {
        return;
    }
	SET_AND_PERFORM(TiRefreshViewSize,return);

	if (![self absoluteLayout])
	{
		[self willChangeLayout];
	}
    else {
        [self willResizeChildren];
    }
	if(TiDimensionIsUndefined(layoutProperties.centerX) ||
			TiDimensionIsUndefined(layoutProperties.centerY))
	{
		[self willChangePosition];
	}

	[self willEnqueueIfVisible];
    [self parentContentWillChange];
	
    if (!allowContentChange) return;
//    [self makeChildrenPerformSelector:@selector(parentSizeWillChange) withObject:nil];
    
    if (instantUpdates) {
        TiThreadPerformOnMainThread(^{[self refreshViewOrParent];}, NO);
    }
#endif
}

-(void)willChangePosition
{
#ifndef TI_USE_AUTOLAYOUT
    if (!_canRepositionItself) {
        return;
    }
	SET_AND_PERFORM(TiRefreshViewPosition,return);

	if(TiDimensionIsUndefined(layoutProperties.width) || 
			TiDimensionIsUndefined(layoutProperties.height))
	{//The only time size can be changed by the margins is if the margins define the size.
		[self willChangeSize];
	}
	[self willEnqueueIfVisible];
    [self parentContentWillChange];
#endif
}

-(void)willChange
{
    [self willChangeSize];
    [self willChangePosition];
}

-(void)willChangeZIndex
{
#ifndef TI_USE_AUTOLAYOUT
    SET_AND_PERFORM(TiRefreshViewZIndex, return);
    //Nothing cascades from here.
    [self willEnqueueIfVisible];
#endif
}


-(void)willShow;
{
//    [self willChangeZIndex];
//    pthread_rwlock_rdlock(&childrenLock);
//    if (allowContentChange)
//    {
        [self makeViewChildrenPerformSelector:@selector(parentWillShow) withObject:nil];
//    }
//    else {
//        [self makeChildrenPerformSelector:@selector(parentWillShowWithoutUpdate) withObject:nil];
//    }
//    pthread_rwlock_unlock(&childrenLock);
    
//    if (parent && ![[self viewParent] absoluteLayout])
//        [self parentContentWillChange];
//    else {
//        [self contentsWillChange];
//    }
    [self willChange];
}

-(void)willHide;
{
    //	SET_AND_PERFORM(TiRefreshViewZIndex,);
//    dirtyflags = 0;

    [self makeViewChildrenPerformSelector:@selector(parentWillHide) withObject:nil];
    
//    if (parent && ![[self viewParent] absoluteLayout])
//        [self parentContentWillChange];
}

-(void)willResizeChildren
{
    if (childrenCount == 0) return;
	SET_AND_PERFORM(TiRefreshViewChildrenPosition,return);
	[self willEnqueueIfVisible];
}

-(void)willChangeLayout
{
    if (!viewInitialized)return;
    BOOL alreadySet = OSAtomicTestAndSet(TiRefreshViewChildrenPosition, &dirtyflags);

	[self willEnqueueIfVisible];

//    if (!allowContentChange || alreadySet) return;
//    [self makeChildrenPerformSelector:@selector(parentWillRelay) withObject:nil];
}

-(BOOL) widthIsAutoSize
{
#ifndef TI_USE_AUTOLAYOUT
    if (layoutProperties.fullscreen) return NO;
    BOOL isAutoSize = NO;
    if (TiDimensionIsAutoSize(layoutProperties.width))
    {
        isAutoSize = YES;
    }
    else if (TiDimensionIsAuto(layoutProperties.width) && TiDimensionIsAutoSize([self defaultAutoWidthBehavior:nil]) )
    {
        isAutoSize = YES;
    }
    else if (TiDimensionIsUndefined(layoutProperties.width) && TiDimensionIsAutoSize([self defaultAutoWidthBehavior:nil]))
    {
        int pinCount = 0;
        if (!TiDimensionIsUndefined(layoutProperties.left) ) {
            pinCount ++;
        }
        if (!TiDimensionIsUndefined(layoutProperties.centerX) ) {
            pinCount ++;
        }
        if (!TiDimensionIsUndefined(layoutProperties.right) ) {
            pinCount ++;
        }
        if (pinCount < 2) {
            isAutoSize = YES;
        }
    }
    return isAutoSize;
#else
	return NO;
#endif
}

-(BOOL) heightIsAutoSize
{
#ifndef TI_USE_AUTOLAYOUT
    if (layoutProperties.fullscreen) return NO;
    BOOL isAutoSize = NO;
    if (TiDimensionIsAutoSize(layoutProperties.height))
    {
        isAutoSize = YES;
    }
    else if (TiDimensionIsAuto(layoutProperties.height) && TiDimensionIsAutoSize([self defaultAutoHeightBehavior:nil]) )
    {
        isAutoSize = YES;
    }
    else if (TiDimensionIsUndefined(layoutProperties.height) && TiDimensionIsAutoSize([self defaultAutoHeightBehavior:nil]))
    {
        int pinCount = 0;
        if (!TiDimensionIsUndefined(layoutProperties.top) ) {
            pinCount ++;
        }
        if (!TiDimensionIsUndefined(layoutProperties.centerY) ) {
            pinCount ++;
        }
        if (!TiDimensionIsUndefined(layoutProperties.bottom) ) {
            pinCount ++;
        }
        if (pinCount < 2) {
            isAutoSize = YES;
        }
    }
    return isAutoSize;
#else
	return NO;
#endif
}

-(BOOL) widthIsAutoFill
{
#ifndef TI_USE_AUTOLAYOUT
    if (layoutProperties.fullscreen) return YES;
    BOOL isAutoFill = NO;
    BOOL followsFillBehavior = TiDimensionIsAutoFill([self defaultAutoWidthBehavior:nil]);
    
    if (TiDimensionIsAutoFill(layoutProperties.width))
    {
        isAutoFill = YES;
    }
    else if (TiDimensionIsAuto(layoutProperties.width))
    {
        isAutoFill = followsFillBehavior;
    }
    else if (TiDimensionIsUndefined(layoutProperties.width))
    {
        BOOL centerDefined = NO;
        int pinCount = 0;
        if (!TiDimensionIsUndefined(layoutProperties.left) ) {
            pinCount ++;
        }
        if (!TiDimensionIsUndefined(layoutProperties.centerX) ) {
            centerDefined = YES;
            pinCount ++;
        }
        if (!TiDimensionIsUndefined(layoutProperties.right) ) {
            pinCount ++;
        }
        if (!centerDefined){
            isAutoFill = followsFillBehavior;
        }
    }
    return isAutoFill;
#else
	return NO;
#endif
}

-(BOOL) heightIsAutoFill
{
#ifndef TI_USE_AUTOLAYOUT
    if (layoutProperties.fullscreen) return YES;
    BOOL isAutoFill = NO;
    BOOL followsFillBehavior = TiDimensionIsAutoFill([self defaultAutoHeightBehavior:nil]);
    
    if (TiDimensionIsAutoFill(layoutProperties.height))
    {
        isAutoFill = YES;
    }
    else if (TiDimensionIsAuto(layoutProperties.height))
    {
        isAutoFill = followsFillBehavior;
    }
    else if (TiDimensionIsUndefined(layoutProperties.height))
    {
        BOOL centerDefined = NO;
        int pinCount = 0;
        if (!TiDimensionIsUndefined(layoutProperties.top) ) {
            pinCount ++;
        }
        if (!TiDimensionIsUndefined(layoutProperties.centerY) ) {
            centerDefined = YES;
            pinCount ++;
        }
        if (!TiDimensionIsUndefined(layoutProperties.bottom) ) {
            pinCount ++;
        }
        if ((!centerDefined) ) {
            isAutoFill = followsFillBehavior;
        }
    }
    return isAutoFill;
#else
	return NO;
#endif
}

-(BOOL)wantsToFillVerticalLayout
{
    if ([self heightIsAutoFill]) return YES;
    BOOL followsFillBehavior = TiDimensionIsAutoFill([self defaultAutoHeightBehavior:nil]);
    if (TiDimensionIsDip(layoutProperties.height) || TiDimensionIsPercent(layoutProperties.height) ||
        (!followsFillBehavior && TiDimensionIsUndefined(layoutProperties.height)))return NO;
    NSArray* subproxies = [self visibleChildren];
    for (TiViewProxy* child in subproxies) {
        if ([child wantsToFillVerticalLayout]) return YES;
    }
    return NO;
}

-(CGFloat)layoutWeight
{
    return layoutProperties.weight;
}

-(BOOL)wantsToFillHorizontalLayout
{
    if ([self widthIsAutoFill]) return YES;
    BOOL followsFillBehavior = TiDimensionIsAutoFill([self defaultAutoWidthBehavior:nil]);
    if (TiDimensionIsDip(layoutProperties.width) || TiDimensionIsPercent(layoutProperties.width) ||
        (!followsFillBehavior && TiDimensionIsUndefined(layoutProperties.width)))return NO;
    NSArray* subproxies = [self visibleChildren];
    for (TiViewProxy* child in subproxies) {
        if ([child wantsToFillHorizontalLayout]) return YES;
    }
    return NO;
}


-(void)contentsWillChange
{
#ifndef TI_USE_AUTOLAYOUT
    BOOL isAutoSize = [self widthIsAutoSize] || [self heightIsAutoSize];
    
	if (isAutoSize)
	{
		[self willChangeSize];
	}
	else
	{
		[self willChangeLayout];
	}
#endif
}

-(BOOL)allowContentChange
{
    return allowContentChange;
}

-(void)contentsWillChangeImmediate
{
    allowContentChange = NO;
    [self contentsWillChange];
    allowContentChange = YES;
    [self refreshViewOrParent];
}
-(void)performLayoutBlockAndRefresh:(void (^)(void))block
{
    [self performBlockWithoutLayout:block];
    [self refreshViewOrParent];
}

-(void)contentsWillChangeAnimated:(NSTimeInterval)duration
{
    [UIView animateWithDuration:duration animations:^{
        [self contentsWillChangeImmediate];
    }];
}

-(void)parentSizeWillChange
{
#ifndef TI_USE_AUTOLAYOUT
//	if not dip, change size
	if(!TiDimensionIsDip(layoutProperties.width) || !TiDimensionIsDip(layoutProperties.height) )
	{
		[self willChangeSize];
	}
	if(!TiDimensionIsDip(layoutProperties.centerX) ||
			!TiDimensionIsDip(layoutProperties.centerY))
	{
		[self willChangePosition];
	}
#endif
}

-(void)parentWillRelay
{
#ifndef TI_USE_AUTOLAYOUT
//	if percent or undefined size, change size
	if(TiDimensionIsUndefined(layoutProperties.width) ||
			TiDimensionIsUndefined(layoutProperties.height) ||
			TiDimensionIsPercent(layoutProperties.width) ||
			TiDimensionIsPercent(layoutProperties.height))
	{
		[self willChangeSize];
	}
	[self willChangePosition];
#endif
}

-(void)parentWillShow
{
	VerboseLog(@"[INFO] Parent Will Show for %@",self);
	if(parentVisible)
	{//Nothing to do here, we're already visible here.
		return;
	}
	parentVisible = YES;
	if(!hidden)
	{	//We should propagate this new status! Note this does not change the visible property.
		[self willShow];
	}
}

-(void)parentWillShowWithoutUpdate
{
    BOOL wasSet = allowContentChange;
    allowContentChange = NO;
    [self parentWillShow];
    allowContentChange = wasSet;
}

-(void)parentWillHide
{
	VerboseLog(@"[INFO] Parent Will Hide for %@",self);
	if(!parentVisible)
	{//Nothing to do here, we're already invisible here.
		return;
	}
	parentVisible = NO;
	if(!hidden)
	{	//We should propagate this new status! Note this does not change the visible property.
		[self willHide];
	}
}

#pragma mark Layout actions

-(void)updateZIndex {
    if(OSAtomicTestAndClearBarrier(TiRefreshViewZIndex, &dirtyflags) && vzIndex > 0) {
        if(parent != nil) {
            [[self viewParent] reorderZChildren];
        }
    }
}

// Need this so we can overload the sandbox bounds on split view detail/master
-(void)determineSandboxBounds
{
 #ifndef TI_USE_AUTOLAYOUT
   if (controller) return;
    [self updateZIndex];
    UIView * ourSuperview = [[self view] superview];
    CGRect bounds = [ourSuperview bounds];
    if(ourSuperview != nil && !TiCGRectIsEmpty(bounds))
    {
        sandboxBounds = bounds;
    }
#endif
}

-(void)refreshView:(TiUIView *)transferView
{
    [self refreshView:transferView withinAnimation:nil];
}


-(void)refreshView
{
    [self dirtyItAll];
	[self refreshViewIfNeeded];
}

-(void)refreshViewIfNeeded
{
	[self refreshViewIfNeeded:NO];
}

-(void)refreshViewOrParent
{
    TiViewProxy* viewParent = [self viewParent];
    if (viewParent && [viewParent isDirty]) {
        [self performBlock:^{
            [viewParent refreshViewOrParent];
        } withinOurAnimationOnProxy:viewParent];
    }
    else {
        [self refreshViewIfNeeded:YES];
    }
}
-(void)refreshViewIfNeededNSNumber:(NSNumber*)recursive
{
    [self refreshViewIfNeeded:[recursive boolValue]];
}
-(void)refreshViewIfNeeded:(BOOL)recursive
{
    TiViewProxy* viewParent = isUsingBarButtonItem?nil:[self viewParent];
    if (hidden || (viewParent && [viewParent willBeRelaying] && ![viewParent absoluteLayout])) {
        return;
    }
    BOOL needsRefresh = OSAtomicTestAndClear(TiRefreshViewEnqueued, &dirtyflags);

    if (!needsRefresh)
    {
        dirtyflags = 0;
        //even if our sandbox is null and we are not ready (next test) let s still call refresh our our children. They wont refresh but at least they will clear their TiRefreshViewEnqueued flags !
        if (recursive){
            [self makeViewChildrenPerformSelector:@selector(refreshViewIfNeededNSNumber:) withObject:@(recursive)];
        }
        return;
	}
    if (TiCGRectIsEmpty(sandboxBounds) && (!view || ![view superview])) {
        //we have no way to get our size yet. May be we need to be added to a superview
        //let s keep our flags set
        return;
    }
    
	if(viewParent && !parentVisible)
	{
		VerboseLog(@"[INFO] Parent Invisible");
		return;
	}
	
	
    
    if (view != nil)
	{
        BOOL relayout = ![self suppressesRelayout];
        if (viewParent != nil && ![viewParent absoluteLayout]) {
            //Do not mess up the sandbox in vertical/horizontal layouts
            relayout = NO;
        }
        if(relayout)
        {
            [self determineSandboxBounds];
        }
        BOOL layoutChanged = [self relayout];
        
        if (OSAtomicTestAndClear(TiRefreshViewChildrenPosition, &dirtyflags) || layoutChanged) {
            [self layoutChildren:NO];
        }
        [self handlePendingAnimation];
	}
}

-(void)dirtyItAll
{
#ifndef TI_USE_AUTOLAYOUT
    OSAtomicTestAndSet(TiRefreshViewZIndex, &dirtyflags);
    OSAtomicTestAndSet(TiRefreshViewEnqueued, &dirtyflags);
    if (_canResizeItself){
        OSAtomicTestAndSet(TiRefreshViewSize, &dirtyflags);
    }
    if (_canRepositionItself) {
        OSAtomicTestAndSet(TiRefreshViewPosition, &dirtyflags);
    }
    if (childrenCount > 0) {
        OSAtomicTestAndSet(TiRefreshViewChildrenPosition, &dirtyflags);
    }
#endif
}

-(void)clearItAll
{
    dirtyflags = 0;
}

-(BOOL)isDirty
{
    return [self willBeRelaying];
}

-(void)refreshView:(TiUIView *)transferView withinAnimation:(TiViewAnimationStep*)animation
{
    [transferView setRunningAnimation:animation];
#ifndef TI_USE_AUTOLAYOUT
    WARN_IF_BACKGROUND_THREAD_OBJ;
	OSAtomicTestAndClearBarrier(TiRefreshViewEnqueued, &dirtyflags);
	
	if(!parentVisible)
	{
		VerboseLog(@"[INFO] Parent Invisible");
		return;
	}
	
	if(hidden)
	{
		return;
	}
    
	BOOL changedFrame = NO;
    //BUG BARRIER: Code in this block is legacy code that should be factored out.
	if ([self viewAttached])
	{
		CGRect oldFrame = [[self view] frame];
        BOOL relayout = ![self suppressesRelayout];
        if (parent != nil && ![[self viewParent] absoluteLayout]) {
            //Do not mess up the sandbox in vertical/horizontal layouts
            relayout = NO;
        }
        if(relayout)
        {
            [self determineSandboxBounds];
        }
        if ([self relayout] || relayout || animation || OSAtomicTestAndClear(TiRefreshViewChildrenPosition, &dirtyflags)) {
            OSAtomicTestAndClear(TiRefreshViewChildrenPosition, &dirtyflags);
            [self layoutChildren:NO];
        }
		if (!CGRectEqualToRect(oldFrame, [[self view] frame])) {
			[[self viewParent] childWillResize:self withinAnimation:animation];
		}
	}
    
    //END BUG BARRIER
    
	if(OSAtomicTestAndClearBarrier(TiRefreshViewSize, &dirtyflags))
	{
		[self refreshSize];
		if(TiLayoutRuleIsAbsolute(layoutProperties.layoutStyle))
		{
			for (TiViewProxy * thisChild in [self viewChildren])
			{
				[thisChild setSandboxBounds:sizeCache];
			}
		}
		changedFrame = YES;
	}
	else if(transferView != nil)
	{
		[transferView setBounds:sizeCache];
	}
    
	if(OSAtomicTestAndClearBarrier(TiRefreshViewPosition, &dirtyflags))
	{
		[self refreshPosition];
		changedFrame = YES;
	}
	else if(transferView != nil)
	{
		[transferView setCenter:positionCache];
	}
    
    //We should only recurse if we're a non-absolute layout. Otherwise, the views can take care of themselves.
	if(OSAtomicTestAndClearBarrier(TiRefreshViewChildrenPosition, &dirtyflags) && (transferView == nil))
        //If transferView is non-nil, this will be managed by the table row.
	{
		
	}
    
	if(transferView != nil)
	{
        //TODO: Better handoff of view
		[self setView:transferView];
	}
    
    //By now, we MUST have our view set to transferView.
	if(changedFrame || (transferView != nil))
	{
		[view setAutoresizingMask:autoresizeCache];
	}
    
    [self updateZIndex];
#endif
    [transferView setRunningAnimation:nil];
}

-(void)refreshPosition
{
#ifndef TI_USE_AUTOLAYOUT
    if (_canRepositionItself) {
        OSAtomicTestAndClearBarrier(TiRefreshViewPosition, &dirtyflags);
    }
#endif
}

-(void)refreshSize
{
#ifndef TI_USE_AUTOLAYOUT
    if (_canResizeItself) {
        OSAtomicTestAndClearBarrier(TiRefreshViewSize, &dirtyflags);
    }
#endif
}

+(void)reorderViewsInParent:(UIView*)parentView
{
    if (parentView == nil) return;
    NSMutableArray* parentViewToSort = [NSMutableArray array];
    for (UIView* subview in [parentView subviews])
    {
        if ([subview isKindOfClass:[TiUIView class]]) {
            [parentViewToSort addObject:subview];
        }
    }
    NSArray *sortedArray = [parentViewToSort sortedArrayUsingComparator:^NSComparisonResult(TiUIView* a, TiUIView* b) {
        NSInteger first = [(TiViewProxy*)(a.proxy) vzIndex];
        NSInteger second = [(TiViewProxy*)(b.proxy) vzIndex];
        return (first > second) ? NSOrderedDescending : ( first < second ? NSOrderedAscending : NSOrderedSame );
    }];
    for (TiUIView* view in sortedArray) {
        [parentView bringSubviewToFront:view];
    }
}

-(void)reorderZChildren{
    if (view == nil) return;
    NSArray *sortedArray = [[self viewChildren] sortedArrayUsingComparator:^NSComparisonResult(TiViewProxy* a, TiViewProxy* b) {
        NSInteger first = [a vzIndex];
        NSInteger second = [b vzIndex];
        return (first > second) ? NSOrderedDescending : ( first < second ? NSOrderedAscending : NSOrderedSame );
    }];
    for (TiViewProxy* child in sortedArray) {
        [view bringSubviewToFront:[child view]];
    }
}

-(void)insertSubview:(UIView *)childView forProxy:(TiViewProxy *)childProxy
{
    UIView * ourView = [self parentViewForChild:childProxy];
    
    if (ourView==nil || childView == nil) {
        return;
    }
    [ourView addSubview:[childProxy view]];
}

-(BOOL)absoluteLayout
{
    return TiLayoutRuleIsAbsolute(layoutProperties.layoutStyle);
}


-(CGRect)computeBoundsForParentBounds:(CGRect)parentBounds
{
    CGSize size = SizeConstraintViewWithSizeAddingResizing(&layoutProperties,self, parentBounds.size, &autoresizeCache);
    if (!CGSizeEqualToSize(size, sizeCache.size)) {
        sizeCache.size = size;
    }
    CGPoint position = PositionConstraintGivenSizeBoundsAddingResizing(&layoutProperties, [[self viewParent] layoutProperties], self, sizeCache.size,
                                                               [[view layer] anchorPoint], parentBounds.size, sandboxBounds.size, &autoresizeCache);
    position.x += sizeCache.origin.x + sandboxBounds.origin.x;
    position.y += sizeCache.origin.y + sandboxBounds.origin.y;
    if (!CGPointEqualToPoint(position, positionCache)) {
        positionCache = position;
    }
    return CGRectMake(position.x - size.width/2, position.y - size.height/2, size.width, size.height);
}

#pragma mark Layout commands that need refactoring out

-(BOOL)relayout
{
#ifndef TI_USE_AUTOLAYOUT
    if (!repositioning)
    {
        if (CGSizeEqualToSize(sandboxBounds.size, CGSizeZero)) {
            dirtyflags = 0;
            repositioning = NO;
            return NO;
        }
        ENSURE_UI_THREAD_0_ARGS
        OSAtomicTestAndClear(TiRefreshViewEnqueued, &dirtyflags);
        repositioning = YES;
        
        TiViewProxy* parentViewProxy = [self viewParent];
        UIView *parentView = [parentViewProxy parentViewForChild:self];
        CGSize referenceSize = sandboxBounds.size;
        CGRect parentBounds = [parentView bounds];
        if (parentView && !(TiCGRectIsEmpty(parentBounds))) {
            referenceSize = parentView.bounds.size;
        }
        if (CGSizeEqualToSize(referenceSize, CGSizeZero)) {
            repositioning = NO;
            dirtyflags = 0;
            return;
        }
        BOOL needsAll = TiCGRectIsEmpty(sizeCache);
        BOOL needsSize = OSAtomicTestAndClear(TiRefreshViewSize, &dirtyflags) || needsAll;
        BOOL needsPosition = OSAtomicTestAndClear(TiRefreshViewPosition, &dirtyflags) || needsAll;
        BOOL layoutChanged = NO;
        if (needsSize) {
            CGSize size;
            if (parentViewProxy != nil && ![parentViewProxy absoluteLayout] ) {
                size = SizeConstraintViewWithSizeAddingResizing(&layoutProperties,self, sandboxBounds.size, &autoresizeCache);
            }
            else {
                size = SizeConstraintViewWithSizeAddingResizing(&layoutProperties,self, referenceSize, &autoresizeCache);
            }
            if (!CGSizeEqualToSize(size, sizeCache.size)) {
                sizeCache.size = size;
                layoutChanged = YES;
            }
        }
        if (needsPosition) {
            CGPoint position;
            position = PositionConstraintGivenSizeBoundsAddingResizing(&layoutProperties, [parentViewProxy layoutProperties], self, sizeCache.size,
                                                                       [[view layer] anchorPoint], referenceSize, sandboxBounds.size, &autoresizeCache);
            
            position.x += sizeCache.origin.x + sandboxBounds.origin.x;
            position.y += sizeCache.origin.y + sandboxBounds.origin.y;
            if (!CGPointEqualToPoint(position, positionCache)) {
                positionCache = position;
                layoutChanged = YES;
            }
        }
        
        layoutChanged |= autoresizeCache != view.autoresizingMask;
        if (!layoutChanged && [view isKindOfClass:[TiUIView class]]) {
            //Views with flexible margins might have already resized when the parent resized.
            //So we need to explicitly check for oldSize here which triggers frameSizeChanged
            CGSize oldSize = [(TiUIView*) view oldSize];
            layoutChanged = layoutChanged || !(CGSizeEqualToSize(oldSize,sizeCache.size) || !CGRectEqualToRect([view bounds], sizeCache) || !CGPointEqualToPoint([view center], positionCache));
        }
        
        
        [view setAutoresizingMask:autoresizeCache];
        if (needsPosition) {
            [view setCenter:positionCache];
        }
        if (needsSize) {
            [view setBounds:sizeCache];
        }
        
        [self updateZIndex];
        
        if ([observer respondsToSelector:@selector(proxyDidRelayout:)]) {
            [observer proxyDidRelayout:self];
        }
        
        if (layoutChanged && [self _hasListeners:@"postlayout" checkParent:NO]) {
            [self fireEvent:@"postlayout" propagate:NO checkForListener:NO];
        }
        repositioning = NO;
        return layoutChanged;
    }
#endif
    return NO;
}

-(void)layoutChildrenIfNeeded
{
	IGNORE_IF_NOT_OPENED
	
    // if not visible, ignore layout
    if (hidden)
    {
//        OSAtomicTestAndClearBarrier(TiRefreshViewEnqueued, &dirtyflags);
        return;
    }
    
    [self refreshView:nil];
}

-(BOOL)willBeRelaying
{
#ifndef TI_USE_AUTOLAYOUT
    DeveloperLog(@"DIRTY FLAGS %d WILLBERELAYING %d",dirtyflags, (*((char*)&dirtyflags) & (1 << (7 - TiRefreshViewEnqueued))));
    return ((*((char*)&dirtyflags) & (1 << (7 - TiRefreshViewEnqueued))) != 0);
#else
    return NO;
#endif
}

-(void)childWillResize:(TiViewProxy *)child
{
    [self childWillResize:child withinAnimation:nil];
}

-(void)childWillResize:(TiViewProxy *)child withinAnimation:(TiViewAnimationStep*)animation
{
    if (animation != nil) {
        [self refreshView:nil withinAnimation:animation];
        return;
    }
    
#ifndef TI_USE_AUTOLAYOUT
	[self contentsWillChange];

	IGNORE_IF_NOT_OPENED
	
//	BOOL containsChild = [[self children] containsObject:child];
//
//	ENSURE_VALUE_CONSISTENCY(containsChild,YES);

	if (![self absoluteLayout])
	{
		BOOL alreadySet = OSAtomicTestAndSet(TiRefreshViewChildrenPosition, &dirtyflags);
		if (!alreadySet)
		{
			[self willEnqueue];
		}
	}
#endif
}

-(void)reposition
{
    [self repositionWithinAnimation:nil];
}

-(TiViewAnimationStep*)runningAnimation
{
    return [view runningAnimation];
}

-(void)setRunningAnimation:(TiViewAnimationStep*)animation
{
    [view setRunningAnimation:animation];
}

-(void)setRunningAnimationRecursive:(TiViewAnimationStep*)animation
{
    [view setRunningAnimation:animation];
    [self runBlock:^(TiViewProxy *proxy) {
        [proxy setRunningAnimationRecursive:animation];
    } onlyVisible:YES recursive:YES];
}

-(void)setFakeAnimationOfDuration:(NSTimeInterval)duration andCurve:(CAMediaTimingFunction*)curve
{
    TiFakeAnimation* anim = [[TiFakeAnimation alloc] init];
    anim.duration = duration;
    anim.curve = curve;
    [self setRunningAnimationRecursive:anim];
    [anim release];
}

-(BOOL)isRotating
{
    return [[self runningAnimation] isKindOfClass:[TiFakeAnimation class]];
}

-(void)removeFakeAnimation
{
//    id anim = [self runningAnimation];
    if ([[self runningAnimation] isKindOfClass:[TiFakeAnimation class]])
    {
        [self setRunningAnimationRecursive:nil];
//        [anim release];
    }
}

-(void)repositionWithinAnimation
{
    [self repositionWithinAnimation:[self runningAnimation]];
}
-(void)repositionWithinAnimation:(TiViewAnimationStep*)animation
{
	IGNORE_IF_NOT_OPENED
	
//	UIView* superview = [[self view] superview];
	if (![self viewReady] || hidden)
	{
		VerboseLog(@"[INFO] Reposition is exiting early in %@.",self);
		return;
	}
	if ([NSThread isMainThread])
    {
        [self performBlock:^{
            [self performBlockWithoutLayout:^{
                [self willChangeSize];
                [self willChangePosition];
            }];
            
            [self refreshViewOrParent];
        } withinAnimation:animation];
	}
	else
	{
		VerboseLog(@"[INFO] Reposition was called by a background thread in %@.",self);
		TiThreadPerformOnMainThread(^{[self reposition];}, NO);
	}
    
}

-(CGRect)boundsForMeasureForChild:(TiViewProxy*)child
{
    UIView * ourView = [self parentViewForChild:child];
    if (!ourView) return CGRectZero;
    CGRect result = [ourView bounds];
//    BOOL autoWidth = [self widthIsAutoSize];
//    BOOL autoHeight = [self heightIsAutoSize];
//    if (autoWidth || autoHeight) {
//        CGRect parentBounds = [[self viewParent] boundsForMeasureForChild:self];
//        if (!CGSizeEqualToSize(parentBounds.size, CGSizeZero)) {
//            if (autoWidth) {
//                result.size.width = parentBounds.size.width;
//            }
//            if (autoHeight) {
//                result.size.height = parentBounds.size.height;
//            }
//        }
//        
//    }
//    if (!TiLayoutFlagsHasHorizontalWrap(&layoutProperties)) {
//        result.size.width -= horizontalLayoutBoundary;
//    }
//    result.size.height -= verticalLayoutBoundary;
    return result;
}

-(NSArray*)measureChildren:(NSArray*)childArray
{
#ifndef TI_USE_AUTOLAYOUT
    if ([childArray count] == 0) {
        return nil;
    }
    
    BOOL horizontal =  TiLayoutRuleIsHorizontal(layoutProperties.layoutStyle);
    BOOL vertical =  TiLayoutRuleIsVertical(layoutProperties.layoutStyle);
	BOOL horizontalNoWrap = horizontal && !TiLayoutFlagsHasHorizontalWrap(&layoutProperties);
	BOOL horizontalWrap = horizontal && TiLayoutFlagsHasHorizontalWrap(&layoutProperties);
    NSMutableArray * measuredBounds = [NSMutableArray arrayWithCapacity:[childArray count]];
    int i, count = (int)[childArray count];
	int maxHeight = 0;
    
    NSMutableArray * widthFillChildren = horizontal?[NSMutableArray array]:nil;
    NSMutableArray * heightFillChildren = (vertical || horizontalWrap)?[NSMutableArray array]:nil;
    CGFloat widthNonFill = 0;
    CGFloat heightNonFill = 0;
    CGFloat autoFillWidthTotalWeight = 0;
    CGFloat autoFillHeightTotalWeight = 0;
    
    //First measure the sandbox bounds
    for (id child in childArray)
    {
        CGRect bounds = [self boundsForMeasureForChild:child];
        CGRect childBounds = CGRectZero;
        
        if(![self absoluteLayout])
        {
            if (horizontalNoWrap) {
                if ([child wantsToFillHorizontalLayout])
                {
                    autoFillWidthTotalWeight += [child layoutWeight];
                    [widthFillChildren addObject:child];
                }
                else{
                    childBounds = [self computeChildSandbox:child withBounds:bounds];
                    maxHeight = MAX(maxHeight, childBounds.size.height);
                    widthNonFill += childBounds.size.width;
                }
            }
            else if (vertical) {
                if ([child wantsToFillVerticalLayout])
                {
                    autoFillHeightTotalWeight += [child layoutWeight];
                    [heightFillChildren addObject:child];
                }
                else{
                    childBounds = [self computeChildSandbox:child withBounds:bounds];
                    heightNonFill += childBounds.size.height;
                }
            }
            else {
                childBounds = [self computeChildSandbox:child withBounds:bounds];
            }
        }
        else {
            childBounds = bounds;
        }
        [measuredBounds addObject:[NSValue valueWithCGRect:childBounds]];
    }
    //If it is a horizontal layout ensure that all the children in a row have the
    //same height for the sandbox
    
    NSUInteger nbWidthAutoFill = [widthFillChildren count];
    if (nbWidthAutoFill > 0) {
        //it is horizontalNoWrap
//        horizontalLayoutBoundary = 0;
        CGFloat counter = 0.0f;
        for (int i =0; i < [childArray count]; i++) {
            id child = [childArray objectAtIndex:i];
            CGRect bounds = [self boundsForMeasureForChild:child];
            if ([widthFillChildren containsObject:child]){
                CGFloat weight = [child layoutWeight];
                CGFloat width = ((bounds.size.width - horizontalLayoutBoundary)*weight / (autoFillWidthTotalWeight - counter));
                counter += weight;
                CGRect usableRect = CGRectMake(0,0,width, bounds.size.height);
                CGRect result = [self computeChildSandbox:child withBounds:usableRect];
                maxHeight = MAX(maxHeight, result.size.height);
                [measuredBounds replaceObjectAtIndex:i withObject:[NSValue valueWithCGRect:result]];
            }
        }
    }
    
    NSUInteger nbHeightAutoFill = [heightFillChildren count];
    if (nbHeightAutoFill > 0) {
        //it is vertical
//        verticalLayoutBoundary = 0;
        CGFloat counter = 0.0f;
        for (int i =0; i < [childArray count]; i++) {
            id child = [childArray objectAtIndex:i];
            CGRect bounds = [self boundsForMeasureForChild:child];
            if ([heightFillChildren containsObject:child]){
                CGFloat weight = [child layoutWeight];
                CGFloat height = ((bounds.size.height - verticalLayoutBoundary)*weight / (autoFillHeightTotalWeight - counter));
                counter += weight;
                CGRect usableRect = CGRectMake(0,0,bounds.size.width, height);
                CGRect result = [self computeChildSandbox:child withBounds:usableRect];
                [measuredBounds replaceObjectAtIndex:i withObject:[NSValue valueWithCGRect:result]];
           }

        }
    }
	if (horizontalNoWrap)
	{
        int currentLeft = 0;
		for (i=0; i<count; i++)
		{
            CGRect rect = [[measuredBounds objectAtIndex:i] CGRectValue];
            rect.origin.x = currentLeft;
            [measuredBounds replaceObjectAtIndex:i withObject:[NSValue valueWithCGRect:rect]];
            currentLeft += rect.size.width;
		}
	}
    else if(vertical && (count > 1) )
    {
        int currentTop = 0;
		for (i=0; i<count; i++)
		{
            CGRect rect = [[measuredBounds objectAtIndex:i] CGRectValue];
            rect.origin.y = currentTop;
            [measuredBounds replaceObjectAtIndex:i withObject:[NSValue valueWithCGRect:rect]];
            currentTop += rect.size.height;
		}
    }
	else if(horizontal && (count > 1) )
    {
        int startIndex,endIndex, currentTop;
        startIndex = endIndex = maxHeight = currentTop = -1;
        for (i=0; i<count; i++)
        {
            CGRect childSandbox = [[measuredBounds objectAtIndex:i] CGRectValue];
            if (startIndex == -1)
            {
                //FIRST ELEMENT
                startIndex = i;
                maxHeight = childSandbox.size.height;
                currentTop = childSandbox.origin.y;
            }
            else
            {
                if (childSandbox.origin.y != currentTop)
                {
                    //MOVED TO NEXT ROW
                    endIndex = i;
                    for (int j=startIndex; j<endIndex; j++)
                    {
                        CGRect rect = [[measuredBounds objectAtIndex:j] CGRectValue];
                        rect.size.height = maxHeight;
                        [measuredBounds replaceObjectAtIndex:j withObject:[NSValue valueWithCGRect:rect]];
                    }
                    startIndex = i;
                    endIndex = -1;
                    maxHeight = childSandbox.size.height;
                    currentTop = childSandbox.origin.y;
                }
                else if (childSandbox.size.height > maxHeight)
                {
                    //SAME ROW HEIGHT CHANGED
                    maxHeight = childSandbox.size.height;
                }
            }
        }
        if (endIndex == -1)
        {
            //LAST ROW
            for (i=startIndex; i<count; i++)
            {
                CGRect rect = [[measuredBounds objectAtIndex:i] CGRectValue];
                rect.size.height = maxHeight;
                [measuredBounds replaceObjectAtIndex:i withObject:[NSValue valueWithCGRect:rect]];
            }
        }
    }
    return measuredBounds;
#else
    return nil;
#endif
}

-(CGRect)computeChildSandbox:(TiViewProxy*)child withBounds:(CGRect)bounds
{
#ifndef TI_USE_AUTOLAYOUT
    CGRect originalBounds = bounds;
    __block BOOL followsFillWBehavior = TiDimensionIsAutoFill([child defaultAutoWidthBehavior:nil]);
    __block BOOL followsFillHBehavior = TiDimensionIsAutoFill([child defaultAutoHeightBehavior:nil]);
    LayoutConstraint * childConstraint = [child layoutProperties];
    __block CGSize autoSize;
    __block BOOL fullscreen = childConstraint->fullscreen;
    __block BOOL autoSizeComputed = FALSE;
    __block BOOL recalculateWidth = NO;
    UIView *parentView = [self parentViewForChild:child];
    __block CGFloat boundingWidth = bounds.size.width;
    __block CGFloat boundingHeight = bounds.size.height;
    if (boundingHeight < 0) {
        boundingHeight = 0;
    }
    
    CGFloat offsetx = fullscreen?0:TiDimensionCalculateValue(childConstraint->left, boundingWidth)
    + TiDimensionCalculateValue(childConstraint->right, boundingWidth);
    
    CGFloat offsety = fullscreen?0:TiDimensionCalculateValue(childConstraint->top, boundingHeight)
    + TiDimensionCalculateValue(childConstraint->bottom, boundingHeight);
    
    
    void (^computeAutoSize)() = ^() {
        if (autoSizeComputed == FALSE) {
            autoSize = [child minimumParentSizeForSizeNoPadding:CGSizeMake(boundingWidth - offsetx, boundingHeight - offsety)];
            autoSizeComputed = YES;
        }
    };
    
    CGFloat (^computeHeight)() = ^CGFloat() {
        if (fullscreen == YES) {
            followsFillHBehavior = YES;
            return boundingHeight;
        }
        TiDimension constraint = childConstraint->height;
        CGFloat ratio = childConstraint->sizeRatio;
      
        if (TiDimensionIsDip(constraint) || TiDimensionIsPercent(constraint))
        {
            followsFillHBehavior = NO;
            return  TiDimensionCalculateValue(constraint, boundingHeight-offsety);
        }
        else if (TiDimensionIsUndefined(constraint) && ratio == 0)
        {
            if (!TiDimensionIsUndefined(childConstraint->top) && !TiDimensionIsUndefined(childConstraint->centerY) ) {
                followsFillHBehavior = NO;
                return 2 * ( TiDimensionCalculateValue(childConstraint->centerY, boundingHeight) - TiDimensionCalculateValue(childConstraint->top, boundingHeight) );
            }
            //            else if (!TiDimensionIsUndefined(childConstraint->top) && !TiDimensionIsUndefined(childConstraint->bottom) ) {
            //                recalculateWidth = YES;
            ////                followsFillHBehavior = YES;
            //                return boundingHeight-offsety;
            //            }
            else if (!TiDimensionIsUndefined(childConstraint->centerY) && !TiDimensionIsUndefined(childConstraint->bottom) ) {
                return 2 * ( boundingHeight - TiDimensionCalculateValue(childConstraint->bottom, boundingHeight) - TiDimensionCalculateValue(childConstraint->centerY, boundingHeight));
            }
            else if (followsFillHBehavior){
                recalculateWidth = YES;
                return boundingHeight-offsety;
            } else {
                //This block takes care of auto,SIZE and FILL. If it is size ensure followsFillBehavior is set to false
                computeAutoSize();
                followsFillHBehavior = NO;
                return autoSize.height;
            }
        }
        else if(TiDimensionIsAutoFill(constraint) || (TiDimensionIsAuto(constraint) && followsFillHBehavior)){
            followsFillHBehavior = YES;
            return boundingHeight-offsety;
        }
        else if(ratio > 0){
            followsFillHBehavior = NO;
            return 0.0f;
        }
        else {
            //This block takes care of auto,SIZE and FILL. If it is size ensure followsFillBehavior is set to false
            computeAutoSize();
            followsFillHBehavior = NO;
            return autoSize.height;
        }
    };
    
    CGFloat (^computeWidth)() = ^CGFloat() {
        if (fullscreen == YES) {
            followsFillWBehavior = YES;
            return boundingWidth;
        }
        TiDimension constraint = childConstraint->width;
        CGFloat ratio = childConstraint->sizeRatio;
        
        if (TiDimensionIsDip(constraint) || TiDimensionIsPercent(constraint))
        {
            followsFillWBehavior = NO;
            return  TiDimensionCalculateValue(constraint, boundingWidth-offsetx);
        }
        else if (TiDimensionIsUndefined(constraint) && ratio == 0)
        {
            if (!TiDimensionIsUndefined(childConstraint->left) && !TiDimensionIsUndefined(childConstraint->centerX) ) {
                return 2 * ( TiDimensionCalculateValue(childConstraint->centerX, boundingWidth) - TiDimensionCalculateValue(childConstraint->left, boundingWidth) );
            }
            //            else if (!TiDimensionIsUndefined(childConstraint->left) && !TiDimensionIsUndefined(childConstraint->right) ) {
            //                recalculateWidth = YES;
            ////                followsFillWBehavior = YES;
            //                return boundingWidth-offsetx;
            //            }
            else if (!TiDimensionIsUndefined(childConstraint->centerX) && !TiDimensionIsUndefined(childConstraint->right) ) {
                return 2 * ( boundingWidth - TiDimensionCalculateValue(childConstraint->right, boundingWidth) - TiDimensionCalculateValue(childConstraint->centerX, boundingWidth));
            }
            else if (followsFillWBehavior){
                recalculateWidth = YES;
                return boundingWidth-offsetx;
            } else {
                //This block takes care of auto,SIZE and FILL. If it is size ensure followsFillBehavior is set to false
                recalculateWidth = YES;
                computeAutoSize();
                followsFillWBehavior = NO;
                return autoSize.width;
            }
        }
        else if(TiDimensionIsAutoFill(constraint) || (TiDimensionIsAuto(constraint) && followsFillWBehavior)){
            followsFillWBehavior = YES;
            return boundingWidth-offsetx;
        }
        else if(ratio > 0){
            followsFillWBehavior = NO;
            return 0.0f;
        }
        else {
            //This block takes care of auto,SIZE and FILL. If it is size ensure followsFillBehavior is set to false
            recalculateWidth = YES;
            computeAutoSize();
            followsFillWBehavior = NO;
            return autoSize.width;
        }
    };
    
    bounds.size = CGSizeMake(computeWidth(), computeHeight());
    
    //    if (followsFillWBehavior) {
    //        bounds.size.width -= offsetx;
    //    }
    //    if (followsFillHBehavior) {
    //        bounds.size.height -= offsety;
    //    }
    
    bounds.size = minmaxSize(childConstraint, bounds.size, originalBounds.size);
    
    bounds.size.width += offsetx;
    bounds.size.height += offsety;
    
    if(TiLayoutRuleIsVertical(layoutProperties.layoutStyle))
    {
        bounds.origin.y = verticalLayoutBoundary;
        verticalLayoutBoundary += bounds.size.height;
    }
    else if(TiLayoutRuleIsHorizontal(layoutProperties.layoutStyle))
    {
        BOOL horizontalWrap = TiLayoutFlagsHasHorizontalWrap(&layoutProperties);
        
        CGFloat desiredWidth = bounds.size.width;
        
        if (horizontalWrap && (horizontalLayoutBoundary + desiredWidth >   boundingWidth)) {
            if (horizontalLayoutBoundary == 0.0) {
                //This is start of row
                bounds.origin.x = horizontalLayoutBoundary;
                bounds.origin.y = verticalLayoutBoundary;
                verticalLayoutBoundary += bounds.size.height;
                horizontalLayoutRowHeight = 0.0;
            }
            else {
                //This is not the start of row. Move to next row
                horizontalLayoutBoundary = 0.0;
                verticalLayoutBoundary += horizontalLayoutRowHeight;
                horizontalLayoutRowHeight = 0;
                bounds.origin.x = horizontalLayoutBoundary;
                bounds.origin.y = verticalLayoutBoundary;
                
                boundingWidth = originalBounds.size.width;
                boundingHeight = originalBounds.size.height - verticalLayoutBoundary;
                
                if (!recalculateWidth) {
                    if (desiredWidth < boundingWidth) {
                        horizontalLayoutBoundary += desiredWidth;
                        bounds.size.width = desiredWidth;
                        horizontalLayoutRowHeight = bounds.size.height;
                    }
                    else {
                        verticalLayoutBoundary += bounds.size.height;
                    }
                }
                else if (followsFillHBehavior) {
                    
                    verticalLayoutBoundary += bounds.size.height;
                }
                else {
                    computeAutoSize();
                    desiredWidth = autoSize.width;
                    if (desiredWidth < boundingWidth) {
                        
                        bounds.size.width = desiredWidth;
                        horizontalLayoutBoundary = bounds.size.width;
                        horizontalLayoutRowHeight = bounds.size.height;
                    }
                    else {
                        //fill whole space, another row again
                        verticalLayoutBoundary += bounds.size.height;
                    }
                }
                
            }
        }
        else {
            //If it fits update the horizontal layout row height
            bounds.origin.x = horizontalLayoutBoundary;
            bounds.origin.y = verticalLayoutBoundary;
            
            if (bounds.size.height > horizontalLayoutRowHeight) {
                horizontalLayoutRowHeight = bounds.size.height;
            }
            if (!recalculateWidth) {
                //DIP,PERCENT,UNDEFINED WITH ATLEAST 2 PINS one of them being centerX
                bounds.size.width = desiredWidth;
                horizontalLayoutBoundary += bounds.size.width;
            }
            else if(followsFillWBehavior)
            {
                //FILL that fits in left over space. Move to next row
                bounds.size.width = boundingWidth;
                if (horizontalWrap) {
                    horizontalLayoutBoundary = 0.0;
                    verticalLayoutBoundary += horizontalLayoutRowHeight;
                    horizontalLayoutRowHeight = 0.0;
                } else {
                    horizontalLayoutBoundary += bounds.size.width;
                }
            }
            else
            {
                //SIZE behavior
                bounds.size.width = desiredWidth;
                horizontalLayoutBoundary += bounds.size.width;
            }
        }
    }
    else {
        //        CGSize autoSize = [child minimumParentSizeForSize:bounds.size];
    }
    
    return bounds;
#else
    return CGRectZero;
#endif
}

-(void)layoutChild:(TiViewProxy*)child optimize:(BOOL)optimize withMeasuredBounds:(CGRect)bounds
{
#ifdef TI_USE_KROLL_THREAD
	IGNORE_IF_NOT_OPENED
#endif
	UIView * ourView = [self parentViewForChild:child];

	if (ourView==nil || [child isHidden])
	{
        [child clearItAll];
		return;
	}
	
	if (optimize==NO)
	{
		TiUIView *childView = [child view];
		TiUIView *parentView = (TiUIView*)[childView superview];
		if (parentView!=ourView)
		{
            [self insertSubview:childView forProxy:child];
            [self reorderZChildren];
		}
	}
	[child setSandboxBounds:bounds];
    [child dirtyItAll]; //for multileve recursion we need to make sure the child resizes itself
    [self performBlock:^{
        [child relayout];
        // tell our children to also layout
        [child layoutChildren:optimize];
    } withinOurAnimationOnProxy:child];
	
    [child handlePendingAnimation];
}

-(void)layoutNonRealChild:(TiViewProxy*)child withParent:(UIView*)parentView
{
    CGRect bounds = [self computeChildSandbox:child withBounds:[parentView bounds]];
    [child setSandboxBounds:bounds];
    [child refreshViewIfNeeded];
}

-(void)layoutChildren:(BOOL)optimize
{
#ifndef TI_USE_AUTOLAYOUT
	IGNORE_IF_NOT_OPENED
	
	verticalLayoutBoundary = 0.0;
	horizontalLayoutBoundary = 0.0;
	horizontalLayoutRowHeight = 0.0;
	
	if (optimize==NO)
	{
		OSAtomicTestAndSetBarrier(TiRefreshViewChildrenPosition, &dirtyflags);
	}
    
    if (CGSizeEqualToSize([[self view] bounds].size, CGSizeZero)) return;
    
    if (childrenCount > 0)
    {
        //TODO: This is really expensive, but what can you do? Laying out the child needs the lock again.
        NSArray * childrenArray = [[self visibleChildren] retain];
        
        NSUInteger childCount = [childrenArray count];
        if (childCount > 0) {
            NSArray * measuredBounds = [[self measureChildren:childrenArray] retain];
            NSUInteger childIndex;
            for (childIndex = 0; childIndex < childCount; childIndex++) {
                id child = [childrenArray objectAtIndex:childIndex];
                CGRect childSandBox = [[measuredBounds objectAtIndex:childIndex] CGRectValue];
                [self layoutChild:child optimize:optimize withMeasuredBounds:childSandBox];
            }
            [measuredBounds release];
        }
        [childrenArray release];
    }


	
	if (optimize==NO)
	{
		OSAtomicTestAndClearBarrier(TiRefreshViewChildrenPosition, &dirtyflags);
	}
#endif
}


-(TiDimension)defaultAutoWidthBehavior:(id)unused
{
    return TiDimensionAutoFill;
}
-(TiDimension)defaultAutoHeightBehavior:(id)unused
{
    return TiDimensionAutoFill;
}

#pragma mark - Accessibility API

- (void)setAccessibilityLabel:(id)accessibilityLabel
{
	ENSURE_UI_THREAD(setAccessibilityLabel, accessibilityLabel);
	if ([self viewAttached]) {
		[[self view] setAccessibilityLabel_:accessibilityLabel];
	}
	[self replaceValue:accessibilityLabel forKey:@"accessibilityLabel" notification:NO];
}

- (void)setAccessibilityValue:(id)accessibilityValue
{
	ENSURE_UI_THREAD(setAccessibilityValue, accessibilityValue);
	if ([self viewAttached]) {
		[[self view] setAccessibilityValue_:accessibilityValue];
	}
	[self replaceValue:accessibilityValue forKey:@"accessibilityValue" notification:NO];
}

- (void)setAccessibilityHint:(id)accessibilityHint
{
	ENSURE_UI_THREAD(setAccessibilityHint, accessibilityHint);
	if ([self viewAttached]) {
		[[self view] setAccessibilityHint_:accessibilityHint];
	}
	[self replaceValue:accessibilityHint forKey:@"accessibilityHint" notification:NO];
}

- (void)setAccessibilityHidden:(id)accessibilityHidden
{
	ENSURE_UI_THREAD(setAccessibilityHidden, accessibilityHidden);
	if ([self viewAttached]) {
		[[self view] setAccessibilityHidden_:accessibilityHidden];
	}
	[self replaceValue:accessibilityHidden forKey:@"accessibilityHidden" notification:NO];
}

#pragma mark - View Templates

+ (NSString*)defaultTemplateType
{
    return @"Ti.UI.View";
}

+ (TiProxy *)createFromDictionary:(NSDictionary*)dictionary rootProxy:(TiParentingProxy*)rootProxy inContext:(id<TiEvaluator>)context
{
	return [[self class] createFromDictionary:dictionary rootProxy:rootProxy inContext:context defaultType:[[self class] defaultTemplateType]];
}


-(void)hideKeyboard:(id)arg
{
    [self blur:nil];
}

-(void)blur:(id)args
{
	if ([self viewAttached])
	{
        TiThreadPerformBlockOnMainThread(^{
            [[self view] resignFirstResponder];
            [[self view] makeRootViewFirstResponder];
        }, NO);
	}
}

-(void)focus:(id)args
{
	if ([self viewAttached])
	{
        TiThreadPerformBlockOnMainThread(^{
            [[self view] becomeFirstResponder];
        }, NO);
    } else {
        needsFocusOnAttach = YES;
    }
}

- (BOOL)focused:(id)unused
{
    return [self focused];
}

-(BOOL)focussed
{
    return [self focused];
}

-(BOOL)focused
{
	BOOL result=NO;
	if ([self viewAttached])
	{
		result = [[self view] isFirstResponder];
	}
    
	return result;
}


-(void)handlePendingTransition
{
    if (_pendingTransition) {
        id args = _pendingTransition;
        _pendingTransition = nil;
        [self transitionViews:args];
        RELEASE_TO_NIL(args);
    }
}

-(void)transitionViews:(id)args
{
    
    if (_transitioning) {
        _pendingTransition = [args retain];
        return;
    }
    ENSURE_UI_THREAD_1_ARG(args)
    _transitioning = YES;
    if ([args count] > 1) {
        __block TiViewProxy *view1Proxy = nil;
        __block TiViewProxy *view2Proxy = nil;
        __block KrollCallback* callback = nil;
        NSDictionary* props = nil;
        ENSURE_ARG_OR_NIL_AT_INDEX(view1Proxy, args, 0, TiViewProxy);
        ENSURE_ARG_OR_NIL_AT_INDEX(view2Proxy, args, 1, TiViewProxy);
        ENSURE_ARG_OR_NIL_AT_INDEX(props, args, 2, NSDictionary);
        ENSURE_ARG_OR_NIL_AT_INDEX(callback, args, 3, KrollCallback);
        [callback retain];
        void (^onCompletion)() = ^() {
            if (view1Proxy) {
                [self removeProxy:view1Proxy shouldDetach:YES];
                view1Proxy.hiddenForLayout = NO;
            }
            [self refreshViewIfNeeded];
            _transitioning = NO;
            if (callback) {
                [self _fireEventToListener:@"done" withObject:nil listener:callback thisObject:nil];
            }
            [callback release];
            [self handlePendingTransition];
        };
        if ([self viewAttached] && parentVisible && [self viewLayedOut])
        {
            if (view1Proxy != nil) {
                pthread_rwlock_wrlock(&childrenLock);
                if (![children containsObject:view1Proxy])
                {
                    pthread_rwlock_unlock(&childrenLock);
                    if (view2Proxy){
                        [self add:view2Proxy];
                    }
                    _transitioning = NO;
                    [self handlePendingTransition];
                    return;
                }
            }
            
            pthread_rwlock_unlock(&childrenLock);
            
            TiUIView* view1 = nil;
            __block TiUIView* view2 = nil;
            
            if (view2Proxy) {
                view2Proxy.hiddenForLayout = YES;
                [self add:view2Proxy];
                if (view2Proxy.view) {
                    view2 = view2Proxy.view;
                    [view2Proxy determineSandboxBoundsForce];
                    [view2Proxy dirtyItAll];
                    [view2Proxy refreshViewIfNeeded];
                } else {
                    view2 = [view2Proxy getAndPrepareViewForOpening];
                }
                
            }
            if (view1Proxy != nil) {
                view1 = [view1Proxy getAndPrepareViewForOpening];
                [view1Proxy refreshViewIfNeeded];
            }
            void (^animationBlock)() = ^() {
                [self performBlockWithoutLayout:^{
                    if (view2Proxy) {
                        view2Proxy.hiddenForLayout = NO;
                    }
                    if (view1Proxy) {
                        view1Proxy.hiddenForLayout = YES;
                    }
                    [self contentsWillChange];
                }];
                
                [self refreshViewOrParent];
            };
            [self refreshViewOrParent];
            
            TiTransition* transition = [TiTransitionHelper transitionFromArg:props containerView:self.view];
            transition.adTransition.type = ADTransitionTypePush;
                 [[self view] transitionFromView:view1 toView:view2 withTransition:transition animationBlock:animationBlock completionBlock:onCompletion];
        }
        else {
            if (view2Proxy) {
                [self add:view2Proxy];
            }
            onCompletion();
            
        }
    }
}


-(void)blurBackground:(id)args
{
    ENSURE_UI_THREAD_1_ARG(args)
    if ([self viewAttached]) {
        [[self view] blurBackground:args];
    }
}
-(void)configurationStartNSNumber:(NSNumber*)ready
{
    [self configurationStart:[ready boolValue]];
}

-(void)handleUpdatedValue:(id)value forKey:(NSString*)key {
    if (eventOverrideDelegate && [self.eventOverrideDelegate respondsToSelector:@selector(viewProxy:updatedValue:forType:)]) {
        [self.eventOverrideDelegate viewProxy:self updatedValue:value forType:key];
    }
}

-(void)configurationStart:(BOOL)recursive
{
    needsContentChange = allowContentChange = NO;
    [view configurationStart];
    if (recursive) {
        [self makeViewChildrenPerformSelector:@selector(configurationStartNSNumber:) withObject:@(recursive)];
    }
}

-(void)configurationStart
{
    [self configurationStart:NO];
}

-(void)configurationSetNSNumber:(NSNumber*)ready
{
    [self configurationSet:[ready boolValue]];
}

-(void)configurationSet:(BOOL)recursive
{
    [view configurationSet];
    if (recursive) {
        [self makeViewChildrenPerformSelector:@selector(configurationSetNSNumber:) withObject:@(recursive)];
    }
    allowContentChange = YES;
}

-(BOOL)isConfigurationSet
{
    return ![self inReproxy] && view && [view isConfigurationSet];
}

-(void)configurationSet
{
    [self configurationSet:NO];
}



-(id)containsView:(id)args
{
    ENSURE_SINGLE_ARG(args, TiProxy);
    return @([self containsChild:args]);
}

-(BOOL)canBeNextResponder
{
    return !hidden && [[self view] interactionEnabled];
}


@end
