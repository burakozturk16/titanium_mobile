

#ifdef USE_TI_UITABLEVIEW
#import "TableView_Categories.h"

@implementation TiViewProxy (TiUITableViewProxy)
static NSArray* layoutKeys = nil;
static NSSet* transferableProps = nil;
+(NSArray*)layoutKeys
{
    if (layoutKeys == nil) {
        layoutKeys = [[NSArray alloc] initWithObjects:@"left", @"right", @"top", @"bottom", @"width", @"height", @"fullscreen", @"sizeRatio", @"weight", @"minWidth", @"minHeight", @"maxWidth", @"maxHeight", nil];
    }
    return layoutKeys;
}

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [[NSSet alloc] initWithObjects:@"imageCap",@"visible", @"backgroundImage", @"backgroundGradient", @"backgroundColor", @"backgroundSelectedImage", @"backgroundSelectedGradient", @"backgroundSelectedColor", @"backgroundDisabledImage", @"backgroundDisabledGradient", @"backgroundDisabledColor", @"backgroundRepeat",@"focusable", @"touchEnabled", @"viewShadow", @"viewMask", @"accessibilityLabel", @"accessibilityValue", @"accessibilityHint", @"accessibilityHidden",
                             @"opacity", @"borderWidth", @"borderColor", @"borderRadius", @"tileBackground",
                             @"transform", @"center", @"anchorPoint", @"clipChildren", @"touchPassThrough", @"transform", nil];
    }
    return transferableProps;
}
@end

@implementation TiUIView(TiUITableView)


-(SEL)selectorForlayoutProperty:(NSString*)key
{
    NSString *method = [NSString stringWithFormat:@"set%@%@:", [[key substringToIndex:1] uppercaseString], [key substringFromIndex:1]];
    return NSSelectorFromString(method);
}

//Todo: Generalize.
-(void)setKrollValue:(id)value forKey:(NSString *)key withObject:(id)props
{
    if(value == [NSNull null])
    {
        value = nil;
    }
    
    NSString *method = SetterStringForKrollProperty(key);
    
    SEL methodSel = NSSelectorFromString([method stringByAppendingString:@"withObject:"]);
    if([self respondsToSelector:methodSel])
    {
        [self performSelector:methodSel withObject:value withObject:props];
        return;
    }
    
    methodSel = NSSelectorFromString(method);
    if([self respondsToSelector:methodSel])
    {
        [self performSelector:methodSel withObject:value];
    }
}

-(void)transferProxy:(TiViewProxy*)newProxy
{
    [self transferProxy:newProxy deep:NO];
}

-(void)transferProxy:(TiViewProxy*)newProxy deep:(BOOL)deep
{
    [self transferProxy:newProxy withBlockBefore:nil withBlockAfter:nil deep:deep];
}

-(void)transferProxy:(TiViewProxy*)newProxy withBlockBefore:(void (^)(TiViewProxy* proxy))blockBefore
      withBlockAfter:(void (^)(TiViewProxy* proxy))blockAfter deep:(BOOL)deep
{
    TiViewProxy * oldProxy = (TiViewProxy *)[self proxy];
    
    // We can safely skip everything if we're transferring to ourself.
    if (oldProxy != newProxy) {
        
        if(blockBefore)
        {
            blockBefore(newProxy);
        }
        
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        [transferLock lock];
        
        if (deep) {
            NSArray *subProxies = [newProxy children];
            [[oldProxy children] enumerateObjectsUsingBlock:^(TiViewProxy *oldSubProxy, NSUInteger idx, BOOL *stop) {
                TiViewProxy *newSubProxy = idx < [subProxies count] ? [subProxies objectAtIndex:idx] : nil;
                [[oldSubProxy view] transferProxy:newSubProxy withBlockBefore:blockBefore withBlockAfter:blockAfter deep:deep];
            }];
        }
        
        NSSet* transferableProperties = [[oldProxy class] transferableProperties];
        NSMutableSet* oldProperties = [NSMutableSet setWithArray:(NSArray *)[oldProxy allKeys]];
        NSMutableSet* newProperties = [NSMutableSet setWithArray:(NSArray *)[newProxy allKeys]];
        NSMutableSet* keySequence = [NSMutableSet setWithArray:[newProxy keySequence]];
        NSMutableSet* layoutProps = [NSMutableSet setWithArray:[TiViewProxy layoutKeys]];
        [oldProperties minusSet:newProperties];
        [oldProperties minusSet:layoutProps];
        [newProperties minusSet:keySequence];
        [layoutProps intersectSet:newProperties];
        [newProperties intersectSet:transferableProperties];
        [oldProperties intersectSet:transferableProperties];
        
        id<NSFastEnumeration> keySeq = keySequence;
        id<NSFastEnumeration> oldProps = oldProperties;
        id<NSFastEnumeration> newProps = newProperties;
        id<NSFastEnumeration> fastLayoutProps = layoutProps;
        
        [oldProxy retain];
        
        [self configurationStart];
        [newProxy setReproxying:YES];
        
        [oldProxy setView:nil];
        [newProxy setView:self];
        
        [self setProxy:newProxy];
        
        //The important sequence first:
        for (NSString * thisKey in keySeq)
        {
            id newValue = [newProxy valueForKey:thisKey];
            id oldValue = [oldProxy valueForKey:thisKey];
            if ((oldValue != newValue) && ![oldValue isEqual:newValue]) {
                [self setKrollValue:newValue forKey:thisKey withObject:nil];
            }
        }
        
        for (NSString * thisKey in fastLayoutProps)
        {
            id newValue = [newProxy valueForKey:thisKey];
            id oldValue = [oldProxy valueForKey:thisKey];
            if ((oldValue != newValue) && ![oldValue isEqual:newValue]) {
                SEL selector = [self selectorForlayoutProperty:thisKey];
                if([[self proxy] respondsToSelector:selector])
                {
                    [[self proxy] performSelector:selector withObject:newValue];
                }
            }
        }
        
        for (NSString * thisKey in oldProps)
        {
            [self setKrollValue:nil forKey:thisKey withObject:nil];
        }
        
        for (NSString * thisKey in newProps)
        {
            id newValue = [newProxy valueForKey:thisKey];
            id oldValue = [oldProxy valueForKey:thisKey];
            if ((oldValue != newValue) && ![oldValue isEqual:newValue]) {
                [self setKrollValue:newValue forKey:thisKey withObject:nil];
            }
        }
        
        [pool release];
        pool = nil;
        
        [self configurationSet];
        
        [oldProxy release];
        
        [newProxy setReproxying:NO];
        
        
        if(blockAfter)
        {
            blockAfter(newProxy);
        }
        
        [transferLock unlock];
        
    }
    
}

-(BOOL)validateTransferToProxy:(TiViewProxy*)newProxy deep:(BOOL)deep
{
    TiViewProxy * oldProxy = (TiViewProxy *)[self proxy];
    
    if (oldProxy == newProxy) {
        return YES;
    }
    if (![newProxy isMemberOfClass:[oldProxy class]]) {
        DebugLog(@"[ERROR] Cannot reproxy not same proxy class");
        return NO;
    }
    
    UIView * ourView = [(TiViewProxy*)[oldProxy parent] parentViewForChild:oldProxy];
    UIView *parentView = [self superview];
    if (parentView!=ourView)
    {
        DebugLog(@"[ERROR] Cannot reproxy not same parent view");
        return NO;
    }
    
    __block BOOL result = YES;
    if (deep) {
        NSArray *subProxies = [newProxy children];
        NSArray *oldSubProxies = [oldProxy children];
        if ([subProxies count] != [oldSubProxies count]) {
            DebugLog(@"[ERROR] Cannot reproxy not same number of subproxies");
            return NO;
        }
        [oldSubProxies enumerateObjectsUsingBlock:^(TiViewProxy *oldSubProxy, NSUInteger idx, BOOL *stop) {
            TiViewProxy *newSubProxy = [subProxies objectAtIndex:idx];
            TiUIView* view = [oldSubProxy view];
            if (!view){
                DebugLog(@"[ERROR] Cannot reproxy no subproxy view");
                result = NO;
                *stop = YES;
            }
            else
                result = [view validateTransferToProxy:newSubProxy deep:YES]; //we assume that the view is already created
            if (!result) {
                *stop = YES;
            }
        }];
    }
    return result;
}
@end

#ifdef USE_TI_UIACTIVITYINDICATOR
#import "TiUIActivityIndicatorProxy.h"

@implementation TiUIActivityIndicatorProxy(TiUITableViewProxy)

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [[TiViewProxy transferableProperties] setByAddingObjectsFromSet:[NSSet setWithObjects:@"style",
                                              @"font", @"color", @"message", nil]];
    }
    return transferableProps;
}
@end
#endif

#ifdef USE_TI_UIBUTTON
#import "TiUIButtonProxy.h"

@implementation TiUIButtonProxy(TiUITableViewProxy)

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [[TiViewProxy transferableProperties] setByAddingObjectsFromSet:[NSSet setWithObjects:@"style", @"title",
             @"color", @"highlightedColor", @"selectedColor",
             @"enabled", @"selected", @"style",
             @"image", @"backgroundHighlightedImage",
             @"backgroundDisabledImage", @"backgroundSelectedImage",
             @"backgroundFocusedImage", @"verticalAlign",
             @"textAlign", @"font", @"backgroundPaddingLeft",
             @"backgroundPaddingRight",
             @"backgroundPaddingBottom", @"backgroundPaddingTop",
             @"shadowOffset", @"shadowRadius", @"shadowColor",
             @"padding",
             @"wordWrap", @"borderWidth", nil]];
    }
    return transferableProps;
}
@end
#endif

#import "TiUITableViewProxy.h"
#import "TiUIHorizontalTableViewProxy.h"

@implementation TiUITableViewProxy(TiUITableViewProxy)

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [[TiViewProxy transferableProperties] setByAddingObjectsFromSet:[NSSet setWithObjects:@"scrollsToTop",
                                              @"selectedBackgroundGradient",@"data",@"allowSelection",
                                              @"allowSelectionDuringEditing",@"separatorStyle",
                                              @"search", @"scrollIndicatorStyle", @"showVerticalScrollIndicator",
                                              @"searchHidden", @"hideSearchOnSelection", @"filterAttribute",
                                              @"index", @"filterCaseInsensitive", @"editable",
                                              @"moveable", @"scrollable", @"editing",
                                              @"moving", @"rowHeight", @"minRowHeight", @"maxRowHeight",
                                              @"headerPullView", @"contentInsets",
                                              @"separatorColor", @"headerTitle", @"headerView",
                                              @"footerTitle", @"footerView", nil]];
    }
    return transferableProps;
}
@end

@implementation TiUIHorizontalTableViewProxy(TiUITableViewProxy)

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [TiUITableViewProxy transferableProperties];
    }
    return transferableProps;
}
@end

#ifdef USE_TI_UIIMAGEVIEW
#import "TiUIImageViewProxy.h"

@implementation TiUIImageViewProxy(TiUITableViewProxy)

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [[TiViewProxy transferableProperties] setByAddingObjectsFromSet:[NSSet setWithObjects:@"image",
                                              @"scaleType",@"localLoadSync",@"images",
                                              @"duration", @"repeatCount", @"reverse",@"animatedImages", nil]];
    }
    return transferableProps;
}
@end
#endif

#ifdef USE_TI_UILABEL
#import "TiUILabelProxy.h"

@implementation TiUILabelProxy(TiUITableViewProxy)

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [[TiViewProxy transferableProperties] setByAddingObjectsFromSet:[NSSet setWithObjects:@"text",@"html",
                                              @"color", @"highlightedColor", @"autoLink",
                                              @"verticalAlign", @"textAlign", @"font",
                                              @"minimumFontSize", @"backgroundPaddingLeft",
                                              @"backgroundPaddingRight", @"backgroundPaddingBottom", @"backgroundPaddingTop", @"shadowOffset",
                                              @"shadowRadius", @"shadowColor",
                                              @"padding",
                                              @"wordWrap", @"borderWidth", @"maxLines",
                                              @"ellipsize", @"multiLineEllipsize", nil]];
    }
    return transferableProps;
}
@end
#endif

#ifdef USE_TI_UIMASKEDIMAGE
#import "TiUIMaskedImageProxy.h"

@implementation TiUIMaskedImageProxy(TiUITableViewProxy)

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [[TiViewProxy transferableProperties] setByAddingObjectsFromSet:[NSSet setWithObjects:@"image",
                                              @"mask",@"tint",@"mode", nil]];
    }
    return transferableProps;
}
@end
#endif

#ifdef USE_TI_UIPROGRESSBAR
#import "TiUIProgressBarProxy.h"

@implementation TiUIProgressBarProxy(TiUITableViewProxy)

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [[TiViewProxy transferableProperties] setByAddingObjectsFromSet:[NSSet setWithObjects:@"min",
                                              @"max", @"value", @"font",
                                              @"color", @"message", nil]];
    }
    return transferableProps;
}
@end
#endif

#ifdef USE_TI_UISCROLLABLEVIEW
#import "TiUIScrollableViewProxy.h"

@implementation TiUIScrollableViewProxy(TiUITableViewProxy)

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [[TiViewProxy transferableProperties] setByAddingObjectsFromSet:[NSSet setWithObjects:@"currentPage",
                                              @"pagingControlColor",@"pagingControlHeight",@"showPagingControl",
                                              @"pagingControlAlpha",@"overlayEnabled",
                                              @"pagingControlOnTop", @"cacheSize", @"views",
                                              @"pageControlHeight", @"scrollingEnabled", @"disableBounce", nil]];
    }
    return transferableProps;
}
@end
#endif

#ifdef USE_TI_UISCROLLVIEW
#import "TiUIScrollViewProxy.h"

@implementation TiUIScrollViewProxy(TiUITableViewProxy)

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [[TiViewProxy transferableProperties] setByAddingObjectsFromSet:[NSSet setWithObjects:@"contentOffset",
                                              @"minZoomScale",@"maxZoomScale",@"zoomScale",
                                              @"canCancelEvents",@"contentWidth",@"contentHeight",
                                              @"showHorizontalScrollIndicator",@"showVerticalScrollIndicator",
                                              @"scrollIndicatorStyle", @"scrollsToTop", @"horizontalBounce",
                                              @"verticalBounce", @"scrollingEnabled", @"disableBounce", nil]];
    }
    return transferableProps;
}
@end
#endif

#ifdef USE_TI_UISLIDER
#import "TiUISliderProxy.h"

@implementation TiUISliderProxy(TiUITableViewProxy)

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [[TiViewProxy transferableProperties] setByAddingObjectsFromSet:[NSSet setWithObjects:@"min",
                                              @"max",@"value",@"enabled",@"leftTrackLeftCap",
                                              @"leftTrackTopCap",@"rightTrackLeftCap",
                                              @"rightTrackTopCap", @"thumbImage",
                                              @"selectedThumbImage",@"highlightedThumbImage",
                                              @"disabledThumbImage", @"leftTrackImage",
                                              @"selectedLeftTrackImage",@"highlightedLeftTrackImage",
                                              @"disabledLeftTrackImage", @"rightTrackImage",
                                              @"selectedRightTrackImage",@"highlightedRightTrackImage",
                                              @"disabledRightTrackImage", nil]];
    }
    return transferableProps;
}
@end
#endif

#ifdef USE_TI_UISWITCH
#import "TiUISwitchProxy.h"

@implementation TiUISwitchProxy(TiUITableViewProxy)

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [[TiViewProxy transferableProperties] setByAddingObjectsFromSet:[NSSet setWithObjects:@"enabled",
                                              @"value", nil]];
    }
    return transferableProps;
}
@end
#endif

#if defined(USE_TI_UITEXTWIDGET) || defined(USE_TI_UITEXTAREA) || defined(USE_TI_UITEXTFIELD)
#import "TiUITextWidgetProxy.h"

@implementation TiUITextWidgetProxy(TiUITableViewProxy)

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [[TiViewProxy transferableProperties] setByAddingObjectsFromSet:[NSSet setWithObjects:@"color",
                                              @"font",@"textAlign",@"value",@"returnKeyType",
                                              @"enableReturnKey",@"keyboardType",
                                              @"autocorrect", @"passwordMask",
                                              @"appearance",@"autocapitalization", nil]];
    }
    return transferableProps;
}
@end
#endif

#ifdef USE_TI_UITEXTAREA
#import "TiUITextAreaProxy.h"

@implementation TiUITextAreaProxy(TiUITableViewProxy)

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [[TiUITextWidgetProxy transferableProperties] setByAddingObjectsFromSet:[NSSet setWithObjects:@"enabled",
                                              @"scrollable",@"editable",@"autoLink",
                                              @"borderStyle", nil]];
    }
    return transferableProps;
    
}
@end
#endif

#ifdef USE_TI_UITEXTFIELD
#import "TiUITextFieldProxy.h"

@implementation TiUITextFieldProxy(TiUITableViewProxy)

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [[TiUITextWidgetProxy transferableProperties] setByAddingObjectsFromSet:[NSSet setWithObjects:@"paddingLeft",
                                              @"paddingRight",@"leftButtonPadding",@"rightButtonPadding",
                                              @"editable", @"enabled", @"hintText", @"minimumFontSize",
                                              @"clearOnEdit", @"borderStyle", @"clearButtonMode",
                                              @"leftButton", @"leftButtonMode", @"verticalAlign",
                                              @"rightButton", @"rightButtonMode",
                                              @"backgroundDisabledImage", nil]];
    }
    return transferableProps;
}
@end
#endif

#ifdef USE_TI_UIVOLUMEVIEW
#import "TiUIVolumeViewProxy.h"

@implementation TiUIVolumeViewProxy(TiUITableViewProxy)

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [[TiViewProxy transferableProperties] setByAddingObjectsFromSet:[NSSet setWithObjects:@"leftTrackLeftCap",
                                              @"leftTrackTopCap",@"rightTrackLeftCap",
                                              @"rightTrackTopCap", @"thumbImage",
                                              @"selectedThumbImage",@"highlightedThumbImage",
                                              @"disabledThumbImage", @"leftTrackImage",
                                              @"selectedLeftTrackImage",@"highlightedLeftTrackImage",
                                              @"disabledLeftTrackImage", @"rightTrackImage",
                                              @"selectedRightTrackImage",@"highlightedRightTrackImage",
                                              @"disabledRightTrackImage", nil]];
    }
    return transferableProps;
}
@end
#endif

#ifdef USE_TI_UIWEBVIEW
#import "TiUIWebViewProxy.h"

@implementation TiUIWebViewProxy(TiUITableViewProxy)

+(NSSet*)transferableProperties
{
    if (transferableProps == nil) {
        transferableProps = [[TiViewProxy transferableProperties] setByAddingObjectsFromSet:[NSSet setWithObjects:@"autoDetect",
                                              @"html",@"data",@"scalesPageToFit",
                                              @"url",@"scrollsToTop",@"disableBounce", nil]];
    }
    return transferableProps;
}
@end
#endif

#endif
