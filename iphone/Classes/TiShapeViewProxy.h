//
//  ShapeViewProxy.h
//  Titanium
//
//  Created by Martin Guillon on 10/08/13.
//
//

#import "TiViewProxy.h"

@interface TiShapeViewProxy : TiViewProxy
{
    NSMutableArray* mShapes;
}
-(NSArray*)shapes;
-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds;
-(BOOL)animating;
-(void)redraw;
@end
