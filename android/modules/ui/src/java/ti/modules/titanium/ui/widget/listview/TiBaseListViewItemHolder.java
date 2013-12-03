package ti.modules.titanium.ui.widget.listview;

import org.appcelerator.titanium.view.TiCompositeLayout;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public class TiBaseListViewItemHolder extends TiCompositeLayout {
	private ListView listView = null;

	public TiBaseListViewItemHolder(Context context) {
		super(context);
	}

	public TiBaseListViewItemHolder(Context context, AttributeSet set) {
		super(context, set);
	}
	public void setListView(ListView listView_) {
		listView = listView_;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (listView != null)
			super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(listView.getMeasuredHeight(), MeasureSpec.AT_MOST));
		else 
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);

	}
}
