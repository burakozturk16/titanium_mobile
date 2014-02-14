#import "TiHLSAnimationStep.h"
#import "HLSAnimationStep+Friend.h"
#import "TiAnimation.h"


@interface TiHLSAnimationStep ()

@end

@implementation TiHLSAnimationStep

#pragma mark Object creation and destruction

- (id)init
{
    if ((self = [super init])) {
        self.curve = nil;
    }
    return self;
}

- (void)dealloc
{
    RELEASE_TO_NIL(m_curve);
    [super dealloc];
}

#pragma mark Accessors and mutators

@synthesize curve = m_curve;

#pragma mark Managing the animation


- (NSTimeInterval)elapsedTime
{
    return self.duration;
}

#pragma mark Reverse animation

- (id)reverseAnimationStep
{
    TiHLSAnimationStep *reverseAnimationStep = [super reverseAnimationStep];
    reverseAnimationStep.curve = [TiAnimation reverseCurve:self.curve];

    return reverseAnimationStep;
}

#pragma mark NSCopying protocol implementation

- (id)copyWithZone:(NSZone *)zone
{
    TiHLSAnimationStep *animationStepCopy = [super copyWithZone:zone];
    animationStepCopy.curve = self.curve;
    return animationStepCopy;
}

@end
