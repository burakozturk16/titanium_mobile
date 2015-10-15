/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.searchview;

import java.lang.reflect.Field;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.util.TiRHelper.ResourceNotFoundException;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.AttributedStringProxy;
import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.widget.searchbar.TiUISearchBar.OnSearchChangeListener;
import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.text.TextUtils.TruncateAt;
import android.text.method.DialerKeyListener;
import android.text.method.DigitsKeyListener;
import android.text.method.NumberKeyListener;
import android.text.method.PasswordTransformationMethod;

@SuppressLint("NewApi")
public class TiUISearchView extends TiUIView implements
        SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    private SearchView searchView;

    public static final String TAG = "SearchView";

    private static final float DEFAULT_SHADOW_RADIUS = 0.5f;

    protected static final int TIFLAG_NEEDS_COLORS = 0x00000001;
    protected static final int TIFLAG_NEEDS_TEXT = 0x00000002;
    protected static final int TIFLAG_NEEDS_TEXT_HTML = 0x00000004;
    protected static final int TIFLAG_NEEDS_SHADOW = 0x00000008;
    protected static final int TIFLAG_NEEDS_KEYBOARD = 0x00000010;
    protected static final int TIFLAG_NEEDS_RETURN_KEY = 0x00000011;

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
    protected RectF padding = null;

    private boolean field;
//    private boolean disableChangeEvent = false;
    protected boolean isEditable = true;
    private boolean suppressReturn = true;
    private boolean iconifyOnBlur = true;

    protected OnSearchChangeListener searchChangeListener;

    public TiUISearchView(TiViewProxy proxy) {
        super(proxy);

        searchView = new SearchView(proxy.getActivity()) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent event) {
                if (touchPassThrough == true)
                    return false;
                return super.dispatchTouchEvent(event);
            }
        };
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);
        searchView.setOnQueryTextFocusChangeListener(this);

        setNativeView(searchView);

    }
    
    private void setSearchIcons() {
        try {
            Field searchField = SearchView.class.getDeclaredField("mCloseButton");
            searchField.setAccessible(true);
            ImageView closeBtn = (ImageView) searchField.get(searchView);
            closeBtn.setImageResource(TiRHelper.getResource("R$", "drawable.ic_menu_cancel"));
 
            searchField = SearchView.class.getDeclaredField("mVoiceButton");
            searchField.setAccessible(true);
            ImageView voiceBtn = (ImageView) searchField.get(searchView);
            voiceBtn.setImageResource(TiRHelper.getResource("R$", "drawable.ic_menu_voice_input"));
            ImageView searchButton = (ImageView) searchView.findViewById(TiRHelper.getResource("android.support.v7.appcompat.R$", "id.abs__search_button"));
            searchButton.setImageResource(TiRHelper.getResource("R$", "drawable.ic_menu_search"));
 
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e, Log.DEBUG_MODE);

        }
    }
    
    static int SEARCH_TEXT_ID = -1;

    public AutoCompleteTextView getTextView() {
        if (SEARCH_TEXT_ID == -1) {
            try {
                SEARCH_TEXT_ID = TiRHelper.getResource("android.support.v7.appcompat.R$", "id.search_src_text");
//                setSearchIcons();
            } catch (ResourceNotFoundException e) {
                SEARCH_TEXT_ID = 0;
            }
        }
        if (SEARCH_TEXT_ID > 0) {
            return (AutoCompleteTextView) searchView.findViewById(SEARCH_TEXT_ID);
        }
        return null;
    }

    @Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_ICONIFIED:
            searchView.setIconified(TiConvert.toBoolean(newValue));
            break;
        case TiC.PROPERTY_ICONIFIED_BY_DEFAULT:
            searchView.setIconifiedByDefault(TiConvert.toBoolean(newValue));
            break;
        case TiC.PROPERTY_SUBMIT_ENABLED:
            searchView.setSubmitButtonEnabled(TiConvert.toBoolean(newValue));
            break;
        case TiC.PROPERTY_COLOR:
            color = selectedColor = disabledColor = color = TiConvert.toColor(
                    newValue, this.color);
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
            TiUIHelper.styleText(getTextView(), TiConvert.toKrollDict(newValue));
            setNeedsLayout();
            break;
        case TiC.PROPERTY_TEXT_ALIGN:
            TiUIHelper.setAlignment(getTextView(),
                    TiConvert.toString(newValue), null);
            setNeedsLayout();
            break;
        case TiC.PROPERTY_VERTICAL_ALIGN:
            TiUIHelper.setAlignment(getTextView(), null,
                    TiConvert.toString(newValue));
            setNeedsLayout();
            break;
        case TiC.PROPERTY_PADDING:
            padding = TiConvert.toPaddingRect(newValue, padding);
            TiUIHelper.setPadding(getTextView(), padding);
            setNeedsLayout();
            break;
        case TiC.PROPERTY_WORD_WRAP:
            getTextView().setSingleLine(!TiConvert.toBoolean(newValue));
            setNeedsLayout();
            break;
        case TiC.PROPERTY_SHADOW_OFFSET:
            if (newValue instanceof HashMap) {
                HashMap dict = (HashMap) newValue;
                shadowX = TiUIHelper.getInPixels(dict.get(TiC.PROPERTY_X));
                shadowY = TiUIHelper.getInPixels(dict.get(TiC.PROPERTY_Y));
            } else {
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
        case TiC.PROPERTY_SUPPRESS_RETURN:
            suppressReturn = TiConvert.toBoolean(newValue, true);
            break;
        case TiC.PROPERTY_HINT_TEXT:
            searchView.setQueryHint(TiConvert.toString(newValue));
            break;
        case TiC.PROPERTY_ATTRIBUTED_HINT_TEXT:
            if (newValue instanceof AttributedStringProxy) {
                searchView.setQueryHint(AttributedStringProxy.toSpannable(
                        (AttributedStringProxy) newValue,
                        TiApplication.getAppCurrentActivity()));
            }
            break;
        case TiC.PROPERTY_ATTRIBUTED_STRING:
            if (newValue instanceof AttributedStringProxy) {
//                disableChangeEvent = !changedProperty;
                searchView.setQuery(AttributedStringProxy.toSpannable(
                        (AttributedStringProxy) newValue,
                        TiApplication.getAppCurrentActivity()), false);
//                int pos = searchView.getQuery().length();
//                getTextView().setSelection(pos);
//                disableChangeEvent = false;
            }
            break;
        case TiC.PROPERTY_VALUE:
//            disableChangeEvent = !changedProperty;
            searchView.setQuery(TiConvert.toString(newValue), false);
//            int pos = searchView.getQuery().length();
//            getTextView().setSelection(pos);
//            disableChangeEvent = false;
            break;
        case TiC.PROPERTY_ELLIPSIZE:
            getTextView().setEllipsize(
                    TiConvert.toBoolean(newValue) ? TruncateAt.END : null);
            break;
        case TiC.PROPERTY_HINT_COLOR:
            getTextView().setHintTextColor(
                    TiConvert.toColor(newValue, Color.GRAY));
            break;
        case TiC.PROPERTY_AUTOCORRECT:
            autocorrect = TiConvert.toBoolean(newValue, true) ? InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
                    : 0;
            mProcessUpdateFlags |= TIFLAG_NEEDS_KEYBOARD;
            break;
        case TiC.PROPERTY_AUTOCAPITALIZATION: {
            switch (TiConvert.toInt(newValue,
                    UIModule.TEXT_AUTOCAPITALIZATION_NONE)) {
            case UIModule.TEXT_AUTOCAPITALIZATION_NONE:
                autoCapValue = 0;
                break;
            case UIModule.TEXT_AUTOCAPITALIZATION_ALL:
                autoCapValue = InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS;
                break;
            case UIModule.TEXT_AUTOCAPITALIZATION_SENTENCES:
                autoCapValue = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
                break;

            case UIModule.TEXT_AUTOCAPITALIZATION_WORDS:
                autoCapValue = InputType.TYPE_TEXT_FLAG_CAP_WORDS;
                break;
            default:
                Log.w(TAG,
                        "Unknown AutoCapitalization Value ["
                                + TiConvert.toString(newValue) + "]");
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
            returnKeyType = TiConvert.toInt(newValue,
                    UIModule.RETURNKEY_DEFAULT);
            mProcessUpdateFlags |= TIFLAG_NEEDS_RETURN_KEY;
            break;
        case TiC.PROPERTY_AUTO_LINK:
            TiUIHelper.linkifyIfEnabled(getTextView(), newValue);
            break;
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }

    @Override
    protected void didProcessProperties() {
        super.didProcessProperties();
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_COLORS) != 0) {
            updateTextColors();
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_COLORS;
        }
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_SHADOW) != 0) {
            getTextView().setShadowLayer(shadowRadius, shadowX, shadowY,
                    shadowColor);
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_SHADOW;
        }

        if ((mProcessUpdateFlags & TIFLAG_NEEDS_KEYBOARD) != 0) {
            mProcessUpdateFlags |= TIFLAG_NEEDS_RETURN_KEY;
            handleKeyboard();
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_KEYBOARD;
        }

        // the order is important because returnKeyType must overload keyboard
        // return key defined
        // by keyboardType
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_RETURN_KEY) != 0) {
            handleReturnKeyType();
            mProcessUpdateFlags &= ~TIFLAG_NEEDS_RETURN_KEY;
        }
    }

    private void updateTextColors() {
        int[][] states = new int[][] { TiUIHelper.BACKGROUND_DISABLED_STATE, // disabled
                TiUIHelper.BACKGROUND_SELECTED_STATE, // pressed
                TiUIHelper.BACKGROUND_FOCUSED_STATE, // pressed
                TiUIHelper.BACKGROUND_CHECKED_STATE, // pressed
                new int[] { android.R.attr.state_pressed }, // pressed
                new int[] { android.R.attr.state_focused }, // pressed
                new int[] {} };

        ColorStateList colorStateList = new ColorStateList(states, new int[] {
                disabledColor, selectedColor, selectedColor, selectedColor,
                selectedColor, selectedColor, color });

        getTextView().setTextColor(colorStateList);
    }

    private void setEditable(final boolean editable) {
        if (isEditable == editable)
            return;
        isEditable = editable;
        boolean focusable = isEditable && isEnabled;
        TiUIView.setFocusable(searchView, focusable);
        getTextView().setCursorVisible(focusable);
    }

    @Override
    public void release() {
        searchChangeListener = null;
        super.release();
    }

    @Override
    public boolean onClose() {
//        TiUIHelper.showSoftKeyboard(nativeView, false);
        if (hasListeners(TiC.EVENT_CANCEL, false)) {
            fireEvent(TiC.EVENT_CANCEL, null, false, false);
        }
        this.blur();
        return false;
    }
    
    @Override
    public void onFocusChange(final View v, final boolean hasFocus)
    {
        if (!TiApplication.isUIThread()) {
            proxy.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onFocusChange(v, hasFocus);
                }
            });
            return;
        }
        super.onFocusChange(v, hasFocus);
        if (!hasFocus && iconifyOnBlur) {
            searchView.setIconified(true);
        }
    }
    
    @Override
    public void setVisibility(int visibility)
    {
        if ((visibility == View.INVISIBLE))
            this.blur();
        super.setVisibility(visibility);
    }
    
    @Override
    protected KrollDict getFocusEventObject(boolean hasFocus)
    {
        KrollDict event = new KrollDict();
        event.put(TiC.PROPERTY_VALUE, searchView.getQuery().toString());
        return event;
    }

    @Override
    public boolean onQueryTextChange(String query) {
//        if (disableChangeEvent) {
//            Log.d(TAG, "onTextChanged ignore as configuring", Log.DEBUG_MODE);
//            return false;
//        }
        proxy.setProperty(TiC.PROPERTY_VALUE, query);
        if (searchChangeListener != null) {
            searchChangeListener.filterBy(query);
        }
        if (hasListeners(TiC.EVENT_CHANGE, false)) {
            //We use the previous value to make it consistent with pre Jelly Bean behavior (onEditorAction is called before 
            //onTextChanged.
            KrollDict data = new KrollDict();
            data.put(TiC.PROPERTY_VALUE, query);
            fireEvent(TiC.EVENT_CHANGE, data, false, false);
        }
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
//        TiUIHelper.showSoftKeyboard(nativeView, false);
        proxy.setProperty(TiC.PROPERTY_VALUE, query);
        if (hasListeners(TiC.EVENT_RETURN, false)) 
        {
            KrollDict data = new KrollDict();
            data.put(TiC.PROPERTY_VALUE, query);
            fireEvent(TiC.EVENT_RETURN, data, false, false);
        }
        if (suppressReturn) {
            blur();
        }
        return false;
    }

    public void setOnSearchChangeListener(OnSearchChangeListener listener) {
        searchChangeListener = listener;
    }

    public void handleKeyboard() {
        TextView realtv = getTextView();
        int typeModifiers = autocorrect | autoCapValue;
        int textTypeAndClass = typeModifiers;

        if (type != UIModule.KEYBOARD_DECIMAL_PAD) {
            textTypeAndClass = textTypeAndClass | InputType.TYPE_CLASS_TEXT;
        }

        realtv.setCursorVisible(true);
        switch (type) {
        case UIModule.KEYBOARD_DEFAULT:
        case UIModule.KEYBOARD_ASCII:
            // Don't need a key listener, inputType handles that.
            break;
        case UIModule.KEYBOARD_NUMBERS_PUNCTUATION:
            textTypeAndClass |= (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_CLASS_TEXT);
            realtv.setKeyListener(new NumberKeyListener() {
                @Override
                public int getInputType() {
                    return InputType.TYPE_CLASS_NUMBER
                            | InputType.TYPE_CLASS_TEXT;
                }

                @Override
                protected char[] getAcceptedChars() {
                    return new char[] { '0', '1', '2', '3', '4', '5', '6', '7',
                            '8', '9', '.', '-', '+', '_', '*', '-', '!', '@',
                            '#', '$', '%', '^', '&', '*', '(', ')', '=', '{',
                            '}', '[', ']', '|', '\\', '<', '>', ',', '?', '/',
                            ':', ';', '\'', '"', '~' };
                }
            });
            break;
        case UIModule.KEYBOARD_URL:
            Log.d(TAG, "Setting keyboard type URL-3", Log.DEBUG_MODE);
            realtv.setImeOptions(EditorInfo.IME_ACTION_GO);
            textTypeAndClass |= InputType.TYPE_TEXT_VARIATION_URI;
            break;
        case UIModule.KEYBOARD_DECIMAL_PAD:
            textTypeAndClass |= (InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        case UIModule.KEYBOARD_NUMBER_PAD:
            realtv.setKeyListener(DigitsKeyListener.getInstance(true, true));
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
            // Sometimes password transformation does not work properly when the
            // input type is set after the transformation method.
            // This issue has been filed at
            // http://code.google.com/p/android/issues/detail?id=7092
            searchView.setInputType(textTypeAndClass);
            // Workaround for
            // https://code.google.com/p/android/issues/detail?id=55418 since
            // setInputType
            // with InputType.TYPE_TEXT_VARIATION_PASSWORD sets the typeface to
            // monospace.
            realtv.setTransformationMethod(PasswordTransformationMethod
                    .getInstance());
            realtv.setTypeface(origTF);

            // turn off text UI in landscape mode b/c Android numeric passwords
            // are not masked correctly in landscape mode.
            if (type == UIModule.KEYBOARD_NUMBERS_PUNCTUATION
                    || type == UIModule.KEYBOARD_DECIMAL_PAD
                    || type == UIModule.KEYBOARD_NUMBER_PAD) {
                searchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            }

        } else {
            searchView.setInputType(textTypeAndClass);
            if (realtv.getTransformationMethod() instanceof PasswordTransformationMethod) {
                realtv.setTransformationMethod(null);
            }
        }

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
        searchView.setImeOptions(option);

        // Set input type caches ime options, so whenever we change ime options,
        // we must reset input type
        searchView.setInputType(searchView.getInputType());
    }

}
