package ti.modules.titanium.ui.transitionstyle;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.transition.TransitionHelper;

import ti.modules.titanium.ui.UIModule;

@Kroll.module(parentModule=UIModule.class)
public class TransitionStyleModule extends KrollModule {

	@Kroll.constant public static final int CUBE = TransitionHelper.Types.kTransitionCube.ordinal();
	@Kroll.constant public static final int CAROUSEL = TransitionHelper.Types.kTransitionCarousel.ordinal();
	@Kroll.constant public static final int SWIPE = TransitionHelper.Types.kTransitionSwipe.ordinal();
	@Kroll.constant public static final int SWIPE_FADE = TransitionHelper.Types.kTransitionSwipeFade.ordinal();
	@Kroll.constant public static final int FLIP = TransitionHelper.Types.kTransitionFlip.ordinal();
	@Kroll.constant public static final int FADE = TransitionHelper.Types.kTransitionFade.ordinal();
	@Kroll.constant public static final int BACK_FADE = TransitionHelper.Types.kTransitionBackFade.ordinal();
	@Kroll.constant public static final int FOLD = TransitionHelper.Types.kTransitionFold.ordinal();
	@Kroll.constant public static final int PUSH_ROTATE = TransitionHelper.Types.kTransitionPushRotate.ordinal();
	@Kroll.constant public static final int SCALE = TransitionHelper.Types.kTransitionScale.ordinal();
	@Kroll.constant public static final int SLIDE = TransitionHelper.Types.kTransitionSlide.ordinal();
    @Kroll.constant public static final int SWIPE_DUAL_FADE = TransitionHelper.Types.kTransitionSwipeDualFade.ordinal();
    @Kroll.constant public static final int MODERN_PUSH = TransitionHelper.Types.kTransitionModernPush.ordinal();

	@Kroll.constant public static final int LEFT_TO_RIGHT = TransitionHelper.SubTypes.kLeftToRight.ordinal();
	@Kroll.constant public static final int RIGHT_TO_LEFT = TransitionHelper.SubTypes.kRightToLeft.ordinal();
	@Kroll.constant public static final int TOP_TO_BOTTOM = TransitionHelper.SubTypes.kTopToBottom.ordinal();
	@Kroll.constant public static final int BOTTOM_TO_TOP = TransitionHelper.SubTypes.kBottomToTop.ordinal();

}
