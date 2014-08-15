/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UIOPTIONDIALOG

#import "TiUIOptionDialogProxy.h"
#import "TiUtils.h"
#import "TiApp.h"
#import "TiToolbar.h"
#import "TiToolbarButton.h"
#import	"TiTab.h"

@interface TiCustomActionSheet : CustomActionSheet
{
    TiViewProxy* _customView;
}
@property (nonatomic, readwrite, retain) TiViewProxy *customView;
@property(nonatomic, readwrite) BOOL hideOnClick;
-(void)setCustomView:(id)value fromProxy:(TiProxy*)parentProxy;

@end

@implementation TiCustomActionSheet
@synthesize customView = _customView;
@synthesize hideOnClick;


-(void)setCustomView:(id)value fromProxy:(TiParentingProxy*)parentProxy
{
    if (_customView){
        [_customView detachView];
        //            [_customView setParent:nil];
        [parentProxy forgetProxy:_customView];
        _customView = nil;
        RELEASE_TO_NIL(_customView)
    }
    
    TiViewProxy* vp = ( TiViewProxy*)[parentProxy createChildFromObject:value];
    if (vp) {
        _customView = [vp retain];
        LayoutConstraint* constraint = [vp layoutProperties];
        if (TiDimensionIsUndefined(constraint->top))
        {
            constraint->top = TiDimensionDip(0);
        }
    }
}

- (void) dealloc
{
    if (_customView) {
        [_customView detachView];
        RELEASE_TO_NIL(_customView);
    }
	[super dealloc];
}

- (UIView *)configuredCustomView {
    //make sure we are detached first so that  the size is computed correctly
    [_customView detachView];
    CGRect frame = [TiUtils appFrame];
    // we don't want to take the status bar height in account
    frame = CGRectMake(0, 0, frame.size.width, frame.size.height);
    TiUIView* tiview = [_customView getAndPrepareViewForOpening:frame];
    CGRect tiBounds = tiview.bounds;
    UIView* view  = [[UIView alloc] initWithFrame:tiBounds];
    view.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    [view addSubview:tiview];
    return view;
}

- (IBAction)customButtonPressed:(id)sender {
    UIBarButtonItem *button = (UIBarButtonItem*)sender;
    NSInteger index = button.tag;
    NSAssert((index >= 0 && index < self.customButtons.count), @"Bad custom button tag: %d, custom button count: %d", index, self.customButtons.count);
}

@end


@implementation TiUIOptionDialogProxy
{
    UIDeviceOrientation currentOrientation;
    UIActionSheet *actionSheet;
    TiCustomActionSheet *customActionSheet;
    //We need to hold onto this information for whenever the status bar rotates.
    TiViewProxy *dialogView;
    CGRect dialogRect;
    BOOL animated;
    NSUInteger accumulatedOrientationChanges;
    BOOL showDialog;
    BOOL persistentFlag;
    BOOL forceOpaqueBackground;
    BOOL hideOnClick;
}

@synthesize dialogView;

- (void) dealloc
{
    if (customActionSheet) {
        customActionSheet.customView = nil;
        RELEASE_WITH_DELEGATE(customActionSheet)
    }
    RELEASE_WITH_DELEGATE(actionSheet)
	RELEASE_TO_NIL(dialogView);
	[super dealloc];
}

-(void)clearCustomActionSheet {
    showDialog = NO;
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    if (customActionSheet) {
        customActionSheet.customView = nil;
        RELEASE_WITH_DELEGATE(customActionSheet)
    }
    [self forgetSelf];
    [self release];
}

-(NSMutableDictionary*)langConversionTable
{
    return [NSMutableDictionary dictionaryWithObject:@"title" forKey:@"titleid"];
}

-(NSString*)apiName
{
    return @"Ti.UI.OptionDialog";
}

- (UIBarButtonItem *)createButtonWithTitle:(NSString*)title target:(id)target action:(SEL)buttonAction {
    
    UIBarButtonItem *barButton = [[UIBarButtonItem alloc] initWithTitle:title style:UIBarButtonItemStylePlain target:target action:buttonAction];
    
    if (NSFoundationVersionNumber > NSFoundationVersionNumber_iOS_6_1)
        [barButton setTintColor: [[UIApplication sharedApplication] keyWindow].tintColor];
    
    return barButton;
}

- (UIBarButtonItem *)createButtonWithStyle:(UIBarButtonSystemItem)style target:(id)target action:(SEL)buttonAction {
    
    UIBarButtonItem *barButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:style target:target action:buttonAction];
    
    if (NSFoundationVersionNumber > NSFoundationVersionNumber_iOS_6_1)
        [barButton setTintColor: [[UIApplication sharedApplication] keyWindow].tintColor];
    
    return barButton;
}


-(void)setTitle:(id)title
{
    ENSURE_UI_THREAD_1_ARG(title)
	[self replaceValue:title forKey:@"title" notification:NO];
	if (customActionSheet) {
        
        [customActionSheet setHtmlTitle:[TiUtils stringValue:title]];
    }
    else if (actionSheet) {
        [actionSheet setTitle:[TiUtils stringValue:title]];
    }
}

-(void)show:(id)args
{
	ENSURE_SINGLE_ARG_OR_NIL(args,NSDictionary);
	[self rememberSelf];
	ENSURE_UI_THREAD(show,args);
    
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(suspended:) name:kTiSuspendNotification object:nil];
    
	showDialog = YES;
	NSMutableArray *options = [self valueForKey:@"options"];
	if (options==nil)
	{
		options = [[[NSMutableArray alloc] initWithCapacity:2] autorelease];
		[options addObject:NSLocalizedString(@"OK",@"Alert OK Button")];
	}
    
    hideOnClick = [TiUtils boolValue:[self valueForKey:@"hideOnClick"] def:YES];
    persistentFlag = [TiUtils boolValue:[self valueForKey:@"persistent"] def:YES];
    forceOpaqueBackground = [TiUtils boolValue:[self valueForKey:@"opaquebackground"] def:NO];
    
    RELEASE_WITH_DELEGATE(actionSheet)
    if (customActionSheet) {
        customActionSheet.customView = nil;
        RELEASE_WITH_DELEGATE(customActionSheet)
    }
    
    if([self valueForKey:@"customView"]) {
        customActionSheet = [[TiCustomActionSheet alloc] init];
        [customActionSheet setDelegate:self];
        customActionSheet.dismissOnAction = hideOnClick;
        [customActionSheet setCustomView:[self valueForKey:@"customView"] fromProxy:self];
        [customActionSheet setHtmlTitle:[TiUtils stringValue:[self valueForKey:@"title"]]];
        
        NSMutableArray *buttonNames = [self valueForKey:@"buttonNames"];
        int cancelIndex = [TiUtils intValue:[self valueForKey:@"cancel"] def:-1];
        
        customActionSheet.tapOutDismiss = [TiUtils boolValue:[self valueForKey:@"tapOutDismiss"] def:cancelIndex != -1];
		if (buttonNames==nil || (id)buttonNames == [NSNull null])
		{
			buttonNames = [[[NSMutableArray alloc] initWithCapacity:2] autorelease];
			NSString *ok = [self valueForUndefinedKey:@"ok"];
			if (ok==nil)
			{
				ok = @"OK";
			}
			[buttonNames addObject:ok];
		}
        
        if (buttonNames!=nil && (id)buttonNames != [NSNull null])
		{
            if (cancelIndex != -1 && cancelIndex < [buttonNames count]) {
                NSString* cancelText = [buttonNames objectAtIndex:cancelIndex];
                [customActionSheet setCancelButton:[self createButtonWithTitle:cancelText target:self action:@selector(actionSheetCancelButtonClicked:)]];
                [buttonNames removeObjectAtIndex:cancelIndex];
            }
            NSString* doneText = [buttonNames lastObject];
            if (doneText) {
                [customActionSheet setDoneButton:[self createButtonWithTitle:doneText target:self action:@selector(actionSheetDoneButtonClicked:)]];
                [buttonNames removeObject:doneText];
            }
            for (id buttonName in buttonNames)
            {
                NSString * thisButtonName = [TiUtils stringValue:buttonName];
                [customActionSheet addCustomButtonWithTitle:thisButtonName value:buttonName];
            }
        }
        else {
            if ([self valueForUndefinedKey:@"ok"]) {
                [customActionSheet setDoneButton:[self createButtonWithTitle:[self valueForUndefinedKey:@"ok"] target:self action:@selector(actionSheetDoneButtonClicked:)]];
            }
            else {
                [customActionSheet setDoneButton:[self createButtonWithStyle:UIBarButtonItemStyleDone target:self action:@selector(actionSheetDoneButtonClicked:)]];
            }
            
            [customActionSheet setCancelButton:[self createButtonWithStyle:UIBarButtonSystemItemCancel target:self action:@selector(actionSheetDoneButtonClicked:)]];
        }
        if ([TiUtils isIOS7OrGreater]) {
            if ([self valueForKey:@"tintColor"]) {
                TiColor *ticolor = [TiUtils colorValue:[self valueForKey:@"tintColor"]];
                customActionSheet.tintColor = [ticolor _color];
            }
            else {
                UIView* topView = [[[TiApp app] controller] topWindowProxyView];
                customActionSheet.tintColor = topView.tintColor;
            }
        }
    }
    else {
        actionSheet = [[UIActionSheet alloc] init];
        [actionSheet setDelegate:self];
        
        [actionSheet setTitle:[TiUtils stringValue:[self valueForKey:@"title"]]];
        
        for (id thisOption in options)
        {
            NSString * thisButtonName = [TiUtils stringValue:thisOption];
            [actionSheet addButtonWithTitle:thisButtonName];
        }
        
        [actionSheet setCancelButtonIndex:[TiUtils intValue:[self valueForKey:@"cancel"] def:-1]];
        [actionSheet setDestructiveButtonIndex:[TiUtils intValue:[self valueForKey:@"destructive"] def:-1]];
        if ([TiUtils isIOS7OrGreater]) {
            if ([self valueForKey:@"tintColor"]) {
                TiColor *ticolor = [TiUtils colorValue:[self valueForKey:@"tintColor"]];
                actionSheet.tintColor = [ticolor _color];
            }
            else {
                UIView* topView = [[[TiApp app] controller] topWindowProxyView];
                actionSheet.tintColor = topView.tintColor;
            }
        }
        
    }
    [self retain];
	
    
	if ([TiUtils isIPad])
	{
		[self setDialogView:[args objectForKey:@"view"]];
		animated = [TiUtils boolValue:@"animated" properties:args def:YES];
		id obj = [args objectForKey:@"rect"];
		if (obj!=nil)
		{
			dialogRect = [TiUtils rectValue:obj];
		}
		else
		{
			dialogRect = CGRectZero;
		}
	}
    currentOrientation = [UIApplication sharedApplication].statusBarOrientation;
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(deviceRotationBegan:) name:UIDeviceOrientationDidChangeNotification object:nil];
    
    [self updateOptionDialogNow];
    
}

-(void)completeWithButton:(int)buttonIndex
{
    if (customActionSheet) {
        if (showDialog && (hideOnClick || ![customActionSheet isVisible])) {
            showDialog = NO;
            if (![customActionSheet isVisible]) {
                [self clearCustomActionSheet];
            }
        }
        if ([self _hasListeners:@"click"])
        {
            BOOL isCancel = (buttonIndex == 0);
            int index = isCancel?[TiUtils boolValue:[self valueForKey:@"cancel"] def:-1]:buttonIndex;
            NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:
                                   @(index),@"index",
                                   @(isCancel),@"cancel",
                                   nil];
            [self fireEvent:@"click" withObject:event];
        }
        
    }
    else {
        if (showDialog) {
            showDialog = NO;
            if ([self _hasListeners:@"click"])
            {
                NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:
                                       [NSNumber numberWithInt:buttonIndex],@"index",
                                       [NSNumber numberWithBool:(buttonIndex == [actionSheet cancelButtonIndex])],@"cancel",
                                       [NSNumber numberWithInt:[actionSheet destructiveButtonIndex]],@"destructive",
                                       nil];
                [self fireEvent:@"click" withObject:event];
            }
            [[NSNotificationCenter defaultCenter] removeObserver:self];
            [self forgetSelf];
            [self release];
        }
    }
}

-(void)hide:(id)args
{
	if((actionSheet == nil && customActionSheet == nil) || !showDialog){
		return;
	}

	id options = nil;
	if ([args count]>0) {
		options = [args objectAtIndex:0];
	}
	BOOL animatedhide = [TiUtils boolValue:@"animated" properties:options def:YES];

    TiThreadPerformOnMainThread(^{
        if ([actionSheet isVisible]) {
            [actionSheet dismissWithClickedButtonIndex:[actionSheet cancelButtonIndex] animated:animatedhide];
        }
        else if ([customActionSheet isVisible]) {
            [customActionSheet dismissAnimated:animatedhide];
        }
        else if(showDialog) {
            [self completeWithButton:customActionSheet?0:[actionSheet cancelButtonIndex]];
        }
    }, NO);
}

-(void)suspended:(NSNotification*)note
{
    if (!persistentFlag) {
        [self hide:[NSArray arrayWithObject: [NSDictionary dictionaryWithObject:NUMBOOL(NO) forKey:@"animated"]]];
    }
}

#pragma mark AlertView Delegate

- (void)willPresentActionSheet:(UIActionSheet *)actionSheet_
{
    //TIMOB-15939. Workaround rendering issue on iPAD on iOS7
    if (actionSheet_ == actionSheet && forceOpaqueBackground &&[TiUtils isIOS7OrGreater] && [TiUtils isIPad]) {
        NSArray* subviews = [actionSheet subviews];
        
        for (UIView* subview in subviews) {
            [subview setBackgroundColor:[UIColor whiteColor]];
        }
        [actionSheet setBackgroundColor:[UIColor whiteColor]];
    }
}

- (void)actionSheet:(UIActionSheet *)actionSheet_ didDismissWithButtonIndex:(NSInteger)buttonIndex;
{
	if (buttonIndex == -2)
	{
		return;
		//A -2 is used by us to indicate that this was programatically dismissed to properly
		//place the option dialog during a roation.
	}
	[self completeWithButton:buttonIndex];
}

-(void)deviceRotationBegan:(NSNotification *)notification
{
    [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(updateOptionDialogNow) object:nil];
    NSTimeInterval delay = [[UIApplication sharedApplication] statusBarOrientationAnimationDuration];
    UIDeviceOrientation nextOrientation =  [[UIDevice currentDevice] orientation];
    if (nextOrientation == UIDeviceOrientationFaceUp || nextOrientation == UIDeviceOrientationFaceDown ||
        nextOrientation == UIDeviceOrientationUnknown) return;
    if (currentOrientation == nextOrientation) return;
    currentOrientation = nextOrientation;
    if (UIInterfaceOrientationIsPortrait(currentOrientation) == UIInterfaceOrientationIsPortrait(nextOrientation)) {
        ++accumulatedOrientationChanges; // double for a 180 degree orientation change
    }
    if (++accumulatedOrientationChanges > 1) {
        delay *= MIN(accumulatedOrientationChanges, 4);
    }
	[(id)(customActionSheet?customActionSheet:actionSheet) dismissWithClickedButtonIndex:-2 animated:animated];
	[self performSelector:@selector(updateOptionDialogNow) withObject:nil afterDelay:delay];
}

-(void)updateOptionDialogNow;
{
	if (!showDialog) {
		return;
	}
    accumulatedOrientationChanges = 0;
	UIView *view = nil;
	if (dialogView==nil)
	{
		view = [[[TiApp app] controller] topWindowProxyView];
	}
	else 
	{
		//TODO: need to deal with button in a Toolbar which will have a nil view
		
		if ([dialogView supportsNavBarPositioning] && [dialogView isUsingBarButtonItem])
		{
			UIBarButtonItem *button = [dialogView barButtonItem];
			[(id)(customActionSheet?customActionSheet:actionSheet) showFromBarButtonItem:button animated:animated];
			return;
		}
		
		if ([dialogView conformsToProtocol:@protocol(TiToolbar)])
		{
			UIToolbar *toolbar = [(id<TiToolbar>)dialogView toolbar];
			[(id)(customActionSheet?customActionSheet:actionSheet) showFromToolbar:toolbar];
			return;
		}
		
		if ([dialogView conformsToProtocol:@protocol(TiTab)])
		{
			id<TiTab> tab = (id<TiTab>)dialogView;
			UITabBar *tabbar = [[tab tabGroup] tabbar];
			[(id)(customActionSheet?customActionSheet:actionSheet) showFromTabBar:tabbar];
			return;
		}
		
		view = [dialogView view];
		CGRect rect;
		if (CGRectIsEmpty(dialogRect))
		{
			if(view == nil)
			{
				rect = CGRectZero;
			}
			else
			{
				rect = [view bounds];
			}

		}
		else
		{
			rect = dialogRect;
		}

		[(id)(customActionSheet?customActionSheet:actionSheet) showFromRect:rect inView:view animated:animated];
		return;
	}
    [(id)(customActionSheet?customActionSheet:actionSheet) showInView:view];
}

#pragma mark TiCustomActionSheetDelegate

- (void)customActionSheetCancel:(TiCustomActionSheet *)actionSheet {
    if (!persistentFlag && hideOnClick) {
        [self hide:@{@"animated":@(NO)}];
    }
}


- (void)customActionSheet:(CustomActionSheet *)actionSheet didDismissWithButtonIndex:(NSInteger)buttonIndex {
    if (buttonIndex == -2)
	{
		return;
		//A -2 is used by us to indicate that this was programatically dismissed to properly
		//place the option dialog during a roation.
	}
    [self clearCustomActionSheet];
}
- (void)customActionSheet:(CustomActionSheet *)actionSheet clickedButtonAtIndex:(NSInteger)buttonIndex {
    [self completeWithButton:buttonIndex];
}

@end

#endif
