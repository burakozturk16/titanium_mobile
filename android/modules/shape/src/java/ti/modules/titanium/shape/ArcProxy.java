
package ti.modules.titanium.shape;
import java.util.HashMap;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollPropertyChange;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.TiPoint;
import org.appcelerator.titanium.util.TiAnimatorSet;
import org.appcelerator.titanium.util.TiConvert;

import ti.modules.titanium.shape.ShapeProxy.PointEvaluator;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;

@SuppressWarnings("rawtypes")
@Kroll.proxy(creatableInModule = ShapeModule.class, propertyAccessors={
	ShapeModule.PROPERTY_SWEEPANGLE,ShapeModule.PROPERTY_STARTANGLE
})
public class ArcProxy extends ShapeProxy{
	// Standard Debugging variables
	private static final String TAG = "ShapeProxy";
	
	public ArcProxy() {
		super();
		pathable = new Arc();
	}
	
	public ArcProxy(TiContext context) {
		this();
	}

	@Override
	public void processProperties(KrollDict properties) {
		super.processProperties(properties);
		if (properties.containsKey(ShapeModule.PROPERTY_SWEEPANGLE)) {
			((Arc) pathable).setSweepAngle(properties.getFloat(ShapeModule.PROPERTY_SWEEPANGLE));
		}
		if (properties.containsKey(ShapeModule.PROPERTY_STARTANGLE)) {
			((Arc) pathable).setStartAngle(properties.getFloat(ShapeModule.PROPERTY_STARTANGLE));
		}
	}
	
	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {
		if (key.equals(ShapeModule.PROPERTY_SWEEPANGLE)) {
			setSweepAngle(TiConvert.toFloat(newValue));
		}
		else if (key.equals(ShapeModule.PROPERTY_STARTANGLE)) {
			setStartAngle(TiConvert.toFloat(newValue));
		}
		else super.propertyChanged(key, oldValue, newValue, proxy);
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void prepareAnimatorSet(TiAnimatorSet tiSet, List<Animator> list, HashMap options) {
		super.prepareAnimatorSet(tiSet, list, options);
		
		KrollDict animOptions = new KrollDict(options);
		KrollDict properties = getProperties();
		
		createAnimForFloat(ShapeModule.PROPERTY_SWEEPANGLE, animOptions, properties, list, 0.0f);
		createAnimForFloat(ShapeModule.PROPERTY_STARTANGLE, animOptions, properties, list, 0.0f);
	}
	
	@Kroll.method
	public void clear() {
		path.reset();
	}
	
//	@Kroll.method
//	@Kroll.setProperty
//	public void setSweepAngle(Object value) {
//		setProperty(ShapeModule.PROPERTY_SWEEPANGLE, value);
//		((Arc) pathable).setSweepAngle(TiConvert.toFloat(value));
//	}
//	@Kroll.method
//	@Kroll.getProperty
//	public Object getSweepAngle() {
//		return getProperty(ShapeModule.PROPERTY_SWEEPANGLE);
//	}

//	@Kroll.method
//	@Kroll.setProperty
//	public void setStartAngle(Object value) {
//		setProperty(ShapeModule.PROPERTY_STARTANGLE, value);
//		((Arc) pathable).setStartAngle(TiConvert.toFloat(value));
//	}

	public void setStartAngle(float value) {
		((Arc) pathable).setStartAngle(value);
		redraw();
	}
	public float getStartAngle() {
		return((Arc) pathable).startAngle;
	}
	
	public void setSweepAngle(float value) {
		((Arc) pathable).setSweepAngle(value);
		redraw();
	}
	public float getSweepAngle() {
		return((Arc) pathable).sweepAngle;
	}
}