package ti.modules.titanium.ui.slidemenu;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiBaseActivity.ConfigurationChangedListener;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.ActivityProxy;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.view.ViewGroup;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import ti.modules.titanium.ui.SlideMenuProxy;
import ti.modules.titanium.ui.UIModule;

public class TiUISlideMenu extends TiUIView implements ConfigurationChangedListener{
	private SlidingMenu slidingMenu;
	private TiViewProxy leftView;
	private TiViewProxy rightView;
	private TiViewProxy centerView;
	private static final String TAG = "TiUISlideMenu";
	private TiBaseActivity activity;
	private int menuWidth;
	
	
	public TiUISlideMenu(final SlideMenuProxy proxy, TiBaseActivity activity)
	{
		super(proxy);
		this.activity = activity;
		activity.addConfigurationChangedListener(this);
        // configure the SlidingMenu
		slidingMenu = new SlidingMenu(activity);
		slidingMenu.setOnCloseListener(new SlidingMenu.OnCloseListener() {
			@Override
			public void onClose(int leftOrRight, boolean animated, int duration) {
				if (proxy.hasListeners("closemenu"))
				{
					KrollDict options = new KrollDict();
					options.put("side", (leftOrRight == 1)?UIModule.RIGHT_VIEW:UIModule.LEFT_VIEW);
					options.put("animated", animated);
					options.put("duration", duration);
					proxy.fireEvent("closemenu", options);
				}
			}
		});
		slidingMenu.setOnOpenListener(new SlidingMenu.OnOpenListener() {
			@Override
			public void onOpen(int leftOrRight, boolean animated, int duration) {
				if (proxy.hasListeners("openmenu"))
				{
					KrollDict options = new KrollDict();
					options.put("side", (leftOrRight == 1)?UIModule.RIGHT_VIEW:UIModule.LEFT_VIEW);
					options.put("animated", animated);
					options.put("duration", duration);
					proxy.fireEvent("openmenu", options);
				}
			}
		});
		slidingMenu.setOnScrolledListener(new SlidingMenu.OnScrolledListener() {
			
			@Override
			public void onScrolled(int scroll) {
				if (proxy.hasListeners("scroll"))
				{
					KrollDict options = new KrollDict();
					options.put("offset", scroll);
					proxy.fireEvent("scroll", options);
				}
				
			}

			@Override
			public void onScrolledEnded(int scroll) {
				// TODO Auto-generated method stub
				if (proxy.hasListeners("scrollend"))
				{
					KrollDict options = new KrollDict();
					options.put("offset", scroll);
					proxy.fireEvent("scrollend", options);
				}
			}

			@Override
			public void onScrolledStarted(int scroll) {
				// TODO Auto-generated method stub
				if (proxy.hasListeners("scrollstart"))
				{
					KrollDict options = new KrollDict();
					options.put("offset", scroll);
					proxy.fireEvent("scrollstart", options);
				}
			}
		});
		
		slidingMenu.setMode(SlidingMenu.LEFT);
		slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		menuWidth = -100;
		slidingMenu.setFadeDegree(0.0f);
		slidingMenu.setBehindScrollScale(0.0f);
		slidingMenu.setShadowWidth(0);
		
		updateMenuWidth();
		
		slidingMenu.attachToActivity(activity, SlidingMenu.SLIDING_WINDOW);
		
		int[] colors1 = {Color.argb(0, 0, 0, 0), Color.argb(128, 0, 0, 0)};
		GradientDrawable shadow = new GradientDrawable(Orientation.LEFT_RIGHT, colors1);		
		GradientDrawable shadowR = new GradientDrawable(Orientation.RIGHT_LEFT, colors1);		
		slidingMenu.setShadowDrawable(shadow);
		slidingMenu.setSecondaryShadowDrawable(shadowR);

		setNativeView(slidingMenu);
	}
	
	public SlidingMenu getSlidingMenu()
	{
		return slidingMenu;
	}
	
	private void updateMenuWidth()
	{
		if (menuWidth > 0)
			slidingMenu.setBehindWidth(menuWidth);
		else
			slidingMenu.setBehindOffset(-menuWidth);
	}
	
	public int getMenuWidth()
	{
		return menuWidth;
	}
	
	private void updatePanningMode(int panningMode)
	{
		slidingMenu.setTouchModeBehind(SlidingMenu.TOUCHMODE_MARGIN);
		if (panningMode == UIModule.MENU_PANNING_BORDERS) {
			slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		} else if (panningMode == UIModule.MENU_PANNING_CENTER_VIEW)
			slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		else if (panningMode == UIModule.MENU_PANNING_ALL_VIEWS) {
			slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
			slidingMenu.setTouchModeBehind(SlidingMenu.TOUCHMODE_FULLSCREEN);
		} else{
			slidingMenu.setTouchModeBehind(SlidingMenu.TOUCHMODE_NONE);
			slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
		}
	}
	
	private void updateMenus() {
		if (this.leftView != null && this.rightView != null) {
			slidingMenu.setMode(SlidingMenu.LEFT_RIGHT);
			slidingMenu.setMenu((this.leftView).getOrCreateView().getOuterView());
			slidingMenu.setSecondaryMenu((this.rightView).getOrCreateView().getOuterView());
		}
		else if (this.rightView != null)
		{
			slidingMenu.setMode(SlidingMenu.RIGHT);
			slidingMenu.setMenu((this.rightView).getOrCreateView().getOuterView());
			slidingMenu.setSecondaryMenu(null);
		}
		else if (this.leftView != null)
		{
			slidingMenu.setMode(SlidingMenu.LEFT);
			slidingMenu.setMenu((this.leftView).getOrCreateView().getOuterView());
			slidingMenu.setSecondaryMenu(null);
		}
		else
		{
			slidingMenu.setMode(SlidingMenu.LEFT);
			slidingMenu.setMenu(null);
			slidingMenu.setSecondaryMenu(null);
		}
	}

	@Override
	public void processProperties(KrollDict d)
	{
		if (d.containsKey(TiC.PROPERTY_ACTIVITY)) {
			Object activityObject = d.get(TiC.PROPERTY_ACTIVITY);
			ActivityProxy activityProxy = getProxy().getActivityProxy();
			if (activityObject instanceof HashMap<?, ?> && activityProxy != null) {
				@SuppressWarnings("unchecked")
				KrollDict options = new KrollDict((HashMap<String, Object>) activityObject);
				activityProxy.handleCreationDict(options);
			}
		}
		if (d.containsKey(TiC.PROPERTY_LEFT_VIEW)) {
			Object leftView = d.get(TiC.PROPERTY_LEFT_VIEW);
			if (leftView != null && leftView instanceof TiViewProxy) {
				this.leftView = (TiViewProxy)leftView;
			} else {
				Log.e(TAG, "Invalid type for leftView");
			}
		}
		if (d.containsKey(TiC.PROPERTY_RIGHT_VIEW)) {
			Object rightView = d.get(TiC.PROPERTY_RIGHT_VIEW);
			if (rightView != null && rightView instanceof TiViewProxy) {
				this.rightView = (TiViewProxy)rightView;
			} else {
				Log.e(TAG, "Invalid type for rightView");
			}
		}
		
		if (d.containsKey(TiC.PROPERTY_CENTER_VIEW)) {
			Object centerView = d.get(TiC.PROPERTY_CENTER_VIEW);
			if (centerView != null && centerView instanceof TiViewProxy) {
				this.centerView = (TiViewProxy)centerView;
				TiCompositeLayout content = ((TiCompositeLayout) activity.getLayout());
				TiCompositeLayout.LayoutParams params = new TiCompositeLayout.LayoutParams();
				params.autoFillsHeight = true;
				params.autoFillsWidth = true;
				content.addView(((TiViewProxy)centerView).getOrCreateView().getOuterView(), params);						
			} else {
				Log.e(TAG, "Invalid type for centerView");
			}
		}
		
		updateMenus();	
		
		if (d.containsKey(TiC.PROPERTY_PANNING_MODE)) {
			updatePanningMode(TiConvert.toInt(d.get(TiC.PROPERTY_PANNING_MODE), UIModule.MENU_PANNING_CENTER_VIEW));
		}

		if (d.containsKey(TiC.PROPERTY_LEFT_VIEW_WIDTH)) {
			menuWidth = d.getInt(TiC.PROPERTY_LEFT_VIEW_WIDTH);
			updateMenuWidth();
		}
		if (d.containsKey(TiC.PROPERTY_RIGHT_VIEW_WIDTH)) {
			menuWidth = d.getInt(TiC.PROPERTY_RIGHT_VIEW_WIDTH);
			updateMenuWidth();
		}
		
		if (d.containsKey(TiC.PROPERTY_FADING)) {
			slidingMenu.setFadeDegree(d.getDouble(TiC.PROPERTY_FADING).floatValue());
		}
		if (d.containsKey(TiC.PROPERTY_MENU_SCROLL_SCALE)) {
			slidingMenu.setBehindScrollScale(d.getDouble(TiC.PROPERTY_MENU_SCROLL_SCALE).floatValue());
		}
		if (d.containsKey(TiC.PROPERTY_SHADOW_WIDTH)) {
			slidingMenu.setShadowWidth(d.getInt(TiC.PROPERTY_SHADOW_WIDTH));
		}
		super.processProperties(d);
	}
	
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy)
	{
		Log.d(TAG, "Property: " + key + " old: " + oldValue + " new: " + newValue, Log.DEBUG_MODE);

		if (key.equals(TiC.PROPERTY_LEFT_VIEW)) {
			if (newValue == this.leftView) return;
			TiViewProxy newProxy = null;
			if (newValue != null && newValue instanceof TiViewProxy) {
				newProxy = (TiViewProxy)newValue;
			} else {
				Log.e(TAG, "Invalid type for leftView");
			}
			this.leftView = newProxy;
			updateMenus();
		} else if (key.equals(TiC.PROPERTY_RIGHT_VIEW)) {
			if (newValue == this.rightView) return;
			TiViewProxy newProxy = null;
			if (newValue != null && newValue instanceof TiViewProxy) {
				newProxy = (TiViewProxy)newValue;
			} else {
				Log.e(TAG, "Invalid type for leftView");
			}
			this.rightView = newProxy;
			updateMenus();
		} else if (key.equals(TiC.PROPERTY_CENTER_VIEW)) {
			if (newValue == this.centerView) return;
			TiCompositeLayout content = ((TiCompositeLayout) activity.getLayout());
			TiViewProxy newProxy = null;
			int index = 0;
			if (this.centerView != null)
			{
				index = content.indexOfChild(this.centerView.getOrCreateView().getNativeView());
			}
			if (newValue != null && newValue instanceof TiViewProxy) {
					newProxy = (TiViewProxy)newValue;
					TiCompositeLayout.LayoutParams params = new TiCompositeLayout.LayoutParams();
					params.autoFillsHeight = true;
					params.autoFillsWidth = true;
					content.addView(newProxy.getOrCreateView().getOuterView(), index, params);						
			} else {
				Log.e(TAG, "Invalid type for centerView");
			}
			if (this.centerView != null)
			{
				content.removeView(this.centerView.getNativeView());
			}
			this.centerView = newProxy;	
		} else if (key.equals(TiC.PROPERTY_PANNING_MODE)) {
			updatePanningMode(TiConvert.toInt(newValue, UIModule.MENU_PANNING_CENTER_VIEW));
		} else if (key.equals(TiC.PROPERTY_LEFT_VIEW_WIDTH)) {
			menuWidth = TiConvert.toInt(newValue);
			updateMenuWidth();
		} else if (key.equals(TiC.PROPERTY_RIGHT_VIEW_WIDTH)) {
			menuWidth = TiConvert.toInt(newValue);
			updateMenuWidth();
		} else if (key.equals(TiC.PROPERTY_FADING)) {
			slidingMenu.setFadeDegree(TiConvert.toFloat(newValue));
		} else if (key.equals(TiC.PROPERTY_MENU_SCROLL_SCALE)) {
			slidingMenu.setBehindScrollScale(TiConvert.toFloat(newValue));
		} else if (key.equals(TiC.PROPERTY_SHADOW_WIDTH)) {
			slidingMenu.setShadowWidth(TiConvert.toInt(newValue));
		} else {
			super.propertyChanged(key, oldValue, newValue, proxy);
		}
	}

	@Override
	public void onConfigurationChanged(TiBaseActivity activity,
			Configuration newConfig) {
		updateMenuWidth();
	}

}
