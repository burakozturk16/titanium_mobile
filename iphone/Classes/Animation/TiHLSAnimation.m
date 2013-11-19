#import "TiViewAnimation.h"
#import "HLSObjectAnimation+Friend.h"
#import "TiAnimation.h"
#import "TiAnimatableProxy.h"

@interface TiHLSAnimation ()

@property (nonatomic, retain) TiAnimation* animationProxy;
@property (nonatomic, retain) TiAnimatableProxy* animatedProxy;
@property (nonatomic, assign) BOOL isReversed;

- (TiAnimation*)animationProxy;
- (TiAnimatableProxy*)animatedProxy;
- (BOOL)isReversed;

@end

@implementation TiHLSAnimation

#pragma mark Object creation and destruction

- (id)init
{
    if ((self = [super init])) {
        m_isReversed = NO;
    }
    return self;
}

-(void)dealloc
{
    RELEASE_TO_NIL(m_animatedProxy);
	RELEASE_TO_NIL(m_animationProxy);
    [super dealloc];
}

#pragma mark Accessors and mutators

@synthesize animationProxy = m_animationProxy;
@synthesize animatedProxy = m_animatedProxy;
@synthesize isReversed = m_isReversed;

- (TiAnimation*)animationProxy
{
    return m_animationProxy;
}

- (TiAnimatableProxy*)animatedProxy
{
    return m_animatedProxy;
}

- (BOOL)isReversed
{
    return m_isReversed;
}

#pragma mark API

-(NSDictionary*)animationProperties
{
    if (m_animationProxy) {
        return [m_animationProxy propertiesForAnimation:self];
    }
    return nil;
}

#pragma mark Reverse animation

- (id)reverseObjectAnimation
{
    // See remarks at the beginning
    TiHLSAnimation *reverseAnimation = [super reverseObjectAnimation];
    reverseAnimation.animationProxy = self.animationProxy;
    reverseAnimation.animatedProxy = self.animatedProxy;
    reverseAnimation.isReversed = !self.isReversed;
    return reverseAnimation;
}

#pragma mark NSCopying protocol implementation

- (id)copyWithZone:(NSZone *)zone
{
    TiHLSAnimation *animationCopy = [super copyWithZone:zone];
    animationCopy.animationProxy = self.animationProxy;
    animationCopy.animatedProxy = self.animatedProxy;
    animationCopy.isReversed = self.isReversed;
    return animationCopy;
}

#pragma mark Description

- (NSString *)description
{
    return [NSString stringWithFormat:@"<%@: %p>",
            [self class],
            self];
}

@end
