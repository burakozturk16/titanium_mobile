package ti.modules.titanium.ui.widget;

import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiConvert;

import yaochangwei.pulltorefreshlistview.widget.RefreshableListView;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


public class CustomListView extends RefreshableListView {
	private int mPosition;
	private boolean mScrollingEnabled = true;
	
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	private void init() {
		if (Build.VERSION.SDK_INT > TiC.API_LEVEL_HONEYCOMB_MR1) {
			addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
				private int top;
				private int index;
				private boolean needsPositionReset = false;
			    @Override
			    public void onViewDetachedFromWindow(View v) {
			    	index = getFirstVisiblePosition();
					View firstChild = getChildAt(0);
					if (firstChild != null) {
						top = firstChild.getTop();
						needsPositionReset = true;
					}
			    }
	
			    @Override
			    public void onViewAttachedToWindow(View v) {
			        if (needsPositionReset) {
			        	needsPositionReset = false;
			        	post(new Runnable() {
			              public void run() {
			          		setSelectionFromTop(index, top);
			              }
			          });
			        }
			    }
			});
		}
	}
	 
    public CustomListView(Context context) {
        super(context);
        init();
    }
 
    public CustomListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
 
    public CustomListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }
    
    public void setScrollingEnabled(Object value)
	{
		try {
			mScrollingEnabled = TiConvert.toBoolean(value);
		} catch (IllegalArgumentException e) {
			mScrollingEnabled = true;
		}
	}
 
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
    	if (mScrollingEnabled) return super.dispatchTouchEvent(ev);
        final int actionMasked = ev.getActionMasked() & MotionEvent.ACTION_MASK;
 
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            // Record the position the list the touch landed on
            mPosition = pointToPosition((int) ev.getX(), (int) ev.getY());
            return super.dispatchTouchEvent(ev);
        }
 
        if (actionMasked == MotionEvent.ACTION_MOVE) {
            // Ignore move events
            return true;
        }
 
        if (actionMasked == MotionEvent.ACTION_UP) {
        	
            // Check if we are still within the same view
            if (pointToPosition((int) ev.getX(), (int) ev.getY()) == mPosition) {
                super.dispatchTouchEvent(ev);
            } else {
            	ev.setAction(MotionEvent.ACTION_CANCEL); 
                return super.dispatchTouchEvent(ev);
            }
        }
 
        return super.dispatchTouchEvent(ev);
    }
}