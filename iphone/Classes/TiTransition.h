//
//  TiTransion.h
//  Titanium
//
//  Created by Martin Guillon on 14/10/13.
//
//

#import "ADTransition.h"
#import "TiTransitionHelper.h"
#import "TiAnimation.h"

#define kPerspective -1000

static inline NSString* NSStringFromCATransform3D(CATransform3D transform) {
    return [NSString stringWithFormat:@"[%f %f %f %f; %f %f %f %f; %f %f %f %f; %f %f %f %f]",
            transform.m11,
            transform.m12,
            transform.m13,
            transform.m14,
            transform.m21,
            transform.m22,
            transform.m23,
            transform.m24,
            transform.m31,
            transform.m32,
            transform.m33,
            transform.m44,
            transform.m41,
            transform.m42,
            transform.m43,
            transform.m44
            ];
    
}


@interface TiTransition: TiAnimation
{
    ADTransition* _adTransition;
}
@property(nonatomic,readonly)	ADTransition* adTransition;
@property(nonatomic,assign)	NWTransition type;
//@property(nonatomic,assign)	CGFloat duration;
@property(nonatomic,readonly)	ADTransitionOrientation orientation;

@property(nonatomic,assign)	BOOL custom;


-(TiTransition*)initCustomTransitionWithDict:(NSDictionary*)options;

- (id)initWithADTransition:(ADTransition*)transition;
-(void)transformView:(UIView*)view withPosition:(CGFloat)position;
-(void)transformView:(UIView*)view withPosition:(CGFloat)position size:(CGSize)size;
-(BOOL)needsReverseDrawOrder;
-(void)prepareViewHolder:(UIView*)holder;
-(BOOL)isTransitionVertical;
-(BOOL)isTransitionPush;
-(void)reverseADTransitionForSourceRect:(CGRect)rect;
-(void)finishedTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer;
-(Class) adTransitionClass;
- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)_orientation sourceRect:(CGRect)sourceRect reversed:(BOOL)reversed;
@end
