package org.appcelerator.titanium.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollPropertyChange;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.view.KrollProxyReusableListener;

@Kroll.proxy
public class ReusableProxy extends ParentingProxy implements KrollProxyReusableListener {
    public ReusableProxy() {
        super();
        setModelListener(this);
    }

    @Override
    public void setReusing(boolean reusing) {
    }

    @Override
    public void listenerAdded(String arg0, int arg1, KrollProxy arg2) {
    }

    @Override
    public void listenerRemoved(String arg0, int arg1, KrollProxy arg2) {
    }

    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
    }
    @Override
    public void processApplyProperties(KrollDict d) {
        aboutToProcessProperties(d);
        handleProperties(d, true);
        didProcessProperties();
    }
    
    @Override
    public void processProperties(KrollDict d)
    {
        aboutToProcessProperties(d);
        handleProperties(d, false);
        didProcessProperties();

    }
    
    protected void aboutToProcessProperties(KrollDict d) {
    }
    protected void didProcessProperties() {
    }
    
    @Override
    public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy)
    {
        propertySet(key, newValue, oldValue, true);
        didProcessProperties();
    }

    @Override
    public void propertiesChanged(List<KrollPropertyChange> changes, KrollProxy proxy)
    {
        handleProperties(changes, true);
        didProcessProperties();
    }
    
    protected ArrayList<String> keySequence() {
        return null;
    }
    
    protected void handleProperties(KrollDict d, final boolean changed) {
        if (keySequence() != null) {
            for (final String key : keySequence()) {
                if (d.containsKey(key)) {
                    propertySet(key, d.get(key), getProperty(key), changed);
                    d.remove(key);
                }
            }
        }
        for (Map.Entry<String, Object> entry : d.entrySet()) {
            final String key = entry.getKey();
            propertySet(key, entry.getValue(), getProperty(key), changed);
        }
    }
    
    protected void handleProperties(List<KrollPropertyChange> changes, final boolean changed) {
        KrollDict d = new KrollDict();
        for (KrollPropertyChange change : changes) {
            d.put(change.getName(), change.getNewValue());
        }
        handleProperties(d, changed);
    }
}
