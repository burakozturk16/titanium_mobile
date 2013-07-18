
package ti.modules.titanium.shape;

import java.util.ArrayList;
import java.util.HashMap;

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
import android.graphics.Path.Direction;
import android.graphics.RectF;
import android.graphics.Region;

@SuppressWarnings("rawtypes")
@Kroll.proxy(creatableInModule = ShapeModule.class, propertyAccessors={
	TiC.PROPERTY_NAME
})
public class PathProxy extends ShapeProxy{
	// Standard Debugging variables
	private static final String TAG = "ShapeProxy";
	private Path fillPath;
	private final ArrayList<Pathable> mPathables;
	
	private class MoveTo extends Pathable {
		private TiPoint point;
		public MoveTo(TiPoint point) {
			super();
			this.point = point;
		}
		public void updatePathForRect(Context context, Path path, int width, int height) {

			float pointX = point.getX().getAsPixels(context, width, height);
			float pointY = point.getY().getAsPixels(context, width, height);
			path.moveTo(pointX, pointY);
		}
	}
	
	private class Oval extends Pathable {
		private Direction direction;
		private TiRect rect;
		public Oval(TiRect rect) {
			super();
			this.direction = Direction.CW;
			this.rect = rect;
		}
		public void updatePathForRect(Context context, Path path, int width, int height) {
			path.addOval(rect.getAsPixels(context, width, height), direction);
		}
	}
	private class PRect extends Pathable {
		private Direction direction;
		private TiRect rect;
		public PRect(TiRect rect) {
			super();
			this.direction = Direction.CW;
			this.rect = rect;
		}
		public void updatePathForRect(Context context, Path path, int width, int height) {
			path.addRect(rect.getAsPixels(context, width, height), direction);
		}
	}
	private class PRoundRect extends Pathable {
		private Direction direction;
		private TiRect rect;
		TiDimension radiusx;
		TiDimension radiusy;
		public PRoundRect(TiRect rect, String radiusx, String radiusy) {
			super();
			this.direction = Direction.CW;
			this.radiusx = new TiDimension (radiusx, TiDimension.TYPE_WIDTH);
			this.radiusy = new TiDimension (radiusy, TiDimension.TYPE_HEIGHT);
		}
		public PRoundRect(TiRect rect, String radius) {
			super();
			this.direction = Direction.CW;
			this.radiusx = new TiDimension (radius, TiDimension.TYPE_WIDTH);
			this.radiusy = new TiDimension (radius, TiDimension.TYPE_HEIGHT);
		}
		public void updatePathForRect(Context context, Path path, int width, int height) {
			path.addRoundRect(rect.getAsPixels(context, width, height), radiusx.getAsPixels(context, width, height), radiusy.getAsPixels(context, width, height), direction);
		}
	}
	
	@Override
	protected void updatePath() {
		path.reset();
		for (int i = 0; i < mPathables.size(); i++) {
			Pathable pathable = mPathables.get(i);
			pathable.updatePathForRect(context, path, parentBounds.width(), parentBounds.height());
		}
		fillPath = new Path(path);
		fillPath.close();
	}
	
	public PathProxy() {
		super();
		fillPath = new Path();
		mPathables = new ArrayList<PathProxy.Pathable>();
	}
	
	public PathProxy(TiContext context) {
		this();
	}
	
	@Kroll.method
	public void clear() {
		path.reset();
		fillPath.reset();
		mPathables.clear();
	}
	
	@Kroll.method
	public void addArc(Object[] args) {
		if (args.length == 3) {
			mPathables.add(new ArcProxy.Arc(new TiRect((HashMap)args[0]), TiConvert.toFloat(args[1]), TiConvert.toFloat(args[2])));
		} else if (args.length == 6) {
			TiRect rect = new TiRect(TiConvert.toString(args[0]),
					TiConvert.toString(args[1]),
					TiConvert.toString(args[2]),
					TiConvert.toString(args[3]));
			mPathables.add(new ArcProxy.Arc(rect, TiConvert.toFloat(args[4]), TiConvert.toFloat(args[5])));
		}
		else {
			Log.e(TAG, "Wrong number of arguments", Log.DEBUG_MODE);
		}
	}
	
	@Kroll.method
	public void moveTo(Object[] args) {
		if (args.length == 1) {
			mPathables.add(new MoveTo(new TiPoint((HashMap)args[0])));
		} else if (args.length == 2) {
			mPathables.add(new MoveTo(new TiPoint(TiConvert.toString(args[0]), TiConvert.toString(args[1]))));
		}
		else {
			Log.e(TAG, "Wrong number of arguments", Log.DEBUG_MODE);
		}
	}

	@Kroll.method
	public void addCircle(Object[] args) {
		if (args.length == 2) {
			mPathables.add(new CircleProxy.Circle(new TiPoint((HashMap)args[0]), TiConvert.toString(args[1])));
		} else if (args.length == 3) {
			mPathables.add(new CircleProxy.Circle(new TiPoint(TiConvert.toString(args[0]), TiConvert.toString(args[1])), TiConvert.toString(args[2])));
		}
		else {
			Log.e(TAG, "Wrong number of arguments", Log.DEBUG_MODE);
		}
	}
	
	@Kroll.method
	public void addOval(Object[] args) {
		if (args.length == 1) {
			mPathables.add(new Oval(new TiRect((HashMap)args[0])));
		} else if (args.length == 4) {
			mPathables.add(new Oval(new TiRect(TiConvert.toString(args[0]), TiConvert.toString(args[1]), TiConvert.toString(args[2]), TiConvert.toString(args[3]))));
		}
		else {
			Log.e(TAG, "Wrong number of arguments", Log.DEBUG_MODE);
		}
	}

	@Kroll.method
	public void addRect(Object[] args) {
		if (args.length == 1) {
			mPathables.add(new PRect(new TiRect((HashMap)args[0])));
		} else if (args.length == 4) {
			mPathables.add(new PRect(new TiRect(TiConvert.toString(args[0]), TiConvert.toString(args[1]), TiConvert.toString(args[2]), TiConvert.toString(args[3]))));
		}
		else {
			Log.e(TAG, "Wrong number of arguments", Log.DEBUG_MODE);
		}
	}
	
	@Kroll.method
	public void addRoundedRect(Object[] args) {
		if (args.length == 2) {
			mPathables.add(new PRoundRect(new TiRect((HashMap)args[0]), TiConvert.toString(args[1])));
		} else if (args.length == 3) {
			mPathables.add(new PRoundRect(new TiRect((HashMap)args[0]), TiConvert.toString(args[1]), TiConvert.toString(args[2])));
		} else if (args.length == 5) {
			TiRect rect = new TiRect(TiConvert.toString(args[0]),
					TiConvert.toString(args[1]),
					TiConvert.toString(args[2]),
					TiConvert.toString(args[3]));
			mPathables.add(new PRoundRect(rect, TiConvert.toString(args[4])));
		} else if (args.length == 6) {
			TiRect rect = new TiRect(TiConvert.toString(args[0]),
					TiConvert.toString(args[1]),
					TiConvert.toString(args[2]),
					TiConvert.toString(args[3]));
			mPathables.add(new PRoundRect(rect, TiConvert.toString(args[4]), TiConvert.toString(args[5])));
		}
		else {
			Log.e(TAG, "Wrong number of arguments", Log.DEBUG_MODE);
		}
	}
}