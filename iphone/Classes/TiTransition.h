//
//  TiTransion.h
//  Titanium
//
//  Created by Martin Guillon on 14/10/13.
//
//

#import "ADTransition.h"

@protocol TiTransition

@required
-(void)transformView:(UIView*)view withPosition:(CGFloat)position;
-(BOOL)needsReverseDrawOrder;
@end
