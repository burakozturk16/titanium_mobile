/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiProxy.h"

/**
 The proxy representing a 2D matrix for an affine transform.
 */
@interface Ti2DMatrix : TiProxy {
}

/**
 Initializes the proxy with properties.
 @param dict_ The properties dictionary.
 */
-(id)initWithProperties:(NSDictionary*)dict_;

@property(nonatomic,assign) BOOL ownFrameCoord;

-(Ti2DMatrix*)translate:(id)args;
-(Ti2DMatrix*)scale:(id)args;
-(Ti2DMatrix*)rotate:(id)args;
-(Ti2DMatrix*)invert:(id)args;
-(Ti2DMatrix*)multiply:(id)args;

-(CGAffineTransform) matrixInViewSize:(CGSize)size andParentSize:(CGSize)parentSize;
-(CGAffineTransform) matrixInViewSize:(CGSize)size andParentSize:(CGSize)parentSize decale:(CGSize)decale;

@end

