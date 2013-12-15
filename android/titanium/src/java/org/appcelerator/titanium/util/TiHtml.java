package org.appcelerator.titanium.util;

import org.appcelerator.titanium.TiDimension;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.ReplacementSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;

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
			Rect rect = new Rect((int)(x + dx), (int)(top + dx + 2), (int)(x + measureText(paint, text, start, end) - strokeWidth/2), (int)(bottom - strokeWidth/2));
		    this.mDrawable.setBounds(rect);
		    canvas.save();
//	        
//	        int transY = bottom - this.mDrawable.getBounds().bottom;
//	        if (mVerticalAlignment == ALIGN_BASELINE) {
//	            transY -= paint.getFontMetricsInt().descent;
//	        }
//
//	        canvas.translate(x, transY);
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
	
	// the formatting rules, implemented in a breadth-first DOM traverse
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static class FormattingVisitor implements NodeVisitor {
        private SpannableStringBuilder mSpannableStringBuilder = new SpannableStringBuilder();
        private Html.ImageGetter mImageGetter;
    
        private static final float[] HEADER_SIZES = {
            1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f,
        };
        
        private static void handleP(SpannableStringBuilder text) {
            int len = text.length();

            if (len >= 1 && text.charAt(len - 1) == '\n') {
                if (len >= 2 && text.charAt(len - 2) == '\n') {
                    return;
                }

                text.append("\n");
                return;
            }

            if (len != 0) {
                text.append("\n\n");
            }
        }

        private static void handleBr(SpannableStringBuilder text) {
            text.append("\n");
        }

        private static Object getLast(Spanned text, Class kind) {
            /*
             * This knows that the last returned object from getSpans()
             * will be the most recently added.
             */
			Object[] objs = text.getSpans(0, text.length(), kind);

            if (objs.length == 0) {
                return null;
            } else {
                return objs[objs.length - 1];
            }
        }

        private static void start(SpannableStringBuilder text, Object mark) {
            int len = text.length();
            text.setSpan(mark, len, len, Spannable.SPAN_MARK_MARK);
        }

        private static void end(SpannableStringBuilder text, Class kind,
                                Object repl) {
            int len = text.length();
            Object obj = getLast(text, kind);
            int where = text.getSpanStart(obj);

            text.removeSpan(obj);

            if (where != len) {
                text.setSpan(repl, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        private static void startImg(SpannableStringBuilder text,
                                     Attributes attributes, Html.ImageGetter img) {
            String src = attributes.get("src");
            Drawable d = null;

            if (img != null) {
                d = img.getDrawable(src);
            }

//            if (d == null) {
//                d = Resources.getSystem().
//                        getDrawable(com.android.internal.R.drawable.unknown_image);
//                d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
//            }

            int len = text.length();
            text.append("\uFFFC");

            text.setSpan(new ImageSpan(d, src), len, text.length(),
                         Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        private static void startFont(SpannableStringBuilder text,
                                      Attributes attributes) {
            String color = attributes.get("color");
            String face = attributes.get("face");

            int len = text.length();
            text.setSpan(new Font(color, face), len, len, Spannable.SPAN_MARK_MARK);
        }

        private static void endFont(SpannableStringBuilder text) {
            int len = text.length();
            Object obj = getLast(text, Font.class);
            int where = text.getSpanStart(obj);

            text.removeSpan(obj);

            if (where != len) {
                Font f = (Font) obj;

                if (!TextUtils.isEmpty(f.mColor)) {
                    if (f.mColor.startsWith("@")) {
                        Resources res = Resources.getSystem();
                        String name = f.mColor.substring(1);
                        int colorRes = res.getIdentifier(name, "color", "android");
                        if (colorRes != 0) {
                            ColorStateList colors = res.getColorStateList(colorRes);
                            text.setSpan(new TextAppearanceSpan(null, 0, 0, colors, null),
                                    where, len,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    } else {
                        int c = TiColorHelper.parseColor(f.mColor);
                        if (c != -1) {
                            text.setSpan(new ForegroundColorSpan(c | 0xFF000000),
                                    where, len,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }

                if (f.mFace != null) {
                    text.setSpan(new TiTypefaceSpan(f.mFace), where, len,
                                 Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        private static void startA(SpannableStringBuilder text, Attributes attributes) {
            String href = attributes.get("href");

            int len = text.length();
            text.setSpan(new Href(href), len, len, Spannable.SPAN_MARK_MARK);
        }

        private static void endA(SpannableStringBuilder text) {
            int len = text.length();
            Object obj = getLast(text, Href.class);
            int where = text.getSpanStart(obj);

            text.removeSpan(obj);

            if (where != len) {
                Href h = (Href) obj;

                if (h.mHref != null) {
                    text.setSpan(new URLSpan(h.mHref), where, len,
                                 Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        private static void endHeader(SpannableStringBuilder text) {
            int len = text.length();
            Object obj = getLast(text, Header.class);

            int where = text.getSpanStart(obj);

            text.removeSpan(obj);

            // Back off not to change only the text, not the blank line.
            while (len > where && text.charAt(len - 1) == '\n') {
                len--;
            }

            if (where != len) {
                Header h = (Header) obj;

                text.setSpan(new RelativeSizeSpan(HEADER_SIZES[h.mLevel]),
                             where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                text.setSpan(new StyleSpan(Typeface.BOLD),
                             where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        
        private static class Bold { }
        private static class Italic { }
        private static class Underline { }
        private static class Big { }
        private static class Small { }
        private static class Monospace { }
        private static class Blockquote { }
        private static class Super { }
        private static class Sub { }
        private static class Span { }
        private static class Div { }

        private static class Font {
            public String mColor;
            public String mFace;

            public Font(String color, String face) {
                mColor = color;
                mFace = face;
            }
        }

        private static class Href {
            public String mHref;

            public Href(String href) {
                mHref = href;
            }
        }

        private static class Header {
            private int mLevel;

            public Header(int level) {
                mLevel = level;
            }
        }
        
        private void addText(String characters) {
//            StringBuilder sb = new StringBuilder();
//            /*
//             * Ignore whitespace that immediately follows other whitespace;
//             * newlines count as spaces.
//             */
//
//            for (int i = 0; i < characters.length(); i++) {
//                char c = characters.charAt(i);
//
//                if (c == ' ' || c == '\n') {
//                    char pred;
//                    int len = sb.length();
//
//                    if (len == 0) {
//                        len = mSpannableStringBuilder.length();
//
//                        if (len == 0) {
//                            pred = '\n';
//                        } else {
//                            pred = mSpannableStringBuilder.charAt(len - 1);
//                        }
//                    } else {
//                        pred = sb.charAt(len - 1);
//                    }
//
//                    if (pred != ' ' && pred != '\n') {
//                        sb.append(' ');
//                    }
//                } else {
//                    sb.append(c);
//                }
//            }

            mSpannableStringBuilder.append(characters);
        }

        // hit when the node is first seen
        public void head(Node node, int depth) {
            String tag = node.nodeName();
            if (node instanceof TextNode) {
                mSpannableStringBuilder.append(((TextNode) node).text());
//           	addText(((TextNode) node).getWholeText()); // TextNodes carry all user-readable text in the DOM.
                return;
        	}	
            Attributes attributes = node.attributes();
            if (tag.equalsIgnoreCase("br")) {
                // We don't need to handle this. TagSoup will ensure that there's a </br> for each <br>
                // so we can safely emite the linebreaks when we handle the close tag.
            } else if (tag.equalsIgnoreCase("p")) {
                handleP(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("div")) {
                handleP(mSpannableStringBuilder);
                start(mSpannableStringBuilder, new Div());
            } else if (tag.equalsIgnoreCase("strong")) {
                start(mSpannableStringBuilder, new Bold());
            } else if (tag.equalsIgnoreCase("b")) {
                start(mSpannableStringBuilder, new Bold());
            } else if (tag.equalsIgnoreCase("em")) {
                start(mSpannableStringBuilder, new Italic());
            } else if (tag.equalsIgnoreCase("cite")) {
                start(mSpannableStringBuilder, new Italic());
            } else if (tag.equalsIgnoreCase("dfn")) {
                start(mSpannableStringBuilder, new Italic());
            } else if (tag.equalsIgnoreCase("i")) {
                start(mSpannableStringBuilder, new Italic());
            } else if (tag.equalsIgnoreCase("big")) {
                start(mSpannableStringBuilder, new Big());
            } else if (tag.equalsIgnoreCase("small")) {
                start(mSpannableStringBuilder, new Small());
            } else if (tag.equalsIgnoreCase("font")) {
                startFont(mSpannableStringBuilder, attributes);
            } else if (tag.equalsIgnoreCase("blockquote")) {
                handleP(mSpannableStringBuilder);
                start(mSpannableStringBuilder, new Blockquote());
            } else if (tag.equalsIgnoreCase("tt")) {
                start(mSpannableStringBuilder, new Monospace());
            } else if (tag.equalsIgnoreCase("a")) {
                startA(mSpannableStringBuilder, attributes);
            } else if (tag.equalsIgnoreCase("u")) {
                start(mSpannableStringBuilder, new Underline());
            } else if (tag.equalsIgnoreCase("sup")) {
                start(mSpannableStringBuilder, new Super());
            } else if (tag.equalsIgnoreCase("sub")) {
                start(mSpannableStringBuilder, new Sub());
            } else if (tag.equalsIgnoreCase("span")) {
                start(mSpannableStringBuilder, new Span());
            } else if (tag.length() == 2 &&
                       Character.toLowerCase(tag.charAt(0)) == 'h' &&
                       tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
                handleP(mSpannableStringBuilder);
                start(mSpannableStringBuilder, new Header(tag.charAt(1) - '1'));
            } else if (tag.equalsIgnoreCase("img")) {
                startImg(mSpannableStringBuilder, attributes, mImageGetter);
            }
        }
        
        private CharacterStyle getStyleSpan(Attributes attr) {
			if (attr.hasKey("style")) {
				String[] items = attr.get("style").toLowerCase().trim().split(";");
				int fillColor = Color.TRANSPARENT;
				int strokeColor = Color.TRANSPARENT;
				int strokeWidth = 1;
				int radius = 0;
				boolean needsBgdSpan = false;
				for (String item : items) {
					String[] values = item.split(":");
					String key = values[0];
					String value = values[1];
					if (key.equals("background-color")) {
						fillColor = TiColorHelper.parseColor(value);
						needsBgdSpan = true;
					} else if (key.equals("border-color")) {
						strokeColor = TiColorHelper.parseColor(value);
						needsBgdSpan = true;
					} else if (key.equals("border-width")) {
						strokeWidth = TiConvert.toTiDimension(value, TiDimension.TYPE_HEIGHT).getAsPixels();
						needsBgdSpan = true;
					} else if (key.equals("border-radius")) {
						radius = TiConvert.toTiDimension(value, TiDimension.TYPE_HEIGHT).getAsPixels();
						needsBgdSpan = true;
					}
				}
				if (needsBgdSpan)
					return new CustomBackgroundSpan(radius, fillColor, strokeColor, strokeWidth);
			}
			// if (attr.get(key))
			return null;
		}

        // hit when all of the node's children (if any) have been visited
        public void tail(Node node, int depth) {
            String tag = node.nodeName();
            Attributes attributes = node.attributes();
          if (tag.equalsIgnoreCase("br")) {
                handleBr(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("p")) {
                handleP(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("div")) {
                handleP(mSpannableStringBuilder);
                end(mSpannableStringBuilder, Div.class, getStyleSpan(attributes));
            } else if (tag.equalsIgnoreCase("strong")) {
                end(mSpannableStringBuilder, Bold.class, new StyleSpan(Typeface.BOLD));
            } else if (tag.equalsIgnoreCase("b")) {
                end(mSpannableStringBuilder, Bold.class, new StyleSpan(Typeface.BOLD));
            } else if (tag.equalsIgnoreCase("em")) {
                end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("cite")) {
                end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("dfn")) {
                end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("i")) {
                end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
            } else if (tag.equalsIgnoreCase("big")) {
                end(mSpannableStringBuilder, Big.class, new RelativeSizeSpan(1.25f));
            } else if (tag.equalsIgnoreCase("small")) {
                end(mSpannableStringBuilder, Small.class, new RelativeSizeSpan(0.8f));
            } else if (tag.equalsIgnoreCase("font")) {
                endFont(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("blockquote")) {
                handleP(mSpannableStringBuilder);
                end(mSpannableStringBuilder, Blockquote.class, new QuoteSpan());
            } else if (tag.equalsIgnoreCase("tt")) {
                end(mSpannableStringBuilder, Monospace.class,
                        new TypefaceSpan("monospace"));
            } else if (tag.equalsIgnoreCase("a")) {
                endA(mSpannableStringBuilder);
            } else if (tag.equalsIgnoreCase("u")) {
                end(mSpannableStringBuilder, Underline.class, new UnderlineSpan());
            } else if (tag.equalsIgnoreCase("sup")) {
                end(mSpannableStringBuilder, Super.class, new SuperscriptSpan());
            } else if (tag.equalsIgnoreCase("sub")) {
                end(mSpannableStringBuilder, Sub.class, new SubscriptSpan());
            } else if (tag.equalsIgnoreCase("span")) {
                end(mSpannableStringBuilder, Span.class, getStyleSpan(attributes));
            } else if (tag.length() == 2 &&
                    Character.toLowerCase(tag.charAt(0)) == 'h' &&
                    tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
                handleP(mSpannableStringBuilder);
                endHeader(mSpannableStringBuilder);
            }
        }
        
        public Spanned spannable(){
        	 // Fix flags and range for paragraph-type markup.
            Object[] obj = mSpannableStringBuilder.getSpans(0, mSpannableStringBuilder.length(), ParagraphStyle.class);
            for (int i = 0; i < obj.length; i++) {
                int start = mSpannableStringBuilder.getSpanStart(obj[i]);
                int end = mSpannableStringBuilder.getSpanEnd(obj[i]);

                // If the last line of the range is blank, back off by one.
                if (end - 2 >= 0) {
                    if (mSpannableStringBuilder.charAt(end - 1) == '\n' &&
                        mSpannableStringBuilder.charAt(end - 2) == '\n') {
                        end--;
                    }
                }

                if (end == start) {
                    mSpannableStringBuilder.removeSpan(obj[i]);
                } else {
                    mSpannableStringBuilder.setSpan(obj[i], start, end, Spannable.SPAN_PARAGRAPH);
                }
            }

            return mSpannableStringBuilder;
        }
    }

	public static Spanned fromHtml(String html) {
		Document doc = Jsoup.parse(html);
		FormattingVisitor formatter = new FormattingVisitor();
        NodeTraversor traversor = new NodeTraversor(formatter);
        traversor.traverse(doc); // walk the DOM, and call .head() and .tail() for each node
        return formatter.spannable();
	}
}
