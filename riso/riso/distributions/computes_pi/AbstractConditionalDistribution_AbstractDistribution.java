package riso.distributions.computes_pi;
import java.rmi.*;
import java.util.*;
import riso.distributions.*;
import riso.approximation.*;
import numerical.*;
import TopDownSplayTree;

/** @see PiHelper
 */
public class AbstractConditionalDistribution_AbstractDistribution implements PiHelper
{
	public Distribution compute_pi( ConditionalDistribution pxu, Distribution[] pi ) throws Exception
	{
// System.err.println( "AbsCondDist_AbsDist.compute_pi: called." );
		IntegralCache integral_cache = new IntegralCache( pxu, pi );
		MixGaussians q = integral_cache.initial_mix( null );
		GaussianMixApproximation.debug = true;
		q = GaussianMixApproximation.do_approximation( integral_cache, q, integral_cache.merged_support, 1e-4 );
		return q;
	}
}

class IntegralCache extends AbstractDistribution implements Callback_1d
{
	public ConditionalDistribution conditional;
	public Distribution[] distributions;
	public double[] x1;

	double[] u_known, u1, a, b;
	double[][] merged_support;

	boolean support_known = false;
	boolean[] is_discrete, skip_integration;

	FunctionCache cache;
	Integral integral;
	Integral.Integrand integrand;

	public IntegralCache( ConditionalDistribution conditional, Distribution[] distributions ) throws Exception
	{
// System.err.println( "AbsCondDist_AbsDist.IntegralCache: constructor called." );
		int i;

		x1 = new double[1];
		u1 = new double[1];

		this.conditional = conditional;
		this.distributions = distributions;

		is_discrete = new boolean[ distributions.length ];
		for ( i = 0; i < distributions.length; i++ )
			is_discrete[i] = (distributions[i] instanceof Discrete);

		u_known = new double[ distributions.length ];
		skip_integration = new boolean[ distributions.length ];
		for ( i = 0; i < distributions.length; i++ )
		{
			if ( distributions[i] instanceof Delta )
			{
				skip_integration[i] = true;
				u_known[i] = ((Delta)distributions[i]).get_support()[0];
// System.err.println( "\t"+"set u_known["+i+"] to "+u_known[i] );
			}
		}

		a = new double[ distributions.length ];
		b = new double[ distributions.length ];

		for ( i = 0; i < distributions.length; i++ )
		{
			double[] support_i = distributions[i].effective_support( 1e-8 );
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
	  * a large number of cross-sections, evaluated for random 
	  * combinations of parents. The actual effective support is probably
	  * somewhat smaller than what is returned by this method.
	  */ 
	public double[] effective_support( double tolerance ) throws Exception
	{
		int i, j;

		if ( !support_known )
		{
			// Generate (nxsections_per_parent)*(#parents) random combinations.
			// That's not enough if there are many parents -- should !!!
			// grow exponentially !!!

			int nxsections_per_parent = 40;
			int nparents = distributions.length;
			int ncombo = nxsections_per_parent * nparents;

			double[] u = new double[ nparents ];
			Vector random_supports = new Vector();
			double[][] parent_support = new double[ nparents ][];

			for ( j = 0; j < nparents; j++ )
				try { parent_support[j] = distributions[j].effective_support( tolerance ); }
				catch (Exception e) { throw new Exception( "IntegralCache.effective_support: failed: "+e ); }

			for ( i = 0; i < ncombo; )
			{
				for ( j = 0; j < nparents; j++ )
					if ( is_discrete[j] )
						// Generate random integer in parent support.
						u[j] = uniform_random_integer( parent_support[j] );
					else
						// Generate random floating point in parent support.
						u[j] = uniform_random_float( parent_support[j] );

				try
				{
					Distribution xsection = conditional.get_density( u );
					double[] s = xsection.effective_support( tolerance );
					random_supports.addElement( s );

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
		}

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
			ih = new IntegralHelper( integrand, a, b, is_discrete, skip_integration );
		}

		public double f( double x ) throws Exception
		{
			x1[0] = x;

			try
			{
// System.err.println( "Integral.p: evaluate integral w/ x: "+x1[0]+"... " );
				double px = ih.do_integral( u_known );
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
				for ( int i = 0; i < distributions.length; i++ )
				{
					u1[0] = u[i];
					double pu1 = distributions[i].p( u1 );
// System.err.print( pu1+"," );
					product *= pu1;
				}

// System.err.println( "), product: "+product );
				return product;
			}
		}
	}
}
