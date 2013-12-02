package ti.modules.titanium.ui.widget.listview;

import org.appcelerator.titanium.view.TiCompositeLayout;

import android.content.Context;
import android.util.AttributeSet;

public class TiBaseListViewItemHolder extends TiCompositeLayout {
	private int listViewHeight = -1;

	public TiBaseListViewItemHolder(Context context) {
		super(context);
	}

	public TiBaseListViewItemHolder(Context context, AttributeSet set) {
		super(context, set);
	}
	public void setListViewHeight(int value) {
		listViewHeight = value;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(listViewHeight, MeasureSpec.AT_MOST));
	}
}
