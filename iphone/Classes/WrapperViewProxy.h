//
//  WrapperViewProxy.h
//  Titanium
//
//  Created by Martin Guillon on 22/11/2014.
//
//

#import "TiViewProxy.h"

@class TiTableView;
@interface WrapperViewProxy : TiViewProxy
- (id)initWithVerticalLayout:(BOOL)vertical tableView:(TiTableView*)tableView;
- (id)initWithVerticalLayout:(BOOL)vertical;
@property (nonatomic, readwrite, retain) TiTableView *tableView;

@end
