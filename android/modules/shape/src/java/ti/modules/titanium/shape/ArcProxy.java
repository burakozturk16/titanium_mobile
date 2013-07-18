
package ti.modules.titanium.shape;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.TiPoint;
import org.appcelerator.titanium.util.TiConvert;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;

@SuppressWarnings("rawtypes")
@Kroll.proxy(creatableInModule = ShapeModule.class, propertyAccessors={
	"startAngle","center","topLeft","widthRadius","heightRadius","xRadius","yRadius"
})
public class ArcProxy extends ShapeProxy{
	// Standard Debugging variables
	private static final String TAG = "ShapeProxy";
	private Arc arc;
	
	public static class Arc extends Pathable {
		protected float startAngle;
		protected float sweepAngle;
		private TiDimension radiusX;
		private TiDimension radiusY;
		protected TiPoint point;
		private AnchorPosition anchor;
		public Arc(TiRect rect, float startAngle, float sweepAngle) {
			super();
			this.anchor = AnchorPosition.CENTER;
			this.startAngle = startAngle;
			this.sweepAngle = sweepAngle;
		}
		
		public Arc(TiPoint point, String radiusStr, float startAngle, float sweepAngle) {
			super();
			this.anchor = AnchorPosition.CENTER;
			this.point = point;
			this.radiusX = new TiDimension(radiusStr, TiDimension.TYPE_WIDTH);
			this.radiusY = new TiDimension(radiusStr, TiDimension.TYPE_HEIGHT);
			this.startAngle = startAngle;
			this.sweepAngle = sweepAngle;
		}
		
		public Arc(TiPoint point, String radiusXStr, String radiusYStr, float startAngle, float sweepAngle) {
			super();
			this.anchor = AnchorPosition.CENTER;
			this.point = point;
			this.radiusX = new TiDimension(radiusXStr, TiDimension.TYPE_WIDTH);
			this.radiusY = new TiDimension(radiusYStr, TiDimension.TYPE_HEIGHT);
			this.startAngle = startAngle;
			this.sweepAngle = sweepAngle;
		}
		
		public Arc() {
			super();
			this.point = new TiPoint("50%", "50%");
			this.anchor = AnchorPosition.CENTER;
			this.startAngle = 0;
			this.sweepAngle = 0;
		}
		
		public void setStartAngle(float startAngle) {
			this.startAngle = startAngle;
		}
		
		public void setSweepAngle(float sweepAngle) {
			Log.d(TAG, "setSweepAngle " + sweepAngle, Log.DEBUG_MODE);
			this.sweepAngle = sweepAngle;
		}

		public void setPoint(TiPoint point) {
			this.point = point;
		}
		
		public void setAnchor(AnchorPosition anchor) {
			this.anchor = anchor;
		}
		
		public void setXRadius(String radius) {
			this.radiusX = new TiDimension(radius, TiDimension.TYPE_WIDTH);
		}
		
		public void setYRadius(String radius) {
			this.radiusY = new TiDimension(radius, TiDimension.TYPE_HEIGHT);
		}

		public void setWidthRadius(String radius) {
			this.radiusX = new TiDimension(radius, TiDimension.TYPE_WIDTH);
			this.radiusY = new TiDimension(radius, TiDimension.TYPE_WIDTH);
		}
		
		public void setHeightRadius(String radius) {
			this.radiusX = new TiDimension(radius, TiDimension.TYPE_HEIGHT);
			this.radiusY = new TiDimension(radius, TiDimension.TYPE_HEIGHT);
		}
		
		@Override
		public void updatePathForRect(Context context, Path path, int width, int height) {
			float pointX = point.getX().getAsPixels(context, width, height);
			float pointY = point.getY().getAsPixels(context, width, height);
			float radx = radiusX.getAsPixels(context, width, height);
			float rady = radiusY.getAsPixels(context, width, height);
			if (anchor == AnchorPosition.LEFT_TOP ||
					anchor == AnchorPosition.LEFT_MIDDLE || 
					anchor == AnchorPosition.LEFT_BOTTOM)
				pointX += radx;
			else if (anchor == AnchorPosition.RIGHT_TOP ||
					anchor == AnchorPosition.RIGHT_MIDDLE || 
					anchor == AnchorPosition.RIGHT_BOTTOM)
				pointX = width - pointX - radx;
			
			if (anchor == AnchorPosition.LEFT_TOP ||
					anchor == AnchorPosition.TOP_MIDDLE || 
					anchor == AnchorPosition.RIGHT_TOP)
				pointY += rady;
			else if (anchor == AnchorPosition.LEFT_BOTTOM ||
					anchor == AnchorPosition.BOTTOM_MIDDLE || 
					anchor == AnchorPosition.RIGHT_BOTTOM)
				pointY = height - pointY - rady;
			RectF rect = new RectF(pointX - radx, pointY - rady, pointX + radx, pointY + rady);
			path.addArc(rect,  startAngle-90.0f,  sweepAngle);
		}
	}
	
	@Override
	protected void updatePath() {
		path.reset();
		arc.updatePathForRect(context, path, parentBounds.width(), parentBounds.height());
	}
	
	public ArcProxy() {
		super();
		arc = new Arc();
		path = new Path();
	}
	
	public ArcProxy(TiContext context) {
		this();
	}

	public Path getPath() {
		return path;
	}

	@Override
	public void handleCreationDict(KrollDict properties) {
		super.handleCreationDict(properties);
		if (properties == null) return;
		if (properties.containsKey("startAngle")) {
			arc.setStartAngle(properties.getFloat("startAngle"));
		}
		if (properties.containsKey(ShapeModule.PROPERTY_SWEEPANGLE)) {
			arc.setSweepAngle(properties.getFloat(ShapeModule.PROPERTY_SWEEPANGLE));
		}
		if (properties.containsKey(ShapeModule.PROPERTY_STARTANGLE)) {
			arc.setStartAngle(properties.getFloat(ShapeModule.PROPERTY_STARTANGLE));
		}
		if (properties.containsKey(ShapeModule.PROPERTY_POINT)) {
			arc.setPoint(TiConvert.toPoint(properties.get(ShapeModule.PROPERTY_POINT)));
		}
		if (properties.containsKey(ShapeModule.PROPERTY_X_RADIUS)) {
			arc.setXRadius(properties.getString(ShapeModule.PROPERTY_X_RADIUS));
		}
		if (properties.containsKey(ShapeModule.PROPERTY_Y_RADIUS)) {
			arc.setYRadius(properties.getString(ShapeModule.PROPERTY_Y_RADIUS));
		}
		if (properties.containsKey(ShapeModule.PROPERTY_WIDTH_RADIUS)) {
			arc.setWidthRadius(properties.getString(ShapeModule.PROPERTY_WIDTH_RADIUS));
		}
		if (properties.containsKey(ShapeModule.PROPERTY_HEIGHT_RADIUS)) {
			arc.setHeightRadius(properties.getString(ShapeModule.PROPERTY_HEIGHT_RADIUS));
		}
		if (properties.containsKey(ShapeModule.PROPERTY_ANCHOR)) {
			arc.setAnchor(AnchorPosition.values()[properties.getInt(ShapeModule.PROPERTY_ANCHOR)]);
		}
	}
	
	@Kroll.method
	public void clear() {
		path.reset();
	}
	
	@Kroll.method
	@Kroll.setProperty
	public void setSweepAngle(Object value) {
		setProperty(ShapeModule.PROPERTY_SWEEPANGLE, value);
		arc.setSweepAngle(TiConvert.toFloat(value));
	}
	@Kroll.method
	@Kroll.getProperty
	public Object getSweepAngle() {
		return getProperty(ShapeModule.PROPERTY_SWEEPANGLE);
	}

	@Kroll.method
	@Kroll.setProperty
	public void setStartAngle(Object value) {
		setProperty(ShapeModule.PROPERTY_STARTANGLE, value);
		arc.setStartAngle(TiConvert.toFloat(value));
	}
	@Kroll.method
	@Kroll.getProperty
	public Object getStartAngle() {
		return getProperty(ShapeModule.PROPERTY_STARTANGLE);
	}
	
	@Kroll.method
	@Kroll.setProperty
	public void setPoint(Object value) {
		setProperty(ShapeModule.PROPERTY_POINT, value);
		arc.setPoint(new TiPoint((HashMap)value));
	}
	@Kroll.method
	@Kroll.getProperty
	public Object getPoint() {
		return getProperty(ShapeModule.PROPERTY_POINT);
	}

	@Kroll.method
	@Kroll.setProperty
	public void setXRadius(Object value) {
		setProperty(ShapeModule.PROPERTY_X_RADIUS, value);
		arc.setXRadius(TiConvert.toString(value));
	}
	@Kroll.method
	@Kroll.getProperty
	public Object getXRadius() {
		return getProperty(ShapeModule.PROPERTY_X_RADIUS);
	}
	@Kroll.method
	@Kroll.setProperty
	public void setYRadius(Object value) {
		setProperty(ShapeModule.PROPERTY_Y_RADIUS, value);
		arc.setYRadius(TiConvert.toString(value));
	}
	@Kroll.method
	@Kroll.getProperty
	public Object getYRadius() {
		return getProperty(ShapeModule.PROPERTY_Y_RADIUS);
	}
	@Kroll.method
	@Kroll.setProperty
	public void setWidthRadius(Object value) {
		setProperty(ShapeModule.PROPERTY_WIDTH_RADIUS, value);
		arc.setWidthRadius(TiConvert.toString(value));
	}
	@Kroll.method
	@Kroll.getProperty
	public Object getWidthRadius() {
		return getProperty(ShapeModule.PROPERTY_WIDTH_RADIUS);
	}
	@Kroll.method
	@Kroll.setProperty
	public void setHeighthRadius(Object value) {
		setProperty(ShapeModule.PROPERTY_HEIGHT_RADIUS, value);
		arc.setHeightRadius(TiConvert.toString(value));
	}
	@Kroll.method
	@Kroll.getProperty
	public Object getHeightRadius() {
		return getProperty(ShapeModule.PROPERTY_HEIGHT_RADIUS);
	}
	@Kroll.method
	@Kroll.setProperty
	public void setAnchor(Object value) {
		setProperty(ShapeModule.PROPERTY_ANCHOR, value);
		arc.setAnchor(AnchorPosition.values()[TiConvert.toInt(value)]);
	}
	@Kroll.method
	@Kroll.getProperty
	public Object getAnchor() {
		return getProperty(ShapeModule.PROPERTY_ANCHOR);
	}
}