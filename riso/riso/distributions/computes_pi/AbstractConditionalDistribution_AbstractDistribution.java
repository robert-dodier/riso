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
package riso.distributions.computes_pi;
import java.rmi.*;
import java.util.*;
import riso.distributions.*;
import riso.approximation.*;
import riso.numerical.*;
import riso.general.*;

/** @see PiHelper
 */
public class AbstractConditionalDistribution_AbstractDistribution implements PiHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>AbstractConditionalDistribution</tt>
	  * followed by any number of <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.AbstractConditionalDistribution", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", -1 );
		description_array = s;
	}

	public Distribution compute_pi( ConditionalDistribution pxu, Distribution[] pi_messages ) throws Exception
	{
		IntegralCache integral_cache = new IntegralCache( pxu, pi_messages );
		MixGaussians q = integral_cache.initial_mix( null );
		GaussianMixApproximation.debug = true;
		q = GaussianMixApproximation.do_approximation( integral_cache, q, integral_cache.merged_support, 1e-4 );

// double[][] xy = integral_cache.cache.dump();
// double[] x = new double[1];
// System.err.println( "computes_pi: dump: (eval at) (exact) (approx)" );
// for ( int i = 0; i < xy.length; i++ ) {
// x[0] = xy[i][0];
// System.err.println( xy[i][0]+"\t"+xy[i][1]+"\t"+q.p(x) ); }

		return q;
	}
}

class IntegralCache extends AbstractDistribution implements Callback_1d
{
	public ConditionalDistribution conditional;
	public Distribution[] pi_messages;
	public double[] x1;

	double[] u_known, u1, a, b;
	double[][] merged_support;

	boolean support_known = false;
	boolean[] is_discrete, skip_integration;

	FunctionCache cache;
	Integral integral;
	Integral.Integrand integrand;

	public IntegralCache( ConditionalDistribution conditional, Distribution[] pi_messages ) throws Exception
	{
// System.err.println( "AbsCondDist_AbsDist.IntegralCache: constructor called." );
		int i;

		x1 = new double[1];
		u1 = new double[1];

		this.conditional = conditional;
		this.pi_messages = pi_messages;

		is_discrete = new boolean[ pi_messages.length ];
		for ( i = 0; i < pi_messages.length; i++ )
			is_discrete[i] = (pi_messages[i] instanceof Discrete);

		u_known = new double[ pi_messages.length ];
		skip_integration = new boolean[ pi_messages.length ];
		for ( i = 0; i < pi_messages.length; i++ )
		{
			if ( pi_messages[i] instanceof Delta )
			{
				skip_integration[i] = true;
				u_known[i] = ((Delta)pi_messages[i]).get_support()[0];
System.err.println( "\t"+"set u_known["+i+"] to "+u_known[i] );
			}
		}

		a = new double[ pi_messages.length ];
		b = new double[ pi_messages.length ];

		for ( i = 0; i < pi_messages.length; i++ )
		{
			double[] support_i = pi_messages[i].effective_support( 1e-4 );
			a[i] = support_i[0];
			b[i] = support_i[1];
		}

		integral = this. new Integral();
		cache = new FunctionCache( -1, -1, integral );
	}

	public double p( double[] x_in ) throws Exception
	{
		return f( x_in[0] );
	}

	public double f( double x ) throws Exception
	{
		return cache.lookup( x );
	}

	public int ndimensions() { return 1; }

	/** Computes an approximate support for this distribution. 
	  * The approximation is taken as the union of the supports of 
	  * a number of cross-sections, evaluated for random 
	  * combinations of parents. The actual effective support is probably
	  * somewhat smaller than what is returned by this method.
	  */ 
	public double[] effective_support( double tolerance ) throws Exception
	{
		int i, j;

		if ( support_known )
		{
			// HACK !!! NEED TO UPDATE effective_support TO RETURN 
			// double[][] !!! USE THIS TIL THEN !!!
			double[] return_support = new double[2];
			return_support[0] = merged_support[0][0];
			return_support[1] = merged_support[merged_support.length-1][1];
			return return_support;
		}

		int nxsections_per_parent = 100;
		int nparents = pi_messages.length;
		int ncombo = 1;

		for ( i = 0; i < pi_messages.length; i++ )
			if ( skip_integration[i] )
				ncombo *= 1;
			else
			{
				if ( pi_messages[i] instanceof Discrete )
					ncombo *= ((Discrete)pi_messages[i]).probabilities.length;
				else
					ncombo *= nxsections_per_parent;
			}

		double[] u = new double[ nparents ];
		Vector random_supports = new Vector();
		double[][] parent_support = new double[ nparents ][];

		for ( j = 0; j < nparents; j++ )
			try { parent_support[j] = pi_messages[j].effective_support( tolerance ); }
			catch (Exception e) { throw new Exception( "IntegralCache.effective_support: failed: "+e ); }

		for ( i = 0; i < ncombo; )
		{
			for ( j = 0; j < nparents; j++ )
			{
				if ( skip_integration[j] )
					u[j] = u_known[j];
				else
				{
					if ( is_discrete[j] )
						// Generate random integer in parent support.
						u[j] = uniform_random_integer( parent_support[j] );
					else
						// Generate random floating point in parent support.
						u[j] = uniform_random_float( parent_support[j] );
				}
			}

			try
			{
				Distribution xsection = conditional.get_density( u );
				double[] s = xsection.effective_support( tolerance );
				random_supports.addElement( s );
// System.err.print( "AbsConDist_AbsDist: rand supt: "+s[0]+", "+s[1]+"  u: "  );
// for(j=0;j<u.length;j++)System.err.print(u[j]+" ");
// System.err.println("  xsect: "+xsection.format_string("----") );
				++i;	// increment only if get_density succeeds
			}
			catch (ConditionalNotDefinedException e) {}
		}

		double[][] supports_array = new double[ random_supports.size() ][];
		random_supports.copyInto( supports_array );

		merged_support = Intervals.union_merge_intervals( supports_array );
		support_known = true;
		
System.err.println( "AbsCondDist_AbsDist.Integral.eff_supt: ncombo: "+ncombo );
for ( i = 0; i < merged_support.length; i++ )
System.err.println( "\t"+"merged_support["+i+"]: "+merged_support[i][0]+", "+merged_support[i][1] );

		// HACK !!! NEED TO UPDATE effective_support TO RETURN 
		// double[][] !!! USE THIS TIL THEN !!!
		double[] return_support = new double[2];
		return_support[0] = merged_support[0][0];
		return_support[1] = merged_support[merged_support.length-1][1];
		return return_support;
	}

	/** Given a range of integers 0, ..., n, generates a random
	  * integer uniformly distributed from 0 to n, inclusive.
	  */
	static int uniform_random_integer( double[] range )
	{
		int i;
		do
		{
			// It's possible, with floating point math, to get
			// result of i == n+1. If that happens just try again.

			i = (int) (Math.random() * (range[1]+1));
		}
		while ( i > (int)range[1] );

		return i;
	}

	/** Given a floating point range [a,b], generates a random
	  * float uniformly distributed over [a,b].
	  */
	static double uniform_random_float( double[] range )
	{
		return range[0] + (range[1]-range[0])*Math.random();
	}

	public class Integral implements Callback_1d
	{
		IntegralHelper ih;

		public Integral()
		{
			integrand = this. new Integrand();
			ih = IntegralHelperFactory.make_helper( integrand, a, b, is_discrete, skip_integration );
		}

		public double f( double x ) throws Exception
		{
			x1[0] = x;

			try
			{
				double px = ih.do_integral( u_known );
// System.err.println( "computes_pi.Integral.p("+x1[0]+"): "+px );
				return px;
			}
			catch (Exception e)
			{
				throw new Exception( "Integral.p: failed:\n\t"+e );
			}
		}

		/** This class represents a product of a conditional with one or more
		  * unconditional distributions, such as appear in the integrand in the
		  * computation of <tt>p(x|e+)</tt>. The product has the form
		  * <pre>
		  *   p(x|u1,...,un) p(u1) ... p(un)
		  * </pre>
		  * Note that after <tt>u1</tt> through <tt>un</tt> are integrated
		  * out, the result is a function of <tt>x</tt>. The value of <tt>x</tt>
		  * is a member datum in this class; the function <tt>f</tt> implemented
		  * by this class takes <tt>u1</tt> through <tt>un</tt> as arguments.
		  */
		public class Integrand implements Callback_nd
		{
			public double f( double[] u ) throws Exception
			{
				double product = conditional.p( x1, u );
// System.err.print( "Integrand.f: x1: "+x1[0]+", u: (" );
// for ( int j = 0; j < u.length; j++ ) System.err.print( u[j]+"," ); System.err.print("); p(x1|u): "+product+", pu1: (");
				for ( int i = 0; i < pi_messages.length; i++ )
				{
					if ( pi_messages[i] instanceof Delta ) continue;

					u1[0] = u[i];
					double pu1 = pi_messages[i].p( u1 );
// System.err.print( pu1+"," );
					product *= pu1;
				}

// System.err.println( "), product: "+product );
				return product;
			}
		}
	}
}
