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
import riso.numerical.*;
import riso.general.*;

/** An instance of this class approximates a density function by a spline.
  */
public class SplineDensity extends AbstractDistribution implements Translatable
{
	protected boolean expected_value_OK = false, sqrt_variance_OK = false;
	protected double expected_value_result, sqrt_variance_result;

	protected static double sqr( double x )  { return x*x; }

	/** The spline function.
	  */
	public MonotoneSpline spline = null;

	/** <tt>Class.forName()</tt> uses this do-nothing constructor.
	  */
	public SplineDensity() {}

	/** Construct a density approximation from the specified list of pairs
	  * <tt>(x,p(x))</tt>. Fudge the spline parameters so that this spline density
	  * integrates to unity.
	  */
	public SplineDensity( double[] x, double[] px ) throws Exception
	{
		spline = new MonotoneSpline( x, px );
		double total = cdf0( x[ x.length-1 ] );

		for ( int i = 0; i < x.length; i++ )
		{
			spline.f[i] /= total;
			spline.d[i] /= total;
			spline.alpha2[i] /= total;
			spline.alpha3[i] /= total;
		}
	}

	/** Returns the number of dimensions in which this distribution lives.
	  * Always returns 1.
	  */
	public int ndimensions() { return 1; }

	/** Computes the density at the point <code>x</code>. If <tt>x</tt> is outside the
	  * support of the spline, return zero.
	  * @param x Point at which to evaluate density -- must
	  *   be a one-element array.
	  */
	public double p( double[] x ) throws Exception
	{
		if ( x[0] < spline.x[0] || x[0] > spline.x[spline.x.length-1] ) return 0;

		return spline.f( x[0] );
	}

	/** Compute the cumulative distribution function.
	  * If <tt>x</tt> is to the right of the support of this spline,
	  * skip the computation and return 0; to the left, return 1.
	  */
	public double cdf( double x ) throws Exception
	{
		if ( x <= spline.x[0] ) return 0;
		if ( x >= spline.x[ spline.x.length-1 ] ) return 1;

		return cdf0(x);
	}

	/** Compute the cumulative distribution function.
	  */
	public double cdf0( double x ) throws Exception
	{
		double sum = 0;
		int i;

		// Do complete intervals first. Could cache these results; hmm.
		for ( i = 0; spline.x[i+1] < x; i++ )
		{
			double dx  = spline.x[i+1] - spline.x[i];
			double dx2 = dx*dx, dx3 = dx*dx2, dx4 = dx*dx3;

			double term = dx*spline.f[i] + dx2*spline.d[i]/2 + dx3*spline.alpha2[i]/3 + dx4*spline.alpha3[i]/4;
			sum += term;
		}

		// Now do the last partial interval.
		double dx  = x - spline.x[i];
		double dx2 = dx*dx, dx3 = dx*dx2, dx4 = dx*dx3;

		double term = dx*spline.f[i] + dx2*spline.d[i]/2 + dx3*spline.alpha2[i]/3 + dx4*spline.alpha3[i]/4;
		sum += term;

		return sum;
	}

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, assuming some prior distribution has been 
	  * established. This method is not implemented.
	  */
	public double log_prior() throws Exception
	{
		throw new Exception( "SplineDensity.log_prior: not implemented." );
	}

	/** Return an instance of a random variable from this distribution.
	  * This method is not implemented.
	  */
	public double[] random() throws Exception
	{
		throw new Exception( "SplineDensity.random: not implemented." );
	}

	/** Use data to modify the parameters of the distribution.
	  * This method is not implemented.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "SplineDensity.update: not implemented." );
	}

	/** Returns the expected value of this distribution.
	  */
	public double expected_value() 
	{
		// Retrieve the cached result, if possible. Otherwise we need to compute it.

		if ( ! expected_value_OK )
		{
			double sum = 0;
			for ( int i = 0; i < spline.x.length-1; i++ )
			{
				double dx = spline.x[i+1] - spline.x[i];
				double dx2 = dx*dx, dx3 = dx*dx2, dx4 = dx*dx3, dx5 = dx*dx4;

				double term1 = dx*spline.f[i] + dx2*spline.d[i]/2 + dx3*spline.alpha2[i]/3 + dx4*spline.alpha3[i]/4;
				double term2 = dx2*spline.f[i]/2 + dx3*spline.d[i]/6 + dx4*spline.alpha2[i]/12 + dx5*spline.alpha3[i]/20;
				sum += spline.x[i+1]*term1 - term2;
			}

			expected_value_result = sum;
			expected_value_OK = true;
		}

		return expected_value_result;
	}

	/** Returns the square root of the variance of this distribution.
	  */
	public double sqrt_variance()
	{
		// Retrieve the cached result, if possible. Otherwise we need to compute it.

		if ( ! sqrt_variance_OK )
		{
			double sum = 0, sum2 = 0;
			for ( int i = 0; i < spline.x.length-1; i++ )
			{
				double dx = spline.x[i+1] - spline.x[i];
				double dx2 = dx*dx, dx3 = dx*dx2, dx4 = dx*dx3, dx5 = dx*dx4, dx6 = dx*dx5;

				double term1 = dx*spline.f[i] + dx2*spline.d[i]/2 + dx3*spline.alpha2[i]/3 + dx4*spline.alpha3[i]/4;
				double term2 = dx2*spline.f[i]/2 + dx3*spline.d[i]/6 + dx4*spline.alpha2[i]/12 + dx5*spline.alpha3[i]/20;
				double term3 = dx3*spline.f[i]/6 + dx4*spline.d[i]/24 + dx5*spline.alpha2[i]/60 + dx6*spline.alpha3[i]/120;

				sum  += spline.x[i+1]*term1 - term2;
				sum2 += sqr(spline.x[i+1])*term1 - 2*spline.x[i+1]*term2 + 2*term3;
			}

			sqrt_variance_result = Math.sqrt( sum2 - sum*sum );
			sqrt_variance_OK = true;
		}

		return sqrt_variance_result;
	}

	/** Returns the support of the spline.
	  */
	public double[] effective_support( double epsilon ) throws Exception
	{
		double[] support = new double[2];
		support[0] = spline.x[0];
		support[1] = spline.x[ spline.x.length-1 ];
		return support;
	}

	/** Formats the parameters of the spline into a string.
	  */
	public String format_string( String leading_ws )
	{
		StringBuffer result = new StringBuffer(5*20*spline.x.length+200);
		String more_ws = leading_ws+"\t";

		result.append( this.getClass().getName()+"\n"+leading_ws+"{"+"\n" );
		result.append( more_ws+"% x\tf\td\talpha2\talpha3; "+(spline.x.length-1)+" intervals\n" );

		for ( int i = 0; i < spline.x.length; i++ )
			result.append(more_ws).append(spline.x[i]).append("\t").append(spline.f[i]).append("\t").append(spline.d[i]).append("\t").append(spline.alpha2[i]).append("\t").append(spline.alpha3[i]).append("\n");

		result.append("}"+"\n");
		return result.toString();
	}

	/** Read an instance of this distribution from an input stream.
	  * This method is not implemented.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		st.nextToken(); // eat left brace.
		if ( st.ttype != '{' ) throw new IOException( "SplineDensity.pretty_input: no left brace; tokenizer state: "+st );

		// Figure out how many tokens there are before the right brace.
		// Divide by five to get the number of support points, then parse
		// the tokens into the x, f, d, alpha2, and alpha3.

		Vector tokens = new Vector(5000); // try to avoid reallocations, since that's slow

		for ( st.nextToken(); st.ttype != '}' && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			tokens.addElement( st.sval );

		if ( st.ttype == StreamTokenizer.TT_EOF ) throw new IOException( "SplineDensity.pretty_input: unexpected end of file." );

		int n = tokens.size();
		if ( n % 5 != 0 ) System.err.println( "SplineDensity.pretty_input: hmm, number of tokens "+n+" is not a multiple of 5; round downward and stagger on." );
		int m = n/5;

		double[] x = new double[m], px = new double[m];

		Enumeration e = tokens.elements();
		for ( int i = 0; i < m; i++ )
		{
			x[i] = Double.parseDouble( (String) e.nextElement() );
			px[i] = Double.parseDouble( (String) e.nextElement() );
			e.nextElement(); e.nextElement(); e.nextElement(); // BURN OFF d, alpha2, alpha3 !!!
		}

		try { spline = new MonotoneSpline( x, px ); } // RECOMPUTE d, alpha2, alpha3 HERE !!!
		catch (Exception ex)
		{
			ex.printStackTrace();
			throw new IOException( "SplineDensity.pretty_input: failed, "+ex );
		}
	}

	/** Move the support interval of this distribution from <tt>[x0,x1]</tt>
	  * to <tt>x0+a,x1+a</tt>. This is accomplished by moving the support points and 
	  * adjusting the expected value (if cached); since the coefficients are all 
	  * computed from differences of the support points, they don't need to be recomputed.
	  */
	public void translate( double a )
	{
		if ( expected_value_OK ) expected_value_result += a;

		for ( int i = 0; i < spline.x.length; i++ ) spline.x[i] += a;
	}
}
