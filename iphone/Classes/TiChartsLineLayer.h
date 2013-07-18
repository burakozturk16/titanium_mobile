#import "CPTAnnotationHostLayer.h"
#import "CPTLineStyle.h"

/// @file

/**
 *  @brief Enumeration of line drawing directions.
 **/
typedef enum TiChartsLineDirection {
    CPTLineDirectionHorizontal,       ///< Horizontal Line.
    CPTLineDirectionVertical ///< Vertical Line.
}
TiChartsLineDirection;

@interface TiChartsLineLayer : CPTLayer {
@protected
    CPTLineStyle *lineStyle;
    TiChartsLineDirection direction;
    CPTLayer* parentLayer;
}

@property (readwrite, retain, nonatomic) CPTLineStyle *lineStyle;
@property (readwrite, retain, nonatomic) CPTLayer *parentLayer;
@property (readwrite, assign, nonatomic) TiChartsLineDirection direction;

/// @name Initialization
/// @{
-(id)initWithDirection:(TiChartsLineDirection)direction;
-(id)initWithDirection:(TiChartsLineDirection)direction style:(CPTLineStyle *)newStyle;
/// @}

/// @name Layout
/// @{
-(CGSize)sizeThatFits;
-(void)sizeToFit;
/// @}

@end
