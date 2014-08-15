/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.searchview;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.widget.searchbar.TiUISearchBar.OnSearchChangeListener;
import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.support.v7.widget.SearchView;
import android.text.InputType;

@SuppressLint("NewApi")
public class TiUISearchView extends TiUIView implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {
	private SearchView searchView;

	public static final String TAG = "SearchView";

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
	
	public void handleKeyboard(KrollDict d) 
    {
        int type = UIModule.KEYBOARD_ASCII;
        boolean passwordMask = false;
        int autocorrect = InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;
        int autoCapValue = 0;

        if (d.containsKey(TiC.PROPERTY_AUTOCORRECT) && !TiConvert.toBoolean(d, TiC.PROPERTY_AUTOCORRECT, true)) {
            autocorrect = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        }

        if (d.containsKey(TiC.PROPERTY_AUTOCAPITALIZATION)) {

            switch (TiConvert.toInt(d.get(TiC.PROPERTY_AUTOCAPITALIZATION), UIModule.TEXT_AUTOCAPITALIZATION_NONE)) {
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
                    Log.w(TAG, "Unknown AutoCapitalization Value ["+d.getString(TiC.PROPERTY_AUTOCAPITALIZATION)+"]");
                break;
            }
        }

        if (d.containsKey(TiC.PROPERTY_PASSWORD_MASK)) {
            passwordMask = TiConvert.toBoolean(d, TiC.PROPERTY_PASSWORD_MASK, false);
        }

        if (d.containsKey(TiC.PROPERTY_KEYBOARD_TYPE)) {
            type = TiConvert.toInt(d.get(TiC.PROPERTY_KEYBOARD_TYPE), UIModule.KEYBOARD_DEFAULT);
        }

        int typeModifiers = autocorrect | autoCapValue;
        int textTypeAndClass = typeModifiers;
        // For some reason you can't set both TYPE_CLASS_TEXT and TYPE_TEXT_FLAG_NO_SUGGESTIONS together.
        // Also, we need TYPE_CLASS_TEXT for passwords.
        if ((autocorrect != InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS || passwordMask) && type != UIModule.KEYBOARD_DECIMAL_PAD) {
            textTypeAndClass = textTypeAndClass | InputType.TYPE_CLASS_TEXT;
        }

        switch(type) {
            case UIModule.KEYBOARD_DEFAULT:
            case UIModule.KEYBOARD_ASCII:
                // Don't need a key listener, inputType handles that.
                break;
            case UIModule.KEYBOARD_NUMBERS_PUNCTUATION:
                textTypeAndClass |= (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_CLASS_TEXT);
                break;
            case UIModule.KEYBOARD_URL:
                Log.d(TAG, "Setting keyboard type URL-3", Log.DEBUG_MODE);
                searchView.setImeOptions(EditorInfo.IME_ACTION_GO);
                textTypeAndClass |= InputType.TYPE_TEXT_VARIATION_URI;
                break;
            case UIModule.KEYBOARD_DECIMAL_PAD:
                textTypeAndClass |= (InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
            case UIModule.KEYBOARD_NUMBER_PAD:
                textTypeAndClass |= InputType.TYPE_CLASS_NUMBER;
                break;
            case UIModule.KEYBOARD_PHONE_PAD:
                textTypeAndClass |= InputType.TYPE_CLASS_PHONE;
                break;
            case UIModule.KEYBOARD_EMAIL:
                textTypeAndClass |= InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
                break;
        }
        searchView.setInputType(textTypeAndClass);
    }

	@Override
	public void processProperties(KrollDict props) {
		super.processProperties(props);

		// Check if the hint text is specified when the view is created.
		if (props.containsKey(TiC.PROPERTY_HINT_TEXT)) {
			searchView.setQueryHint(props.getString(TiC.PROPERTY_HINT_TEXT));
		} 
		if (props.containsKey(TiC.PROPERTY_VALUE)) {
			searchView.setQuery(props.getString(TiC.PROPERTY_VALUE), false);
		} 
		if (props.containsKey(TiC.PROPERTY_ICONIFIED)) {
			searchView.setIconified(props.getBoolean(TiC.PROPERTY_ICONIFIED));
		} 
		if (props.containsKey(TiC.PROPERTY_ICONIFIED_BY_DEFAULT)) {
			searchView.setIconifiedByDefault(props.getBoolean(TiC.PROPERTY_ICONIFIED_BY_DEFAULT));
		}
		if (props.containsKey(TiC.PROPERTY_SUBMIT_ENABLED)) {
			searchView.setSubmitButtonEnabled((props.getBoolean(TiC.PROPERTY_SUBMIT_ENABLED)));			
		} 
        if (props.containsKey(TiC.PROPERTY_KEYBOARD_TYPE)
                || props.containsKey(TiC.PROPERTY_AUTOCORRECT)
                || props.containsKey(TiC.PROPERTY_AUTOCAPITALIZATION)) {
            handleKeyboard(props);
        }
      //the order is important because returnKeyType must overload keyboard return key defined
        // by keyboardType
        if (props.containsKey(TiC.PROPERTY_RETURN_KEY_TYPE)) {
            handleReturnKeyType(TiConvert.toInt(props.get(TiC.PROPERTY_RETURN_KEY_TYPE), UIModule.RETURNKEY_SEARCH));
        }
	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {

		if (key.equals(TiC.PROPERTY_HINT_TEXT)) {
			searchView.setQueryHint((String) newValue);
		}  else if (key.equals(TiC.PROPERTY_VALUE)) {
			searchView.setQuery((String) newValue, false);			
		} else if (key.equals(TiC.PROPERTY_ICONIFIED)) {
			searchView.setIconified(TiConvert.toBoolean(newValue));
		} else if (key.equals(TiC.PROPERTY_ICONIFIED_BY_DEFAULT)) {
			searchView.setIconifiedByDefault(TiConvert.toBoolean(newValue));
		} else if  (key.equals(TiC.PROPERTY_SUBMIT_ENABLED)) {
			searchView.setSubmitButtonEnabled(TiConvert.toBoolean(newValue));
		} else if (key.equals(TiC.PROPERTY_RETURN_KEY_TYPE)) {
            handleReturnKeyType(TiConvert.toInt(newValue));
		} else if (key.equals(TiC.PROPERTY_KEYBOARD_TYPE)
	            || (key.equals(TiC.PROPERTY_AUTOCORRECT) || key.equals(TiC.PROPERTY_AUTOCAPITALIZATION))) {
	            KrollDict d = proxy.getProperties();
	            handleKeyboard(d);
		} else {
			super.propertyChanged(key, oldValue, newValue, proxy);
		}
	}

    public void handleReturnKeyType(int type) {
        switch (type) {
        case UIModule.RETURNKEY_GO:
            searchView.setImeOptions(EditorInfo.IME_ACTION_GO);
            break;
        case UIModule.RETURNKEY_GOOGLE:
            searchView.setImeOptions(EditorInfo.IME_ACTION_GO);
            break;
        case UIModule.RETURNKEY_JOIN:
            searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
            break;
        case UIModule.RETURNKEY_NEXT:
            searchView.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            break;
        case UIModule.RETURNKEY_ROUTE:
            searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
            break;
        case UIModule.RETURNKEY_SEARCH:
            searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            break;
        case UIModule.RETURNKEY_YAHOO:
            searchView.setImeOptions(EditorInfo.IME_ACTION_GO);
            break;
        case UIModule.RETURNKEY_DONE:
            searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
            break;
        case UIModule.RETURNKEY_EMERGENCY_CALL:
            searchView.setImeOptions(EditorInfo.IME_ACTION_GO);
            break;
        case UIModule.RETURNKEY_DEFAULT:
            searchView.setImeOptions(EditorInfo.IME_ACTION_UNSPECIFIED);
            break;
        case UIModule.RETURNKEY_SEND:
            searchView.setImeOptions(EditorInfo.IME_ACTION_SEND);
            break;
        }

        // Set input type caches ime options, so whenever we change ime options,
        // we must reset input type
        searchView.setInputType(searchView.getInputType());
    }

	@Override
	public boolean onClose() {
		fireEvent(TiC.EVENT_CANCEL, null);
		return false;
	}

	@Override
	public boolean onQueryTextChange(String query) {
		proxy.setProperty(TiC.PROPERTY_VALUE, query);
		if (searchChangeListener != null) {
			searchChangeListener.filterBy(query);
		}
		fireEvent(TiC.EVENT_CHANGE, null);
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		TiUIHelper.showSoftKeyboard(nativeView, false);
		fireEvent(TiC.EVENT_SUBMIT, null);
		return false;
	}

	public void setOnSearchChangeListener(OnSearchChangeListener listener) {
		searchChangeListener = listener;
	}



}
