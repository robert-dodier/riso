/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999, Robert Dodier.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA, 02111-1307, USA,
 * or visit the GNU web site, www.gnu.org.
 */
package riso.distributions;
import java.io.*;
import java.util.*;
import riso.general.*;

/** This class is the superclass of all class which represent functional relations.
  * A functional relation is a conditional distribution which has a conditional
  * distribution which is a delta function. The location of the delta is determined
  * by the <tt>f</tt> variable.
  */
public abstract class FunctionalRelation extends AbstractConditionalDistribution
{
	/** This is the number of grid points over <tt>[a,b]</tt> in which we seek 
	  * sign changes of <tt>c - F(x)</tt>. This parameter is used by <tt>component_roots</tt>.
	  */
	int NGRID = 100;

	/** The function which defines the functional relation.
	  * Subclasses must override this method.
	  */
	public abstract double F( double[] x ) throws Exception;

	/** This method returns the gradient of <tt>F</tt> evaluated at <tt>x</tt>.
	  * Subclasses must override this method.
	  */
	public abstract double[] dFdx( double[] x ) throws Exception;

	/** This method searches for values of <tt>x[k]</tt> in the interval <tt>[a,b]</tt>
	  * such that <tt>F(x) = c</tt>. 
	  * An array is returned containing as many roots as were found; if no roots were found,
	  * this method returns a zero-length array.
	  *
	  * <p> The interval <tt>[a,b]</tt> is typically the effective support of a probability density.
	  *
	  * <p> The default implementation of this method uses a 1-dimensional numerical search to
	  * locate roots of the equation <tt>F(x) = c</tt> in the interval <tt>[a,b]</tt>, 
	  * so subclass need not override this method. However, if a better search method is known,
	  * it should be implemented.
	  */
	public double[] component_roots( double c, int k, double a, double b, double[] x_in ) throws Exception
	{
		double[] x = (double[]) x_in.clone();

		// First throw down a large number of evenly spread over [a,b]
		// and look for sign changes. Then zero in on the intervals which contain sign changes.

		Vector sign_changes = new Vector(), roots = new Vector();
		double dx = (b-a)/NGRID;

		x[k] = a;
		double F_left = F(x), F_right;
		if ( F_left == c ) roots.addElement( new Double(x[k]) );

		for ( int i = 0; i < NGRID; i++ )
		{
			x[k] = a + (i+1)*dx;
			F_right = F(x);
			
			if ( (F_left < c && c < F_right) || (F_left > c && c > F_right) )
				sign_changes.addElement( new Integer(i) );
			else if ( F_right == c )
				roots.addElement( new Double(x[k]) );

			F_left = F_right;
		}

		for ( int i = 0; i < sign_changes.size(); i++ )
		{
			int ii = ((Integer)sign_changes.elementAt(i)).intValue();
			double left = a + ii*dx, right = left+dx;

			// USE BISECTION. SHOULD USE A SMARTER SEARCH ALGORITHM HERE !!!

			double eps = (b-a)*1e-8;

			x[k] = left; F_left = F(x);
			x[k] = right; F_right = F(x);

			double mid = left + (right-left)/2; // give it a definite value in case right-left <= eps.

			while ( right-left > eps )
			{
				mid = left + (right-left)/2;
				x[k] = mid;
				double F_mid = F(x);
				
				if ( F_mid == c )
					break;
				else if ( (F_left < c && c < F_mid) || (F_left > c && c > F_mid) )
				{
					F_right = F_mid;
					right = mid;
				}
				else
				{
					F_left = F_mid;
					left = mid;
				}
			}

			roots.addElement( new Double(mid) );
		}

		double[] xx = new double[ roots.size() ];
		for ( int i = 0; i < roots.size(); i++ ) xx[i] = ((Double)roots.elementAt(i)).doubleValue();

// System.err.print( "FunctionalRelation.component_roots: for y = "+c+", x = " );
// for ( int i = 0; i < x.length; i++ ) System.err.print( x[i]+" " );
// System.err.print( "k = "+k+", found "+xx.length+" roots; " );
// for ( int i = 0; i < xx.length; i++ ) System.err.print( xx[i]+" " );
// System.err.println( "(searched ["+a+", "+b+"]" );
		return xx;
	}

	/** Return a copy of this object.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		FunctionalRelation copy = (FunctionalRelation) super.clone();
		copy.NGRID = this.NGRID;
		return copy;
	}

	/** Return the number of dimensions of the child variable. Always returns 1.
	  */
	public int ndimensions_child() { return 1; }

	/** For a given value <code>c</code> of the parents, return a Gaussian delta distribution
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  * @param c Values of parent variables.
	  */
	public Distribution get_density( double[] c ) throws Exception { return new GaussianDelta(F(c)); }

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  */
	public double p( double[] x, double[] c ) throws Exception
	{
		if ( x[0] == F(c) )
			return Double.POSITIVE_INFINITY;
		else
			return 0;
	}

	/** Return an instance of a random variable from this distribution.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws Exception
	{
		double[] x = new double[1];
		x[0] = F(c);
		return x;
	}

	/** Returns a string of the form <tt>classname { }</tt>.
	  * @param leading_ws This argument is ignored.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		String result = this.getClass().getName()+" { }\n";
		return result;
	}
}
