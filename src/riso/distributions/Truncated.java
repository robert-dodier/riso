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
import java.rmi.*;
import java.util.*;
import riso.numerical.*;
import riso.general.*;

class Xpx implements Callback_1d
{
	Distribution d;
	Xpx( Distribution d ) { this.d = d; }
	double[] x = new double[1];
	public double f( double x_in ) throws Exception { x[0] = x_in; return x_in*d.p(x); }
}

class X2px implements Callback_1d
{
	Distribution d;
	X2px( Distribution d ) { this.d = d; }
	double[] x = new double[1];
	public double f( double x_in ) throws Exception { x[0] = x_in; return x_in*x_in*d.p(x); }
}

/** An instance of this class represents a truncated distribution;
  * the instance holds a reference to the distribution in question,
  * and the effective support, mean value, etc., are computed from
  * appropriate methods of the underlying distribution. 
  */
public class Truncated extends AbstractDistribution
{
	double expected_value_cache;
	boolean expected_value_known = false;
	double variance_cache;
	boolean variance_known = false;

	/** The left end of the interval of truncation.
	  */
	public double left;

	/** The right end of the interval of truncation.
	  */
	public double right;

	/** The cdf evaluated at the left end of the interval of truncation.
	  */
	double d_cdf_left = -1;		// -1 shows cdf is not yet evaluated.

	/** The cdf evaluated at the right end of the interval of truncation.
	  */
	double d_cdf_right = -1;	// -1 shows cdf is not yet evaluated.

	/** The underlying distribution.
	  */
	public Distribution d;

	/** Empty constructor so objects can be constructed from description files.
	  */
	public Truncated() {}

	/** Constructs a <tt>Truncated</tt> given the specified distribution and 
	  * left and right limits.
	  */
	public Truncated( Distribution d, double left, double right )
	{
		this.d = d;
		this.left = left;
		this.right = right;
	}

	/** Returns the number of dimensions in which this distribution lives.
	  * Always returns 1.
	  */
	public int ndimensions() { return 1; }

	/** Return a copy of this object. <tt>super.clone</tt> handles the generic copy,
	  * and this method copies only the class-specific data.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		Truncated copy = (Truncated) super.clone();
		copy.left = this.left;
		copy.right = this.right;
		copy.d = (Distribution) this.d.clone();
		return copy;
	}

	/** Compute cumulative distribution function for this distribution.
	  */
	public double cdf( double x ) throws Exception
	{
		if ( x <= left ) return 0;
		if ( x >= right ) return 1;

		if ( d_cdf_left < 0 ) d_cdf_left = d.cdf(left);
		if ( d_cdf_right < 0 ) d_cdf_right = d.cdf(right);
		return (d.cdf(x) - d_cdf_left)/(d_cdf_right - d_cdf_left);
	}

	/** Computes the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density -- must
	  *   be a one-element array.
	  */
	public double p( double[] x ) throws Exception
	{
		if ( x[0] < left || x[0] > right ) return 0;

		if ( d_cdf_left < 0 ) d_cdf_left = d.cdf(left);
		if ( d_cdf_right < 0 ) d_cdf_right = d.cdf(right);
		return d.p(x)/(d_cdf_right - d_cdf_left);
	}

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, assuming some prior distribution has been 
	  * established. This may not be meaningful for all distributions.
	  */
	public double log_prior() throws Exception
	{
		throw new Exception( "Truncated.log_prior: not implemented." );
	}

	/** Return an instance of a random variable from this distribution.
	  */
	public double[] random() throws Exception
	{
		double[] x;
		while ( (x = d.random())[0] < left || x[0] > right );
		return x;
	}

	/** Use data to modify the parameters of the distribution.
	  * This method is not implemented.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "Truncated.update: not implemented." );
	}

	/** Returns the expected value of this distribution.
	  * The result is computed numerically, whatever the underlying 
	  * distribution. A method in <tt>Distribution</tt> to return the
	  * antiderivative of <tt>x p(x)</tt> would be helpful. !!!
	  */
	public double expected_value() throws Exception
	{
		if ( expected_value_known ) return expected_value_cache;

		double[][] support = new double[1][];
		support[0] = effective_support(1e-6);
		IntegralHelper1d ih1d = new IntegralHelper1d( new Xpx(this), support, d instanceof Discrete );
		expected_value_cache = ih1d.do_integral();
		expected_value_known = true;
		return expected_value_cache;
	}

	/** Returns the square root of the variance of this distribution. 
	  * The result is computed numerically, whatever the underlying 
	  * distribution. A method in <tt>Distribution</tt> to return the
	  * antiderivative of <tt>x^2 p(x)</tt> would be helpful. !!!
	  */
	public double sqrt_variance() throws Exception
	{	
		if ( variance_known ) return Math.sqrt(variance_cache);

		double[][] support = new double[1][];
		support[0] = effective_support(1e-6);
		IntegralHelper1d ih1d = new IntegralHelper1d( new X2px(this), support, d instanceof Discrete );
		double Ex2 = ih1d.do_integral(), Ex = expected_value();
		variance_cache = Ex2 - Ex*Ex;
		variance_known = true;
		return Math.sqrt(variance_cache);
	}

	/** Returns an interval which contains almost all the mass of this
	  * distribution.
	  *
	  * @param epsilon This much mass or less lies outside the interval
	  *   which is returned.
	  * @return An interval represented as a 2-element array.
	  */
	public double[] effective_support( double epsilon ) throws Exception
	{
		double x0 = left, x1 = right;

		// Now try to make [x0,x1] smaller, while still containing mass 1-epsilon.
		// First chop off upper tail, then chop off lower tail.

		double z0 = x0, z1 = x1, zm = x1;
		for ( int i = 0; i < 20; i++ ) // reduce initial interval by 10^(-6) factor.
		{
			zm = z0 + (z1-z0)/2;
			if ( this.cdf(zm) > 1-epsilon/2 )
				z1 = zm;
			else 
				z0 = zm;
		}
		x1 = zm;

		z0 = x0; z1 = x1;
		for ( int i = 0; i < 20; i++ ) // reduce initial interval by 10^(-6) factor.
		{
			zm = z0 + (z1-z0)/2;
			if ( this.cdf(zm) > epsilon/2 )
				z1 = zm;
			else 
				z0 = zm;
		}
		x0 = zm;
System.err.println( "\t"+"final: "+this.cdf(x0)+" below "+x0+", "+(1-this.cdf(x1))+" above "+x1 );

		double[] interval = new double[2];
		interval[0] = x0;
		interval[1] = x1;
		return interval;
	}

	/** Formats a string representation of this distribution.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		String result = "";
		result += this.getClass().getName()+" ";
		result += left+" "+right+" "+d.format_string(leading_ws);
		return result;
	}

	/** Read an instance of this distribution from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param st Stream tokenizer to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;
		Vector plist = new Vector();

		try
		{
			st.nextToken();
			left = Double.parseDouble( st.sval );
			st.nextToken();
			right = Double.parseDouble( st.sval );

			st.nextToken();
			Class c = java.rmi.server.RMIClassLoader.loadClass(st.sval);
			d = (Distribution) c.newInstance();
			st.nextBlock();
			d.parse_string(st.sval);
		}
		catch (Exception e)
		{
			throw new IOException( "Truncated.pretty_input: attempt to read object failed:\n"+e );
		}
	}
}
