#import "TiTransitionAnimation+Friend.h"
#import "TiTransitionAnimationStep.h"

#import "TiViewProxy.h"

@interface TiTransitionAnimation ()

@property (nonatomic, retain) TiViewProxy* holderViewProxy;
@property (nonatomic, retain) TiViewProxy* transitionViewProxy;
@property (nonatomic, assign) int transition;
@property (nonatomic, assign) BOOL closeTransition;
@property (nonatomic, assign) BOOL openTransition;

- (TiViewProxy*)holderViewProxy;
- (TiViewProxy*)transitionViewProxy;

@end

@implementation TiTransitionAnimation

#pragma mark Object creation and destruction

- (id)init
{
    if ((self = [super init])) {
        transition = UIViewAnimationTransitionNone;
        m_holderViewProxy = nil;
        closeTransition = NO;
        openTransition = NO;
    }
    return self;
}

-(void)dealloc
{
	RELEASE_TO_NIL(m_holderViewProxy);
	RELEASE_TO_NIL(m_transitionViewProxy);
    [super dealloc];
}

#pragma mark Private API


#pragma mark Accessors and mutators

@synthesize holderViewProxy = m_holderViewProxy;
@synthesize transitionViewProxy = m_transitionViewProxy;
@synthesize transition, openTransition, closeTransition;

- (TiViewProxy*)holderViewProxy
{
    return m_holderViewProxy;
}

- (TiViewProxy*)transitionViewProxy
{
    return m_transitionViewProxy;
}

#pragma mark Reverse animation

- (id)reverseObjectAnimation
{
    // See remarks at the beginning
    TiTransitionAnimation *reverseViewAnimation = [super reverseObjectAnimation];
    reverseViewAnimation.holderViewProxy = self.holderViewProxy;
    reverseViewAnimation.transitionViewProxy = self.transitionViewProxy;
    reverseViewAnimation.transition = self.transition;
    reverseViewAnimation.openTransition = self.openTransition;
    reverseViewAnimation.closeTransition = self.closeTransition;
    return reverseViewAnimation;
}

#pragma mark NSCopying protocol implementation

- (id)copyWithZone:(NSZone *)zone
{
    TiTransitionAnimation *viewAnimationCopy = [super copyWithZone:zone];
    viewAnimationCopy.holderViewProxy = self.holderViewProxy;
    viewAnimationCopy.transitionViewProxy = self.transitionViewProxy;
    viewAnimationCopy.transition = self.transition;
    viewAnimationCopy.openTransition = self.openTransition;
    viewAnimationCopy.closeTransition = self.closeTransition;
    return viewAnimationCopy;
}

@end
