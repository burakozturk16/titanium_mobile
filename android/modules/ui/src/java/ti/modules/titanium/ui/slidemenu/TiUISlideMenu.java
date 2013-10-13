package ti.modules.titanium.ui.slidemenu;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiBaseActivity.ConfigurationChangedListener;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.ActivityProxy;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.proxy.TiWindowProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;

import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.view.animation.Interpolator;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.CanvasTransformer;

import ti.modules.titanium.ui.SlideMenuProxy;
import ti.modules.titanium.ui.UIModule;
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
	private static TiDimension defaultDisplacement = new TiDimension(0, TiDimension.TYPE_WIDTH);
	private TiDimension leftViewDisplacement =  defaultDisplacement;
	private TiDimension rightViewDisplacement =  defaultDisplacement;
	
	public TiUISlideMenu(final SlideMenuProxy proxy, TiBaseActivity activity)
	{
		super(proxy);
		this.activity = activity;
		activity.addConfigurationChangedListener(this);
        // configure the SlidingMenu
		slidingMenu = new SlidingMenu(activity) {
			@Override
			protected void onSizeChanged(int w, int h, int oldw, int oldh) {
				super.onSizeChanged(w, h, oldw, oldh);
				updateDisplacements();
			}
		};
		slidingMenu.setClassForNonViewPager(TiViewPagerLayout.class);
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
		updateDisplacements();
	}

	private void updateDisplacements()
	{
		int leftMenuWidth = menuWidth;
		if (leftMenuWidth < 0) {
			leftMenuWidth += slidingMenu.getWidth();
		}
		if (leftMenuWidth > 0) slidingMenu.setBehindScrollScale(leftViewDisplacement.getAsPixels(slidingMenu.getContext(), leftMenuWidth, leftMenuWidth)/(float)leftMenuWidth);

		int myRightMenuWidth = rightMenuWidth;
		if (myRightMenuWidth < 0) {
			myRightMenuWidth += slidingMenu.getWidth();
		}
		if (myRightMenuWidth > 0) slidingMenu.setBehindSecondaryScrollScale(rightViewDisplacement.getAsPixels(slidingMenu.getContext(), myRightMenuWidth, myRightMenuWidth)/(float)myRightMenuWidth);
	}
	
	public int getLeftMenuWidth()
	{
		return menuWidth;
	}
	
	public int getRightMenuWidth()
	{
		return rightMenuWidth;
	}
	
	// for animations
	private static Interpolator interp = new Interpolator() {
		@Override
		public float getInterpolation(float t) {
			t -= 1.0f;
			return t * t * t + 1.0f;
		}		
	};
		
	private void updateAnimationMode(int mode, boolean right)
	{
		CanvasTransformer transformer = null;
		if (mode == SlideMenuOptionsModule.ANIMATION_SCALE) {
			// scale
			transformer = new CanvasTransformer() {
				@Override
				public void transformCanvas(Canvas canvas, float percentOpen) {
					canvas.scale(percentOpen, 1, 0, 0);
				}			
			};	
		} else if (mode == SlideMenuOptionsModule.ANIMATION_SLIDEUP) {
			// slide
			transformer = new CanvasTransformer() {
				@Override
				public void transformCanvas(Canvas canvas, float percentOpen) {
					canvas.translate(0, canvas.getHeight()*(1-interp.getInterpolation(percentOpen)));
				}			
			};
		} else if (mode == SlideMenuOptionsModule.ANIMATION_ZOOM) {
			// zoom animation
			transformer = new CanvasTransformer() {
				@Override
				public void transformCanvas(Canvas canvas, float percentOpen) {
					float scale = (float) (percentOpen*0.25 + 0.75);
					canvas.scale(scale, scale, canvas.getWidth()/2, canvas.getHeight()/2);
				}
			};
		}
		
		if (right)
			slidingMenu.setBehindSecondaryCanvasTransformer(transformer);
		else 
			slidingMenu.setBehindCanvasTransformer(transformer);
		// we need to reset the scrollScale when applying custom animations
		if( transformer != null){
			leftViewDisplacement = defaultDisplacement;
			rightViewDisplacement = defaultDisplacement;
			updateDisplacements();
		}

	}
	
	private void updatePanningMode(int panningMode)
	{
		slidingMenu.setTouchModeBehind(SlidingMenu.TOUCHMODE_FULLSCREEN);
		if (panningMode == SlideMenuOptionsModule.MENU_PANNING_BORDERS) {
			slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		} else if (panningMode == SlideMenuOptionsModule.MENU_PANNING_CENTER_VIEW)
			slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		else if (panningMode == SlideMenuOptionsModule.MENU_PANNING_ALL_VIEWS) {
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
		if (d.containsKey(SlideMenuOptionsModule.PROPERTY_LEFT_VIEW)) {
			setProxy((TiViewProxy) d.get(SlideMenuOptionsModule.PROPERTY_LEFT_VIEW), 1);
		}
		if (d.containsKey(SlideMenuOptionsModule.PROPERTY_RIGHT_VIEW)) {
			setProxy((TiViewProxy) d.get(SlideMenuOptionsModule.PROPERTY_RIGHT_VIEW), 2);
		}
		
		if (d.containsKey(SlideMenuOptionsModule.PROPERTY_CENTER_VIEW)) {
			setProxy((TiViewProxy) d.get(SlideMenuOptionsModule.PROPERTY_CENTER_VIEW), 0);
		}
		
		updateMenus();	
		
		if (d.containsKey(SlideMenuOptionsModule.PROPERTY_PANNING_MODE)) {
			updatePanningMode(TiConvert.toInt(d.get(SlideMenuOptionsModule.PROPERTY_PANNING_MODE), SlideMenuOptionsModule.MENU_PANNING_CENTER_VIEW));
		}

		if (d.containsKey(SlideMenuOptionsModule.PROPERTY_LEFT_VIEW_WIDTH)) {
			menuWidth = d.getInt(SlideMenuOptionsModule.PROPERTY_LEFT_VIEW_WIDTH);
		}
		if (d.containsKey(SlideMenuOptionsModule.PROPERTY_RIGHT_VIEW_WIDTH)) {
			rightMenuWidth = d.getInt(SlideMenuOptionsModule.PROPERTY_RIGHT_VIEW_WIDTH);
		}
		
		if (d.containsKey(SlideMenuOptionsModule.PROPERTY_FADING)) {
			slidingMenu.setFadeDegree(d.getDouble(SlideMenuOptionsModule.PROPERTY_FADING).floatValue());
		}
		if (d.containsKey(SlideMenuOptionsModule.PROPERTY_LEFT_VIEW_DISPLACEMENT)) {
			leftViewDisplacement = TiConvert.toTiDimension(d, SlideMenuOptionsModule.PROPERTY_LEFT_VIEW_DISPLACEMENT, TiDimension.TYPE_WIDTH);
		}
		if (d.containsKey(SlideMenuOptionsModule.PROPERTY_RIGHT_VIEW_DISPLACEMENT)) {
			rightViewDisplacement = TiConvert.toTiDimension(d, SlideMenuOptionsModule.PROPERTY_RIGHT_VIEW_DISPLACEMENT, TiDimension.TYPE_WIDTH);
		}
		if (d.containsKey(SlideMenuOptionsModule.PROPERTY_ANIMATION_LEFT)) {
			updateAnimationMode(TiConvert.toInt(d.get(SlideMenuOptionsModule.PROPERTY_ANIMATION_LEFT)), false);
		}
		if (d.containsKey(SlideMenuOptionsModule.PROPERTY_ANIMATION_RIGHT)) {
			updateAnimationMode(TiConvert.toInt(d.get(SlideMenuOptionsModule.PROPERTY_ANIMATION_RIGHT)), true);
		}
		if (d.containsKey(TiC.PROPERTY_SHADOW_WIDTH)) {
			slidingMenu.setShadowWidth(d.getInt(TiC.PROPERTY_SHADOW_WIDTH));
		}
		if (d.containsKey(TiC.PROPERTY_ENABLED)) {
			slidingMenu.setSlidingEnabled(d.getBoolean(TiC.PROPERTY_ENABLED));
		}
		updateMenuWidth();
		super.processProperties(d);
	}
	
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy)
	{
		Log.d(TAG, "Property: " + key + " old: " + oldValue + " new: " + newValue, Log.DEBUG_MODE);

		if (key.equals(SlideMenuOptionsModule.PROPERTY_LEFT_VIEW)) {
			setProxy((TiViewProxy) newValue, 1);
			updateMenus();	
		} else if (key.equals(SlideMenuOptionsModule.PROPERTY_RIGHT_VIEW)) {
			setProxy((TiViewProxy) newValue, 2);
			updateMenus();	
		} else if (key.equals(SlideMenuOptionsModule.PROPERTY_CENTER_VIEW)) {
			setProxy((TiViewProxy) newValue, 0);
			slidingMenu.showContent(true);
		} else if (key.equals(SlideMenuOptionsModule.PROPERTY_PANNING_MODE)) {
			updatePanningMode(TiConvert.toInt(newValue, SlideMenuOptionsModule.MENU_PANNING_CENTER_VIEW));
		} else if (key.equals(SlideMenuOptionsModule.PROPERTY_LEFT_VIEW_WIDTH)) {
			menuWidth = TiConvert.toInt(newValue);
			updateMenuWidth();
		} else if (key.equals(SlideMenuOptionsModule.PROPERTY_RIGHT_VIEW_WIDTH)) {
			menuWidth = TiConvert.toInt(newValue);
			updateMenuWidth();
		} else if (key.equals(SlideMenuOptionsModule.PROPERTY_FADING)) {
			slidingMenu.setFadeDegree(TiConvert.toFloat(newValue));
		} else if (key.equals(SlideMenuOptionsModule.PROPERTY_ANIMATION_LEFT)) {
			updateAnimationMode(TiConvert.toInt(newValue), false);
		} else if (key.equals(SlideMenuOptionsModule.PROPERTY_ANIMATION_RIGHT)) {
			updateAnimationMode(TiConvert.toInt(newValue), true);
		} else if (key.equals(SlideMenuOptionsModule.PROPERTY_LEFT_VIEW_DISPLACEMENT)) {
			leftViewDisplacement = TiConvert.toTiDimension(newValue, TiDimension.TYPE_WIDTH);
			updateDisplacements();
		} else if (key.equals(SlideMenuOptionsModule.PROPERTY_RIGHT_VIEW_DISPLACEMENT)) {
			rightViewDisplacement = TiConvert.toTiDimension(newValue, TiDimension.TYPE_WIDTH);
			updateDisplacements();
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
				content.removeView(oldProxy.getOuterView());
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
