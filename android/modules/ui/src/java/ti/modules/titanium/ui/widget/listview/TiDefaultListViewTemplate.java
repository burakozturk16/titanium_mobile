/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget.listview;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.TiC;

public class TiDefaultListViewTemplate extends TiListViewTemplate {
	private static final KrollDict TEMPLATE_DICT = createTemplateDict();

    private static KrollDict createTemplateDict() {
    	KrollDict result = new KrollDict();
    	KrollDict imageDict = new KrollDict();
    	imageDict.put(TiC.PROPERTY_TYPE, "Ti.UI.ImageView");
    	imageDict.put(TiC.PROPERTY_BIND_ID, "imageView");
    	KrollDict props = new KrollDict();
    	props.put(TiC.PROPERTY_TOUCH_ENABLED, false);
    	props.put(TiC.PROPERTY_RIGHT, "25dp");
    	props.put(TiC.PROPERTY_WIDTH, "15%");
    	imageDict.put(TiC.PROPERTY_PROPERTIES, props);
    	
    	KrollDict labelDict = new KrollDict();
    	labelDict.put(TiC.PROPERTY_TYPE, "Ti.UI.Label");
    	labelDict.put(TiC.PROPERTY_BIND_ID, "titleView");
    	props = new KrollDict();
    	props.put(TiC.PROPERTY_TOUCH_ENABLED, false);
    	props.put(TiC.PROPERTY_LEFT, "2dp");
    	props.put(TiC.PROPERTY_WIDTH, "55%");
    	props.put(TiC.PROPERTY_TEXT, "label");
    	labelDict.put(TiC.PROPERTY_PROPERTIES, props);
    	
    	result.put(TiC.PROPERTY_CHILD_TEMPLATES, new Object[]{imageDict, labelDict});
        return result;
    }

	public TiDefaultListViewTemplate(String id) {
		super(id, TEMPLATE_DICT);
	}
	
	@Override
	public KrollDict prepareDataDict(KrollDict dict)
	{
		KrollDict result = super.prepareDataDict(dict);
		if (!result.containsKey(TiC.PROPERTY_PROPERTIES)) return dict;
		KrollDict properties = result.getKrollDict(TiC.PROPERTY_PROPERTIES);
		if (properties.containsKey(TiC.PROPERTY_TITLE) || properties.containsKey(TiC.PROPERTY_FONT) || properties.containsKey(TiC.PROPERTY_COLOR))
		{
			KrollDict labelDict = result.getKrollDict("titleView");
			if (labelDict == null)
			{
				labelDict = new KrollDict();
				result.put("titleView", labelDict);
			}
			if (properties.containsKey(TiC.PROPERTY_TITLE)) {
				labelDict.put(TiC.PROPERTY_TEXT, properties.get(TiC.PROPERTY_TITLE));
			}
			if (properties.containsKey(TiC.PROPERTY_FONT)) {
				labelDict.put(TiC.PROPERTY_FONT, properties.get(TiC.PROPERTY_FONT));
			}
			if (properties.containsKey(TiC.PROPERTY_COLOR)) {
				labelDict.put(TiC.PROPERTY_COLOR, properties.get(TiC.PROPERTY_COLOR));
			}
		}
		if (properties.containsKey(TiC.PROPERTY_IMAGE))
		{
			KrollDict imageDict = result.getKrollDict("imageView");
			if (imageDict == null)
			{
				imageDict = new KrollDict();
				result.put("imageView", imageDict);
			}
			if (properties.containsKey(TiC.PROPERTY_IMAGE)) {
				imageDict.put(TiC.PROPERTY_IMAGE, properties.get(TiC.PROPERTY_IMAGE));
			}
		}
		return result;
	}
}
