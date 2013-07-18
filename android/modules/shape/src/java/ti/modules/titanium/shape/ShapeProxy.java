
package ti.modules.titanium.shape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollPropertyChange;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollProxyListener;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiPoint;
import org.appcelerator.titanium.proxy.NonViewAnimatableProxy;
import org.appcelerator.titanium.util.TiAnimatorSet;

import ti.modules.titanium.shape.ShapeViewProxy.TiShapeView;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Region;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Build;
import android.view.MotionEvent;

@Kroll.proxy(creatableInModule = ShapeModule.class, propertyAccessors={
	TiC.PROPERTY_NAME
})
public class ShapeProxy extends NonViewAnimatableProxy {
	// Standard Debugging variables
	private static final String TAG = "PathProxy";
	private ShapeViewProxy shapeViewProxy;
	protected Paint fillPaint;
	protected Paint linePaint;
	protected Rect currentBounds;
	protected Rect parentBounds;
	protected Region currentRegion;
	protected Context context;
	private final ArrayList<ShapeProxy> mShapes;
	protected Path path;
	
	
	public static class Pathable {
		public void updatePathForRect(Context context, Path path, int width, int height) {}
	}

	public ShapeProxy() {
		super();
		mShapes = new ArrayList<ShapeProxy>();
		shapeViewProxy = null;
		fillPaint = null;
		linePaint = null;
		path = new Path();
		currentRegion = new Region();
	}
	
	private Paint getOrCreateFillPaint() {
		if (fillPaint == null) {
			fillPaint = new Paint();
			fillPaint.setStyle(Style.FILL);
			fillPaint.setAntiAlias(true);
		}
		return fillPaint;
	}
	
	private Paint getOrCreateLinePaint() {
		if (linePaint == null) {
			linePaint = new Paint();
			linePaint.setStyle(Style.STROKE);
			linePaint.setAntiAlias(true);
		}
		return linePaint;
	}

	public Path getPath() {
		return path;
	}
	
	public ShapeProxy(TiContext context) {
		this();
	}
	
	protected void onLayoutChanged(Context context, Rect parentBounds) {
		this.context = context;
		update(context, parentBounds);
		Log.d(TAG, "onLayoutChanged " + parentBounds.toString(), Log.DEBUG_MODE);
		// child is gonna be drawn relatively to its parent so we need to remove the translation for computation
//		Rect childParentBounds = new Rect(0,0, currentBounds.width(), currentBounds.height());
		for (int i = 0; i < mShapes.size(); i++) {
			ShapeProxy shapeProxy = mShapes.get(i);
			shapeProxy.onLayoutChanged(this.context, currentBounds);
		}
	}
	
	protected void updatePath() {
		
	}

	protected void update(Context context, Rect parentBounds) {
		this.parentBounds = parentBounds;
		updatePath();
		this.currentRegion = clipRectWithPath(parentBounds, path);
		this.currentBounds = currentRegion.getBounds();
		updateGradients(context, currentBounds);
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
		if (options.containsKey("fillGradient")) {
			KrollDict bgOptions = options.getKrollDict("fillGradient");
			fillPaint.setShader(
					Utils.styleGradient(bgOptions, context, bounds));
			Utils.styleOpacity(options, "fillOpacity", fillPaint);
		}
		if (options.containsKey("lineGradient")) {
			KrollDict bgOptions = options.getKrollDict("lineGradient");
			Shader shader = Utils.styleGradient(bgOptions, context, bounds);
			linePaint.setShader(shader);
			Utils.styleOpacity(options, "lineOpacity", linePaint);
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
	
	public void drawOnCanvas(Canvas canvas) {
		
		
		if (fillPaint != null) {
			canvas.drawPath(path, fillPaint);
		}
		if (linePaint != null) {
			canvas.drawPath(path, linePaint);
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
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void prepareAnimatorSet(TiAnimatorSet tiSet, List<Animator> list, HashMap options) {
		super.prepareAnimatorSet(tiSet, list, options);
//		if (options.containsKey(TiC.PROPERTY_VALUE)) {
//			ObjectAnimator anim = ObjectAnimator.ofInt(this, TiC.PROPERTY_VALUE, TiConvert.toInt(options.get(TiC.PROPERTY_VALUE)));
//			list.add(anim);
//		}
	}

	// Start Utility Methods

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}
	
	protected void handleTouchEvent(String motionEvent, MotionEvent event, KrollDict data) {
		if (context != null) {
			if (currentRegion.contains((int)event.getX(), (int)event.getY())) {
				if (hasListeners(motionEvent)) {
					fireEvent(motionEvent, data);
				}
				MotionEvent eventForChidren = MotionEvent.obtain(event.getDownTime(), 
						event.getEventTime(), 
						event.getAction(), 
						event.getX() - currentBounds.left, 
						event.getY() - currentBounds.top, 
						event.getMetaState());
				for (int i = 0; i < mShapes.size(); i++) {
					
					ShapeProxy shapeProxy = mShapes.get(i);
					shapeProxy.handleTouchEvent(motionEvent, eventForChidren, data);
				}
			}
		}
	}
	
//	@Override
//	public void propertyChanged(String key, Object oldValue, Object newValue,
//			KrollProxy proxy) {
//		boolean needsUpdateGradients = false;
//		if (key.equals("lineWidth")) {
//			Utils.styleStrokeWidth(properties, "lineWidth", "1", getOrCreateLinePaint());
//		}
//		else if (key.equals("lineOpacity")) {
//			Utils.styleOpacity(properties, "lineOpacity", getOrCreateLinePaint());
//		}
//		else if (key.equals("lineDash")) {
//			Utils.styleDash(properties, "lineDash", getOrCreateLinePaint());
//		}
//		else if (key.equals("fillOpacity")) {
//			Utils.styleOpacity(properties, "fillOpacity", getOrCreateFillPaint());
//		}
//		else if (key.equals("fillColor")) {
//			Utils.styleColor(properties, "fillColor", getOrCreateFillPaint());
//		}
//		else return;
//		if (shapeViewProxy != null) {
//			shapeViewProxy.redraw();
//		}
//		if (needsUpdateGradients == true) {
//			updateGradients();
//		}
//	}

	@Override
	public void handleCreationDict(KrollDict properties) {
		super.handleCreationDict(properties);
		if (properties == null) return;
		if (properties.containsKey("lineEmboss")) {
			Utils.styleEmboss(properties, "lineEmboss", getOrCreateLinePaint());
		}
		if (properties.containsKey("lineWidth")) {
			Utils.styleStrokeWidth(properties, "lineWidth", "1", getOrCreateLinePaint());
		}
		if (properties.containsKey("lineOpacity")) {
			Utils.styleOpacity(properties, "lineOpacity", getOrCreateLinePaint());
		}
		
		if (properties.containsKey("lineJoin")) {
			Utils.styleJoin(properties, "lineJoin", getOrCreateLinePaint());
		}
		if (properties.containsKey("lineColor")) {
			Utils.styleColor(properties, "lineColor", getOrCreateLinePaint());
		}
		if (properties.containsKey("lineCap")) {
			Utils.styleCap(properties, "lineCap", getOrCreateLinePaint());
		}
		if (properties.containsKey("lineShadow")) {
			Utils.styleShadow(properties, "lineShadow", getOrCreateLinePaint());
		}
		if (properties.containsKey("lineDash")) {
			Utils.styleDash(properties, "lineDash", getOrCreateLinePaint());
		}
		if (properties.containsKey("fillOpacity")) {
			Utils.styleOpacity(properties, "fillOpacity", getOrCreateFillPaint());
		}
		if (properties.containsKey("fillColor")) {
			Utils.styleColor(properties, "fillColor", getOrCreateFillPaint());
		}
		if (properties.containsKey("fillShadow")) {
			Utils.styleShadow(properties, "fillShadow", getOrCreateFillPaint());
		}
		if (properties.containsKey("fillEmboss")) {
			Utils.styleEmboss(properties, "fillEmboss", getOrCreateFillPaint());
		}
		
	}

	private void addShape(ShapeProxy proxy) {
		if (!mShapes.contains(proxy)) {
			mShapes.add(proxy);
			proxy.setShapeViewProxy(shapeViewProxy);
		}
	}

	private void removeShape(ShapeProxy proxy) {
		if (!mShapes.contains(proxy))
			return;
		mShapes.remove(proxy);
		proxy.setShapeViewProxy(null);
	}
	
	@Kroll.method
	public void update() {
		if (context != null) {
			update(context, parentBounds);
			for (int i = 0; i < mShapes.size(); i++) {
				ShapeProxy shapeProxy = mShapes.get(i);
				shapeProxy.update(this.context, currentBounds);
			}
		}
	}
	
	@Kroll.method
	public void add(Object shape) {
		Log.d(TAG, "add", Log.DEBUG_MODE);
		if (!(shape instanceof ShapeProxy)) {
			Log.e(TAG, "add: must be a Shape");
			return;
		}
		addShape((ShapeProxy)shape);
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
}