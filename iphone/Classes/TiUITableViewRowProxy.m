/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UITABLEVIEW

#import "TiUITableViewRowProxy.h"
#import "TiUITableViewAction.h"
#import "TiUITableViewSectionProxy.h"
#import "TiUITableView.h"
#import "TiViewProxy.h"
#import "TiUtils.h"
#import "Webcolor.h"
#import "ImageLoader.h"
#import <objc/runtime.h>
#import "TiSelectedCellbackgroundView.h"
#import "TiLayoutQueue.h"
#import <libkern/OSAtomic.h>
#import "TiLayoutQueue.h"

static NSInteger const kRowContainerTag = 100;
NSString * const defaultRowTableClass = @"_default_";
#define CHILD_ACCESSORY_WIDTH 20.0
#define CHECK_ACCESSORY_WIDTH 20.0
#define DETAIL_ACCESSORY_WIDTH 33.0
#define IOS7_ACCESSORY_EXTRA_OFFSET 15.0
// TODO: Clean this up a bit
#define NEEDS_UPDATE_ROW 1

@interface TiUITableViewRowContainer : TiUIView
{
	TiProxy * hitTarget;
	CGPoint hitPoint;
    int index;
}
@property(nonatomic,retain,readwrite) TiProxy * hitTarget;
@property(nonatomic,assign,readwrite) CGPoint hitPoint;
@property(nonatomic,assign,readwrite) int index;
-(void)clearHitTarget;

@end

TiProxy * DeepScanForProxyOfViewContainingPoint(UIView * targetView, CGPoint point)
{
	if (!CGRectContainsPoint([targetView bounds], point))
	{
		return nil;
	}
	for (UIView * subView in [targetView subviews])
	{
		TiProxy * subProxy = DeepScanForProxyOfViewContainingPoint(subView,[targetView convertPoint:point toView:subView]);
		if (subProxy != nil)
		{
			return subProxy;
		}
	}

	//By now, no subviews have claimed ownership.
	if ([targetView respondsToSelector:@selector(proxy)])
	{
		return [(TiUIView *)targetView proxy];
	}
	return nil;
}

@implementation TiUITableViewRowContainer
@synthesize hitTarget, hitPoint, index;

-(id)init
{
	if (self = [super init]) {
		hitPoint = CGPointZero;
        index= 0;
	}
	return self;
}

-(void)clearHitTarget
{
	[hitTarget autorelease];
	hitTarget = nil;
}

-(NSString*)apiName
{
    return @"Ti.UI.TableViewRow";
}

-(TiProxy *)hitTarget
{
	TiProxy * result = hitTarget;
	[self clearHitTarget];
	return result;
}

- (UIView *)hitTest:(CGPoint) point withEvent:(UIEvent *)event 
{
    UIView * result = [super hitTest:point withEvent:event];
	[self setHitPoint:point];
	
	if (result==nil)
	{
		[self setHitTarget:DeepScanForProxyOfViewContainingPoint(self,point)];
		return nil;
	}

	if ([result respondsToSelector:@selector(proxy)])
	{
		[self setHitTarget:[(TiUIView *)result proxy]];
	}
	else
	{
		[self clearHitTarget];
	}

	return result;
}

- (void) dealloc
{
	[self clearHitTarget];
	[super dealloc];
}


-(void)setBackgroundColor_:(id)color
{
    if ([self proxy])
        [(TiUITableViewRowProxy*)[self proxy] configureBackgroundColor];
}


-(void)propertyChanged:(NSString*)key oldValue:(id)oldValue newValue:(id)newValue proxy:(TiProxy*)proxy
{
    [super propertyChanged:key oldValue:oldValue newValue:newValue proxy:proxy];
    if ([self proxy])
    {
        [(TiUITableViewRowProxy*)[self proxy] propertyChanged:key oldValue:oldValue newValue:newValue proxy:proxy];
    }
}


@end

@implementation TiUITableViewRowProxy

@synthesize tableClass, table, section, row, callbackCell;
@synthesize reusable = reusable_;

-(id)init
{
	if (self = [super init]) {
		reusable_ = YES;
	}
	return self;
}

-(void)_destroy
{
	RELEASE_TO_NIL(tableClass);
	TiThreadRemoveFromSuperviewOnMainThread(view, NO);
	TiThreadReleaseOnMainThread(view, NO);
	view = nil;
	[callbackCell setProxy:nil];
	callbackCell = nil;
	[super _destroy];
}

-(void)setTable:(TiUITableView *)newTable
{
	if ([self isAttached])
    {
        [self detachView];
    }
	table = newTable;
    
    //use that time to make sure we dont have pendingAdds (would make scrolling a lot slower)
    [self processPendingAdds];
}

-(void)processPendingAdds
{
    ENSURE_UI_THREAD_0_ARGS
    [super processPendingAdds];
}

-(id)_initWithPageContext:(id<TiEvaluator>)context_ args:(NSArray*)args
{
    return [self _initWithPageContext:context_ args:args withPropertiesInit:YES];
}

-(void)_initWithProperties:(NSDictionary *)properties
{
	[super _initWithProperties:properties];
//	self.modelDelegate = self;
}

-(void)layoutChildren:(BOOL)optimize
{
    if (configuredChildren)
        [super layoutChildren:optimize];
}

-(void)setClassName:(id)value
{
    RELEASE_TO_NIL(tableClass);
    [self replaceValue:value forKey:@"className" notification:YES];
}

-(NSString*)tableClass
{
	if (tableClass==nil)
	{
		// must use undefined key since class is a special 
		// property on the NSObject class
		id value = [self valueForUndefinedKey:@"className"];
		if (value==nil)
		{
			value = defaultRowTableClass;
		}
		// tableClass must always be a string so we coerce it
		tableClass = [[TiUtils stringValue:value] retain];
	}
	return tableClass;
}

-(id)height
{
    return [self valueForUndefinedKey:@"height"];
}

-(void)setHeight:(id)value
{
    [super setHeight:value];
    [self update];
}

-(id) backgroundLeftCap
{
    return [self valueForUndefinedKey:@"backgroundLeftCap"];
}

-(void)setBackgroundLeftCap:(id)value
{
    leftCap = TiDimensionFromObject(value);
    [self replaceValue:value forKey:@"backgroundLeftCap" notification:NO];
    if (callbackCell != nil) {
        [self configureBackground:callbackCell];
    }
}

-(id) backgroundTopCap
{
    return [self valueForUndefinedKey:@"backgroundTopCap"];
}

-(void)setBackgroundTopCap:(id)value
{
    topCap = TiDimensionFromObject(value);
    [self replaceValue:value forKey:@"backgroundTopCap" notification:NO];
    if (callbackCell != nil) {
        [self configureBackground:callbackCell];
    }
}

// Special handling to try and avoid Apple's detection of private API 'layout'
//-(void)setValue:(id)value forUndefinedKey:(NSString *)key
//{
//    if ([key isEqualToString:[@"lay" stringByAppendingString:@"out"]]) {
//        //CAN NOT USE THE MACRO 
//        if (ENFORCE_BATCH_UPDATE) {
//            if (updateStarted) {
//                [self setTempProperty:value forKey:key]; \
//                return;
//            }
//            else if(!allowLayoutUpdate){
//                return;
//            }
//        }
//        layoutProperties.layoutStyle = TiLayoutRuleFromObject(value);
//        [self replaceValue:value forKey:[@"lay" stringByAppendingString:@"out"] notification:YES];
//        return;
//    }
//    [super setValue:value forUndefinedKey:key];
//}

-(CGFloat)sizeWidthForDecorations:(CGFloat)oldWidth forceResizing:(BOOL)force
{
    CGFloat width = oldWidth;
    BOOL updateForiOS7 = NO;
    if (force || !configuredChildren) {
        if ([TiUtils boolValue:[self valueForKey:@"hasChild"] def:NO]) {
            width -= CHILD_ACCESSORY_WIDTH;
            updateForiOS7 = YES;
        }
        else if ([TiUtils boolValue:[self valueForKey:@"hasDetail"] def:NO]) {
            width -= DETAIL_ACCESSORY_WIDTH;
            updateForiOS7 = YES;
        }
        else if ([TiUtils boolValue:[self valueForKey:@"hasCheck"] def:NO]) {
            width -= CHECK_ACCESSORY_WIDTH;
            updateForiOS7 = YES;
        }
		
        id rightImage = [self valueForKey:@"rightImage"];
        if (rightImage != nil) {
            NSURL *url = [TiUtils toURL:rightImage proxy:self];
            UIImage *image = [[ImageLoader sharedLoader] loadImmediateImage:url];
            width -= [image size].width;
        }
		
        id leftImage = [self valueForKey:@"leftImage"];
        if (leftImage != nil) {
            NSURL *url = [TiUtils toURL:leftImage proxy:self];
            UIImage *image = [[ImageLoader sharedLoader] loadImmediateImage:url];
            width -= [image size].width;
        }
    }
    
    if (updateForiOS7 && [TiUtils isIOS7OrGreater]) {
        width -= IOS7_ACCESSORY_EXTRA_OFFSET;
    }
	
    return width;
}

-(CGFloat)rowHeight:(CGFloat)width
{
    TiDimension height = layoutProperties.height;
	if (TiDimensionIsDip(height))
	{
		return height.value;
	}
	CGFloat result = 0;
	if (TiDimensionIsAuto(height) || TiDimensionIsAutoSize(height) || TiDimensionIsUndefined(height))
	{
		result = [self minimumParentSizeForSize:CGSizeMake(width, INT_MAX)].height;
	}
    if (TiDimensionIsPercent(height) && [self table] != nil) {
        result = TiDimensionCalculateValue(height, [self table].bounds.size.height);
    }
	return (result == 0) ? [table tableRowHeight:0] : result;
}

-(void)updateRow:(NSDictionary *)data withObject:(NSDictionary *)properties
{
	modifyingRow = YES;
	[super _initWithProperties:data];
	
	// check to see if we have a section header change, too...
	if ([data objectForKey:@"header"])
	{
		[section setValue:[data objectForKey:@"header"] forUndefinedKey:@"headerTitle"];
		// we can return since we're reloading the section, will cause the 
		// row to be repainted at the same time
	}
	if ([data objectForKey:@"footer"])
	{
		[section setValue:[data objectForKey:@"footer"] forUndefinedKey:@"footerTitle"];
		// we can return since we're reloading the section, will cause the 
		// row to be repainted at the same time
	}
	modifyingRow = NO;
}

-(void)configureTitle:(UITableViewCell*)cell
{
	UILabel * textLabel = [cell textLabel];
    [textLabel setBackgroundColor:[UIColor clearColor]];

	NSString *title = [self valueForKey:@"title"];
	if (title!=nil)
	{
		[textLabel setText:title]; //UILabel already checks to see if it hasn't changed.
		
		UIColor * textColor = [[TiUtils colorValue:[self valueForKey:@"color"]] _color];
		[textLabel setTextColor:(textColor==nil)?[UIColor blackColor]:textColor];
		
		UIColor * selectedTextColor = [[TiUtils colorValue:[self valueForKey:@"selectedColor"]] _color];
		[textLabel setHighlightedTextColor:(selectedTextColor==nil)?[UIColor whiteColor]:selectedTextColor];
		
		id fontValue = [self valueForKey:@"font"];
		UIFont * font;
		if (fontValue!=nil)
		{
			font = [[TiUtils fontValue:fontValue] font];
		}
		else
		{
			font = [UIFont boldSystemFontOfSize:20.0]; //seems to be the default
		}
		[textLabel setFont:font];
	}
	else
	{
		[textLabel setText:nil];
	}
}

-(void)configureRightSide:(UITableViewCell*)cell
{
	BOOL hasChild = [TiUtils boolValue:[self valueForKey:@"hasChild"] def:NO];
	if (hasChild)
	{
		cell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;
	}
	else
	{
		BOOL hasDetail = [TiUtils boolValue:[self valueForKey:@"hasDetail"] def:NO];
		if (hasDetail)
		{
			cell.accessoryType = UITableViewCellAccessoryDetailDisclosureButton;
		}
		else
		{
			BOOL hasCheck = [TiUtils boolValue:[self valueForKey:@"hasCheck"] def:NO];
			if (hasCheck)
			{
				cell.accessoryType = UITableViewCellAccessoryCheckmark;
			}
			else
			{
				cell.accessoryType = UITableViewCellAccessoryNone;
			}
		}
	}
	id rightImage = [self valueForKey:@"rightImage"];
	if (rightImage!=nil)
	{
		NSURL *url = [TiUtils toURL:rightImage proxy:self];
		UIImage *image = [[ImageLoader sharedLoader] loadImmediateImage:url];
		cell.accessoryView = [[[UIImageView alloc] initWithImage:image] autorelease];
	}
    else {
        cell.accessoryView = nil;
    }
}

-(void)configureBackground:(UITableViewCell*)cell
{
	[(TiUITableViewCell *)cell setBackgroundGradient_:[self valueForKey:@"backgroundGradient"]];
	[(TiUITableViewCell *)cell setSelectedBackgroundGradient_:[self valueForKey:@"selectedBackgroundGradient"]];

	id bgImage = [self valueForKey:@"backgroundImage"];
	id selBgColor = [self valueForKey:@"selectedBackgroundColor"];

	if (bgImage!=nil)
	{
		NSURL *url = [TiUtils toURL:bgImage proxy:(TiProxy*)table.proxy];
		UIImage *image = [[ImageLoader sharedLoader] loadImmediateStretchableImage:url withLeftCap:leftCap topCap:topCap];
		if ([cell.backgroundView isKindOfClass:[UIImageView class]]==NO)
		{
			UIImageView *view_ = [[[UIImageView alloc] initWithFrame:CGRectZero] autorelease];
			cell.backgroundView = view_;
		}
		if (image!=((UIImageView*)cell.backgroundView).image)
		{
			((UIImageView*)cell.backgroundView).image = image;
		}
	}
	else if (cell.backgroundView!=nil && [cell.backgroundView isKindOfClass:[UIImageView class]] && ((UIImageView*)cell.backgroundView).image!=nil)
	{
		cell.backgroundView = nil;
	}
	
    id selBgImage = [self valueForKey:@"selectedBackgroundImage"];
    if (selBgImage!=nil) {
        NSURL *url = [TiUtils toURL:selBgImage proxy:(TiProxy*)table.proxy];
        UIImage *image = [[ImageLoader sharedLoader] loadImmediateStretchableImage:url withLeftCap:leftCap topCap:topCap];
        if ([cell.selectedBackgroundView isKindOfClass:[UIImageView class]]==NO) {
            UIImageView *view_ = [[[UIImageView alloc] initWithFrame:CGRectZero] autorelease];
            cell.selectedBackgroundView = view_;
        }
        if (image!=((UIImageView*)cell.selectedBackgroundView).image) {
            ((UIImageView*)cell.selectedBackgroundView).image = image;
        }
        
        UIColor* theColor = [Webcolor webColorNamed:selBgColor];
        cell.selectedBackgroundView.backgroundColor = ((theColor == nil)?[UIColor clearColor]:theColor);
    } else {
        if (![cell.selectedBackgroundView isKindOfClass:[TiSelectedCellBackgroundView class]]) {
            cell.selectedBackgroundView = [[[TiSelectedCellBackgroundView alloc] initWithFrame:CGRectZero] autorelease];
        }
        TiSelectedCellBackgroundView *selectedBGView = (TiSelectedCellBackgroundView*)cell.selectedBackgroundView;
        selectedBGView.grouped = [[table tableView] style]==UITableViewStyleGrouped;
        UIColor* theColor = [Webcolor webColorNamed:selBgColor];
        if (theColor == nil) {
            switch (cell.selectionStyle) {
                case UITableViewCellSelectionStyleGray:theColor = [Webcolor webColorNamed:@"#bbb"];break;
                case UITableViewCellSelectionStyleNone:theColor = [UIColor clearColor];break;
                case UITableViewCellSelectionStyleBlue:theColor = [Webcolor webColorNamed:@"#0272ed"];break;
                default:theColor = [TiUtils isIOS7OrGreater] ? [Webcolor webColorNamed:@"#e0e0e0"] : [Webcolor webColorNamed:@"#0272ed"];break;
            }
        }
        selectedBGView.fillColor = theColor;
        int count = [section rowCount];
        if (count == 1) {
            selectedBGView.position = TiCellBackgroundViewPositionSingleLine;
        }
        else {
            if (row == 0) {
                selectedBGView.position = TiCellBackgroundViewPositionTop;
            }
            else if (row == count-1) {
                selectedBGView.position = TiCellBackgroundViewPositionBottom;
            }
            else {
                selectedBGView.position = TiCellBackgroundViewPositionMiddle;
            }
        }
    }
}

-(void)configureLeftSide:(UITableViewCell*)cell
{
	id image = [self valueForKey:@"leftImage"];
	if (image!=nil)
	{
		NSURL *url = [TiUtils toURL:image proxy:(TiProxy*)table.proxy];
		UIImage *image = [[ImageLoader sharedLoader] loadImmediateImage:url];
		if (cell.imageView.image!=image)
		{
			cell.imageView.image = image;
		}
	}
	else if (cell.imageView!=nil && cell.imageView.image!=nil)
	{
		cell.imageView.image = nil;
	}
}

-(void)configureIndentionLevel:(UITableViewCell*)cell
{
	cell.indentationLevel = [TiUtils intValue:[self valueForKey:@"indentionLevel"] def:0];
}

-(void)configureSelectionStyle:(UITableViewCell*)cell
{
	id value = [self valueForKey:@"selectionStyle"];
	if (value == nil)
	{
		if (table!=nil)
		{
			// look at the tableview if not on the row
			value = [[table proxy] valueForUndefinedKey:@"selectionStyle"];
		}
	}
	if (value!=nil)
	{
		cell.selectionStyle = [TiUtils intValue:value];
	}
	else
	{
		cell.selectionStyle = UITableViewCellSelectionStyleBlue;
	}
}

-(UIView *)parentViewForChild:(TiViewProxy *)child
{
	return view;
}

-(BOOL)viewAttached
{
	return (callbackCell != nil) && (callbackCell.proxy == self);
}

-(BOOL)canHaveControllerParent
{
	return NO;
}

-(void)redelegateViews:(TiViewProxy *)proxy toView:(UIView *)touchDelegate;
{
	[[proxy view] setTouchDelegate:touchDelegate];
    NSArray* subproxies = [proxy children];
	for (TiViewProxy * childProxy in subproxies)
	{
		[self redelegateViews:childProxy toView:touchDelegate];
	}
}

-(TiProxy*)parentForBubbling
{
	return section;
}

-(UIView*)view
{
	return view;
}
//Private method :For internal use only. Called from layoutSubviews of the cell.
-(void)triggerLayout
{
    if (modifyingRow) {
        return;
    }
    modifyingRow = YES;
    [TiLayoutQueue layoutProxy:self];
    modifyingRow = NO;
    
}

- (void)prepareTableRowForReuse
{
    [super prepareForReuse];
    
	if (self.reusable) {
		return;
	}
    
	if (![self.tableClass isEqualToString:defaultRowTableClass]) {
		return;
	}
    
    [self detachView];
}

//we dont want the force create view to work. We want to handle this ourselves
//-(TiUIView*)getOrCreateView
//{
//    return view;
//}

//-(void)detachView
//{
//    [destroyLock lock];
//    
//    pthread_rwlock_rdlock(&childrenLock);
//    [[self children] makeObjectsPerformSelector:@selector(detachView)];
//    pthread_rwlock_unlock(&childrenLock);
//    
//	if (view!=nil)
//	{
//		[self viewWillDetach];
//		[view removeFromSuperview];
//		view.proxy = nil;
//        readyToCreateView = NO;
//		if (self.modelDelegate!=nil)
//		{
//            if ([self.modelDelegate respondsToSelector:@selector(detachProxy)])
//                [self.modelDelegate detachProxy];
//            self.modelDelegate = nil;
//		}
//		RELEASE_TO_NIL(view);
//		[self viewDidDetach];
//	}
//	[destroyLock unlock];
//}

- (void)didReceiveMemoryWarning:(NSNotification *)notification
{
    [super didReceiveMemoryWarning:notification];
}


-(void)configureChildren:(UITableViewCell*)cell
{
	// this method is called when the cell is initially created
	// to be initialized. on subsequent repaints of a re-used
	// table cell, the updateChildren below will be called instead
	configuredChildren = NO;
    [self setReadyToCreateView:NO];
    [self fakeOpening];
	if ([[self children] count] > 0)
	{
		UIView *contentView = cell.contentView;
		CGRect rect = [contentView bounds];
        CGSize cellSize = [(TiUITableViewCell*)cell computeCellSize];
		CGFloat rowWidth = cellSize.width;
		CGFloat rowHeight = cellSize.height;
		if (rowHeight < rect.size.height || rowWidth < rect.size.width)
		{
			rect.size.height = rowHeight;
			rect.size.width = rowWidth;
			contentView.frame = rect;
		}
        else if (CGSizeEqualToSize(rect.size, CGSizeZero)) {
            rect.size = CGSizeMake(rowWidth, rowHeight);
            [contentView setFrame:rect];
        }
		rect.origin = CGPointZero;
		if (self.reusable || (view == nil)) {
            TiUITableViewRowContainer* newcontainer = nil;
			if (self.reusable) {
                newcontainer = (TiUITableViewRowContainer*)[[cell contentView] viewWithTag:kRowContainerTag];
			}
			NSArray *rowChildren = [self children];
			if (self.reusable && (newcontainer != nil)) {
				__block BOOL canReproxy = YES;
				NSArray *existingSubviews = [newcontainer subviews];
				if ([rowChildren count] != [existingSubviews count]) {
					DebugLog(@"[ERROR] Cannot reproxy not same number of childs");
					canReproxy = NO;
				} else {
					[rowChildren enumerateObjectsUsingBlock:^(TiViewProxy *proxy, NSUInteger idx, BOOL *stop) {
						TiUIView *uiview = [existingSubviews objectAtIndex:idx];
						if (![uiview validateTransferToProxy:proxy deep:YES]) {
							canReproxy = NO;
							*stop = YES;
						}
					}];
				}
				if (!canReproxy && ([existingSubviews count] > 0)) {
					DebugLog(@"[ERROR] TableViewRow structures for className %@ does not match", self.tableClass);
                    [newcontainer detach];
                    newcontainer = nil;
				}
			}
            
            if (newcontainer != nil)
            {
                RELEASE_TO_NIL(view);
                view = [newcontainer retain];
            }
            else if (view != nil){
                //we cant reuse so in any case we have to clear the view. The question is do we really remove the views
                //associated. They might actually be used by another cell
                if ([view proxy] == self || [view proxy] == nil)
                    [self detachView];
                else
                    [self clearView:YES];//our currentRowContainer might be used by someone else, lets just set view to nil
                RELEASE_TO_NIL(view);
            }
            
			if (view == nil) {
				view = [[TiUITableViewRowContainer alloc] initWithFrame:rect];
                view.tag = kRowContainerTag;
                [view setBackgroundColor:[UIColor clearColor]];
                [view setAutoresizingMask:UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight];
				[contentView addSubview:view];
			} else {
				[view setFrame:rect];
			}
            
            view.proxy = self;
			
			NSArray *existingSubviews = [view subviews];
			[rowChildren enumerateObjectsUsingBlock:^(TiViewProxy *proxy, NSUInteger idx, BOOL *stop) {
				TiUIView *uiview = idx < [existingSubviews count] ? [existingSubviews objectAtIndex:idx] : nil;
				if (!CGRectEqualToRect([proxy sandboxBounds], rect)) {
					[proxy setSandboxBounds:rect];
				}
                
				[proxy setReproxying:YES];
                if (uiview == nil) {
                    
                    //1 detach view if necessary
//                    [proxy runBlock:^(TiViewProxy *blockproxy) {
//                        [blockproxy detachView:NO];
//                    } onlyVisible:NO recursive:YES];
                    
                    //First case no reusable cell container
                    
                    // 1 create view recursively
					[view addSubview:[proxy getOrCreateView]];
                    
                    // we use on recursion to optimize
                    [proxy runBlock:^(TiViewProxy *blockproxy) {
                        //we dont really want to use willOpenWindow so we fake opening
                        [blockproxy fakeOpening];
                        
                        //we update touchDelegate
                        [[blockproxy view] setTouchDelegate:contentView];
                        
                        //now we are ready to create sub added views
                        [blockproxy setReadyToCreateView:YES recursive:NO];
                    } onlyVisible:NO recursive:YES];
				}
                else{
                    //reusable cell container
                    
                    //we transfer proxy and use its recursion to apply blocks
                    [uiview transferProxy:proxy withBlockBefore:^(TiViewProxy *blockproxy) {
                        //we dont really want to use willOpenWindow so we fake opening
                        [blockproxy fakeOpening];
                    } withBlockAfter:^(TiViewProxy *blockproxy) {
                        
                        //we update touchDelegate
                        [[blockproxy view] setTouchDelegate:contentView];
                        
                        //now we are ready to create sub added views
                        [blockproxy setReadyToCreateView:YES recursive:NO];
                    } deep:YES];
                }
				[proxy setReproxying:NO];
                uiview = nil;
			}];
		} else {
			[view setFrame:rect];
			[contentView addSubview:view];
		}
	}
	configuredChildren = YES;
    //now we are ready to create views!
    [self setReadyToCreateView:YES recursive:NO];
    [self fakeOpening];
}

-(void)initializeTableViewCell:(UITableViewCell*)cell
{
	modifyingRow = YES;
	[self configureTitle:cell];
	[self configureSelectionStyle:cell];
	[self configureLeftSide:cell];
	[self configureRightSide:cell];
	[self configureBackground:cell];
	[self configureIndentionLevel:cell];
	[self configureChildren:cell];
	[self configureAccessibility:cell];
	modifyingRow = NO;
}

-(BOOL)isAttached
{
	return (table!=nil) && ([self parent]!=nil);
}

-(void)triggerAttach
{
	if (!attaching && ![self viewAttached]) {
		attaching = YES;
		[self windowWillOpen];
		[self willShow];
		attaching = NO;
	}
}

-(void)updateRow:(TiUITableViewAction*)action
{
	OSAtomicTestAndClearBarrier(NEEDS_UPDATE_ROW, &dirtyRowFlags);
	[table dispatchAction:action];
}

-(void)update
{
    if ([self isAttached] && !modifyingRow && !attaching)
	{
        TiThreadPerformOnMainThread(^{
			[UIView setAnimationsEnabled:NO];
            [[[self table] tableView] beginUpdates];
            [[[self table] tableView] endUpdates];
            [UIView setAnimationsEnabled:YES];
        }, NO);
    }
}

-(void)updateAnimated:(TiAnimation*)animation
{
    if ([self isAttached] && !modifyingRow && !attaching)
	{
        TiThreadPerformOnMainThread(^{
            [CATransaction begin];
            [[[self table] tableView] beginUpdates];
            [[[self table] tableView] endUpdates];
            [CATransaction commit];
        }, NO);
    }
}

-(void)triggerRowUpdate
{	
	if ([self isAttached] && self.viewAttached && !modifyingRow && !attaching)
	{
		if (OSAtomicTestAndSetBarrier(NEEDS_UPDATE_ROW, &dirtyRowFlags)) {
			return;
		}
		
		TiUITableViewAction *action = [[[TiUITableViewAction alloc] initWithObject:self 
																		 animation:nil 
																			  type:TiUITableViewActionRowReload] autorelease];
		TiThreadPerformOnMainThread(^{[self updateRow:action];}, NO);
	}
}

-(void)windowWillOpen
{
	attaching = YES;
	[super windowWillOpen];
	[self setParentVisible:YES];
	attaching = NO;
}

//-(void)triggerUpdateIfHeightChanged
//{
//    TiThreadPerformOnMainThread(^{
//        if ([self viewAttached] && rowContainerView != nil) {
//            CGFloat curHeight = rowContainerView.bounds.size.height;
//            CGSize newSize = [callbackCell computeCellSize];
//            if (newSize.height != curHeight) {
//                DeveloperLog(@"Height changing from %.1f to %.1f. Triggering update.",curHeight,newSize.height);
//                [self triggerRowUpdate];
//            } else {
//                DeveloperLog(@"Height does not change. Just laying out children. Height %.1f",curHeight);
//                [callbackCell setNeedsDisplay];
//            }
//        } else {
//            [callbackCell setNeedsDisplay];
//        }
//    }, NO);
//}

-(void)contentsWillChange
{
	if (attaching==NO)
	{
		//[self triggerUpdateIfHeightChanged];
	}
}

-(void)repositionWithinAnimation:(TiAnimation*)animation
{
//	[self triggerUpdateIfHeightChanged];
}

-(void)childWillResize:(TiViewProxy *)child withinAnimation:(TiViewAnimationStep*)animation
{
    if (animation)
        [self updateAnimated:animation];
    else
        [self update];
	[super childWillResize:child withinAnimation:animation];
}

-(TiProxy *)touchedViewProxyInCell:(UITableViewCell *)targetCell atPoint:(CGPoint*)point
{
    if (view != nil)
    {
        TiProxy * result = [(TiUITableViewRowContainer*)view hitTarget];
        *point = [(TiUITableViewRowContainer*)view hitPoint];
        if (result != nil)
        {
            return result;
        }
    }
	return self;
}

-(id)createEventObject:(id)initialObject
{
    if (initialObject && [initialObject objectForKey:@"row"])
        return initialObject; //row property already there
    
	NSMutableDictionary *dict = nil;
	if (initialObject == nil)
	{
		dict = [NSMutableDictionary dictionary];
	}
	else
	{
		dict = [NSMutableDictionary dictionaryWithDictionary:initialObject];
	}
	NSInteger index = [table indexForRow:self];
	[dict setObject:NUMINT(index) forKey:@"index"];
    // TODO: We really need to ensure that a row's section is set upon creation - even if this means changing how tables work.
    if (section != nil) {
        [dict setObject:section forKey:@"section"];
    }
	[dict setObject:self forKey:@"row"];
	[dict setObject:self forKey:@"rowData"];
	[dict setObject:NUMBOOL(NO) forKey:@"detail"];
	[dict setObject:NUMBOOL(NO) forKey:@"searchMode"];
	
	return dict;
}

//TODO: Remove when deprication is done.
-(void)fireEvent:(NSString*)type withObject:(id)obj withSource:(id)source propagate:(BOOL)propagate reportSuccess:(BOOL)report errorCode:(int)code message:(NSString*)message;
{
	// merge in any row level properties for the event
	if (source!=self)
	{
		obj = [self createEventObject:obj];
	}
	[callbackCell handleEvent:type];
	[super fireEvent:type withObject:obj withSource:source propagate:propagate reportSuccess:report errorCode:code message:message];
}

-(void)fireEvent:(NSString*)type withObject:(id)obj propagate:(BOOL)propagate reportSuccess:(BOOL)report errorCode:(int)code message:(NSString*)message;
{
	[callbackCell handleEvent:type];
	[super fireEvent:type withObject:obj propagate:propagate reportSuccess:report errorCode:code message:message];
}

-(void)configureAccessibility:(UITableViewCell*)cell
{
    cell.accessibilityLabel = [TiUtils stringValue:[self valueForUndefinedKey:@"accessibilityLabel"]];
	cell.accessibilityValue = [TiUtils stringValue:[self valueForUndefinedKey:@"accessibilityValue"]];
	cell.accessibilityHint = [TiUtils stringValue:[self valueForUndefinedKey:@"accessibilityHint"]];
}

-(void)configureBackgroundColor
{
	if (callbackCell == nil) return;
    NSString *color = [self valueForKey:@"backgroundColor"];
	if (color==nil)
	{
		color = [table.proxy valueForKey:@"rowBackgroundColor"];
	}
    if (color==nil)
	{
		color = [table.proxy valueForKey:@"backgroundColor"];
	}
	UIColor * cellColor = [Webcolor webColorNamed:color];
	if (cellColor == nil) {
		cellColor = [UIColor whiteColor];
	}
    [callbackCell setBackgroundColor:cellColor];
}

-(void)propertyChanged:(NSString*)key oldValue:(id)oldValue newValue:(id)newValue proxy:(TiProxy*)proxy
{
	if (callbackCell == nil) return;
    else if ([key isEqualToString:@"hasDetail"] ||
             [key isEqualToString:@"hasChild"] ||
             [key isEqualToString:@"rightImage"] ||
             [key isEqualToString:@"hasCheck"])
		TiThreadPerformOnMainThread(^{[self configureRightSide:callbackCell];}, NO);
    else if ([key isEqualToString:@"leftImage"])
		TiThreadPerformOnMainThread(^{[self configureLeftSide:callbackCell];}, NO);
    else if ([key isEqualToString:@"selectedBackgroundImage"] ||
             [key isEqualToString:@"selectedBackgroundColor"] ||
             [key isEqualToString:@"selectedBackgroundGradient"] ||
             [key isEqualToString:@"backgroundImage"] ||
             [key isEqualToString:@"backgroundGradient"] ||
             [key isEqualToString:@"selectionStyle"])
		TiThreadPerformOnMainThread(^{[self configureBackground:callbackCell];}, NO);
    else if ([key isEqualToString:@"indentionLevel"])
		TiThreadPerformOnMainThread(^{[self configureIndentionLevel:callbackCell];}, NO);
    else if ([key isEqualToString:@"color"] ||
                  [key isEqualToString:@"selectedColor"] ||
                  [key isEqualToString:@"title"])
		TiThreadPerformOnMainThread(^{[self configureTitle:callbackCell];}, YES);
    else if ([key isEqualToString:@"accessibilityLabel"] ||
             [key isEqualToString:@"accessibilityValue"] ||
             [key isEqualToString:@"accessibilityHint"])
		TiThreadPerformOnMainThread(^{[self configureAccessibility:callbackCell];}, YES);
}

-(TiDimension)defaultAutoHeightBehavior:(id)unused
{
    return TiDimensionAutoSize;
}

#pragma mark Animation Delegates

-(id)animationDelegate
{
    return self;
}

-(void)animationWillStart:(id)sender
{
    TiAnimation* anim = (TiAnimation*)sender;
//    CABasicAnimation *positionAnimation = nil;
//        
//    positionAnimation = [CABasicAnimation animationWithKeyPath:@"position"];
//    positionAnimation.fromValue = [NSValue valueWithCGPoint:CGPointMake([view bounds].size.width / 2, [view bounds].size.height / 2)];
//    positionAnimation.duration = [anim animationDuration];
    
}

-(void)animationDidComplete:(id)sender
{
    
}


@end

#endif