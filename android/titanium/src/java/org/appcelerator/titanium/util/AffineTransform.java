package org.appcelerator.titanium.util;

import java.math.BigDecimal;

import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;


public class AffineTransform implements Cloneable {
	private double[] m_transform = new double[6];

public AffineTransform()
{
    setMatrix(1, 0, 0, 1, 0, 0);
}

public AffineTransform(double a, double b, double c, double d, double e, double f)
{
    setMatrix(a, b, c, d, e, f);
}

public AffineTransform(AffineTransform transform)
{
    setMatrix(transform.m_transform);
}

public AffineTransform(DecomposedType decomp)
{
    recompose(decomp);
}

public AffineTransform(Matrix matrix)
{
    setMatrix(matrix);
}


void makeIdentity()
{
    setMatrix(1, 0, 0, 1, 0, 0);
}

void setMatrix(double a, double b, double c, double d, double e, double f)
{
    m_transform[0] = a;
    m_transform[1] = b;
    m_transform[2] = c;
    m_transform[3] = d;
    m_transform[4] = e;
    m_transform[5] = f;
}

public boolean isIdentity()
{
    return (m_transform[0] == 1 && m_transform[1] == 0
         && m_transform[2] == 0 && m_transform[3] == 1
         && m_transform[4] == 0 && m_transform[5] == 0);
}

public double a() { return m_transform[0]; }
void setA(double a) { m_transform[0] = a; }
public double b() { return m_transform[1]; }
void setB(double b) { m_transform[1] = b; }
public double c() { return m_transform[2]; }
void setC(double c) { m_transform[2] = c; }
public double d() { return m_transform[3]; }
void setD(double d) { m_transform[3] = d; }
public double e() { return m_transform[4]; }
void setE(double e) { m_transform[4] = e; }
public double f() { return m_transform[5]; }
void setF(double f) { m_transform[5] = f; }

private boolean isIdentityOrTranslation()
{
    return m_transform[0] == 1 && m_transform[1] == 0 && m_transform[2] == 0 && m_transform[3] == 1;
}

private boolean isIdentityOrTranslationOrFlipped()
{
    return m_transform[0] == 1 && m_transform[1] == 0 && m_transform[2] == 0 && (m_transform[3] == 1 || m_transform[3] == -1);
}

private boolean preservesAxisAlignment()
{
    return (m_transform[1] == 0 && m_transform[2] == 0) || (m_transform[0] == 0 && m_transform[3] == 0);
}

public boolean equals( AffineTransform m2)
{
    return (m_transform[0] == m2.m_transform[0]
         && m_transform[1] == m2.m_transform[1]
         && m_transform[2] == m2.m_transform[2]
         && m_transform[3] == m2.m_transform[3]
         && m_transform[4] == m2.m_transform[4]
         && m_transform[5] == m2.m_transform[5]);
}

public double xScale()
{
    return Math.sqrt(m_transform[0] * m_transform[0] + m_transform[1] * m_transform[1]);
}

public double yScale()
{
    return Math.sqrt(m_transform[2] * m_transform[2] + m_transform[3] * m_transform[3]);
}

public double det()
{
    return m_transform[0] * m_transform[3] - m_transform[1] * m_transform[2];
}

public boolean isInvertible()
{
    return det() != 0.0;
}

public static AffineTransform translation(double x, double y)
{
    return new AffineTransform(1, 0, 0, 1, x, y);
}

void setMatrix(Matrix matrix)
{
    if (matrix != null) {
    	float[] values = new float[9];
		matrix.getValues(values);
		m_transform[0] = values[0];
		m_transform[2] = values[1];
		m_transform[4] = values[2];
		m_transform[1] = values[3];
		m_transform[3] = values[4];
		m_transform[5] = values[5];
    }
}

void setMatrix(double[] m)
{
    if (m!= null && m.length == 6) {
    	m_transform[0] = m[0];
    	m_transform[1] = m[1];
    	m_transform[2] = m[2];
    	m_transform[3] = m[3];
    	m_transform[4] = m[4];
    	m_transform[5] = m[5];
    }
}

public AffineTransform inverse()
{
    AffineTransform result = new AffineTransform();
    
    double determinant = det();
    if (determinant == 0.0)
        return result;
    
    if (isIdentityOrTranslation()) {
        result.m_transform[4] = -m_transform[4];
        result.m_transform[5] = -m_transform[5];
        return result;
    }

    result.m_transform[0] = m_transform[3] / determinant;
    result.m_transform[1] = -m_transform[1] / determinant;
    result.m_transform[2] = -m_transform[2] / determinant;
    result.m_transform[3] = m_transform[0] / determinant;
    result.m_transform[4] = (m_transform[2] * m_transform[5]
                           - m_transform[3] * m_transform[4]) / determinant;
    result.m_transform[5] = (m_transform[1] * m_transform[4]
                           - m_transform[0] * m_transform[5]) / determinant;

    setMatrix(result.m_transform);
    return this;
}


// Multiplies this AffineTransform by the provided AffineTransform - i.e.
// this = this * other;
public AffineTransform multiply(AffineTransform other)
{
    AffineTransform trans = new AffineTransform();
    
    trans.m_transform[0] = other.m_transform[0] * m_transform[0] + other.m_transform[1] * m_transform[2];
    trans.m_transform[1] = other.m_transform[0] * m_transform[1] + other.m_transform[1] * m_transform[3];
    trans.m_transform[2] = other.m_transform[2] * m_transform[0] + other.m_transform[3] * m_transform[2];
    trans.m_transform[3] = other.m_transform[2] * m_transform[1] + other.m_transform[3] * m_transform[3];
    trans.m_transform[4] = other.m_transform[4] * m_transform[0] + other.m_transform[5] * m_transform[2] + m_transform[4];
    trans.m_transform[5] = other.m_transform[4] * m_transform[1] + other.m_transform[5] * m_transform[3] + m_transform[5];

    setMatrix(trans.m_transform);
    return this;
}

public AffineTransform rotate(double a)
{
    // angle is in degree. Switch to radian
    a = deg2rad(a);
    double cosAngle = Math.cos(a);
    double sinAngle = Math.sin(a);
    AffineTransform rot = new AffineTransform(cosAngle, sinAngle, -sinAngle, cosAngle, 0, 0);

    multiply(rot);
    return this;
}

public AffineTransform rotate(double a, double ax, double ay)
{
	translate(ax, ay);
	rotate(a);
	translate(-ax, -ay);
    return this;
}

public AffineTransform scale(double s)
{
    return scale(s, s);
}

public AffineTransform scale(double sx, double sy)
{
    m_transform[0] *= sx;
    m_transform[1] *= sx;
    m_transform[2] *= sy;
    m_transform[3] *= sy;
    return this;
}

public AffineTransform scale(double sx, double sy, double ax, double ay)
{
	translate(ax, ay);
    scale(sx, sy);
	translate(-ax, -ay);
    return this;
}

// *this = *this * translation
public AffineTransform translate(double tx, double ty)
{
    if (isIdentityOrTranslation()) {
        m_transform[4] += tx;
        m_transform[5] += ty;
        return this;
    }
        
    m_transform[4] += tx * m_transform[0] + ty * m_transform[2];
    m_transform[5] += tx * m_transform[1] + ty * m_transform[3];
    return this;
}

AffineTransform scaleNonUniform(double sx, double sy)
{
    return scale(sx, sy);
}

AffineTransform rotateFromVector(double x, double y)
{
    return rotate(rad2deg(Math.atan2(y, x)));
}

AffineTransform flipX()
{
    return scale(-1, 1);
}

AffineTransform flipY()
{
    return scale(1, -1);
}

AffineTransform shear(double sx, double sy)
{
    double a = m_transform[0];
    double b = m_transform[1];

    m_transform[0] += sy * m_transform[2];
    m_transform[1] += sy * m_transform[3];
    m_transform[2] += sx * a;
    m_transform[3] += sx * b;

    return this;
}

private double deg2rad(double deg) {
	return deg / 180.0d * Math.PI;
}

private double rad2deg(double rad) {
	return rad * 180.0d / Math.PI;
}

AffineTransform skew(double angleX, double angleY)
{
    return shear(Math.tan(deg2rad(angleX)), Math.tan(deg2rad(angleY)));
}

AffineTransform skewX(double angle)
{
    return shear(Math.tan(deg2rad(angle)), 0);
}

AffineTransform skewY(double angle)
{
    return shear(0, Math.tan(deg2rad(angle)));
}

AffineTransform makeMapBetweenRects(Rect source, Rect dest)
{
    return (new AffineTransform()).translate(dest.left - source.left, dest.top - source.top)
    		.scale(dest.width() / source.width(), dest.height() / source.height());
}

double[] map(double x, double y)
{
	return new double[]{m_transform[0] * x + m_transform[2] * y + m_transform[4],
			m_transform[1] * x + m_transform[3] * y + m_transform[5]};
}

Point mapPoint(Point point)
{
    double[] newXY  = map(point.x, point.y);
    
    // Round the point.
    return new Point((int)Math.round(newXY[0]), (int)Math.round(newXY[1]));
}

public AffineTransform blend(AffineTransform from, double progress)
{
	DecomposedType srA = from.decompose();
    DecomposedType srB = decompose();
    
    double piDouble = Math.PI;

    // If x-axis of one is flipped, and y-axis of the other, convert to an unflipped rotation.
    if ((srA.scaleX < 0 && srB.scaleY < 0) || (srA.scaleY < 0 &&  srB.scaleX < 0)) {
        srA.scaleX = -srA.scaleX;
        srA.scaleY = -srA.scaleY;
        srA.angle += srA.angle < 0 ? piDouble : -piDouble;
    }

    // Don't rotate the long way around.
    srA.angle = (new BigDecimal(srA.angle)).remainder(new BigDecimal(2*piDouble)).doubleValue();
    srB.angle = (new BigDecimal(srB.angle)).remainder(new BigDecimal(2*piDouble)).doubleValue();

    if (Math.abs(srA.angle - srB.angle) > piDouble) {
        if (srA.angle > srB.angle)
            srA.angle -= piDouble * 2;
        else
            srB.angle -= piDouble * 2;
    }
    
    srA.scaleX += progress * (srB.scaleX - srA.scaleX);
    srA.scaleY += progress * (srB.scaleY - srA.scaleY);
    srA.angle += progress * (srB.angle - srA.angle);
    srA.remainderA += progress * (srB.remainderA - srA.remainderA);
    srA.remainderB += progress * (srB.remainderB - srA.remainderB);
    srA.remainderC += progress * (srB.remainderC - srA.remainderC);
    srA.remainderD += progress * (srB.remainderD - srA.remainderD);
    srA.translateX += progress * (srB.translateX - srA.translateX);
    srA.translateY += progress * (srB.translateY - srA.translateY);

    recompose(srA);
    return this;
}

public Matrix toMatrix()
{
	Matrix result = new Matrix();
	result.setValues(new float[]{(float) m_transform[0], (float) m_transform[2], (float) m_transform[4], 
			(float) m_transform[1], (float) m_transform[3], (float) m_transform[5], 
			0.0f, 0.0f, 1.0f});
    return result;
}


public static class DecomposedType {
	public double scaleX, scaleY;
	public double angle;
	public double remainderA, remainderB, remainderC, remainderD;
	public double translateX, translateY;
} ;

public DecomposedType decompose()
{
    AffineTransform m = new AffineTransform(this);
    
    DecomposedType decomp = new DecomposedType();
    
    // Compute scaling factors
    double sx = xScale();
    double sy = yScale();
    
    // Compute cross product of transformed unit vectors. If negative,
    // one axis was flipped.
    if (m.a() * m.d() - m.c() * m.b() < 0) {
        // Flip axis with minimum unit vector dot product
        if (m.a() < m.d())
            sx = -sx;
        else
            sy = -sy;
    }
    
    // Remove scale from matrix
    m.scale(1 / sx, 1 / sy);
    
    // Compute rotation
    double angle = Math.atan2(m.b(), m.a());
    
    // Remove rotation from matrix
    m.rotate(rad2deg(-angle));
    
    // Return results    
    decomp.scaleX = sx;
    decomp.scaleY = sy;
    decomp.angle = angle;
    decomp.remainderA = m.a();
    decomp.remainderB = m.b();
    decomp.remainderC = m.c();
    decomp.remainderD = m.d();
    decomp.translateX = m.e();
    decomp.translateY = m.f();
    
    return decomp;
}

void recompose(DecomposedType decomp)
{
    setA(decomp.remainderA);
    setB(decomp.remainderB);
    setC(decomp.remainderC);
    setD(decomp.remainderD);
    setE(decomp.translateX);
    setF(decomp.translateY);
    rotate(rad2deg(decomp.angle));
    scale(decomp.scaleX, decomp.scaleY);
}

}