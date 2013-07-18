/**
 * Ti.Charts Module
 * Copyright (c) 2011-2013 by Appcelerator, Inc. All Rights Reserved.
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiUIView.h"

#import "CorePlot-CocoaTouch.h"

typedef enum {
	kPlotTypeBar,
    kPlotTypeLine,
	kPlotTypePie
} ChartPlotType;

@class TiChartsPlotProxy;

@interface TiChartsChart : TiUIView {

@protected
	CPTGraphHostingView	*hostingView;
	CPTGraph			*graph;
	CPTLayerAnnotation	*symbolTextAnnotation;
}
@property(nonatomic,readonly) CPTGraphHostingView* hostingView;

-(void)configurePlot;
-(void)initPlot;
-(void)configureHost;
-(void)configureGraph;
-(void)configureChart;
-(void)configureLegend;
-(void)configureTheme;

-(void)removeAllPlots;
-(void)addPlot:(id)plot;
-(void)removePlot:(id)plot;
-(void)refreshPlotSpaces;

-(CGFloat) getAvailableWidth;
-(CGFloat) getAvailableHeight;

-(void)removeAllMarkers;
-(void)addMarker:(id)marker;
-(void)removeMarker:(id)marker;

@end
