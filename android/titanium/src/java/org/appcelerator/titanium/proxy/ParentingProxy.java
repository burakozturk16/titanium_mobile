package org.appcelerator.titanium.proxy;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollRuntime;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiConvert;

import android.app.Activity;

@Kroll.proxy
public class ParentingProxy extends KrollProxy {

    protected ArrayList<KrollProxy> children;
    protected WeakReference<ParentingProxy> parent;
    protected WeakReference<KrollProxy> parentForBubbling;
    protected HashMap<String, KrollProxy> holdedProxies = null;
    private static final String TAG = "ParentingProxy";
    protected boolean shouldAskForGC = true; 

    @Override
    public void handleCreationDict(KrollDict options) {
        boolean needsToUpdateProps = false;
        if (options == null) {
            return;
        }
        if (options.containsKey(TiC.PROPERTY_PROPERTIES)) {
            super.handleCreationDict(options
                    .getKrollDict(TiC.PROPERTY_PROPERTIES));
            needsToUpdateProps = true;
        } else {
            super.handleCreationDict(options);
        }
        if (options.containsKey(TiC.PROPERTY_CHILD_TEMPLATES)
                || options.containsKey(TiC.PROPERTY_EVENTS)) {
            initFromTemplate(options, this, true, true);
        }
        if (needsToUpdateProps) {
            updateKrollObjectProperties();
        }
        setReadyToUpdateNativeSideProperties(true);
    }

    public void reloadProperties() {
        super.reloadProperties();
        for (KrollProxy p : getChildren()) {
            p.reloadProperties();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void initFromTemplate(HashMap template_, KrollProxy rootProxy,
            boolean updateKrollProperties, boolean recursive) {
        super.initFromTemplate(template_, rootProxy, updateKrollProperties,
                recursive);
        if (recursive && template_.containsKey(TiC.PROPERTY_CHILD_TEMPLATES)) {
            Object childProperties = template_
                    .get(TiC.PROPERTY_CHILD_TEMPLATES);
            if (childProperties instanceof Object[]) {
                Object[] propertiesArray = (Object[]) childProperties;
                for (int i = 0; i < propertiesArray.length; i++) {
                    Object childDict = propertiesArray[i];
                    if (childDict instanceof TiViewProxy) {
                        TiViewProxy child = (TiViewProxy) childDict;
                        String bindId = TiConvert.toString(
                                child.getProperty(TiC.PROPERTY_BIND_ID), null);
                        if (bindId != null) {
                            rootProxy.addBinding(bindId, child);
                        }
                        addProxy(child, -1);
                    } else {
                        KrollProxy childProxy = createProxyFromTemplate(
                                (HashMap) childDict, rootProxy,
                                updateKrollProperties);
                        if (childProxy != null) {
                            if (updateKrollProperties)
                                childProxy.updateKrollObjectProperties();
                            addProxy(childProxy, -1);
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
    @Kroll.getProperty
    @Kroll.method
    public ParentingProxy getParent() {
        if (this.parent == null) {
            return null;
        }

        return this.parent.get();
    }

    public void setParent(ParentingProxy parent) {
        if (parent == null) {
            this.parent = null;
            this.parentForBubbling = null;
            return;
        }

        this.parent = new WeakReference<ParentingProxy>(parent);
    }
    
    public void setParentForBubbling(KrollProxy parent) {
        if (parent == null) {
            this.parentForBubbling = null;
            return;
        }

        this.parentForBubbling = new WeakReference<KrollProxy>(parent);
    }
    
    @Override
    public KrollProxy getParentForBubbling() {
        if (this.parentForBubbling == null) {
            return getParent();
        }
        return this.parentForBubbling.get();
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
        int realIndex = index;
        synchronized (children) {
            children.remove(child);
        }
        if(index < 0 || index > children.size()) {
            realIndex = children.size();
        }
        synchronized (children) {
            children.add(realIndex, child);
        }

        if (child instanceof ParentingProxy) {
            ((ParentingProxy) child).parent = new WeakReference<ParentingProxy>(
                    this);
        }
        handleChildAdded(child, realIndex);

    }

    /**
     * Adds a child to this view proxy.
     * 
     * @param child
     *            The child view proxy to add.
     * @module.api
     */
    @Kroll.method
    public void add(Object args, @Kroll.argument(optional = true) Object index) {
        
        if (args == null) return;
        if (args instanceof Object[]) {
            boolean oldValue  = isReadyToUpdateNativeSideProperties();
            setReadyToUpdateNativeSideProperties(false);
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
            setReadyToUpdateNativeSideProperties(oldValue);
            return;
        } else {
            KrollProxy child = null;
            if (args instanceof HashMap) {
                child = createProxyFromTemplate((HashMap) args, this, true);
                if (child != null) {
                    child.updateKrollObjectProperties();
                }
            } else {
                child = (KrollProxy) args;
                String bindId = TiConvert.toString(
                        child.getProperty(TiC.PROPERTY_BIND_ID), null);
                if (bindId != null) {
                    addBinding(bindId, child);
                }
            }
            if (child != null) {
                int i = -1; // default to top
                if (index instanceof Number) {
                    i = ((Number) index).intValue();
                }
                addProxy(child, i);
                updatePropertiesNativeSide();
            }
        }
    }

    @Kroll.method
    public void replaceAt(Object params) {
        if (!(params instanceof HashMap)) {
            Log.e(TAG, "Argument for replaceAt must be a dictionary");
            return;
        }
        @SuppressWarnings("rawtypes")
        HashMap options = (HashMap) params;
        Integer position = -1;
        if (options.containsKey("position")) {
            position = (Integer) options.get("position");
        }
        if (children != null) {
            synchronized (children) {
                if (children.size() > position) {
                    KrollProxy childToRemove = children.get(position);
                    insertAt(params);
                    remove(childToRemove);
                    insertAt(params);
                }
            }
        }
    }

    /**
     * Adds a child to this view proxy in the specified position. This is useful
     * for "vertical" and "horizontal" layouts.
     * 
     * @param params
     *            A Dictionary containing a TiViewProxy for the view and an int
     *            for the position
     * @module.api
     */
    @Kroll.method
    public void insertAt(Object params) {
        if (!(params instanceof HashMap)) {
            Log.e(TAG, "Argument for insertAt must be a dictionary");
            return;
        }
        @SuppressWarnings("rawtypes")
        HashMap options = (HashMap) params;
        add(options.get("view"), options.get("position"));
    }

    public void add(KrollProxy child) {
        add(child, Integer.valueOf(-1));
    }

    protected void removeProxy(Object args, final boolean shouldDetach) {
        KrollProxy child = null;
        if (args instanceof KrollProxy)
            child = (KrollProxy) args;
        if (child == null) {
            Log.e(TAG, "Add called with a null child");
            return;
        }
        int index = -1;
        if (children != null) {
            synchronized (children) {
                index = children.indexOf(child);
                if (index >= 0) {
                    children.remove(index);
                }
                if (child instanceof ParentingProxy) {
                    ((ParentingProxy) child).setParent(null);
                }
            }
        }
        handleChildRemoved(child, index, shouldDetach);
    }

    public void removeProxy(Object args) {
        removeProxy(args, true);
    }

    /**
     * Removes a view from this view proxy, releasing the underlying native view
     * if it exists.
     * 
     * @param child
     *            The child to remove.
     * @module.api
     */
    @Kroll.method
    public void remove(final Object args) {
        if (args == null) {
            Log.e(TAG, "remove called with null");
            return;
        }
//        if (!TiApplication.isUIThread()) {
//            getActivity().runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    remove(args);
//                }
//            });
//            return;
//        }
        
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
     * 
     * @module.api
     */
    @Kroll.method
    public void removeFromParent() {
        if (parent != null) {
            getParent().remove(this);
        }
    }

    /**
     * Removes all children views.
     * 
     * @module.api
     */
    @Kroll.method
    public void removeAllChildren() {
        if (children != null) {
            // children might be altered while we loop through it (threading)
            // so we first copy children as it was when asked to remove all
            // children
            ArrayList<KrollProxy> childViews = new ArrayList<KrollProxy>();
            synchronized (children) {
                childViews.addAll(children);
                children.clear();
            }
            for (int i = 0; i < childViews.size(); i++) {
                handleChildRemoved(childViews.get(i), i, true);
            }
        }
    }

    protected void handleChildAdded(final KrollProxy child, final int index) {
        if (!TiApplication.isUIThread()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    child.setActivity(getActivity());
                }
            });
            return;
        }
        child.setActivity(getActivity());
    }

    protected void handleChildRemoved(final KrollProxy child, final int index,
            final boolean shouldDetach) {
        if (child == null) return;
        if (!TiApplication.isUIThread()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handleChildRemoved(child, shouldDetach);
                }
            });
            return;
        }
        if (shouldDetach && child instanceof TiViewProxy) {
            ((TiViewProxy) child).releaseViews(shouldDetach);
        }
        child.setActivity(null);
        if (child instanceof ParentingProxy) {
            ((ParentingProxy) child).setParent(null);
        }
        KrollRuntime.suggestGC();
    }
    
    protected void handleChildRemoved(KrollProxy child, final boolean shouldDetach) {
        handleChildRemoved(child, -1, shouldDetach);
    }

    @Override
    public void setActivity(Activity activity) {
        super.setActivity(activity);
        if (children != null) {
            synchronized (children) {
                for (KrollProxy child : children) {
                    child.setActivity(activity);
                }
            }
        }
        if (holdedProxies != null) {
            Iterator it = holdedProxies.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry)it.next();
                ((KrollProxy) pairs.getValue()).setActivity(activity);
            }
        }
    }

    /**
     * @return An array of the children proxies of this view.
     * @module.api
     */
    @Kroll.getProperty
    @Kroll.method
    public KrollProxy[] getChildren() {
        if (children == null)
            return new KrollProxy[0];
        synchronized (children) {
            return children.toArray(new KrollProxy[children.size()]);
        }
    }
    
    public int getChildrenCount() {
        if (children == null)
            return 0;
        synchronized (children) {
            return children.size();
        }
    }

    @Override
    public void release() {
        super.release();
        activity = null;
        if (children != null) {
            synchronized (children) {
                for (KrollProxy child : children) {
                    child.release();
                }
            }
        }
        if (holdedProxies != null) {
            Iterator it = holdedProxies.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry)it.next();
                ((KrollProxy) pairs.getValue()).release();
            }
        }
    }
    
    
    public void removeHoldedProxy(final String key) {
        if (key != null && holdedProxies != null && holdedProxies.containsKey(key)) {
            handleChildRemoved(holdedProxies.remove(key), true);
        }
    }
    
    public KrollProxy getHoldedProxy(final String key) {
        if (holdedProxies != null) {
            return holdedProxies.get(key);
        }
        return null;
    }
    
    public KrollProxy addProxyToHold(final Object arg, final String key) {
        return addProxyToHold(arg, key, true, false);
    }
    
    public KrollProxy addProxyToHold(final Object arg, final String key, final boolean setParent, final boolean setParentBubbling) {
        if (arg instanceof KrollProxy) {
            if (holdedProxies == null) {
                holdedProxies = new HashMap<String, KrollProxy>();
            }
            final KrollProxy newOne =  (KrollProxy) arg;
            if (holdedProxies.containsKey(key)) {
                final KrollProxy oldOne =  holdedProxies.get(key);
                if (oldOne.equals(arg)) {
                    return oldOne;
                }
                handleChildRemoved(oldOne, true);

            }
            holdedProxies.put(key, newOne);
            if (newOne instanceof ParentingProxy) {
                if (setParent) {
                    ((ParentingProxy) newOne).setParent(this);
                } else if (setParentBubbling) {
                    ((ParentingProxy) newOne).setParentForBubbling(this);
                }
            }
            
            return newOne;
        } else if (arg instanceof HashMap) {
            removeHoldedProxy(key);
            KrollProxy obj = createProxyFromTemplate((HashMap) arg, this, true);
            if (obj != null) {
                obj.updateKrollObjectProperties();
                return addProxyToHold(obj, key);
            }
        } else if(arg == null) {
            removeHoldedProxy(key);
        }
        return null;
    }
}
