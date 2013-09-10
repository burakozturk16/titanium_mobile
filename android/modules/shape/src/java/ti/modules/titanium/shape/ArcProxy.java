
package ti.modules.titanium.shape;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.util.TiAnimatorSet;
import org.appcelerator.titanium.util.TiConvert;

import android.animation.PropertyValuesHolder;
import android.annotation.TargetApi;
import android.os.Build;

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
		redraw();
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void preparePropertiesSet(TiAnimatorSet tiSet, List<PropertyValuesHolder> propertiesList, KrollDict animOptions) {
		super.preparePropertiesSet(tiSet, propertiesList, animOptions);
		
		createAnimForFloat(ShapeModule.PROPERTY_SWEEPANGLE, animOptions, properties, propertiesList, 0.0f);
		createAnimForFloat(ShapeModule.PROPERTY_STARTANGLE, animOptions, properties, propertiesList, 0.0f);
	}
	
	public void setStartAngle(float value) {
		((Arc) pathable).setStartAngle(value);
	}
	public float getStartAngle() {
		return((Arc) pathable).startAngle;
	}
	
	public void setSweepAngle(float value) {
		((Arc) pathable).setSweepAngle(value);
	}
	public float getSweepAngle() {
		return((Arc) pathable).sweepAngle;
	}
}