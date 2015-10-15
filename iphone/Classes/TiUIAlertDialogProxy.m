/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiUIAlertDialogProxy.h"
#import "TiUtils.h"
#import "TiApp.h"

static NSCondition* alertCondition;
static BOOL alertShowing = NO;

@interface TiAlertView : UIAlertView {
}
@property(nonatomic, readwrite) BOOL hideOnClick;
@end

@interface TiAlertAction : UIAlertAction {
}
@property(nonatomic, assign) NSUInteger index;
@end

@interface TiAlertController : UIAlertController
@property(nonatomic, assign) TiUIAlertDialogProxy* proxy;
@end

@interface TiUIAlertDialogProxy()
-(void) cleanup;
@end


@implementation TiAlertView
@synthesize hideOnClick;

-(void)dismissWithClickedButtonIndex:(NSInteger)buttonIndex animated:(BOOL)animated {
    if (!hideOnClick)
        return;
    [super dismissWithClickedButtonIndex:buttonIndex animated:animated];
}

@end

@implementation TiAlertAction
@synthesize index;
@end

@implementation TiAlertController

- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];
    [_proxy cleanup];
}

@end


@implementation TiUIAlertDialogProxy

-(void)_configure
{
    [self setValue:NUMBOOL(YES) forKey:@"hideOnClick"];
    [super _configure];
}

-(void)_destroy
{
    if (alert != nil) {
        [alertCondition lock];
        alertShowing = NO;
        persistentFlag = NO;
        [alertCondition broadcast];
        [alertCondition unlock];
    }
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    RELEASE_TO_NIL(alert);
    if (alertController) {
        alertController.proxy = nil;
        RELEASE_TO_NIL(alertController);
    }
    [super _destroy];
}

-(NSMutableDictionary*)langConversionTable
{
    return [NSMutableDictionary dictionaryWithObjectsAndKeys:
            @"title",@"titleid",
            @"ok",@"okid",
            @"message",@"messageid",
            nil];
}

-(NSString*)apiName
{
    return @"Ti.UI.AlertDialog";
}

-(void) cleanup
{
    if(alert != nil || alertController != nil)
    {
        [alertCondition lock];
        alertShowing = NO;
        persistentFlag = NO;
        hideOnClick = YES;
        [alertCondition broadcast];
        [alertCondition unlock];
        [self forgetSelf];
        [self autorelease];
        RELEASE_TO_NIL(alert);
        if (alertController) {
            alertController.proxy = nil;
            RELEASE_TO_NIL(alertController);
        }
        [[[TiApp app] controller] decrementActiveAlertControllerCount];
        [[NSNotificationCenter defaultCenter] removeObserver:self];
    }
}

-(void)hide:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args,NSDictionary);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = [TiUtils boolValue:@"animated" properties:args def:YES];
    if (alert!=nil) {
        [self fireEvent:@"close" withObject:nil];
        //On IOS5 sometimes the delegate does not get called when hide is called soon after show
        //So we do the cleanup here itself
        
        //Remove ourselves as the delegate. This ensures didDismissWithButtonIndex is not called on dismissWithClickedButtonIndex
        [alert setDelegate:nil];
        [alert dismissWithClickedButtonIndex:[alert cancelButtonIndex] animated:animated];
        [self cleanup];
    } else if (alertController != nil){
        [self fireEvent:@"close" withObject:nil];
        [alertController dismissViewControllerAnimated:animated completion:^{
            [self cleanup];
        }];
    }
}

-(void)show:(id)args
{
    if (alertCondition==nil) {
        alertCondition = [[NSCondition alloc] init];
    }
    
    [self rememberSelf];
    // prevent more than one JS thread from showing an alert box at a time
    if ([NSThread isMainThread]==NO) {
        
        [alertCondition lock];
        if (alertShowing) {
            [alertCondition wait];
        }
        alertShowing = YES;
        [alertCondition broadcast];
        [alertCondition unlock];
        // alert show should block the JS thread like the browser
        TiThreadPerformOnMainThread(^{[self show:args];}, YES);
    }
    else {
        hideOnClick = [TiUtils boolValue:[self valueForKey:@"hideOnClick"] def:YES];
        persistentFlag = [TiUtils boolValue:[self valueForKey:@"persistent"] def:NO];
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(suspended:) name:kTiSuspendNotification object:nil];
        NSMutableArray *buttonNames = [self valueForKey:@"buttonNames"];
        if (buttonNames==nil || (id)buttonNames == [NSNull null]) {
            buttonNames = [[[NSMutableArray alloc] initWithCapacity:2] autorelease];
            NSString *ok = [self valueForUndefinedKey:@"ok"];
            if (ok==nil) {
                ok = @"OK";
            }
            [buttonNames addObject:ok];
        }
        
        cancelIndex = [TiUtils intValue:[self valueForKey:@"cancel"] def:-1];
        destructiveIndex = [TiUtils intValue:[self valueForKey:@"destructive"] def:-1];
        
        if (cancelIndex >= [buttonNames count]) {
            cancelIndex = -1;
        }
        
        if (destructiveIndex >= [buttonNames count]) {
            destructiveIndex = -1;
        }
        
        style = [TiUtils intValue:[self valueForKey:@"style"] def:UIAlertViewStyleDefault];
        
        [[[TiApp app] controller] incrementActiveAlertControllerCount];
        if ([TiUtils isIOS8OrGreater]) {
            RELEASE_TO_NIL(alertController);
            
            alertController = [[TiAlertController alertControllerWithTitle:[TiUtils stringValue:[self valueForKey:@"title"]]
                                                                   message:[TiUtils stringValue:[self valueForKey:@"message"]]
                                                            preferredStyle:UIAlertControllerStyleAlert] retain];
            alertController.proxy = self;
            //        ((TiAlertView*)alert).hideOnClick = hideOnClick;
            int curIndex = 0;
            
            //Configure the Buttons
            for (id btn in buttonNames) {
                NSString* btnName = [TiUtils stringValue:btn];
                if (!IS_NULL_OR_NIL(btnName)) {
                    
                    UIAlertActionStyle alertActionStyle;
                    
                    if (curIndex == cancelIndex) {
                        alertActionStyle = UIAlertActionStyleCancel;
                    } else if (curIndex == destructiveIndex) {
                        alertActionStyle = UIAlertActionStyleDestructive;
                    } else {
                        alertActionStyle  = UIAlertActionStyleDefault;
                    }

                    TiAlertAction* theAction = [TiAlertAction actionWithTitle:btnName
                                                                        style:alertActionStyle
                                                                      handler:^(UIAlertAction * action){
                                                                          [self fireClickEventWithAction:action];
                                                                      }];
                    theAction.index = curIndex;
                    [alertController addAction:theAction];
                }
                curIndex++;
            }
            
            //Configure the TextFields
            if ( (style == UIAlertViewStylePlainTextInput) || (style == UIAlertViewStyleSecureTextInput) ) {
                [alertController addTextFieldWithConfigurationHandler:^(UITextField *textField) {
                    textField.secureTextEntry = (style == UIAlertViewStyleSecureTextInput);
                    textField.placeholder = [TiUtils stringValue:[self valueForKey:@"placeholder"]] ?: @"";
                    textField.keyboardType = [TiUtils intValue:[self valueForKey:@"keyboardType"] def:UIReturnKeyDefault];
                    textField.returnKeyType = [TiUtils intValue:[self valueForKey:@"returnKeyType"] def:UIKeyboardTypeDefault];
                }];
            } else if ((style == UIAlertViewStyleLoginAndPasswordInput)) {
                [alertController addTextFieldWithConfigurationHandler:^(UITextField *textField) {
                    textField.placeholder = [TiUtils stringValue:[self valueForKey:@"loginPlaceholder"]] ?: @"Login";
                    textField.secureTextEntry = NO;
                }];
                [alertController addTextFieldWithConfigurationHandler:^(UITextField *textField) {
                    textField.placeholder = [TiUtils stringValue:[self valueForKey:@"passwordPlaceholder"]] ?: @"Password";
                    textField.secureTextEntry = YES;
                }];
            }
            
            [self retain];
            [[TiApp app] showModalController:alertController animated:YES];
            
        } else {
            RELEASE_TO_NIL(alert);
            alert = [[TiAlertView alloc] initWithTitle:[TiUtils stringValue:[self valueForKey:@"title"]]
                                               message:[TiUtils stringValue:[self valueForKey:@"message"]]
                                              delegate:self cancelButtonTitle:nil otherButtonTitles:nil];
            ((TiAlertView*)alert).hideOnClick = hideOnClick;
            for (id btn in buttonNames)
            {
                NSString * thisButtonName = [TiUtils stringValue:btn];
                [alert addButtonWithTitle:thisButtonName];
            }
            
            [alert setCancelButtonIndex:cancelIndex];
            
            
            [alert setAlertViewStyle:style];
            
            [self retain];
            [[[TiApp app] controller] incrementActiveAlertControllerCount];
            [alert show];
            [self fireEvent:@"open" withObject:nil];
        }
    }
}

-(void)suspended:(NSNotification*)note
{
    if (!persistentFlag) {
        [self hide:[NSDictionary dictionaryWithObject:NUMBOOL(NO) forKey:@"animated"]];
    }
}

-(void) fireClickEventWithAction:(UIAlertAction*)theAction
{
    if ([self _hasListeners:@"click"]) {
        NSUInteger indexOfAction = [(TiAlertAction*)theAction index];
        
        NSMutableDictionary *event = [NSMutableDictionary dictionaryWithObjectsAndKeys:
                                      NUMINTEGER(indexOfAction),@"index",
                                      [NSNumber numberWithBool:(indexOfAction == cancelIndex)],@"cancel",
                                      [NSNumber numberWithBool:(indexOfAction == destructiveIndex)],@"destructive",
                                      nil];
        
        
        if (style == UIAlertViewStylePlainTextInput || style == UIAlertViewStyleSecureTextInput) {
            NSString* theText = [[[alertController textFields] objectAtIndex:0] text];
            [event setObject:(IS_NULL_OR_NIL(theText) ? @"" : theText) forKey:@"text"];
        }
        else if (style == UIAlertViewStyleLoginAndPasswordInput) {
            NSArray* textFields = [alertController textFields];
            for (UITextField* theField in textFields) {
                NSString* theText = [theField text];
                [event setObject:(IS_NULL_OR_NIL(theText) ? @"" : theText) forKey:([theField isSecureTextEntry] ? @"password" : @"login")];
            }
        }
        [self fireEvent:@"click" withObject:event checkForListener:NO];
    }
    [self cleanup];
}
#pragma mark AlertView Delegate

- (void)alertView:(UIAlertView *)alertView didDismissWithButtonIndex:(NSInteger)buttonIndex
{
    [self cleanup];
}

- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex
{
    if ([self _hasListeners:@"click"]) {
        NSMutableDictionary *event = [NSMutableDictionary dictionaryWithObjectsAndKeys:
                                      NUMINTEGER(buttonIndex),@"index",
                                      [NSNumber numberWithBool:(buttonIndex == [alertView cancelButtonIndex])],@"cancel",
                                      nil];
        
        if ([alertView alertViewStyle] == UIAlertViewStylePlainTextInput || [alertView alertViewStyle] == UIAlertViewStyleSecureTextInput) {
            NSString* theText = [[alertView textFieldAtIndex:0] text];
            [event setObject:(IS_NULL_OR_NIL(theText) ? @"" : theText) forKey:@"text"];
        }
        else if ([alertView alertViewStyle] == UIAlertViewStyleLoginAndPasswordInput) {
            NSString* theText = [[alertView textFieldAtIndex:0] text];
            [event setObject:(IS_NULL_OR_NIL(theText) ? @"" : theText) forKey:@"login"];
            
            // If password field never gets focus, `text` property becomes `nil`.
            NSString *password = [[alertView textFieldAtIndex:1] text];
            [event setObject:(IS_NULL_OR_NIL(password) ? @"" : password) forKey:@"password"];
        }
        
        [self fireEvent:@"click" withObject:event checkForListener:NO];
    }
}


-(void)alertViewCancel:(UIAlertView *)alertView
{
    if (!persistentFlag && hideOnClick) {
        [self hide:[NSDictionary dictionaryWithObject:NUMBOOL(NO) forKey:@"animated"]];
    }
}

@end