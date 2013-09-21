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
import org.appcelerator.titanium.proxy.TiWindowProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import ti.modules.titanium.ui.SlideMenuProxy;
import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.WindowProxy;
import ti.modules.titanium.ui.widget.TiUIScrollableView.TiViewPagerLayout;

public class TiUISlideMenu extends TiUIView implements ConfigurationChangedListener{
	private SlidingMenu slidingMenu;
	private TiViewProxy leftView;
	private TiViewProxy rightView;
	private TiViewProxy centerView;
	private static final String TAG = "TiUISlideMenu";
	private TiBaseActivity activity;
	private int menuWidth;
	private int rightMenuWidth;
	
	
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
		rightMenuWidth = -100;
		slidingMenu.setFadeDegree(0.0f);
		slidingMenu.setBehindScrollScale(0.0f);
		slidingMenu.setShadowWidth(20);
		
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
		if (rightMenuWidth > 0)
			slidingMenu.setSecondaryBehindWidth(rightMenuWidth);
		else
			slidingMenu.setSecondaryBehindOffset(-rightMenuWidth);
	}
	
	public int getLeftMenuWidth()
	{
		return menuWidth;
	}
	
	public int getRightMenuWidth()
	{
		return rightMenuWidth;
	}
	
	private void updatePanningMode(int panningMode)
	{
		slidingMenu.setTouchModeBehind(SlidingMenu.TOUCHMODE_MARGIN);
		if (panningMode == UIModule.MENU_PANNING_BORDERS) {
			slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		} else if (panningMode == UIModule.MENU_PANNING_NON_SCROLLVIEW) {
			slidingMenu.setClassForNonViewPager(TiViewPagerLayout.class);
			slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_NON_VIEWPAGER);
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
			setProxy((TiViewProxy) d.get(TiC.PROPERTY_LEFT_VIEW), 1);
		}
		if (d.containsKey(TiC.PROPERTY_RIGHT_VIEW)) {
			setProxy((TiViewProxy) d.get(TiC.PROPERTY_RIGHT_VIEW), 2);
		}
		
		if (d.containsKey(TiC.PROPERTY_CENTER_VIEW)) {
			setProxy((TiViewProxy) d.get(TiC.PROPERTY_CENTER_VIEW), 0);
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
			rightMenuWidth = d.getInt(TiC.PROPERTY_RIGHT_VIEW_WIDTH);
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
		if (d.containsKey(TiC.PROPERTY_ENABLED)) {
			slidingMenu.setSlidingEnabled(d.getBoolean(TiC.PROPERTY_ENABLED));
		}
		super.processProperties(d);
	}
	
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy)
	{
		Log.d(TAG, "Property: " + key + " old: " + oldValue + " new: " + newValue, Log.DEBUG_MODE);

		if (key.equals(TiC.PROPERTY_LEFT_VIEW)) {
			setProxy((TiViewProxy) newValue, 1);
			updateMenus();	
		} else if (key.equals(TiC.PROPERTY_RIGHT_VIEW)) {
			setProxy((TiViewProxy) newValue, 2);
			updateMenus();	
		} else if (key.equals(TiC.PROPERTY_CENTER_VIEW)) {
			setProxy((TiViewProxy) newValue, 0);
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
		} else if (key.equals(TiC.PROPERTY_ENABLED)) {
				slidingMenu.setSlidingEnabled(TiConvert.toBoolean(newValue));
		} else {
			super.propertyChanged(key, oldValue, newValue, proxy);
		}
	}

	@Override
	public void onConfigurationChanged(TiBaseActivity activity,
			Configuration newConfig) {
		updateMenuWidth();
	}
	
	private void setProxy(TiViewProxy newProxy, int forSlideView)//0center,1left,2right
	{
		
		boolean isCenterView = forSlideView == 0;
		TiViewProxy oldProxy = isCenterView?this.centerView:((forSlideView == 1)?this.leftView:this.rightView);
		if (newProxy == oldProxy) return;
		TiCompositeLayout content = ((TiCompositeLayout) activity.getLayout());
		int index = 0;
		if (isCenterView && oldProxy != null)
		{
			index = content.indexOfChild(oldProxy.getOrCreateView().getNativeView());
		}
		if (newProxy != null && newProxy instanceof TiViewProxy) {
				TiBaseActivity activity = (TiBaseActivity) this.proxy.getActivity();
				newProxy.setActivity(activity);
				if (isCenterView) {
					TiCompositeLayout.LayoutParams params = new TiCompositeLayout.LayoutParams();
					params.autoFillsHeight = true;
					params.autoFillsWidth = true;
					content.addView(newProxy.getOrCreateView().getOuterView(), index, params);
					if (newProxy instanceof TiWindowProxy) {
						activity.setWindowProxy((TiWindowProxy) newProxy);
					}
				}
				if (newProxy instanceof TiWindowProxy) {
					((TiWindowProxy)newProxy).onWindowActivityCreated();
					newProxy.focus();
				}
		} else {
			Log.e(TAG, "Invalid type for centerView");
		}
		if (oldProxy != null)
		{
			if (isCenterView) {
				content.removeView(oldProxy.getNativeView());
			}
			oldProxy.setActivity(null);
			oldProxy.blur();
		}
		switch (forSlideView) {
		case 0:
			this.centerView = newProxy;
			break;
		case 1:
			this.leftView = newProxy;
			break;
		case 2:
			this.rightView = newProxy;
			break;
		default:
			break;
		}
	}
}
