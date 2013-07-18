/**
 * Ti.Charts Module
 * Copyright (c) 2011-2013 by Appcelerator, Inc. All Rights Reserved.
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiChartsPlotStepProxy.h"
#import "TiChartsChart.h"
#import "TiChartsParsers.h"
#import "TiUtils.h"

@implementation TiChartsPlotStepProxy

-(void)configurePlot
{
	[super configurePlot];
	
	CPTScatterPlot* plot = (CPTScatterPlot*)[self plot];
	plot.interpolation = CPTScatterPlotInterpolationStepped;
}

-(id)init
{
	if (self = [super init]) {
	}
	
	return self;
}

-(void)dealloc
{
	[super dealloc];
}


@end
