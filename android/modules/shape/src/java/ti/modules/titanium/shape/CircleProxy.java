
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
	private static final String TAG = "ShapeProxy";
	private Circle circle;
	
	public static class Circle extends Pathable {
		private String radiusStr;
		private Direction direction;
		private TiPoint point;
		private AnchorPosition anchor;
		public Circle(TiPoint center, String radius) {
			super();
			this.direction = Direction.CW;
			this.anchor = AnchorPosition.CENTER;
			this.point = center;
			this.radiusStr = radius;
		}
		
		public Circle() {
			super();
			this.direction = Direction.CW;
			this.point = new TiPoint("50%", "50%");
			this.anchor = AnchorPosition.CENTER;
			this.radiusStr = "50%";
		}
		
		public void setPoint(TiPoint point) {
			this.point = point;
		}
		
		public void setAnchor(AnchorPosition anchor) {
			this.anchor = anchor;
		}
		
		public void setRadius(String radius) {
			this.radiusStr = radius;
		}
		
		public void updatePathForRect(Context context, Path path, int width, int height) {
			TiDimension radius;
			if (width < height) {
				radius = new TiDimension(radiusStr, TiDimension.TYPE_WIDTH);
			}
			else {
				radius = new TiDimension(radiusStr, TiDimension.TYPE_HEIGHT);
			}
			float pointX = point.getX().getAsPixels(context, width, height);
			float pointY = point.getY().getAsPixels(context, width, height);
			float rad = radius.getAsPixels(context, width, height);
			if (anchor == AnchorPosition.LEFT_TOP ||
					anchor == AnchorPosition.LEFT_MIDDLE || 
					anchor == AnchorPosition.LEFT_BOTTOM)
				pointX += rad;
			else if (anchor == AnchorPosition.RIGHT_TOP ||
					anchor == AnchorPosition.RIGHT_MIDDLE || 
					anchor == AnchorPosition.RIGHT_BOTTOM)
				pointX = width - pointX - rad;
			
			if (anchor == AnchorPosition.LEFT_TOP ||
					anchor == AnchorPosition.TOP_MIDDLE || 
					anchor == AnchorPosition.RIGHT_TOP)
				pointY += rad;
			else if (anchor == AnchorPosition.LEFT_BOTTOM ||
					anchor == AnchorPosition.BOTTOM_MIDDLE || 
					anchor == AnchorPosition.RIGHT_BOTTOM)
				pointY = height - pointY - rad;

			path.addCircle(pointX, pointY, rad, direction);
		}
	}
	
	@Override
	protected void updatePath() {
		path.reset();
		circle.updatePathForRect(context, path, parentBounds.width(), parentBounds.height());
	}
	
	public CircleProxy() {
		super();
		circle = new Circle();
		path = new Path();
	}
	
	public CircleProxy(TiContext context) {
		this();
	}

	public Path getPath() {
		return path;
	}

//	@Override
//	public void propertyChanged(String key, Object oldValue, Object newValue,
//			KrollProxy proxy) {
//		if (key.equals(ShapeModule.PROPERTY_POINT)) {
//			circle.setPoint(TiConvert.toPoint(newValue));
//		}
//		else if (key.equals(ShapeModule.PROPERTY_RADIUS)) {
//			circle.setRadius(TiConvert.toString(newValue));
//		}
//		else if (key.equals(ShapeModule.PROPERTY_ANCHOR)) {
//			circle.setAnchor(AnchorPosition.values()[TiConvert.toInt(newValue)]);
//		}
//		else
//			super.processProperties(properties);
//	}

	@Override
	public void handleCreationDict(KrollDict properties) {
		super.handleCreationDict(properties);
		if (properties == null) return;
		if (properties.containsKey(ShapeModule.PROPERTY_POINT)) {
			circle.setPoint(TiConvert.toPoint(properties.get(ShapeModule.PROPERTY_POINT)));
		}
		if (properties.containsKey(ShapeModule.PROPERTY_RADIUS)) {
			circle.setRadius(properties.getString(ShapeModule.PROPERTY_RADIUS));
		}
		if (properties.containsKey(ShapeModule.PROPERTY_ANCHOR)) {
			circle.setAnchor(AnchorPosition.values()[properties.getInt(ShapeModule.PROPERTY_ANCHOR)]);
		}
	}
	
	@Kroll.method
	public void clear() {
		path.reset();
	}

	@Kroll.method
	@Kroll.setProperty
	public void setPoint(Object value) {
		setProperty(ShapeModule.PROPERTY_POINT, value);
		circle.setPoint(new TiPoint((HashMap)value));
	}
	@Kroll.method
	@Kroll.getProperty
	public Object getPoint() {
		return getProperty(ShapeModule.PROPERTY_POINT);
	}

	@Kroll.method
	@Kroll.setProperty
	public void setRadius(Object value) {
		setProperty(ShapeModule.PROPERTY_RADIUS, value);
		circle.setRadius(TiConvert.toString(value));
	}
	@Kroll.method
	@Kroll.getProperty
	public Object getRadius() {
		return getProperty(ShapeModule.PROPERTY_RADIUS);
	}

	@Kroll.method
	@Kroll.setProperty
	public void setAnchor(Object value) {
		setProperty(ShapeModule.PROPERTY_ANCHOR, value);
		circle.setAnchor(AnchorPosition.values()[TiConvert.toInt(value)]);
	}
	@Kroll.method
	@Kroll.getProperty
	public Object getAnchor() {
		return getProperty(ShapeModule.PROPERTY_ANCHOR);
	}

}