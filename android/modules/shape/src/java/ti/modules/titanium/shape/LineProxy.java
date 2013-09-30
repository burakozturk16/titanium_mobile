
package ti.modules.titanium.shape;
import java.util.ArrayList;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiPoint;
import org.appcelerator.titanium.util.TiAnimatorSet;


import android.animation.PropertyValuesHolder;
import android.animation.TypeEvaluator;
import android.annotation.TargetApi;
import android.graphics.Point;
import android.os.Build;

@Kroll.proxy(creatableInModule = ShapeModule.class, propertyAccessors={
	ShapeModule.PROPERTY_POINTS
})
public class LineProxy extends ArcProxy{
	// Standard Debugging variables
	private static final String TAG = "LineProxy";
	protected ArrayList<TiBezierPoint> points;
	
	private class TiBezierPoint {
		public TiPoint point = null;
		public TiPoint curvePoint1 = null;
		public TiPoint curvePoint2 = null;
		public TiBezierPoint() {
		}
	}
	
	public LineProxy() {
		super();
		pathable = new Line();
		points = new ArrayList<TiBezierPoint>();
		anchor = AnchorPosition.LEFT_MIDDLE;
	}
	
	public LineProxy(TiContext context) {
		this();
	}
	
	@Override
	protected void updatePath() {
		int width = parentBounds.width();
		int height = parentBounds.height();
		ArrayList<BezierPoint> realPoints = new ArrayList<BezierPoint>(this.points.size());
		for (int i = 0; i < this.points.size(); i++) {
			TiBezierPoint tiBPoint = this.points.get(i);
			BezierPoint bPoint = new BezierPoint();
			bPoint.point = computePoint(tiBPoint.point, anchor, width, height);
			if (tiBPoint.curvePoint1 != null) bPoint.curvePoint1 = computePoint(tiBPoint.curvePoint1, anchor, width, height);
			if (tiBPoint.curvePoint2 != null) bPoint.curvePoint2 = computePoint(tiBPoint.curvePoint2, anchor, width, height);
			realPoints.add(bPoint);
		}
		((Line) pathable).setPoints(realPoints);
		super.updatePath();
	}
	
	private void setPointsFromObject(Object[] obj) {
		this.points.clear();
		if (obj == null) return;
		for (int i = 0; i < obj.length; i++) {
			Object[] pointArray = (Object[]) obj[i];
			if (pointArray == null || pointArray.length < 2) continue;
			TiBezierPoint tiBPoint = new TiBezierPoint();
			tiBPoint.point = new TiPoint(pointArray[0], pointArray[1]);
			if (pointArray.length >= 4) {
				tiBPoint.curvePoint1 = new TiPoint(pointArray[2], pointArray[3]);
				if (pointArray.length >= 6) {
					tiBPoint.curvePoint2 = new TiPoint(pointArray[4], pointArray[5]);
				}
			}
			this.points.add(tiBPoint);
		}
	}
	
	private ArrayList<BezierPoint> getRealPointsFromObject(Object[] obj, int width, int height) {
		if (obj == null) return null;
		ArrayList<BezierPoint> result = new ArrayList<BezierPoint>();
		for (int i = 0; i < obj.length; i++) {
			Object[] pointArray = (Object[]) obj[i];
			if (pointArray == null || pointArray.length < 2) continue;
			BezierPoint bPoint = new BezierPoint();
			bPoint.point = computePoint(new TiPoint(pointArray[0], pointArray[1]), anchor, width, height);
			if (pointArray.length >= 4) {
				bPoint.curvePoint1 = computePoint(new TiPoint(pointArray[2], pointArray[3]), anchor, width, height);
				if (pointArray.length >= 6) {
					bPoint.curvePoint2 = computePoint(new TiPoint(pointArray[4], pointArray[5]), anchor, width, height);
				}
			}
			result.add(bPoint);
		}
		return result;
	}

	@Override
	public void processProperties(KrollDict properties) {
		super.processProperties(properties);
		if (properties.containsKey(ShapeModule.PROPERTY_POINTS)) {
			setPointsFromObject((Object[]) properties.get(ShapeModule.PROPERTY_POINTS));
		}
	}
	
	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {
		if (key.equals(ShapeModule.PROPERTY_POINTS)) {
			setPointsFromObject((Object[]) newValue);
		}
		else super.propertyChanged(key, oldValue, newValue, proxy);
		redraw();
	}

	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public class BezierPointsEvaluator implements TypeEvaluator<ArrayList<BezierPoint>> {
		
		public BezierPointsEvaluator() {
		}
		
		public ArrayList<BezierPoint> evaluate(float fraction, ArrayList<BezierPoint> startValue,
				ArrayList<BezierPoint> endValue) {
			ArrayList<BezierPoint> result = new ArrayList<BezierPoint>();
			if (startValue == null) {
				for (int i = 0; i < endValue.size(); i++) {
					BezierPoint point = endValue.get(i);
					result.add(point.fraction(fraction));
				}
			}
			else if (endValue.size() != startValue.size()) return endValue;
			else {
				for (int i = 0; i < endValue.size(); i++) {
					BezierPoint startPoint = startValue.get(i);
					BezierPoint endPoint = endValue.get(i);
					result.add(startPoint.fraction(fraction, endPoint));
				}
			}
			return result;
		}

	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void preparePropertiesSet(TiAnimatorSet tiSet, List<PropertyValuesHolder> propertiesList, KrollDict animOptions) {
		super.preparePropertiesSet(tiSet, propertiesList, animOptions);
		
		if (animOptions.containsKey(ShapeModule.PROPERTY_POINTS)) {
			
			int width = parentBounds.width();
			int height = parentBounds.height();
			
			ArrayList<BezierPoint> realPoints = getRealPointsFromObject((Object[]) animOptions.get(ShapeModule.PROPERTY_POINTS), width, height);
			PropertyValuesHolder anim = PropertyValuesHolder.ofObject("points", new BezierPointsEvaluator(), getPoints(), realPoints);
			propertiesList.add(anim);
			
		}
	}
	
	//ANIMATION getter/setter
	public void setPoints(ArrayList<BezierPoint> points) {
		((Line) pathable).setPoints(points);
	}
	public ArrayList<BezierPoint> getPoints() {
		return ((Line) pathable).getPoints();		
	}
}