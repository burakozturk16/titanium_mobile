//
//  ShapeViewProxy.m
//  Titanium
//
//  Created by Martin Guillon on 10/08/13.
//
//

#import "TiShapeViewProxy.h"
#import "ShapeProxy.h"

@implementation TiShapeViewProxy
- (id)init {
    if (self = [super init])
    {
        mShapes = [[NSMutableArray alloc] init];
    }
    return self;
}

- (void) dealloc
{
    for (ShapeProxy* proxy in mShapes) {
        [proxy setShapeViewProxy:nil];
        [self forgetProxy:proxy];
    }
	RELEASE_TO_NIL(mShapes);
	[super dealloc];
}

-(NSArray*)shapes
{
    return [[mShapes copy] autorelease];
}

-(void)detachView
{
    ENSURE_UI_THREAD_0_ARGS
    for (ShapeProxy* shape in mShapes) {
        [shape removeFromSuperLayer];
    }
	[super detachView];
}

static NSArray *supportedEvents;
+ (NSArray *)supportedEvents
{
    if (!supportedEvents)
        supportedEvents = [[NSArray arrayWithObjects:@"click",@"dbclick",@"singtap",@"doubletap"
                          ,@"longpress",@"touchstart", @"touchmove"
                          , @"touchend", @"touchcancel",nil] retain];
    
    return supportedEvents;
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
    if (CGSizeEqualToSize(bounds.size,CGSizeZero)) return;
    for (int i = 0; i < [mShapes count]; i++) {
        ShapeProxy* shapeProxy = [mShapes objectAtIndex:i];
        [shapeProxy boundsChanged:bounds];
    }
}

-(void)update:(id)arg
{
    if (![self viewAttached]) return;
    ENSURE_UI_THREAD_1_ARG(arg)
    [CATransaction begin];
    [CATransaction setDisableActions: YES];
    for (ShapeProxy* shapeProxy in mShapes) {
        [shapeProxy boundsChanged:[self view].bounds];
    }
    [[self view] setNeedsDisplay];
    [CATransaction commit];
    
}

-(void)redraw
{
    [[self view] setNeedsDisplay];
}

-(void)add:(id)args
{
    ShapeProxy * shape = [ShapeProxy shapeFromArg:args context:[self executionContext]];

   if (shape != nil) {
       if ([mShapes indexOfObject:shape] == NSNotFound) {
           [mShapes addObject:shape];
           [self rememberProxy:shape];
           [shape setShapeViewProxy:self];
           if ([self viewAttached]) {
               [shape boundsChanged:self.view.bounds];
               [[self view] setNeedsDisplay];
           }
       }
   }
   else {
       [super add:args];
   }
}

-(BOOL)animating
{
    return [self viewAttached] && [[self view] animating];
}

-(void)remove:(id)child
{
 	ENSURE_SINGLE_ARG(child,TiProxy);
    if ([child isKindOfClass:[TiViewProxy class]]) {
        [super remove:child];
        return;
    }
    ENSURE_SINGLE_ARG(child, ShapeProxy)
    if ([mShapes indexOfObject:child] != NSNotFound) {
        [self forgetProxy:child];
        [mShapes removeObject:child];
        [child setShapeViewProxy:nil];
        if ([self viewAttached]) {
            [[self view] setNeedsDisplay];
        }
    }
}

-(BOOL)_hasListeners:(NSString *)type
{
    BOOL handledByChildren = NO;
    for (int i = 0; i < [mShapes count]; i++) {
        ShapeProxy* shapeProxy = [mShapes objectAtIndex:i];
        handledByChildren |= [shapeProxy _hasListeners:type];
    }
	return [super _hasListeners:type] || handledByChildren;
}

-(void)fireEvent:(NSString*)type withObject:(id)obj propagate:(BOOL)yn
{
	if ([[TiShapeViewProxy supportedEvents] indexOfObject:type] != NSNotFound && [mShapes count] > 0) {
        CGPoint point  = CGPointMake(-1, -1);
        if ([obj isKindOfClass:[NSDictionary class]]) {
            point.x = [[((NSDictionary*)obj) objectForKey:@"x"] intValue];
            point.y = [[((NSDictionary*)obj) objectForKey:@"y"] intValue];
        }
        BOOL handledByChildren = NO;
        
        for (int i = 0; i < [mShapes count]; i++) {
            ShapeProxy* shapeProxy = [mShapes objectAtIndex:i];
            handledByChildren |= [shapeProxy handleTouchEvent:type withObject:obj propagate:yn point:point];
        }
        if (handledByChildren && yn) {
            return YES;
        }
    }
    return [super fireEvent:type withObject:obj propagate:yn];
}



@end
