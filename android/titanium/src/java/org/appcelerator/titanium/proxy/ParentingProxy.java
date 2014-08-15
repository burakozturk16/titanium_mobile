package org.appcelerator.titanium.proxy;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiConvert;

import android.app.Activity;

@Kroll.proxy
public class ParentingProxy extends KrollProxy {
    
    protected ArrayList<KrollProxy> children;
    protected WeakReference<ParentingProxy> parent;
    private static final String TAG = "ParentingProxy";

    @Override
    public void handleCreationDict(KrollDict options)
    {
        boolean needsToUpdateProps = false;
        if (options == null) {
            return;
        }
        if (options.containsKey(TiC.PROPERTY_PROPERTIES)) {
            super.handleCreationDict(options.getKrollDict(TiC.PROPERTY_PROPERTIES));
            needsToUpdateProps = true;
        }
        else {
            super.handleCreationDict(options);
        }
        if (options.containsKey(TiC.PROPERTY_CHILD_TEMPLATES) || options.containsKey(TiC.PROPERTY_EVENTS)) {
            initFromTemplate(options, this, true, true);
        }
        if (needsToUpdateProps) {
            updateKrollObjectProperties();
        }
        else {
            //we don't need to update them all, bindings might be there though
            updatePropertiesNativeSide();
        }
    }
    
    public void reloadProperties()
    {
        super.reloadProperties();
        // Use a copy so bundle can be modified as it passes up the inheritance
        // tree. Allows defaults to be added and keys removed.
        if (children != null) {
            try {
                for (KrollProxy p : children) {
                    p.reloadProperties();
                }
            } catch (ConcurrentModificationException e) {
                Log.e(TAG , e.getMessage(), e);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void initFromTemplate(HashMap template_,
            KrollProxy rootProxy, boolean updateKrollProperties, boolean recursive) {
        super.initFromTemplate(template_, rootProxy, updateKrollProperties, recursive);
        if (recursive && template_.containsKey(TiC.PROPERTY_CHILD_TEMPLATES)) {
            Object childProperties = template_
                    .get(TiC.PROPERTY_CHILD_TEMPLATES);
            if (childProperties instanceof Object[]) {
                Object[] propertiesArray = (Object[]) childProperties;
                for (int i = 0; i < propertiesArray.length; i++) {
                    Object childDict = propertiesArray[i];
                    if (childDict instanceof TiViewProxy) {
                        TiViewProxy child = (TiViewProxy) childDict;
                        String bindId = TiConvert.toString(child.getProperty(TiC.PROPERTY_BIND_ID) , null);
                        if (bindId != null) {
                            rootProxy.addBinding(bindId, child);
                        }
                        this.add(child);
                    } else {
                        KrollProxy childProxy = createProxyFromTemplate(
                                (HashMap) childDict, rootProxy, updateKrollProperties);
                        if (childProxy != null){
                            if (updateKrollProperties) childProxy.updateKrollObjectProperties();
                            this.add(childProxy);
                        }
                    }
                }
            }
        }
    }
    
    
    /**
     * @return The parent view proxy of this view proxy.
     * @module.api
     */
    @Kroll.getProperty @Kroll.method
    public ParentingProxy getParent()
    {
        if (this.parent == null) {
            return null;
        }

        return this.parent.get();
    }

    public void setParent(ParentingProxy parent)
    {
        if (parent == null) {
            this.parent = null;
            return;
        }

        this.parent = new WeakReference<ParentingProxy>(parent);
    }
    
    protected void addProxy(Object args, final int index) {
        KrollProxy child = null;
        if (args instanceof KrollProxy)
            child = (KrollProxy) args;
        if (child == null) {
            Log.e(TAG, "Add called with a null child");
            return;
        }
        if (children == null) {
            children = new ArrayList<KrollProxy>();
        }
        children.remove(child);
        if (index >= 0) {
            children.add(index, child);
        }
        else {
            children.add(child);
        }
        if (child instanceof ParentingProxy) {
            ((ParentingProxy)child).parent = new WeakReference<ParentingProxy>(this);
        }
        handleChildAdded(child, index);
        
    }
    /**
     * Adds a child to this view proxy.
     * @param child The child view proxy to add.
     * @module.api
     */
    @Kroll.method
    public void add(Object args, @Kroll.argument(optional = true) Object index)
    {
        if (args instanceof Object[]) {
            int i = -1; // no index by default
            if (index instanceof Number) {
                i = ((Number) index).intValue();
            }
            int arrayIndex = i;
            for (Object obj : (Object[]) args) {
                add(obj, Integer.valueOf(arrayIndex));
                if (arrayIndex != -1)
                    arrayIndex++;
            }
            return;
        } else {
            KrollProxy child = null;
            if (args instanceof HashMap) {
                child = createProxyFromTemplate((HashMap) args,
                        this, true);
                if (child != null) {
                    child.updateKrollObjectProperties();
                }
            } else {
                child = (KrollProxy) args;
                String bindId = TiConvert.toString(child.getProperty(TiC.PROPERTY_BIND_ID) , null);
                if (bindId != null) {
                    addBinding(bindId, child);
                }
            }
            if (child != null) {
                int i = -1; // no index by default
                if (index instanceof Number) {
                    i = ((Number)index).intValue();
                }
                addProxy(child, i);
            }
            
        }
    }
    
    @Kroll.method
    public void replaceAt(Object params)
    {
        if (!(params instanceof HashMap)) {
            Log.e(TAG, "Argument for replaceAt must be a dictionary");
            return;
        }
        @SuppressWarnings("rawtypes")
        HashMap options = (HashMap) params;
        Integer position = -1;
        if(options.containsKey("position")) {
            position = (Integer) options.get("position");
        }
        if(children != null && children.size() > position) {
            KrollProxy childToRemove = children.get(position);
            insertAt(params);
            remove(childToRemove);
            insertAt(params);
        }
    }
    

    /**
     * Adds a child to this view proxy in the specified position. This is useful for "vertical" and
     * "horizontal" layouts.
     * @param params A Dictionary containing a TiViewProxy for the view and an int for the position 
     * @module.api
     */
    @Kroll.method
    public void insertAt(Object params)
    {
        if (!(params instanceof HashMap)) {
            Log.e(TAG, "Argument for insertAt must be a dictionary");
            return;
        }
        @SuppressWarnings("rawtypes")
        HashMap options = (HashMap) params;
        add(options.get("view"), options.get("position"));
    }

    public void add(KrollProxy child)
    {
        add(child, Integer.valueOf(-1));
    }
    
    protected void removeProxy(Object args)
    {
        KrollProxy child = null;
        if (args instanceof KrollProxy)
            child = (KrollProxy) args;
        if (child == null) {
            Log.e(TAG, "Add called with a null child");
            return;
        }
        if (children != null) {
            children.remove(child);
            if (child instanceof ParentingProxy) {
                ((ParentingProxy) child).setParent(null);
            }
        }
        handleChildRemoved(child);
    }
    /**
     * Removes a view from this view proxy, releasing the underlying native view if it exists.
     * @param child The child to remove.
     * @module.api
     */
    @Kroll.method
    public void remove(Object args)
    {
        if (args == null) {
            Log.e(TAG, "remove called with null");
            return;
        }
        if (args instanceof Object[]) {
            for (Object obj : (Object[]) args) {
                remove(obj);
            }
            return;
        }
        removeProxy(args);
    }

    /**
     * tries to remove this view from it parent
     * @module.api
     */
    @Kroll.method
    public void removeFromParent()
    {
        if (parent != null) {
            getParent().remove(this);
        }
    }

    /**
     * Removes all children views.
     * @module.api
     */
    @Kroll.method
    public void removeAllChildren()
    {
        if (children != null) {
            //children might be altered while we loop through it (threading)
            //so we first copy children as it was when asked to remove all children
            ArrayList<KrollProxy> childViews = new ArrayList<KrollProxy>();
            childViews.addAll(children);
            children.clear();
            for (KrollProxy child : childViews) {
                handleChildRemoved(child);
            }
        }
    }
    
    protected void handleChildAdded(KrollProxy child, int index) {
        
    }
    protected void handleChildRemoved(KrollProxy child) {
        
    }
    

    @Override
    public void setActivity(Activity activity)
    {
        super.setActivity(activity);
        if (children != null) {
            for (KrollProxy child : children) {
                child.setActivity(activity);
            }
        }
    }
    

    /**
     * @return An array of the children proxies of this view.
     * @module.api
     */
    @Kroll.getProperty @Kroll.method
    public KrollProxy[] getChildren()
    {
        if (children == null) return new KrollProxy[0];
        return children.toArray(new KrollProxy[children.size()]);
    }
    
    @Override
    public void release()
    {
        super.release();
        if (children != null) {
            for (KrollProxy child : children) {
                child.release();
            }
        }
    }

}
