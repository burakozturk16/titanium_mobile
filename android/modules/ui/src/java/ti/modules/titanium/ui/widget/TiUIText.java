/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import java.util.ArrayList;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUINonViewGroupView;
import org.appcelerator.titanium.view.TiUIView;
import org.appcelerator.titanium.view.TiCompositeLayout;

import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.AttributedStringProxy;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.text.method.DigitsKeyListener;
import android.text.method.NumberKeyListener;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.content.res.ColorStateList;

public class TiUIText extends TiUINonViewGroupView
	implements TextWatcher, OnEditorActionListener, OnFocusChangeListener
{
	private static final String TAG = "TiUIText";
    private static final float DEFAULT_SHADOW_RADIUS = 0.5f;

    protected static final int TIFLAG_NEEDS_COLORS               = 0x00000001;
    protected static final int TIFLAG_NEEDS_TEXT                 = 0x00000002;
    protected static final int TIFLAG_NEEDS_TEXT_HTML            = 0x00000004;
    protected static final int TIFLAG_NEEDS_SHADOW               = 0x00000008;
    protected static final int TIFLAG_NEEDS_KEYBOARD             = 0x00000010;
    protected static final int TIFLAG_NEEDS_RETURN_KEY           = 0x00000011;
	
	private int selectedColor, color, disabledColor;
	private float shadowRadius = DEFAULT_SHADOW_RADIUS;
    private float shadowX = 0f;
    private float shadowY = 0f;
    private int shadowColor = Color.TRANSPARENT;
    
    int type = UIModule.KEYBOARD_ASCII;
    boolean passwordMask = false;
    int autocorrect = InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;
    int autoCapValue = 0;
    int returnKeyType = UIModule.RETURNKEY_DEFAULT;
    
	private boolean field;
	private int maxLength = -1;
	private boolean isTruncatingText = false;
	private boolean disableChangeEvent = false;
    protected boolean isEditable = true;
    private boolean suppressReturn = true;
    protected RectF padding = null;

	protected FocusFixedEditText tv;
	protected TiEditText realtv;

	public class TiEditText extends AppCompatEditText 
	{
	    
		public TiEditText(Context context) 
		{
			super(context);
		}
		
		@Override
		public View focusSearch(int direction) {
			View result = super.focusSearch(direction);
	        return result;
	    }
		
		/** 
		 * Check whether the called view is a text editor, in which case it would make sense to 
		 * automatically display a soft input window for it.
		 */
		@Override
		public boolean onCheckIsTextEditor () {
			if (proxy.hasProperty(TiC.PROPERTY_SOFT_KEYBOARD_ON_FOCUS)
					&& TiConvert.toInt(proxy.getProperty(TiC.PROPERTY_SOFT_KEYBOARD_ON_FOCUS)) == TiUIView.SOFT_KEYBOARD_HIDE_ON_FOCUS) {
					return false;
			}
			if (!isEditable) {
				return false;
			}
			return true;
		}
		
		@Override
	    protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec) {
			
			//In the TextView when using AT_MOST,
			//it would size to Math.min(widthSize, width); which is NOT what we want
		    // when using FILL
			int widthMode = MeasureSpec.getMode(widthMeasureSpec);
	        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
	        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
	        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
	        
	        if (widthMode == MeasureSpec.AT_MOST && layoutParams.autoFillsWidth) {
	        	widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
	        }
	        if (heightMode == MeasureSpec.AT_MOST && layoutParams.autoFillsHeight) {
	        	heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
	        }
			 super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	    }

		@Override
		protected void onLayout(boolean changed, int left, int top, int right, int bottom)
		{
			super.onLayout(changed, left, top, right, bottom);
			TiUIHelper.firePostLayoutEvent(TiUIText.this);
		}

		@Override
		public boolean dispatchTouchEvent(MotionEvent event) {
			if (touchPassThrough == true)
				return false;
			return super.dispatchTouchEvent(event);
		}
		

		@Override
        public void dispatchSetPressed(boolean pressed) {
            if (propagateSetPressed(this, pressed)) {
                super.dispatchSetPressed(pressed);
            }
        }

        @Override
        public void clearFocus() {
            //clear focused is called in setInputType and clearfocus request the focus
            //in root even if we didnt have the focus. DUMB!
            if (!hasFocus()) {
                return;
            } else {
                super.clearFocus();
            }
        }
        
        @Override
        public boolean dispatchKeyEventPreIme(KeyEvent event) {
                InputMethodManager imm = getIMM();
                if (imm != null && imm.isActive() && 
                        event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    //when hiding the keyboard with the back button also blur
                    blur();
                    return true;
                }
            return super.dispatchKeyEventPreIme(event);
        }
	}

	public class FocusFixedEditText extends LinearLayout {
		TiEditText editText;
		protected TiCompositeLayout leftPane;
		protected TiCompositeLayout rightPane;

		private LinearLayout.LayoutParams createBaseParams()
		{
		    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 0.0f);
            params.gravity = Gravity.CENTER;
			return params;
		}

		private void init(Context context) {
			this.setFocusableInTouchMode(false);
			this.setFocusable(false);
			this.setAddStatesFromChildren(true);
			this.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
//			this.requestFocus();
			this.setOrientation(LinearLayout.HORIZONTAL);

			leftPane = new TiCompositeLayout(context);
			leftPane.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
			leftPane.setFocusable(false);
			leftPane.setId(100);
			leftPane.setVisibility(View.GONE);
			this.addView(leftPane, createBaseParams());

			editText = new TiEditText(context);
			editText.setId(200);
			this.addView(editText, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f));

			rightPane = new TiCompositeLayout(context);
			rightPane.setId(300);
			rightPane.setVisibility(View.GONE);
			rightPane.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
			rightPane.setFocusable(false);
			this.addView(rightPane, createBaseParams());
		}
		
		@Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

		public FocusFixedEditText(Context context) {
			super(context);
			init(context);
		}

		public void setLeftView(Object leftView) {
		    leftPane.removeAllViews();
            if (leftView instanceof View) {
                TiUIHelper.safeAddView(leftPane, (View)leftView);
                showLeftView();
            } else {
                KrollProxy viewProxy = proxy.addProxyToHold(leftView, "leftButton");
                if (viewProxy instanceof TiViewProxy) {
                    TiUIHelper.removeViewFromSuperView((TiViewProxy) viewProxy);
                    TiUIHelper.safeAddView(leftPane, ((TiViewProxy) viewProxy).getOrCreateView().getOuterView());
                    showLeftView();
                }
                else {
                    hideLeftView();
                }
            }
		}

		public void setRightView(Object rightView) {
		    rightPane.removeAllViews();
		    if (rightView instanceof View) {
                TiUIHelper.safeAddView(rightPane, (View)rightView);
                showRightView();
            } else {
                KrollProxy viewProxy = proxy.addProxyToHold(rightView, "rightButton");
                if (viewProxy instanceof TiViewProxy) {
                    TiUIHelper.removeViewFromSuperView((TiViewProxy) viewProxy);
                    TiUIHelper.safeAddView(rightPane, ((TiViewProxy) viewProxy).getOrCreateView().getOuterView());
                    showRightView();
                }
                else {
                    hideRightView();
                }
            }
		}

		public void hideLeftView()
		{
			leftPane.setVisibility(View.GONE);
		}

		public void showLeftView()
		{
			leftPane.setVisibility(View.VISIBLE);
		}

		public void hideRightView()
		{
			rightPane.setVisibility(View.GONE);
		}

		public void showRightView()
		{
			rightPane.setVisibility(View.VISIBLE);
		}
		
		@Override
		public boolean dispatchTouchEvent(MotionEvent event) {
			if (touchPassThrough == true)
				return false;
			return super.dispatchTouchEvent(event);
		}

		@Override
		public boolean onCheckIsTextEditor () {
			return editText.onCheckIsTextEditor();
		}

		public TiEditText getRealEditText() {
			return editText;
		}

		@Override
		public void setOnFocusChangeListener(OnFocusChangeListener l) {
			editText.setOnFocusChangeListener(l);
		}
	}
	private static final ArrayList<String> KEY_SEQUENCE;
    static{
      ArrayList<String> tmp = new ArrayList<String>();
      tmp.add(TiC.PROPERTY_COLOR);
      KEY_SEQUENCE = tmp;
    }
    @Override
    protected ArrayList<String> keySequence() {
        return KEY_SEQUENCE;
    }
    
	public TiUIText(final TiViewProxy proxy, boolean field)
	{
		super(proxy);
		this.focusKeyboardState = TiUIView.SOFT_KEYBOARD_SHOW_ON_FOCUS;
		this.isFocusable = true; //default to true
		this.field = field;
		tv = new FocusFixedEditText(getProxy().getActivity());
		realtv = tv.getRealEditText();
        realtv.setSingleLine(field);
		if (field) {
			realtv.setMaxLines(1);
		}
		else {
//            realtv.setMaxLines(1000);
//            realtv.setMinLines(2);
//            realtv.setHorizontallyScrolling(false);
//            realtv.setEllipsize(null);
//            realtv.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		}
		realtv.addTextChangedListener(this);
		realtv.setOnEditorActionListener(this);
		realtv.setIncludeFontPadding(true); 
		if (field) {
			realtv.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
		} else {
			realtv.setGravity(Gravity.TOP | Gravity.LEFT);
		}
		color = disabledColor = selectedColor = realtv.getCurrentTextColor();
		setNativeView(tv);
	}


	private void updateTextColors() {
		int[][] states = new int[][] {
			TiUIHelper.BACKGROUND_DISABLED_STATE, // disabled
			TiUIHelper.BACKGROUND_SELECTED_STATE, // pressed
			TiUIHelper.BACKGROUND_FOCUSED_STATE,  // pressed
			TiUIHelper.BACKGROUND_CHECKED_STATE,  // pressed
			new int [] {android.R.attr.state_pressed},  // pressed
			new int [] {android.R.attr.state_focused},  // pressed
			new int [] {}
		};

		ColorStateList colorStateList = new ColorStateList(
			states,
			new int[] {disabledColor, selectedColor, selectedColor, selectedColor, selectedColor, selectedColor, color}
		);

		realtv.setTextColor(colorStateList);
	}
	
	@Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_COLOR:
            color = selectedColor = disabledColor = color = TiConvert.toColor(newValue, this.color);
            mProcessUpdateFlags |= TIFLAG_NEEDS_COLORS;
            break;
        case TiC.PROPERTY_SELECTED_COLOR:
            selectedColor = TiConvert.toColor(newValue, this.selectedColor);
            mProcessUpdateFlags |= TIFLAG_NEEDS_COLORS;
            break;
        case TiC.PROPERTY_DISABLED_COLOR:
            disabledColor = TiConvert.toColor(newValue, this.disabledColor);
            mProcessUpdateFlags |= TIFLAG_NEEDS_COLORS;
            break;
        case TiC.PROPERTY_FONT:
            TiUIHelper.styleText(getEditText(), TiConvert.toKrollDict(newValue));
            setNeedsLayout();
            break;
        case TiC.PROPERTY_TEXT_ALIGN:
            TiUIHelper.setAlignment(getEditText(), TiConvert.toString(newValue), null);
            setNeedsLayout();
            break;
        case TiC.PROPERTY_VERTICAL_ALIGN:
            TiUIHelper.setAlignment(getEditText(), null, TiConvert.toString(newValue));
            setNeedsLayout();
            break;
        case TiC.PROPERTY_PADDING:
            padding = TiConvert.toPaddingRect(newValue, padding);
            TiUIHelper.setPadding(getEditText(), padding);
            setNeedsLayout();
            break;
        case TiC.PROPERTY_WORD_WRAP:
            getEditText().setSingleLine(!TiConvert.toBoolean(newValue));
            setNeedsLayout();
            break;
        case TiC.PROPERTY_SHADOW_OFFSET:
            if (newValue instanceof HashMap) {
                HashMap dict = (HashMap) newValue;
                shadowX = TiUIHelper.getInPixels(dict.get(TiC.PROPERTY_X));
                shadowY = TiUIHelper.getInPixels(dict.get(TiC.PROPERTY_Y));
            }
            else {
                shadowX = 0f;
                shadowY = 0f;
            }
            mProcessUpdateFlags |= TIFLAG_NEEDS_SHADOW;
            break;
        case TiC.PROPERTY_SHADOW_RADIUS:
            shadowRadius = TiConvert.toFloat(newValue, DEFAULT_SHADOW_RADIUS);
            mProcessUpdateFlags |= TIFLAG_NEEDS_SHADOW;
            break;
        case TiC.PROPERTY_SHADOW_COLOR:
            shadowColor = TiConvert.toColor(newValue);
            mProcessUpdateFlags |= TIFLAG_NEEDS_SHADOW;
            break;
        case TiC.PROPERTY_MAX_LENGTH:
            maxLength = TiConvert.toInt(newValue, -1);
            if (changedProperty) {
              //truncate if current text exceeds max length
                Editable currentText = getEditText().getText();
                if (maxLength >= 0 && currentText.length() > maxLength) {
                    CharSequence truncateText = currentText.subSequence(0, maxLength);
                    int cursor = getEditText().getSelectionStart() - 1;
                    if (cursor > maxLength) {
                        cursor = maxLength;
                    }
                    getEditText().setText(truncateText);
                    getEditText().setSelection(cursor);
                }
            }
            break;
        case TiC.PROPERTY_SUPPRESS_RETURN:
            suppressReturn = TiConvert.toBoolean(newValue, true);
            break;
        case TiC.PROPERTY_HINT_TEXT:
            getEditText().setHint(TiConvert.toString(newValue));
            break;
        case TiC.PROPERTY_ATTRIBUTED_HINT_TEXT:
            if (newValue instanceof AttributedStringProxy) {
                getEditText().setHint(AttributedStringProxy.toSpannable((AttributedStringProxy) newValue, TiApplication.getAppCurrentActivity()));
            }
            break;
        case TiC.PROPERTY_ATTRIBUTED_STRING:
            if (newValue instanceof AttributedStringProxy) {
                disableChangeEvent = !changedProperty;
                getEditText().setText(AttributedStringProxy.toSpannable((AttributedStringProxy) newValue, TiApplication.getAppCurrentActivity()));
                int pos = getEditText().getText().length();
                getEditText().setSelection(pos);
                disableChangeEvent = false;
            }
            break;    
        case TiC.PROPERTY_VALUE:
            disableChangeEvent = !changedProperty;
            getEditText().setText(TiConvert.toString(newValue));
            int pos = getEditText().getText().length();
            getEditText().setSelection(pos);
            disableChangeEvent = false;
            break;
        case TiC.PROPERTY_ELLIPSIZE:
            getEditText().setEllipsize(TiConvert.toBoolean(newValue)?TruncateAt.END:null);
            break;
        case TiC.PROPERTY_HINT_COLOR:
            getEditText().setHintTextColor(TiConvert.toColor(newValue, Color.GRAY));
            break;
        case TiC.PROPERTY_AUTOCORRECT:
            autocorrect = TiConvert.toBoolean(newValue, true)?InputType.TYPE_TEXT_FLAG_AUTO_CORRECT:0;
            mProcessUpdateFlags |= TIFLAG_NEEDS_KEYBOARD;
            break;
        case TiC.PROPERTY_AUTOCAPITALIZATION:
        {
            switch (TiConvert.toInt(newValue, UIModule.TEXT_AUTOCAPITALIZATION_NONE)) {
            case UIModule.TEXT_AUTOCAPITALIZATION_NONE:
                autoCapValue = 0;
                break;
            case UIModule.TEXT_AUTOCAPITALIZATION_ALL:
                autoCapValue = InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | 
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES |
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS
                    ;
                break;
            case UIModule.TEXT_AUTOCAPITALIZATION_SENTENCES:
                autoCapValue = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
                break;
            
            case UIModule.TEXT_AUTOCAPITALIZATION_WORDS:
                autoCapValue = InputType.TYPE_TEXT_FLAG_CAP_WORDS;
                break;
            default:
                Log.w(TAG, "Unknown AutoCapitalization Value ["+TiConvert.toString(newValue)+"]");
            break;
            }
        }
            mProcessUpdateFlags |= TIFLAG_NEEDS_KEYBOARD;
            break;
        case TiC.PROPERTY_PASSWORD_MASK:
            passwordMask = TiConvert.toBoolean(newValue, false);
            mProcessUpdateFlags |= TIFLAG_NEEDS_KEYBOARD;
            break;
        case TiC.PROPERTY_KEYBOARD_TYPE:
            type = TiConvert.toInt(newValue, UIModule.KEYBOARD_DEFAULT);
            mProcessUpdateFlags |= TIFLAG_NEEDS_KEYBOARD;
            break;
        case TiC.PROPERTY_EDITABLE:
            setEditable(TiConvert.toBoolean(newValue, true));   
            break;
        case TiC.PROPERTY_RETURN_KEY_TYPE:
            returnKeyType = TiConvert.toInt(newValue, UIModule.RETURNKEY_DEFAULT);
            mProcessUpdateFlags |= TIFLAG_NEEDS_RETURN_KEY;
            break;
        case TiC.PROPERTY_AUTO_LINK:
            TiUIHelper.linkifyIfEnabled(getEditText(), newValue);
            break;
        case TiC.PROPERTY_LEFT_BUTTON:
            tv.setLeftView(newValue);
           break;
        case TiC.PROPERTY_RIGHT_BUTTON:
            tv.setRightView(newValue);
            break;
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }
	
	private void setEditable(final boolean editable) {
	    if (isEditable == editable) return;
	    isEditable = editable;
        boolean focusable = isEditable && isEnabled;
        TiUIView.setFocusable(realtv, focusable);
        TiUIView.setFocusable(tv, focusable);
        realtv.setCursorVisible(focusable);
	}
    
    @Override
    protected void didProcessProperties() {
        super.didProcessProperties();
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_COLORS) != 0) {
            updateTextColors();
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_COLORS;
        }
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_SHADOW) != 0) {
            getEditText().setShadowLayer(shadowRadius, shadowX, shadowY, shadowColor);
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_SHADOW;
        }
        
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_KEYBOARD) != 0) {
            mProcessUpdateFlags |= TIFLAG_NEEDS_RETURN_KEY;
            handleKeyboard();
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_KEYBOARD;
        }
        
        //the order is important because returnKeyType must overload keyboard return key defined
        // by keyboardType
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_RETURN_KEY) != 0) {
            handleReturnKeyType();
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_RETURN_KEY;
        }
    }
    
    private EditText getEditText() {
        return realtv;
    }
	@Override
	public void afterTextChanged(Editable editable)
	{
		if (maxLength >= 0 && editable.length() > maxLength) {
			// The input characters are more than maxLength. We need to truncate the text and reset text.
			isTruncatingText = true;
			String newText = editable.subSequence(0, maxLength).toString();
			int cursor = realtv.getSelectionStart();
			if (cursor > maxLength) {
				cursor = maxLength;
			}
			realtv.setText(newText); // This method will invoke onTextChanged() and afterTextChanged().
			realtv.setSelection(cursor);
		} else {
			isTruncatingText = false;
		}
	}
	
    private boolean oldTextRequestLayout = false;
	@Override
    public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {
        CharSequence oldText = s.subSequence(start, start + count);
        boolean newLine = oldText.toString().contains("\n");
        oldTextRequestLayout = (newLine && layoutParams.sizeOrFillHeightEnabled && !layoutParams.autoFillsHeight);
    }

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
	    //onTextChanged can be called when reusing a TiUIText in listview
	    //In that case we dont want to report.
	    if (disableChangeEvent
//	            || realtv.willMaskText()
	            ) {
	        Log.d(TAG, "onTextChanged ignore as configuring", Log.DEBUG_MODE);
	        return;
	    }
		//Since Jelly Bean, pressing the 'return' key won't trigger onEditorAction callback
		//http://stackoverflow.com/questions/11311790/oneditoraction-is-not-called-after-enter-key-has-been-pressed-on-jelly-bean-em
		//So here we need to handle the 'return' key manually
		if (Build.VERSION.SDK_INT >= 16 && before == 0 && s.length() > start && s.charAt(start) == '\n' && hasListeners(TiC.EVENT_RETURN, false)) {
			//We use the previous value to make it consistent with pre Jelly Bean behavior (onEditorAction is called before 
			//onTextChanged.
			String value = TiConvert.toString(proxy.getProperty(TiC.PROPERTY_VALUE));
			KrollDict data = new KrollDict();
			data.put(TiC.PROPERTY_VALUE, value);
			fireEvent(TiC.EVENT_RETURN, data, false, false);
		}
		/**
		 * There is an Android bug regarding setting filter on EditText that impacts auto completion.
		 * Therefore we can't use filters to implement "maxLength" property. Instead we manipulate
		 * the text to achieve perfect parity with other platforms.
		 * Android bug url for reference: http://code.google.com/p/android/issues/detail?id=35757
		 */
		if (maxLength >= 0 && s.length() > maxLength) {
			// Can only set truncated text in afterTextChanged. Otherwise, it will crash.
			return;
		}
		
		boolean newLine = oldTextRequestLayout;
		if (!newLine) {
		    CharSequence newText = s.subSequence(start, start + count);
	        newLine  = newText.toString().contains("\n");
		}
        
		if (newLine && layoutParams.sizeOrFillHeightEnabled && !layoutParams.autoFillsHeight) {
		    nativeView.requestLayout();
		}
		String text = realtv.getText().toString();
		if (!isTruncatingText 
			&& proxy.shouldFireChange(proxy.getProperty(TiC.PROPERTY_VALUE), text)) {
            proxy.setProperty(TiC.PROPERTY_VALUE, text);
		    if (hasListeners(TiC.EVENT_CHANGE, false)) {
		        KrollDict data = new KrollDict();
	            data.put(TiC.PROPERTY_VALUE, text);
	            fireEvent(TiC.EVENT_CHANGE, data, false, false);
		    }
			
		}
	}

	@Override
	public void applyCustomBackground()
	{
		super.applyCustomBackground();
		realtv.setBackgroundDrawable(null);
		realtv.postInvalidate();
	}
	
    @Override
    public View getFocusView()
    {
    	return realtv;
    }
    
    @Override
    protected View getTouchView()
    {
        return realtv;
    }

	@Override
	public void setVisibility(int visibility)
	{
		if ((visibility == View.INVISIBLE))
			this.blur();
		super.setVisibility(visibility);
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus)
	{
		
		if (v == realtv)
			Log.d(TAG, "onFocusChange "  + hasFocus + "  for FocusFixedEditText with text " + realtv.getText(), Log.DEBUG_MODE);
		else
			Log.d(TAG, "onFocusChange "  + hasFocus + "  for FocusFixedEditText  layout with text " + realtv.getText(), Log.DEBUG_MODE);
		if (!realtv.isFocusable()) return;
		if (hasFocus) {
			Boolean clearOnEdit = (Boolean) proxy.getProperty(TiC.PROPERTY_CLEAR_ON_EDIT);
			if (clearOnEdit != null && clearOnEdit) {
				realtv.setText("");
			}
			Rect r = new Rect();
			nativeView.getFocusedRect(r);
			nativeView.requestRectangleOnScreen(r);

		}
		super.onFocusChange(v, hasFocus);
	}

	@Override
	protected KrollDict getFocusEventObject(boolean hasFocus)
	{
		KrollDict event = new KrollDict();
		event.put(TiC.PROPERTY_VALUE, realtv.getText().toString());
		return event;
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent keyEvent)
	{
		String value = realtv.getText().toString();
		

		proxy.setProperty(TiC.PROPERTY_VALUE, value);
		Log.d(TAG, "ActionID: " + actionId + " KeyEvent: " + (keyEvent != null ? keyEvent.getKeyCode() : null),
			Log.DEBUG_MODE);
		
        boolean result = false;
        boolean shouldBlur = (actionId != EditorInfo.IME_ACTION_NEXT);
        if (keyEvent == null) {
        } else if (actionId == EditorInfo.IME_NULL) {
            if (!suppressReturn) {
                shouldBlur = false;
            }
        }
		
		//This is to prevent 'return' event from being fired twice when return key is hit. In other words, when return key is clicked,
		//this callback is triggered twice (except for keys that are mapped to EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_ACTION_DONE). The first check is to deal with those keys - filter out
		//one of the two callbacks, and the next checks deal with 'Next' and 'Done' callbacks, respectively.
		//Refer to TiUIText.handleReturnKeyType(int) for a list of return keys that are mapped to EditorInfo.IME_ACTION_NEXT and EditorInfo.IME_ACTION_DONE.
		if (((actionId == EditorInfo.IME_NULL && keyEvent != null) || 
				actionId == EditorInfo.IME_ACTION_NEXT || 
				actionId == EditorInfo.IME_ACTION_DONE )) {
			if (hasListeners(TiC.EVENT_RETURN, false)) 
			{
				KrollDict data = new KrollDict();
				data.put(TiC.PROPERTY_VALUE, value);
				fireEvent(TiC.EVENT_RETURN, data, false, false);
			}
			if (shouldBlur) {
	            blur();
	        }
		}		

		Boolean enableReturnKey = proxy.getProperties().optBoolean(TiC.PROPERTY_ENABLE_RETURN_KEY, false);
		if (enableReturnKey && value.length() == 0) {
			result = true;
		}
		
		return result;
	}

	public void handleKeyboard() 
	{
		int typeModifiers = autocorrect | autoCapValue;
		int textTypeAndClass = typeModifiers;
		
		if (type != UIModule.KEYBOARD_DECIMAL_PAD) {
			textTypeAndClass = textTypeAndClass | InputType.TYPE_CLASS_TEXT;
		}

		realtv.setCursorVisible(true);
		switch(type) {
			case UIModule.KEYBOARD_DEFAULT:
			case UIModule.KEYBOARD_ASCII:
				// Don't need a key listener, inputType handles that.
				break;
			case UIModule.KEYBOARD_NUMBERS_PUNCTUATION:
				textTypeAndClass |= (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_CLASS_TEXT);
				realtv.setKeyListener(new NumberKeyListener()
				{
					@Override
					public int getInputType() {
						return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_CLASS_TEXT;
					}

					@Override
					protected char[] getAcceptedChars() {
						return new char[] {
							'0', '1', '2','3','4','5','6','7','8','9',
							'.','-','+','_','*','-','!','@', '#', '$',
							'%', '^', '&', '*', '(', ')', '=',
							'{', '}', '[', ']', '|', '\\', '<', '>',
							',', '?', '/', ':', ';', '\'', '"', '~'
						};
					}
				});
				break;
			case UIModule.KEYBOARD_URL:
				Log.d(TAG, "Setting keyboard type URL-3", Log.DEBUG_MODE);
				realtv.setImeOptions(EditorInfo.IME_ACTION_GO);
				textTypeAndClass |= InputType.TYPE_TEXT_VARIATION_URI;
				break;
			case UIModule.KEYBOARD_DECIMAL_PAD:
				textTypeAndClass = (InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
			case UIModule.KEYBOARD_NUMBER_PAD:
				realtv.setKeyListener(DigitsKeyListener.getInstance(true,true));
				textTypeAndClass |= InputType.TYPE_CLASS_NUMBER;
				break;
			case UIModule.KEYBOARD_PHONE_PAD:
				realtv.setKeyListener(DialerKeyListener.getInstance());
				textTypeAndClass |= InputType.TYPE_CLASS_PHONE;
				break;
			case UIModule.KEYBOARD_EMAIL:
				textTypeAndClass |= InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
				break;
		}
		
		if (!field) {
            textTypeAndClass |= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
		}

		if (passwordMask) {
			textTypeAndClass |= InputType.TYPE_TEXT_VARIATION_PASSWORD;
			Typeface origTF = realtv.getTypeface();
			// Sometimes password transformation does not work properly when the input type is set after the transformation method.
			// This issue has been filed at http://code.google.com/p/android/issues/detail?id=7092
			realtv.setInputType(textTypeAndClass);
			// Workaround for https://code.google.com/p/android/issues/detail?id=55418 since setInputType
			// with InputType.TYPE_TEXT_VARIATION_PASSWORD sets the typeface to monospace.
            realtv.setTransformationMethod(PasswordTransformationMethod.getInstance());
			realtv.setTypeface(origTF);

			//turn off text UI in landscape mode b/c Android numeric passwords are not masked correctly in landscape mode.
			if (type == UIModule.KEYBOARD_NUMBERS_PUNCTUATION || type == UIModule.KEYBOARD_DECIMAL_PAD || type == UIModule.KEYBOARD_NUMBER_PAD) {
				realtv.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
			}

		} else {
			realtv.setInputType(textTypeAndClass);
			if (realtv.getTransformationMethod() instanceof PasswordTransformationMethod) {
				realtv.setTransformationMethod(null);
			}
		}
        
		
		//setSingleLine() append the flag TYPE_TEXT_FLAG_MULTI_LINE to the current inputType, so we want to call this
		//after we set inputType.
//		if (!field) {
//			realtv.setSingleLine(false);
//		}

	}

	public void setSelection(int start, int end) 
	{
		int textLength = realtv.length();
		if (start < 0 || start > textLength || end < 0 || end > textLength) {
			Log.w(TAG, "Invalid range for text selection. Ignoring.");
			return;
		}
		realtv.setSelection(start, end);
	}
	
	public KrollDict getSelection() {
		KrollDict result = new KrollDict(2);
		int start = realtv.getSelectionStart();
		result.put(TiC.PROPERTY_LOCATION, start);
		if (start != -1) {
			int end = realtv.getSelectionEnd();
			result.put(TiC.PROPERTY_LENGTH, end - start);
		} else {
			result.put(TiC.PROPERTY_LENGTH, -1);
		}
		
		return result;
	}

    public void handleReturnKeyType() {
        int option = EditorInfo.IME_ACTION_UNSPECIFIED;
        switch (returnKeyType) {
        case UIModule.RETURNKEY_GO:
            option = EditorInfo.IME_ACTION_GO;
            break;
        case UIModule.RETURNKEY_GOOGLE:
            option = EditorInfo.IME_ACTION_GO;
            break;
        case UIModule.RETURNKEY_JOIN:
            option = EditorInfo.IME_ACTION_DONE;
            break;
        case UIModule.RETURNKEY_NEXT:
            option = EditorInfo.IME_ACTION_NEXT;
            break;
        case UIModule.RETURNKEY_ROUTE:
            option = EditorInfo.IME_ACTION_DONE;
            break;
        case UIModule.RETURNKEY_SEARCH:
            option = EditorInfo.IME_ACTION_SEARCH;
            break;
        case UIModule.RETURNKEY_YAHOO:
            option = EditorInfo.IME_ACTION_GO;
            break;
        case UIModule.RETURNKEY_DONE:
            option = EditorInfo.IME_ACTION_DONE;
            break;
        case UIModule.RETURNKEY_EMERGENCY_CALL:
            option = EditorInfo.IME_ACTION_GO;
            break;

        case UIModule.RETURNKEY_SEND:
            option = EditorInfo.IME_ACTION_SEND;
            break;
        case UIModule.RETURNKEY_DEFAULT:
        default:
            option = EditorInfo.IME_ACTION_UNSPECIFIED;
            break;
        }
        getEditText().setImeOptions(option);

        // Set input type caches ime options, so whenever we change ime options,
        // we must reset input type
        getEditText().setInputType(getEditText().getInputType());
    }

	@Override
	public boolean focus()
	{
		if (!isEditable || (tv != null && tv.getVisibility() == View.INVISIBLE)) {
			return false;
		}
		return super.focus();
	}
}
