/**
 * Ti.Charts Module
 * Copyright (c) 2011-2013 by Appcelerator, Inc. All Rights Reserved.
 * Please see the LICENSE included with this distribution for details.
 */

#import <Foundation/Foundation.h>
#import "KrollCallback.h"

@interface TiChartsLabelFormatter : NSNumberFormatter {
@private
    NSDictionary* customLabels;
    NSSet* tickLocations;
    KrollCallback* callback;
}

-(id)initWithArray:(NSArray*)values;
-(id)initWithCallback:(KrollCallback*)callback;
@property(nonatomic,readonly) NSSet* tickLocations;
@end
