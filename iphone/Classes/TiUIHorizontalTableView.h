/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */
#ifdef USE_TI_UITABLEVIEW

#import "TiUITableView.h"

@class TiUIHorizontalContentView;
// Overloads hilighting to send touchbegin/touchend events
@interface TiUIHorizontalTableViewCell: TiUITableViewCell
{
@private
    TiUIHorizontalContentView* customContentview;
}
@end

@interface TiUIHorizontalTableView: TiUITableView {
@private
    TiDimension rowWidth;
	TiDimension minRowWidth;
	TiDimension maxRowWidth;
}

@end

#endif