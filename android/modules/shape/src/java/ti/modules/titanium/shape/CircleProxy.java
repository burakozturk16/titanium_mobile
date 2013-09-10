
package ti.modules.titanium.shape;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiContext;

@Kroll.proxy(creatableInModule = ShapeModule.class, propertyAccessors={
})
public class CircleProxy extends ShapeProxy{
	// Standard Debugging variables
	private static final String TAG = "CircleProxy";	
	
	public CircleProxy() {
		super();
		pathable = new Circle();
	}
	
	public CircleProxy(TiContext context) {
		this();
	}

}