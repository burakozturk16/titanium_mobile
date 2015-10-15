/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifdef USE_TI_UILISTVIEW
#import "TouchDelegate_Views.h"

@interface TiTableView : TDUITableView
-(BOOL)shouldHighlightCurrentListItem;
-(CGPoint) touchPoint;
-(void)processBlock:(void(^)(UITableView * tableView))block animated:(BOOL)animated;
@end
#endif
