package org.appcelerator.titanium.util;

import android.graphics.Point;

public class FloatPoint {
	float m_x;
	float m_y;
	
	FloatPoint(){
		m_x = 0;
		m_y = 0;
	}
    FloatPoint(float x, float y){
    	m_x = x;
		m_y = y;
    }
    
    FloatPoint(double x, double y){
    	m_x = (float) x;
		m_y = (float) y;
    }
    FloatPoint(Point p) 
    {
    	m_x = p.x;
		m_y = p.y;
    }
    FloatPoint(FloatPoint p) 
    {
    	m_x = p.x();
		m_y = p.y();
    }

    static FloatPoint zero() { return new FloatPoint(); }

    float x() { return m_x; }
    float y() { return m_y; }

    void setX(float x) { m_x = x; }
    void setY(float y) { m_y = y; }
    void set(float x, float y)
    {
        m_x = x;
        m_y = y;
    }
    void move(float dx, float dy)
    {
        m_x += dx;
        m_y += dy;
    }
    void move(Point a)
    {
        m_x += a.x;
        m_y += a.y;
    }
    void move(FloatPoint a)
    {
        m_x += a.x();
        m_y += a.y();
    }
    void moveBy(Point a)
    {
        m_x += a.x;
        m_y += a.y;
    }
    void moveBy(FloatPoint a)
    {
        m_x += a.x();
        m_y += a.y();
    }
    void scale(float sx, float sy)
    {
        m_x *= sx;
        m_y *= sy;
    }

    float dot(FloatPoint a)
    {
        return m_x * a.x() + m_y * a.y();
    }

    float lengthSquared()
    {
        return m_x * m_x + m_y * m_y;
    }

    FloatPoint expandedTo(FloatPoint other)
    {
        return new FloatPoint(Math.max(m_x, other.m_x), Math.max(m_y, other.m_y));
    }

    FloatPoint transposedPoint()
    {
        return new FloatPoint(m_y, m_x);
    }
    
    void normalize()
    {
        float tempLength = length();

        if (tempLength != 0) {
            m_x /= tempLength;
            m_y /= tempLength;
        }
    }

    float slopeAngleRadians()
    {
        return (float) Math.atan2(m_y, m_x);
    }

    float length()
    {
        return (float) Math.sqrt(lengthSquared());
    }

    FloatPoint matrixTransform(AffineTransform transform)
    {
        double[] newXY  = transform.map(m_x, m_y);
        
        return narrowPrecision(newXY[0], newXY[1]);
    }

    FloatPoint matrixTransform(TransformationMatrix transform)
    {
        double[] newXY  = transform.map(m_x, m_y);
        
        return narrowPrecision(newXY[0], newXY[1]);
    }

    FloatPoint narrowPrecision(double x, double y)
    {
        return new FloatPoint((float)x, (float)y);
    }

    float findSlope(FloatPoint p1, FloatPoint p2, float c)
    {
        if (p2.x() == p1.x())
            return Float.NaN;

        // y = mx + c
        float slope = (p2.y() - p1.y()) / (p2.x() - p1.x());
        c = p1.y() - slope * p1.x();
        return slope;
    }

    boolean findIntersection(FloatPoint p1, FloatPoint p2, FloatPoint d1, FloatPoint d2, FloatPoint intersection) 
    {
        float pOffset = 0;
        float pSlope = findSlope(p1, p2, pOffset);

        float dOffset = 0;
        float dSlope = findSlope(d1, d2, dOffset);

        if (dSlope == pSlope)
            return false;
        
        if (pSlope == Float.NaN) {
            intersection.setX(p1.x());
            intersection.setY(dSlope * intersection.x() + dOffset);
            return true;
        }
        if (dSlope == Float.NaN) {
            intersection.setX(d1.x());
            intersection.setY(pSlope * intersection.x() + pOffset);
            return true;
        }
        
        // Find x at intersection, where ys overlap; x = (c' - c) / (m - m')
        intersection.setX((dOffset - pOffset) / (pSlope - dSlope));
        intersection.setY(pSlope * intersection.x() + pOffset);
        return true;
    }
    
    FloatPoint add(FloatPoint b)
    {
        return new FloatPoint(m_y + b.x(), m_x + b.y());
    }
    FloatPoint subtract(FloatPoint b)
    {
        return new FloatPoint(m_y - b.x(), m_x - b.y());
    }
}
