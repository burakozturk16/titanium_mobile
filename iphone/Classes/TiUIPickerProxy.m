/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UIPICKER

#import "TiUIPickerProxy.h"
#import "TiUIPickerColumnProxy.h"
#import "TiUIPickerRowProxy.h"
#import "TiUIPicker.h"
#import "TiUtils.h"

NSArray* pickerKeySequence;

@implementation TiUIPickerProxy

-(NSArray *)keySequence
{
	if (pickerKeySequence == nil)
	{
		pickerKeySequence = [[[super keySequence] arrayByAddingObjectsFromArray:@[@"type",@"minDate",@"maxDate",@"minuteInterval"]] retain];
	}
	return pickerKeySequence;
}

-(void)_configure
{
	[self replaceValue:NUMINT(-1) forKey:@"type" notification:NO];
	[self replaceValue:nil forKey:@"value" notification:NO];
	[super _configure];
}

-(NSString*)apiName
{
    return @"Ti.UI.Picker";
}

-(void)viewDidAttach
{
    [super viewDidAttach];
    NSArray* selectOnLoad  =[self valueForUndefinedKey:@"selectedRows"];
    if (selectOnLoad != nil) {
        [self setSelectedRows:selectOnLoad];
    }
}

//-(void)windowDidOpen
//{
//    [super windowDidOpen];
//    NSArray* selectOnLoad  =[self valueForUndefinedKey:@"selectedRows"];
//    if (selectOnLoad != nil) {
//        [self setSelectedRows:selectOnLoad];
//    }
//}

-(BOOL)supportsNavBarPositioning
{
	return NO;
}

//-(NSMutableArray*)columns
//{
//	NSMutableArray* columns = [self valueForUndefinedKey:@"columns"];
//	if (columns==nil)
//	{
//		columns = [NSMutableArray array];
//		[self replaceValue:columns forKey:@"columns" notification:NO];
//	}
//	return columns;
//}

//-(void)windowWillOpen
//{
//	[super windowWillOpen];
//	
//	// Tell all of the picker bits that their window has opened.  Can't operate
//	// on the rows array directly; they're returned as a copy from the column.
//	for (TiUIPickerColumnProxy* column in [self columns]) {
//		for (NSInteger i=0; i < [column childrenCount]; i++) {
//			[[column rowAt:i] windowWillOpen];
//		}
//	}
//}

-(TiUIPicker*)picker
{
	return (TiUIPicker*)[self view];
}

-(TiUIPickerColumnProxy*)columnAt:(NSInteger)index
{
    TiUIPickerColumnProxy *column = (TiUIPickerColumnProxy*)[self childAt:(index)];
	if (!column)
	{
        column = [[[TiUIPickerColumnProxy alloc] _initWithPageContext:[self executionContext]] autorelease];
        [self addProxy:column atIndex:index shouldRelayout:true];
        column.column = index;
	}
	return column;
}

#pragma mark support methods for add: 
-(void)childAdded:(TiProxy*)child atIndex:(NSInteger)position shouldRelayout:(BOOL)shouldRelayout
{
    [self reloadColumn:child atIndex:position];
}
//-(void)addPickerRow:(NSDictionary*)params {
//	ENSURE_UI_THREAD(addPickerRow,params);
//	TiUIPickerRowProxy *row = [params objectForKey:@"row"];
//	TiUIPickerColumnProxy *column = [params objectForKey:@"column"];
//	NSNumber* rowIndex = [column addRow:row];
//	
//	if (windowOpened) {
//		[row windowWillOpen];
//		[row windowDidOpen];
//	}
//	
//	[self reloadColumn:column];
//	if ([TiUtils boolValue:[row valueForUndefinedKey:@"selected"] def:NO])
//	{
//		TiThreadPerformOnMainThread(^{[[self picker] selectRow:
//				[NSArray arrayWithObjects:NUMINT(0),rowIndex,nil]];}, NO);
//	}
//}

//-(void)addPickerColumn:(NSDictionary*)params {
//	ENSURE_UI_THREAD_1_ARG(params);
//	NSMutableArray *columns = [params objectForKey:@"columns"];
//	TiUIPickerColumnProxy *column = [params objectForKey:@"column"];
//	if (windowOpened) {
//		for (NSInteger i=0; i < [column rowCount]; i++) {
//			TiUIPickerRowProxy* row = [column rowAt:i];
//			
//			[row windowWillOpen];
//			[row windowDidOpen];
//		}
//	}
//	
//	[columns addObject:column];
//	[self reloadColumn:column];
//}

//-(void)addRowOfColumns:(NSDictionary*)params {
//	ENSURE_UI_THREAD_1_ARG(params);
//	NSMutableArray *columns = [params objectForKey:@"columns"];
//	NSArray *data = [params objectForKey:@"data"];
//	for (id column in data)
//	{
//		if (windowOpened) {
//			for (NSInteger i=0; i < [column rowCount]; i++) {
//				TiUIPickerRowProxy* row = [column rowAt:i];
//				
//				[row windowWillOpen];
//				[row windowDidOpen];
//			}
//		}
//		
//		[columns addObject:column];
//	}
//}

//-(void)addRowOfDicts:(NSDictionary*)params {
//	ENSURE_UI_THREAD_1_ARG(params);
//	TiUIPickerRowProxy *row = [params objectForKey:@"row"];
//	TiUIPickerColumnProxy *column = [params objectForKey:@"column"];
//	NSNumber* rowIndex = [params objectForKey:@"rowIndex"];
//	if (windowOpened) {
//		[row windowWillOpen];
//		[row windowDidOpen];
//	}
//	[self reloadColumn:column];
//	if ([TiUtils boolValue:[row valueForUndefinedKey:@"selected"] def:NO])
//	{
//		[self setSelectedRow:[NSArray arrayWithObjects:NUMINT(0),rowIndex,NUMBOOL(NO),nil]];
//	}
//}
//
//-(void)addDefault:(NSDictionary*)params {
//	ENSURE_UI_THREAD_1_ARG(params);
//	TiUIPickerColumnProxy *column = [params objectForKey:@"column"];
//	NSArray *data = [params objectForKey:@"data"];
//	for (id item in data)
//	{
//		ENSURE_TYPE(item,TiUIPickerRowProxy);
//		
//		if (windowOpened) {
//			[item windowWillOpen];
//			[item windowDidOpen];
//		}
//		
//		[column addRow:item];
//	}
//	[self reloadColumn:column];
//}

#pragma mark Public APIs 

- (id)value
{
    if (![NSThread isMainThread]) {
		__block id result = nil;
		TiThreadPerformOnMainThread(^{result = [[[self picker] value_] retain];}, YES);
		return [result autorelease];
	}
	return [[self picker] value_];
}

+ (NSString*)defaultTemplateType
{
    return @"Ti.UI.PickerColumn";
}

-(void)addProxy:(id)child atIndex:(NSInteger)position shouldRelayout:(BOOL)shouldRelayout
{
    if (IS_OF_CLASS(child, TiUIPickerColumnProxy)) {
        [super addProxy:child atIndex:position shouldRelayout:shouldRelayout];
    } else if (IS_OF_CLASS(child, TiUIPickerRowProxy)) {
        [[self columnAt:0] addProxy:child atIndex:position shouldRelayout:shouldRelayout];
    }
}

-(void)removeProxy:(id)child shouldDetach:(BOOL)shouldDetach
{
    if (IS_OF_CLASS(child, TiUIPickerColumnProxy)) {
        [self removeProxy:child shouldDetach:shouldDetach];
    } else if (IS_OF_CLASS(child, TiUIPickerRowProxy)) {
        [[self columnAt:0] removeProxy:child shouldDetach:shouldDetach];
    }
}

//-(void)add:(id)args
//{
//	// TODO: Probably take advantage of Jeff's performance improvements in ordinary views.
//	// But DO NOT do this until after release!
//	id data = [args objectAtIndex:0];
//	
//	if ([data isKindOfClass:[TiUIPickerRowProxy class]])
//	{
//		TiUIPickerRowProxy *row = (TiUIPickerRowProxy*)data;
//		TiUIPickerColumnProxy *column = [self columnAt:0];
//		NSDictionary* params = [NSDictionary dictionaryWithObjectsAndKeys:row, @"row", column, @"column", nil];
//		[self addPickerRow:params];
//	}
//	else if ([data isKindOfClass:[TiUIPickerColumnProxy class]])
//	{
//		NSMutableArray *columns = [self columns];
//		TiUIPickerColumnProxy* column = (TiUIPickerColumnProxy*)data;
//		NSDictionary* params = [NSDictionary dictionaryWithObjectsAndKeys:columns, @"columns", column, @"column", nil];
//		[self addPickerColumn:params];
//	}
//	else if ([data isKindOfClass:[NSArray class]])
//	{
//		// peek to see what our first row is ... 
//		id firstRow = [data objectAtIndex:0];
//		
//		// if an array of columns, just add them
//		if ([firstRow isKindOfClass:[TiUIPickerColumnProxy class]])
//		{
//			NSMutableArray *columns = [self columns];
//			NSDictionary* params = [NSDictionary dictionaryWithObjectsAndKeys:columns, @"columns", data, @"data", nil];
//			[self addRowOfColumns:params];
//		}
//		else if ([firstRow isKindOfClass:[NSDictionary class]])
//		{
//			for (id rowdata in data)
//			{
//				TiUIPickerRowProxy *row = [[TiUIPickerRowProxy alloc] _initWithPageContext:[self executionContext] args:[NSArray arrayWithObject:rowdata]];
//				TiUIPickerColumnProxy *column = [self columnAt:0];
//				NSNumber* rowIndex = [column addRow:row];
//				[row release];
//
//				NSDictionary* params = [NSDictionary dictionaryWithObjectsAndKeys:row, @"row", column, @"column", rowIndex, @"rowIndex", nil];
//				[self addRowOfDicts:params];
//			}
//		}
//		else
//		{
//			TiUIPickerColumnProxy *column = [self columnAt:0];
//			NSDictionary* params = [NSDictionary dictionaryWithObjectsAndKeys:column, @"column", data, @"data", nil];
//			[self addDefault:params];
//		}
//	}
//}
//
//-(void)remove:(id)args
//{
//	//TODO
//}

-(void)setSelectedRows:(id)args
{
	if ([self viewAttached])
	{
        TiThreadPerformBlockOnMainThread(^{
            BOOL animated = YES;
            for (int i = 0; i < [args count]; i++) {
                [(TiUIPicker*)[self view] selectRowForColumn:i row:[TiUtils intValue:[args objectAtIndex:i]] animated:animated];
            }
        }, NO);
		
	}
    [self replaceValue:args forKey:@"selectedRows" notification:NO];
}

-(void)setColumns:(id)args
{
    //ensure ui thread to make sure removeAllChildren is called before the rest
    ENSURE_UI_THREAD_1_ARG(args)
    [self removeAllChildren:nil];
    if (IS_OF_CLASS(args, NSArray)) {
        [self replaceValue:args forKey:@"columns" notification:NO];
        [args enumerateObjectsUsingBlock:^(id rows, NSUInteger idx, BOOL *stop) {
            if (IS_OF_CLASS(rows, NSArray)) {
                TiUIPickerColumnProxy *column = [self columnAt:idx];
                [rows enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
                    NSDictionary* params = IS_OF_CLASS(obj, NSDictionary)?obj:@{
                                                                                @"title":[TiUtils stringValue:obj]
                                                                                };
                    [column add:params];
                }];
            }
        }];
    }
}

-(void)setRows:(id)args
{
    //ensure ui thread to make sure removeAllChildren is called before the rest
    ENSURE_UI_THREAD_1_ARG(args)
    [self removeAllChildren:nil];
    if (IS_OF_CLASS(args, NSArray)) {
        [self replaceValue:args forKey:@"rows" notification:NO];
        TiUIPickerColumnProxy *column = [self columnAt:0];
        [args enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
            NSDictionary* params = IS_OF_CLASS(obj, NSDictionary)?obj:@{@"title":[TiUtils stringValue:obj]};
            [column add:params];
        }];
    }
    
}

-(UIViewAutoresizing)verifyAutoresizing:(UIViewAutoresizing)suggestedResizing
{
	return suggestedResizing & ~(UIViewAutoresizingFlexibleHeight);
}

USE_VIEW_FOR_VERIFY_HEIGHT
USE_VIEW_FOR_VERIFY_WIDTH


-(void)reloadColumn:(id)column atIndex:(NSUInteger)columnIndex
{
//	ENSURE_SINGLE_ARG(column,NSObject);
//
//	if (![self viewAttached])
//	{
//		return;
//	}

//	ENSURE_VALUE_RANGE(columnIndex,0,[columnArray count]-1);
	[self makeViewPerformSelector:@selector(reloadColumn:) withObject:NUMUINTEGER(columnIndex) createIfNeeded:YES waitUntilDone:NO];
}

-(TiDimension)defaultAutoWidthBehavior:(id)unused
{
    return TiDimensionAutoSize;
}
-(TiDimension)defaultAutoHeightBehavior:(id)unused
{
    return TiDimensionAutoSize;
}

@end

#endif
