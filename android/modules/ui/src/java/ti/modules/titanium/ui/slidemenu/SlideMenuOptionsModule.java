package ti.modules.titanium.ui.slidemenu;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;

import ti.modules.titanium.ui.UIModule;

@Kroll.module(parentModule=UIModule.class)
public class SlideMenuOptionsModule extends KrollModule {

	
	@Kroll.constant public static final int MENU_PANNING_NONE = 0;
	@Kroll.constant public static final int MENU_PANNING_ALL_VIEWS = 1;
	@Kroll.constant public static final int MENU_PANNING_CENTER_VIEW = 2;
	@Kroll.constant public static final int MENU_PANNING_BORDERS = 3;
	
	
	@Kroll.constant public static final int ANIMATION_NONE = 0;
	@Kroll.constant public static final int ANIMATION_ZOOM = 1;
	@Kroll.constant public static final int ANIMATION_SCALE= 2;
	@Kroll.constant public static final int ANIMATION_SLIDEUP = 3;


	@Kroll.constant public static final String PROPERTY_ANIMATION_LEFT = "leftAnimation";
	@Kroll.constant public static final String PROPERTY_ANIMATION_RIGHT = "rightAnimation";
	@Kroll.constant public static final String PROPERTY_LEFT_VIEW = "leftView";
	@Kroll.constant public static final String PROPERTY_LEFT_VIEW_DISPLACEMENT = "leftViewDisplacement";
	@Kroll.constant public static final String PROPERTY_LEFT_VIEW_WIDTH = "leftViewWidth";
	@Kroll.constant public static final String PROPERTY_RIGHT_VIEW = "rightView";
	@Kroll.constant public static final String PROPERTY_RIGHT_VIEW_DISPLACEMENT = "rightViewDisplacement";
	@Kroll.constant public static final String PROPERTY_RIGHT_VIEW_WIDTH = "rightViewWidth";
	@Kroll.constant public static final String PROPERTY_PANNING_MODE = "panningMode";
	@Kroll.constant public static final String PROPERTY_CENTER_VIEW = "centerView";
	@Kroll.constant public static final String PROPERTY_FADING = "fading";
	
}
