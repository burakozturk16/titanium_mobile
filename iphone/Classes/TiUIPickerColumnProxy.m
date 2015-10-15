/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UIPICKER

#import "TiUIPickerColumnProxy.h"
#import "TiUIPickerRowProxy.h"
#import "TiUIPickerProxy.h"
#import "TiUIPicker.h"
#import "TiUtils.h"

@implementation TiUIPickerColumnProxy

@synthesize column;

//-(void)dealloc
//{
//	RELEASE_TO_NIL(rows);
//	[super dealloc];
//}

-(NSString*)apiName
{
    return @"Ti.UI.PickerColumn";
}

//-(NSMutableArray*)rows
//{
//	// return copy so developer can't directly mutate
//	return [[rows copy] autorelease];
//}
//
//-(NSInteger)rowCount
//{
//	return rowsC;
//}
//
//-(id)rowAt:(NSInteger)index
//{
//	return (index < [rows count]) ? [rows objectAtIndex:index] : nil;
//}


+ (NSString*)defaultTemplateType
{
    return @"Ti.UI.PickerRow";
}

-(TiUIView*)getOrCreateView
{
    return nil;
}

-(TiUIView*)view
{
    return nil;
}

-(void)addProxy:(id)child atIndex:(NSInteger)position shouldRelayout:(BOOL)shouldRelayout
{
    if (IS_OF_CLASS(child, TiUIPickerRowProxy)) {
        [super addProxy:child atIndex:position shouldRelayout:shouldRelayout];
    }
}

-(void)removeProxy:(id)child shouldDetach:(BOOL)shouldDetach
{
    if (IS_OF_CLASS(child, TiUIPickerRowProxy)) {
        [self removeProxy:child shouldDetach:shouldDetach];
    }
}

-(void)childAdded:(TiProxy*)child atIndex:(NSInteger)position shouldRelayout:(BOOL)shouldRelayout
{
    if ([TiUtils boolValue:[child valueForUndefinedKey:@"selected"] def:NO])
    {
        if (IS_OF_CLASS(parent, TiUIPickerProxy)) {
            TiThreadPerformOnMainThread(^{[[(TiUIPickerProxy*)[self parent] picker] selectRow:
                                           [NSArray arrayWithObjects:NUMINT(0),position,nil]];}, NO);
        }
    }
}

-(TiUIPickerRowProxy*)rowAt:(NSUInteger)index
{
    return (TiUIPickerRowProxy*)[self childAt:index];
}

//-(NSNumber*)addRow:(id)row
//{
//	ENSURE_SINGLE_ARG(row,TiUIPickerRowProxy);
//	if (rows==nil)
//	{
//		rows = [[NSMutableArray arrayWithObject:row] retain];
//	}
//	else
//	{
//		[rows addObject:row];
//	}
//	return NUMUINTEGER([rows count]-1);
//}
//
//-(void)removeRow:(id)row
//{
//	ENSURE_SINGLE_ARG(row,TiUIPickerRowProxy);
//	if (rows!=nil)
//	{
//		[rows removeObject:row];
//	}
//}


@end

#endif