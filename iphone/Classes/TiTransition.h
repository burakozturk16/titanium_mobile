//
//  TiTransion.h
//  Titanium
//
//  Created by Martin Guillon on 14/10/13.
//
//

#import "ADTransition.h"

#define kPerspective -1000
@protocol TiTransition

@required
-(void)transformView:(UIView*)view withPosition:(CGFloat)position;
-(void)transformView:(UIView*)view withPosition:(CGFloat)position adjustTranslation:(BOOL)adjust;
-(void)transformView:(UIView*)view withPosition:(CGFloat)position size:(CGSize)size;
-(BOOL)needsReverseDrawOrder;
-(void)prepareViewHolder:(UIView*)holder;
@end
