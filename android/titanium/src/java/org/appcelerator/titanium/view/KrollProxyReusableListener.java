package org.appcelerator.titanium.view;

//import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxyListener;

public interface KrollProxyReusableListener extends KrollProxyListener {
    
    //ListView implementation
    public void setReusing(boolean value);
//    public void setAdditionalEventData(KrollDict dict);
//    public KrollDict getAdditionalEventData();
}
