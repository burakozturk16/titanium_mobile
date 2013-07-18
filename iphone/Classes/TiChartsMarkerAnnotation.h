//
//  TiChartsMarkerAnnotation.h
//  Titanium
//
//  Created by Martin Guillon on 11/07/13.
//
//

#import "CorePlot-CocoaTouch.h"
#import "TiChartsLineLayer.h"

@interface TiChartsMarkerAnnotation : CPTPlotSpaceAnnotation
{
@private
//    CPTPlotSpaceAnnotation* layer;

}


-(id)initWithProperties:(NSDictionary*)props;
-(void) setGraph:(CPTGraph*)graph;
@end
