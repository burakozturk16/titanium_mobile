/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.tableviewseparatorstyle;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiContext;

import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.widget.TiUITableView;

@Kroll.module(parentModule=UIModule.class)
public class TableViewSeparatorStyleModule extends KrollModule {
	@Kroll.constant public static final int NONE = TiUITableView.SEPARATOR_NONE;
	@Kroll.constant public static final int SINGLE_LINE = TiUITableView.SEPARATOR_SINGLE_LINE;

	public TableViewSeparatorStyleModule()
	{
		super();
	}

	public TableViewSeparatorStyleModule(TiContext tiContext)
	{
		this();
	}
}
