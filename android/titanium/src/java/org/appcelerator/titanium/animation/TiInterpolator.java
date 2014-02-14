package org.appcelerator.titanium.animation;


import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

public class TiInterpolator {
    protected double duration = 1.0f;
    
    public static class ReverseInterpolator  implements Interpolator
    {
    	Interpolator interpolator;
    	
    	public ReverseInterpolator(Interpolator interpolator)
    	{
    		this.interpolator = interpolator;
    	}
    	@Override
		public float getInterpolation(float input) {
			return 1.0f - interpolator.getInterpolation(1.0f - input);
		}
    }
    
    public enum Types {
		kLinear,
		kEaseInCubic,
		kEaseOutCubic,
		kEaseInOutCubic,
		kEaseInQuad,
		kEaseOutQuad,
		kEaseInOutQuad,
		kEaseInQuart,
		kEaseOutQuart,
		kEaseInOutQuart,
		kEaseInQuint,
		kEaseOutQuint,
		kEaseInOutQuint,
		kEaseInSin,
		kEaseOutSin,
		kEaseInOutSin,
		kEaseInBack,
		kEaseOutBack,
		kEaseInOutBack,
		kEaseInCircle,
		kEaseOutCircle,
		kEaseInOutCircle,
		kEaseInExpo,
		kEaseOutExpo,
		kEaseInOutExpo,
		kTransitionNb
	}
    
    public static Interpolator getInterpolator(int key, double duration)
    {
    	TiInterpolator.Types type = TiInterpolator.Types.values()[key];
    	switch (type) {
		case kEaseInCubic:
	    	return easeInCubic(duration);
		case kEaseOutCubic:
	    	return easeOutCubic(duration);
		case kEaseInOutCubic:
	    	return easeInOutCubic(duration);
		case kEaseInQuad:
	    	return easeInQuad(duration);
		case kEaseOutQuad:
	    	return easeOutQuad(duration);
		case kEaseInOutQuad:
	    	return easeInOutQuad(duration);
		case kEaseInQuart:
	    	return easeInQuart(duration);
		case kEaseOutQuart:
	    	return easeOutQuart(duration);
		case kEaseInOutQuart:
	    	return easeInOutQuart(duration);
		case kEaseInQuint:
	    	return easeInQuint(duration);
		case kEaseOutQuint:
	    	return easeOutQuint(duration);
		case kEaseInOutQuint:
	    	return easeInOutQuint(duration);
		case kEaseInSin:
	    	return easeInSin(duration);
		case kEaseOutSin:
	    	return easeOutSin(duration);
		case kEaseInOutSin:
	    	return easeInOutSin(duration);
		case kEaseInBack:
	    	return easeInBack(duration);
		case kEaseOutBack:
	    	return easeOutBack(duration);
		case kEaseInOutBack:
	    	return easeInOutBack(duration);
		case kEaseInExpo:
	    	return easeInExpo(duration);
		case kEaseOutExpo:
	    	return easeOutExpo(duration);
		case kEaseInOutExpo:
	    	return easeInOutExpo(duration);
		case kEaseInCircle:
	    	return easeInCircle(duration);
		case kEaseOutCircle:
	    	return easeOutCircle(duration);
		case kEaseInOutCircle:
	    	return easeInOutCircle(duration);
		case kLinear:
		default:
	    	return Linear(duration);
		}
    }

    public TiInterpolator(double duration) {
    	this.duration = duration;
    }
    
    public static Interpolator Linear(double duration)
    {
    	return  new LinearInterpolator();
    }

    public static Interpolator easeInCubic(double duration)
    {
    	return new CubicBezierInterpolator(0.55 , 0.055 , 0.675 , 0.19);
    }

    public static Interpolator easeOutCubic(double duration)
    {
    	return new CubicBezierInterpolator(0.215, 0.61, 0.355, 1.0);
    }

    public static Interpolator easeInOutCubic(double duration)
    {
    	return new CubicBezierInterpolator(0.645, 0.045, 0.355, 1.0);
    }

    public static Interpolator easeInQuad(double duration)
    {
    	return new CubicBezierInterpolator(0.55, 0.085, 0.68, 0.53);
    }

    public static Interpolator easeOutQuad(double duration)
    {
    	return new CubicBezierInterpolator(0.25, 0.46, 0.45, 0.94);
    }

    public static Interpolator easeInOutQuad(double duration)
    {
    	return new CubicBezierInterpolator(0.455, 0.03, 0.515, 0.955);
    }

    public static Interpolator easeInQuart(double duration)
    {
    	return new CubicBezierInterpolator(0.895, 0.03, 0.685, 0.22);
    }

    public static Interpolator easeOutQuart(double duration)
    {
    	return new CubicBezierInterpolator(0.165, 0.84, 0.44, 1.0);
    }
    
    public static Interpolator easeInOutQuart(double duration)
    {
    	return new CubicBezierInterpolator(0.77, 0.0, 0.175, 1.0);
    }

    public static Interpolator easeInQuint(double duration)
    {
    	return new CubicBezierInterpolator(0.755, 0.05, 0.855, 0.06);
    }
    
    public static Interpolator easeOutQuint(double duration)
    {
    	return new CubicBezierInterpolator(0.23, 1.0, 0.320, 1.0);
    }
    
    public static Interpolator easeInOutQuint(double duration)
    {
    	return new CubicBezierInterpolator(0.86, 0.0, 0.07, 1.0);
    }
    
    public static Interpolator easeInSin(double duration)
    {
    	return new CubicBezierInterpolator(0.47, 0.0, 0.745, 0.715);
    }
    
    public static Interpolator easeOutSin(double duration)
    {
    	return new CubicBezierInterpolator(0.39, 0.575, 0.565, 1.0);
    }
    
    public static Interpolator easeInOutSin(double duration)
    {
    	return new CubicBezierInterpolator(0.445, 0.05, 0.55, 0.95);
    }
    
    public static Interpolator easeInBack(double duration)
    {
    	return new CubicBezierInterpolator(0.6, -0.28, 0.735, 0.045);
    }
    
    public static Interpolator easeOutBack(double duration)
    {
    	return new CubicBezierInterpolator(0.175, 0.885, 0.320, 1.275);
    }
    
    public static Interpolator easeInOutBack(double duration)
    {
    	return new CubicBezierInterpolator(0.68, -0.55, 0.265, 1.55);
    }

    public static Interpolator easeInCircle(double duration)
    {
    	return new CubicBezierInterpolator(0.6, 0.04, 0.98, 0.335);
    }
    
    public static Interpolator easeOutCircle(double duration)
    {
    	return new CubicBezierInterpolator(0.075, 0.82, 0.165, 1.0);
    }
    
    public static Interpolator easeInOutCircle(double duration)
    {
    	return new CubicBezierInterpolator(0.785, 0.135, 0.15, 0.86);
    }

    public static Interpolator easeInExpo(double duration)
    {
    	return new CubicBezierInterpolator(0.95, 0.05, 0.795, 0.035);
    }
    
    public static Interpolator easeOutExpo(double duration)
    {
    	return new CubicBezierInterpolator(0.19, 1.0, 0.22, 1.0);
    }
    
    public static Interpolator easeInOutExpo(double duration)
    {
    	return new CubicBezierInterpolator(1.0, 0.0, 0.0, 1.0);
    }
}
