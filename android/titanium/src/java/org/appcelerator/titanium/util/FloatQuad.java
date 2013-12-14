package org.appcelerator.titanium.util;

import android.graphics.Rect;
import android.graphics.RectF;

public class FloatQuad implements Cloneable {
    final static double EPSILON = 1E-14;
	private FloatPoint m_p1;
	private FloatPoint m_p2;
	private FloatPoint m_p3;
	private FloatPoint m_p4;
	FloatQuad()
    {
    }
	
	FloatQuad(FloatQuad other)
    {
		m_p1 = new FloatPoint(other.p1());
		m_p2 = new FloatPoint(other.p2());
		m_p3 = new FloatPoint(other.p3());
		m_p4 = new FloatPoint(other.p4());
    }

    FloatQuad(FloatPoint p1, FloatPoint p2, FloatPoint p3, FloatPoint p4)
        
    {
    	m_p1=p1;
        m_p2=p2;
        m_p3=p3;
        m_p4=p4;
    }

    FloatPoint p1() { return m_p1; }
    FloatPoint p2() { return m_p2; }
    FloatPoint p3() { return m_p3; }
    FloatPoint p4() { return m_p4; }

    void setP1(FloatPoint p) { m_p1 = p; }
    void setP2(FloatPoint p) { m_p2 = p; }
    void setP3(FloatPoint p) { m_p3 = p; }
    void setP4(FloatPoint p) { m_p4 = p; }
    
    static float min4(float a, float b, float c, float d)
    {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    static float max4(float a, float b, float c, float d)
    {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    float dot(FloatPoint a, FloatPoint b)
    {
        return a.x() * b.x() + a.y() * b.y();
    }

    float determinant(FloatPoint a, FloatPoint b)
    {
        return a.x() * b.y() - a.y() * b.x();
    }

    boolean isPointInTriangle(FloatPoint p, FloatPoint t1, FloatPoint t2, FloatPoint t3)
    {
        // Compute vectors        
    	FloatPoint v0 = new FloatPoint(t3.x() - t1.x(),t3.y() - t1.y());
    	FloatPoint v1 = new FloatPoint(t2.x() - t1.x(),t2.y() - t1.y());
    	FloatPoint v2 = new FloatPoint(p.x() - t1.x(),p.y() - t1.y());
        
        // Compute dot products
        float dot00 = dot(v0, v0);
        float dot01 = dot(v0, v1);
        float dot02 = dot(v0, v2);
        float dot11 = dot(v1, v1);
        float dot12 = dot(v1, v2);

        // Compute barycentric coordinates
        float invDenom = 1.0f / (dot00 * dot11 - dot01 * dot01);
        float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        float v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        // Check if point is in triangle
        return (u >= 0) && (v >= 0) && (u + v <= 1);
    }

    RectF boundingBox()
    {
        float left   = min4(m_p1.x(), m_p2.x(), m_p3.x(), m_p4.x());
        float top    = min4(m_p1.y(), m_p2.y(), m_p3.y(), m_p4.y());

        float right  = max4(m_p1.x(), m_p2.x(), m_p3.x(), m_p4.x());
        float bottom = max4(m_p1.y(), m_p2.y(), m_p3.y(), m_p4.y());
        
        return new RectF(left, top, right - left, bottom - top);
    }
    
    static boolean withinEpsilon(float a, float b)
    {
        return Math.abs(a - b) < EPSILON;
    }

    boolean isRectilinear()
    {
        return (withinEpsilon(m_p1.x(), m_p2.x()) && withinEpsilon(m_p2.y(), m_p3.y()) && withinEpsilon(m_p3.x(), m_p4.x()) && withinEpsilon(m_p4.y(), m_p1.y()))
            || (withinEpsilon(m_p1.y(), m_p2.y()) && withinEpsilon(m_p2.x(), m_p3.x()) && withinEpsilon(m_p3.y(), m_p4.y()) && withinEpsilon(m_p4.x(), m_p1.x()));
    }

    boolean containsPoint(FloatPoint p)
    {
        return isPointInTriangle(p, m_p1, m_p2, m_p3) || isPointInTriangle(p, m_p1, m_p3, m_p4);
    } 

    // Note that we only handle convex quads here.
    boolean containsQuad(FloatQuad other)
    {
        return containsPoint(other.p1()) && containsPoint(other.p2()) && containsPoint(other.p3()) && containsPoint(other.p4());
    }

    static FloatPoint rightMostCornerToVector(RectF rect, FloatPoint vector)
    {
        // Return the corner of the rectangle that if it is to the left of the vector
        // would mean all of the rectangle is to the left of the vector.
        // The vector here represents the side between two points in a clockwise convex polygon.
        //
        //  Q  XXX
        // QQQ XXX   If the lower left corner of X is left of the vector that goes from the top corner of Q to
        //  QQQ      the right corner of Q, then all of X is left of the vector, and intersection impossible.
        //   Q
        //
        FloatPoint point = new FloatPoint();
        if (vector.x() >= 0)
            point.setY(rect.bottom);
        else
            point.setY(rect.top);
        if (vector.y() >= 0)
            point.setX(rect.left);
        else
            point.setX(rect.right);
        return point;
    }

    boolean intersectsRect(RectF rect)
    {
        // For each side of the quad clockwise we check if the rectangle is to the left of it
        // since only content on the right can onlap with the quad.
        // This only works if the quad is convex.
    	FloatPoint v1, v2, v3, v4;

        // Ensure we use clockwise vectors.
        if (!isCounterclockwise()) {
            v1 = m_p2.subtract(m_p1);
            v2 = m_p3.subtract(m_p2);
            v3 = m_p4.subtract(m_p3);
            v4 = m_p1.subtract(m_p4);
        } else {
            v1 = m_p4.subtract(m_p1);
            v2 = m_p1.subtract(m_p2);
            v3 = m_p2.subtract(m_p3);
            v4 = m_p3.subtract(m_p4);
        }

        FloatPoint p = rightMostCornerToVector(rect, v1);
        if (determinant(v1, p.subtract(m_p1)) < 0)
            return false;

        p = rightMostCornerToVector(rect, v2);
        if (determinant(v2, p.subtract(m_p2)) < 0)
            return false;

        p = rightMostCornerToVector(rect, v3);
        if (determinant(v3, p.subtract(m_p3)) < 0)
            return false;

        p = rightMostCornerToVector(rect, v4);
        if (determinant(v4, p.subtract(m_p4)) < 0)
            return false;

        // If not all of the rectangle is outside one of the quad's four sides, then that means at least
        // a part of the rectangle is overlapping the quad.
        return true;
    }

    // Tests whether the line is contained by or intersected with the circle.
    static boolean lineIntersectsCircle(FloatPoint center, float radius, FloatPoint p0, FloatPoint p1)
    {
        float x0 = p0.x() - center.x(), y0 = p0.y() - center.y();
        float x1 = p1.x() - center.x(), y1 = p1.y() - center.y();
        float radius2 = radius * radius;
        if ((x0 * x0 + y0 * y0) <= radius2 || (x1 * x1 + y1 * y1) <= radius2)
            return true;
        if (p0 == p1)
            return false;

        float a = y0 - y1;
        float b = x1 - x0;
        float c = x0 * y1 - x1 * y0;
        float distance2 = c * c / (a * a + b * b);
        // If distance between the center point and the line > the radius,
        // the line doesn't cross (or is contained by) the ellipse.
        if (distance2 > radius2)
            return false;

        // The nearest point on the line is between p0 and p1?
        float x = - a * c / (a * a + b * b);
        float y = - b * c / (a * a + b * b);
        return (((x0 <= x && x <= x1) || (x0 >= x && x >= x1))
            && ((y0 <= y && y <= y1) || (y1 <= y && y <= y0)));
    }

    boolean intersectsCircle(FloatPoint center, float radius)
    {
        return containsPoint(center) // The circle may be totally contained by the quad.
            || lineIntersectsCircle(center, radius, m_p1, m_p2)
            || lineIntersectsCircle(center, radius, m_p2, m_p3)
            || lineIntersectsCircle(center, radius, m_p3, m_p4)
            || lineIntersectsCircle(center, radius, m_p4, m_p1);
    }

    boolean intersectsEllipse(FloatPoint center, FloatPoint radii)
    {
        // Transform the ellipse to an origin-centered circle whose radius is the product of major radius and minor radius.
        // Here we apply the same transformation to the quad.
        FloatQuad transformedQuad = new FloatQuad(this);
        transformedQuad.move(-center.x(), -center.y());
        transformedQuad.scale(radii.y(), radii.x());

        FloatPoint originPoint = new FloatPoint();
        return transformedQuad.intersectsCircle(originPoint, radii.y() * radii.x());

    }

    boolean isCounterclockwise()
    {
        // Return if the two first vectors are turning clockwise. If the quad is convex then all following vectors will turn the same way.
        return determinant(m_p2.subtract(m_p1), m_p3.subtract(m_p2)) < 0;
    }

    // isEmpty tests that the bounding box is empty. This will not identify
    // "slanted" empty quads.
    boolean isEmpty() { return boundingBox().isEmpty(); }

    // The center of the quad. If the quad is the result of a affine-transformed rectangle this is the same as the original center transformed.
    FloatPoint center()
    {
        return new FloatPoint((m_p1.x() + m_p2.x() + m_p3.x() + m_p4.x()) / 4.0,
                          (m_p1.y() + m_p2.y() + m_p3.y() + m_p4.y()) / 4.0);
    }

    Rect enclosingBoundingBox()
    {
    	Rect result = new Rect();
    	boundingBox().round(result);
        return result;
    }

    void move(FloatPoint offset)
    {
        m_p1.add(offset);
        m_p2.add(offset);
        m_p3.add(offset);
        m_p4.add(offset);
    }

    void move(float dx, float dy)
    {
        m_p1.move(dx, dy);
        m_p2.move(dx, dy);
        m_p3.move(dx, dy);
        m_p4.move(dx, dy);
    }

    void scale(float dx, float dy)
    {
        m_p1.scale(dx, dy);
        m_p2.scale(dx, dy);
        m_p3.scale(dx, dy);
        m_p4.scale(dx, dy);
    }
}
