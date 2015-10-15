
#import "TouchDelegate_Views.h"

@class TiUIView;
@implementation TDUITableView

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code
    }
    return self;
}

#pragma mark - UIResponder

-(void)runSelector:(SEL)selector forTouches:(NSSet *)touches withEvent:(UIEvent *)event
{
    CGPoint location = [[touches anyObject] locationInView:self];
    UIView* view = [self hitTest:location withEvent:event];
    if (view == self || self == view.superview)
        [self.touchDelegate performSelector:selector withObject:touches withObject:event];
}
- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesBegan:touches withEvent:event];
    [self runSelector:@selector(processTouchesBegan:withEvent:) forTouches:touches withEvent:event];
}

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesMoved:touches withEvent:event];
    [self runSelector:@selector(processTouchesMoved:withEvent:) forTouches:touches withEvent:event];
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesEnded:touches withEvent:event];
    [self runSelector:@selector(processTouchesEnded:withEvent:) forTouches:touches withEvent:event];
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesCancelled:touches withEvent:event];
    [self runSelector:@selector(processTouchesCancelled:withEvent:) forTouches:touches withEvent:event];
}

@end

@implementation TDTTTAttributedLabel

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code
    }
    return self;
}

#pragma mark - TDTTTAttributedLabel

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesBegan:touches withEvent:event];
    [self.touchDelegate processTouchesBegan:touches withEvent:event];
}

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesMoved:touches withEvent:event];
    [self.touchDelegate processTouchesMoved:touches withEvent:event];
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    //process first so that activeLink is still set
    if ([[event touchesForView:self] count] > 0) {
        [self.touchDelegate processTouchesEnded:touches withEvent:event];
    }
    [super touchesEnded:touches withEvent:event];
    
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesCancelled:touches withEvent:event];
    [self.touchDelegate processTouchesCancelled:touches withEvent:event];
}

@end

@implementation TDUIView

#pragma mark - UIResponder

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesBegan:touches withEvent:event];
    [self.touchDelegate processTouchesBegan:touches withEvent:event];
}

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesMoved:touches withEvent:event];
    [self.touchDelegate processTouchesMoved:touches withEvent:event];
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesEnded:touches withEvent:event];
    [self.touchDelegate processTouchesEnded:touches withEvent:event];
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesCancelled:touches withEvent:event];
    [self.touchDelegate processTouchesCancelled:touches withEvent:event];
}

@end


@implementation TDUICollectionView
{
@private
    UIView * touchedContentView;
}
#pragma mark - UIResponder
- (BOOL)touchesShouldBegin:(NSSet *)touches withEvent:(UIEvent *)event inContentView:(UIView *)view
{
    //If the content view is of type TiUIView touch events will automatically propagate
    //If it is not of type TiUIView we will fire touch events with ourself as source
    if (view != self && [view isKindOfClass:[TiUIView class]] || [view respondsToSelector:@selector(touchDelegate)]) {
        touchedContentView= view;
    }
    else {
        touchedContentView = nil;
    }
    return [super touchesShouldBegin:touches withEvent:event inContentView:view];
}
- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesBegan:touches withEvent:event];
    if (!self.dragging && !self.zooming && self.userInteractionEnabled && (touchedContentView == nil) ) {
        [self.touchDelegate processTouchesBegan:touches withEvent:event];
    }
}

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesMoved:touches withEvent:event];
    if (!self.dragging && !self.zooming && self.userInteractionEnabled && (touchedContentView == nil) ) {
        [self.touchDelegate processTouchesMoved:touches withEvent:event];
    }
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesEnded:touches withEvent:event];
    if (!self.dragging && !self.zooming && self.userInteractionEnabled && (touchedContentView == nil) ) {
        [self.touchDelegate processTouchesEnded:touches withEvent:event];
    }
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesCancelled:touches withEvent:event];
    if (!self.dragging && !self.zooming && self.userInteractionEnabled && (touchedContentView == nil) ) {
        [self.touchDelegate processTouchesCancelled:touches withEvent:event];
    }
}

@end

@implementation TDUIScrollView
{
    @private
    UIView * touchedContentView;
}
#pragma mark - UIResponder


- (BOOL)touchesShouldBegin:(NSSet *)touches withEvent:(UIEvent *)event inContentView:(UIView *)view
{
    //If the content view is of type TiUIView touch events will automatically propagate
    //If it is not of type TiUIView we will fire touch events with ourself as source
    if (view != self && [view isKindOfClass:[TiUIView class]] || [view respondsToSelector:@selector(touchDelegate)]) {
        touchedContentView= view;
    }
    else {
        touchedContentView = nil;
    }
    return [super touchesShouldBegin:touches withEvent:event inContentView:view];
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    //When userInteractionEnabled is false we do nothing since touch events are automatically
    //propagated. If it is dragging,tracking or zooming do not do anything.
    if (!self.dragging && !self.zooming
        && self.userInteractionEnabled && (touchedContentView == nil) ) {
        [self.touchDelegate processTouchesBegan:touches withEvent:event];
    }
    [super touchesBegan:touches withEvent:event];
}
- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
    if (!self.dragging && !self.zooming && self.userInteractionEnabled && (touchedContentView == nil) ) {
        [self.touchDelegate processTouchesMoved:touches withEvent:event];
    }
    [super touchesMoved:touches withEvent:event];
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    if (!self.dragging && !self.zooming && self.userInteractionEnabled && (touchedContentView == nil)) {
        [self.touchDelegate processTouchesEnded:touches withEvent:event];
    }
    [super touchesEnded:touches withEvent:event];
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
    if (!self.dragging && !self.zooming && self.userInteractionEnabled && (touchedContentView == nil) ) {
        [self.touchDelegate processTouchesCancelled:touches withEvent:event];
    }
    [super touchesCancelled:touches withEvent:event];
}

-(void)setContentSize:(CGSize)contentSize
{
    [super setContentSize:CGSizeMake(floorf(contentSize.width), floorf(contentSize.height))];
}

//- (void)layoutSubviews {
//    [super layoutSubviews];
//}
@end
