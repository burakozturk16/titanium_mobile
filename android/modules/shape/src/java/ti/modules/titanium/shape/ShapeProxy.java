
package ti.modules.titanium.shape;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollPropertyChange;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollProxyListener;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiPoint;
import org.appcelerator.titanium.animation.TiAnimator;
import org.appcelerator.titanium.animation.TiAnimatorSet;
import org.appcelerator.titanium.proxy.AnimatableProxy;
import org.appcelerator.titanium.util.AffineTransform;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.Ti2DMatrix;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ArgbEvaluator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.animation.TypeEvaluator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.MaskFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.FillType;
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
import android.view.View;
import android.view.ViewParent;
import android.view.animation.LinearInterpolator;

@SuppressWarnings({ "unchecked", "rawtypes" })
@Kroll.proxy(creatableInModule = ShapeModule.class, propertyAccessors={
	TiC.PROPERTY_NAME
})
public class ShapeProxy extends AnimatableProxy implements KrollProxyListener {
	// Standard Debugging variables
	private static final String TAG = "PathProxy";
	
	private static final Point NODECALEPOINT = new Point(0,0);
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
	protected float anchorPointX = 0.5f;
	protected float anchorPointY = 0.5f;
	
	private Ti2DMatrix transform;
	private Matrix matrix;
	
	private float lineOpacity = 1.0f;
	private float fillOpacity = 1.0f;
	
	private boolean fillInversed = false;
	private boolean lineInversed = false;
	private boolean lineClipped = false;
	private boolean needsMatrix = false;
	
	protected Object paintLock;
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public class PointEvaluator implements TypeEvaluator<Point> {
		
		public PointEvaluator() {
		}
		
		public Point evaluate(float fraction, Point startValue,
				Point endValue) {
			return new Point((int)(fraction*(endValue.x  - startValue.x) + startValue.x), (int)(fraction*(endValue.y  - startValue.y) + startValue.y));
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
	public Point computePoint(TiPoint point_, AnchorPosition anchor_ , int width, int height)
	{
		return computePoint(point_, anchor_, width, height, NODECALEPOINT);
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
	
	public Point fractionPoints(float fraction, Point point1, Point point2) {
		return new Point((int)(fraction*(point2.x  - point1.x) + point1.x), (int)(fraction*(point2.y  - point1.y) + point1.y));
	}
	public Point fractionPoint(float fraction, Point point) {
		return new Point((int)(fraction*point.x), (int)(fraction*point.y));
	}
	
	public class BezierPoint {
		public Point point = null;
		public Point curvePoint1 = null;
		public Point curvePoint2 = null;
		public BezierPoint() {
		}
		public BezierPoint fraction(float fraction) {
			BezierPoint result = new BezierPoint();
			result.point = fractionPoint(fraction, point);
			if (curvePoint1 != null) result.curvePoint1 = fractionPoint(fraction, curvePoint1);
			if (curvePoint2 != null) result.curvePoint2 = fractionPoint(fraction, curvePoint2);
			return result;
		}
		
		public BezierPoint fraction(float fraction, BezierPoint otherPoint) {
			BezierPoint result = new BezierPoint();
			result.point = fractionPoints(fraction, point, otherPoint.point);
			if (curvePoint1 != null) result.curvePoint1 = fractionPoints(fraction, curvePoint1, otherPoint.curvePoint1);
			if (curvePoint2 != null) result.curvePoint2 = fractionPoints(fraction, curvePoint2, otherPoint.curvePoint2);
			return result;
		}
	}
	
	public class Line extends Pathable {
		private ArrayList<BezierPoint> points;
		public Line() {
			super();
			this.points = new ArrayList<BezierPoint>();
		}
		
		public void setPoints(ArrayList<BezierPoint> points) {
			this.points = points;
		}
		
		public ArrayList<BezierPoint> getPoints() {
			return this.points;
		}
		public void updatePathForRect(Context context, Path path, int width, int height) {
			
			for (int i = 0; i < points.size(); i++) {
				BezierPoint bezierPoint = points.get(i);
				if (i == 0) {
					path.moveTo(bezierPoint.point.x, bezierPoint.point.y);
				}
				else {
					if (bezierPoint.curvePoint1 != null && bezierPoint.curvePoint2 != null) {
						path.cubicTo(bezierPoint.curvePoint1.x, bezierPoint.curvePoint1.y, bezierPoint.curvePoint2.x, bezierPoint.curvePoint2.y, bezierPoint.point.x, bezierPoint.point.y);
					}
					else if (bezierPoint.curvePoint1 != null) {
						path.quadTo(bezierPoint.curvePoint1.x, bezierPoint.curvePoint1.y, bezierPoint.point.x, bezierPoint.point.y);
					}
					else {
						path.lineTo(bezierPoint.point.x, bezierPoint.point.y);
					}
				}
			}
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
		public Oval() {
			super();
		}
		public void updatePathForRect(Context context, Path path, int width, int height) {
			RectF rect = new RectF(center.x - radius.x, center.y - radius.x, center.x + radius.y, center.y + radius.y);
			path.addOval(rect, direction);
		}
	}
	protected class PRect extends Pathable {
		public PRect() {
			super();
		}
		public void updatePathForRect(Context context, Path path, int width, int height) {
			RectF rect = new RectF(center.x - radius.x, center.y - radius.x, center.x + radius.y, center.y + radius.y);
			path.addRect(rect, direction);
		}
	}
	protected class PRoundRect extends Pathable {
		protected Object cornerRadius;
		public PRoundRect() {
			cornerRadius = new TiPoint(0,0);
		}
		public void setCornerRadius(Object value) {
			this.cornerRadius = value;
		}
		public void updatePathForRect(Context context, Path path, int width, int height) {
			RectF rect = new RectF(center.x - radius.x, center.y - radius.x, center.x + radius.y, center.y + radius.y);
			Point corners = computeRadius(cornerRadius, width, height);
			path.addRoundRect(rect, corners.x, corners.y, direction);
		}
	}
	
	protected class PieSlice extends Arc {
		public Point innerRadius;
		public PieSlice() {
			innerRadius = new Point(0,0);
		}
		public void updatePathForRect(Context context, Path path, int width, int height) {
			float realStart = startAngle-90.0f;
			
			RectF rectin = new RectF(center.x - innerRadius.x, center.y - innerRadius.x, center.x + innerRadius.y, center.y + innerRadius.y);
			RectF rect = new RectF(center.x - radius.x, center.y - radius.x, center.x + radius.y, center.y + radius.y);
			path.arcTo(rectin,  realStart,  sweepAngle);
			path.arcTo(rect,  realStart + sweepAngle,  -sweepAngle);
			path.close();
		}
	}
	
	public static Class PathableClassFromString(String value)
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
		paintLock = new Object();
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
	
	private Pathable pathableForOperation(KrollDict properties, int width, int height) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Class className = PathableClassFromString(properties.getString(TiC.PROPERTY_TYPE));
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
	
	private void clearPaths(){
		path.reset();
		if (pathables == null) {
			pathables = new ArrayList<Pathable>();
		}
		else {
			pathables.clear();
		}
	}
	private void appyOperations(Object arg, int width, int height) {
		if (!(arg instanceof Object[]))return;
		Object[] ops = (Object[])arg;
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
			clearPaths();
			appyOperations(getProperty(ShapeModule.PROPERTY_OPERATIONS), width, height);
		}
		else if (hasProperty(TiC.PROPERTY_TYPE)) {
			clearPaths();
			try {
				Pathable opPathable = pathableForOperation(properties, width, height);
				if (opPathable != null) pathables.add(opPathable);
			} catch (Exception e) {
			}
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
		if (lineGradient != null) {
			Shader shader = lineGradient.getShaderFactory().resize(bounds.width(), bounds.height());
			Utils.setShaderForPaint(shader, linePaint);
		}
		if (fillGradient != null) {
			Shader shader = fillGradient.getShaderFactory().resize(bounds.width(), bounds.height());
			Utils.setShaderForPaint(shader, fillPaint);

		}
	}
	protected void updateGradients() {
		updateGradients(context, parentBounds);
	}

	
	public void setShapeViewProxy(ShapeViewProxy shapeViewProxy) {
		this.shapeViewProxy = shapeViewProxy;
		for (int i = 0; i < mShapes.size(); i++) {
			ShapeProxy shapeProxy = mShapes.get(i);
			shapeProxy.setShapeViewProxy(shapeViewProxy);
		}
	}
	
	private void drawPathWithPaint(Path path_, Paint paint_, Canvas canvas_, String shadowProperty_, float opacity_) {
		if (paint_ != null) {
			Shader shader = paint_.getShader();
			MaskFilter filter = paint_.getMaskFilter();
			Boolean hasShadow = hasProperty(shadowProperty_);
			
			int colorAlpha = paint_.getAlpha();
			if (hasShadow  || shader != null) {
				if (hasShadow) Utils.styleShadow(new KrollDict((HashMap<String, Object>)getProperty(shadowProperty_)), paint_);
				canvas_.save();
				if (lineClipped) canvas_.clipPath(path_);
				paint_.setShader(null);
				paint_.setMaskFilter(null);
				paint_.setAlpha((int) (colorAlpha * opacity_));
				canvas_.drawPath(path_, paint_);
				canvas_.restore();
				
				if (hasShadow) paint_.clearShadowLayer();
				paint_.setAlpha((int) (opacity_*255.0f));
				paint_.setShader(shader);
				paint_.setMaskFilter(filter);
				canvas_.drawPath(path_, paint_);
			}
			else {
				canvas_.drawPath(path_, paint_);
			}
			paint_.setAlpha(colorAlpha);
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
			boolean sizeChanged = !rect.equals(currentBounds);
			if (sizeChanged) {
				this.currentBounds = rect;
				for (int i = 0; i < mShapes.size(); i++) {
					ShapeProxy shapeProxy = mShapes.get(i);
					shapeProxy.onLayoutChanged(this.context, currentBounds);
				}
				
			}
			if (sizeChanged || needsMatrix) {
				prepareMatrix();
			}
		}
		if (matrix != null) {
			path.transform(matrix);
		}
		synchronized (paintLock) {
			path.setFillType(fillInversed?FillType.INVERSE_EVEN_ODD:FillType.EVEN_ODD);
			drawPathWithPaint(path, fillPaint, canvas, ShapeModule.PROPERTY_FILL_SHADOW, fillOpacity);
			path.setFillType(lineInversed?FillType.INVERSE_EVEN_ODD:FillType.EVEN_ODD);
			drawPathWithPaint(path, linePaint, canvas, ShapeModule.PROPERTY_LINE_SHADOW, lineOpacity);
		}
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
	
	protected void createAnimForColor(String prop, KrollDict animOptions,
			KrollDict properties, List<PropertyValuesHolder> list,
			List<PropertyValuesHolder> listReverse, int defaultValue) {
		if (animOptions.containsKey(prop)) {
			int inValue = properties.optColor(prop, defaultValue);
			int outValue = animOptions.optColor(prop, inValue);
			PropertyValuesHolder anim = PropertyValuesHolder.ofInt(prop,
					outValue);
			anim.setEvaluator(new ArgbEvaluator());
			list.add(anim);
			if (listReverse != null) {
				anim = PropertyValuesHolder.ofInt(prop, inValue);
				anim.setEvaluator(new ArgbEvaluator());
				listReverse.add(anim);
			}
		}
	}
	
	protected void createAnimForInt(String prop, KrollDict animOptions,
			KrollDict properties, List<PropertyValuesHolder> list,
			List<PropertyValuesHolder> listReverse, int defaultValue) {
		if (animOptions.containsKey(prop)) {
			int inValue = properties.optInt(prop, defaultValue);
			int outValue = animOptions.optInt(prop, inValue);
			list.add(PropertyValuesHolder.ofInt(prop, outValue));
			if (listReverse != null) {
				listReverse.add(PropertyValuesHolder.ofInt(prop, inValue));
			}
		}
	}

	protected void createAnimForRawInt(String prop, KrollDict animOptions,
			KrollDict properties, List<PropertyValuesHolder> list,
			List<PropertyValuesHolder> listReverse, String defaultValue) {
		if (animOptions.containsKey(prop)) {
			int inValue = (int) Utils.getRawSize(properties, prop,
					defaultValue, context);
			int outValue = (int) Utils.getRawSize(animOptions, prop, context);
			list.add(PropertyValuesHolder.ofInt(prop, outValue));
			if (listReverse != null) {
				listReverse.add(PropertyValuesHolder.ofInt(prop, inValue));
			}
		}
	}

	protected void createAnimForFloat(String prop, KrollDict animOptions,
			KrollDict properties, List<PropertyValuesHolder> list,
			List<PropertyValuesHolder> listReverse, float defaultValue) {
		if (animOptions.containsKey(prop)) {
			float inValue = properties.optFloat(prop, defaultValue);
			float outValue = animOptions.optFloat(prop, inValue);
			list.add(PropertyValuesHolder.ofFloat(prop, outValue));
			if (listReverse != null) {
				listReverse.add(PropertyValuesHolder.ofFloat(prop, inValue));
			}
		}
	}

	protected void createAnimForRawFloat(String prop, KrollDict animOptions,
			KrollDict properties, List<PropertyValuesHolder> list,
			List<PropertyValuesHolder> listReverse, String defaultValue) {
		if (animOptions.containsKey(prop)) {
			float inValue = Utils.getRawSize(properties, prop, defaultValue,
					context);
			float outValue = Utils.getRawSize(animOptions, prop, context);
			list.add(PropertyValuesHolder.ofFloat(prop, outValue));
			if (listReverse != null) {
				listReverse.add(PropertyValuesHolder.ofFloat(prop, inValue));
			}
		}
	}
	
	private class Ti2DMatrixEvaluator implements TypeEvaluator<Ti2DMatrix> {
		private ShapeProxy proxy;
		
		public Ti2DMatrixEvaluator(ShapeProxy proxy) {
			this.proxy = proxy;
		}
		
		public Ti2DMatrix evaluate(float fraction, Ti2DMatrix startValue,
				Ti2DMatrix endValue) {
			
			int width = proxy.currentBounds.width();
			int height = proxy.currentBounds.height();
			int parentWidth = proxy.parentBounds.width();
			int parentHeight = proxy.parentBounds.height();
			AffineTransform a = (startValue != null)?startValue.getAffineTransform(context, width, height, parentWidth, parentHeight):(new AffineTransform());
			AffineTransform b = endValue.getAffineTransform(context, width, height, parentWidth, parentHeight);
			b.blend(a, fraction);
			return new Ti2DMatrix(b);
		}

	}
	
	protected void preparePropertiesSet(TiAnimatorSet tiSet,
			List<PropertyValuesHolder> propertiesList,
			List<PropertyValuesHolder> propertiesListReverse,
			KrollDict animOptions) {
		// KrollDict properties = getProperties();
		boolean needsReverse = propertiesListReverse != null;
		Boolean animatingCenter = animOptions.containsKey(TiC.PROPERTY_CENTER);
		Boolean animatingRadius = animOptions
				.containsKey(ShapeModule.PROPERTY_RADIUS);
		if (animatingCenter || animatingRadius) {
			int width = parentBounds.width();
			int height = parentBounds.height();
			Point currentRadius = computeRadius(this.radius, width, height);
			Point animRadius = computeRadius(
					animatingRadius ? animOptions.get(ShapeModule.PROPERTY_RADIUS)
							: this.radius, width, height);
			Point currentCenter = computePoint(this.center, anchor, width,
					height, currentRadius);
			Point animCenter = computePoint(animatingCenter ? new TiPoint(
					(HashMap) animOptions.get(TiC.PROPERTY_CENTER))
					: this.center, anchor, width, height, animRadius);
			PointEvaluator evaluator = new PointEvaluator();
			if (animatingRadius) {
				propertiesList.add(PropertyValuesHolder.ofObject(
						"pathableRadius", evaluator,
						animRadius));
				if (needsReverse) {
					propertiesListReverse.add(PropertyValuesHolder.ofObject(
							"pathableRadius", evaluator, currentRadius));
				}
			}
			if (animatingCenter) {
				propertiesList.add(PropertyValuesHolder.ofObject(
						"pathableCenter", evaluator, animCenter));
				if (needsReverse) {
					propertiesListReverse.add(PropertyValuesHolder.ofObject(
							"pathableCenter", evaluator, currentCenter));
				}
			}

		}

		createAnimForRawFloat(ShapeModule.PROPERTY_LINE_WIDTH, animOptions,
				properties, propertiesList, propertiesListReverse, "1.0");
		createAnimForFloat(ShapeModule.PROPERTY_LINE_OPACITY, animOptions,
				properties, propertiesList, propertiesListReverse, 1.0f);
		createAnimForColor(ShapeModule.PROPERTY_LINE_COLOR, animOptions,
				properties, propertiesList, propertiesListReverse, Color.TRANSPARENT);
		createAnimForFloat(ShapeModule.PROPERTY_FILL_OPACITY, animOptions,
				properties, propertiesList, propertiesListReverse, 1.0f);
		createAnimForColor(ShapeModule.PROPERTY_FILL_COLOR, animOptions,
				properties, propertiesList, propertiesListReverse, Color.TRANSPARENT);

		if (animOptions.containsKey(TiC.PROPERTY_TRANSFORM)) {
			Ti2DMatrix matrix = (Ti2DMatrix) animOptions
					.get(TiC.PROPERTY_TRANSFORM);
			if (matrix.getClass().getSuperclass().equals(Ti2DMatrix.class)) {
				matrix = new Ti2DMatrix(matrix); // case of _2DMatrixProxy
			}
			Ti2DMatrixEvaluator evaluator = new Ti2DMatrixEvaluator(this);
			propertiesList.add(PropertyValuesHolder.ofObject(
					"animated2DMatrix", evaluator, matrix));
			if (needsReverse) {
				matrix = (Ti2DMatrix) animOptions
						.get(TiC.PROPERTY_TRANSFORM);
				propertiesListReverse.add(PropertyValuesHolder.ofObject(
						"animated2DMatrix", evaluator, this.transform));
			}
		}
	}
	
	@Override
	protected void prepareAnimatorSet(TiAnimatorSet tiSet, List<Animator> list, List<Animator> reverseList, HashMap options) {
		super.prepareAnimatorSet(tiSet, list, reverseList, options);
		
		List<PropertyValuesHolder> propertiesList = new ArrayList<PropertyValuesHolder>();
		List<PropertyValuesHolder> propertiesListReverse = (reverseList!=null)?new ArrayList<PropertyValuesHolder>():null;
		KrollDict animOptions = new KrollDict(options);
		preparePropertiesSet(tiSet, propertiesList, propertiesListReverse, animOptions);
		
		ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(this,propertiesList.toArray(new PropertyValuesHolder[0]));
		anim.addUpdateListener(new AnimatorUpdateListener(){
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				redraw();
			}
		});
		anim.setInterpolator(new LinearInterpolator());
		list.add(anim);
		if (reverseList != null) {
			anim = ObjectAnimator.ofPropertyValuesHolder(this,propertiesListReverse.toArray(new PropertyValuesHolder[0]));
			anim.addUpdateListener(new AnimatorUpdateListener(){
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					redraw();
				}
			});
			anim.setInterpolator(new LinearInterpolator());
			reverseList.add(anim);
		}
	}

	// Start Utility Methods

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	} 
	
	protected boolean handleTouchEvent(String eventName, Object data, boolean bubbles, int x, int y) {
		if (context != null) {
			if (currentBounds.contains(x, y)) {
				boolean handledByChildren = false;
				if (mShapes.size() > 0) {
					int childrenX = x - currentBounds.left;
					int childrenY = y - currentBounds.top;
					for (int i = 0; i < mShapes.size(); i++) {
						ShapeProxy shapeProxy = mShapes.get(i);
						handledByChildren |= shapeProxy.handleTouchEvent(eventName, data, bubbles, childrenX, childrenY);
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
	}
	public Point getPathableRadius() {
		return pathable.radius;		
	}
	public void setPathableCenter(Point point) {
		pathable.center = point;
	}
	
	public Point getPathableCenter() {
		return pathable.center;		
	}
	
	public void setLineWidth(float value) {
		getOrCreateLinePaint().setStrokeWidth(value);
	}
	
	public float getLineWidth() {
		float result = getOrCreateLinePaint().getStrokeWidth();
		return result;
	}
	
	public void setLineOpacity(float value) {
		lineOpacity = value;
	}
	public float getLineOpacity() {
		return lineOpacity;
	}
	public void setLineColor(int value) {
		synchronized (paintLock) {
			getOrCreateLinePaint().setColor(value);
		}
	}
	public int getLineColor() {
		synchronized (paintLock) {
			return getOrCreateLinePaint().getColor();
		}
	}
	public void setFillOpacity(float value) {
		fillOpacity = value;
	}
	public float getFillOpacity() {
		return fillOpacity;
	}
	public void setFillColor(int value) {
		synchronized (paintLock) {
			getOrCreateFillPaint().setColor(value);
		}
	}
	public int getFillColor() {
		synchronized (paintLock) {
			return getOrCreateFillPaint().getColor();
		}
	}
	
	private void prepareMatrix()
	{
		if (transform != null) {
			if (transform.getAffineTransform() != null) {
				matrix = transform.getAffineTransform().toMatrix();
			}
			else {
				matrix = new Matrix();
				matrix = transform.getMatrix(context,
						currentBounds.width(), currentBounds.height(),
						parentBounds.width(), parentBounds.height());
			}
			
			float dx = currentBounds.left + currentBounds.width()*anchorPointX;
			float dy = currentBounds.top + currentBounds.height()*anchorPointY;
			
			matrix.preTranslate(-dx, -dy);
			matrix.postTranslate(dx, dy);
		}
	}
	

	private void applyAnchorPoint(Object anchorPoint)
	{
		if (anchorPoint instanceof HashMap) {
			HashMap point = (HashMap) anchorPoint;
			anchorPointX = TiConvert.toFloat(point, TiC.PROPERTY_X);
			anchorPointY = TiConvert.toFloat(point, TiC.PROPERTY_Y);
		}
		else {
			anchorPointX = anchorPointY = 0.5f;
		}
	}
	
	public void setAnimated2DMatrix(Ti2DMatrix matrix) {
		this.transform = matrix;
		prepareMatrix();
	}
	
	public Ti2DMatrix getAnimated2DMatrix() {
		return this.transform;
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
			lineOpacity = TiConvert.toFloat(newValue, 1.0f);
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
//			Utils.styleShadow(properties, ShapeModule.PROPERTY_LINE_SHADOW, getOrCreateLinePaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_LINE_DASH)) {
			Utils.styleDash(properties, ShapeModule.PROPERTY_LINE_DASH, getOrCreateLinePaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_FILL_OPACITY)) {
			fillOpacity = TiConvert.toFloat(newValue, 1.0f);
		}
		else if (key.equals(ShapeModule.PROPERTY_LINE_IMAGE)) {
			setPaintImageDrawable(newValue, getOrCreateLinePaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_FILL_IMAGE)) {
			setPaintImageDrawable(newValue, getOrCreateFillPaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_LINE_GRADIENT)) {
			 lineGradient = TiUIHelper.buildGradientDrawable(properties.getKrollDict(ShapeModule.PROPERTY_LINE_GRADIENT));
		}
		else if (key.equals(ShapeModule.PROPERTY_FILL_GRADIENT)) {
			 fillGradient = TiUIHelper.buildGradientDrawable(properties.getKrollDict(ShapeModule.PROPERTY_FILL_GRADIENT));
		}
		else if (key.equals(ShapeModule.PROPERTY_FILL_COLOR)) {
			Utils.styleColor(properties, ShapeModule.PROPERTY_FILL_COLOR, getOrCreateFillPaint());
		}
		else if (key.equals(ShapeModule.PROPERTY_FILL_SHADOW)) {
//			Utils.styleShadow(properties, ShapeModule.PROPERTY_FILL_SHADOW, getOrCreateFillPaint());
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
		else if (key.equals(TiC.PROPERTY_TRANSFORM)) {
			if (newValue.getClass().getSuperclass().equals(Ti2DMatrix.class)) {
				this.transform = new Ti2DMatrix((Ti2DMatrix)newValue); // case of _2DMatrixProxy
			}
			else {
				this.transform = (Ti2DMatrix)newValue;
			}
			this.matrix = null;
			needsMatrix = true;
		}
		else if (key.equals(ShapeModule.PROPERTY_FILL_INVERSED)) {
			this.fillInversed = TiConvert.toBoolean(newValue);
		}
		else if (key.equals(ShapeModule.PROPERTY_LINE_INVERSED)) {
			this.lineInversed = TiConvert.toBoolean(newValue);
		}
		else if (key.equals(ShapeModule.PROPERTY_LINE_CLIPPED)) {
			this.lineClipped = TiConvert.toBoolean(newValue);
		}
		else if (key.equals(TiC.PROPERTY_ANCHOR_POINT)) {
			applyAnchorPoint(newValue);
			needsMatrix = true;
		} else return;
		
		redraw();
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
			lineOpacity = properties.optFloat(ShapeModule.PROPERTY_LINE_OPACITY, 1.0f);
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
//		if (properties.containsKey(ShapeModule.PROPERTY_LINE_SHADOW)) {
//			Utils.styleShadow(properties, ShapeModule.PROPERTY_LINE_SHADOW, getOrCreateLinePaint());
//		}
		if (properties.containsKey(ShapeModule.PROPERTY_LINE_DASH)) {
			Utils.styleDash(properties, ShapeModule.PROPERTY_LINE_DASH, getOrCreateLinePaint());
		}
		if (properties.containsKey(ShapeModule.PROPERTY_FILL_OPACITY)) {
			fillOpacity = properties.optFloat(ShapeModule.PROPERTY_FILL_OPACITY, 1.0f);
		}
		if (properties.containsKey(ShapeModule.PROPERTY_LINE_IMAGE)) {
			setPaintImageDrawable(properties.get(ShapeModule.PROPERTY_LINE_IMAGE), getOrCreateLinePaint());
		}
		if (properties.containsKey(ShapeModule.PROPERTY_FILL_IMAGE)) {
			setPaintImageDrawable(properties.get(ShapeModule.PROPERTY_FILL_IMAGE), getOrCreateFillPaint());
		}
		if (properties.containsKey(ShapeModule.PROPERTY_LINE_GRADIENT)) {
			 lineGradient = TiUIHelper.buildGradientDrawable(properties.getKrollDict(ShapeModule.PROPERTY_LINE_GRADIENT));
		}
		if (properties.containsKey(ShapeModule.PROPERTY_FILL_GRADIENT)) {
			 fillGradient = TiUIHelper.buildGradientDrawable(properties.getKrollDict(ShapeModule.PROPERTY_FILL_GRADIENT));
		}
		if (properties.containsKey(ShapeModule.PROPERTY_FILL_COLOR)) {
			Utils.styleColor(properties, ShapeModule.PROPERTY_FILL_COLOR, getOrCreateFillPaint());
		}
//		if (properties.containsKey(ShapeModule.PROPERTY_FILL_SHADOW)) {
//			Utils.styleShadow(properties, ShapeModule.PROPERTY_FILL_SHADOW, getOrCreateFillPaint());
//		}
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
		if (properties.containsKey(TiC.PROPERTY_ANCHOR_POINT)) {
			applyAnchorPoint(properties.get(TiC.PROPERTY_ANCHOR_POINT));
		}
		if (properties.containsKey(TiC.PROPERTY_TRANSFORM)) {
			this.transform = (Ti2DMatrix)properties.get(TiC.PROPERTY_TRANSFORM);
			this.matrix = null;
			needsMatrix = (this.transform != null);
		}
		if (properties.containsKey(ShapeModule.PROPERTY_FILL_INVERSED)) {
			this.fillInversed = properties.getBoolean(ShapeModule.PROPERTY_FILL_INVERSED);
		}
		if (properties.containsKey(ShapeModule.PROPERTY_LINE_INVERSED)) {
			this.lineInversed = properties.getBoolean(ShapeModule.PROPERTY_LINE_INVERSED);
		}
		if (properties.containsKey(ShapeModule.PROPERTY_LINE_CLIPPED)) {
			this.lineClipped = properties.getBoolean(ShapeModule.PROPERTY_LINE_CLIPPED);
		}
	}
	
	@Override
	public void animationFinished(TiAnimator animation) {
		super.animationFinished(animation);
		redraw();
	}

	public void afterAnimationReset()
	{
		super.afterAnimationReset();
		update();
	}
	
	public void recursiveCancelAllAnimations() {
		cancelAllAnimations();
		for (int i = 0; i < mShapes.size(); i++) {
			ShapeProxy shapeProxy = mShapes.get(i);
			shapeProxy.recursiveCancelAllAnimations();
		}
	}
	
	@Override
	public void propertiesChanged(List<KrollPropertyChange> changes,KrollProxy proxy) {}

	@Override
	public void listenerAdded(String type, int count, KrollProxy proxy) {}

	@Override
	public void listenerRemoved(String type, int count, KrollProxy proxy) {}
}