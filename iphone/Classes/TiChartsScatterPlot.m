//
//  TiChartsScatterPlot.m
//  Titanium
//
//  Created by Martin Guillon on 15/07/13.
//
//

#import "TiChartsScatterPlot.h"

@implementation TiChartsScatterPlot
@synthesize labelDisplacement;

-(void)positionLabelAnnotation:(CPTPlotSpaceAnnotation *)label forIndex:(NSUInteger)idx
{
    [super positionLabelAnnotation:label forIndex:idx];

    label.displacement = CGPointMake(labelDisplacement.x + label.displacement.x, labelDisplacement.y + label.displacement.y);
    
}

-(void)updateContentAnchorForLabel:(CPTPlotSpaceAnnotation *)label
{
    if ( label ) {
        switch (self.interpolation){
            case CPTScatterPlotInterpolationStepped:
            case CPTScatterPlotInterpolationLinear:
            default:
                label.contentAnchorPoint = CPTPointMake(0.5f,0.0f);
                break;
        }
    }
}

-(void)setLabelDisplacement:(CGPoint)newLabelDisplacement
{
    if (!CGPointEqualToPoint(newLabelDisplacement, labelDisplacement))
    {
        labelDisplacement = newLabelDisplacement;
        [self repositionAllLabelAnnotations];
    }
}



@end
