/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "Ti2DMatrix.h"
#import "TiPoint.h"

typedef enum
{
	AffineOpTranslate = 0,
	AffineOpRotate,
	AffineOpScale,
	AffineOpMultiply,
	AffineOpInverse
} AffineOpType;


@interface Ti2DMatrix()
{
    NSMutableArray* _operations;
    TiPoint* _anchor;
}
@property(nonatomic,retain) NSMutableArray* operations;
@property(nonatomic,retain) TiPoint* anchor;
-(id)initWithMatrix:(Ti2DMatrix*)matrix_;

@end

@interface AffineOp: NSObject <NSCopying>
{
    TiPoint* _anchor;
    TiPoint* _translateValue;
    Ti2DMatrix* _multiplyValue;
    float angle;
    float scaleX;
    float scaleY;
}
@property(nonatomic,assign) AffineOpType type;
@property(nonatomic,assign) float angle;
@property(nonatomic,assign) float scaleX;
@property(nonatomic,assign) float scaleY;
@property(nonatomic,retain) TiPoint* anchor;
@property(nonatomic,retain) TiPoint* translateValue;
@property(nonatomic,retain) Ti2DMatrix* multiplyValue;
-(CGAffineTransform) apply:(CGAffineTransform)transform inSize:(CGSize)size andParentSize:(CGSize)parentSize;
+(AffineOp*)affineOpForMatrix:(Ti2DMatrix*)matrix;
+(AffineOp*)translateOpForMatrix:(Ti2DMatrix*)matrix;
+(AffineOp*)scaleOpForMatrix:(Ti2DMatrix*)matrix;
+(AffineOp*)rotateOpForMatrix:(Ti2DMatrix*)matrix;
+(AffineOp*)inverseOpForMatrix:(Ti2DMatrix*)matrix;
+(AffineOp*)multiplyOpForMatrix:(Ti2DMatrix*)matrix;
@end

@implementation AffineOp
@synthesize anchor = _anchor, translateValue = _translateValue, multiplyValue = _multiplyValue;
@synthesize type, angle, scaleX, scaleY;

+(AffineOp*)affineOpForMatrix:(Ti2DMatrix*)matrix
{
    return [[[AffineOp alloc] initWithMatrix:matrix] autorelease];
}

+(AffineOp*)translateOpForMatrix:(Ti2DMatrix*)matrix
{
    AffineOp* result = [[AffineOp alloc] initWithMatrix:matrix];
    result.type = AffineOpTranslate;
    return [result autorelease];
}

+(AffineOp*)scaleOpForMatrix:(Ti2DMatrix*)matrix
{
    AffineOp* result = [[AffineOp alloc] initWithMatrix:matrix];
    result.type = AffineOpScale;
    return [result autorelease];
}

+(AffineOp*)rotateOpForMatrix:(Ti2DMatrix*)matrix
{
    AffineOp* result = [[AffineOp alloc] initWithMatrix:matrix];
    result.type = AffineOpRotate;
    return [result autorelease];
}

+(AffineOp*)inverseOpForMatrix:(Ti2DMatrix*)matrix
{
    AffineOp* result = [[AffineOp alloc] initWithMatrix:matrix];
    result.type = AffineOpInverse;
    return [result autorelease];
}

+(AffineOp*)multiplyOpForMatrix:(Ti2DMatrix*)matrix
{
    AffineOp* result = [[AffineOp alloc] initWithMatrix:matrix];
    result.type = AffineOpMultiply;
    return [result autorelease];
}

-(id)initWithMatrix:(Ti2DMatrix*)matrix
{
	if (self = [super init])
	{
        angle = 0;
        scaleX = 1;
        scaleY = 1;
        _anchor = [matrix.anchor copy];
        _translateValue = [[TiPoint alloc] init];
        [_translateValue setPoint:CGPointZero];
	}
	return self;
}


-(void)dealloc
{
	RELEASE_TO_NIL(_anchor);
	RELEASE_TO_NIL(_translateValue);
	RELEASE_TO_NIL(_multiplyValue);
	[super dealloc];
}

-(id)copyWithZone:(NSZone *)zone
{
    // We'll ignore the zone for now
    AffineOp *another = [[AffineOp alloc] init];
    another.type = type;
    another.angle = angle;
    another.scaleX = scaleX;
    another.scaleY = scaleY;
    
    another.multiplyValue = _multiplyValue;
    another.anchor = [[_anchor copy] autorelease];
    another.translateValue = [[_translateValue copy] autorelease];
    
    return another;
}

-(CGAffineTransform) apply:(CGAffineTransform)transform inSize:(CGSize)size andParentSize:(CGSize)parentSize decale:(CGSize)decale
{
    CGPoint anchor;
    if (type == AffineOpRotate || type == AffineOpScale) {
        anchor = [_anchor pointWithinSize:size];
        anchor.x = decale.width - anchor.x;
        anchor.y = decale.height - anchor.y;
    }
    int inFactor = -1;
    int outFactor = - inFactor;
    switch (type) {
        case AffineOpTranslate: {
            CGPoint translatePoint = [_translateValue pointWithinSize:parentSize];
            return CGAffineTransformTranslate(transform,translatePoint.x,translatePoint.y);
            break;
        }
        case AffineOpRotate: {
            if (CGPointEqualToPoint(anchor, CGPointZero)){
                return CGAffineTransformRotate(transform, angle);
            }
            else {
                CGAffineTransform result = CGAffineTransformTranslate(transform, inFactor * anchor.x, inFactor * anchor.y);
                result = CGAffineTransformRotate(result, angle);
                return CGAffineTransformTranslate(result, outFactor * anchor.x, outFactor * anchor.y);
            }
            
            break;
        }
        case AffineOpScale: {
            if (CGPointEqualToPoint(anchor, CGPointZero)){
                return CGAffineTransformScale(transform, (scaleX == 0)?0.0001:scaleX, (scaleY == 0)?0.0001:scaleY);
            }
            else {
                CGAffineTransform result = CGAffineTransformTranslate(transform, inFactor * anchor.x, inFactor * anchor.y);
                result = CGAffineTransformScale(result, scaleX, scaleY);
                return CGAffineTransformTranslate(result, outFactor * anchor.x, outFactor * anchor.y);
            }
            
            break;
        }
        case AffineOpMultiply: {
            return  (_multiplyValue)?CGAffineTransformConcat(transform,[_multiplyValue matrixInViewSize:size andParentSize:parentSize]):transform;
            break;
        }
        case AffineOpInverse: {
            return  CGAffineTransformInvert(transform);
            break;
        }
        default:
            return transform;
            break;
    }
}
-(CGAffineTransform) apply:(CGAffineTransform)transform inSize:(CGSize)size andParentSize:(CGSize)parentSize
{
    CGSize decale = CGSizeMake(size.width/2, size.height/2);
    return [self apply:transform inSize:size andParentSize:parentSize decale:decale];
}
@end


@implementation Ti2DMatrix
@synthesize operations = _operations, anchor = _anchor;
-(id)init
{
	if (self = [super init])
	{
        _ownFrameCoord = NO;
        _operations = [[NSMutableArray alloc] init];
        _anchor = [[TiPoint alloc] initWithObject:[NSDictionary dictionaryWithObjectsAndKeys:@"50%", @"x", @"50%", @"y", nil]];
	}
	return self;
}


-(void)dealloc
{
	RELEASE_TO_NIL(_operations);
	RELEASE_TO_NIL(_anchor);
	[super dealloc];
}

//-(id)initWithMatrix:(CGAffineTransform)matrix_
//{
//	if (self = [self init])
//	{
//		matrix = matrix_;
//	}
//	return self;
//}

-(id)initWithMatrix:(Ti2DMatrix*)matrix_
{
	if (self = [self init])
	{
        _ownFrameCoord = matrix_.ownFrameCoord;
		NSArray* otherOperations = matrix_.operations;
        for (AffineOp* op in otherOperations) {
            [_operations addObject: [[op copy] autorelease]];
        }
	}
	return self;
}

-(id)initWithProperties:(NSDictionary*)dict_
{
	if (self = [self init])
	{
        if ([dict_ objectForKey:@"anchorPoint"]!=nil)
		{
            [_anchor setValues:[dict_ objectForKey:@"anchorPoint"]];
		}
		if ([dict_ objectForKey:@"rotate"]!=nil)
		{
			CGFloat angle = [[dict_ objectForKey:@"rotate"] floatValue];
            AffineOp* operation = [AffineOp rotateOpForMatrix:self];
            operation.angle = degreesToRadians(angle);
            [_operations addObject:operation];
		}
		if ([dict_ objectForKey:@"scale"]!=nil)
		{
			CGFloat xy = [[dict_ objectForKey:@"scale"] floatValue];
            AffineOp* operation = [AffineOp scaleOpForMatrix:self];
            operation.scaleX = xy;
            operation.scaleY = xy;
            [_operations addObject:operation];
		}
	}
	return self;
}

-(NSString*)apiName
{
    return @"Ti.UI.2DMatrix";
}

-(CGAffineTransform) matrixInViewSize:(CGSize)size andParentSize:(CGSize)parentSize
{
    if (_ownFrameCoord) parentSize = size;
    CGAffineTransform result = CGAffineTransformIdentity;
    for (AffineOp* op in _operations) {
        result = [op apply:result inSize:size andParentSize:parentSize];
    }
    return result;
}

-(CGAffineTransform) matrixInViewSize:(CGSize)size andParentSize:(CGSize)parentSize decale:(CGSize)decale
{
    if (_ownFrameCoord) parentSize = size;
    CGAffineTransform result = CGAffineTransformIdentity;
    for (AffineOp* op in _operations) {
        result = [op apply:result inSize:size andParentSize:parentSize decale:decale];
    }
    return result;
}


-(void)setAnchorPoint:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSDictionary)
    [_anchor setValues:args];
}

-(Ti2DMatrix*)translate:(id)args
{
    Ti2DMatrix* newMatrix = [[Ti2DMatrix alloc] initWithMatrix:self];
    
    if ([args count] >= 2) {
        TiPoint* translation = [[TiPoint alloc] init];
        [translation setX:[args objectAtIndex:0]];
        [translation setY:[args objectAtIndex:1]];
        AffineOp* operation = [AffineOp translateOpForMatrix:self];
        operation.translateValue = [translation autorelease];
        [newMatrix.operations addObject:operation];
    }
	return [newMatrix autorelease];
}

-(Ti2DMatrix*)scale:(id)args
{
    Ti2DMatrix* newMatrix = [[Ti2DMatrix alloc] initWithMatrix:self];
    
    if ([args count] == 1) {
        AffineOp* operation = [AffineOp scaleOpForMatrix:self];
        operation.scaleX = operation.scaleY = [[args objectAtIndex:0] floatValue];
        [newMatrix.operations addObject:operation];
    }
    else if ([args count] >= 2) {
        AffineOp* operation = [AffineOp scaleOpForMatrix:self];
        operation.scaleX = [[args objectAtIndex:0] floatValue];
        operation.scaleY = [[args objectAtIndex:1] floatValue];
        [newMatrix.operations addObject:operation];
    }
	return [newMatrix autorelease];
}

-(Ti2DMatrix*)rotate:(id)args
{
    Ti2DMatrix* newMatrix = [[Ti2DMatrix alloc] initWithMatrix:self];
    
    if ([args count] >= 1) {
        AffineOp* operation = [AffineOp rotateOpForMatrix:self];
        operation.angle = degreesToRadians([[args objectAtIndex:0] floatValue]);
        [newMatrix.operations addObject:operation];
    }
	return [newMatrix autorelease];
}

-(Ti2DMatrix*)invert:(id)args
{
    Ti2DMatrix* newMatrix = [[Ti2DMatrix alloc] initWithMatrix:self];
    [newMatrix.operations addObject:[AffineOp inverseOpForMatrix:self]];
	return [newMatrix autorelease];
}

-(Ti2DMatrix*)multiply:(id)args
{
	ENSURE_SINGLE_ARG(args,Ti2DMatrix);
    Ti2DMatrix* newMatrix = [[Ti2DMatrix alloc] initWithMatrix:self];
    AffineOp* operation = [AffineOp multiplyOpForMatrix:self];
    operation.multiplyValue = args;
    [newMatrix.operations addObject:operation];
	return [newMatrix autorelease];
}

-(id)description
{
	return @"[object Ti2DMatrix]";
}

@end
