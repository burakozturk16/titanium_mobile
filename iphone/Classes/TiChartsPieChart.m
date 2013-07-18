/**
 * Ti.Charts Module
 * Copyright (c) 2011-2013 by Appcelerator, Inc. All Rights Reserved.
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiChartsPieChart.h"
#import "TiChartsPieChartProxy.h"
#import "TiChartsParsers.h"
#import "TiChartsChartProxy.h"
#import "TiChartsPieSegmentProxy.h"


@implementation TiChartsPieChart


#pragma mark CPTPlotSpaceDelegate methods

-(CPTGraphHostingView*)hostingView
{
	if (hostingView == nil) {
		[super hostingView];
	}
	
	return hostingView;
}


-(void)dealloc
{
	RELEASE_TO_NIL(pieChart);
	[super dealloc];
}

-(CPTGraph*)newGraph
{
    return [[CPTXYGraph alloc] initWithFrame:CGRectZero];
}

-(void)initPlot
{
    [super initPlot];
    RELEASE_TO_NIL(pieChart);
    pieChart = [[CPTPieChart alloc] init];
    pieChart.dataSource = (TiChartsPieChartProxy*)self.proxy;
    pieChart.delegate = (TiChartsPieChartProxy*)self.proxy;
    [graph addPlot:pieChart];
}

-(void)configureChart
{
    [super configureChart];

    pieChart.identifier = graph.title;
    pieChart.shadow = [TiChartsParsers parseShadow:@"shadow" inProperties:self.proxy def:nil];
    pieChart.startAngle = M_PI_2 - [TiUtils floatValue:[self.proxy valueForUndefinedKey:@"startAngle"] def:0.0f] * M_PI / 180 ;
    pieChart.endAngle = M_PI_2 - [TiUtils floatValue:[self.proxy valueForUndefinedKey:@"endAngle"] def:360.0f] * M_PI / 180 ;

    pieChart.sliceDirection = CPTPieDirectionClockwise;
    
    pieChart.overlayFill = [TiChartsParsers parseFillColor:[self.proxy valueForUndefinedKey:@"overlayColor"]
                                              withGradient:[self.proxy valueForUndefinedKey:@"overlayGradient"]
                                                andOpacity:[self.proxy valueForUndefinedKey:@"overlayOpacity"]
                                                       def:nil];
	pieChart.borderLineStyle = [TiChartsParsers parseLineColor:[self.proxy valueForUndefinedKey:@"borderColor"]
                                                     withWidth:[self.proxy valueForUndefinedKey:@"borderWidth"]
                                                  withGradient:[self.proxy valueForUndefinedKey:@"borderGradient"]
                                                    andOpacity:[self.proxy valueForUndefinedKey:@"borderOpacity"]
                                                           def:nil];
}

-(void)configureGraph
{
    [super configureGraph];
    graph.axisSet = nil;
}


-(void)refreshPlotSpaces
{
    [super refreshPlotSpaces];
    pieChart.pieRadius = 0.95 * MIN([self getAvailableWidth] / 2.0, [self getAvailableHeight] / 2.0);
    CGFloat innerRadius = TiDimensionCalculateValue([TiUtils dimensionValue:[self.proxy valueForUndefinedKey:@"donutSize"]], pieChart.pieRadius);
    if (innerRadius < 0) {
        innerRadius = pieChart.pieRadius + innerRadius;
    }
    pieChart.pieInnerRadius = innerRadius;
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
@end
