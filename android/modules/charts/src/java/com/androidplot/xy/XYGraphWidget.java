/*
 * Copyright 2012 AndroidPlot.com
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.androidplot.xy;

import android.graphics.*;
import android.graphics.Paint.Align;
import android.graphics.Region.Op;
import android.view.Gravity;

import com.androidplot.exception.PlotRenderException;
import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.LayoutManager;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.widget.Widget;
import com.androidplot.util.FontUtils;
import com.androidplot.util.ValPixConverter;
import com.androidplot.util.ZHash;
import com.androidplot.util.ZIndexable;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.HashMap;

/**
 * Displays graphical data annotated with domain and range tick markers.
 */
public class XYGraphWidget extends Widget {

    public float getRangeLabelOrientation() {
        return rangeLabelOrientation;
    }

    public void setRangeLabelOrientation(float rangeLabelOrientation) {
        this.rangeLabelOrientation = rangeLabelOrientation;
    }

    public float getDomainLabelOrientation() {
        return domainLabelOrientation;
    }

    public void setDomainLabelOrientation(float domainLabelOrientation) {
        this.domainLabelOrientation = domainLabelOrientation;
    }

    /**
     * Will be used in a future version.
     */
    public enum XYPlotOrientation {
        HORIZONTAL, VERTICAL
    }

    private static final int MARKER_LABEL_SPACING = 2;
    private static final int CURSOR_LABEL_SPACING = 2; // space between cursor
    private static final String TAG = "AndroidPlot";
                                                       // lines and label in
                                                       // pixels
    private float domainLabelWidth = 15; // how many pixels is the area
                                         // allocated for domain labels
    private float rangeLabelWidth = 41; // ...
    private float domainLabelVerticalOffset = -5;
    private float domainLabelHorizontalOffset = 0.0f;
    private float rangeLabelHorizontalOffset = 1.0f;   // allows tweaking of text position
    private float rangeLabelVerticalOffset = 0.0f;  // allows tweaking of text position
    
    private int ticksPerRangeLabel = 1;
    private int ticksPerDomainLabel = 1;
    private float gridPaddingTop = 0;
    private float gridPaddingBottom = 0;
    private float gridPaddingLeft = 0;
    private float gridPaddingRight = 0;
    private int domainLabelTickExtension = 5;
    private int rangeLabelTickExtension = 5;
    private Paint gridBackgroundPaint;
    private Paint rangeGridLinePaint;
    private Paint rangeSubGridLinePaint;
    private Paint domainGridLinePaint;
    private Paint domainSubGridLinePaint;
    private Paint domainLabelPaint;
    private Paint rangeLabelPaint;
    private Paint domainCursorPaint;
    private Paint rangeCursorPaint;
    private Paint cursorLabelPaint;
    private Paint cursorLabelBackgroundPaint;
    private XYPlot plot;
    private Format rangeValueFormat;
    private Format domainValueFormat;
    private Paint domainOriginLinePaint;
    private Paint rangeOriginLinePaint;
    private Paint domainOriginLabelPaint;
    private float domainOriginLabelAngle;
    private float domainOriginLabelOffset;
    private String domainOriginLabel;
    private Paint rangeOriginLabelPaint;
    private float rangeOriginLabelAngle;
    private float rangeOriginLabelOffset;
    private String rangeOriginLabel;
    private RectF gridRect;
    private RectF paddedGridRect;
    private float domainCursorPosition;
    private float rangeCursorPosition;
    @SuppressWarnings("FieldCanBeLocal")
    private boolean drawCursorLabelEnabled = true;
    private boolean drawMarkersEnabled = true;
    
    private boolean rangeAxisLeft = true;
    private boolean domainAxisBottom = true;
    
    private HashMap<Double, String> domainTicksValues;
    private HashMap<Double, String> rangeTicksValues;

    private float rangeLabelOrientation;
    private float domainLabelOrientation;

    // TODO: consider typing this manager with a special
    // axisLabelRegionFormatter
    // private ZHash<LineRegion, AxisValueLabelFormatter> domainLabelRegions;
    // private ZHash<LineRegion, AxisValueLabelFormatter> rangeLabelRegions;
    private ZHash<RectRegion, AxisValueLabelFormatter> axisValueLabelRegions;
	private int domainAxisAlignment;
	private  int rangeAxisAlignment;
	private RectF domainOriginLabelSize;
	private RectF rangeOriginLabelSize;

    {
        gridBackgroundPaint = new Paint();
        gridBackgroundPaint.setColor(Color.rgb(140, 140, 140));
        gridBackgroundPaint.setStyle(Paint.Style.FILL);
        rangeGridLinePaint = new Paint();
        rangeGridLinePaint.setColor(Color.rgb(180, 180, 180));
        rangeGridLinePaint.setAntiAlias(true);
        rangeGridLinePaint.setStyle(Paint.Style.STROKE);
        domainGridLinePaint = new Paint(rangeGridLinePaint);
        domainSubGridLinePaint = new Paint(domainGridLinePaint);
        rangeSubGridLinePaint = new Paint(rangeGridLinePaint);
        domainOriginLinePaint = new Paint();
        domainOriginLinePaint.setColor(Color.WHITE);
        domainOriginLinePaint.setAntiAlias(true);
        rangeOriginLinePaint = new Paint();
        rangeOriginLinePaint.setColor(Color.WHITE);
        rangeOriginLinePaint.setAntiAlias(true);
        domainOriginLabelPaint = new Paint();
        domainOriginLabelPaint.setColor(Color.WHITE);
        domainOriginLabelPaint.setAntiAlias(true);
        domainOriginLabelPaint.setTextAlign(Paint.Align.CENTER);
        rangeOriginLabelPaint = new Paint();
        rangeOriginLabelPaint.setColor(Color.WHITE);
        rangeOriginLabelPaint.setAntiAlias(true);
        rangeOriginLabelPaint.setTextAlign(Paint.Align.CENTER);
        domainLabelPaint = new Paint();
        domainLabelPaint.setColor(Color.LTGRAY);
        domainLabelPaint.setAntiAlias(true);
        domainLabelPaint.setTextAlign(Paint.Align.CENTER);
        rangeLabelPaint = new Paint();
        rangeLabelPaint.setColor(Color.LTGRAY);
        rangeLabelPaint.setAntiAlias(true);
        rangeLabelPaint.setTextAlign(Paint.Align.RIGHT);
        domainCursorPaint = new Paint();
        domainCursorPaint.setColor(Color.YELLOW);
        rangeCursorPaint = new Paint();
        rangeCursorPaint.setColor(Color.YELLOW);
        cursorLabelPaint = new Paint();
        cursorLabelPaint.setColor(Color.YELLOW);
        cursorLabelBackgroundPaint = new Paint();
        cursorLabelBackgroundPaint.setColor(Color.argb(100, 50, 50, 50));
        setMarginTop(7);
        setMarginRight(4);
        setMarginBottom(4);
        rangeValueFormat = new DecimalFormat("0.0");
        domainValueFormat = new DecimalFormat("0.0");
        
        domainOriginLabelAngle = 0;
        domainOriginLabelOffset = 20.0f;
        rangeOriginLabelAngle = -90;
        rangeOriginLabelOffset = 20.0f;
        // domainLabelRegions = new ZHash<LineRegion,
        // AxisValueLabelFormatter>();
        // rangeLabelRegions = new ZHash<LineRegion, AxisValueLabelFormatter>();
        axisValueLabelRegions = new ZHash<RectRegion, AxisValueLabelFormatter>();
    }

    public XYGraphWidget(LayoutManager layoutManager, XYPlot plot, SizeMetrics sizeMetrics) {
        super(layoutManager, sizeMetrics);
        this.plot = plot;
        domainTicksValues = new HashMap<Double, String>();
        rangeTicksValues = new HashMap<Double, String>();
    }

    public ZIndexable<RectRegion> getAxisValueLabelRegions() {
        return axisValueLabelRegions;
    }

    /**
     * Add a new Region used for rendering axis valuelabels. Note that it is
     * possible to add multiple Region instances which overlap, in which cast
     * the last region to be added will be used. It is up to the developer to
     * guard against this often undesireable situation.
     * 
     * @param region
     * @param formatter
     */
    public void addAxisValueLabelRegion(RectRegion region,
            AxisValueLabelFormatter formatter) {
        axisValueLabelRegions.addToTop(region, formatter);
    }

    /**
     * Convenience method - wraps addAxisValueLabelRegion, using
     * Double.POSITIVE_INFINITY and Double.NEGATIVE_INFINITY to mask off range
     * axis value labels.
     * 
     * @param min
     * @param max
     * @param formatter
     * 
     */
    public void addDomainAxisValueLabelRegion(double min, double max,
            AxisValueLabelFormatter formatter) {
        addAxisValueLabelRegion(new RectRegion(min, max,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, null),
                formatter);
    }

    /**
     * Convenience method - wraps addAxisValueLabelRegion, using
     * Double.POSITIVE_INFINITY and Double.NEGATIVE_INFINITY to mask off domain
     * axis value labels.
     * 
     * @param min
     * @param max
     * @param formatter
     */
    public void addRangeAxisValueLabelRegion(double min, double max,
            AxisValueLabelFormatter formatter) {
        addAxisValueLabelRegion(new RectRegion(Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, min, max, null), formatter);
    }

    /*
     * public void addRangeLabelRegion(LineRegion region,
     * AxisValueLabelFormatter formatter) { rangeLabelRegions.addToTop(region,
     * formatter); }
     * 
     * public boolean removeRangeLabelRegion(LineRegion region) { return
     * rangeLabelRegions.remove(region); }
     */

    /**
     * Returns the formatter associated with the first (bottom) Region
     * containing x and y.
     * 
     * @param x
     * @param y
     * @return the formatter associated with the first (bottom) region
     *         containing x and y. null otherwise.
     */
    public AxisValueLabelFormatter getAxisValueLabelFormatterForVal(double x,
            double y) {
        for (RectRegion r : axisValueLabelRegions.elements()) {
            if (r.containsValue(x, y)) {
                return axisValueLabelRegions.get(r);
            }
        }
        return null;
    }

    public AxisValueLabelFormatter getAxisValueLabelFormatterForDomainVal(
            double val) {
        for (RectRegion r : axisValueLabelRegions.elements()) {
            if (r.containsDomainValue(val)) {
                return axisValueLabelRegions.get(r);
            }
        }
        return null;
    }

    public AxisValueLabelFormatter getAxisValueLabelFormatterForRangeVal(
            double val) {
        for (RectRegion r : axisValueLabelRegions.elements()) {
            if (r.containsRangeValue(val)) {
                return axisValueLabelRegions.get(r);
            }
        }
        return null;
    }

    /**
     * Returns the formatter associated with the first (bottom-most) Region
     * containing value.
     * 
     * @param value
     * @return
     */
    /*
     * public AxisValueLabelFormatter getXYAxisFormatterForRangeVal(double
     * value) { return getRegionContainingVal(rangeLabelRegions, value); }
     *//**
     * Returns the formatter associated with the first (bottom-most) Region
     * containing value.
     * 
     * @param value
     * @return
     */
    /*
     * public AxisValueLabelFormatter getXYAxisFormatterForDomainVal(double
     * value) { return getRegionContainingVal(domainLabelRegions, value); }
     */

    /*
     * private AxisValueLabelFormatter getRegionContainingVal(ZHash<LineRegion,
     * AxisValueLabelFormatter> zhash, double val) { for (LineRegion r :
     * zhash.elements()) { if (r.contains(val)) { return
     * rangeLabelRegions.get(r); } } // nothing found return null; }
     */

    /**
     * Returns a RectF representing the grid area last drawn by this plot.
     * 
     * @return
     */
    public RectF getGridRect() {
        return paddedGridRect;
    }
     private String getFormattedRangeValue(Number value) {
    	double v = value.doubleValue();
    	if (rangeTicksValues.containsKey(v)) {
        	return rangeTicksValues.get(v);
        }
        else {
        	return rangeValueFormat.format(value);
        }
    }

    private String getFormattedDomainValue(Number value) {
    	double v = value.doubleValue();
    	if (domainTicksValues.containsKey(v)) {
        	return domainTicksValues.get(v);
        }
        else {
        	return domainValueFormat.format(value);
        }
    }

    /**
     * Convenience method. Wraps getYVal(float)
     * 
     * @param point
     * @return
     */
    public Double getYVal(PointF point) {
        return getYVal(point.y);
    }

    /**
     * Converts a y pixel to a y value.
     * 
     * @param yPix
     * @return
     */
    public Double getYVal(float yPix) {
        if (plot.getCalculatedMinY() == null
                || plot.getCalculatedMaxY() == null) {
            return null;
        }
        return ValPixConverter.pixToVal(yPix - paddedGridRect.top, plot
                .getCalculatedMinY().doubleValue(), plot.getCalculatedMaxY()
                .doubleValue(), paddedGridRect.height(), true);
    }

    /**
     * Convenience method. Wraps getXVal(float)
     * 
     * @param point
     * @return
     */
    public Double getXVal(PointF point) {
        return getXVal(point.x);
    }

    /**
     * Converts an x pixel into an x value.
     * 
     * @param xPix
     * @return
     */
    public Double getXVal(float xPix) {
        if (plot.getCalculatedMinX() == null
                || plot.getCalculatedMaxX() == null) {
            return null;
        }
        return ValPixConverter.pixToVal(xPix - paddedGridRect.left, plot
                .getCalculatedMinX().doubleValue(), plot.getCalculatedMaxX()
                .doubleValue(), paddedGridRect.width(), false);
    }

    @Override
    protected void doOnDraw(Canvas canvas, RectF widgetRect)
            throws PlotRenderException {
        gridRect = getGridRect(widgetRect); // used for drawing the background
                                            // of the grid
        paddedGridRect = getPaddedGridRect(gridRect); // used for drawing lines
                                                      // etc.
        //Log.v(TAG, "gridRect :" + gridRect);
        //Log.v(TAG, "paddedGridRect :" + paddedGridRect);
        // if (!plot.isEmpty()) {
        // don't draw if we have no space to draw into
        if ((paddedGridRect.height() > 0.0f) && (paddedGridRect.width() > 0.0f)) {
            if (plot.getCalculatedMinX() != null
                    && plot.getCalculatedMaxX() != null
                    && plot.getCalculatedMinY() != null
                    && plot.getCalculatedMaxY() != null) {
            	float domainOriginF;
        		if (plot.getDomainOrigin() != null) {
        			double domainOriginVal = plot.getDomainOrigin().doubleValue();
        			domainOriginF = ValPixConverter.valToPix(domainOriginVal, plot
        					.getCalculatedMinX().doubleValue(), plot
        					.getCalculatedMaxX().doubleValue(), paddedGridRect.width(),
        					false);
        			domainOriginF += paddedGridRect.left;
        			// if no origin is set, use the leftmost value visible on the grid:
        		} else {
        			domainOriginF = paddedGridRect.left;
        		}

        		float rangeOriginF;
        		if (plot.getRangeOrigin() != null) {
        			// --------- NEW WAY ------
        			double rangeOriginD = plot.getRangeOrigin().doubleValue();
        			rangeOriginF = ValPixConverter.valToPix(rangeOriginD, plot
        					.getCalculatedMinY().doubleValue(), plot
        					.getCalculatedMaxY().doubleValue(),
        					paddedGridRect.height(), true);
        			rangeOriginF += paddedGridRect.top;
        			// if no origin is set, use the leftmost value visible on the grid
        		} else {
        			rangeOriginF = paddedGridRect.bottom;
        		}
        		
        		XYStep domainStep = XYStepCalculator.getStep(plot, XYAxisType.DOMAIN,
                        paddedGridRect, plot.getCalculatedMinX().doubleValue(), plot
                                .getCalculatedMaxX().doubleValue());
        		XYStep rangeStep = XYStepCalculator.getStep(plot, XYAxisType.RANGE,
                        paddedGridRect, plot.getCalculatedMinY().doubleValue(), plot
                                .getCalculatedMaxY().doubleValue());
        		canvas.save();
        		canvas.clipRect(paddedGridRect);
                drawGrid(canvas, domainOriginF, rangeOriginF, domainStep, rangeStep);
                drawData(canvas);
                drawCursors(canvas);
                if (isDrawMarkersEnabled()) {
                    drawMarkers(canvas);
                }
        		canvas.restore();
               drawAxis(canvas, domainOriginF, rangeOriginF, domainStep, rangeStep);
            }
        }
        // }
    }

    private RectF getGridRect(RectF widgetRect) {
        return new RectF(widgetRect.left + ((rangeAxisLeft)?rangeLabelWidth:1),
                widgetRect.top + ((domainAxisBottom)?1:domainLabelWidth),
                widgetRect.right - ((rangeAxisLeft)?1:rangeLabelWidth),
                widgetRect.bottom - ((domainAxisBottom)?domainLabelWidth:1));
    }

    private RectF getPaddedGridRect(RectF gridRect) {
        return new RectF(gridRect.left + gridPaddingLeft, gridRect.top
                + gridPaddingTop, gridRect.right - gridPaddingRight,
                gridRect.bottom - gridPaddingBottom);
    }

    private void drawTickText(Canvas canvas, XYAxisType axis, Number value,
            float xPix, float yPix, Paint labelPaint) {
        AxisValueLabelFormatter rf = null;
        String txt = null;
        double v = value.doubleValue();

        int canvasState = canvas.save();
        try {
            switch (axis) {
                case DOMAIN:
                    rf = getAxisValueLabelFormatterForDomainVal(v);
                    txt = getFormattedDomainValue(value);
                    canvas.rotate(getDomainLabelOrientation(), xPix, yPix);
                    break;
                case RANGE:
                    rf = getAxisValueLabelFormatterForRangeVal(v);
                    txt = getFormattedRangeValue(value);
                    canvas.rotate(getRangeLabelOrientation(), xPix, yPix);
                    break;
            }

            // if a matching region formatter was found, create a clone
            // of labelPaint and use the formatter's color. Otherwise
            // just use labelPaint:
            Paint p;
            if (rf != null) {
                // p = rf.getPaint();
                p = new Paint(labelPaint);
                p.setColor(rf.getColor());
                // p.setColor(Color.RED);
            } else {
                p = labelPaint;
            }
            canvas.drawText(txt, xPix, yPix, p);
        } finally {
            canvas.restoreToCount(canvasState);
        }
    }

    private void drawDomainTick(Canvas canvas, float xPix, Number xVal,
            Paint labelPaint, Paint linePaint, boolean drawLineOnly) {
    	
    	if (linePaint != null) {
        	float extensions = drawLineOnly?0:(domainAxisBottom?domainLabelTickExtension:-domainLabelTickExtension);
        	canvas.drawLine(xPix, gridRect.top, xPix, gridRect.bottom
                    + extensions, linePaint);
    	}
    }

    public void drawRangeTick(Canvas canvas, float yPix, Number yVal,
            Paint labelPaint, Paint linePaint, boolean drawLineOnly) {
        if (linePaint != null) {
        	float extensions = drawLineOnly?0:(rangeAxisLeft?-rangeLabelTickExtension:rangeLabelTickExtension);
        	if (rangeAxisLeft){
        		canvas.drawLine(gridRect.left + extensions, yPix,
                    gridRect.right, yPix, linePaint);
        	}
        }
    }

    /**
     * Draws the drid and domain/range labels for the plot.
     * 
     * @param canvas
     */
	protected void drawAxis(Canvas canvas, float domainOriginF,
			float rangeOriginF, XYStep domainStep, XYStep rangeStep) {

		Boolean domainNeedsDrawing = true;
		Boolean rangeNeedsDrawing = true;
		float domainOffset = domainOriginF;
		if (domainAxisAlignment == Gravity.LEFT) {
			domainOffset = paddedGridRect.left;

		} else if (domainAxisAlignment == Gravity.RIGHT) {
			domainOffset = paddedGridRect.right;
			if (domainOriginLinePaint != null) {
				domainOffset -= domainOriginLinePaint.getStrokeWidth();
			}
		} else {
			if (domainOriginF < paddedGridRect.left
					|| domainOriginF > paddedGridRect.right) {
				domainNeedsDrawing = false;
			}
		}
		float rangeOffset = rangeOriginF;
		if (rangeAxisAlignment == Gravity.TOP) {
			rangeOffset = paddedGridRect.top;

		} else if (domainAxisAlignment == Gravity.BOTTOM) {
			rangeOffset = paddedGridRect.bottom
					- rangeOriginLinePaint.getStrokeWidth();
		} else {
			if (rangeOriginF < paddedGridRect.top
					|| rangeOriginF > paddedGridRect.bottom) {
				rangeNeedsDrawing = false;
			}
		}
		
		
		
		if (domainNeedsDrawing == true) {
			if (domainOriginLinePaint != null) {
				canvas.drawLine(domainOffset, gridRect.top, domainOffset,
						gridRect.bottom, domainOriginLinePaint);
			}

			if (rangeLabelPaint != null) {
				float xPix;
				if (rangeAxisLeft) {
					xPix = domainOffset
							- (rangeLabelTickExtension + rangeLabelHorizontalOffset);
				} else {
					xPix = domainOffset
							+ (rangeLabelTickExtension + rangeLabelHorizontalOffset);
				}

				// draw ticks ABOVE origin:
				{
					int i = 1;
					double yVal;
					float yPix = rangeOriginF - rangeStep.getStepPix();
					for (; yPix >= paddedGridRect.top; yPix = rangeOriginF
							- (i * rangeStep.getStepPix())) {
						yVal = plot.getRangeOrigin().doubleValue() + i
								* rangeStep.getStepVal();
						if (yPix >= paddedGridRect.top
								&& yPix <= paddedGridRect.bottom) {
							if (i % getTicksPerRangeLabel() == 0) {
								drawTickText(canvas, XYAxisType.RANGE,
										yVal, xPix, yPix
												- rangeLabelVerticalOffset,
										rangeLabelPaint);
							}
						}
						i++;
					}
				}

				// draw ticks BENEATH origin:
				{
					int i = 1;
					double yVal;
					float yPix = rangeOriginF + rangeStep.getStepPix();
					for (; yPix <= paddedGridRect.bottom; yPix = rangeOriginF
							+ (i * rangeStep.getStepPix())) {
						yVal = plot.getRangeOrigin().doubleValue() - i
								* rangeStep.getStepVal();
						if (yPix >= paddedGridRect.top
								&& yPix <= paddedGridRect.bottom) {
							if (i % getTicksPerRangeLabel() == 0) {
								drawTickText(canvas, XYAxisType.RANGE,
										yVal, xPix, yPix
												- rangeLabelVerticalOffset,
										rangeLabelPaint);
							}
						}
						i++;
					}
				}
			}
			if (rangeOriginLabelPaint != null && rangeOriginLabel != null) {
				canvas.save();
				float vOffset = rangeOffset - paddedGridRect.height() / 2.0f;
				float hOffset = domainOffset - rangeOriginLabelOffset;
				if (rangeOriginLabelPaint.getTextAlign() == Align.LEFT) {
					vOffset = rangeOffset;
				} else if (rangeOriginLabelPaint.getTextAlign() == Align.RIGHT) {
					vOffset = rangeOffset - paddedGridRect.height();
				}
        		canvas.clipRect(getWidgetDimensions().canvasRect.left, paddedGridRect.top,getWidgetDimensions().canvasRect.right, paddedGridRect.bottom);
				canvas.rotate(rangeOriginLabelAngle, hOffset, vOffset);
				canvas.drawText(rangeOriginLabel, hOffset, vOffset, rangeOriginLabelPaint);
				canvas.restore();
			}
		}

		// draw range origin:
		
		if (rangeNeedsDrawing == true) {
			if (rangeOriginLinePaint != null) {
				canvas.drawLine(gridRect.left, rangeOffset, gridRect.right,
						rangeOffset, rangeOriginLinePaint);
			}

			if (domainLabelPaint != null) {
				float fontHeight = FontUtils.getFontHeight(domainLabelPaint);
                float yPix;
                if (domainAxisBottom){
                    yPix = gridRect.bottom + domainLabelTickExtension
                            + domainLabelVerticalOffset + fontHeight;
                } else {
                    yPix = gridRect.top - domainLabelTickExtension
                            - domainLabelVerticalOffset;
                }
                

             // draw ticks LEFT of origin:
                {
                    int i = 1;
                    double xVal;
                    float xPix = domainOriginF - domainStep.getStepPix();
                    for (; xPix >= paddedGridRect.left; xPix = domainOriginF
                            - (i * domainStep.getStepPix())) {
                        xVal = plot.getDomainOrigin().doubleValue() - i
                                * domainStep.getStepVal();
                        if (xPix >= paddedGridRect.left && xPix <= paddedGridRect.right) {
                            if (i % getTicksPerDomainLabel() == 0) {
                            	drawTickText(canvas, XYAxisType.DOMAIN, xVal, xPix + domainLabelHorizontalOffset, yPix,
                                		domainLabelPaint);
                            }
                        }
                        i++;
                    }
                }

                // draw ticks RIGHT of origin:
                {
                    int i = 1;
                    double xVal;
                    float xPix = domainOriginF + domainStep.getStepPix();
                    for (; xPix <= paddedGridRect.right; xPix = domainOriginF
                            + (i * domainStep.getStepPix())) {
                        xVal = plot.getDomainOrigin().doubleValue() + i
                                * domainStep.getStepVal();
                        if (xPix >= paddedGridRect.left && xPix <= paddedGridRect.right) {

                            if (i % getTicksPerDomainLabel() == 0) {
                            	drawTickText(canvas, XYAxisType.DOMAIN, xVal, xPix + domainLabelHorizontalOffset, yPix,
                                		domainLabelPaint);
                            }
                        }
                        i++;
                    }
                }
			}
			if (domainOriginLabelPaint != null && domainOriginLabel != null) {
				canvas.save();
				float hOffset = domainOffset + paddedGridRect.width() / 2.0f;
				float vOffset = rangeOffset + domainOriginLabelOffset;
				if (domainOriginLabelPaint.getTextAlign() == Align.LEFT) {
					hOffset = domainOffset;
				} else if (domainOriginLabelPaint.getTextAlign() == Align.RIGHT) {
					hOffset = domainOffset + paddedGridRect.height();
				}
        		canvas.clipRect(paddedGridRect.left, getWidgetDimensions().canvasRect.top, paddedGridRect.right, getWidgetDimensions().canvasRect.bottom);
				canvas.rotate(domainOriginLabelAngle, hOffset, vOffset);
				canvas.drawText(domainOriginLabel, hOffset, vOffset, domainOriginLabelPaint);
				canvas.restore();
			}
		}
	}

    protected void drawGrid(Canvas canvas, float domainOriginF, float rangeOriginF, XYStep domainStep, XYStep rangeStep) {

        if (gridBackgroundPaint != null) {
            canvas.drawRect(gridRect, gridBackgroundPaint);
        }

        // draw ticks LEFT of origin:
        {
            int i = 1;
            double xVal;
            float xPix = domainOriginF - domainStep.getStepPix();
            for (; xPix >= paddedGridRect.left; xPix = domainOriginF
                    - (i * domainStep.getStepPix())) {
                xVal = plot.getDomainOrigin().doubleValue() - i
                        * domainStep.getStepVal();
                if (xPix >= paddedGridRect.left && xPix <= paddedGridRect.right) {
                    if (i % getTicksPerDomainLabel() == 0) {
                        drawDomainTick(canvas, xPix, xVal, domainLabelPaint,
                                domainGridLinePaint, false);
                    } else {
                        drawDomainTick(canvas, xPix, xVal, domainLabelPaint,
                                domainSubGridLinePaint, true);
                    }
                }
                i++;
            }
        }
        // draw origin if necessary:
        if (domainAxisAlignment == Gravity.LEFT || domainAxisAlignment == Gravity.RIGHT &&
        		domainOriginF >= paddedGridRect.left && domainOriginF <= paddedGridRect.right) {
			canvas.drawLine(domainOriginF, gridRect.top, domainOriginF,
					gridRect.bottom, domainGridLinePaint);
		}
        // draw ticks RIGHT of origin:
        {
            int i = 1;
            double xVal;
            float xPix = domainOriginF + domainStep.getStepPix();
            for (; xPix <= paddedGridRect.right; xPix = domainOriginF
                    + (i * domainStep.getStepPix())) {
                xVal = plot.getDomainOrigin().doubleValue() + i
                        * domainStep.getStepVal();
                if (xPix >= paddedGridRect.left && xPix <= paddedGridRect.right) {

                    if (i % getTicksPerDomainLabel() == 0) {
                        drawDomainTick(canvas, xPix, xVal, domainLabelPaint,
                                domainGridLinePaint, false);
                    } else {
                        drawDomainTick(canvas, xPix, xVal, domainLabelPaint,
                                domainSubGridLinePaint, true);
                    }
                }
                i++;
            }
        }

        // draw ticks ABOVE origin:
        {
            int i = 1;
            double yVal;
            float yPix = rangeOriginF - rangeStep.getStepPix();
            for (; yPix >= paddedGridRect.top; yPix = rangeOriginF
                    - (i * rangeStep.getStepPix())) {
                yVal = plot.getRangeOrigin().doubleValue() + i
                        * rangeStep.getStepVal();
                if (yPix >= paddedGridRect.top && yPix <= paddedGridRect.bottom) {
                    if (i % getTicksPerRangeLabel() == 0) {
                        drawRangeTick(canvas, yPix, yVal, rangeLabelPaint,
                                rangeGridLinePaint, false);
                    } else {
                        drawRangeTick(canvas, yPix, yVal, rangeLabelPaint,
                                rangeSubGridLinePaint, true);
                    }
                }
                i++;
            }
        }
     // draw origin if necessary:
        if (rangeAxisAlignment == Gravity.TOP || rangeAxisAlignment == Gravity.BOTTOM &&
        		rangeOriginF >= paddedGridRect.top && rangeOriginF <= paddedGridRect.bottom) {
			canvas.drawLine(gridRect.left, rangeOriginF, gridRect.right,
					rangeOriginF, rangeGridLinePaint);
		}
        // draw ticks BENEATH origin:
        {
            int i = 1;
            double yVal;
            float yPix = rangeOriginF + rangeStep.getStepPix();
            for (; yPix <= paddedGridRect.bottom; yPix = rangeOriginF
                    + (i * rangeStep.getStepPix())) {
                yVal = plot.getRangeOrigin().doubleValue() - i
                        * rangeStep.getStepVal();
                if (yPix >= paddedGridRect.top && yPix <= paddedGridRect.bottom) {
                    if (i % getTicksPerRangeLabel() == 0) {
                        drawRangeTick(canvas, yPix, yVal, rangeLabelPaint,
                                rangeGridLinePaint, false);
                    } else {
                        drawRangeTick(canvas, yPix, yVal, rangeLabelPaint,
                                rangeSubGridLinePaint, true);
                    }
                }
                i++;
            }
        }
        
     
    }

    /**
     * Renders the text associated with user defined markers
     * 
     * @param canvas
     * @param text
     * @param marker
     * @param x
     * @param y
     */
    private void drawMarkerText(Canvas canvas, String text, ValueMarker marker,
            float x, float y) {
        x += MARKER_LABEL_SPACING;
        y -= MARKER_LABEL_SPACING;
        RectF textRect = new RectF(FontUtils.getStringDimensions(text,
                marker.getTextPaint()));
        textRect.offsetTo(x, y - textRect.height());

        if (marker instanceof YValueMarker && textRect.right > paddedGridRect.right) {
            textRect.offset(-(textRect.right - paddedGridRect.right), 0);
        }

        if (marker instanceof XValueMarker && textRect.top < paddedGridRect.top) {
            textRect.offset(0, paddedGridRect.top - textRect.top);
        }

        canvas.drawText(text, textRect.left, textRect.bottom,
                marker.getTextPaint());

    }

    protected void drawMarkers(Canvas canvas) {
        for (YValueMarker marker : plot.getYValueMarkers()) {

            if (marker.getValue() != null) {
                double yVal = marker.getValue().doubleValue();
                
                float yPix = ValPixConverter.valToPix(yVal, plot
                        .getCalculatedMinY().doubleValue(), plot
                        .getCalculatedMaxY().doubleValue(), paddedGridRect
                        .height(), true);
                yPix += paddedGridRect.top;
                canvas.drawLine(paddedGridRect.left, yPix,
                        paddedGridRect.right, yPix, marker.getLinePaint());

                float xPix = marker.getTextPosition().getPixelValue(
                        paddedGridRect.width());
                xPix += paddedGridRect.left;
                yPix += marker.vOffset;

                if (marker.getText() != null) {
                    drawMarkerText(canvas, marker.getText(), marker, xPix, yPix);
                } else {
                    drawMarkerText(canvas,
                            getFormattedRangeValue(marker.getValue()), marker,
                            xPix, yPix);
                }
            }
        }

        for (XValueMarker marker : plot.getXValueMarkers()) {
            if (marker.getValue() != null) {
                double xVal = marker.getValue().doubleValue();
                float xPix = ValPixConverter.valToPix(xVal, plot
                        .getCalculatedMinX().doubleValue(), plot
                        .getCalculatedMaxX().doubleValue(), paddedGridRect
                        .width(), false);
                xPix += paddedGridRect.left;
                canvas.drawLine(xPix, paddedGridRect.top, xPix,
                        paddedGridRect.bottom, marker.getLinePaint());

                float yPix = marker.getTextPosition().getPixelValue(
                        paddedGridRect.height());
                yPix += paddedGridRect.top;
                xPix += marker.hOffset;
                if (marker.getText() != null) {
                    drawMarkerText(canvas, marker.getText(), marker, xPix, yPix);
                } else {
                    drawMarkerText(canvas,
                            getFormattedDomainValue(marker.getValue()), marker,
                            xPix, yPix);
                }
            }
        }
    }

    protected void drawCursors(Canvas canvas) {
        boolean hasDomainCursor = false;
        // draw the domain cursor:
        if (domainCursorPaint != null
                && domainCursorPosition <= paddedGridRect.right
                && domainCursorPosition >= paddedGridRect.left) {
            hasDomainCursor = true;
            canvas.drawLine(domainCursorPosition, paddedGridRect.top,
                    domainCursorPosition, paddedGridRect.bottom,
                    domainCursorPaint);
        }

        boolean hasRangeCursor = false;
        // draw the range cursor:
        if (rangeCursorPaint != null
                && rangeCursorPosition >= paddedGridRect.top
                && rangeCursorPosition <= paddedGridRect.bottom) {
            hasRangeCursor = true;
            canvas.drawLine(paddedGridRect.left, rangeCursorPosition,
                    paddedGridRect.right, rangeCursorPosition, rangeCursorPaint);
        }

        if (drawCursorLabelEnabled && cursorLabelPaint != null
                && hasRangeCursor && hasDomainCursor) {

            String label = "X="
                    + getDomainValueFormat().format(getDomainCursorVal());
            label += " Y=" + getRangeValueFormat().format(getRangeCursorVal());

            // convert the label dimensions rect into floating-point:
            RectF cursorRect = new RectF(FontUtils.getPackedStringDimensions(
                    label, cursorLabelPaint));
            cursorRect.offsetTo(domainCursorPosition, rangeCursorPosition
                    - cursorRect.height());

            // if we are too close to the right edge of the plot, we will move
            // the
            // label to the left side of our cursor:
            if (cursorRect.right >= paddedGridRect.right) {
                cursorRect.offsetTo(domainCursorPosition - cursorRect.width(),
                        cursorRect.top);
            }

            // same thing for the top edge of the plot:
            // dunno why but these rects can have negative values for top and
            // bottom.
            if (cursorRect.top <= paddedGridRect.top) {
                cursorRect.offsetTo(cursorRect.left, rangeCursorPosition);
            }

            if (cursorLabelBackgroundPaint != null) {
                canvas.drawRect(cursorRect, cursorLabelBackgroundPaint);
            }

            canvas.drawText(label, cursorRect.left, cursorRect.bottom,
                    cursorLabelPaint);
        }
    }

    /**
     * Draws lines and points for each element in the series.
     * 
     * @param canvas
     * @throws PlotRenderException
     */
    protected void drawData(Canvas canvas) throws PlotRenderException {
        // TODO: iterate through a XYSeriesRenderer list

        // int canvasState = canvas.save();
        try {
            canvas.save(Canvas.ALL_SAVE_FLAG);
            canvas.clipRect(gridRect, android.graphics.Region.Op.INTERSECT);
            for (XYSeriesRenderer renderer : plot.getRendererList()) {
                renderer.render(canvas, paddedGridRect);
            }
            // canvas.restoreToCount(canvasState);
        } finally {
            canvas.restore();
        }
    }

    protected void drawPoint(Canvas canvas, PointF point, Paint paint) {
        canvas.drawPoint(point.x, point.y, paint);
    }

    public float getDomainLabelWidth() {
        return domainLabelWidth;
    }

    public void setDomainLabelWidth(float domainLabelWidth) {
        this.domainLabelWidth = domainLabelWidth;
    }

    public float getRangeLabelWidth() {
        return rangeLabelWidth;
    }

    public void setRangeLabelWidth(float rangeLabelWidth) {
        this.rangeLabelWidth = rangeLabelWidth;
    }

    public float getDomainLabelVerticalOffset() {
        return domainLabelVerticalOffset;
    }

    public void setDomainLabelVerticalOffset(float domainLabelVerticalOffset) {
        this.domainLabelVerticalOffset = domainLabelVerticalOffset;
    }

    public float getDomainLabelHorizontalOffset() {
        return domainLabelHorizontalOffset;
    }

    public void setDomainLabelHorizontalOffset(float domainLabelHorizontalOffset) {
        this.domainLabelHorizontalOffset = domainLabelHorizontalOffset;
    }

    public float getRangeLabelHorizontalOffset() {
        return rangeLabelHorizontalOffset;
    }

    public void setRangeLabelHorizontalOffset(float rangeLabelHorizontalOffset) {
        this.rangeLabelHorizontalOffset = rangeLabelHorizontalOffset;
    }

    public float getRangeLabelVerticalOffset() {
        return rangeLabelVerticalOffset;
    }

    public void setRangeLabelVerticalOffset(float rangeLabelVerticalOffset) {
        this.rangeLabelVerticalOffset = rangeLabelVerticalOffset;
    }

    public Paint getGridBackgroundPaint() {
        return gridBackgroundPaint;
    }

    public void setGridBackgroundPaint(Paint gridBackgroundPaint) {
        this.gridBackgroundPaint = gridBackgroundPaint;
    }

    public Paint getDomainLabelPaint() {
        return domainLabelPaint;
    }

    public void setDomainLabelPaint(Paint domainLabelPaint) {
        this.domainLabelPaint = domainLabelPaint;
    }

    public Paint getRangeLabelPaint() {
        return rangeLabelPaint;
    }

    public void setRangeLabelPaint(Paint rangeLabelPaint) {
        this.rangeLabelPaint = rangeLabelPaint;
    }

    /**
     * Get the paint used to draw the domain grid line.
     */
    public Paint getDomainGridLinePaint() {
        return domainGridLinePaint;
    }

    /**
     * Set the paint used to draw the domain grid line.
     * @param gridLinePaint
     */
    public void setDomainGridLinePaint(Paint gridLinePaint) {
        this.domainGridLinePaint = gridLinePaint;
    }

    /**
     * Get the paint used to draw the range grid line.
     */
    public Paint getRangeGridLinePaint() {
        return rangeGridLinePaint;
    }

    /**
     * Get the paint used to draw the domain grid line.
     */
    public Paint getDomainSubGridLinePaint() {
        return domainSubGridLinePaint;
    }
    
    /**
     * Set the paint used to draw the domain grid line.
     * @param gridLinePaint
     */
    public void setDomainSubGridLinePaint(Paint gridLinePaint) {
        this.domainSubGridLinePaint = gridLinePaint;
    }

    /**
     * Set the Paint used to draw the range grid line.
     * @param gridLinePaint
     */
    public void setRangeGridLinePaint(Paint gridLinePaint) {
        this.rangeGridLinePaint = gridLinePaint;
    }

    /**
     * Get the paint used to draw the range grid line.
     */
    public Paint getRangeSubGridLinePaint() {
        return rangeSubGridLinePaint;
    }

    /**
     * Set the Paint used to draw the range grid line.
     * @param gridLinePaint
     */
    public void setRangeSubGridLinePaint(Paint gridLinePaint) {
        this.rangeSubGridLinePaint = gridLinePaint;
    }
    
    // TODO: make a generic renderer queue.
    

    public void addRangeValueFormat(Number value, String text) {
        rangeTicksValues.put(value.doubleValue(), text);
    }
    public void removeRangeValueFormat(Number value, String text) {
        rangeTicksValues.remove(value.doubleValue());
    }

    public Format getRangeValueFormat() {
        return rangeValueFormat;
    }

    public void setRangeValueFormat(Format rangeValueFormat) {
        this.rangeValueFormat = rangeValueFormat;
    }

    public void addDomainValueFormat(Number value, String text) {
    	domainTicksValues.put(value.doubleValue(), text);
    }
    public void removeDomainValueFormat(Number value, String text) {
        domainTicksValues.remove(value.doubleValue());
    }
    
    public Format getDomainValueFormat() {
        return domainValueFormat;
    }

    public void setDomainValueFormat(Format domainValueFormat) {
        this.domainValueFormat = domainValueFormat;
    }

    public int getDomainLabelTickExtension() {
        return domainLabelTickExtension;
    }

    public void setDomainLabelTickExtension(int domainLabelTickExtension) {
        this.domainLabelTickExtension = domainLabelTickExtension;
    }

    public int getRangeLabelTickExtension() {
        return rangeLabelTickExtension;
    }

    public void setRangeLabelTickExtension(int rangeLabelTickExtension) {
        this.rangeLabelTickExtension = rangeLabelTickExtension;
    }

    public int getTicksPerRangeLabel() {
        return ticksPerRangeLabel;
    }

    public void setTicksPerRangeLabel(int ticksPerRangeLabel) {
        this.ticksPerRangeLabel = ticksPerRangeLabel;
    }

    public int getTicksPerDomainLabel() {
        return ticksPerDomainLabel;
    }

    public void setTicksPerDomainLabel(int ticksPerDomainLabel) {
        this.ticksPerDomainLabel = ticksPerDomainLabel;
    }

    public void setGridPaddingTop(float gridPaddingTop) {
        this.gridPaddingTop = gridPaddingTop;
    }

    public float getGridPaddingBottom() {
        return gridPaddingBottom;
    }

    public void setGridPaddingBottom(float gridPaddingBottom) {
        this.gridPaddingBottom = gridPaddingBottom;
    }

    public float getGridPaddingLeft() {
        return gridPaddingLeft;
    }

    public void setGridPaddingLeft(float gridPaddingLeft) {
        this.gridPaddingLeft = gridPaddingLeft;
    }

    public float getGridPaddingRight() {
        return gridPaddingRight;
    }

    public void setGridPaddingRight(float gridPaddingRight) {
        this.gridPaddingRight = gridPaddingRight;
    }

    public float getGridPaddingTop() {
        return gridPaddingTop;
    }

    public void setGridPadding(float left, float top, float right, float bottom) {
        setGridPaddingLeft(left);
        setGridPaddingTop(top);
        setGridPaddingRight(right);
        setGridPaddingBottom(bottom);
    }

    public Paint getDomainOriginLinePaint() {
        return domainOriginLinePaint;
    }

    public void setDomainOriginLinePaint(Paint domainOriginLinePaint) {
        this.domainOriginLinePaint = domainOriginLinePaint;
    }

    public Paint getRangeOriginLinePaint() {
        return rangeOriginLinePaint;
    }

    public void setRangeOriginLinePaint(Paint rangeOriginLinePaint) {
        this.rangeOriginLinePaint = rangeOriginLinePaint;
    }

    public Paint getRangeOriginLabelPaint() {
        return rangeOriginLabelPaint;
    }
    
    public Paint getDomainOriginLabelPaint() {
        return domainOriginLabelPaint;
    }

    public void setDomainOriginLabelPaint(Paint domainOriginLabelPaint) {
        this.domainOriginLabelPaint = domainOriginLabelPaint;
        updateDomainOriginLabelSize();
   }

    
    private void updateDomainOriginLabelSize() {
    	domainOriginLabelSize = new RectF(FontUtils.getStringDimensions(domainOriginLabel, domainOriginLinePaint));
    }
    
    public void setDomainOriginLabelOffset(float offset) {
        this.domainOriginLabelOffset = offset;
    }

    public float getDomainOriginLabelOffset() {
        return domainOriginLabelOffset;
    }

    public void setDomainOriginLabelAngle(float angle) {
        this.domainOriginLabelAngle = angle;
   }

    public float getDomainOriginLabelAngle() {
        return domainOriginLabelAngle;
    }
    
    public void setDomainOriginLabel(String label) {
        this.domainOriginLabel = label;
        updateDomainOriginLabelSize();
    }

    public String getDomainOriginLabel() {
        return domainOriginLabel;
    }

    private void updateRangeOriginLabelSize() {
    	rangeOriginLabelSize = new RectF(FontUtils.getStringDimensions(rangeOriginLabel, rangeOriginLinePaint));
    }
    
    public void setRangeOriginLabelPaint(Paint rangeOriginLabelPaint) {
        this.rangeOriginLabelPaint = rangeOriginLabelPaint;
        updateRangeOriginLabelSize();
    }

    public void setRangeOriginLabelOffset(float offset) {
        this.rangeOriginLabelOffset = offset;
    }

    public float getRangeOriginLabelOffset() {
        return rangeOriginLabelOffset;
    }

    public void setRangeOriginLabelAngle(float angle) {
        this.rangeOriginLabelAngle = angle;
    }

    public float getRangeOriginLabelAngle() {
        return rangeOriginLabelAngle;
    }
    
    public void setRangeOriginLabel(String label) {
        this.rangeOriginLabel = label;
        updateRangeOriginLabelSize();
    }

    public String getRangeOriginLabel() {
        return rangeOriginLabel;
    }

    public void setCursorPosition(float x, float y) {
        setDomainCursorPosition(x);
        setRangeCursorPosition(y);
    }

    public void setCursorPosition(PointF point) {
        setCursorPosition(point.x, point.y);
    }

    public float getDomainCursorPosition() {
        return domainCursorPosition;
    }

    public Double getDomainCursorVal() {
        return getXVal(getDomainCursorPosition());
    }

    public void setDomainCursorPosition(float domainCursorPosition) {
        this.domainCursorPosition = domainCursorPosition;
    }

    public float getRangeCursorPosition() {
        return rangeCursorPosition;
    }

    public Double getRangeCursorVal() {
        return getYVal(getRangeCursorPosition());
    }

    public void setRangeCursorPosition(float rangeCursorPosition) {
        this.rangeCursorPosition = rangeCursorPosition;
    }

    public Paint getCursorLabelPaint() {
        return cursorLabelPaint;
    }

    public void setCursorLabelPaint(Paint cursorLabelPaint) {
        this.cursorLabelPaint = cursorLabelPaint;
    }

    public Paint getCursorLabelBackgroundPaint() {
        return cursorLabelBackgroundPaint;
    }

    public void setCursorLabelBackgroundPaint(Paint cursorLabelBackgroundPaint) {
        this.cursorLabelBackgroundPaint = cursorLabelBackgroundPaint;
    }

    public boolean isDrawMarkersEnabled() {
        return drawMarkersEnabled;
    }

    public void setDrawMarkersEnabled(boolean drawMarkersEnabled) {
        this.drawMarkersEnabled = drawMarkersEnabled;
    }

    public boolean isRangeAxisLeft() {
        return rangeAxisLeft;
    }

    public void setRangeAxisLeft(boolean rangeAxisLeft) {
        this.rangeAxisLeft = rangeAxisLeft;
    }

    public boolean isDomainAxisBottom() {
        return domainAxisBottom;
    }

    public void setDomainAxisBottom(boolean domainAxisBottom) {
        this.domainAxisBottom = domainAxisBottom;
    }
    
    public void setDomainAxisAlignment( int i) {
        this.domainAxisAlignment = i;
    }
    
    public int getDomainAxisAlignment() {
    	return this.domainAxisAlignment;
    }
    
    public void setRangeAxisAlignment( int position) {
        this.rangeAxisAlignment = position;
    }

    public int getRangeAxisAlignment() {
    	return this.rangeAxisAlignment;
    }
    
    /*
     * set the position of the range axis labels.  Set the labelPaint textSizes before setting this.
     * This call sets the various vertical and horizontal offsets and widths to good defaults.
     * 
     * @param rangeAxisLeft axis labels are on the left hand side not the right hand side.
     * @param rangeAxisOverlay axis labels are overlaid on the plot, not external to it.
     * @param tickSize the size of the tick extensions for none overlaid axis.
     * @param maxLableString Sample label representing the biggest size space needs to be allocated for.
     */
    public void setRangeAxisPosition(boolean rangeAxisLeft, boolean rangeAxisOverlay, int tickSize, String maxLableString){
        setRangeAxisLeft(rangeAxisLeft);
        
        if (rangeAxisOverlay) {
            setRangeLabelWidth(1);    // needs to be at least 1 to display grid line.
            setRangeLabelHorizontalOffset(-2.0f);
            setRangeLabelVerticalOffset(2.0f);    // get above the line
            Paint p = getRangeLabelPaint();
            if (p != null) {
                p.setTextAlign(((rangeAxisLeft)?Paint.Align.LEFT:Paint.Align.RIGHT));
            }
            Paint po = getRangeOriginLabelPaint();
            if (po != null) {
                po.setTextAlign(((rangeAxisLeft)?Paint.Align.LEFT:Paint.Align.RIGHT));
            }
            setRangeLabelTickExtension(0); 
        } else {
            setRangeLabelWidth(1);    // needs to be at least 1 to display grid line.
                                      // if we have a paint this gets bigger.
            setRangeLabelHorizontalOffset(1.0f);
            setRangeLabelTickExtension(tickSize);
            Paint p = getRangeLabelPaint();
            if (p != null) {
                p.setTextAlign(((!rangeAxisLeft)?Paint.Align.LEFT:Paint.Align.RIGHT));
                Rect r = FontUtils.getPackedStringDimensions(maxLableString,p);
                setRangeLabelVerticalOffset(r.top/2);
                setRangeLabelWidth(r.right + getRangeLabelTickExtension());
            }
            Paint po = getRangeOriginLabelPaint();
            if (po != null) {
                po.setTextAlign(((!rangeAxisLeft)?Paint.Align.LEFT:Paint.Align.RIGHT));
            }
        }
    }
    
    /*
     * set the position of the domain axis labels.  Set the labelPaint textSizes before setting this.
     * This call sets the various vertical and horizontal offsets and widths to good defaults.
     * 
     * @param domainAxisBottom axis labels are on the bottom not the top of the plot.
     * @param domainAxisOverlay axis labels are overlaid on the plot, not external to it.
     * @param tickSize the size of the tick extensions for non overlaid axis.
     * @param maxLableString Sample label representing the biggest size space needs to be allocated for.
     */
    public void setDomainAxisPosition(boolean domainAxisBottom, boolean domainAxisOverlay, int tickSize, String maxLabelString){
        setDomainAxisBottom(domainAxisBottom);
        if (domainAxisOverlay) {
            setDomainLabelWidth(1);    // needs to be at least 1 to display grid line.
            setDomainLabelVerticalOffset(2.0f);    // get above the line
            setDomainLabelTickExtension(0);
            Paint p = getDomainLabelPaint();
            if (p != null) {
                Rect r = FontUtils.getPackedStringDimensions(maxLabelString,p);
                if (domainAxisBottom){
                    setDomainLabelVerticalOffset(2 * r.top);
                } else {
                    setDomainLabelVerticalOffset(r.top - 1.0f);
                }
            }
        } else {
            setDomainLabelWidth(1);    // needs to be at least 1 to display grid line.
                                       // if we have a paint this gets bigger.
            setDomainLabelTickExtension(tickSize);
            Paint p = getDomainLabelPaint();
            if (p != null) {
                float fontHeight = FontUtils.getFontHeight(p);
                if (domainAxisBottom){
                    setDomainLabelVerticalOffset(-4.0f);
                } else {
                    setDomainLabelVerticalOffset(+1.0f);
                }
                setDomainLabelWidth(fontHeight + getDomainLabelTickExtension());
            }
        }
    }
}
