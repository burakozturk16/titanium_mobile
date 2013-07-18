package ti.modules.titanium.charts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.util.TiConvert;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeriesFormatter;

@Kroll.proxy
public class XYSerieProxy extends KrollProxy {
	// Standard Debugging variables
	private static final String TAG = "PlotStepProxy";
	protected SimpleXYSeries series;
	protected String mTitle;
	protected Context context;

	// Constructor
	public XYSerieProxy(TiContext tiContext) {
		super(tiContext);
	}

	public XYSerieProxy() {
		super();
	}

	public void setContext(Context context) {
		this.context = context;
	}
	
	public void initRenderer(XYPlot plot) {
	}


	public SimpleXYSeries getSeries() {
		return series;
	}
	
	public XYSeriesFormatter<?> getFormatter() {
		return null;
	}
	
	public void updateGradients(Context context, Rect rect) {
	}

	// Handle creation options
	@Override
	public void handleCreationDict(KrollDict options) {
		Log.d(TAG, "handleCreationDict ");
		super.handleCreationDict(options);

		mTitle = options.optString("name", "");
		series = new SimpleXYSeries(mTitle);
		series.useImplicitXVals();
		if (options.containsKey("implicitXVals")) {
			series.useImplicitXVals();
		}
		if (options.containsKey("data")) {
			setData((Object[]) options.get("data"));
		}


	}

	@Kroll.method
	public void setData(Object args) {
		Object[] data = (Object[]) args;
		if (data == null || data.length == 0)
			series.setModel(new ArrayList<Number>(),
					SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
		else {
			if (data[0].getClass().isArray()) {
				List<Number> model = new ArrayList<Number>();
				Object[] x = (Object[]) (data[0]);
				Object[] y = (Object[]) (data[1]);
				int length = x.length;
				for (int i = 0; i < length; i++) {
					model.add((Number) x[i]);
					model.add((Number) y[i]);
				}
				series.setModel(model,
						SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED);
			} else {
				series.setModel(Arrays.asList(TiConvert.toNumberArray(data)),
						SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
			}
		}
	}
	
	

	// Methods
	@Kroll.method
	public void useImplicitXVals() {
		series.useImplicitXVals();
	}
	@Kroll.method
	public void removeFirst() {
		series.removeFirst();
	}
	@Kroll.method
	public void removeLast() {
		series.removeLast();
	}
	@Kroll.method
	public void addFirst(Object x, Object y) {
		series.addFirst((Number)x, (Number)y);
	}
	@Kroll.method
	public void addLast(Object x, Object y) {
		series.addLast((Number)x, (Number)y);
	}
	
	@Kroll.method
	public int size() {
		return series.size();
	}
}
