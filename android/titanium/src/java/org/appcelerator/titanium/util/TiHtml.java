package org.appcelerator.titanium.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.NodeTraversor;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ReplacementSpan;
import android.text.style.URLSpan;

public class TiHtml {
	private static final String TAG = "TiHtml";

	public static class RoundedRectDrawable extends ShapeDrawable {
	    private final Paint fillpaint, strokepaint;
	    public RoundedRectDrawable(int radius, int fillColor, int strokeColor, int strokeWidth) {
	        super(new RoundRectShape(new float[] { radius, radius, radius, radius, radius, radius, radius, radius }, 
	        		null, null));
	        fillpaint = new Paint(this.getPaint());
	        fillpaint.setColor(fillColor);
	        strokepaint = new Paint(fillpaint);
	        strokepaint.setStyle(Paint.Style.STROKE);
	        strokepaint.setStrokeWidth(strokeWidth);
	        strokepaint.setColor(strokeColor);
	    }
	 
	    @Override
	    protected void onDraw(Shape shape, Canvas canvas, Paint paint) {
	        shape.draw(canvas, fillpaint);
	        shape.draw(canvas, strokepaint);
	    }
	}
	
	public static class URLSpanNoUnderline extends URLSpan {
        public URLSpanNoUnderline(String url) {
            super(url);
        }
        @Override 
        public void updateDrawState(TextPaint ds) {
//            super.updateDrawState(ds);
//            ds.setUnderlineText(false);
        }
    }
	
	public static class CustomBackgroundSpan extends ReplacementSpan {
		private RoundedRectDrawable mDrawable;
		
		int radius;
		int fillColor;
		int strokeColor;
		int strokeWidth;
		
		public CustomBackgroundSpan(int radius, int fillColor, int strokeColor, int strokeWidth) {
		    this.mDrawable = new RoundedRectDrawable(radius, fillColor, strokeColor, strokeWidth);
		    this.radius = radius;
		    this.fillColor = fillColor;
		    this.strokeColor = strokeColor;
		    this.strokeWidth = strokeWidth;		    
		}
		
		public CustomBackgroundSpan(CustomBackgroundSpan toCopy) {
			this(toCopy.radius, toCopy.fillColor, toCopy.strokeColor, toCopy.strokeWidth);	    
		}
		
		@Override
		public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
		    return measureText(paint, text, start, end);

		}
		
		private int measureText(Paint paint, CharSequence text, int start, int end) {
		    return Math.round(paint.measureText(text, start, end));
		}
		
		@Override
		public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
		    float dx = strokeWidth / 2;
			Rect rect = new Rect((int)(x + dx), (int)(top + dx), (int)(x + measureText(paint, text, start, end) - strokeWidth/2), (int)(bottom - strokeWidth/2));
		    this.mDrawable.setBounds(rect);
		    canvas.save();
		    this.mDrawable.draw(canvas);
	        canvas.restore();
	        canvas.drawText(text, start, end, x, y, paint);
		}

	}
	
//	private static class CustomLineHeightSpan implements LineHeightSpan {
//        private final TiDimension height;
//
//        CustomLineHeightSpan(int height) {
//            this.height = height;
//        }
//
//        @Override
//        public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v,
//                FontMetricsInt fm) {
//            fm.bottom += height;
//            fm.descent += height;
//        }
//
//    }
	
	public static Spanned fromHtml(String html, final boolean disableLinkStyle) {
		Document doc = Jsoup.parse(html);
		TiHTMLFormattingVisitor formatter = new TiHTMLFormattingVisitor(disableLinkStyle);
        NodeTraversor traversor = new NodeTraversor(formatter);
        traversor.traverse(doc); // walk the DOM, and call .head() and .tail() for each node
        return formatter.spannable();
	}
}
