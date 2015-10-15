#import "TiUIView.h"

@interface TiScrollingView : TiUIView<UIScrollViewDelegate>
{
    BOOL scrollingEnabled;
}
- (void)zoomToPoint:(CGPoint)zoomPoint withScale: (CGFloat)scale animated: (BOOL)animated;

- (NSMutableDictionary *) eventObjectForScrollView: (UIScrollView *) scrollView;
// For now, this is fired on `scrollstart` and `scrollend`
- (void)fireScrollEvent:(NSString*)eventName forScrollView:(UIScrollView*)scrollView withAdditionalArgs:(NSDictionary*)args;
- (void)fireScrollEvent:(UIScrollView *)scrollView;
- (void)scrollViewDidZoom:(UIScrollView *)scrollView_;
- (void)scrollViewDidEndZooming:(UIScrollView *)scrollView_ withView:(UIView *)view atScale:(CGFloat)scale;
- (void)scrollViewDidScroll:(UIScrollView *)scrollView;
- (void)scrollViewWillBeginDragging:(UIScrollView *)scrollView;
- (void)scrollViewDidEndDragging:(UIScrollView *)scrollView willDecelerate:(BOOL)decelerate;
- (void)scrollViewDidEndDecelerating:(UIScrollView *)scrollView;
- (BOOL)scrollViewShouldScrollToTop:(UIScrollView *)scrollView;
- (void)scrollViewDidScrollToTop:(UIScrollView *)scrollView;
-(void)setContentOffset_:(id)value withObject:(id)property;
-(void)setZoomScale_:(id)value withObject:(id)property;
-(void)proxyDidRelayout:(id)sender;
-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds;
-(void)setContentOffsetToTop:(NSInteger)top animated:(BOOL)animated;
-(void)setContentOffsetToBottom:(NSInteger)bottom animated:(BOOL)animated;
@end
