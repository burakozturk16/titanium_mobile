
package ti.modules.titanium.shape;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiContext;

@Kroll.proxy(creatableInModule = ShapeModule.class, propertyAccessors={
})
public class RectProxy extends ShapeProxy{
	// Standard Debugging variables
	private static final String TAG = "RectProxy";	
	
	public RectProxy() {
		super();
		pathable = new PRect();
	}
	
	public RectProxy(TiContext context) {
		this();
	}

}