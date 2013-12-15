package org.appcelerator.titanium.util;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class TransformationMatrix implements Cloneable {
	//
	// Supporting Math Functions
	//
	// This is a set of function from various places (attributed inline) to do things like
	// inversion and decomposition of a 4x4 matrix. They are used throughout the code
	//

	//
	// Adapted from Matrix Inversion by Richard Carling, Graphics Gems <http://tog.acm.org/GraphicsGems/index.html>.

	// EULA: The Graphics Gems code is copyright-protected. In other words, you cannot claim the text of the code
	// as your own and resell it. Using the code is permitted in any program, product, or library, non-commercial
	// or commercial. Giving credit is not required, though is a nice gesture. The code comes as-is, and if there
	// are any flaws or problems with any Gems code, nobody involved with Gems - authors, editors, publishers, or
	// webmasters - are to be held responsible. Basically, don't be a jerk, and remember that anything free comes
	// with no guarantee.

	// A clarification about the storage of matrix elements
	//
	// This class uses a 2 dimensional array internally to store the elements of the matrix. The first index into
	// the array refers to the column that the element lies in; the second index refers to the row.
	//
	// In other words, this is the layout of the matrix:
	//
	// | m_matrix[0][0] m_matrix[1][0] m_matrix[2][0] m_matrix[3][0] |
	// | m_matrix[0][1] m_matrix[1][1] m_matrix[2][1] m_matrix[3][1] |
	// | m_matrix[0][2] m_matrix[1][2] m_matrix[2][2] m_matrix[3][2] |
	// | m_matrix[0][3] m_matrix[1][3] m_matrix[2][3] m_matrix[3][3] |

	static double SMALL_NUMBER = 1.e-8;
	
	public static class Decomposed2Type {
		public double scaleX, scaleY;
		public double translateX, translateY;
		public double angle;
		public double m11, m12, m21, m22;
		public Decomposed2Type() {
	        scaleX = 1;
	        scaleY = 1;
		}
	}
	
	public static class Decomposed4Type {
		public double scaleX, scaleY, scaleZ;
		public double skewXY, skewXZ, skewYZ;
		public double quaternionX, quaternionY, quaternionZ, quaternionW;
		public double translateX, translateY, translateZ;
		public double perspectiveX, perspectiveY, perspectiveZ, perspectiveW;
		public Decomposed4Type() {
			perspectiveW = 1;
	        scaleX = 1;
	        scaleY = 1;
	        scaleZ = 1;
		}
	}

	public class BooleanRef { 
		public boolean value; 
		BooleanRef(boolean value) 
		{
			this.value = value;
		}
	}
	
	private double[][] m_matrix = new double[4][4];

	// inverse(original_matrix, inverse_matrix)
	//
	// calculate the inverse of a 4x4 matrix
	//
	// -1
	// A  = ___1__ adjoint A
//	       det A

	//  double = determinant2x2(double a, double b, double c, double d)
	//
	//  calculate the determinant of a 2x2 matrix.

	static double determinant2x2(double a, double b, double c, double d)
	{
	    return a * d - b * c;
	}

	//  double = determinant3x3(a1, a2, a3, b1, b2, b3, c1, c2, c3)
	//
	//  Calculate the determinant of a 3x3 matrix
	//  in the form
	//
//	      | a1,  b1,  c1 |
//	      | a2,  b2,  c2 |
//	      | a3,  b3,  c3 |

	static double determinant3x3(double a1, double a2, double a3, double b1, double b2, double b3, double c1, double c2, double c3)
	{
	    return a1 * determinant2x2(b2, b3, c2, c3)
	         - b1 * determinant2x2(a2, a3, c2, c3)
	         + c1 * determinant2x2(a2, a3, b2, b3);
	}

	//  double = determinant4x4(matrix)
	//
	//  calculate the determinant of a 4x4 matrix.

	static double determinant4x4(double[][] m)
	{
	    // Assign to individual variable names to aid selecting
	    // correct elements

	    double a1 = m[0][0];
	    double b1 = m[0][1];
	    double c1 = m[0][2];
	    double d1 = m[0][3];

	    double a2 = m[1][0];
	    double b2 = m[1][1];
	    double c2 = m[1][2];
	    double d2 = m[1][3];

	    double a3 = m[2][0];
	    double b3 = m[2][1];
	    double c3 = m[2][2];
	    double d3 = m[2][3];

	    double a4 = m[3][0];
	    double b4 = m[3][1];
	    double c4 = m[3][2];
	    double d4 = m[3][3];

	    return a1 * determinant3x3(b2, b3, b4, c2, c3, c4, d2, d3, d4)
	         - b1 * determinant3x3(a2, a3, a4, c2, c3, c4, d2, d3, d4)
	         + c1 * determinant3x3(a2, a3, a4, b2, b3, b4, d2, d3, d4)
	         - d1 * determinant3x3(a2, a3, a4, b2, b3, b4, c2, c3, c4);
	}

	// adjoint( original_matrix, inverse_matrix )
	//
	//   calculate the adjoint of a 4x4 matrix
	//
//	    Let  a   denote the minor determinant of matrix A obtained by
//	         ij
	//
//	    deleting the ith row and jth column from A.
	//
//	                  i+j
	//   Let  b   = (-1)    a
//	        ij            ji
	//
	//  The matrix B = (b  ) is the adjoint of A
//	                   ij

	static double[][] adjoint(double[][] matrix)
	{
	    // Assign to individual variable names to aid
	    // selecting correct values
	    double a1 = matrix[0][0];
	    double b1 = matrix[0][1];
	    double c1 = matrix[0][2];
	    double d1 = matrix[0][3];

	    double a2 = matrix[1][0];
	    double b2 = matrix[1][1];
	    double c2 = matrix[1][2];
	    double d2 = matrix[1][3];

	    double a3 = matrix[2][0];
	    double b3 = matrix[2][1];
	    double c3 = matrix[2][2];
	    double d3 = matrix[2][3];

	    double a4 = matrix[3][0];
	    double b4 = matrix[3][1];
	    double c4 = matrix[3][2];
	    double d4 = matrix[3][3];
	    
	    double[][] result = new double[4][4];
	    // Row column labeling reversed since we transpose rows & columns
	    result[0][0]  =   determinant3x3(b2, b3, b4, c2, c3, c4, d2, d3, d4);
	    result[1][0]  = - determinant3x3(a2, a3, a4, c2, c3, c4, d2, d3, d4);
	    result[2][0]  =   determinant3x3(a2, a3, a4, b2, b3, b4, d2, d3, d4);
	    result[3][0]  = - determinant3x3(a2, a3, a4, b2, b3, b4, c2, c3, c4);

	    result[0][1]  = - determinant3x3(b1, b3, b4, c1, c3, c4, d1, d3, d4);
	    result[1][1]  =   determinant3x3(a1, a3, a4, c1, c3, c4, d1, d3, d4);
	    result[2][1]  = - determinant3x3(a1, a3, a4, b1, b3, b4, d1, d3, d4);
	    result[3][1]  =   determinant3x3(a1, a3, a4, b1, b3, b4, c1, c3, c4);

	    result[0][2]  =   determinant3x3(b1, b2, b4, c1, c2, c4, d1, d2, d4);
	    result[1][2]  = - determinant3x3(a1, a2, a4, c1, c2, c4, d1, d2, d4);
	    result[2][2]  =   determinant3x3(a1, a2, a4, b1, b2, b4, d1, d2, d4);
	    result[3][2]  = - determinant3x3(a1, a2, a4, b1, b2, b4, c1, c2, c4);

	    result[0][3]  = - determinant3x3(b1, b2, b3, c1, c2, c3, d1, d2, d3);
	    result[1][3]  =   determinant3x3(a1, a2, a3, c1, c2, c3, d1, d2, d3);
	    result[2][3]  = - determinant3x3(a1, a2, a3, b1, b2, b3, d1, d2, d3);
	    result[3][3]  =   determinant3x3(a1, a2, a3, b1, b2, b3, c1, c2, c3);
	    return result;
	}

	// Returns null if the matrix is not invertible
	static double[][] inverse(double[][] matrix)
	{
	    // Calculate the adjoint matrix
	    double[][] result = adjoint(matrix);
	    

	    // Calculate the 4x4 determinant
	    // If the determinant is zero,
	    // then the inverse matrix is not unique.
	    double det = determinant4x4(matrix);

	    if (Math.abs(det) < SMALL_NUMBER)
	        return null;

	    // Scale the adjoint matrix to get the inverse

	    for (int i = 0; i < 4; i++)
	        for (int j = 0; j < 4; j++)
	            result[i][j] = result[i][j] / det;

	    return result;
	}

	// End of code adapted from Matrix Inversion by Richard Carling

	// Perform a decomposition on the passed matrix, return false if unsuccessful
	// From Graphics Gems: unmatrix.c

	// Transpose rotation portion of matrix a, return b
	static double[][] transposeMatrix4(double[][] a)
	{
		double[][] result = new double[4][4];
	    for (int i = 0; i < 4; i++)
	        for (int j = 0; j < 4; j++)
	            result[i][j] = a[j][i];
	    return result;
	}

	// Multiply a homogeneous point by a matrix and return the transformed point
	static double[] v4MulPointByMatrix(double[] p, double[][] m)
	{
		double[] result = new double[4];
	    result[0] = (p[0] * m[0][0]) + (p[1] * m[1][0]) +
	                (p[2] * m[2][0]) + (p[3] * m[3][0]);
	    result[1] = (p[0] * m[0][1]) + (p[1] * m[1][1]) +
	                (p[2] * m[2][1]) + (p[3] * m[3][1]);
	    result[2] = (p[0] * m[0][2]) + (p[1] * m[1][2]) +
	                (p[2] * m[2][2]) + (p[3] * m[3][2]);
	    result[3] = (p[0] * m[0][3]) + (p[1] * m[1][3]) +
	                (p[2] * m[2][3]) + (p[3] * m[3][3]);
	    return result;
	}

	static double v3Length(double[] a)
	{
	    return Math.sqrt((a[0] * a[0]) + (a[1] * a[1]) + (a[2] * a[2]));
	}

	static void v3Scale(double[] v, double desiredLength)
	{
	    double len = v3Length(v);
	    if (len != 0) {
	        double l = desiredLength / len;
	        v[0] *= l;
	        v[1] *= l;
	        v[2] *= l;
	    }
	}

	static double v3Dot(double[] a, double[] b)
	{
	    return (a[0] * b[0]) + (a[1] * b[1]) + (a[2] * b[2]);
	}

	// Make a linear combination of two vectors and return the result.
	// result = (a * ascl) + (b * bscl)
	static void v3Combine(double[] a, double[] b, double[] result, double ascl, double bscl)
	{
	    result[0] = (ascl * a[0]) + (bscl * b[0]);
	    result[1] = (ascl * a[1]) + (bscl * b[1]);
	    result[2] = (ascl * a[2]) + (bscl * b[2]);
	}

	// Return the cross product result = a cross b */
	static double[] v3Cross(double[] a, double[] b)
	{
		double[] result = new double[3];
	    result[0] = (a[1] * b[2]) - (a[2] * b[1]);
	    result[1] = (a[2] * b[0]) - (a[0] * b[2]);
	    result[2] = (a[0] * b[1]) - (a[1] * b[0]);
	    return result;
	}

	static Decomposed2Type decompose2(double[][] matrix)
	{
		Decomposed2Type result = new Decomposed2Type();
	    double row0x = matrix[0][0];
	    double row0y = matrix[0][1];
	    double row1x = matrix[1][0];
	    double row1y = matrix[1][1];
	    result.translateX = matrix[3][0];
	    result.translateY = matrix[3][1];

	    // Compute scaling factors.
	    result.scaleX = Math.sqrt(row0x * row0x + row0y * row0y);
	    result.scaleY = Math.sqrt(row1x * row1x + row1y * row1y);

	    // If determinant is negative, one axis was flipped.
	    double determinant = row0x * row1y - row0y * row1x;
	    if (determinant < 0) {
	        // Flip axis with minimum unit vector dot product.
	        if (row0x < row1y)
	            result.scaleX = -result.scaleX;
	        else
	            result.scaleY = -result.scaleY;
	    }

	    // Renormalize matrix to remove scale.
	    if (result.scaleX != 0) {
	        row0x *= 1 / result.scaleX;
	        row0y *= 1 / result.scaleX;
	    }
	    if (result.scaleY != 0) {
	        row1x *= 1 / result.scaleY;
	        row1y *= 1 / result.scaleY;
	    }

	    // Compute rotation and renormalize matrix.
	    result.angle = Math.atan2(row0y, row0x);

	    if (result.angle != 0) {
	        // Rotate(-angle) = [cos(angle), sin(angle), -sin(angle), cos(angle)]
	        //                = [row0x, -row0y, row0y, row0x]
	        // Thanks to the normalization above.
	        double sn = -row0y;
	        double cs = row0x;
	        double m11 = row0x, m12 = row0y;
	        double m21 = row1x, m22 = row1y;

	        row0x = cs * m11 + sn * m21;
	        row0y = cs * m12 + sn * m22;
	        row1x = -sn * m11 + cs * m21;
	        row1y = -sn * m12 + cs * m22;
	    }

	    result.m11 = row0x;
	    result.m12 = row0y;
	    result.m21 = row1x;
	    result.m22 = row1y;

	    // Convert into degrees because our rotation functions expect it.
	    result.angle = Math.toDegrees(result.angle);

	    return result;
	}

	static Decomposed4Type decompose4(double[][] mat)
	{
	    double[][] localMatrix = mat.clone();

	    // Normalize the matrix.
	    if (localMatrix[3][3] == 0)
	        return null;

	    int i, j;
	    for (i = 0; i < 4; i++)
	        for (j = 0; j < 4; j++)
	            localMatrix[i][j] /= localMatrix[3][3];
	    
	    // perspectiveMatrix is used to solve for perspective, but it also provides
	    // an easy way to test for singularity of the upper 3x3 component.
	    double[][] perspectiveMatrix = localMatrix.clone();
	    for (i = 0; i < 3; i++)
	        perspectiveMatrix[i][3] = 0;
	    perspectiveMatrix[3][3] = 1;

	    if (determinant4x4(perspectiveMatrix) == 0)
	        return null;

	    Decomposed4Type result = new Decomposed4Type();

	    // First, isolate perspective. This is the messiest.
	    if (localMatrix[0][3] != 0 || localMatrix[1][3] != 0 || localMatrix[2][3] != 0) {
	        // rightHandSide is the right hand side of the equation.
	        double[] rightHandSide = new double[4];
	        rightHandSide[0] = localMatrix[0][3];
	        rightHandSide[1] = localMatrix[1][3];
	        rightHandSide[2] = localMatrix[2][3];
	        rightHandSide[3] = localMatrix[3][3];

	        // Solve the equation by inverting perspectiveMatrix and multiplying
	        // rightHandSide by the inverse. (This is the easiest way, not
	        // necessarily the best.)
	        double[][] inversePerspectiveMatrix, transposedInversePerspectiveMatrix;
	        inversePerspectiveMatrix = inverse(perspectiveMatrix);
	        transposedInversePerspectiveMatrix = transposeMatrix4(inversePerspectiveMatrix);

	        double[] perspectivePoint = new double[4];
	        perspectivePoint = v4MulPointByMatrix(rightHandSide, transposedInversePerspectiveMatrix);

	        result.perspectiveX = perspectivePoint[0];
	        result.perspectiveY = perspectivePoint[1];
	        result.perspectiveZ = perspectivePoint[2];
	        result.perspectiveW = perspectivePoint[3];

	        // Clear the perspective partition
	        localMatrix[0][3] = localMatrix[1][3] = localMatrix[2][3] = 0;
	        localMatrix[3][3] = 1;
	    } else {
	        // No perspective.
	        result.perspectiveX = result.perspectiveY = result.perspectiveZ = 0;
	        result.perspectiveW = 1;
	    }

	    // Next take care of translation (easy).
	    result.translateX = localMatrix[3][0];
	    localMatrix[3][0] = 0;
	    result.translateY = localMatrix[3][1];
	    localMatrix[3][1] = 0;
	    result.translateZ = localMatrix[3][2];
	    localMatrix[3][2] = 0;

	    // Vector4 type and functions need to be added to the common set.
	    double[][] row = new double[3][3];
	    double[] pdum3 = new double[3];

	    // Now get scale and shear.
	    for (i = 0; i < 3; i++) {
	        row[i][0] = localMatrix[i][0];
	        row[i][1] = localMatrix[i][1];
	        row[i][2] = localMatrix[i][2];
	    }

	    // Compute X scale factor and normalize first row.
	    result.scaleX = v3Length(row[0]);
	    v3Scale(row[0], 1.0);

	    // Compute XY shear factor and make 2nd row orthogonal to 1st.
	    result.skewXY = v3Dot(row[0], row[1]);
	    v3Combine(row[1], row[0], row[1], 1.0, -result.skewXY);

	    // Now, compute Y scale and normalize 2nd row.
	    result.scaleY = v3Length(row[1]);
	    v3Scale(row[1], 1.0);
	    result.skewXY /= result.scaleY;

	    // Compute XZ and YZ shears, orthogonalize 3rd row.
	    result.skewXZ = v3Dot(row[0], row[2]);
	    v3Combine(row[2], row[0], row[2], 1.0, -result.skewXZ);
	    result.skewYZ = v3Dot(row[1], row[2]);
	    v3Combine(row[2], row[1], row[2], 1.0, -result.skewYZ);

	    // Next, get Z scale and normalize 3rd row.
	    result.scaleZ = v3Length(row[2]);
	    v3Scale(row[2], 1.0);
	    result.skewXZ /= result.scaleZ;
	    result.skewYZ /= result.scaleZ;

	    // At this point, the matrix (in rows[]) is orthonormal.
	    // Check for a coordinate system flip. If the determinant
	    // is -1, then negate the matrix and the scaling factors.
	    pdum3 = v3Cross(row[1], row[2]);
	    if (v3Dot(row[0], pdum3) < 0) {

	        result.scaleX *= -1;
	        result.scaleY *= -1;
	        result.scaleZ *= -1;

	        for (i = 0; i < 3; i++) {
	            row[i][0] *= -1;
	            row[i][1] *= -1;
	            row[i][2] *= -1;
	        }
	    }

	    // Now, get the rotations out, as described in the gem.

	    // FIXME - Add the ability to return either quaternions (which are
	    // easier to recompose with) or Euler angles (rx, ry, rz), which
	    // are easier for authors to deal with. The latter will only be useful
	    // when we fix https://bugs.webkit.org/show_bug.cgi?id=23799, so I
	    // will leave the Euler angle code here for now.

	    // ret.rotateY = asin(-row[0][2]);
	    // if (cos(ret.rotateY) != 0) {
	    //     ret.rotateX = atan2(row[1][2], row[2][2]);
	    //     ret.rotateZ = atan2(row[0][1], row[0][0]);
	    // } else {
	    //     ret.rotateX = atan2(-row[2][0], row[1][1]);
	    //     ret.rotateZ = 0;
	    // }

	    double s, t, x, y, z, w;

	    t = row[0][0] + row[1][1] + row[2][2] + 1.0;

	    if (t > 1e-4) {
	        s = 0.5 / Math.sqrt(t);
	        w = 0.25 / s;
	        x = (row[2][1] - row[1][2]) * s;
	        y = (row[0][2] - row[2][0]) * s;
	        z = (row[1][0] - row[0][1]) * s;
	    } else if (row[0][0] > row[1][1] && row[0][0] > row[2][2]) {
	        s = Math.sqrt(1.0 + row[0][0] - row[1][1] - row[2][2]) * 2.0; // S = 4 * qx.
	        x = 0.25 * s;
	        y = (row[0][1] + row[1][0]) / s;
	        z = (row[0][2] + row[2][0]) / s;
	        w = (row[2][1] - row[1][2]) / s;
	    } else if (row[1][1] > row[2][2]) {
	        s = Math.sqrt(1.0 + row[1][1] - row[0][0] - row[2][2]) * 2.0; // S = 4 * qy.
	        x = (row[0][1] + row[1][0]) / s;
	        y = 0.25 * s;
	        z = (row[1][2] + row[2][1]) / s;
	        w = (row[0][2] - row[2][0]) / s;
	    } else {
	        s = Math.sqrt(1.0 + row[2][2] - row[0][0] - row[1][1]) * 2.0; // S = 4 * qz.
	        x = (row[0][2] + row[2][0]) / s;
	        y = (row[1][2] + row[2][1]) / s;
	        z = 0.25 * s;
	        w = (row[1][0] - row[0][1]) / s;
	    }

	    result.quaternionX = x;
	    result.quaternionY = y;
	    result.quaternionZ = z;
	    result.quaternionW = w;

	    return result;
	}

	// Perform a spherical linear interpolation between the two
	// passed quaternions with 0 <= t <= 1.
	static double[] slerp(double[] qa, double[] qb, double t)
	{
	    double ax, ay, az, aw;
	    double bx, by, bz, bw;
	    double cx, cy, cz, cw;
	    double angle;
	    double th, invth, scale, invscale;

	    ax = qa[0]; ay = qa[1]; az = qa[2]; aw = qa[3];
	    bx = qb[0]; by = qb[1]; bz = qb[2]; bw = qb[3];

	    angle = ax * bx + ay * by + az * bz + aw * bw;

	    if (angle < 0.0) {
	        ax = -ax; ay = -ay;
	        az = -az; aw = -aw;
	        angle = -angle;
	    }

	    if (angle + 1.0 > .05) {
	        if (1.0 - angle >= .05) {
	            th = Math.acos (angle);
	            invth = 1.0 / Math.sin (th);
	            scale = Math.sin (th * (1.0 - t)) * invth;
	            invscale = Math.sin (th * t) * invth;
	        } else {
	            scale = 1.0 - t;
	            invscale = t;
	        }
	    } else {
	        bx = -ay;
	        by = ax;
	        bz = -aw;
	        bw = az;
	        scale = Math.sin(2*Math.PI * (.5 - t));
	        invscale = Math.sin (2*Math.PI * t);
	    }

	    cx = ax * scale + bx * invscale;
	    cy = ay * scale + by * invscale;
	    cz = az * scale + bz * invscale;
	    cw = aw * scale + bw * invscale;
	    
	    double[] result = new double[4];
	    result[0] = cx; result[1] = cy; result[2] = cz; result[3] = cw;
	    return result;
	}

	// End of Supporting Math Functions
	
	 TransformationMatrix() { makeIdentity(); }
	    TransformationMatrix(TransformationMatrix t) { 
	    	setMatrix(t) ;
	    }
	    TransformationMatrix(double a, double b, double c, double d, double e, double f) { setMatrix(a, b, c, d, e, f); }
	    TransformationMatrix(double m11, double m12, double m13, double m14,
	                         double m21, double m22, double m23, double m24,
	                         double m31, double m32, double m33, double m34,
	                         double m41, double m42, double m43, double m44)
	    {
	        setMatrix(m11, m12, m13, m14, m21, m22, m23, m24, m31, m32, m33, m34, m41, m42, m43, m44);
	    }
	    
		TransformationMatrix(AffineTransform t)
		{
		    setMatrix(t.a(), t.b(), t.c(), t.d(), t.e(), t.f());
		}

		TransformationMatrix scale(double s)
		{
		    return scaleNonUniform(s, s);
		}

		TransformationMatrix rotateFromVector(double x, double y)
		{
		    return rotate(Math.toDegrees(Math.atan2(y, x)));
		}

		TransformationMatrix flipX()
		{
		    return scaleNonUniform(-1.0, 1.0);
		}

		TransformationMatrix flipY()
		{
		    return scaleNonUniform(1.0, -1.0);
		}

	    void setMatrix(double a, double b, double c, double d, double e, double f)
	    {
	        m_matrix[0][0] = a; m_matrix[0][1] = b; m_matrix[0][2] = 0; m_matrix[0][3] = 0; 
	        m_matrix[1][0] = c; m_matrix[1][1] = d; m_matrix[1][2] = 0; m_matrix[1][3] = 0; 
	        m_matrix[2][0] = 0; m_matrix[2][1] = 0; m_matrix[2][2] = 1; m_matrix[2][3] = 0; 
	        m_matrix[3][0] = e; m_matrix[3][1] = f; m_matrix[3][2] = 0; m_matrix[3][3] = 1;
	    }
	    
	    void setMatrix(double m11, double m12, double m13, double m14,
	                   double m21, double m22, double m23, double m24,
	                   double m31, double m32, double m33, double m34,
	                   double m41, double m42, double m43, double m44)
	    {
	        m_matrix[0][0] = m11; m_matrix[0][1] = m12; m_matrix[0][2] = m13; m_matrix[0][3] = m14; 
	        m_matrix[1][0] = m21; m_matrix[1][1] = m22; m_matrix[1][2] = m23; m_matrix[1][3] = m24; 
	        m_matrix[2][0] = m31; m_matrix[2][1] = m32; m_matrix[2][2] = m33; m_matrix[2][3] = m34; 
	        m_matrix[3][0] = m41; m_matrix[3][1] = m42; m_matrix[3][2] = m43; m_matrix[3][3] = m44;
	    }
	    
	    void setMatrix(double[][] mat)
		 {
	    	m_matrix = mat.clone();
		 }
	    void setMatrix(TransformationMatrix mat)
		 {
	    	setMatrix(mat.m_matrix);
		 }

	    TransformationMatrix makeIdentity()
	    {
	        setMatrix(1, 0, 0, 0,  0, 1, 0, 0,  0, 0, 1, 0,  0, 0, 0, 1);
	        return this;
	    }

	    boolean isIdentity()
	    {
	        return m_matrix[0][0] == 1 && m_matrix[0][1] == 0 && m_matrix[0][2] == 0 && m_matrix[0][3] == 0 &&
	               m_matrix[1][0] == 0 && m_matrix[1][1] == 1 && m_matrix[1][2] == 0 && m_matrix[1][3] == 0 &&
	               m_matrix[2][0] == 0 && m_matrix[2][1] == 0 && m_matrix[2][2] == 1 && m_matrix[2][3] == 0 &&
	               m_matrix[3][0] == 0 && m_matrix[3][1] == 0 && m_matrix[3][2] == 0 && m_matrix[3][3] == 1;
	    }
	
	double m11() { return m_matrix[0][0]; }
 void setM11(double f) { m_matrix[0][0] = f; }
 double m12() { return m_matrix[0][1]; }
 void setM12(double f) { m_matrix[0][1] = f; }
 double m13() { return m_matrix[0][2]; }
 void setM13(double f) { m_matrix[0][2] = f; }
 double m14() { return m_matrix[0][3]; }
 void setM14(double f) { m_matrix[0][3] = f; }
 double m21() { return m_matrix[1][0]; }
 void setM21(double f) { m_matrix[1][0] = f; }
 double m22() { return m_matrix[1][1]; }
 void setM22(double f) { m_matrix[1][1] = f; }
 double m23() { return m_matrix[1][2]; }
 void setM23(double f) { m_matrix[1][2] = f; }
 double m24() { return m_matrix[1][3]; }
 void setM24(double f) { m_matrix[1][3] = f; }
 double m31() { return m_matrix[2][0]; }
 void setM31(double f) { m_matrix[2][0] = f; }
 double m32() { return m_matrix[2][1]; }
 void setM32(double f) { m_matrix[2][1] = f; }
 double m33() { return m_matrix[2][2]; }
 void setM33(double f) { m_matrix[2][2] = f; }
 double m34() { return m_matrix[2][3]; }
 void setM34(double f) { m_matrix[2][3] = f; }
 double m41() { return m_matrix[3][0]; }
 void setM41(double f) { m_matrix[3][0] = f; }
 double m42() { return m_matrix[3][1]; }
 void setM42(double f) { m_matrix[3][1] = f; }
 double m43() { return m_matrix[3][2]; }
 void setM43(double f) { m_matrix[3][2] = f; }
 double m44() { return m_matrix[3][3]; }
 void setM44(double f) { m_matrix[3][3] = f; }
 
 double a() { return m_matrix[0][0]; }
 void setA(double a) { m_matrix[0][0] = a; }

 double b() { return m_matrix[0][1]; }
 void setB(double b) { m_matrix[0][1] = b; }

 double c() { return m_matrix[1][0]; }
 void setC(double c) { m_matrix[1][0] = c; }

 double d() { return m_matrix[1][1]; }
 void setD(double d) { m_matrix[1][1] = d; }

 double e() { return m_matrix[3][0]; }
 void setE(double e) { m_matrix[3][0] = e; }

 double f() { return m_matrix[3][1]; }
 void setF(double f) { m_matrix[3][1] = f; }
	
	boolean isAffine()
 {
     return (m13() == 0 && m14() == 0 && m23() == 0 && m24() == 0 && 
             m31() == 0 && m32() == 0 && m33() == 1 && m34() == 0 && m43() == 0 && m44() == 1);
 }
	boolean isIdentityOrTranslation()
	    {
	        return m_matrix[0][0] == 1 && m_matrix[0][1] == 0 && m_matrix[0][2] == 0 && m_matrix[0][3] == 0
	            && m_matrix[1][0] == 0 && m_matrix[1][1] == 1 && m_matrix[1][2] == 0 && m_matrix[1][3] == 0
	            && m_matrix[2][0] == 0 && m_matrix[2][1] == 0 && m_matrix[2][2] == 1 && m_matrix[2][3] == 0
	            && m_matrix[3][3] == 1;
	    }


	FloatPoint projectPoint(FloatPoint p, BooleanRef clamped)
	{
	    // This is basically raytracing. We have a point in the destination
	    // plane with z=0, and we cast a ray parallel to the z-axis from that
	    // point to find the z-position at which it intersects the z=0 plane
	    // with the transform applied. Once we have that point we apply the
	    // inverse transform to find the corresponding point in the source
	    // space.
	    //
	    // Given a plane with normal Pn, and a ray starting at point R0 and
	    // with direction defined by the vector Rd, we can find the
	    // intersection point as a distance d from R0 in units of Rd by:
	    //
	    // d = -dot (Pn', R0) / dot (Pn', Rd)
	    if (clamped != null)
	    	clamped.value = false;

	    if (m33() == 0) {
	        // In this case, the projection plane is parallel to the ray we are trying to
	        // trace, and there is no well-defined value for the projection.
	        return new FloatPoint();
	    }

	    double x = p.x();
	    double y = p.y();
	    double z = -(m13() * x + m23() * y + m43()) / m33();

	    // FIXME: use multVecMatrix()
	    double outX = x * m11() + y * m21() + z * m31() + m41();
	    double outY = x * m12() + y * m22() + z * m32() + m42();

	    double w = x * m14() + y * m24() + z * m34() + m44();
	    if (w <= 0) {
	        // Using int max causes overflow when other code uses the projected point. To
	        // represent infinity yet reduce the risk of overflow, we use a large but
	        // not-too-large number here when clamping.
	        int largeNumber = 100000000;
	        outX = Math.copySign(largeNumber, outX);
	        outY = Math.copySign(largeNumber, outY);
		    if (clamped != null)
		    	clamped.value = true;
	    } else if (w != 1) {
	        outX /= w;
	        outY /= w;
	    }

	    return new FloatPoint(outX, outY);
	}
	
	// This form preserves the double math from input to output
    double[] map(double x, double y) { 
    	return multVecMatrix(x, y); 
    }
    
    FloatPoint internalMapPoint(FloatPoint sourcePoint)
    {
        double[] result = multVecMatrix(sourcePoint.x(), sourcePoint.y());
        return new FloatPoint(result[0], result[1]);
    }

	FloatQuad projectQuad(FloatQuad q, BooleanRef clamped)
	{
	    FloatQuad projectedQuad = new FloatQuad();

	    BooleanRef clamped1 = new BooleanRef(false);
	    BooleanRef clamped2 = new BooleanRef(false);
	    BooleanRef clamped3 = new BooleanRef(false);
	    BooleanRef clamped4 = new BooleanRef(false);

	    projectedQuad.setP1(projectPoint(q.p1(), clamped1));
	    projectedQuad.setP2(projectPoint(q.p2(), clamped2));
	    projectedQuad.setP3(projectPoint(q.p3(), clamped3));
	    projectedQuad.setP4(projectPoint(q.p4(), clamped4));

	    if (clamped != null)
	        clamped.value = clamped1.value || clamped2.value || clamped3.value || clamped4.value;

	    // If all points on the quad had w < 0, then the entire quad would not be visible to the projected surface.
	    boolean everythingWasClipped = clamped1.value && clamped2.value && clamped3.value && clamped4.value;
	    if (everythingWasClipped)
	        return new FloatQuad();

	    return projectedQuad;
	}

//	static float clampEdgeValue(float f)
//	{
//	    return Math.min(Math.max(f, -LayoutUnit::max() / 2), LayoutUnit::max() / 2);
//	}

//	LayoutRect clampedBoundsOfProjectedQuad(FloatQuad& q)
//	{
//	    FloatRect mappedQuadBounds = projectQuad(q).boundingBox();
//
//	    float left = clampEdgeValue(floorf(mappedQuadBounds.x()));
//	    float top = clampEdgeValue(floorf(mappedQuadBounds.y()));
//
//	    float right;
//	    if (std::isinf(mappedQuadBounds.x()) && std::isinf(mappedQuadBounds.width()))
//	        right = LayoutUnit::max() / 2;
//	    else
//	        right = clampEdgeValue(ceilf(mappedQuadBounds.maxX()));
//
//	    float bottom;
//	    if (std::isinf(mappedQuadBounds.y()) && std::isinf(mappedQuadBounds.height()))
//	        bottom = LayoutUnit::max() / 2;
//	    else
//	        bottom = clampEdgeValue(ceilf(mappedQuadBounds.maxY()));
//
//	    return LayoutRect(LayoutUnit::clamp(left), LayoutUnit::clamp(top),  LayoutUnit::clamp(right - left), LayoutUnit::clamp(bottom - top));
//	}

	FloatPoint mapPoint(FloatPoint p)
	{
	    if (isIdentityOrTranslation())
	        return new FloatPoint(p.x() + m_matrix[3][0], p.y() + m_matrix[3][1]);

	    return internalMapPoint(p);
	}

//	FloatPoint3D mapPoint(FloatPoint3D& p)
//	{
//	    if (isIdentityOrTranslation())
//	        return FloatPoint3D(p.x() + static_cast<float>(m_matrix[3][0]),
//	                            p.y() + static_cast<float>(m_matrix[3][1]),
//	                            p.z() + static_cast<float>(m_matrix[3][2]));
//
//	    return internalMapPoint(p);
//	}

	Rect mapRect(Rect rect)
	{
		Rect resut = new Rect();
		mapRect(new RectF(rect)).round(resut);
	    return resut;
	}

//	LayoutRect mapRect(LayoutRect& r)
//	{
//	    return enclosingLayoutRect(mapRect(FloatRect(r)));
//	}

	RectF mapRect(RectF r)
	{
	    if (isIdentityOrTranslation()) {
	    	RectF mappedRect = new RectF(r);
	        mappedRect.offset((float)m_matrix[3][0], (float)m_matrix[3][1]);
	        return mappedRect;
	    }

	    FloatQuad result = new FloatQuad();

	    float maxX = r.right;
	    float maxY = r.bottom;
	    result.setP1(internalMapPoint(new FloatPoint(r.left, r.top)));
	    result.setP2(internalMapPoint(new FloatPoint(maxX, r.top)));
	    result.setP3(internalMapPoint(new FloatPoint(maxX, maxY)));
	    result.setP4(internalMapPoint(new FloatPoint(r.left, maxY)));

	    return result.boundingBox();
	}

	FloatQuad mapQuad(FloatQuad q)
	{
	    if (isIdentityOrTranslation()) {
	        FloatQuad mappedQuad = new FloatQuad(q);
	        mappedQuad.move((float)m_matrix[3][0], (float)m_matrix[3][1]);
	        return mappedQuad;
	    }

	    FloatQuad result = new FloatQuad();
	    result.setP1(internalMapPoint(q.p1()));
	    result.setP2(internalMapPoint(q.p2()));
	    result.setP3(internalMapPoint(q.p3()));
	    result.setP4(internalMapPoint(q.p4()));
	    return result;
	}

	TransformationMatrix scaleNonUniform(double sx, double sy)
	{
	    m_matrix[0][0] *= sx;
	    m_matrix[0][1] *= sx;
	    m_matrix[0][2] *= sx;
	    m_matrix[0][3] *= sx;

	    m_matrix[1][0] *= sy;
	    m_matrix[1][1] *= sy;
	    m_matrix[1][2] *= sy;
	    m_matrix[1][3] *= sy;
	    return this;
	}

	TransformationMatrix scale3d(double sx, double sy, double sz)
	{
	    scaleNonUniform(sx, sy);

	    m_matrix[2][0] *= sz;
	    m_matrix[2][1] *= sz;
	    m_matrix[2][2] *= sz;
	    m_matrix[2][3] *= sz;
	    return this;
	}

	TransformationMatrix rotate3d(double x, double y, double z, double angle)
	{
	    // Normalize the axis of rotation
	    double length = Math.sqrt(x * x + y * y + z * z);
	    if (length == 0) {
	        // A direction vector that cannot be normalized, such as [0, 0, 0], will cause the rotation to not be applied.
	        return this;
	    } else if (length != 1) {
	        x /= length;
	        y /= length;
	        z /= length;
	    }

	    // Angles are in degrees. Switch to radians.
	    angle = Math.toRadians(angle);

	    double sinTheta = Math.sin(angle);
	    double cosTheta = Math.cos(angle);

	    TransformationMatrix mat = new TransformationMatrix();

	    // Optimize cases where the axis is along a major axis
	    if (x == 1.0 && y == 0.0 && z == 0.0) {
	        mat.m_matrix[0][0] = 1.0;
	        mat.m_matrix[0][1] = 0.0;
	        mat.m_matrix[0][2] = 0.0;
	        mat.m_matrix[1][0] = 0.0;
	        mat.m_matrix[1][1] = cosTheta;
	        mat.m_matrix[1][2] = sinTheta;
	        mat.m_matrix[2][0] = 0.0;
	        mat.m_matrix[2][1] = -sinTheta;
	        mat.m_matrix[2][2] = cosTheta;
	        mat.m_matrix[0][3] = mat.m_matrix[1][3] = mat.m_matrix[2][3] = 0.0;
	        mat.m_matrix[3][0] = mat.m_matrix[3][1] = mat.m_matrix[3][2] = 0.0;
	        mat.m_matrix[3][3] = 1.0;
	    } else if (x == 0.0 && y == 1.0 && z == 0.0) {
	        mat.m_matrix[0][0] = cosTheta;
	        mat.m_matrix[0][1] = 0.0;
	        mat.m_matrix[0][2] = -sinTheta;
	        mat.m_matrix[1][0] = 0.0;
	        mat.m_matrix[1][1] = 1.0;
	        mat.m_matrix[1][2] = 0.0;
	        mat.m_matrix[2][0] = sinTheta;
	        mat.m_matrix[2][1] = 0.0;
	        mat.m_matrix[2][2] = cosTheta;
	        mat.m_matrix[0][3] = mat.m_matrix[1][3] = mat.m_matrix[2][3] = 0.0;
	        mat.m_matrix[3][0] = mat.m_matrix[3][1] = mat.m_matrix[3][2] = 0.0;
	        mat.m_matrix[3][3] = 1.0;
	    } else if (x == 0.0 && y == 0.0 && z == 1.0) {
	        mat.m_matrix[0][0] = cosTheta;
	        mat.m_matrix[0][1] = sinTheta;
	        mat.m_matrix[0][2] = 0.0;
	        mat.m_matrix[1][0] = -sinTheta;
	        mat.m_matrix[1][1] = cosTheta;
	        mat.m_matrix[1][2] = 0.0;
	        mat.m_matrix[2][0] = 0.0;
	        mat.m_matrix[2][1] = 0.0;
	        mat.m_matrix[2][2] = 1.0;
	        mat.m_matrix[0][3] = mat.m_matrix[1][3] = mat.m_matrix[2][3] = 0.0;
	        mat.m_matrix[3][0] = mat.m_matrix[3][1] = mat.m_matrix[3][2] = 0.0;
	        mat.m_matrix[3][3] = 1.0;
	    } else {
	        // This case is the rotation about an arbitrary unit vector.
	        //
	        // Formula is adapted from Wikipedia article on Rotation matrix,
	        // http://en.wikipedia.org/wiki/Rotation_matrix#Rotation_matrix_from_axis_and_angle
	        //
	        // An alternate resource with the same matrix: http://www.fastgraph.com/makegames/3drotation/
	        //
	        double oneMinusCosTheta = 1 - cosTheta;
	        mat.m_matrix[0][0] = cosTheta + x * x * oneMinusCosTheta;
	        mat.m_matrix[0][1] = y * x * oneMinusCosTheta + z * sinTheta;
	        mat.m_matrix[0][2] = z * x * oneMinusCosTheta - y * sinTheta;
	        mat.m_matrix[1][0] = x * y * oneMinusCosTheta - z * sinTheta;
	        mat.m_matrix[1][1] = cosTheta + y * y * oneMinusCosTheta;
	        mat.m_matrix[1][2] = z * y * oneMinusCosTheta + x * sinTheta;
	        mat.m_matrix[2][0] = x * z * oneMinusCosTheta + y * sinTheta;
	        mat.m_matrix[2][1] = y * z * oneMinusCosTheta - x * sinTheta;
	        mat.m_matrix[2][2] = cosTheta + z * z * oneMinusCosTheta;
	        mat.m_matrix[0][3] = mat.m_matrix[1][3] = mat.m_matrix[2][3] = 0.0;
	        mat.m_matrix[3][0] = mat.m_matrix[3][1] = mat.m_matrix[3][2] = 0.0;
	        mat.m_matrix[3][3] = 1.0;
	    }
	    multiply(mat);
	    return this;
	}

	TransformationMatrix rotate3d(double rx, double ry, double rz)
	{
	    // Angles are in degrees. Switch to radians.
	    rx = Math.toRadians(rx);
	    ry = Math.toRadians(ry);
	    rz = Math.toRadians(rz);

	    TransformationMatrix mat = new TransformationMatrix();

	    double sinTheta = Math.sin(rz);
	    double cosTheta = Math.cos(rz);

	    mat.m_matrix[0][0] = cosTheta;
	    mat.m_matrix[0][1] = sinTheta;
	    mat.m_matrix[0][2] = 0.0;
	    mat.m_matrix[1][0] = -sinTheta;
	    mat.m_matrix[1][1] = cosTheta;
	    mat.m_matrix[1][2] = 0.0;
	    mat.m_matrix[2][0] = 0.0;
	    mat.m_matrix[2][1] = 0.0;
	    mat.m_matrix[2][2] = 1.0;
	    mat.m_matrix[0][3] = mat.m_matrix[1][3] = mat.m_matrix[2][3] = 0.0;
	    mat.m_matrix[3][0] = mat.m_matrix[3][1] = mat.m_matrix[3][2] = 0.0;
	    mat.m_matrix[3][3] = 1.0;

	    TransformationMatrix rmat = new TransformationMatrix(mat);

	    sinTheta = Math.sin(ry);
	    cosTheta = Math.cos(ry);

	    mat.m_matrix[0][0] = cosTheta;
	    mat.m_matrix[0][1] = 0.0;
	    mat.m_matrix[0][2] = -sinTheta;
	    mat.m_matrix[1][0] = 0.0;
	    mat.m_matrix[1][1] = 1.0;
	    mat.m_matrix[1][2] = 0.0;
	    mat.m_matrix[2][0] = sinTheta;
	    mat.m_matrix[2][1] = 0.0;
	    mat.m_matrix[2][2] = cosTheta;
	    mat.m_matrix[0][3] = mat.m_matrix[1][3] = mat.m_matrix[2][3] = 0.0;
	    mat.m_matrix[3][0] = mat.m_matrix[3][1] = mat.m_matrix[3][2] = 0.0;
	    mat.m_matrix[3][3] = 1.0;

	    rmat.multiply(mat);

	    sinTheta = Math.sin(rx);
	    cosTheta = Math.cos(rx);

	    mat.m_matrix[0][0] = 1.0;
	    mat.m_matrix[0][1] = 0.0;
	    mat.m_matrix[0][2] = 0.0;
	    mat.m_matrix[1][0] = 0.0;
	    mat.m_matrix[1][1] = cosTheta;
	    mat.m_matrix[1][2] = sinTheta;
	    mat.m_matrix[2][0] = 0.0;
	    mat.m_matrix[2][1] = -sinTheta;
	    mat.m_matrix[2][2] = cosTheta;
	    mat.m_matrix[0][3] = mat.m_matrix[1][3] = mat.m_matrix[2][3] = 0.0;
	    mat.m_matrix[3][0] = mat.m_matrix[3][1] = mat.m_matrix[3][2] = 0.0;
	    mat.m_matrix[3][3] = 1.0;

	    rmat.multiply(mat);

	    multiply(rmat);
	    return this;
	}
	TransformationMatrix rotate(double d) { return rotate3d(0, 0, d); }
	TransformationMatrix translate(double tx, double ty)
	{
	    m_matrix[3][0] += tx * m_matrix[0][0] + ty * m_matrix[1][0];
	    m_matrix[3][1] += tx * m_matrix[0][1] + ty * m_matrix[1][1];
	    m_matrix[3][2] += tx * m_matrix[0][2] + ty * m_matrix[1][2];
	    m_matrix[3][3] += tx * m_matrix[0][3] + ty * m_matrix[1][3];
	    return this;
	}

	TransformationMatrix translate3d(double tx, double ty, double tz)
	{
	    m_matrix[3][0] += tx * m_matrix[0][0] + ty * m_matrix[1][0] + tz * m_matrix[2][0];
	    m_matrix[3][1] += tx * m_matrix[0][1] + ty * m_matrix[1][1] + tz * m_matrix[2][1];
	    m_matrix[3][2] += tx * m_matrix[0][2] + ty * m_matrix[1][2] + tz * m_matrix[2][2];
	    m_matrix[3][3] += tx * m_matrix[0][3] + ty * m_matrix[1][3] + tz * m_matrix[2][3];
	    return this;
	}

	TransformationMatrix translateRight(double tx, double ty)
	{
	    if (tx != 0) {
	        m_matrix[0][0] +=  m_matrix[0][3] * tx;
	        m_matrix[1][0] +=  m_matrix[1][3] * tx;
	        m_matrix[2][0] +=  m_matrix[2][3] * tx;
	        m_matrix[3][0] +=  m_matrix[3][3] * tx;
	    }

	    if (ty != 0) {
	        m_matrix[0][1] +=  m_matrix[0][3] * ty;
	        m_matrix[1][1] +=  m_matrix[1][3] * ty;
	        m_matrix[2][1] +=  m_matrix[2][3] * ty;
	        m_matrix[3][1] +=  m_matrix[3][3] * ty;
	    }

	    return this;
	}

	TransformationMatrix translateRight3d(double tx, double ty, double tz)
	{
	    translateRight(tx, ty);
	    if (tz != 0) {
	        m_matrix[0][2] +=  m_matrix[0][3] * tz;
	        m_matrix[1][2] +=  m_matrix[1][3] * tz;
	        m_matrix[2][2] +=  m_matrix[2][3] * tz;
	        m_matrix[3][2] +=  m_matrix[3][3] * tz;
	    }

	    return this;
	}

	TransformationMatrix skew(double sx, double sy)
	{
	    // angles are in degrees. Switch to radians
	    sx = Math.toRadians(sx);
	    sy = Math.toRadians(sy);

	    TransformationMatrix mat = new TransformationMatrix();
	    mat.m_matrix[0][1] = Math.tan(sy); // note that the y shear goes in the first row
	    mat.m_matrix[1][0] = Math.tan(sx); // and the x shear in the second row

	    multiply(mat);
	    return this;
	}

	TransformationMatrix applyPerspective(double p)
	{
	    TransformationMatrix mat = new TransformationMatrix();
	    if (p != 0)
	        mat.m_matrix[2][3] = -1/p;

	    multiply(mat);
	    return this;
	}

	TransformationMatrix rectToRect(RectF from, RectF to)
	{
	    return new TransformationMatrix((double)(to.width() / from.width()),
	                                0.0, 0.0,
	                                (double)(to.height() / from.height()),
	                                (double)(to.left- from.left),
	                                (double)(to.top - from.top));
	}

	// this = mat * this.
	TransformationMatrix multiply(TransformationMatrix mat)
	{
	    double[][] tmp = new double[4][4];

	    tmp[0][0] = (mat.m_matrix[0][0] * m_matrix[0][0] + mat.m_matrix[0][1] * m_matrix[1][0]
	               + mat.m_matrix[0][2] * m_matrix[2][0] + mat.m_matrix[0][3] * m_matrix[3][0]);
	    tmp[0][1] = (mat.m_matrix[0][0] * m_matrix[0][1] + mat.m_matrix[0][1] * m_matrix[1][1]
	               + mat.m_matrix[0][2] * m_matrix[2][1] + mat.m_matrix[0][3] * m_matrix[3][1]);
	    tmp[0][2] = (mat.m_matrix[0][0] * m_matrix[0][2] + mat.m_matrix[0][1] * m_matrix[1][2]
	               + mat.m_matrix[0][2] * m_matrix[2][2] + mat.m_matrix[0][3] * m_matrix[3][2]);
	    tmp[0][3] = (mat.m_matrix[0][0] * m_matrix[0][3] + mat.m_matrix[0][1] * m_matrix[1][3]
	               + mat.m_matrix[0][2] * m_matrix[2][3] + mat.m_matrix[0][3] * m_matrix[3][3]);

	    tmp[1][0] = (mat.m_matrix[1][0] * m_matrix[0][0] + mat.m_matrix[1][1] * m_matrix[1][0]
	               + mat.m_matrix[1][2] * m_matrix[2][0] + mat.m_matrix[1][3] * m_matrix[3][0]);
	    tmp[1][1] = (mat.m_matrix[1][0] * m_matrix[0][1] + mat.m_matrix[1][1] * m_matrix[1][1]
	               + mat.m_matrix[1][2] * m_matrix[2][1] + mat.m_matrix[1][3] * m_matrix[3][1]);
	    tmp[1][2] = (mat.m_matrix[1][0] * m_matrix[0][2] + mat.m_matrix[1][1] * m_matrix[1][2]
	               + mat.m_matrix[1][2] * m_matrix[2][2] + mat.m_matrix[1][3] * m_matrix[3][2]);
	    tmp[1][3] = (mat.m_matrix[1][0] * m_matrix[0][3] + mat.m_matrix[1][1] * m_matrix[1][3]
	               + mat.m_matrix[1][2] * m_matrix[2][3] + mat.m_matrix[1][3] * m_matrix[3][3]);

	    tmp[2][0] = (mat.m_matrix[2][0] * m_matrix[0][0] + mat.m_matrix[2][1] * m_matrix[1][0]
	               + mat.m_matrix[2][2] * m_matrix[2][0] + mat.m_matrix[2][3] * m_matrix[3][0]);
	    tmp[2][1] = (mat.m_matrix[2][0] * m_matrix[0][1] + mat.m_matrix[2][1] * m_matrix[1][1]
	               + mat.m_matrix[2][2] * m_matrix[2][1] + mat.m_matrix[2][3] * m_matrix[3][1]);
	    tmp[2][2] = (mat.m_matrix[2][0] * m_matrix[0][2] + mat.m_matrix[2][1] * m_matrix[1][2]
	               + mat.m_matrix[2][2] * m_matrix[2][2] + mat.m_matrix[2][3] * m_matrix[3][2]);
	    tmp[2][3] = (mat.m_matrix[2][0] * m_matrix[0][3] + mat.m_matrix[2][1] * m_matrix[1][3]
	               + mat.m_matrix[2][2] * m_matrix[2][3] + mat.m_matrix[2][3] * m_matrix[3][3]);

	    tmp[3][0] = (mat.m_matrix[3][0] * m_matrix[0][0] + mat.m_matrix[3][1] * m_matrix[1][0]
	               + mat.m_matrix[3][2] * m_matrix[2][0] + mat.m_matrix[3][3] * m_matrix[3][0]);
	    tmp[3][1] = (mat.m_matrix[3][0] * m_matrix[0][1] + mat.m_matrix[3][1] * m_matrix[1][1]
	               + mat.m_matrix[3][2] * m_matrix[2][1] + mat.m_matrix[3][3] * m_matrix[3][1]);
	    tmp[3][2] = (mat.m_matrix[3][0] * m_matrix[0][2] + mat.m_matrix[3][1] * m_matrix[1][2]
	               + mat.m_matrix[3][2] * m_matrix[2][2] + mat.m_matrix[3][3] * m_matrix[3][2]);
	    tmp[3][3] = (mat.m_matrix[3][0] * m_matrix[0][3] + mat.m_matrix[3][1] * m_matrix[1][3]
	               + mat.m_matrix[3][2] * m_matrix[2][3] + mat.m_matrix[3][3] * m_matrix[3][3]);

	    setMatrix(tmp);
	    return this;
	}

	double[] multVecMatrix(double x, double y)
	{
		double[] result = new double[2];
	    result[0] = m_matrix[3][0] + x * m_matrix[0][0] + y * m_matrix[1][0];
	    result[1] = m_matrix[3][1] + x * m_matrix[0][1] + y * m_matrix[1][1];
	    double w = m_matrix[3][3] + x * m_matrix[0][3] + y * m_matrix[1][3];
	    if (w != 1 && w != 0) {
	        result[0] /= w;
	        result[1] /= w;
	    }
	    return result;
	}

	double[] multVecMatrix(double x, double y, double z)
	{
		double[] result = new double[3];
		result[0] = m_matrix[3][0] + x * m_matrix[0][0] + y * m_matrix[1][0] + z * m_matrix[2][0];
		result[1] = m_matrix[3][1] + x * m_matrix[0][1] + y * m_matrix[1][1] + z * m_matrix[2][1];
		result[2] = m_matrix[3][2] + x * m_matrix[0][2] + y * m_matrix[1][2] + z * m_matrix[2][2];
	    double w = m_matrix[3][3] + x * m_matrix[0][3] + y * m_matrix[1][3] + z * m_matrix[2][3];
	    if (w != 1 && w != 0) {
	    	result[0] /= w;
	        result[1] /= w;
	        result[2] /= w;
	    }
	    return result;
	}

	boolean isInvertible()
	{
	    if (isIdentityOrTranslation())
	        return true;

	    double det = determinant4x4(m_matrix);

	    if (Math.abs(det) < SMALL_NUMBER)
	        return false;

	    return true;
	}

	TransformationMatrix inverse()
	{
	    if (isIdentityOrTranslation()) {
	        // identity matrix
	        if (m_matrix[3][0] == 0 && m_matrix[3][1] == 0 && m_matrix[3][2] == 0)
	            return new TransformationMatrix();

	        // translation
	        return new TransformationMatrix(1, 0, 0, 0,
	                                    0, 1, 0, 0,
	                                    0, 0, 1, 0,
	                                    -m_matrix[3][0], -m_matrix[3][1], -m_matrix[3][2], 1);
	    }

	    TransformationMatrix invMat = new TransformationMatrix();
	    double[][] inverted = inverse(m_matrix);
	    if (inverted != null)
		    invMat.m_matrix = inverted;
	    return invMat;
	}

	void makeAffine()
	{
	    m_matrix[0][2] = 0;
	    m_matrix[0][3] = 0;

	    m_matrix[1][2] = 0;
	    m_matrix[1][3] = 0;

	    m_matrix[2][0] = 0;
	    m_matrix[2][1] = 0;
	    m_matrix[2][2] = 1;
	    m_matrix[2][3] = 0;

	    m_matrix[3][2] = 0;
	    m_matrix[3][3] = 1;
	}

	AffineTransform toAffineTransform()
	{
	    return new AffineTransform(m_matrix[0][0], m_matrix[0][1], m_matrix[1][0],
	                           m_matrix[1][1], m_matrix[3][0], m_matrix[3][1]);
	}

	static void blendFloat(double from, double to, double progress)
	{
	    if (from != to)
	        from = from + (to - from) * progress;
	}

	void blend2(TransformationMatrix from, double progress)
	{
	    Decomposed2Type fromDecomp;
	    Decomposed2Type toDecomp;
	    fromDecomp = from.decompose2();
	    toDecomp = decompose2();

	    // If x-axis of one is flipped, and y-axis of the other, convert to an unflipped rotation.
	    if ((fromDecomp.scaleX < 0 && toDecomp.scaleY < 0) || (fromDecomp.scaleY < 0 && toDecomp.scaleX < 0)) {
	        fromDecomp.scaleX = -fromDecomp.scaleX;
	        fromDecomp.scaleY = -fromDecomp.scaleY;
	        fromDecomp.angle += fromDecomp.angle < 0 ? 180 : -180;
	    }

	    // Don't rotate the long way around.
	    if (fromDecomp.angle == 0)
	        fromDecomp.angle = 360;
	    if (toDecomp.angle == 0)
	        toDecomp.angle = 360;

	    if (Math.abs(fromDecomp.angle - toDecomp.angle) > 180) {
	        if (fromDecomp.angle > toDecomp.angle)
	            fromDecomp.angle -= 360;
	        else
	            toDecomp.angle -= 360;
	    }

	    blendFloat(fromDecomp.m11, toDecomp.m11, progress);
	    blendFloat(fromDecomp.m12, toDecomp.m12, progress);
	    blendFloat(fromDecomp.m21, toDecomp.m21, progress);
	    blendFloat(fromDecomp.m22, toDecomp.m22, progress);
	    blendFloat(fromDecomp.translateX, toDecomp.translateX, progress);
	    blendFloat(fromDecomp.translateY, toDecomp.translateY, progress);
	    blendFloat(fromDecomp.scaleX, toDecomp.scaleX, progress);
	    blendFloat(fromDecomp.scaleY, toDecomp.scaleY, progress);
	    blendFloat(fromDecomp.angle, toDecomp.angle, progress);

	    recompose2(fromDecomp);
	}

	void blend4(TransformationMatrix from, double progress)
	{
	    Decomposed4Type fromDecomp;
	    Decomposed4Type toDecomp;
	    fromDecomp = from.decompose4();
	    toDecomp = decompose4();

	    blendFloat(fromDecomp.scaleX, toDecomp.scaleX, progress);
	    blendFloat(fromDecomp.scaleY, toDecomp.scaleY, progress);
	    blendFloat(fromDecomp.scaleZ, toDecomp.scaleZ, progress);
	    blendFloat(fromDecomp.skewXY, toDecomp.skewXY, progress);
	    blendFloat(fromDecomp.skewXZ, toDecomp.skewXZ, progress);
	    blendFloat(fromDecomp.skewYZ, toDecomp.skewYZ, progress);
	    blendFloat(fromDecomp.translateX, toDecomp.translateX, progress);
	    blendFloat(fromDecomp.translateY, toDecomp.translateY, progress);
	    blendFloat(fromDecomp.translateZ, toDecomp.translateZ, progress);
	    blendFloat(fromDecomp.perspectiveX, toDecomp.perspectiveX, progress);
	    blendFloat(fromDecomp.perspectiveY, toDecomp.perspectiveY, progress);
	    blendFloat(fromDecomp.perspectiveZ, toDecomp.perspectiveZ, progress);
	    blendFloat(fromDecomp.perspectiveW, toDecomp.perspectiveW, progress);
	    
	    double[] quatFrom = new double[]{fromDecomp.quaternionX,fromDecomp.quaternionY,fromDecomp.quaternionZ,fromDecomp.quaternionW};
	    double[] quatTo = new double[]{toDecomp.quaternionX,toDecomp.quaternionY,toDecomp.quaternionZ,toDecomp.quaternionW};
	    double[] result = slerp(quatFrom, quatTo, progress);
	    fromDecomp.quaternionX = result[0];
	    fromDecomp.quaternionY = result[1];
	    fromDecomp.quaternionZ = result[2];
	    fromDecomp.quaternionW = result[3];
	    recompose4(fromDecomp);
	}

	void blend(TransformationMatrix from, double progress)
	{
	    if (from.isIdentity() && isIdentity())
	        return;

	    if (from.isAffine() && isAffine())
	        blend2(from, progress);
	    else
	        blend4(from, progress);
	}

	Decomposed2Type decompose2()
	{
	    if (isIdentity()) {
	    	return new Decomposed2Type();
	    }

	    return decompose2(m_matrix);
	}

	Decomposed4Type decompose4()
	{
	    if (isIdentity()) {
	    	return new Decomposed4Type();
	    }

	    return decompose4(m_matrix);
	}

	void recompose2(Decomposed2Type decomp)
	{
	    makeIdentity();

	    m_matrix[0][0] = decomp.m11;
	    m_matrix[0][1] = decomp.m12;
	    m_matrix[1][0] = decomp.m21;
	    m_matrix[1][1] = decomp.m22;

	    translate3d(decomp.translateX, decomp.translateY, 0);
	    rotate(decomp.angle);
	    scale3d(decomp.scaleX, decomp.scaleY, 1);
	}

	void recompose4(Decomposed4Type decomp)
	{
	    makeIdentity();

	    // First apply perspective.
	    m_matrix[0][3] = decomp.perspectiveX;
	    m_matrix[1][3] = decomp.perspectiveY;
	    m_matrix[2][3] = decomp.perspectiveZ;
	    m_matrix[3][3] = decomp.perspectiveW;

	    // Next, translate.
	    translate3d(decomp.translateX, decomp.translateY, decomp.translateZ);

	    // Apply rotation.
	    double xx = decomp.quaternionX * decomp.quaternionX;
	    double xy = decomp.quaternionX * decomp.quaternionY;
	    double xz = decomp.quaternionX * decomp.quaternionZ;
	    double xw = decomp.quaternionX * decomp.quaternionW;
	    double yy = decomp.quaternionY * decomp.quaternionY;
	    double yz = decomp.quaternionY * decomp.quaternionZ;
	    double yw = decomp.quaternionY * decomp.quaternionW;
	    double zz = decomp.quaternionZ * decomp.quaternionZ;
	    double zw = decomp.quaternionZ * decomp.quaternionW;

	    //ruct a composite rotation matrix from the quaternion values.
	    TransformationMatrix rotationMatrix = new TransformationMatrix(1 - 2 * (yy + zz), 2 * (xy - zw), 2 * (xz + yw), 0,
	                           2 * (xy + zw), 1 - 2 * (xx + zz), 2 * (yz - xw), 0,
	                           2 * (xz - yw), 2 * (yz + xw), 1 - 2 * (xx + yy), 0,
	                           0, 0, 0, 1);

	    multiply(rotationMatrix);

	    // Apply skew.
	    if (decomp.skewYZ != 0) {
	        TransformationMatrix tmp = new TransformationMatrix();
	        tmp.setM32(decomp.skewYZ);
	        multiply(tmp);
	    }

	    if (decomp.skewXZ != 0) {
	        TransformationMatrix tmp = new TransformationMatrix();
	        tmp.setM31(decomp.skewXZ);
	        multiply(tmp);
	    }

	    if (decomp.skewXY != 0) {
	        TransformationMatrix tmp = new TransformationMatrix();
	        tmp.setM21(decomp.skewXY);
	        multiply(tmp);
	    }

	    // Finally, apply scale.
	    scale3d(decomp.scaleX, decomp.scaleY, decomp.scaleZ);
	}

	boolean isIntegerTranslation()
	{
	    if (!isIdentityOrTranslation())
	        return false;

	    // Check for translate Z.
	    if (m_matrix[3][2] != 0)
	        return false;

	    // Check for non-integer translate X/Y.
	    if (m_matrix[3][0] != m_matrix[3][0] || m_matrix[3][1] != m_matrix[3][1])
	        return false;

	    return true;
	}

	TransformationMatrix to2dTransform()
	{
	    return new TransformationMatrix(m_matrix[0][0], m_matrix[0][1], 0, m_matrix[0][3],
	                                m_matrix[1][0], m_matrix[1][1], 0, m_matrix[1][3],
	                                0, 0, 1, 0,
	                                m_matrix[3][0], m_matrix[3][1], 0, m_matrix[3][3]);
	}

	double[] toColumnMajorFloatArray()
	{
		double[] result = new double[16];
	    result[0] = m11();
	    result[1] = m12();
	    result[2] = m13();
	    result[3] = m14();
	    result[4] = m21();
	    result[5] = m22();
	    result[6] = m23();
	    result[7] = m24();
	    result[8] = m31();
	    result[9] = m32();
	    result[10] = m33();
	    result[11] = m34();
	    result[12] = m41();
	    result[13] = m42();
	    result[14] = m43();
	    result[15] = m44();
	    return result;
	}

	boolean isBackFaceVisible()
	{
	    // Back-face visibility is determined by transforming the normal vector (0, 0, 1) and
	    // checking the sign of the resulting z component. However, normals cannot be
	    // transformed by the original matrix, they require being transformed by the
	    // inverse-transpose.
	    //
	    // Since we know we will be using (0, 0, 1), and we only care about the z-component of
	    // the transformed normal, then we only need the m33() element of the
	    // inverse-transpose. Therefore we do not need the transpose.
	    //
	    // Additionally, if we only need the m33() element, we do not need to compute a full
	    // inverse. Instead, knowing the inverse of a matrix is adjoint(matrix) / determinant,
	    // we can simply compute the m33() of the adjoint (adjugate) matrix, without computing
	    // the full adjoint.

	    double determinant = determinant4x4(m_matrix);

	    // If the matrix is not invertible, then we assume its backface is not visible.
	    if (Math.abs(determinant) < SMALL_NUMBER)
	        return false;

	    double cofactor33 = determinant3x3(m11(), m12(), m14(), m21(), m22(), m24(), m41(), m42(), m44());
	    double zComponentOfTransformedNormal = cofactor33 / determinant;

	    return zComponentOfTransformedNormal < 0;
	}
}
