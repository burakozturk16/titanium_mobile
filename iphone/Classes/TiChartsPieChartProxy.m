/**
 * Ti.Charts Module
 * Copyright (c) 2011-2013 by Appcelerator, Inc. All Rights Reserved.
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiChartsPieChartProxy.h"
#import "TiChartsPieSegmentProxy.h"

@implementation TiChartsPieChartProxy

-(NSUInteger)numberOfRecordsForPlot:(CPTPlot *)plot
{
    return [plots count];
}

-(NSNumber *)numberForPlot:(CPTPlot *)plot field:(NSUInteger)fieldEnum recordIndex:(NSUInteger)idx
{
    if (fieldEnum == CPTPieChartFieldSliceWidth) {
        TiChartsPieSegmentProxy* segment = [plots objectAtIndex:idx];
        if (segment)
            return [NSDecimalNumber decimalNumberWithDecimal:[segment.value decimalValue]];
    }
    return [NSDecimalNumber zero];
}

-(CPTFill *)sliceFillForPieChart:(CPTPieChart *)pieChart recordIndex:(NSUInteger)idx
{
    TiChartsPieSegmentProxy* segment = [plots objectAtIndex:idx];
    if (segment)
        return segment.fill;
    return nil;
}


-(CPTLineStyle *)sliceBorderForPieChart:(CPTPieChart *)pieChart recordIndex:(NSUInteger)idx
{
    TiChartsPieSegmentProxy* segment = [plots objectAtIndex:idx];
    if (segment)
        return segment.border;
    return nil;
}

-(CPTLayer *)dataLabelForPlot:(CPTPlot *)plot recordIndex:(NSUInteger)idx
{
    TiChartsPieSegmentProxy* segment = [plots objectAtIndex:idx];
    if (segment)
        return [[CPTTextLayer alloc] initWithText:segment.title style:segment.labelStyle];
    return nil;
}

-(CGFloat)radialOffsetForPieChart:(CPTPieChart *)pieChart recordIndex:(NSUInteger)index
{

    TiChartsPieSegmentProxy* segment = [plots objectAtIndex:index];
    if (segment)
        return segment.explodeOffset;
	
	return 0.0f;
}

//
//-(void)pieChart:(CPTPieChart *)plot sliceWasSelectedAtRecordIndex:(NSUInteger)idx
//{
//    
//}
//
//-(void)pieChart:(CPTPieChart *)plot sliceWasSelectedAtRecordIndex:(NSUInteger)idx withEvent:(CPTNativeEvent *)event
//{
//    
//}
@end
