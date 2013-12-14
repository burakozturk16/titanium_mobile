/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollPropertyChange;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollProxyListener;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiActivityWindow;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;
import org.appcelerator.titanium.TiLifecycle.interceptOnHomePressedEvent;
import org.appcelerator.titanium.TiLifecycle.interceptOnBackPressedEvent;
import org.appcelerator.titanium.TiWindowManager;
import org.appcelerator.titanium.proxy.ActionBarProxy;
import org.appcelerator.titanium.proxy.ActivityProxy;
import org.appcelerator.titanium.proxy.TiWindowProxy;
import org.appcelerator.titanium.transition.Transition;
import org.appcelerator.titanium.transition.TransitionHelper;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;

import com.actionbarsherlock.app.ActionBar;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.AnimatorSet;

import ti.modules.titanium.ui.transitionstyle.TransitionStyleModule;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Message;
import android.util.Pair;
import android.view.View;
import android.view.ViewParent;

import android.view.ViewGroup;

@SuppressWarnings({"unchecked", "rawtypes", "serial"})
@SuppressLint({ "ValidFragment"})
@Kroll.proxy(creatableInModule=UIModule.class, propertyAccessors={
	TiC.PROPERTY_MODAL,
	TiC.PROPERTY_WINDOW,
	TiC.PROPERTY_ACTIVITY,
	TiC.PROPERTY_URL,
	TiC.PROPERTY_WINDOW_PIXEL_FORMAT
})
public class NavigationWindowProxy extends WindowProxy implements OnLifecycleEvent, TiActivityWindow, interceptOnBackPressedEvent, TiWindowManager, interceptOnHomePressedEvent, KrollProxyListener
{
	private static final String TAG = "NavigationWindowProxy";

	private static final int MSG_FIRST_ID = WindowProxy.MSG_LAST_ID + 1;
	private static final int MSG_PUSH = MSG_FIRST_ID + 1;
	private static final int MSG_POP = MSG_FIRST_ID + 2;
	protected static final int MSG_LAST_ID = MSG_FIRST_ID + 100;


	private ArrayList<TiWindowProxy> preAddedWindows = new ArrayList<TiWindowProxy>();
	private ArrayList<TiWindowProxy> windows = new ArrayList<TiWindowProxy>();
	private HashMap<TiWindowProxy, Transition> animations = new HashMap<TiWindowProxy, Transition>();
	private HashMap defaultTransition = kDefaultTransition;
	
	public NavigationWindowProxy()
	{
		super();
		setModelListener(this, false);
	}

	@Override
	public void open(@Kroll.argument(optional = true) Object arg)throws IllegalArgumentException
	{
		
		if (!hasProperty(TiC.PROPERTY_WINDOW) || !(getProperty(TiC.PROPERTY_WINDOW) instanceof WindowProxy)) {
			throw new IllegalArgumentException("You must set a 'window' property");
		}

		super.open(arg);
	}

	@Override
	public void close(@Kroll.argument(optional = true) Object arg)
	{
		if (!(opened || opening)) {
			return;
		}
		super.close(arg);
	}
	
	@Override
	public boolean interceptOnHomePressed() {
		if (pushing || poping) return true;
		ActivityProxy activityProxy = getActivityProxy();
		if (activityProxy != null) {
			ActionBarProxy actionBarProxy = activityProxy.getActionBar();
			if (actionBarProxy != null) {
				KrollFunction onHomeIconItemSelected = (KrollFunction) actionBarProxy
					.getProperty(TiC.PROPERTY_ON_HOME_ICON_ITEM_SELECTED);
				if (onHomeIconItemSelected != null) {
					KrollDict event = new KrollDict();
					event.put(TiC.EVENT_PROPERTY_SOURCE, actionBarProxy);
					event.put(TiC.EVENT_PROPERTY_WINDOW, ((TiBaseActivity) getActivity()).getWindowProxy());
					onHomeIconItemSelected.call(activityProxy.getKrollObject(), new Object[] { event });
					return true;
				}
			}
		}
		popCurrentWindow(null);
		return true;
	}
	
	private void updateHomeButton(TiWindowProxy proxy){
		boolean canGoBack = (windows.size() > 1);
	    	ActionBarProxy actionBarProxy = getActivityProxy().getActionBar();
	    	ActionBar actionBar = ((TiBaseActivity)getActivity()).getSupportActionBar();
	    	if (actionBar == null) return;
	    	if (proxy == null) {
	    		actionBar.setDisplayHomeAsUpEnabled(canGoBack);
	    		actionBar.setHomeButtonEnabled(canGoBack);
	    	}
	    	else {
	    		KrollDict props = actionBarProxy.getProperties();
	    		if (props == null) props = new KrollDict();
	    		actionBar.setDisplayHomeAsUpEnabled(props.optBoolean(TiC.PROPERTY_DISPLAY_HOME_AS_UP, canGoBack));
	    		actionBar.setHomeButtonEnabled(canGoBack || props.get(TiC.PROPERTY_ON_HOME_ICON_ITEM_SELECTED) != null);
	    		
	    	}
	}
	
	private void removeWindow(TiWindowProxy proxy) {
		proxy.setWindowManager(null);
		windows.remove(proxy);
		animations.remove(proxy);
	}
	private void addWindow(TiWindowProxy proxy, Transition transition) {
		if (!windows.contains(proxy)) {
			windows.add(proxy);
		}
		proxy.setWindowManager(this);
		if (transition != null) animations.put(proxy, transition);
	}
	
	public TiWindowProxy getCurrentWindow()
	{
		int size = windows.size();
		if (size > 0) return windows.get(size - 1);
		return this;
	}
	
	public TiWindowProxy popWindow()
	{
		int size = windows.size();
		if (size > 0) return windows.remove(size - 1);
		return null;
	}

	private boolean pushing = false;
	private boolean poping = false;
	
	
	private boolean popUpToWindow(final TiWindowProxy winToFocus, Object arg) {
		if (!opened || opening) {
			int index = preAddedWindows.indexOf(winToFocus);
			int size = preAddedWindows.size();
			if (size > 0 && index != -1) {
				preAddedWindows.subList(index + 1, size).clear();
			}
			poping = false;
			return true;
		}
		int index = windows.indexOf(winToFocus);
		int size = windows.size();
		if (index == -1 || index >= size - 1) {
			poping = false;
			return true;
		}
		TiWindowProxy toRemove = popWindow();
		size = windows.size();
		ViewGroup viewToRemoveFrom = (ViewGroup) getParentViewForChild();
		for (int i = index + 1; i < size; i++) {
			TiWindowProxy window = windows.get(i);
			View viewToRemove = window.getOuterView();
			if (viewToRemove != null) {
				viewToRemoveFrom.removeView(window.getOuterView());
			}
			window.closeFromActivity(false);
			window.setActivity(null);
			animations.remove(window);
		}
		windows.subList(index + 1, size).clear();
		return transitionFromWindowToWindow(toRemove, winToFocus, arg);
	}
	
	public boolean popWindow(TiWindowProxy proxy, Object arg)
	{
		if (!opened || opening) {
			preAddedWindows.remove(proxy);
			return true;
		}
		int index = windows.indexOf(proxy);
		if (index >=1 && windows.size() > 1){
			int realIndex = Math.max(0, index - 1);
			TiWindowProxy winToFocus = windows.get(realIndex);
			return popUpToWindow(winToFocus, arg);
		}
		else {
			poping = false;		
			return true;
		}
	}
	private void handleWindowClosed(TiWindowProxy toRemove) 
	{
		poping = false;
		if (toRemove == this) return;
		removeWindow(toRemove);
		View view = toRemove.getOuterView();
		if (view != null) {
			ViewParent parent = view.getParent();
			if (parent != null) {
				((ViewGroup)parent).removeView(view);
			}
		}
		TiBaseActivity activity = ((TiBaseActivity) getActivity());	
		if (activity != null) {
			activity.removeWindowFromStack(toRemove);
		}
		else {
			Log.e(TAG, "handleWindowClosed called with not activity, shouldn't be possible");
		}
		toRemove.setActivity(null);
		toRemove.closeFromActivity(true);
	}
	
	public boolean popCurrentWindow(Object arg)
	{
		if (windows.size() > 1) //we dont pop the window 0!
		 {
			final TiWindowProxy toRemove = popWindow();
			TiWindowProxy winToFocus = getCurrentWindow();
			return transitionFromWindowToWindow(toRemove, winToFocus, arg);
		 }
		poping = false;
		return false;
	}
	
	public boolean transitionFromWindowToWindow(final TiWindowProxy toRemove, TiWindowProxy winToFocus, Object arg)
	{
		Transition transition = null;
		if (animations.containsKey(toRemove)) {
			transition = animations.get(toRemove);
		}
		boolean animated = true;
		if (arg != null && arg instanceof HashMap<?, ?>) {
			animated = TiConvert.toBoolean((HashMap) arg, TiC.PROPERTY_ANIMATED, animated);
		}
		if (animated) {
			transition = TransitionHelper.transitionFromObject((HashMap) ((arg != null)?((HashMap)arg).get(TiC.PROPERTY_TRANSITION):null), null, transition);
			if (transition != null) transition.setReversed(!transition.isReversed());
		}
		
		if (hasListeners("closeWindow")) {
			KrollDict options = new KrollDict();
			options.put(TiC.PROPERTY_WINDOW, winToFocus);
			options.put("stackIndex", windows.indexOf(winToFocus));
			options.put(TiC.PROPERTY_ANIMATED, animated);
			options.put(TiC.PROPERTY_TRANSITION, getDictFromTransition(transition));
			fireEvent("closeWindow", options);
		}
		
		if (!opened || opening) {
			handleWindowClosed(toRemove);
			return true;
		}
					
		final ViewGroup viewToRemoveFrom = (ViewGroup) getParentViewForChild();
		
		if (viewToRemoveFrom != null) {
			final View viewToRemove = toRemove.getOuterView();
			final View viewToFocus = winToFocus.getOrCreateView().getOuterView();
			viewToFocus.setVisibility(View.GONE);
			TiUIHelper.addView(viewToRemoveFrom, viewToFocus, winToFocus.peekView().getLayoutParams());
			if (transition != null && animated) {
				transition.setTargets(viewToRemoveFrom, viewToFocus, viewToRemove);
				AnimatorSet set = transition.getSet(new AnimatorListener() {
					@Override
					public void onAnimationStart(Animator arg0) {	
					}
					
					@Override
					public void onAnimationRepeat(Animator arg0) {							
					}
					
					@Override
					public void onAnimationEnd(Animator arg0) {	
						handleWindowClosed(toRemove);
					}

					@Override
					public void onAnimationCancel(Animator arg0) {		
						handleWindowClosed(toRemove);
					}
				});
				set.start();
			}
			else {
				handleWindowClosed(toRemove);
			}
			viewToFocus.setVisibility(View.VISIBLE);
		}
		
		TiBaseActivity activity = ((TiBaseActivity) getActivity());	
		if (activity != null) activity.setWindowProxy(winToFocus);
    	updateHomeButton(winToFocus);
		winToFocus.focus();

		return true;
	}
	
	@Override
	public void setActivity(Activity activity)
	{
		TiBaseActivity oldActivity = (TiBaseActivity) getActivity();
		TiBaseActivity newActivity = (TiBaseActivity) activity;
		Log.d(TAG, "setActivity :" + newActivity);
		if (newActivity == oldActivity) return;
		super.setActivity(activity);
		
		if (oldActivity != null) {
			oldActivity.removeOnLifecycleEventListener(this);
			oldActivity.removeInterceptOnBackPressedEventListener(this);
			oldActivity.removeInterceptOnHomePressedEventListener(this);
		}
		
		if (newActivity != null) {
			newActivity.addOnLifecycleEventListener(this);
			newActivity.addInterceptOnBackPressedEventListener(this);
			newActivity.addInterceptOnHomePressedEventListener(this);
		}
	}
	
	private void handlePushFirst (){
		if (windows.size() == 0) {
			TiWindowProxy firstWindow = (WindowProxy)getProperty(TiC.PROPERTY_WINDOW);
			if (preAddedWindows.size() > 0 ) {
				addWindow(firstWindow, null);
				for (int i = 0; i < preAddedWindows.size() - 1; i++) {
					windows.add(preAddedWindows.get(i));
				}
				firstWindow = preAddedWindows.get(preAddedWindows.size() - 1);
				preAddedWindows.clear();
			}			
			handlePush(firstWindow, true, null);

		}
	}

	private static int viewId = 10000;
	@Override
	public void onWindowActivityCreated()
	{
		
		if (opened == true) { 
			handlePushFirst();
			((TiBaseActivity) getActivity()).setWindowProxy(windows.get(windows.size() - 1));
			updateHomeButton(getCurrentWindow());
			super.onWindowActivityCreated();
		}
		else {
			opened = true; //because handlePush needs this
			opening = false;
			updateHomeButton(getCurrentWindow());
			super.onWindowActivityCreated();
			getParentViewForChild().setId(viewId++);
			handlePushFirst();
		}
	}
	
	protected int getContainerId(){
		int id = getParentViewForChild().getId();
		return id;
	}
	
	
	@Override
	public void windowCreated(TiBaseActivity activity) {
		super.windowCreated(activity);

	}
	
	
	private boolean handlePop(final TiWindowProxy proxy, Object arg) 
	{
		if (!windows.contains(proxy)) {
			poping = false;
			return true;
		}
		if (proxy == windows.get(0)) {
			//first window, closing it is closing ourself
			poping = false;
			close(arg);
			return true;
		}
		return popWindow(proxy, arg);
	}
	
	
	static HashMap kDefaultTransition = new HashMap<String, Object>(){{
	       put(TiC.PROPERTY_STYLE, Integer.valueOf(TransitionStyleModule.SWIPE)); 
	       put(TiC.PROPERTY_SUBSTYLE,  Integer.valueOf(TransitionStyleModule.RIGHT_TO_LEFT));}};
	       
	private KrollDict getDictFromTransition(Transition transition)
	{
		if (transition == null) return null;
		KrollDict transitionDict = new KrollDict();
		transitionDict.put(TiC.PROPERTY_STYLE, transition.getType());
		transitionDict.put(TiC.PROPERTY_SUBSTYLE, transition.subType.ordinal());
		transitionDict.put(TiC.PROPERTY_DURATION, transition.getDuration());
		transitionDict.put(TiC.PROPERTY_REVERSE, transition.isReversed());
		return transitionDict;
	}
	
	private void handlePush(final TiWindowProxy proxy, boolean isFirst, Object arg) 
	{
		int index = windows.indexOf(proxy);
		if (index >=0 && !isFirst){
			pushing = false;
			poping = true;
			popUpToWindow(proxy, arg);
			return;
		}

		Transition transition = null;
		boolean animated = true;
		if (arg != null && arg instanceof HashMap<?, ?>) {
			animated = TiConvert.toBoolean((HashMap) arg, TiC.PROPERTY_ANIMATED, animated);
		}
		final ViewGroup viewToAddTo = (ViewGroup) getParentViewForChild();
		
		if (!isFirst && animated) {
			transition = TransitionHelper.transitionFromObject((HashMap) ((arg != null)?((HashMap)arg).get(TiC.PROPERTY_TRANSITION):null), defaultTransition, null);
		}
		if (!isFirst && hasListeners("openWindow")) {
			KrollDict options = new KrollDict();
			options.put(TiC.PROPERTY_WINDOW, proxy);
			options.put("stackIndex", windows.size());
			options.put(TiC.PROPERTY_ANIMATED, animated);
			options.put(TiC.PROPERTY_TRANSITION, getDictFromTransition(transition));
			fireEvent("openWindow", options);
		}
		TiBaseActivity activity = (TiBaseActivity) getActivity();
		if (viewToAddTo != null) {
			proxy.setActivity(activity);
			final View viewToAdd = proxy.getOrCreateView().getOuterView();
			viewToAdd.setVisibility(View.GONE);			
			TiUIHelper.addView(viewToAddTo, viewToAdd, proxy.peekView().getLayoutParams());
			TiWindowProxy winToBlur = getCurrentWindow();
			final View viewToHide = winToBlur.getOuterView();
			if (transition != null) {	
				transition.setTargets(viewToAddTo, viewToAdd, viewToHide);

				AnimatorSet set = transition.getSet(new AnimatorListener() {
					public void onAnimationStart(Animator arg0) {							
					}
					
					public void onAnimationRepeat(Animator arg0) {							
					}
					
					public void onAnimationEnd(Animator arg0) {	
						viewToAddTo.removeView(viewToHide);
						pushing = false;
					}

					public void onAnimationCancel(Animator arg0) {		
						viewToAddTo.removeView(viewToHide);
						pushing = false; 
					}
				});
				set.start();
			}
			else {
				viewToAddTo.removeView(viewToHide);
				pushing = false; 
			}
   			viewToAdd.setVisibility(View.VISIBLE);			
		}
		addWindow(proxy, transition);
		proxy.onWindowActivityCreated();
		activity.setWindowProxy((TiWindowProxy) proxy);
		updateHomeButton(proxy);
	}
	
	@Kroll.method
	public void openWindow(final TiWindowProxy proxy, @Kroll.argument(optional = true) Object arg)
	{
		if (pushing || poping) return;
		if (!opened || opening) {
			preAddedWindows.add(proxy);
			return;
		}
		pushing = true;
		if (TiApplication.isUIThread()) {
			handlePush(proxy, false, arg);
			return;
		}

		TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_PUSH), new Pair<TiWindowProxy, Object>(proxy, arg));
	}
	@Override
	public boolean handleMessage(Message msg)
	{
		switch (msg.what) {
			case MSG_PUSH: {
				AsyncResult result = (AsyncResult) msg.obj;
				Pair<TiWindowProxy, Object> pair = (Pair<TiWindowProxy, Object>) result.getArg();
				handlePush(pair.first, false, pair.second);
				result.setResult(null); // signal opened
				return true;
			}
			case MSG_POP: {
				AsyncResult result = (AsyncResult) msg.obj;
				Pair<TiWindowProxy, Object> pair = (Pair<TiWindowProxy, Object>) result.getArg();
				result.setResult(handlePop(pair.first, pair.second)); // signal opened
				return true;
			}
		}
		return super.handleMessage(msg);
	}
	
	@Kroll.method @Kroll.getProperty
	public int getStackSize()
	{
		return windows.size();
	}
	
	@Kroll.method
	public Object getWindow(int index)
	{
		if (!opened || opening) {
			if (index >= 0  && index < preAddedWindows.size())
				return preAddedWindows.get(index);
		}
		else if (index >= 0  && index < windows.size())
			return windows.get(index);
		return null;
	}
	
	@Kroll.method
	public void closeWindow(final TiWindowProxy proxy, @Kroll.argument(optional = true) Object arg)
	{
		if (pushing || poping) return;
		if (!opened || opening) {
			preAddedWindows.remove(proxy);
			return;
		}
		poping = true;
		if (TiApplication.isUIThread()) {
			handlePop(proxy, arg);
			return;
		}

		TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_POP), new Pair<TiWindowProxy, Object>(proxy, arg));
	}
	
	@Kroll.method
	public void closeCurrentWindow(@Kroll.argument(optional = true) Object arg)
	{
		if (pushing || poping) return;
		if (!opened || opening) {
			TiWindowProxy currentWindow = getCurrentWindow();
			if (currentWindow != this) {
				preAddedWindows.remove(currentWindow);
			}
			return;
		}
		TiWindowProxy currentWindow = getCurrentWindow();
		if (currentWindow != this) {
			poping = true;
			if (TiApplication.isUIThread()) {
				handlePop(currentWindow, arg);
				return;
			}

			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_POP), new Pair<TiWindowProxy, Object>(currentWindow, arg));
		}
	}
	
	@Kroll.method
	public void closeAllWindows(@Kroll.argument(optional = true) Object arg)
	{
		if (pushing || poping) return;
		if (!opened || opening) {
			preAddedWindows.clear();
			return;
		}
		TiWindowProxy window = windows.get(0);
		poping = true;
		if (TiApplication.isUIThread()) {
			handlePop(window, arg);
			return;
		}

		TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_POP), new Pair<TiWindowProxy, Object>(window, arg));
	}
	
	@Override
	public boolean shouldExitOnClose() {
		if (windows.size() == 1) {
			return super.shouldExitOnClose();
		}
		return false;
	}

	@Override
	public boolean interceptOnBackPressed() {
	if (pushing || poping) return true;
		if (windows.size() > 1) {
			TiWindowProxy currentWindow = getCurrentWindow();
			if (currentWindow.hasListeners(TiC.EVENT_ANDROID_BACK)) {
				currentWindow.fireEvent(TiC.EVENT_ANDROID_BACK);
				return true;
			}
			poping = true;
			return popCurrentWindow(null);
		}
		return false;
	}

	@Override
	public boolean handleClose(TiWindowProxy proxy, Object arg) {
		if (pushing || poping) return true;
		poping = true;
		if (TiApplication.isUIThread()) {
			return handlePop(proxy, arg);
		}
		Object result = TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_POP), new Pair<TiWindowProxy, Object>(proxy, arg));
		if (result instanceof Boolean) {
			return ((Boolean) result).booleanValue();
		} else {
			return false;
		}
	}
	
	@Override
	public KrollProxy getParentForBubbling(TiWindowProxy winProxy) {
		return this;
//		return (TiViewProxy.winProxy).getParentForBubbling();
	}


	@Override
	public boolean handleOpen(TiWindowProxy proxy, Object arg) {
		return false;
	}

	public void onStart(Activity activity) {
	}

	public void onResume(Activity activity) {
	}

	public void onPause(Activity activity) {		
	}
	
	public void clearWindowsStack(){
		if (windows.size() == 0) return;
		ViewGroup viewToRemoveFrom = (ViewGroup) getParentViewForChild();
		for (int i = 0; i < windows.size(); i++) {
			TiWindowProxy proxy = windows.get(i);
			View viewToRemove = proxy.getOuterView();
			if (viewToRemove != null) {
				viewToRemoveFrom.removeView(viewToRemove);
			}
			proxy.closeFromActivity(false);
		}
		windows.clear();
	}

	public void onStop(Activity activity) {
	}

	public void onDestroy(Activity activity) {		
		clearWindowsStack();
	}
	@Override
	public boolean realUpdateOrientationModes()
	{
		TiWindowProxy current = getCurrentWindow();
		if (current != this)
		{
			if (current.realUpdateOrientationModes())
				return true;
		}
		return super.realUpdateOrientationModes();
	}


	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue,
			KrollProxy proxy) {
		if (key.equals(TiC.PROPERTY_TRANSITION)) {
			if (newValue instanceof HashMap) {
				defaultTransition = (HashMap) newValue;
			}
			else {
				defaultTransition = kDefaultTransition;
			}
		}
	}


	@Override
	public void processProperties(KrollDict properties) {
		if (properties.containsKey(TiC.PROPERTY_TRANSITION)) {
			Object value = properties.get(TiC.PROPERTY_TRANSITION);
			if (value instanceof HashMap) {
				defaultTransition = (HashMap) value;
			}
			else {
				defaultTransition = kDefaultTransition;
			}
		}
	}


	@Override
	public void propertiesChanged(List<KrollPropertyChange> changes,
			KrollProxy proxy) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void listenerAdded(String type, int count, KrollProxy proxy) {}


	@Override
	public void listenerRemoved(String type, int count, KrollProxy proxy) {}
}
