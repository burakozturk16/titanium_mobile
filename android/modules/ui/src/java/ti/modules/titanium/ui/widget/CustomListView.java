package ti.modules.titanium.ui.widget;

import org.appcelerator.titanium.util.TiConvert;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;


public class CustomListView extends ListView {
	private int mPosition;
	private boolean mScrollingEnabled = true;
	 
    public CustomListView(Context context) {
        super(context);
    }
 
    public CustomListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
 
    public CustomListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
