package ti.modules.titanium.ui.widget;

import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import yaochangwei.pulltorefreshlistview.widget.RefreshableListView;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class CustomListView extends RefreshableListView {
	private int mPosition;
	private boolean mScrollingEnabled = true;
	private static boolean HONEYCOMB_MR1_OR_HIGHER = Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB_MR1;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	private void init(final Context context) {
		if (HONEYCOMB_MR1_OR_HIGHER) {
			addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
				private int top;
				private int index;
				private boolean needsPositionReset = false;

				@Override
				public void onViewDetachedFromWindow(View v) {
					index = getFirstVisiblePosition();
					View firstChild = getListChildAt(0);
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
	}

	public CustomListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CustomListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public void setScrollingEnabled(Object value) {
		try {
			mScrollingEnabled = TiConvert.toBoolean(value);
		} catch (IllegalArgumentException e) {
			mScrollingEnabled = true;
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (mScrollingEnabled)
			return super.dispatchTouchEvent(ev);
		final int actionMasked = ev.getActionMasked() & MotionEvent.ACTION_MASK;

		if (actionMasked == MotionEvent.ACTION_DOWN) {
			// Record the position the list the touch landed on
			mPosition = getWrappedList().pointToPosition((int) ev.getX(), (int) ev.getY());
			return super.dispatchTouchEvent(ev);
		}

		if (actionMasked == MotionEvent.ACTION_MOVE) {
			// Ignore move events
			return true;
		}

		if (actionMasked == MotionEvent.ACTION_UP) {

			// Check if we are still within the same view
			if (getWrappedList().pointToPosition((int) ev.getX(), (int) ev.getY()) == mPosition) {
				super.dispatchTouchEvent(ev);
			} else {
				ev.setAction(MotionEvent.ACTION_CANCEL);
				return super.dispatchTouchEvent(ev);
			}
		}

		return super.dispatchTouchEvent(ev);
	}
	
	@Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        OnFocusChangeListener focusListener = null;
        final View focusedView = findFocus();
        int cursorPositionStart = -1;
        int cursorPositionEnd = -1;
        if (focusedView != null) {
            OnFocusChangeListener listener = focusedView.getOnFocusChangeListener();
            if (listener != null && listener instanceof TiUIView) {
                //Before unfocus the current editText, store cursor position so
                //we can restore it later
                if (focusedView instanceof EditText) {
                    cursorPositionStart = ((EditText)focusedView).getSelectionStart();
                    cursorPositionEnd = ((EditText)focusedView).getSelectionEnd();
                }
                focusedView.setOnFocusChangeListener(null);
                focusListener = listener;
            }
        }
        
        int oldDesc = getDescendantFocusability();
        //We are temporarily going to block focus to descendants 
        //because LinearLayout on layout will try to find a focusable descendant
        if (focusedView != null) {
            setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        }
        super.onLayout(changed, left, top, right, bottom);
        //Now we reset the descendant focusability
        setDescendantFocusability(oldDesc);


        // Layout is finished, re-enable focus events.
        if (focusListener != null || focusedView != null) {
            // If the configuration changed, we manually fire the blur event
            if (changed) {
                if (focusedView != null && focusListener != null) {
                    focusedView.setOnFocusChangeListener(focusListener);
                    focusListener.onFocusChange(focusedView, false);
                }
            } else {
                //Ok right now focus is with listView. So set it back to the focusedView
                focusedView.setOnFocusChangeListener(focusListener);
                if (!focusedView.hasFocus()) {
                      focusedView.requestFocus();
                }
                //Restore cursor position
                if (cursorPositionStart != -1) {
                    try {
                        ((EditText)focusedView).setSelection(cursorPositionStart, cursorPositionEnd);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }
}