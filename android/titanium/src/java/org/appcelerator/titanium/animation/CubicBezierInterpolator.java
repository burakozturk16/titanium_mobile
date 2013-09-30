package org.appcelerator.titanium.animation;

import android.view.animation.Interpolator;

public class CubicBezierInterpolator implements Interpolator {
	double cx, bx, ax, cy, by, ay, duration;

	private double sampleCurveX(double t) {
		// `ax t^3 + bx t^2 + cx t' expanded using Horner's rule.
		return ((ax * t + bx) * t + cx) * t;
	};

	/**
	 * @param t
	 *            {number} parametric timing value
	 * @return {number}
	 */
	private double sampleCurveY(double t) {
		return ((ay * t + by) * t + cy) * t;
	};

	/**
	 * @param t
	 *            {number} parametric timing value
	 * @return {number}
	 */
	private double sampleCurveDerivativeX(double t) {
		return (3.0 * ax * t + 2.0 * bx) * t + cx;
	};

	private double solveEpsilon(double d) {
		return 1.0 / (200.0 * d);
	};

	private double solveCurveX(double x) {

		double t0, t1, t2, x2, d2, i;

		// First try a few iterations of Newton's method -- normally very fast.
		for (t2 = x, i = 0; i < 8; i++) {
			x2 = sampleCurveX(t2) - x;
			if (Math.abs(x2) < duration) {
				return t2;
			}
			d2 = sampleCurveDerivativeX(t2);
			if (Math.abs(d2) < 1e-6) {
				break;
			}
			t2 = t2 - x2 / d2;
		}

		// Fall back to the bisection method for reliability.
		t0 = 0.0;
		t1 = 1.0;
		t2 = x;

		if (t2 < t0) {
			return t0;
		}
		if (t2 > t1) {
			return t1;
		}

		while (t0 < t1) {
			x2 = sampleCurveX(t2);
			if (Math.abs(x2 - x) < duration) {
				return t2;
			}
			if (x > x2) {
				t0 = t2;
			} else {
				t1 = t2;
			}
			t2 = (t1 - t0) * 0.5 + t0;
		}

		// Failure.
		return t2;
	}

	public float getInterpolation(float time) {
		return (float) sampleCurveY(solveCurveX(time));
	}

	public CubicBezierInterpolator(final double p1, final double p2,
			final double p3, final double p4, final double duration) {
		this.duration = solveEpsilon(duration);
		/**
		 * X component of Bezier coefficient C
		 * 
		 * @const
		 * @type {number}
		 */
		cx = 3.0 * p1;

		/**
		 * X component of Bezier coefficient B
		 * 
		 * @const
		 * @type {number}
		 */
		bx = 3.0 * (p3 - p1) - cx;

		/**
		 * X component of Bezier coefficient A
		 * 
		 * @const
		 * @type {number}
		 */
		ax = 1.0 - cx - bx;

		/**
		 * Y component of Bezier coefficient C
		 * 
		 * @const
		 * @type {number}
		 */
		cy = 3.0 * p2;

		/**
		 * Y component of Bezier coefficient B
		 * 
		 * @const
		 * @type {number}
		 */
		by = 3.0 * (p4 - p2) - cy;

		/**
		 * Y component of Bezier coefficient A
		 * 
		 * @const
		 * @type {number}
		 */
		ay = 1.0 - cy - by;
	}
}
