/**
 * Ti.Charts Module
 * Copyright (c) 2011-2013 by Appcelerator, Inc. All Rights Reserved.
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiChartsChartProxy.h"
#import "TiChartsChart.h"
#import "TiChartsPlotProxy.h"

#import "TiChartsPieSegmentProxy.h"
#import "TiChartsMarkerAnnotation.h"

#import "TiUtils.h"

@implementation TiChartsChartProxy

-(void)_initWithProperties:(NSDictionary*)properties
{
    if ([properties isEqual:[NSNull null]]) {
        [self initializeProperty:@"fillColor" defaultValue:[properties objectForKey:@"backgroundColor"]];
        [self initializeProperty:@"fillGradient" defaultValue:[properties objectForKey:@"backgroundGradient"]];
        [self initializeProperty:@"fillOpacity" defaultValue:[properties objectForKey:@"backgroundOpacity"]];
    }
	[super _initWithProperties:properties];
}

-(void)dealloc
{
	// release any resources that have been retained by the module
	RELEASE_TO_NIL(plots);
	RELEASE_TO_NIL(markers);
	
	[super dealloc];
}

-(void)refreshPlotSpaces
{
	if ([self view]) {
		[(TiChartsChart*)[self view] refreshPlotSpaces];
	}
}

-(void)relayout:(id)args
{
    [(TiChartsChart*)[self view] refreshPlotSpaces];
}

-(NSMutableArray*)plots
{
	if (plots == nil) {
		plots = [[NSMutableArray alloc] init];
	}
	
	return plots;
}

-(NSMutableDictionary*)markers
{
	if (markers == nil) {
		markers = [[NSMutableDictionary alloc] init];
	}
	
	return markers;
}

-(void)setPlots:(id)args
{
	// If a view is currently attached to this proxy then tell it to remove all plots
	// currently shown in the graph
	if ([self view]) {
		[(TiChartsChart*)[self view] removeAllPlots];
	}
	
	// Clear the current list of plots
	RELEASE_TO_NIL(plots);
	
	// Now set the current list to this new list
	[self add:args];
}

-(void)add:(id)arg
{
	if (!IS_NULL_OR_NIL(arg)) {
		// If we get an array of plot proxy objects we can just iterate through it
		// and add each one individually. This is just a helper for adding a set of
		// plot proxies in one call.
		if ([arg isKindOfClass:[NSArray class]])
		{
			for (id a in arg) {
				[self add:a];
			}
			return;
		}
		
		// Make sure that we are getting a plot proxy object
		if (![arg isKindOfClass:[TiChartsPlotProxy class]] && ![arg isKindOfClass:[TiChartsPieSegmentProxy class]]) {
			[self throwException:@"Plot type is invalid" subreason:nil location:CODELOCATION];
		}
		
		// Only add if not already it the list
//		TiChartsPlotProxy *plot = (TiChartsPlotProxy*)arg;
		if ([[self plots] indexOfObject:arg] == NSNotFound) {
			[[self plots] addObject:arg];
            
            if ([arg isKindOfClass:[TiChartsPlotProxy class]])
                ((TiChartsPlotProxy*)arg).chartProxy = self;
            else if ([arg isKindOfClass:[TiChartsPieSegmentProxy class]])
                ((TiChartsPieSegmentProxy*)arg).chartProxy = self;
		
			// If a view is currently attached to this proxy then tell it to add this new plot
			// to the graph
			if ([self view]) {
				[(TiChartsChart*)[self view] addPlot:arg];
			}
            
            // Remember the proxy or else it will get GC'd if created by a logic variable
            [self rememberProxy:arg];
		}
		else {
			NSLog(@"[DEBUG] Attempted to add plot that is already in the plot array");
		}
	}
}

-(void)remove:(id)arg
{
	ENSURE_SINGLE_ARG(arg, TiProxy);
	
	// Make sure that we are getting a plot proxy object
    if (![arg isKindOfClass:[TiChartsPlotProxy class]] && ![arg isKindOfClass:[TiChartsPieSegmentProxy class]]) {
		[self throwException:@"Plot type is invalid" subreason:nil location:CODELOCATION];
	}
	
//	TiChartsPlotProxy *plot = (TiChartsPlotProxy*)arg;
    
    // Remove the plot from our list of plot proxy objects
	[plots removeObject:arg];
    
    // Forget the previously remembered proxy
    [self forgetProxy:arg];
	
	// If a view is currently attached to this proxy then tell it to remove the plot
	// from the graph
	if ([self view]) {
		[(TiChartsChart*)[self view] removePlot:arg];
	}
}

-(void)viewDidAttach
{
	[super viewDidAttach];
    for (id plot in plots) {
        [(TiChartsChart*)[self view] addPlot:plot];
    }
    for (NSNumber* mId in markers) {
        [(TiChartsChart*)[self view] addMarker:[markers objectForKey:mId]];
    }
}

-(void)viewWillDetach
{
    [(TiChartsChart*)[self view] removeAllPlots];
    [(TiChartsChart*)[self view] removeAllMarkers];
	[super viewWillDetach];
}

#ifndef USE_VIEW_FOR_UI_METHOD
	#define USE_VIEW_FOR_UI_METHOD(methodname)\
	-(void)methodname:(id)args\
	{\
		[self makeViewPerformSelector:@selector(methodname:) withObject:args createIfNeeded:YES waitUntilDone:NO];\
	}
#endif
USE_VIEW_FOR_UI_METHOD(refresh);

-(void)addMarker:(id)arg
{
    ENSURE_SINGLE_ARG(arg, NSDictionary)
    int currentMarkerId = markerId;
    markerId ++;
    
    TiChartsMarkerAnnotation* marker = [[TiChartsMarkerAnnotation alloc] initWithProperties:arg];
    [[self markers] setObject:marker forKey:[NSNumber numberWithInt:markerId]];
    if ([self view]) {
        [(TiChartsChart*)[self view] addMarker:marker];
    }
}

-(void)removeMarker:(id)arg
{
    ENSURE_SINGLE_ARG(arg, NSNumber)
    NSNumber* currentMarkerId = (NSNumber*)arg;
    
    TiChartsMarkerAnnotation* marker = [[self markers] objectForKey:currentMarkerId];
    if (marker != nil) {
        if ([self view]) {
            [(TiChartsChart*)[self view] removeMarker:marker];
        }
        [markers removeObjectForKey:currentMarkerId];
    }
}

@end
