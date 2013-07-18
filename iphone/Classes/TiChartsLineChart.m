/**
 * Ti.Charts Module
 * Copyright (c) 2011-2013 by Appcelerator, Inc. All Rights Reserved.
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiChartsLineChart.h"
#import "TiChartsParsers.h"
#import "TiChartsPlotProxy.h"
#import "TiChartsChartProxy.h"


@implementation TiChartsLineChart


#pragma mark CPTPlotSpaceDelegate methods

-(void) initPlot{
    minXValue = 0;
    minYValue = 0;
    maxXValue = 0;
    maxYValue = 0;
    [super initPlot];
}

-(void)configureAxesX:(id)xProperties andY:(id)yProperties
{
	NSMutableArray *axes = [[[NSMutableArray alloc] init] autorelease];
	CPTXYAxis* axis;
	if (xProperties) {
		axis = [TiChartsParsers parseAxis:CPTCoordinateX properties:xProperties usingPlotSpace:graph.defaultPlotSpace def:nil];
		if (axis) {
			[axes addObject:axis];
		}
	}
	if (yProperties) {
		axis = [TiChartsParsers parseAxis:CPTCoordinateY properties:yProperties usingPlotSpace:graph.defaultPlotSpace def:nil];
		if (axis) {
			[axes addObject:axis];
		}
	}
    
	
	graph.axisSet.axes = [axes count] > 0 ? axes : nil;
	
	//NOTE: To support additional axes being added at a later time, copy the current axes set and add to the new one
}

-(void)configureUserInteraction
{
	userInteractionEnabled = [TiUtils boolValue:[self.proxy valueForUndefinedKey:@"userInteraction"] def:YES];
    if (userInteractionEnabled) {
        panEnabled = zoomEnabled = true;
    }
    panEnabled = [TiUtils boolValue:[self.proxy valueForUndefinedKey:@"panEnabled"] def:panEnabled];
    zoomEnabled = [TiUtils boolValue:[self.proxy valueForUndefinedKey:@"zoomEnabled"] def:zoomEnabled];
    clampInteraction = [TiUtils boolValue:[self.proxy valueForUndefinedKey:@"clampInteraction"] def:clampInteraction];
	for (CPTPlotSpace* plotSpace in graph.allPlotSpaces) {
		plotSpace.allowsUserInteraction = userInteractionEnabled;
		// Set the plotspace delegate so we get the shouldHandleTouch... callbacks
		plotSpace.delegate = self;
	}
	// Setting to YES reduces GPU memory usage, but can slow drawing/scrolling
	[[self hostingView] setAllowPinchScaling:userInteractionEnabled];
}

-(void)updateMinMaxWithPlot:(TiChartsPlotProxy*)plot
{
    if (plot.minXValue < minXValue)
        minXValue = plot.minXValue;
    if (plot.maxXValue > maxXValue)
        maxXValue = plot.maxXValue;
    if (plot.minYValue < minYValue)
        minYValue = plot.minYValue;
    if (plot.minYValue > maxYValue)
        maxYValue = plot.minYValue;
}

-(void)updateMinMax
{
    minXValue = 0;
    minYValue = 0;
    maxXValue = 0;
    maxYValue = 0;
    for (id plot in ((TiChartsChartProxy*)self.proxy).plots) {
        [self updateMinMaxWithPlot:plot];
    }
}

-(void)removeAllPlots
{
	[super removeAllPlots];
    minXValue = 0;
    minYValue = 0;
    maxXValue = 0;
    maxYValue = 0;
}

-(void)addPlot:(TiChartsPlotProxy*)plot
{
    [self updateMinMaxWithPlot:plot];
	[super addPlot:plot];

}

-(void)removePlot:(TiChartsPlotProxy *)plot
{
	[super removePlot:plot];
    [self updateMinMax];
}

-(CPTGraph*)newGraph
{
    return [[CPTXYGraph alloc] initWithFrame:CGRectZero];
}

-(void)configureGraph
{
    [super configureGraph];
    
	[self configureAxesX:[self.proxy valueForUndefinedKey:@"xAxis"] andY:[self.proxy valueForUndefinedKey:@"yAxis"]];
}

-(void)configurePlot
{
    [super configurePlot];
    
	[self configureUserInteraction];
	
}


-(void)refreshPlotSpaces
{
    [super refreshPlotSpaces];
    
	//BUGBUG: Set this as properties
	// Add these to property watch list
    
	BOOL scaleToFit;
	float expandBy;
    CPTXYPlotSpace* plotSpace;
    
	
	id options = [self.proxy valueForUndefinedKey:@"plotSpace"];
	if (options) {
		scaleToFit = [TiUtils boolValue:@"scaleToFit" properties:options def:(![options valueForKey:@"xRange"] && ![options valueForKey:@"xRange"])];
		expandBy = [TiUtils floatValue:@"expandRangeByFactor" properties:options def:1.0];
        
        if (scaleToFit == NO) {
            plotSpace = (CPTXYPlotSpace*)graph.defaultPlotSpace;
            plotSpace.xRange = [TiChartsParsers parsePlotRange:[options valueForKey:@"xRange"] def:plotSpace.xRange];
            plotSpace.yRange = [TiChartsParsers parsePlotRange:[options valueForKey:@"yRange"] def:plotSpace.yRange];
        }
	} else {
        // default
		scaleToFit = YES;
		expandBy = 1.25;
	}
    
	if (scaleToFit == YES) {
		[graph.defaultPlotSpace scaleToFitPlots:[graph allPlots]];
		CPTXYPlotSpace *plotSpace = (CPTXYPlotSpace*)graph.defaultPlotSpace;
		CPTPlotRange *xRange = plotSpace.xRange;
		CPTPlotRange *yRange = plotSpace.yRange;
        
		if ([xRange respondsToSelector:@selector(expandRangeByFactor:)]) {
            [xRange expandRangeByFactor:CPTDecimalFromDouble(expandBy)];
            [yRange expandRangeByFactor:CPTDecimalFromDouble(expandBy)];
            plotSpace.yRange = yRange;
            plotSpace.xRange = xRange;
		}
	}
}

-(CGPoint)viewPointFromGraphPoint:(CGPoint)point
{
    // Convert the point from the graph's coordinate system to the view. Note that the
    // graph's coordinate system has (0,0) in the lower left hand corner and the
    // view's coordinate system has (0,0) in the upper right hand corner
    CGPoint viewPoint = [self.hostingView.hostedGraph convertPoint:point toLayer:self.hostingView.layer];
    return viewPoint;
}

-(void)notifyOfTouchEvent:(NSString*)type atPoint:(CGPoint)viewPoint
{
	if ([self.proxy _hasListeners:type]) {
        NSDictionary *evt = [NSDictionary dictionaryWithObjectsAndKeys:
                             NUMFLOAT(viewPoint.x), @"x",
                             NUMFLOAT(viewPoint.y), @"y",
                             nil
                             ];
        [self.proxy fireEvent:type withObject:evt];
    }
}

-(BOOL)plotSpace:(CPTPlotSpace *)space shouldHandlePointingDeviceDownEvent:(id)event atPoint:(CGPoint)point
{
    CGPoint viewPoint = [self viewPointFromGraphPoint:point];
    [self notifyOfTouchEvent:@"touchstart" atPoint:viewPoint];
    
	return YES;
}

-(BOOL)plotSpace:(CPTPlotSpace *)space shouldHandlePointingDeviceDraggedEvent:(id)event atPoint:(CGPoint)point
{
    CGPoint viewPoint = [self viewPointFromGraphPoint:point];
    [self notifyOfTouchEvent:@"touchmove" atPoint:viewPoint];
    
	return YES;
}

-(BOOL)plotSpace:(CPTPlotSpace *)space shouldHandlePointingDeviceCancelledEvent:(id)event
{
    [self.proxy fireEvent:@"touchcancel"];
    
	return YES;
}

-(BOOL)plotSpace:(CPTPlotSpace *)space shouldHandlePointingDeviceUpEvent:(id)event atPoint:(CGPoint)point
{
    CGPoint viewPoint = [self viewPointFromGraphPoint:point];
    [self notifyOfTouchEvent:@"touchend" atPoint:viewPoint];
    
	return YES;
}

//for stop vertical scrolling

-(CGPoint)plotSpace:(CPTPlotSpace *)space willDisplaceBy:(CGPoint)displacement{
    if (panEnabled == NO)
        return CGPointZero;
    else if (clampInteraction == YES) {
        CPTPlotRange *xRange = ((CPTXYPlotSpace*)space).xRange;
        float newMinX = xRange.locationDouble - displacement.x;
        float newMaxX = newMinX + xRange.lengthDouble;
        if (newMinX < minXValue && newMaxX <= maxXValue) {
            return CGPointMake(-(minXValue - xRange.locationDouble) + 1 ,0);// +1 to make sure we saw the axis
        } else if (newMinX >= minXValue && newMaxX > maxXValue) {
            return CGPointMake(-(maxXValue - (xRange.locationDouble + xRange.lengthDouble)) -1 ,0);// -1 to make sure we saw the axis
        }
        return CGPointMake(displacement.x,0);
    }
    else
        return CGPointMake(displacement.x,0);
}

-(CPTPlotRange *)plotSpace:(CPTPlotSpace *)space willChangePlotRangeTo:(CPTPlotRange *)newRange forCoordinate:(CPTCoordinate)coordinate{
    if (coordinate == CPTCoordinateY) {
        newRange = ((CPTXYPlotSpace*)space).yRange;
    }
    return newRange;
}


-(BOOL)plotSpace:(CPTPlotSpace *)space shouldScaleBy:(CGFloat)interactionScale aboutPoint:(CGPoint)interactionPoint {
    return zoomEnabled;
}

@end
