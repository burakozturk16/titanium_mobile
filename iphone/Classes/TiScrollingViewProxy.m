#import "TiScrollingViewProxy.h"
#import "TiScrollingView.h"

@implementation TiScrollingViewProxy


- (void)viewDidInitialize
{
    [super viewDidInitialize];
    if (parentVisible) {
         [[NSNotificationCenter defaultCenter] addObserver:self.view selector:@selector(updateKeyboardInset) name:kTiKeyboardHeightChangedNotification object:nil];
    }
}

- (void)willShow
{
    [[NSNotificationCenter defaultCenter] addObserver:self.view selector:@selector(updateKeyboardInset) name:kTiKeyboardHeightChangedNotification object:nil];
	[super willShow];
}

- (void)willHide
{
    [[NSNotificationCenter defaultCenter] removeObserver:self.view name:kTiKeyboardHeightChangedNotification object:nil];
	[super willHide];
}

-(void)viewWillDetach
{
    [[NSNotificationCenter defaultCenter] removeObserver:self.view name:kTiKeyboardHeightChangedNotification object:nil];
	[super viewWillDetach];
}


-(void) setContentOffset:(id)value withObject:(id)animated
{
    TiThreadPerformOnMainThread(^{
        [(TiScrollingView *)[self view] setContentOffset_:value withObject:animated];
    }, YES);
}

-(void) setZoomScale:(id)value withObject:(id)animated
{
    TiThreadPerformOnMainThread(^{
        [(TiScrollingView *)[self view] setZoomScale_:value withObject:animated];
    }, YES);
}


-(void)showPullView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args,NSNumber);
    [self makeViewPerformSelector:@selector(showPullView:) withObject:args createIfNeeded:NO waitUntilDone:NO];
}

-(void)closePullView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args,NSNumber);
    [self makeViewPerformSelector:@selector(closePullView:) withObject:args createIfNeeded:NO waitUntilDone:NO];
}


-(void)scrollToTop:(id)args
{
    NSInteger top = [TiUtils intValue:[args objectAtIndex:0] def:0];
    NSDictionary *options = [args count] > 1 ? [args objectAtIndex:1] : nil;
    BOOL animated = [TiUtils boolValue:@"animated" properties:options def:YES];
    TiThreadPerformBlockOnMainThread(^{
        [(TiScrollingView*)[self view] setContentOffsetToTop:top animated:animated];
    }, NO);
}

-(void)scrollToBottom:(id)args
{
    NSInteger top = [TiUtils intValue:[args objectAtIndex:0] def:0];
    NSDictionary *options = [args count] > 1 ? [args objectAtIndex:1] : nil;
    BOOL animated = [TiUtils boolValue:@"animated" properties:options def:YES];
    TiThreadPerformBlockOnMainThread(^{
        [(TiScrollingView*)[self view] setContentOffsetToBottom:top animated:animated];
    }, NO);
}
@end
