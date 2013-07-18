/**
 * Ti.Charts Module
 * Copyright (c) 2011-2013 by Appcelerator, Inc. All Rights Reserved.
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiChartsPieSegmentProxy.h"
#import "TiChartsParsers.h"
#import "TiUtils.h"

@implementation TiChartsPieSegmentProxy
@synthesize fill, title, value, border, labelStyle, explodeOffset;
-(void)_initWithProperties:(NSDictionary*)properties
{
	[super _initWithProperties:properties];
    self.fill = [TiChartsParsers parseFillColor:[properties objectForKey:@"fillColor"]
                              withGradient:[properties objectForKey:@"fillGradient"]
                                andOpacity:[properties objectForKey:@"fillOpacity"]
                                       def:nil];
    self.title = [TiUtils stringValue:@"title" properties:properties def:nil];
    NSDictionary* labelProps = [properties valueForKey:@"label"];
    if (labelProps) {
        self.labelStyle = [TiChartsParsers parseTextStyle:labelProps def:nil];
    }
    self.border = [TiChartsParsers parseLine:properties withPrefix:@"line" def:nil];
    self.value = [properties objectForKey:@"value"];
    self.explodeOffset = [TiUtils floatValue:@"explodeOffset" properties:properties def:0.0f];
}

-(void)dealloc
{
	RELEASE_TO_NIL(fill);
	RELEASE_TO_NIL(title);
	RELEASE_TO_NIL(value);
	[super dealloc];
}

-(void)removeFromChart:(CPTGraph*)fromGraph
{
}

-(void)renderInChart:(CPTGraph*)toGraph
{
//	[self configurePlot];
}

@end
