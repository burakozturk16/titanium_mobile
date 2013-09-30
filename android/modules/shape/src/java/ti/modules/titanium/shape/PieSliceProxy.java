
package ti.modules.titanium.shape;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.util.TiAnimatorSet;

import com.nineoldandroids.animation.PropertyValuesHolder;

import android.graphics.Point;

@Kroll.proxy(creatableInModule = ShapeModule.class, propertyAccessors={
	ShapeModule.PROPERTY_INNERRADIUS
})
public class PieSliceProxy extends ArcProxy{
	// Standard Debugging variables
	private static final String TAG = "ShapeProxy";
	protected Object innerRadius;
	
	public PieSliceProxy() {
		super();
		pathable = new PieSlice();
	}
	
	public PieSliceProxy(TiContext context) {
		this();
	}
	
	@Override
	protected void updatePath() {
		int width = parentBounds.width();
		int height = parentBounds.height();
		((PieSlice) pathable).innerRadius = computeRadius(this.innerRadius, width, height);
		super.updatePath();
	}

	@Override
	public void processProperties(KrollDict properties) {
		super.processProperties(properties);
		if (properties.containsKey(ShapeModule.PROPERTY_INNERRADIUS)) {
			this.innerRadius = properties.get(ShapeModule.PROPERTY_INNERRADIUS);
		}
	}
	
	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {
		if (key.equals(ShapeModule.PROPERTY_INNERRADIUS)) {
			this.innerRadius = newValue;
		}
		else super.propertyChanged(key, oldValue, newValue, proxy);
		redraw();
	}
	
	@Override
	protected void preparePropertiesSet(TiAnimatorSet tiSet, List<PropertyValuesHolder> propertiesList, KrollDict animOptions) {
		super.preparePropertiesSet(tiSet, propertiesList, animOptions);
		
		if (animOptions.containsKey(ShapeModule.PROPERTY_INNERRADIUS)) {
			int width = parentBounds.width();
			int height = parentBounds.height();
			Point currentRadius = computeRadius(this.innerRadius, width, height);
			Point animRadius = computeRadius(animOptions.get(ShapeModule.PROPERTY_INNERRADIUS), width, height);
			PropertyValuesHolder anim = PropertyValuesHolder.ofObject("innereRadius", new PointEvaluator(), currentRadius, animRadius);
			propertiesList.add(anim);
			
		}
	}
	
	//ANIMATION getter/setter
	public void setInnerRadius(Point point) {
		((PieSlice) pathable).innerRadius = point;
	}
	public Point getInnereRadius() {
		return ((PieSlice) pathable).innerRadius;		
	}
}