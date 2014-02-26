#import "TiViewAnimation+Friend.h"
#import "TiViewAnimationStep.h"

#import "TiViewProxy.h"
#import "TiPoint.h"
#import "TiColor.h"

/**
 * Please read the remarks at the top of HLSLayerAnimation.m
 */

@interface TiViewAnimation ()

@property (nonatomic, retain) TiViewProxy* tiViewProxy;

@property(nonatomic,retain,readwrite) NSNumber	*zIndex;
@property(nonatomic,retain,readwrite) TiPoint	*center;
@property(nonatomic,retain,readwrite) TiColor	*color;
@property(nonatomic,retain,readwrite) TiColor	*backgroundColor;
@property(nonatomic,retain,readwrite) NSNumber	*opacity;
@property(nonatomic,retain,readwrite) NSNumber	*opaque;
@property(nonatomic,retain,readwrite) NSNumber	*visible;
@property(nonatomic,retain,readwrite) TiProxy	*transform;

- (TiViewProxy*)viewProxy;

@end

@implementation TiViewAnimation

static NSArray *layoutProps;

+ (NSArray *)layoutProps
{
    if (!layoutProps)
        layoutProps = [[NSArray alloc] initWithObjects:@"left", @"right", @"top", @"bottom", @"width", @"height", @"transform", @"fullscreen", nil];
    
    return layoutProps;
}

static NSArray *animProps;

+ (NSArray *)animProps
{
    if (!animProps)
        animProps = [[NSArray alloc] initWithObjects:@"backgroundColor", @"color", @"visible", @"opacity", @"zIndex", nil];
    
    return animProps;
}

#pragma mark Object creation and destruction

- (id)init
{
    if ((self = [super init])) {
    }
    return self;
}

-(void)dealloc
{
    RELEASE_TO_NIL(zIndex);
	RELEASE_TO_NIL(left);
	RELEASE_TO_NIL(right);
	RELEASE_TO_NIL(top);
	RELEASE_TO_NIL(bottom);
	RELEASE_TO_NIL(width);
	RELEASE_TO_NIL(height);
	RELEASE_TO_NIL(center);
	RELEASE_TO_NIL(color);
	RELEASE_TO_NIL(backgroundColor);
	RELEASE_TO_NIL(opacity);
	RELEASE_TO_NIL(opaque);
	RELEASE_TO_NIL(visible);
	RELEASE_TO_NIL(transform);
	RELEASE_TO_NIL(transition);
	RELEASE_TO_NIL(m_tiViewProxy);
    [super dealloc];
}

#pragma mark Private API

-(void)checkParameters
{
    NSDictionary *properties = [self animationProperties];
#define SET_FLOAT_PROP(p,d) \
{\
id v = d==nil ? nil : [d objectForKey:@#p];\
if (v!=nil && ![v isKindOfClass:[NSNull class]]) {\
self.p = [NSNumber numberWithFloat:[TiUtils floatValue:v]];\
}\
}\

#define SET_INT_PROP(p,d) \
{\
id v = d==nil ? nil : [d objectForKey:@#p];\
if (v!=nil && ![v isKindOfClass:[NSNull class]]) {\
self.p = [NSNumber numberWithInt:[TiUtils intValue:v]];\
}\
}\

#define SET_BOOL_PROP(p,d) \
{\
id v = d==nil ? nil : [d objectForKey:@#p];\
if (v!=nil && ![v isKindOfClass:[NSNull class]]) {\
self.p = [NSNumber numberWithBool:[TiUtils boolValue:v]];\
}\
}\

#define SET_POINT_PROP(p,d) \
{\
id v = d==nil ? nil : [d objectForKey:@#p];\
if (v!=nil && ![v isKindOfClass:[NSNull class]]) {\
self.p = [[[TiPoint alloc] initWithPoint:[TiUtils pointValue:v]] autorelease];\
}\
}\

#define SET_COLOR_PROP(p,d) \
{\
id v = d==nil ? nil : [d objectForKey:@#p];\
if (v!=nil && ![v isKindOfClass:[NSNull class]]) {\
self.p = [TiUtils colorValue:v];\
}\
}\

#define SET_LAYOUT_PROP(p,d) \
{\
id v = d==nil ? nil : [d objectForKey:@#p];\
[self setValue:[d objectForKey:@#p] forKey:@#p]; {\
}\
}\

#define SET_PROXY_PROP(p,d) \
{\
id v = d==nil ? nil : [d objectForKey:@#p];\
if (v!=nil && ![v isKindOfClass:[NSNull class]]) {\
self.p = v;\
}\
}\

    leftDefined = rightDefined = topDefined = bottomDefined = widthDefined = heightDefined = transformDefined = fullscreenDefined = NO;
    
    NSMutableSet *intersection = [NSMutableSet setWithArray:[TiViewAnimation layoutProps]];
    [intersection intersectSet:[NSSet setWithArray:[properties allKeys]]];
    
    NSArray *layoutpropsToProcess = [intersection allObjects];
    for (id prop in layoutpropsToProcess) {
        [self setValue:[properties objectForKey:prop] forKey:prop];
    }
    

    SET_FLOAT_PROP(zIndex,properties);
    SET_FLOAT_PROP(opacity,properties);
    SET_BOOL_PROP(visible,properties);
    SET_BOOL_PROP(opaque,properties);
    SET_POINT_PROP(center,properties);
    SET_COLOR_PROP(backgroundColor,properties);
    SET_COLOR_PROP(color,properties);
}


-(void)setValue:(id)value forKey:(NSString *)key
{
    [super setValue:value forKey:key];
    if ([key isEqualToString:@"top"]) {
        RELEASE_TO_NIL(top);
        top = [value retain];
        topDefined = YES;
    }
    else if ([key isEqualToString:@"bottom"]) {
        RELEASE_TO_NIL(bottom);
        bottom = [value retain];
        bottomDefined = YES;
    }
    else if ([key isEqualToString:@"left"]) {
        RELEASE_TO_NIL(left);
        left = [value retain];
        leftDefined = YES;
    }
    else if ([key isEqualToString:@"right"]) {
        RELEASE_TO_NIL(right);
        right = [value retain];
        rightDefined = YES;
    }
    else if ([key isEqualToString:@"width"]) {
        RELEASE_TO_NIL(width);
        width = [value retain];
        widthDefined = YES;
    }
    else if ([key isEqualToString:@"height"]) {
        RELEASE_TO_NIL(height);
        height = [value retain];
        heightDefined = YES;
    }
    else if ([key isEqualToString:@"minWidth"]) {
        RELEASE_TO_NIL(minWidth);
        minWidth = [value retain];
        minWidthDefined = YES;
    }
    else if ([key isEqualToString:@"minHeight"]) {
        RELEASE_TO_NIL(minHeight);
        minHeight = [value retain];
        minHeightDefined = YES;
    }
    else if ([key isEqualToString:@"maxWidth"]) {
        RELEASE_TO_NIL(maxWidth);
        maxWidth = [value retain];
        maxWidthDefined = YES;
    }
    else if ([key isEqualToString:@"maxHeight"]) {
        RELEASE_TO_NIL(maxHeight);
        maxHeight = [value retain];
        maxHeightDefined = YES;
    }
    else if ([key isEqualToString:@"transform"]) {
        RELEASE_TO_NIL(transform);
        transform = [value retain];
        transformDefined = YES;
    }
    else if ([key isEqualToString:@"fullscreen"]) {
        _fullscreen = [TiUtils boolValue:value def:NO];
        fullscreenDefined = YES;
    }
}

-(void)applyLayoutConstraintsForStep:(TiViewAnimationStep*) step
{
    LayoutConstraint *layoutProperties = [m_tiViewProxy layoutProperties];
    if (layoutProperties == NULL) return;
    BOOL doReposition = NO;
    
#define CHECK_LAYOUT_CHANGE(a, b) \
if (b == YES) \
{\
layoutProperties->a = TiDimensionFromObject(a); \
doReposition = YES;\
}
    
    CHECK_LAYOUT_CHANGE(left, leftDefined);
    CHECK_LAYOUT_CHANGE(right, rightDefined);
    CHECK_LAYOUT_CHANGE(width, widthDefined);
    CHECK_LAYOUT_CHANGE(height, heightDefined);
    CHECK_LAYOUT_CHANGE(top, topDefined);
    CHECK_LAYOUT_CHANGE(bottom, bottomDefined);
    if (fullscreenDefined) {
        layoutProperties->fullscreen = _fullscreen;
        doReposition = YES;
    }
    if (center!=nil && layoutProperties != NULL)
    {
        layoutProperties->centerX = [center xDimension];
        layoutProperties->centerY = [center yDimension];
        
        doReposition = YES;
    }
    if (doReposition)
    {
        [m_tiViewProxy repositionWithinAnimation:step];
    }
}

-(void)applyOnView:(UIView*)_view forStep:(TiViewAnimationStep*) step
{
    //that could be the future but for now it doesnt work because
    //applyProperties will set the actual object props which we dont want
    
//    [m_tiViewProxy applyProperties:[m_animationProxy allProperties]];
//    [m_tiViewProxy setRunningAnimationRecursive:step];
//    [m_tiViewProxy refreshViewOrParent];
//    [m_tiViewProxy setRunningAnimationRecursive:nil];
    if ([_view isKindOfClass:[TiUIView class]])
    {
        TiUIView *uiview = (TiUIView*)_view;
        
        if (transformDefined == YES)
        {
            [uiview setTransform_:transform];
        }
        
        [self applyLayoutConstraintsForStep:step];
        
        if (zIndex!=nil)
        {
            [m_tiViewProxy setVzIndex:[zIndex intValue]];
        }
    
        if (backgroundColor!=nil)
        {
            //we have to use setBackgroundColor_ because setBackgroundColor has been overriden
            // on purpose
            [_view setBackgroundColor_:[backgroundColor _color]];
        }
        
        if (color!=nil && [uiview respondsToSelector:@selector(setColor_:)])
        {
            [uiview performSelector:@selector(setColor_:) withObject:color];
        }
        
        if (opacity!=nil)
        {
            uiview.alpha = [opacity floatValue];
        }
        
        if (opaque!=nil)
        {
            uiview.opaque = [opaque boolValue];
        }
        
        if (visible!=nil)
        {
            uiview.hidden = ![visible boolValue];
        }
    }
}

#pragma mark Accessors and mutators

@synthesize tiViewProxy = m_tiViewProxy;
@synthesize zIndex, center, color, backgroundColor, opacity, opaque, visible, transform;

- (TiViewProxy*)viewProxy
{
    return m_tiViewProxy;
}

#pragma mark Reverse animation

- (id)reverseObjectAnimation
{
    // See remarks at the beginning
    TiViewAnimation *reverseViewAnimation = [super reverseObjectAnimation];
    reverseViewAnimation.tiViewProxy = self.tiViewProxy;
    return reverseViewAnimation;
}

#pragma mark NSCopying protocol implementation

- (id)copyWithZone:(NSZone *)zone
{
    TiViewAnimation *viewAnimationCopy = [super copyWithZone:zone];
    viewAnimationCopy.tiViewProxy = self.tiViewProxy;
    return viewAnimationCopy;
}

@end
