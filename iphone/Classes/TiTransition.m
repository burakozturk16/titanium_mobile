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
{
    TiAnimation* _inAnimation;
    TiAnimation* _outAnimation;
}
@synthesize adTransition = _adTransition;
@synthesize orientation;
@synthesize custom;

-(TiTransition*)initCustomTransitionWithDict:(NSDictionary*)options {
    if (self = [super init]) {
        custom = YES;
        id defaultDuration = [options objectForKey:@"duration"]?[options objectForKey:@"duration"]:@(0);
        id defaultCurve = [options objectForKey:@"curve"]?[options objectForKey:@"curve"]:[NSNull null];
        BOOL reversed =  [TiUtils boolValue:@"reverse" properties:options def:NO];
        NSDictionary* from = [options objectForKey:@"from"];
        NSDictionary* to = [options objectForKey:@"to"];
        _inAnimation = [[TiAnimation alloc] initWithDictionary:@{
                                                                @"from":from,
                                                                @"duration":[from objectForKey:@"duration"]?[from objectForKey:@"duration"]:defaultDuration,
                                                                @"curve":[from objectForKey:@"curve"]?[from objectForKey:@"curve"]:defaultDuration,
                                                                } context:[self executionContext] callback:nil];
        _outAnimation = [[TiAnimation alloc] initWithDictionary:@{
                                                                 @"to":to,
                                                                 @"duration":[to objectForKey:@"duration"]?[to objectForKey:@"duration"]:defaultDuration,
                                                                 @"curve":[from objectForKey:@"curve"]?[to objectForKey:@"curve"]:defaultDuration,
                                                                 } context:[self executionContext] callback:nil];
    }
    return self;
}
-(id)init
{
    if (self = [super init]) {
        custom = NO;
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
        self.duration = duration;
    }
    return self;
}

-(void)dealloc
{
    [super dealloc];
    RELEASE_TO_NIL(_inAnimation);
    RELEASE_TO_NIL(_outAnimation);
    RELEASE_TO_NIL(_adTransition);
}
- (id)initWithADTransition:(ADTransition*)transition
{
    if (self = [super init]) {
        _adTransition = [transition retain];
    }
    return self;
}

-(void)transformView:(UIView*)view withPosition:(CGFloat)position size:(CGSize)size
{
    view.layer.hidden = YES;
    view.layer.transform = CATransform3DIdentity;
}
-(void)transformView:(UIView*)view withPosition:(CGFloat)position
{
    [self transformView:view withPosition:position size:view.bounds.size];
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
