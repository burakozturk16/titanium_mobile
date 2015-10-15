package ti.modules.titanium.ui.widget;

import java.util.ArrayList;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUIHelper.FontDesc;
import org.appcelerator.titanium.view.TiUIView;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.v7.widget.AppCompatRadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class TiUIRadioGroup extends TiUIView implements OnCheckedChangeListener {
    RadioGroup radioGroup;
    private boolean ignoreChangeEvent = false;
    private FontDesc fontDesc = null;
    private int selectedColor, color, disabledColor;
    private boolean colorsSet = false;

    public TiUIRadioGroup(TiViewProxy proxy) {
        super(proxy);
        radioGroup = new RadioGroup(proxy.getActivity());
        layoutParams.sizeOrFillWidthEnabled = true;
        layoutParams.sizeOrFillHeightEnabled = true;
        radioGroup.setOnCheckedChangeListener(this);
        setNativeView(radioGroup);
    }
    
    protected static final ArrayList<String> KEY_SEQUENCE;
    static{
      ArrayList<String> tmp = new ArrayList<String>();
      tmp.add(TiC.PROPERTY_COLOR);
      tmp.add(TiC.PROPERTY_FONT);
      tmp.add(TiC.PROPERTY_BUTTONS);
      KEY_SEQUENCE = tmp;
    }
    @Override
    protected ArrayList<String> keySequence() {
        return KEY_SEQUENCE;
    }
    
    private AppCompatRadioButton createButton(final Context context, Object value, final ColorStateList colorStateList) {
        final AppCompatRadioButton result = new AppCompatRadioButton(context);
        if (value instanceof HashMap) {
            HashMap options = (HashMap) value;
            result.setText(TiConvert.toString(options, TiC.PROPERTY_TITLE));
        } else {
            result.setText(TiConvert.toString(value));
        }
        if (fontDesc != null) {
            TiUIHelper.styleText(result, fontDesc);
        }
        if (colorStateList != null) {
            result.setTextColor(colorStateList);
        }
        
        return result;
    }
    
    private void processButtons(final Context context, Object value) {
        if (!(value instanceof Object[])) {
            return;
        }
        ColorStateList colorStateList = null;
        radioGroup.removeAllViews();
        if (colorsSet) {
            int[][] states = new int[][] {
                    TiUIHelper.BACKGROUND_DISABLED_STATE, // disabled
                    TiUIHelper.BACKGROUND_SELECTED_STATE, // pressed
                    TiUIHelper.BACKGROUND_FOCUSED_STATE,  // pressed
                    TiUIHelper.BACKGROUND_CHECKED_STATE,  // pressed
                    new int [] {android.R.attr.state_pressed},  // pressed
                    new int [] {android.R.attr.state_focused},  // pressed
                    new int [] {}
                };

            colorStateList = new ColorStateList(
                states,
                new int[] {disabledColor, selectedColor, selectedColor, selectedColor, selectedColor, selectedColor, color}
            );
        }
        Object[] array = (Object[]) value;
        for (int i = 0; i < array.length; i++) {
            AppCompatRadioButton button = createButton(context, array[i], colorStateList);
            if (button != null) {
                button.setId(i);
                radioGroup.addView(button);
            }
        }
    }

    @Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {

        switch (key) {
        case TiC.PROPERTY_LAYOUT: {
            switch (TiConvert.toString(newValue)) {
            case TiC.LAYOUT_HORIZONTAL:
                radioGroup.setOrientation(RadioGroup.HORIZONTAL);
                break;
            case TiC.LAYOUT_VERTICAL:
            default:
                radioGroup.setOrientation(RadioGroup.VERTICAL);
                break;
            }
            break;
        }
        case TiC.PROPERTY_VALUE:
            ignoreChangeEvent = !changedProperty;
            check(TiConvert.toInt(newValue, -1));
            break;
        case TiC.PROPERTY_FONT:
            fontDesc = TiUIHelper.getFontStyle(getContext(),
                    TiConvert.toKrollDict(newValue));   
            break;
        case TiC.PROPERTY_COLOR:
            if (!colorsSet) {
                final AppCompatRadioButton button = new AppCompatRadioButton(
                        proxy.getActivity());
                color = disabledColor = selectedColor = button
                        .getCurrentTextColor();
                colorsSet = true;
            }
            color = selectedColor = disabledColor = color = TiConvert.toColor(newValue, this.color);
            break;
        case TiC.PROPERTY_SELECTED_COLOR:
            selectedColor = TiConvert.toColor(newValue, this.selectedColor);
            break;
        case TiC.PROPERTY_DISABLED_COLOR:
            disabledColor = TiConvert.toColor(newValue, this.disabledColor);
            break;
        case TiC.PROPERTY_BUTTONS:
            processButtons(proxy.getActivity(), newValue);
            break;
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (!ignoreChangeEvent) {
            proxy.setProperty(TiC.PROPERTY_VALUE, checkedId);
            if (hasListeners(TiC.EVENT_CHANGE)) {
                KrollDict data = new KrollDict();
                data.put(TiC.PROPERTY_VALUE, checkedId);
                fireEvent(TiC.EVENT_CHANGE, data, false, false);
            }
        }
        ignoreChangeEvent = false;

    }
    
    public void check(int id) {
        radioGroup.check(id);
    }
}
