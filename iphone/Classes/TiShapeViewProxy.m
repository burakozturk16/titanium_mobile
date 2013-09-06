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
	[mShapes release];
	[super dealloc];
}

-(void)_destroy
{
	RELEASE_TO_NIL(mShapes);
	[super _destroy];
}

-(NSArray*)shapes
{
    return [mShapes copy];
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
    for (int i = 0; i < [mShapes count]; i++) {
        ShapeProxy* shapeProxy = [mShapes objectAtIndex:i];
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
        [mShapes removeObject:child];
        [child setShapeViewProxy:nil];
        if ([self viewAttached]) {
            [[self view] setNeedsDisplay];
        }
    }
}

@end
