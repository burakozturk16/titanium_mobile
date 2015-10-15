package ti.modules.titanium.ui.android;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.TiUIRadioGroup;
import android.app.Activity;

@Kroll.proxy(creatableInModule = AndroidModule.class, propertyAccessors = {
    TiC.PROPERTY_VALUE,
    TiC.PROPERTY_BUTTONS 
    })
public class RadioGroupProxy extends TiViewProxy {

    public RadioGroupProxy()
    {
        super();
        defaultValues.put(TiC.PROPERTY_VALUE, false);
    }
    
    @Override
    public TiUIView createView(Activity activity) {
        return new TiUIRadioGroup(this);
    }
    @Override
    public String getApiName()
    {
        return "Ti.UI.Android.RadioGroup";
    }
    
    private TiUIRadioGroup getRadioGroup() {
        return (TiUIRadioGroup) peekView();
    }
    
    @Kroll.method(runOnUiThread=true)
    public void check(Object value) {
        int id = TiConvert.toInt(value, -1);
        TiUIRadioGroup view = getRadioGroup();
        if (view != null) {
            view.check(id);
        } else {
            setProperty(TiC.PROPERTY_VALUE, id);
        }
    }
    
    @Kroll.method(runOnUiThread=true)
    public void clear() {
        check(-1);
    }
}

