/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */
#ifdef USE_TI_UITABLEVIEW

#import "TiUIHorizontalTableView.h"
#import "TiUIHorizontalTableViewProxy.h"

@interface TiUIHorizontalContentView : UIView
{
}
@end

@implementation TiUIHorizontalContentView

@end

@interface TiUITableView ()
-(CGFloat)computeRowWidth;
-(TiUITableViewRowProxy*)rowForIndexPath:(NSIndexPath*)indexPath;
@end

@interface TiUITableViewRowProxy ()
-(id)height;
@end

@interface TiUIHorizontalTableView ()
-(CGFloat)rowWidth:(CGFloat)height forProxy:(TiUITableViewRowProxy*)proxy;
@end


@implementation TiUIHorizontalTableViewCell

-(id)initWithStyle:(UITableViewCellStyle)style_ reuseIdentifier:(NSString *)reuseIdentifier_ row:(TiUITableViewRowProxy *)row_
{
	if (self = [super initWithStyle:style_ reuseIdentifier:reuseIdentifier_]) {
        self.layer.transform = CATransform3DRotate(CATransform3DIdentity,1.57079633,0,0,1);
	}
	
	return self;
}

-(CGSize)computeCellSize
{
    //in ourheighttransform tableview the width here is actually height
    CGFloat height = 0;
    if ([proxy table] != nil) {
        height = [proxy sizeWidthForDecorations:[[proxy table] computeRowWidth] forceResizing:YES];
    }
	CGFloat width = [(TiUIHorizontalTableView*)[proxy table] rowWidth:height forProxy:proxy];
	width = [[proxy table] tableRowHeight:width];
    
    // If there is a separator, then it's included as part of the row height as the system, so remove the pixel for it
    // from our cell size
    if ([[[proxy table] tableView] separatorStyle] == UITableViewCellSeparatorStyleSingleLine) {
        height -= 1;
    }
    
    return CGSizeMake(width, height);
}



@end

@implementation TiUIHorizontalTableView

-(CGFloat)rowWidth:(CGFloat)height forProxy:(TiUITableViewRowProxy*)rowproxy
{
    TiDimension width = [TiUtils dimensionValue:[rowproxy height]];
	if (TiDimensionIsDip(width))
	{
		return width.value;
	}
	CGFloat result = 0;
    CGFloat tableviewWidth = self.bounds.size.height;
	if (TiDimensionIsAuto(width) || TiDimensionIsAutoSize(width) || TiDimensionIsUndefined(width))
	{
		result = [rowproxy minimumParentSizeForSize:CGSizeMake(0, height)].width;
	}
    if (TiDimensionIsPercent(width)) {
        result = TiDimensionCalculateValue(width, tableviewWidth);
    }
	return (result == 0) ? [self tableRowHeight:0] : result;
}


- (CGFloat)tableView:(UITableView *)ourTableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
	NSIndexPath* index = indexPath;
	if (ourTableView != tableview) {
		index = [self indexPathFromSearchIndex:[indexPath row]];
	}
	
	TiUITableViewRowProxy *row = [super rowForIndexPath:index];
    
    
	CGFloat height = [row sizeWidthForDecorations:[super computeRowWidth] forceResizing:YES];
	CGFloat width = [self rowWidth:height forProxy:row];
	width = [self tableRowHeight:width];
	return width < 1 ? tableview.rowHeight : width;
}

-(void)keyboardDidShowAtHeight:(CGFloat)keyboardTop
{
//	int lastSectionIndex = [(TiUITableViewProxy *)[self proxy] sectionCount]-1;
//	ENSURE_CONSISTENCY(lastSectionIndex>=0);
//	CGRect minimumContentRect = [tableview rectForSection:lastSectionIndex];
//	InsetScrollViewForKeyboard(tableview,keyboardTop,minimumContentRect.size.height + minimumContentRect.origin.y);
}

-(TDUITableView*)tableView
{
    tableview = [super tableView];
    self.layer.transform = CATransform3DRotate(CATransform3DIdentity,-1.57079633,0,0,1);
	return tableview;
}


-(void)setFrame:(CGRect)frame
{
	[super setFrame:CGRectMake(frame.origin.x, frame.origin.y, frame.size.height, frame.size.width)];
}

-(void)setBounds:(CGRect)bounds
{
	[super setBounds:CGRectMake(bounds.origin.x, bounds.origin.y, bounds.size.height, bounds.size.width)];
}

-(Class)cellClass
{
    return [TiUIHorizontalTableViewCell class];
}

@end

#endif
