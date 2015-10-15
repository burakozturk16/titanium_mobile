package ti.modules.titanium.ui.widget.abslistview;

import java.lang.ref.WeakReference;

import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.view.TiCompositeLayout;

import se.emilsjolander.stickylistheaders.StickyListHeadersListViewAbstract;
import ti.modules.titanium.ui.widget.abslistview.AbsListSectionProxy.AbsListItemData;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class TiBaseAbsListViewItemHolder extends TiCompositeLayout {
    private WeakReference<StickyListHeadersListViewAbstract> listView = null;
    private WeakReference<AbsListItemData> itemData = null;
	private boolean hasHeightRelyingOnPercent = false;

	public TiBaseAbsListViewItemHolder(Context context) {
		super(context, LayoutArrangement.HORIZONTAL, null);
		setClipChildren(false);
		addView(new TiBaseAbsListViewItem(context));
		
	    final float scale = getResources().getDisplayMetrics().density;
		TiCompositeLayout.LayoutParams p = new TiCompositeLayout.LayoutParams();
        p.autoFillsHeight = true;
        p.sizeOrFillHeightEnabled = true;
        p.sizeOrFillWidthEnabled = true;
        p.autoFillsWidth = false;
        p.optionLeft = new TiDimension(5, TiDimension.TYPE_LEFT);
        p.optionRight = new TiDimension(5, TiDimension.TYPE_RIGHT);
		ImageView imageView = new ImageView(context);
		imageView.setId(TiAbsListView.accessory);
        imageView.setFocusable(false);
        imageView.setAdjustViewBounds(true);
        imageView.setMaxWidth((int) (25 * scale));
        imageView.setMaxHeight((int) (25 * scale));
        imageView.setScaleType(ScaleType.CENTER_INSIDE);
        imageView.setVisibility(GONE);
        addView(imageView, p);
	}

	public TiBaseAbsListViewItemHolder(Context context, AttributeSet set) {
        this(context);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	    if (hasHeightRelyingOnPercent) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(listView.get().getMeasuredHeight(), MeasureSpec.AT_MOST);
	    }
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	public AbsListItemData getItemData() {
	    if (itemData != null) {
	        return itemData.get();
	    }
	    return null;
	}

    public void setItem(TiBaseAbsListViewItem item, 
            AbsListItemData itemData,
            StickyListHeadersListViewAbstract listView) {
        hasHeightRelyingOnPercent = false;
        if (itemData != null) {
            this.itemData = new WeakReference<AbsListItemData>(itemData);
        } else {
            this.itemData = null;
        }
        if (listView != null) {
            this.listView = new WeakReference<StickyListHeadersListViewAbstract>(listView);
            if (item != null) {
              ViewGroup.LayoutParams params = item.getTiLayoutParams();
              if (params instanceof TiCompositeLayout.LayoutParams && !((TiCompositeLayout.LayoutParams) params).fixedSizeHeight()) {
                  hasHeightRelyingOnPercent = true;
              }
          }
        } else {
            this.listView = null;
        }
    }
}
