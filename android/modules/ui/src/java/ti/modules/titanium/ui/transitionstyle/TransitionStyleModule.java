package ti.modules.titanium.ui.transitionstyle;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.animation.TransitionHelper;

import ti.modules.titanium.ui.UIModule;

@Kroll.module(parentModule=UIModule.class)
public class TransitionStyleModule extends KrollModule {

	@Kroll.constant public static final int CUBE = TransitionHelper.Types.kTransitionCube.ordinal();
	@Kroll.constant public static final int SWIPE = TransitionHelper.Types.kTransitionSwipe.ordinal();
	@Kroll.constant public static final int SWIPEFADE = TransitionHelper.Types.kTransitionSwipeFade.ordinal();
	@Kroll.constant public static final int FLIP = TransitionHelper.Types.kTransitionFlip.ordinal();
}
