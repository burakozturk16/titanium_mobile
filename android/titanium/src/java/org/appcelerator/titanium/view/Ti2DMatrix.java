/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiPoint;
import org.appcelerator.titanium.util.AffineTransform;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.proxy.TiViewProxy;

import android.graphics.Matrix;
import android.view.View;

@Kroll.proxy
public class Ti2DMatrix extends KrollProxy {
	public static final TiPoint DEFAULT_ANCHOR_VALUE = new TiPoint("50%", "50%");
	public static final TiPoint DEFAULT_TRANSLATETO_VALUE = new TiPoint(0, 0);
	public static final float VALUE_UNSPECIFIED = Float.MIN_VALUE;
	

	private static String NUMBER_REGEX_1  = "[0-9]*";
	private static String NUMBER_REGEX_2 = "[-+]?" + NUMBER_REGEX_1;
	private static String NUMBER_REGEX_EXT = "(?:system|px|dp|dip|sp|sip|mm|cm|pt|in|%)?";
	private static String NUMBER_REGEX = NUMBER_REGEX_2 + "\\.?" + NUMBER_REGEX_1 + NUMBER_REGEX_EXT;
	private static String ANCHOR_REGEX = "a(" + NUMBER_REGEX + "\\s*,\\s*" + NUMBER_REGEX + ")";

	private static String REGEX = "(\\.\\.\\.|i|o|(?:a" + NUMBER_REGEX + "\\s*,\\s*" + NUMBER_REGEX + ")?(?:r" + NUMBER_REGEX + "|[st]" + NUMBER_REGEX + "\\s*(?:,\\s*" + NUMBER_REGEX + ")?))";
	
	public ArrayList<Operation> operations = new ArrayList<Operation>();

	public TiPoint anchor = null;
	protected boolean ownFrameCoord = false;

	protected AffineTransform transform = null;

	protected static class Operation {
		protected static final int TYPE_SCALE = 0;
		protected static final int TYPE_TRANSLATE = 1;
		protected static final int TYPE_ROTATE = 2;
		protected static final int TYPE_INVERT = 3;
		protected static final int TYPE_MULTIPLY = 4;

		protected float scaleToX, scaleToY;
		protected float rotateOf;
		protected TiPoint anchor;
		protected TiPoint translateTo;
		protected Ti2DMatrix multiplyWith;
		protected int type;

		public Operation(Ti2DMatrix matrix, int type) {

			if (matrix != null && matrix.anchor != null) {
				this.anchor = matrix.anchor;
			}
			scaleToX = scaleToY = 1;
			translateTo = DEFAULT_TRANSLATETO_VALUE;
			rotateOf = 0;
			this.type = type;
		}
		
		public Operation(String string) {
		    scaleToX = scaleToY = 1;
            translateTo = DEFAULT_TRANSLATETO_VALUE;
            rotateOf = 0;

            Pattern p = Pattern.compile(ANCHOR_REGEX);
            Matcher m = p.matcher(string);
            if (m.find()) {
                String anchorString = m.group(1);
                string = m.replaceAll("");
                this.anchor = new TiPoint(anchorString.split(","));
            }
		    char key = string.charAt(0);
            String theRest = string.substring(1);
            switch(key)
            {
                case 't':
                {
                    String[] values = theRest.split(",");
                    if (values.length > 1) {
                        translateTo = new TiPoint(values);
                    }
                    else {
                        translateTo = new TiPoint(values, values);
                    }
                    type = Operation.TYPE_TRANSLATE;
                    break;
                }
                case 's':
                {
                    String[] values = theRest.split(",");
                    scaleToX = TiConvert.toFloat(values[0]);
                    if (values.length > 1) {
                        scaleToY = TiConvert.toFloat(values[1]);
                    }
                    else {
                        scaleToY = scaleToX;
                    }
                    type = Operation.TYPE_SCALE;
                    break;
                }
                case 'r':
                    rotateOf = TiConvert.toFloat(theRest);
                    type = Operation.TYPE_ROTATE;
                    break;
                case 'i':
                    type = Operation.TYPE_INVERT;
                    break;
            }
		}

		public void apply(int width, int height, int parentWidth, int parentHeight, AffineTransform transform) {
			float anchorX = 0;
			float anchorY = 0;
			if (type == TYPE_SCALE || type == TYPE_ROTATE) {
				TiPoint realAnchor = this.anchor;
				if (realAnchor == null)
					realAnchor = DEFAULT_ANCHOR_VALUE;
				anchorX = realAnchor.getX().getAsPixels(width, height);
				anchorY = realAnchor.getY().getAsPixels(width, height);
				
				anchorX = anchorX - width/2;
				anchorY = anchorY - height/2;
			}
			
			switch (type) {
			case TYPE_SCALE:
				transform.scale(scaleToX, scaleToY, anchorX, anchorY);
			case TYPE_TRANSLATE:
				
				float translateToX = translateTo.getX().getAsPixels(parentWidth, parentHeight);
				float translateToY = translateTo.getY().getAsPixels(parentWidth, parentHeight);
				transform.translate(translateToX, translateToY);
				break;
			case TYPE_ROTATE:
				transform.rotate(rotateOf, anchorX, anchorY);
				break;
			case TYPE_MULTIPLY:
				transform.multiply(multiplyWith.getAffineTransform(width, height, parentWidth, parentHeight));
				break;
			case TYPE_INVERT:
				transform.inverse();
				break;
			}
		}
		
		public String toString()
	    {
	        String result = "";
	        if (anchor != null) {
	            result = String.format("a%s,%s", anchor.getXString(), anchor.getYString());
	        }
	        switch (type) {
            case TYPE_SCALE:
                if (scaleToX == scaleToY) {
                    result += String.format("s%s", Double.valueOf(scaleToX).toString());
                }
                else {
                    result += String.format("s%s,%s", Double.valueOf(scaleToX).toString()
                            , Double.valueOf(scaleToY).toString());
                }
            case TYPE_TRANSLATE:
                result += String.format("t%s,%s", translateTo.getXString(), translateTo.getYString());
                break;
            case TYPE_ROTATE:
                result += String.format("r%s", Double.valueOf(rotateOf).toString());
                break;
            case TYPE_MULTIPLY:
                result = multiplyWith.toString();
                break;
            case TYPE_INVERT:
                result = "i";
                break;
            }
	        return result;
	    }
	}

	// protected Operation op;

	public Ti2DMatrix() {
	}

	public Ti2DMatrix(AffineTransform transform) {
		this.transform = transform;
	}
	
	public Ti2DMatrix(String string) {
	    Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(string);
        while (m.find()) {
            String group = m.group(1);
            if (group.equals("o")) {
                ownFrameCoord = true;
            }
            else {
                operations.add(new Operation(group));
            }
        }
    }

	public Ti2DMatrix(Ti2DMatrix prev) {
		if (prev != null) {
			handleAnchorPoint(prev.getProperties());
			ownFrameCoord = prev.ownFrameCoord;
			operations.addAll(prev.operations);
		}
	}
	
	public Ti2DMatrix(HashMap map) {
	    handleCreationDict(TiConvert.toKrollDict(map));
    }
	
	public Ti2DMatrix reuseForNewMatrix(HashMap map) {
	    operations.clear();
	    anchor = null;
	    ownFrameCoord = false;
	    transform = null;
	    
        handleCreationDict(TiConvert.toKrollDict(map));
        return this;
	}
	
	public Ti2DMatrix reuseForNewMatrix(String string) {
        operations.clear();
        anchor = null;
        ownFrameCoord = false;
        transform = null;
        
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(string);
        while (m.find()) {
            String group = m.group(1);
            if (group.equals("o")) {
                ownFrameCoord = true;
            }
            else {
                operations.add(new Operation(group));
            }
        }
        return this;
    }


	@Override
	public void handleCreationDict(KrollDict dict) {
		super.handleCreationDict(dict);
		handleAnchorPoint(dict);
		if (dict.containsKey(TiC.PROPERTY_ROTATE)) {

			Operation op = new Operation(this, Operation.TYPE_ROTATE);
			op.rotateOf = TiConvert.toFloat(dict, TiC.PROPERTY_ROTATE);
			operations.add(op);
			// If scale also specified in creation dict,
			// then we need to link a scaling matrix separately.
			if (dict.containsKey(TiC.PROPERTY_SCALE)) {
				KrollDict newDict = new KrollDict();
				newDict.put(TiC.PROPERTY_SCALE, dict.get(TiC.PROPERTY_SCALE));
				if (dict.containsKey(TiC.PROPERTY_ANCHOR_POINT)) {
					newDict.put(TiC.PROPERTY_ANCHOR_POINT,
							dict.get(TiC.PROPERTY_ANCHOR_POINT));
				}
				Operation scaleOp = new Operation(this, Operation.TYPE_SCALE);
				scaleOp.scaleToX = op.scaleToY = TiConvert.toFloat(dict,
						TiC.PROPERTY_SCALE);
				operations.add(0, op);
			}

		} else if (dict.containsKey(TiC.PROPERTY_SCALE)) {
			Operation op = new Operation(this, Operation.TYPE_SCALE);
			op.scaleToX = op.scaleToY = TiConvert.toFloat(dict,
					TiC.PROPERTY_SCALE);
			operations.add(op);
		}
		
		if (dict.containsKey(TiC.PROPERTY_OWN_FRAME_COORD)) {
			ownFrameCoord = dict.optBoolean(TiC.PROPERTY_OWN_FRAME_COORD, ownFrameCoord);
		}
	}

	protected void handleAnchorPoint(KrollDict dict) {
		if (dict.containsKey(TiC.PROPERTY_ANCHOR_POINT)) {
			KrollDict anchorPoint = dict
					.getKrollDict(TiC.PROPERTY_ANCHOR_POINT);
			setProperty(TiC.PROPERTY_ANCHOR_POINT, anchorPoint);
			anchor = TiConvert.toPoint(anchorPoint);
		}
	}

	@Kroll.method
	public Ti2DMatrix translate(Object args[]) {
		Ti2DMatrix newMatrix = new Ti2DMatrix(this);
		if (args.length == 2) {
			Operation op = new Operation(newMatrix, Operation.TYPE_TRANSLATE);
			op.translateTo = new TiPoint(TiConvert.toString(args[0]),
					TiConvert.toString(args[1]));
			newMatrix.operations.add(op);
		}
		return newMatrix;
	}

	@Kroll.method
	public Ti2DMatrix scale(Object args[]) {
		Ti2DMatrix newMatrix = new Ti2DMatrix(this);
		Operation op = new Operation(newMatrix, Operation.TYPE_SCALE);
		if (args.length == 2) {
			op.scaleToX = TiConvert.toFloat(args[0]);
			op.scaleToY = TiConvert.toFloat(args[1]);
		} else if (args.length == 1) {
			op.scaleToX = op.scaleToY = TiConvert.toFloat(args[0]);
		}
		newMatrix.operations.add(op);
		return newMatrix;
	}

	@Kroll.method
	public Ti2DMatrix rotate(Object[] args) {
		Ti2DMatrix newMatrix = new Ti2DMatrix(this);

		if (args.length >= 1) {
			Operation op = new Operation(newMatrix, Operation.TYPE_ROTATE);
			op.rotateOf = TiConvert.toFloat(args[0]);
			newMatrix.operations.add(op);
		}
		return newMatrix;
	}

	@Kroll.method
	public Ti2DMatrix invert() {
		Ti2DMatrix newMatrix = new Ti2DMatrix(this);
		Operation op = new Operation(newMatrix, Operation.TYPE_INVERT);
		newMatrix.operations.add(op);
		return newMatrix;
	}

	@Kroll.method
	public Ti2DMatrix multiply(Ti2DMatrix other) {
		Ti2DMatrix newMatrix = new Ti2DMatrix(this);
		Operation op = new Operation(newMatrix, Operation.TYPE_MULTIPLY);
		op.multiplyWith = other;
		newMatrix.operations.add(op);
		return newMatrix;
	}
	
	@Kroll.method @Kroll.setProperty
	public void setAnchorPoint(KrollDict dict) {
		setProperty(TiC.PROPERTY_ANCHOR_POINT, dict);
		anchor = TiConvert.toPoint(dict);
	}

	@Kroll.method
	public float[] finalValuesAfterInterpolation(TiViewProxy proxy) {
		View view = proxy.getOuterView();
		if (view != null) {
			float[] result = new float[9];
			Matrix m = getMatrix(proxy);
			m.getValues(result);
			return result;
		}
		return null;
	}

	public Matrix getMatrix(TiViewProxy proxy) {
		return getMatrix(proxy.getOuterView());
	}
	
	public AffineTransform getAffineTransform(int width, int height, int parentWidth, int parentHeight) {
		if (transform != null) return transform;
		if (ownFrameCoord) {
			parentWidth = width;
			parentHeight = height;
		}
		AffineTransform result = new AffineTransform();
		if (width == 0 || height == 0 || parentWidth == 0 || parentHeight == 0 ) return result;
		for (Operation op : operations) {
			if (op != null) {
				op.apply(width, height, parentWidth, parentHeight, result);
			}
		}
		return result;
	}
	
	public AffineTransform getAffineTransform() {
		return transform;
	}
	
	public AffineTransform getAffineTransform(View view) {
		View parent = (View) view.getParent();
		if (parent == null)
			parent = view;
//		ViewHelper.setPivotX(view, (float) 0.5);
//		ViewHelper.setPivotY(view, (float) 0.5);
		return getAffineTransform(view.getMeasuredWidth(), view.getMeasuredHeight(),
				parent.getMeasuredWidth(), parent.getMeasuredHeight());
	}

	public Matrix getMatrix(View view) {
		View parent = (View) view.getParent();
		if (parent == null)
			parent = view;
		return getMatrix(view.getMeasuredWidth(), view.getMeasuredHeight(),
				parent.getMeasuredWidth(), parent.getMeasuredHeight());
	}
	
	public Matrix getMatrix(int width, int height, int parentWidth, int parentHeight) {
		AffineTransform transform = getAffineTransform(width, height, parentWidth, parentHeight);
		return (transform != null) ? transform.toMatrix() : null;
	}
	
	public String toString()
	{
	    String result = ownFrameCoord?"o":"";
	    for (Operation op : operations) {
            if (op != null) {
                result += op.toString();
            }
        }
	    return result;
	}
}
