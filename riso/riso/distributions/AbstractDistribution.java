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
import java.rmi.server.*;
import java.util.*;
import riso.approximation.*;
import riso.belief_nets.*;
import SmarterTokenizer;

/** Abstract base class for unconditional distributions.
  * Since Distribution is derived from ConditionalDistribution, any
  * unconditional distribution must implement all of the functions defined
  * for conditional distributions. Some of these have trivial implementations,
  * which are given here. It would be cleaner, perhaps, to put these in
  * the definition of Distribution, but Java doesn't allow code in an
  * interface... <sigh>. So here they are.
  */
abstract public class AbstractDistribution implements Distribution, Serializable
{
	/** This distribution is associated with the belief network variable <tt>associated_variable</tt>.
	  * This reference is necessary for some distributions, and generally useful for debugging.
	  */
	public AbstractVariable associated_variable;

	/** Cache a reference to the variable with which this distribution
	  * is associated.
	  */
	public void set_variable( Variable x ) { associated_variable = x; }

	/** The "child" in this case is just the variable itself.
	  * So <tt>ndimensions_child</tt> equals <tt>ndimensions</tt>.
	  */
	public int ndimensions_child() { return ndimensions(); }

	/** There are no parents, so return zero.
	  */
	public int ndimensions_parent() { return 0; }

	/** Context doesn't matter since this distribution is unconditional.
	  * Return <tt>this</tt> for every value of <tt>c</tt>.
	  */
	public Distribution get_density( double[] c ) { return this; }

	/** Compute the cumulative distribution function.
	  * @throws Exception with message "not implemented."
	  */
	public double cdf( double x ) throws Exception
	{
		throw new Exception( this.getClass().getName()+".cdf: not implemented." );
	}

	/** Compute the density at the point <code>x</code>.
	  * Ignore the context <tt>c</tt>.
	  */
	public double p( double[] x, double[] c ) throws Exception { return p(x); }

	/** Return an instance of a random variable from this distribution.
	  * Ignore the context <tt>c</tt>.
	  */
	public double[] random( double[] c ) throws Exception { return random(); }

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, assuming some prior distribution has been 
	  * established. This may not be meaningful for all distributions.
	  */
	public double log_prior() throws Exception
	{
		throw new Exception( getClass()+".log_prior: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  */
	public Object clone() throws CloneNotSupportedException 
	{
		throw new CloneNotSupportedException( getClass()+".clone: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  * @exception Exception
	  */
	public int ndimensions()
	{
		throw new RuntimeException( getClass()+".ndimensions: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  * @exception Exception
	  */
	public double[] random() throws Exception
	{
		throw new Exception( getClass()+".random: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  * @exception Exception
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( getClass()+".update: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  * @exception IOException
	  */
	public void parse_string( String description ) throws IOException
	{
		throw new IOException( getClass()+".parse_string: not implemented." );
	}

	/** This implementation is just a place-holder; an exception is
	  * always thrown.
	  * @exception IOException
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		throw new IOException( getClass()+".pretty_input: not implemented." );
	}

	/** Print the class name.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	/** Put the class name into a string.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		return getClass()+"\n";
	}

	/** Returns the expected value of this distribution.
	  */
	public double expected_value() throws Exception
	{
		throw new Exception( getClass()+".expected_value: not implemented." );
	}

	/** Returns the square root of the variance of this distribution.
	  */
	public double sqrt_variance() throws Exception
	{
		throw new Exception( getClass()+".sqrt_variance: not implemented." );
	}

	/** Returns the support of this distribution, if it is a finite interval;
	  * otherwise returns an interval which contains almost all of the mass.
	  * @param epsilon If an approximation is made, this much mass or less
	  *   lies outside the interval which is returned.
	  * @return An interval represented as a 2-element array.
	  */
	public double[] effective_support( double epsilon ) throws Exception
	{
		throw new Exception( getClass()+".effective_support: not implemented." );
	}

	/** This method constructs a generic approximation for this distribution.
	  * Look for bumps in the density, and assign a Gaussian to each bump;
	  * also "pave" the effective support with uniformly spaced, wide Gaussians.
	  * A derived class should override this method if a more specific
	  * approximation is needed.
	  * @param support If this argument is null, compute the effective support.
	  */
	public MixGaussians initial_mix( double[] support ) throws Exception
	{
		if ( support == null ) support = effective_support( 1e-4 );
System.err.println( getClass().getName()+".initial_mix: support: "+support[0]+", "+support[1] );
		Vector q_vector = new Vector(), bump_ht = new Vector();
		int i, npavers = 7, ngrid = 500;
		
		// Look for regions of high density.

		double dx = (support[1]-support[0])/ngrid;
		double[] px = new double[ ngrid ], x1 = new double[1];

		for ( i = 0; i < ngrid; i++ )
		{
			x1[0] = support[0]+(i+0.5)*dx;
			px[i] = p(x1);
		}

		for ( i = 1; i < ngrid-1; i++ )
		{
			// ??? if ( px[i-2] < px[i-1] && px[i-1] < px[i] && px[i] > px[i+1] && px[i+1] > px[i+2] )
			if ( px[i-1] < px[i] && px[i] > px[i+1] )
			{
				x1[0] = support[0]+(i+0.5)*dx;

				// ESTIMATE 2ND DERIVATIVE OF THE PROBABILITY DENSITY AND SET SIGMA ACCORDINGLY !!!
				// COULD USE A MORE ACCURATE FORMULA !!!

				double dp2 = (px[i-1] - 2*px[i] + px[i+1])/(dx*dx);
				double s = 1 / Math.pow( -dp2, 1/3.0 ) / Math.pow( 2*Math.PI, 1/6.0 );
System.err.println( getClass().getName()+".initial_mix: may be bump at "+x1[0]+"; take stddev = "+s+". p0, p1, p2: "+px[i-1]+", "+px[i]+", "+px[i+1] );
				q_vector.addElement( new Gaussian( x1[0], s ) );
				bump_ht.addElement( new Double(px[i]) );
			}
		}

		int nbumps = q_vector.size();

		// Pave over support, in case bumps are too widely spread.

		double s = (support[1] - support[0])/npavers/2.0;

		for ( i = 0; i < npavers; i++ )
			q_vector.addElement( new Gaussian( support[0]+(2*i+1)*s, s ) );

		MixGaussians q = null;
		q = new MixGaussians( 1, q_vector.size() );
		q_vector.copyInto( q.components );

		// Now fudge the mixing coefficients so that the pavement gets
		// less weight than the bumps. The bumps precede the pavement
		// in the list of components.

		for ( i = 0; i < nbumps; i++ )
		{
			double h = ((Double)bump_ht.elementAt(i)).doubleValue(); // <barf>
			q.mix_proportions[i] += h * q.components[i].sqrt_variance() * Math.sqrt(2*Math.PI);
		}

		double sum = 0;
		for ( i = 0; i < q.mix_proportions.length; i++ ) sum += q.mix_proportions[i];
		for ( i = 0; i < q.mix_proportions.length; i++ ) q.mix_proportions[i] /= sum;

		return q;
	}
}
