//
//  TiLabel.h
//  Titanium
//
//  Created by Martin Guillon on 19/12/13.
//
//

#import "TouchDelegate_Views.h"

@interface TiLabel : TDTTTAttributedLabel

@property (nonatomic, retain) UIColor *strokeColor;
@property (nonatomic, assign) CGFloat strokeWidth;

@property BOOL hasStroke;


@end
