package ti.modules.titanium.ui.widget.listview;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.ViewProxy;
import ti.modules.titanium.ui.widget.listview.TiListView.TiBaseAdapter;
import ti.modules.titanium.ui.widget.listview.TiTemplate.DataItem;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;

@Kroll.proxy(creatableInModule = UIModule.class, propertyAccessors = {
	TiC.PROPERTY_HEADER_TITLE
})
public class ListSectionProxy extends ViewProxy{

	private static final String TAG = "SectionProxy";
	private ArrayList<KrollDict> entryProperties;
	private SparseArray<TiTemplate> templatesByIndex;
	private int itemCount;
	private DefaultTemplate builtInTemplate;
	private TiBaseAdapter adapter;
	
	private WeakReference<TiListView> listView;
	
	private static final int MSG_FIRST_ID = TiViewProxy.MSG_LAST_ID + 1;

	private static final int MSG_SET_ITEM = MSG_FIRST_ID + 700;
	
	public ListSectionProxy () {
		//initialize variables
		entryProperties = new ArrayList<KrollDict>();
		templatesByIndex = new SparseArray<TiTemplate>();
		itemCount = 0;
	}
	
	public void setAdapter(TiBaseAdapter a) {
		adapter = a;
	}

	@Override
	public boolean handleMessage(Message msg) 
	{
		
		switch (msg.what) {

		case MSG_SET_ITEM: {
			handleSetItem(msg.obj);
		}

		default : {
			return super.handleMessage(msg);
		}

		}

	}
	
	@Kroll.method
	public void setItem(Object data) {
		if (TiApplication.isUIThread()) {
			handleSetItem(data);
		} else {
			Handler handler = getMainHandler();
			handler.sendMessage(handler.obtainMessage(MSG_SET_ITEM, data));
		}
		
	}
	
	private void handleSetItem(Object data) {

		if (data instanceof Object[]) {
			Object[] views = (Object[]) data;
			int count = views.length;
			itemCount += count;

			//First pass through data, we process template and update
			//default properties based data given
			for (int i = 0; i < count; i++) {
				Object itemData = views[i];
				if (itemData instanceof HashMap) {
					KrollDict d = new KrollDict((HashMap)itemData);
					TiTemplate template = processTemplate(d, i);
					template.updateDefaultProperties(d);
				}
			}
			//Second pass we would merge properties
			for (int i = 0; i < count; i++) {
				Object itemData = views[i];
				if (itemData instanceof HashMap) {
					KrollDict d = new KrollDict((HashMap)itemData);
					TiTemplate template = templatesByIndex.get(i);
					if (template != null) {
						template.mergeWithDefaultProperties(d);
					}
					d.remove(TiC.PROPERTY_TEMPLATE);
					entryProperties.add(d);
				}
			}
			//Notify adapter that data has changed.
			if (adapter != null) {
				//adapter.notifyDataSetChanged();
			}
		} else {
			Log.e(TAG, "Invalid argument type to setData");
		}
	}
	
	private TiTemplate processTemplate(KrollDict itemData, int index) {
		
		TiListView listView = getListView();
		String defaultTemplateBinding = null;
		if (listView != null) {
			defaultTemplateBinding = listView.getDefaultTemplateBinding();
		}
		//if template is specified in data, we look it up and if one exists, we use it.
		//Otherwise we check if a default template is specified in ListView. If not, we use builtInTemplate.
		if (itemData.containsKey(TiC.PROPERTY_TEMPLATE)) {
			//retrieve template
			String binding = TiConvert.toString(itemData.get(TiC.PROPERTY_TEMPLATE));
			TiTemplate template = listView.getTemplateByBinding(binding);
			//if template is successfully retrieved, bind it to the index. This is b/c
			//each row can have a different template.
			if (template != null) {
				templatesByIndex.put(index, template);
			} else {
				Log.e(TAG, "Template undefined");
			}
						
			return template;
			
		} else {
			//if a valid default template is specify, use that one
			if (defaultTemplateBinding != null && !defaultTemplateBinding.equals(UIModule.LIST_ITEM_TEMPLATE_DEFAULT)) {
				TiTemplate defTemplate = listView.getTemplateByBinding(defaultTemplateBinding);
				if (defTemplate != null) {
					templatesByIndex.put(index, defTemplate);
					return defTemplate;
				}
			} else if (builtInTemplate != null){
				templatesByIndex.put(index, builtInTemplate);
			} else {
				//Create template and generate default properties
				builtInTemplate = new DefaultTemplate(UIModule.LIST_ITEM_TEMPLATE_DEFAULT, null);
				builtInTemplate.generateDefaultProps(getActivity());
				//Each template is treated as an item type, so we can reuse views efficiently.
				//Section templates are given a type in TiListView.processSections(). Here we
				//give default template a type if possible.
				if (listView != null) {
					builtInTemplate.setType(listView.getItemType());
				}
				templatesByIndex.put(index, builtInTemplate);
			}
			
			return builtInTemplate;
		}	
		
	}

	/**
	 * This method creates a new cell and fill it with content. getView() calls this method
	 * when a view needs to be created.
	 * @param index Entry's index relative to its section
	 * @return
	 */
	public TiBaseListViewItem generateCellContent(int index, KrollDict data, TiTemplate template) {
		//Here we create an item content and populate it with data
		//Get item proxy
		TiViewProxy itemProxy = template.getRootItem().getViewProxy();
		//Create corresponding TiUIView for item proxy
		TiListItem item = new TiListItem(itemProxy);	
		
		//Create native view for for TiUIView and set it
		TiBaseListViewItem itemContent = new TiBaseListViewItem(getActivity());
		item.setNativeView(itemContent);
		
		//Connect native view with TiUIView so we can get it from recycled view.
		itemContent.setTag(item);
	
		if (data != null && template != null) {
			generateChildContentViews(template.getRootItem(), null, itemContent, true);
			populateViews(data, itemContent, template);
		}
		return itemContent;
	}
	
	
	public void generateChildContentViews(DataItem item, TiUIView parentContent, TiBaseListViewItem rootItem, boolean root) {

		ArrayList<DataItem> childrenItem = item.getChildren();
		for (int i = 0; i < childrenItem.size(); i++) {
			DataItem child = childrenItem.get(i);
			TiViewProxy proxy = child.getViewProxy();
			TiUIView view = proxy.createView(proxy.getActivity());
			generateChildContentViews(child, view, rootItem, false);
			//Bind view to root.
			rootItem.bindView(child.getBindingId(), view);
			//Process default properties
			view.processProperties(child.getDefaultProperties());
			//Add it to view hierarchy
			if (root) {
				rootItem.addView(view.getNativeView(), view.getLayoutParams());
			} else {
				parentContent.add(view);
			}
		}
	}
	
	public void populateViews(KrollDict data, TiBaseListViewItem cellContent, TiTemplate template) {
		Object cell = cellContent.getTag();
		if (cell instanceof TiUIView) {
			((TiUIView) cell).processProperties(template.getRootItem().getDefaultProperties());
		}
		HashMap<String, TiUIView> views = cellContent.getViewsMap();
		//Loop through all our views and apply default properties
		for (String binding: views.keySet()) {
			//if view doesn't have binding, we don't need to re-apply properties since
			//we know users can't change any properties. If data contains view, we don't
			//need to apply default properties b/c data properties is merged with default
			//properties and we handle it when we loop through data.
			if (binding.startsWith(TiTemplate.GENERATED_BINDING) ||
				data.containsKey(binding)) continue;
			
			DataItem dataItem = template.getDataItem(binding);
			TiUIView view = views.get(binding);
			if (dataItem != null && dataItem != null) {
				view.processProperties(dataItem.getDefaultProperties());
			}
			
			
		}

		for (String key : data.keySet()) {
			KrollDict properties = new KrollDict((HashMap)data.get(key));
			
			if (key.equals(template.getItemID()) && cell instanceof TiUIView) {
				((TiUIView) cell).processProperties(template.getRootItem().getDefaultProperties());
				continue;
			}

			TiUIView view = cellContent.getViewFromBinding(key);
			if (view != null) {
				view.processProperties(properties);
			}
		}
	}
	
	public void createChildView(TiViewProxy proxy, String binding, KrollDict properties, TiBaseListViewItem viewGroup) {

		TiUIView childView = proxy.createView(proxy.getActivity());
		
		if (childView != null) {
			childView.processProperties(properties);
			viewGroup.addView(childView.getNativeView(), childView.getLayoutParams());
			viewGroup.bindView(binding, childView);
		}
	}
	
	public TiTemplate getTemplateByIndex(int index) {
		return templatesByIndex.get(index);
	}
	
	/**
	 * @return number of entries within section
	 */
	public int getItemCount() {
		return itemCount;
	}
	
	public void setListView(TiListView l) {
		listView = new WeakReference<TiListView>(l);
	}
	
	public TiListView getListView() {
		if (listView != null) {
			return listView.get();
		}
		return null;
	}
	
	/**
	 * Attempt to give each existing template a type, if possible
	 */
	public void setTemplateType() {
		
		for (int i = 0; i < templatesByIndex.size(); i++) {
			TiTemplate temp = templatesByIndex.get(i);
			if (temp.getType() == -1) {
				temp.setType(getListView().getItemType());
			}
		}
	}
	
	public KrollDict getEntryProperties(int position) {
		if (position < entryProperties.size()) {
			return entryProperties.get(position);
		} 
		return null;
	}
	
}
