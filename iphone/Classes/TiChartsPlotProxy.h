/**
 * Ti.Charts Module
 * Copyright (c) 2011-2013 by Appcelerator, Inc. All Rights Reserved.
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiProxy.h"

#import "CorePlot-CocoaTouch.h"

@class TiChartsChartProxy;

@interface TiChartsPlotProxy : TiProxy <TiProxyDelegate, CPTPlotDataSource> {
	
@private
	CPTPlot				*plot;
	NSMutableArray		*dataX;
	NSMutableArray		*dataY;
	NSString			*dataKey;
	int					dirtyDataFlags;
	NSSet				*propertyChangedProperties;
	TiChartsChartProxy	*chartProxy;
    float               minXValue;
    float               maxXValue;
    float               minYValue;
    float               maxYValue;
}

@property(nonatomic,readwrite,retain) NSString* dataKey;
@property(nonatomic,readwrite,retain) NSSet* propertyChangedProperties;
@property(nonatomic,readwrite,retain) CPTPlot* plot;
@property(nonatomic,assign) TiChartsChartProxy* chartProxy;
@property(nonatomic, readonly) float minXValue;
@property(nonatomic, readonly) float maxXValue;
@property(nonatomic, readonly) float minYValue;
@property(nonatomic, readonly) float maxYValue;

-(CPTPlot*)allocPlot;
-(void)configurePlot;
-(void)renderInChart:(CPTGraph*)graph;
-(void)removeFromChart:(CPTGraph*)graph;
-(NSNumber*)numberForPlot:(NSUInteger)index;
-(NSNumber*)numberForPlot:(NSUInteger)index forCoordinate:(CPTCoordinate)coordinate;
-(NSArray*)numbersForPlotRange:(NSRange)indexRange forCoordinate:(CPTCoordinate)coordinate;
-(NSUInteger)numberOfRecordsForPlot:(CPTPlot *)plot;
-(void)refreshData;
-(void)notifyOfDataClickedEvent:(NSUInteger)index;
-(void)notifyOfDataClickedEvent:(NSUInteger)index atPlotPoint:(CGPoint)plotPoint;
-(CGPoint)viewPointFromGraphPoint:(CGPoint)point;

-(NSArray*)dataX;
-(NSArray*)dataY;


@end

