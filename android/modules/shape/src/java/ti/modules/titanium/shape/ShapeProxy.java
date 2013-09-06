
package ti.modules.titanium.shape;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollPropertyChange;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollProxyListener;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.TiPoint;
import org.appcelerator.titanium.proxy.AnimatableProxy;
import org.appcelerator.titanium.util.TiAnimatorSet;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.Ti2DMatrix;
import org.appcelerator.titanium.view.TiBackgroundDrawable;


import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.MaskFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Paint.Style;
import android.graphics.Path.Direction;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Build;
import android.view.MotionEvent;
import android.view.animation.LinearInterpolator;

@SuppressLint("NewApi")
@Kroll.proxy(creatableInModule = ShapeModule.class, propertyAccessors={
	TiC.PROPERTY_NAME
})
public class ShapeProxy extends AnimatableProxy implements KrollProxyListener {
	// Standard Debugging variables
	private static final String TAG = "PathProxy";
	private ShapeViewProxy shapeViewProxy;
	protected Paint fillPaint;
	protected Paint linePaint;
	protected ShapeDrawable lineGradient;
	protected ShapeDrawable fillGradient;
	protected Rect currentBounds;
	protected Rect parentBounds;
	protected Region currentRegion;
	protected Context context;
	private final ArrayList<ShapeProxy> mShapes;
	protected Path path;
	
	protected Pathable pathable;
	protected ArrayList<Pathable> pathables;
	
	protected Object radius;
	protected TiPoint center;
	protected AnchorPosition anchor;
	
	private Ti2DMatrix transform;
	private Matrix matrix;
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public class PointEvaluator implements TypeEvaluator<Point> {
		
		public PointEvaluator() {
		}
		
		public Point evaluate(float fraction, Point startValue,
				Point endValue) {
			return new Point((int)(fraction*(endValue.x  - startValue.x) + startValue.x), (int)(fraction*(endValue.x  - startValue.x) + startValue.x));
		}

	}

	public final TiPoint DEFAULT_RADIUS = new TiPoint("50%", null);
	public TiPoint getDefaultRadius() {
		return DEFAULT_RADIUS;
	}
	

	public final TiPoint DEFAULT_CENTER = new TiPoint(0, 0);
	public TiPoint getDefaultCenter() {
		return DEFAULT_CENTER;
	}
	
	public Point computePoint(TiPoint point_, AnchorPosition anchor_ , int width, int height, Point decale)
	{
		Point result = new Point(0,0);
		if (point_ == null){point_ = getDefaultCenter();};
		if (anchor_ == AnchorPosition.CENTER) {
			result.x = point_.getX().getAsPixels(context, width, height) + width/2;
			result.y = point_.getY().getAsPixels(context, width, height) + height/2;
			return result;
		}
		else if (anchor_ == AnchorPosition.RIGHT_TOP ||
				anchor_ == AnchorPosition.RIGHT_MIDDLE || 
						anchor_ == AnchorPosition.RIGHT_BOTTOM) {
			result.x  = point_.getX().getAsPixels(context, width - 2*decale.x, height);
			result.x  = width - result.x - decale.x;
		}
		else if (anchor_ == AnchorPosition.BOTTOM_MIDDLE ||
				anchor_ == AnchorPosition.TOP_MIDDLE) {
			result.x  = point_.getX().getAsPixels(context, width/2 - decale.x, height) + width/2;
		}
		else {
			result.x = point_.getX().getAsPixels(context, width - 2*decale.x, height);
			result.x += decale.x;
		}
		
		if (anchor_ == AnchorPosition.LEFT_BOTTOM ||
				anchor_ == AnchorPosition.BOTTOM_MIDDLE || 
						anchor_ == AnchorPosition.RIGHT_BOTTOM) {
			result.y = point_.getY().getAsPixels(context, width, height - 2*decale.y);
			result.y = height - result.y - decale.y;
		}
		else if (anchor_ == AnchorPosition.LEFT_MIDDLE ||
				anchor_ == AnchorPosition.RIGHT_MIDDLE) {
			result.y  = point_.getY().getAsPixels(context, width, height/2 - decale.y) + height/2;
		}
		else {
			result.y = point_.getY().getAsPixels(context, width, height - 2*decale.y);
			result.y += decale.y;
		}
		return result;
	}
	
	
	public Point computeRadius(Object radius_ , int width, int height)
	{
		Point result = new Point(0,0);
		TiPoint radius;
		Boolean needsMin = false;
		if (radius_ == null) {
			radius = getDefaultRadius();
			needsMin = true;
		}
		else if (radius_ instanceof TiPoint) {
			radius = (TiPoint) radius_;
		}
		else if (radius_ instanceof HashMap) {
			radius = new TiPoint((HashMap)radius_);
		}
		else {
			radius = new TiPoint(TiConvert.toString(radius_), null);
			needsMin = true;
		}
		if (!radius.getX().isUnitUndefined() && !radius.getY().isUnitUndefined()) {
			result.x  = radius.getX().getAsPixels(context, width, height);
			result.y  = radius.getY().getAsPixels(context, width, height);
	    } else if(!radius.getX().isUnitUndefined()) {
	    	result.x  = result.y  = radius.getX().getAsPixels(context, needsMin?Math.min(width, height):width, height);
	    } else if(!radius.getY().isUnitUndefined()) {
	    	result.x  = result.y  = radius.getY().getAsPixels(context, width, height);
	    }
		return result;
	}
	
	public class Pathable {
		protected Point radius;
		protected Point center;
		protected Direction direction;
		
		public Pathable() {
			super();
			this.direction = Direction.CW;
			this.center = new Point(0,0);
		}
		
		public void setCenter(Point center) {
			this.center = center;
		}
		
		public void setRadius(Point radius) {
			this.radius = radius;
		}
		
		
		public void updatePathForRect(Context context, Path path, int width, int height) {}
	}
	
	public class MoveTo extends Pathable {
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
	
	protected class Arc extends Pathable {
		protected float startAngle;
		protected float sweepAngle;
		public Arc() {
			super();
			startAngle = -45;
			sweepAngle = 90;
		}

		public void setStartAngle(float startAngle) {
			this.startAngle = startAngle;
		}
		
		public void setSweepAngle(float sweepAngle) {
			this.sweepAngle = sweepAngle;
		}
		
		@Override
		public void updatePathForRect(Context context, Path path, int width, int height) {
			
			RectF rect = new RectF(center.x - radius.x, center.y - radius.x, center.x + radius.y, center.y + radius.y);
			path.addArc(rect,  startAngle-90.0f,  sweepAngle);
		}
	}
	
	protected class Circle extends Pathable {
		
		public Circle() {
			super();
		}
		
		public void updatePathForRect(Context context, Path path, int width, int height) {

			path.addCircle(center.x, center.y, radius.x, direction);
		}
	}
	
	protected class Oval extends Pathable {
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
	protected class PRect extends Pathable {
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
	protected class PRoundRect extends Pathable {
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
	
	private Class opFromString(String value)
	{
	    if (value == null) return null;
		if (value.equals("circle"))
		{
			return Circle.class;
		}
		else if (value.equals("rect"))
		{
			return PRect.class;
		}
	    else if (value.equals("roundedrect"))
		{
			return PRoundRect.class;
		}
	    else if (value.equals("arc"))
		{
			return Arc.class;
		}
	    else if (value.equals("ellipse"))
		{
			return Oval.class;
		}
	    else if (value.equals("points"))
		{
			return null;
		}
		return null;
	}

	public ShapeProxy() {
		super();
		mShapes = new ArrayList<ShapeProxy>();
		shapeViewProxy = null;
		fillPaint = null;
		linePaint = null;
		path = new Path();
		currentRegion = new Region();
		this.center = new TiPoint(0,0);
		this.radius = getDefaultRadius();
		this.anchor = AnchorPosition.CENTER;
	}
	
	private Paint getOrCreateFillPaint() {
		if (fillPaint == null) {
			fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			fillPaint.setStyle(Style.FILL);
		}
		return fillPaint;
	}
	
	private Paint getOrCreateLinePaint() {
		if (linePaint == null) {
			linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			linePaint.setStyle(Style.STROKE);
		}
		return linePaint;
	}

	public Path getPath() {
		return path;
	}
	
	public ShapeProxy(TiContext context) {
		this();
		this.anchor = AnchorPosition.CENTER;
	}
	
	protected void onLayoutChanged(Context context, Rect parentBounds) {
		this.context = context;
		update(context, parentBounds);
		Log.d(TAG, "onLayoutChanged " + parentBounds.toString(), Log.DEBUG_MODE);
		// child is gonna be drawn relatively to its parent so we need to remove the translation for computation
//		Rect childParentBounds = new Rect(0,0, currentBounds.width(), currentBounds.height());
//		for (int i = 0; i < mShapes.size(); i++) {
//			ShapeProxy shapeProxy = mShapes.get(i);
//			shapeProxy.onLayoutChanged(this.context, currentBounds);
//		}
	}
	
	@SuppressWarnings("rawtypes")
	private Pathable pathableForOperation(KrollDict properties, int width, int height) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Class className = opFromString(properties.getString(TiC.PROPERTY_TYPE));
		if (className == null) return null;
		Pathable opPathable = (Pathable) className.getConstructors()[0].newInstance(this);
		TiPoint center = getDefaultCenter();
		Object radius = getDefaultRadius();
		AnchorPosition anchor = AnchorPosition.CENTER;
		if (properties != null) {
			if (properties.containsKey(TiC.PROPERTY_CENTER)) {
				center = TiConvert.toPoint(properties.get(TiC.PROPERTY_CENTER));
			}
			if (properties.containsKey(ShapeModule.PROPERTY_RADIUS)) {
				radius = properties.get(ShapeModule.PROPERTY_RADIUS);
			}
			if (properties.containsKey(ShapeModule.PROPERTY_ANCHOR)) {
				anchor = AnchorPosition.values()[properties.getInt(ShapeModule.PROPERTY_ANCHOR)];
			}
		}
		opPathable.radius = computeRadius(radius, width, height);
		opPathable.center = computePoint(center, anchor, width, height, opPathable.radius);
		if (opPathable instanceof Arc) {
			if (properties.containsKey(ShapeModule.PROPERTY_SWEEPANGLE)) {
				((Arc) opPathable).setSweepAngle(properties.getFloat(ShapeModule.PROPERTY_SWEEPANGLE));
			}
			if (properties.containsKey(ShapeModule.PROPERTY_STARTANGLE)) {
				((Arc) opPathable).setStartAngle(properties.getFloat(ShapeModule.PROPERTY_STARTANGLE));
			}
		}
		
		return opPathable;
	}
	
	private void appyOperations(Object arg, int width, int height) {
		if (!(arg instanceof Object[]))return;
		Object[] ops = (Object[])arg;
		path.reset();
		if (pathables == null) {
			pathables = new ArrayList<Pathable>();
		}
		else {
			pathables.clear();
		}
		for (int i = 0; i < ops.length; i++) {
			Pathable opPathable = null;
			try {
				opPathable = pathableForOperation(new KrollDict((HashMap)ops[i]), width, height);
			} catch (Exception e) {
			}
			if (opPathable != null) {
				pathables.add(opPathable);
			}
		}
	}
	
	protected void updatePath() {
		int width = parentBounds.width();
		int height = parentBounds.height();
		if (pathable != null) {
			pathable.radius = computeRadius(this.radius, width, height);
			pathable.center = computePoint(this.center, anchor, width, height, pathable.radius);
//			path.reset();
//			pathable.updatePathForRect(context, path, parentBounds.width(), parentBounds.height());
		}
		else if (hasProperty(ShapeModule.PROPERTY_OPERATIONS)) {
			appyOperations(getProperty(ShapeModule.PROPERTY_OPERATIONS), width, height);
		}
	}

	protected void update(Context context, Rect parentBounds) {
		this.parentBounds = parentBounds;
		updatePath();
		updateGradients(context, parentBounds);
	}
	
	
	protected Region clipRectWithPath(Rect parentBounds, Path path) {
		//let s create a 'infinite'  clip rect
		Region parentRegion = new Region(-10000, -10000, 10000, 10000);
		Region myRegion = new Region();
		if (fillPaint != null) {
			Path fillPath = new Path();
			fillPaint.getFillPath(path, fillPath);
			myRegion.setPath(fillPath, parentRegion);
		}
		
		if (linePaint != null) {
			Path linePath = new Path();
			linePaint.getFillPath(path, linePath);
			myRegion.setPath(linePath, parentRegion);
		}	
//		myRegion.translate(- parentBounds.left, - parentBounds.top);
//		Rect text = region.getBounds();
		return myRegion;
	}
	
	protected void updateGradients(Context context, Rect bounds) {
		KrollDict options = getProperties();
		if (lineGradient != null) {
			Shader shader = lineGradient.getShaderFactory().resize(bounds.width(), bounds.height());
			linePaint.setShader(shader);
			if (options.containsKey(ShapeModule.PROPERTY_LINE_OPACITY)) {
				Utils.styleOpacity(options, ShapeModule.PROPERTY_LINE_OPACITY, linePaint);
			}
		}
		if (fillGradient != null) {
			Shader shader = lineGradient.getShaderFactory().resize(bounds.width(), bounds.height());
			fillPaint.setShader(shader);
			if (options.containsKey(ShapeModule.PROPERTY_FILL_OPACITY)) {
				Utils.styleOpacity(options, ShapeModule.PROPERTY_FILL_OPACITY, fillPaint);
			}
		}
	}
	protected void updateGradients() {
		updateGradients(context, currentBounds);
	}

	
	public void setShapeViewProxy(ShapeViewProxy shapeViewProxy) {
		this.shapeViewProxy = shapeViewProxy;
		for (int i = 0; i < mShapes.size(); i++) {
			ShapeProxy shapeProxy = mShapes.get(i);
			shapeProxy.setShapeViewProxy(shapeViewProxy);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void drawPathWithPaint(Path path_, Paint paint_, Canvas canvas_, String shadowProperty_) {
		if (paint_ != null) {
			Shader shader = paint_.getShader();
			MaskFilter filter = paint_.getMaskFilter();
			Boolean hasShadow = hasProperty(shadowProperty_);
			
			if (hasShadow.booleanValue() && (filter!= null || shader != null)) {
				Utils.styleShadow(new KrollDict((HashMap<String, Object>)getProperty(shadowProperty_)), paint_);
				paint_.setShader(null);
				paint_.setMaskFilter(null);
				canvas_.drawPath(path_, paint_);
				
				paint_.clearShadowLayer();
				paint_.setShader(shader);
				paint_.setMaskFilter(filter);
				canvas_.drawPath(path_, paint_);
			}
			else {
				canvas_.drawPath(path_, paint_);
			}
		}
	}
	
	public void drawOnCanvas(Canvas canvas) {
		
		path.reset();
		if (pathable != null) {
			pathable.updatePathForRect(context, path, parentBounds.width(), parentBounds.height());
		} else if (pathables != null) {
			for (int i = 0; i < pathables.size(); i++) {
				pathables.get(i).updatePathForRect(context, path, parentBounds.width(), parentBounds.height());
			}
		}
		
		{
			this.currentRegion = clipRectWithPath(parentBounds, path);
			Rect rect = currentRegion.getBounds();
			if (!rect.equals(currentBounds)) {
				this.currentBounds = rect;
				for (int i = 0; i < mShapes.size(); i++) {
					ShapeProxy shapeProxy = mShapes.get(i);
					shapeProxy.onLayoutChanged(this.context, currentBounds);
				}
				if (transform != null) {
					matrix = new Matrix();
					matrix = transform.getMatrix(context,
							currentBounds.width(), currentBounds.height(),
							parentBounds.width(), parentBounds.height());
					matrix.postTranslate(currentBounds.left, currentBounds.top);
					matrix.preTranslate(-currentBounds.left, -currentBounds.top);
				}
			}
		}
		if (matrix != null) {
			path.transform(matrix);
		}
			
		drawPathWithPaint(path, fillPaint, canvas, ShapeModule.PROPERTY_FILL_SHADOW);
		drawPathWithPaint(path, linePaint, canvas, ShapeModule.PROPERTY_LINE_SHADOW);
		
		canvas.save();
//		canvas.clipRect(currentBounds);
		canvas.translate(currentBounds.left, currentBounds.top);
		for (int i = 0; i < mShapes.size(); i++) {
			ShapeProxy shapeProxy = mShapes.get(i);
			shapeProxy.drawOnCanvas(canvas);
		}
		canvas.restore();		
	}

	public Paint getFillPaint() {
		return fillPaint;
	}

	public Paint getLinePaint() {
		return linePaint;
	}
	
	protected void createAnimForColor(String prop, KrollDict animOptions, KrollDict properties, List<Animator> list, int defaultValue) {
		if(animOptions.containsKey(prop)) {
			int inValue = properties.optColor(prop, defaultValue);
			int outValue = animOptions.optColor(prop, inValue);
			ObjectAnimator anim = ObjectAnimator.ofInt(this, prop, inValue, outValue);
			 anim.setEvaluator(new ArgbEvaluator());
			list.add(anim);
		}
	}
	
	protected void createAnimForInt(String prop, KrollDict animOptions, KrollDict properties, List<Animator> list, int defaultValue) {
		if(animOptions.containsKey(prop)) {
			int inValue = properties.optInt(prop, defaultValue);
			int outValue = animOptions.optInt(prop, inValue);
			ObjectAnimator anim = ObjectAnimator.ofInt(this, prop, inValue, outValue);
			anim.setInterpolator(new LinearInterpolator());
			list.add(anim);
		}
	}
	
	protected void createAnimForRawInt(String prop, KrollDict animOptions, KrollDict properties, List<Animator> list, String defaultValue) {
		if(animOptions.containsKey(prop)) {
			int inValue = (int) Utils.getRawSize(properties, prop, defaultValue, context);
			int outValue = (int) Utils.getRawSize(animOptions, prop, context);
			ObjectAnimator anim = ObjectAnimator.ofInt(this, prop, inValue, outValue);
			anim.setInterpolator(new LinearInterpolator());
			list.add(anim);
		}
	}
	
	protected void createAnimForFloat(String prop, KrollDict animOptions, KrollDict properties, List<Animator> list, float defaultValue) {
		if(animOptions.containsKey(prop)) {
			float inValue = properties.optFloat(prop, defaultValue);
			float outValue = animOptions.optFloat(prop, inValue);
			ObjectAnimator anim = ObjectAnimator.ofFloat(this, prop, inValue, outValue);
			anim.setInterpolator(new LinearInterpolator());
			list.add(anim);
		}
	}
	
	protected void createAnimForRawFloat(String prop, KrollDict animOptions, KrollDict properties, List<Animator> list, String defaultValue) {
		if(animOptions.containsKey(prop)) {
			float inValue =  Utils.getRawSize(properties, prop, defaultValue, context);
			float outValue =  Utils.getRawSize(animOptions, prop, context);
			ObjectAnimator anim = ObjectAnimator.ofFloat(this, prop, inValue, outValue);
			anim.setInterpolator(new LinearInterpolator());
			list.add(anim);
		}
	}
	
	@SuppressWarnings("unchecked")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void prepareAnimatorSet(TiAnimatorSet tiSet, List<Animator> list, HashMap options) {
		super.prepareAnimatorSet(tiSet, list, options);
		
		KrollDict animOptions = new KrollDict(options);
		KrollDict properties = getProperties();
		Boolean animatingCenter = options.containsKey(TiC.PROPERTY_CENTER);
		Boolean animatingRadius = options.containsKey(ShapeModule.PROPERTY_RADIUS);
		if (animatingCenter || animatingRadius) {
			int width = parentBounds.width();
			int height = parentBounds.height();
			Point currentRadius = computeRadius(this.radius, width, height);
			Point animRadius = computeRadius(animatingRadius?options.get(ShapeModule.PROPERTY_RADIUS):this.radius, width, height);
			Point currentCenter = computePoint(this.center, anchor, width, height, currentRadius);
			Point animCenter = computePoint(animatingCenter?new TiPoint((HashMap)options.get(TiC.PROPERTY_CENTER)):this.center, anchor, width, height, animRadius);
			if (animatingRadius) {
				ObjectAnimator anim = ObjectAnimator.ofObject(this, "pathableRadius", new PointEvaluator(), currentRadius, animRadius);
				list.add(anim);
			}
			if (animatingCenter) {
				ObjectAnimator anim = ObjectAnimator.ofObject(this, "pathableCenter", new PointEvaluator(), currentCenter, animCenter);
				list.add(anim);
			}
			
		}
		createAnimForRawFloat(ShapeModule.PROPERTY_LINE_WIDTH, animOptions, properties, list, "1.0");
		createAnimForFloat(ShapeModule.PROPERTY_LINE_OPACITY, animOptions, properties, list, 1.0f);
		createAnimForColor(ShapeModule.PROPERTY_LINE_COLOR, animOptions, properties, list, Color.TRANSPARENT);
		createAnimForFloat(ShapeModule.PROPERTY_FILL_OPACITY, animOptions, properties, list, 1.0f);
		createAnimForColor(ShapeModule.PROPERTY_FILL_COLOR, animOptions, properties, list, Color.TRANSPARENT);
	}

	// Start Utility Methods

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}
	
	@SuppressWarnings("unchecked")
	protected boolean handleTouchEvent(String eventName, Object data, boolean bubbles, int x, int y) {
		if (context != null) {
			if (currentBounds.contains(x, y)) {
				boolean handledByChildren = false;
				if (mShapes.size() > 0) {
					int childrenX = x - currentBounds.left;
					int childrenY = y - currentBounds.top;
					for (int i = 0; i < mShapes.size(); i++) {
						ShapeProxy shapeProxy = mShapes.get(i);
						handledByChildren |= shapeProxy.handleTouchEvent(eventName, data, bubbles, childrenX, childrenX);
					}
				}
				if ((!handledByChildren || bubbles) && hasListeners(eventName)) {
					if (data instanceof HashMap) {
						((HashMap)data).put(TiC.EVENT_PROPERTY_SOURCE, this);
					}
					fireEvent(eventName, data);
					return true;
				}
			}
		}
		return false;
	}

	private void addShape(ShapeProxy proxy) {
		if (!mShapes.contains(proxy)) {
			mShapes.add(proxy);
			if (shapeViewProxy != null) {
				proxy.setShapeViewProxy(shapeViewProxy);
				if (currentBounds != null) {
					proxy.update(this.context, currentBounds);
					shapeViewProxy.redraw();
				}
			}
		}
	}

	private void removeShape(ShapeProxy proxy) {
		if (!mShapes.contains(proxy))
			return;
		mShapes.remove(proxy);
		proxy.setShapeViewProxy(null);
		if (shapeViewProxy != null) {
			shapeViewProxy.redraw();
		}
	}
	
	protected void redraw(){
		if (shapeViewProxy != null) {
			shapeViewProxy.redraw();
		}
	}
	
	@Kroll.method
	public void update() {
		if (context != null) {
			update(context, parentBounds);
			for (int i = 0; i < mShapes.size(); i++) {
				ShapeProxy shapeProxy = mShapes.get(i);
				shapeProxy.update(this.context, currentBounds);
			}
			if (shapeViewProxy != null) {
				shapeViewProxy.redraw();
			}
		}
	}
	
	@Kroll.method
	public void add(Object arg) {
		Log.d(TAG, "add", Log.DEBUG_MODE);
		if(arg instanceof ShapeProxy) {
			addShape((ShapeProxy)arg);
		}
		else if(arg instanceof HashMap) {
			ShapeProxy proxy = (ShapeProxy) KrollProxy.createProxy(ShapeProxy.class, null, new Object[]{arg}, null);
			addShape(proxy);
		}
		else {
			Log.e(TAG, "add: must be a Shape");
		}
	}

	@Kroll.method
	public void remove(Object shape) {
		Log.d(TAG, "remove", Log.DEBUG_MODE);
		if (!(shape instanceof ShapeProxy)) {
			Log.e(TAG, "remove: must be a shape");
			return;
		}
		removeShape((ShapeProxy)shape);
	}
	
	@Kroll.method
	@Kroll.getProperty
	public KrollDict getRect() {
		if (currentBounds != null) {
			KrollDict d = new KrollDict();
			d.put(TiC.PROPERTY_WIDTH, currentBounds.width());
			d.put(TiC.PROPERTY_HEIGHT, currentBounds.height());
			d.put(TiC.PROPERTY_X, currentBounds.left);
			d.put(TiC.PROPERTY_Y, currentBounds.top);
			return d;
		}
		return null;
	}
	

	@Kroll.method
	@Kroll.setProperty
	public void setCenter(Object value) {
		setProperty(TiC.PROPERTY_CENTER, value);
		this.center = new TiPoint((HashMap)value);
		update();
	}
	@Kroll.method
	@Kroll.getProperty
	public Object getCenter() {
		return getProperty(TiC.PROPERTY_CENTER);
	}

	@Kroll.method
	@Kroll.setProperty
	public void setRadius(Object value) {
		setProperty(ShapeModule.PROPERTY_RADIUS, value);
		this.radius = value;
		update();
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
		this.anchor = AnchorPosition.values()[TiConvert.toInt(value)];
		update();
	}
	@Kroll.method
	@Kroll.getProperty
	public Object getAnchor() {
		return getProperty(ShapeModule.PROPERTY_ANCHOR);
	}
	
	
	//ANIMATION getter/setter
	public void setPathableRadius(Point point) {
		pathable.radius = point;
		shapeViewProxy.redraw();
	}
	public Point getPathableRadius() {
		return pathable.radius;		
	}
	public void setPathableCenter(Point point) {
		pathable.center = point;
		shapeViewProxy.redraw();
	}
	public Point getPathableCenter() {
		return pathable.center;		
	}
	
	public void setLineWidth(float value) {
		getOrCreateLinePaint().setStrokeWidth(value);
		redraw();
	}
	public float getLineWidth() {
		float result = getOrCreateLinePaint().getStrokeWidth();
		return result;
	}
	public void setLineOpacity(float value) {
		getOrCreateLinePaint().setAlpha((int) (value * 255));
		redraw();
	}
	public float getLineOpacity() {
		return getOrCreateLinePaint().getAlpha()/255;
	}
	public void setLineColor(int value) {
		getOrCreateLinePaint().setColor(value);
		redraw();
	}
	public int getLineColor() {
		return getOrCreateLinePaint().getColor();
	}
	public void setFillOpacity(float value) {
		getOrCreateFillPaint().setAlpha((int) (value * 255));
		redraw();
	}
	public float getFillOpacity() {
		return getOrCreateFillPaint().getAlpha()/255;
	}
	public void setFillColor(int value) {
		getOrCreateFillPaint().setColor(value);
		redraw();
	}
	public int getFillColor() {
		return getOrCreateFillPaint().getColor();
	}
	
	private void setPaintImageDrawable(Object object, Paint _paint) {
		if (object instanceof TiBlob) {
			_paint.setShader(new BitmapShader(((TiBlob)object).getImage(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
		}
		else {
			Drawable drawable = TiUIHelper.buildImageDrawable(TiConvert.toString(object), false, this);
			if (drawable instanceof BitmapDrawable) {
				_paint.setShader(new BitmapShader(((BitmapDrawable) drawable).getBitmap(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
			}
		}
	}
	

	@Override
	public void handleCreationDict(KrollDict properties) {
		super.handleCreationDict(properties);
		setModelListener(this);
	}
	
	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {
		if (key.equals(ShapeModule.PROPERTY_LINE_EMBOSS)) {
			Utils.styleEmboss(properties, ShapeModule.PROPERTY_LINE_EMBOSS, getOrCreateLinePaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_LINE_WIDTH)) {
			Utils.styleStrokeWidth(properties, ShapeModule.PROPERTY_LINE_WIDTH, "1", getOrCreateLinePaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_LINE_OPACITY)) {
			Utils.styleOpacity(properties, ShapeModule.PROPERTY_LINE_OPACITY, getOrCreateLinePaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_LINE_JOIN)) {
			Utils.styleJoin(properties, ShapeModule.PROPERTY_LINE_JOIN, getOrCreateLinePaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_LINE_COLOR)) {
			Utils.styleColor(properties, ShapeModule.PROPERTY_LINE_COLOR, getOrCreateLinePaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_LINE_CAP)) {
			Utils.styleCap(properties, ShapeModule.PROPERTY_LINE_CAP, getOrCreateLinePaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_LINE_SHADOW)) {
			Utils.styleShadow(properties, ShapeModule.PROPERTY_LINE_SHADOW, getOrCreateLinePaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_LINE_DASH)) {
			Utils.styleDash(properties, ShapeModule.PROPERTY_LINE_DASH, getOrCreateLinePaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_FILL_OPACITY)) {
			Utils.styleOpacity(properties, ShapeModule.PROPERTY_FILL_OPACITY, getOrCreateFillPaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_LINE_IMAGE)) {
			setPaintImageDrawable(newValue, getOrCreateLinePaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_FILL_IMAGE)) {
			setPaintImageDrawable(newValue, getOrCreateFillPaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_LINE_GRADIENT)) {
			 lineGradient = TiUIHelper.buildGradientDrawable(null, properties.getKrollDict(ShapeModule.PROPERTY_LINE_GRADIENT));
		}
		else if (key.equals(ShapeModule.PROPERTY_FILL_GRADIENT)) {
			 fillGradient = TiUIHelper.buildGradientDrawable(null, properties.getKrollDict(ShapeModule.PROPERTY_FILL_GRADIENT));
		}
		else if (key.equals(ShapeModule.PROPERTY_FILL_COLOR)) {
			Utils.styleColor(properties, ShapeModule.PROPERTY_FILL_COLOR, getOrCreateFillPaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_FILL_SHADOW)) {
			Utils.styleShadow(properties, ShapeModule.PROPERTY_FILL_SHADOW, getOrCreateFillPaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_FILL_EMBOSS)) {
			Utils.styleEmboss(properties, ShapeModule.PROPERTY_FILL_EMBOSS, getOrCreateFillPaint());
		}
		else if (key.equals(TiC.PROPERTY_CENTER)) {
			this.center = TiConvert.toPoint(newValue);
		}
		else if (key.equals(ShapeModule.PROPERTY_RADIUS)) {
			this.radius = newValue;
		}
		else if (key.equals(ShapeModule.PROPERTY_ANCHOR)) {
			this.anchor = AnchorPosition.values()[TiConvert.toInt(newValue)];
		}
		else return;
		
		if (shapeViewProxy != null) {
			shapeViewProxy.redraw();
		}
	}

	@Override
	public void processProperties(KrollDict properties) {
		if (properties == null) return;
		if (properties.containsKey(ShapeModule.PROPERTY_LINE_EMBOSS)) {
			Utils.styleEmboss(properties, ShapeModule.PROPERTY_LINE_EMBOSS, getOrCreateLinePaint());
		}
		if (properties.containsKey(ShapeModule.PROPERTY_LINE_WIDTH)) {
			Utils.styleStrokeWidth(properties, ShapeModule.PROPERTY_LINE_WIDTH, "1", getOrCreateLinePaint());
		}
		if (properties.containsKey(ShapeModule.PROPERTY_LINE_OPACITY)) {
			Utils.styleOpacity(properties, ShapeModule.PROPERTY_LINE_OPACITY, getOrCreateLinePaint());
		}
		
		if (properties.containsKey(ShapeModule.PROPERTY_LINE_JOIN)) {
			Utils.styleJoin(properties, ShapeModule.PROPERTY_LINE_JOIN, getOrCreateLinePaint());
		}
		if (properties.containsKey(ShapeModule.PROPERTY_LINE_COLOR)) {
			Utils.styleColor(properties, ShapeModule.PROPERTY_LINE_COLOR, getOrCreateLinePaint());
		}
		if (properties.containsKey(ShapeModule.PROPERTY_LINE_CAP)) {
			Utils.styleCap(properties, ShapeModule.PROPERTY_LINE_CAP, getOrCreateLinePaint());
		}
		if (properties.containsKey(ShapeModule.PROPERTY_LINE_SHADOW)) {
			Utils.styleShadow(properties, ShapeModule.PROPERTY_LINE_SHADOW, getOrCreateLinePaint());
		}
		if (properties.containsKey(ShapeModule.PROPERTY_LINE_DASH)) {
			Utils.styleDash(properties, ShapeModule.PROPERTY_LINE_DASH, getOrCreateLinePaint());
		}
		if (properties.containsKey(ShapeModule.PROPERTY_FILL_OPACITY)) {
			Utils.styleOpacity(properties, ShapeModule.PROPERTY_FILL_OPACITY, getOrCreateFillPaint());
		}
		if (properties.containsKey(ShapeModule.PROPERTY_LINE_IMAGE)) {
			setPaintImageDrawable(properties.get(ShapeModule.PROPERTY_LINE_IMAGE), getOrCreateLinePaint());
		}
		if (properties.containsKey(ShapeModule.PROPERTY_FILL_IMAGE)) {
			setPaintImageDrawable(properties.get(ShapeModule.PROPERTY_FILL_IMAGE), getOrCreateFillPaint());
		}
		if (properties.containsKey(ShapeModule.PROPERTY_LINE_GRADIENT)) {
			 lineGradient = TiUIHelper.buildGradientDrawable(null, properties.getKrollDict(ShapeModule.PROPERTY_LINE_GRADIENT));
		}
		if (properties.containsKey(ShapeModule.PROPERTY_FILL_GRADIENT)) {
			 fillGradient = TiUIHelper.buildGradientDrawable(null, properties.getKrollDict(ShapeModule.PROPERTY_FILL_GRADIENT));
		}
		if (properties.containsKey(ShapeModule.PROPERTY_FILL_COLOR)) {
			Utils.styleColor(properties, ShapeModule.PROPERTY_FILL_COLOR, getOrCreateFillPaint());
		}
		if (properties.containsKey(ShapeModule.PROPERTY_FILL_SHADOW)) {
			Utils.styleShadow(properties, ShapeModule.PROPERTY_FILL_SHADOW, getOrCreateFillPaint());
		}
		if (properties.containsKey(ShapeModule.PROPERTY_FILL_EMBOSS)) {
			Utils.styleEmboss(properties, ShapeModule.PROPERTY_FILL_EMBOSS, getOrCreateFillPaint());
		}
		if (properties.containsKey(TiC.PROPERTY_CENTER)) {
			
			this.center = TiConvert.toPoint(properties.get(TiC.PROPERTY_CENTER));
		}
		if (properties.containsKey(ShapeModule.PROPERTY_RADIUS)) {
			this.radius = properties.get(ShapeModule.PROPERTY_RADIUS);
		}
		if (properties.containsKey(ShapeModule.PROPERTY_ANCHOR)) {
			this.anchor = AnchorPosition.values()[properties.getInt(ShapeModule.PROPERTY_ANCHOR)];
		}
		if (properties.containsKey(TiC.PROPERTY_TRANSFORM)) {
			this.transform = (Ti2DMatrix)properties.get(TiC.PROPERTY_TRANSFORM);
			this.matrix = null;
		}
	}

	@Override
	public void propertiesChanged(List<KrollPropertyChange> changes,KrollProxy proxy) {}

	@Override
	public void listenerAdded(String type, int count, KrollProxy proxy) {}

	@Override
	public void listenerRemoved(String type, int count, KrollProxy proxy) {}
}