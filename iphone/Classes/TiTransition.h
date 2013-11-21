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
@interface TiTransition: TiAnimation
{
    ADTransition* _adTransition;
}
@property(nonatomic,readonly)	ADTransition* adTransition;
@property(nonatomic,assign)	NWTransition type;
@property(nonatomic,assign)	float duration;
@property(nonatomic,readonly)	ADTransitionOrientation orientation;

- (id)initWithADTransition:(ADTransition*)transition;
-(void)transformView:(UIView*)view withPosition:(CGFloat)position;
-(void)transformView:(UIView*)view withPosition:(CGFloat)position adjustTranslation:(BOOL)adjust;
-(void)transformView:(UIView*)view withPosition:(CGFloat)position size:(CGSize)size;
-(BOOL)needsReverseDrawOrder;
-(void)prepareViewHolder:(UIView*)holder;
-(BOOL)isTransitionVertical;
-(BOOL)isTransitionPush;
-(void)reverseADTransition;
-(void)finishedTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer;
@end
