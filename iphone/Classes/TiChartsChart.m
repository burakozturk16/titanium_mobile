/**
 * Ti.Charts Module
 * Copyright (c) 2011-2013 by Appcelerator, Inc. All Rights Reserved.
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiChartsChart.h"
#import "TiChartsParsers.h"
#import "TiUtils.h"
#import "TiBase.h"
#import "TiChartsChartProxy.h"
#import "TiChartsPlotProxy.h"
#import "TiChartsMarkerAnnotation.h"

@implementation TiChartsChart

@synthesize hostingView;

-(void)killGraph
{
	if (hostingView) {
		if (symbolTextAnnotation) {
			[graph.plotAreaFrame.plotArea removeAnnotation:symbolTextAnnotation];
			RELEASE_TO_NIL(symbolTextAnnotation);
		}
		
		[hostingView removeFromSuperview];
		hostingView.hostedGraph = nil;
		RELEASE_TO_NIL(hostingView);
	}
	RELEASE_TO_NIL(graph);
}

-(void)dealloc
{
	[self killGraph];
	[super dealloc];
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
	[super frameSizeChanged:frame bounds:bounds];
	if (hostingView != nil) {
		[TiUtils setView:hostingView positionRect:bounds];
	}
	[self refreshPlotSpaces];
}

-(void)configureTitle:(NSDictionary*)properties
{
	// NOTE: For some reason, directly setting the text style properties
	// using graph.titleTextStyle.xxxxxx does not work properly. It works
	// best by creating a new textStyle object, setting the properties of
	// this object, and then assigning it to the graph.titleTextStyle property
	
	// Configure the font name and size and color
	graph.titleTextStyle  = [TiChartsParsers parseTextStyle:properties def:graph.titleTextStyle];
	
	// The frame anchor defines the location of the title
	graph.titlePlotAreaFrameAnchor = [TiUtils intValue:@"location" properties:properties def:CPTRectAnchorTop];
	
	// The displacement defines the offset from the specified edge
	NSDictionary* offset = [properties objectForKey:@"offset"];
	if (offset) {
		graph.titleDisplacement = CGPointMake(
		  [TiUtils floatValue:@"x" properties:offset def:0.0],
		  [TiUtils floatValue:@"y" properties:offset def:0.0]);
	} else if (graph.title == nil) {
		graph.titleDisplacement = CGPointZero;
	} else {
		graph.titleDisplacement = CGPointMake(0.0f, graph.titleTextStyle.fontSize);
	}
	
	// Set the title after setting the font. For some reason, core-plot will crash on
	// the iPad simulator if the title is set before the font.
	graph.title = [TiUtils stringValue:@"text" properties:properties def:nil];
}
	
-(void)configurePadding:(NSDictionary*)properties
{	
	graph.paddingLeft = [TiUtils floatValue:@"left" properties:properties def:0];
	graph.paddingTop = [TiUtils floatValue:@"top" properties:properties def: 0];
	graph.paddingRight = [TiUtils floatValue:@"right" properties:properties def:0];
	graph.paddingBottom = [TiUtils floatValue:@"bottom" properties:properties def:0];
}

-(void)configureTheme:(NSString*)themeName
{
	if (themeName != nil) {
		CPTTheme *theme = [CPTTheme themeNamed:themeName];
		if (theme != nil) {
			[graph applyTheme:theme];
			return;
		}
	}
	
	// Apply the default theme -- this also sets up default values for
	// a number of parameters of the graph.
//	[graph applyTheme:[CPTTheme themeNamed:kCPTDarkGradientTheme]];
}

-(void)configurePlotArea:(NSDictionary*)properties
{
    // Border
	graph.plotAreaFrame.borderLineStyle = [TiChartsParsers parseLineColor:[properties objectForKey:@"borderColor"]
																withWidth:[properties objectForKey:@"borderWidth"]
                                                             withGradient:[properties objectForKey:@"borderGradient"]
															   andOpacity:[properties objectForKey:@"borderOpacity"]
																	  def:nil];
    graph.plotAreaFrame.cornerRadius = [TiUtils floatValue:@"borderRadius" properties:properties def:0];
	
	// Inner padding
	NSDictionary *padding = [properties objectForKey:@"padding"];
	if (padding != nil) {
		graph.plotAreaFrame.paddingLeft = [TiUtils floatValue:@"left" properties:padding def:0];
		graph.plotAreaFrame.paddingTop = [TiUtils floatValue:@"top" properties:padding def:0];
		graph.plotAreaFrame.paddingRight = [TiUtils floatValue:@"right" properties:padding def:0];
		graph.plotAreaFrame.paddingBottom = [TiUtils floatValue:@"bottom" properties:padding def:0];
	}
	
	// Plot area frame fill
	graph.plotAreaFrame.fill = [TiChartsParsers parseFillColor:[properties objectForKey:@"backgroundColor"]
												  withGradient:[properties objectForKey:@"backgroundGradient"]
													andOpacity:[properties objectForKey:@"backgroundOpacity"]
														   def:nil];
    
    // Plot area fill
    graph.plotAreaFrame.plotArea.fill = [TiChartsParsers parseFillColor:[properties objectForKey:@"fillColor"]
                                                           withGradient:[properties objectForKey:@"fillGradient"]
                                                             andOpacity:[properties objectForKey:@"fillOpacity"]
                                                                    def:nil];
}

-(void)removeAllPlots
{
	if (graph != nil) {
		for (id plot in ((TiChartsChartProxy*)self.proxy).plots) {
			[plot removeFromChart:graph];
		}
	}
}


-(void)addPlot:(id)plot
{
	if (graph != nil && [plot respondsToSelector:@selector(renderInChart:)]) {
		[plot renderInChart:graph];
	}
}

-(void)removePlot:(id)plot
{
    if (graph != nil && [plot respondsToSelector:@selector(removeFromChart:)]) {
		[plot removeFromChart:graph];
	}
}

-(void)configurationSet
{
    [super configurationSet];
    [self initPlot];
    [self configurePlot];
}

-(void)refreshPlotSpaces
{
//    for (TiChartsMarkerAnnotation* marker in ((TiChartsChartProxy*)self.proxy).markers) {
//        CPTAnnotation *annot = marker.layer;
//        annot.contentLayer/.frame = CGRectMake(newXCoord, newYCoord, logoWidth, logoHeight);
//    }
}

-(CPTGraph*)newGraph
{
    return [[CPTGraph alloc] initWithFrame:CGRectZero];
}

#pragma mark - Chart behavior
-(void)initPlot {
    hostingView = [[CPTGraphHostingView alloc] initWithFrame:[self bounds]];
    [self addSubview:hostingView];
    
    // Create graph object
    graph = [self newGraph];
    hostingView.hostedGraph = graph;
    hostingView.collapsesLayers = NO; // Setting to YES reduces GPU memory usage, but can slow drawing/scrolling
}

-(void)configurePlot {
    [self configureHost];
    [self configureGraph];
    [self configureChart];
    [self configureLegend];
    [self configureTheme];
}

-(void)configureHost {
}

-(void)configureGraph {
    [graph setTopDownLayerOrder:[NSArray arrayWithObjects:
                                 [NSNumber numberWithInt:CPTGraphLayerTypeAxisTitles],
                                 [NSNumber numberWithInt:CPTGraphLayerTypeAxisLabels],
                                 [NSNumber numberWithInt:CPTGraphLayerTypeAxisLines],
                                 [NSNumber numberWithInt:CPTGraphLayerTypeAnnotations],
                                 [NSNumber numberWithInt:CPTGraphLayerTypePlots],
                                 [NSNumber numberWithInt:CPTGraphLayerTypeMajorGridLines],
                                 [NSNumber numberWithInt:CPTGraphLayerTypeMinorGridLines],
                                 nil]];
    
    
    // Background fill
    graph.fill = [TiChartsParsers parseFillColor:[self.proxy valueForUndefinedKey:@"fillColor"]
                                    withGradient:[self.proxy valueForUndefinedKey:@"fillGradient"]
                                      andOpacity:[self.proxy valueForUndefinedKey:@"fillOpacity"]
                                             def:nil];
    // Configure the graph title area
	[self configureTitle:[self.proxy valueForUndefinedKey:@"title"]];
	
	// Configure the padding on the outside of the graph
	[self configurePadding:[self.proxy valueForUndefinedKey:@"padding"]];
    
    // Configure the frame and inside padding for the graph
	[self configurePlotArea:[self.proxy valueForUndefinedKey:@"plotArea"]];
}

-(void)configureChart {
}

-(void)configureLegend {
}

-(void)configureTheme {
    // Configure theme first -- it may set default options for the
	// entire graph, which would override any other settings if we don't
	// process it first
	[self configureTheme:[self.proxy valueForUndefinedKey:@"theme"]];
}


-(void)refresh:(id)args
{
	[[self hostingView] setNeedsDisplay];
}

- (void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation
{
	// On rotation, re-render the view
	[self refreshPlotSpaces];
}


-(CGFloat) getAvailableWidth {
    return hostingView.frame.size.width - graph.plotAreaFrame.paddingLeft - graph.plotAreaFrame.paddingRight - graph.paddingLeft - graph.paddingRight;
}

-(CGFloat) getAvailableHeight {
    return hostingView.frame.size.height - graph.paddingTop - graph.paddingBottom - graph.plotAreaFrame.paddingTop - graph.plotAreaFrame.paddingBottom;
}

-(void)removeAllMarkers
{
	if (graph != nil) {
		for (TiChartsMarkerAnnotation* annotation in ((TiChartsChartProxy*)self.proxy).markers) {
			[graph removeAnnotation:annotation];
		}
	}
}


-(void)addMarker:(TiChartsMarkerAnnotation*)marker
{
	if (graph != nil) {
        [marker setGraph:graph];
		[graph.plotAreaFrame.plotArea addAnnotation:marker];
	}
}

-(void)removeMarker:(TiChartsMarkerAnnotation*)marker
{
    if (graph != nil) {
		[graph.plotAreaFrame.plotArea removeAnnotation:marker];
	}
}


@end
