//
//  DirectionPanGestureRecognizer.h
//  MapMe
//
//  Created by Martin Guillon on 26/07/2015.
//
//

#import "TiBase.h"

typedef enum {
    DirectionPangestureRecognizerAll,
    DirectionPangestureRecognizerVertical,
    DirectionPanGestureRecognizerHorizontal
} DirectionPangestureRecognizerDirection;

@interface DirectionPanGestureRecognizer : UIPanGestureRecognizer 
@property (nonatomic, assign) DirectionPangestureRecognizerDirection direction;

@end

TI_INLINE DirectionPangestureRecognizerDirection PanDirectionFromObject(id object)
{
    if ([object isKindOfClass:[NSString class]])
    {
        if ([object caseInsensitiveCompare:@"vertical"]==NSOrderedSame)
        {
            return DirectionPangestureRecognizerVertical;
        }
        if ([object caseInsensitiveCompare:@"horizontal"]==NSOrderedSame)
        {
            return DirectionPanGestureRecognizerHorizontal;
        }
    }
    return DirectionPangestureRecognizerAll;
}