
package ti.modules.titanium.shape;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.TiPoint;
import org.appcelerator.titanium.util.TiConvert;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Path.Direction;
import android.view.Gravity;

@SuppressWarnings("rawtypes")
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