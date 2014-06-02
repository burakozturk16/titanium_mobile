//
//  TiTransition.m
//  Titanium
//
//  Created by Martin Guillon on 18/10/13.
//
//

#import "TiTransition.h"
#import "TiBase.h"

@implementation TiTransition
@synthesize adTransition = _adTransition;
@synthesize orientation;
-(id)init
{
    if (self = [super init]) {
        
    }
    return self;
}

-(Class) adTransitionClass {
    return [ADTransition class];
}

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)_orientation sourceRect:(CGRect)sourceRect reversed:(BOOL)reversed
{
    if (self = [super init]) {
        _adTransition = [[[self adTransitionClass] alloc] initWithDuration:duration orientation:_orientation sourceRect:sourceRect reversed:reversed];
    }
    return self;
}

-(void)dealloc
{
    [super dealloc];
    RELEASE_TO_NIL(_adTransition);
}
- (id)initWithADTransition:(ADTransition*)transition
{
    if (self = [super init]) {
        _adTransition = [transition retain];
    }
    return self;
}
-(void)transformView:(UIView*)view withPosition:(CGFloat)position adjustTranslation:(BOOL)adjust size:(CGSize)size;
{
}
-(void)transformView:(UIView*)view withPosition:(CGFloat)position adjustTranslation:(BOOL)adjust
{
    [self transformView:view withPosition:position adjustTranslation:adjust size:view.bounds.size];
}
-(void)transformView:(UIView*)view withPosition:(CGFloat)position size:(CGSize)size
{
    [self transformView:view withPosition:position adjustTranslation:NO size:size];
}
-(void)transformView:(UIView*)view withPosition:(CGFloat)position
{
    [self transformView:view withPosition:position adjustTranslation:NO size:view.bounds.size];
}
-(BOOL)needsReverseDrawOrder{
    return NO;
}
-(void)prepareViewHolder:(UIView*)holder{}

-(BOOL)isTransitionPush
{
    return _adTransition && [TiTransitionHelper  isTransitionPush:_adTransition];
}

-(BOOL)isTransitionVertical
{
    return _adTransition && [TiTransitionHelper  isTransitionVertical:_adTransition];
}

-(ADTransitionOrientation)getOrientation
{
    return _adTransition && _adTransition.orientation;
}

-(void)reverseADTransitionForSourceRect:(CGRect)rect
{
    if (_adTransition) {
        ADTransition* reverse = [_adTransition reverseTransitionForSourceRect:rect];
        _adTransition = [reverse retain];
    }
}

-(void)finishedTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer {
}

@end
