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
System.err.println( "AbsCondDist_AbsDist.compute_pi: called." );
		IntegralCache integral_cache = new IntegralCache( pxu, pi );
		MixGaussians q = integral_cache.initial_mix( null );
		GaussianMixApproximation.debug = true;
		q = GaussianMixApproximation.do_approximation( integral_cache, q, integral_cache.merged_support, 1e-4 );
		return q;
	}

	public static class IntegralCache extends AbstractDistribution implements Callback_1d
	{
		public ConditionalDistribution conditional;
		public Distribution[] distributions;
		public double[] xx;

		double[] uu, a, b;
		double[][] merged_support;

		boolean support_known = false;
		boolean[] is_discrete;

		FunctionCache cache;
		Integral integral;
		Integral.Integrand integrand;

		public IntegralCache( ConditionalDistribution conditional, Distribution[] distributions ) throws RemoteException
		{
System.err.println( "AbsCondDist_AbsDist.IntegralCache: constructor called." );
			int i;

			xx = new double[1];
			uu = new double[1];

			this.conditional = conditional;
			this.distributions = distributions;

			is_discrete = new boolean[ distributions.length ];
			for ( i = 0; i < distributions.length; i++ )
				is_discrete[i] = (distributions[i] instanceof Discrete);

			a = new double[ distributions.length ];
			b = new double[ distributions.length ];

			for ( i = 0; i < distributions.length; i++ )
			{
				double[] support_i = distributions[i].effective_support( 1e-8 );
				a[i] = support_i[0];
				b[i] = support_i[1];
			}

			integral = this. new Integral();
			integrand = integral. new Integrand();
			cache = new FunctionCache( -1, -1, integral );
		}

		public double p( double[] x_in ) throws RemoteException
		{
			try { return f( x_in[0] ); }
			catch (Exception e) { throw new RemoteException( "IntegralCache.p: unexpected: "+e ); }
		}

		public double f( double x ) throws Exception
		{
			return cache.lookup( x );
		}

		public int ndimensions() throws RemoteException { return 1; }

		/** Computes an approximate support for this distribution. 
		  * The approximation is taken as the union of the supports of 
		  * a large number of cross-sections, evaluated for random 
		  * combinations of parents. The actual effective support is probably
		  * somewhat smaller than what is returned by this method.
		  */ 
		public double[] effective_support( double tolerance ) throws RemoteException
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
					catch (RemoteException e) { throw new RemoteException( "IntegralCache.effective_support: failed: "+e ); }

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

		/** Constructs an initial approximation to this integral.
		  * Finds the effective support (which may be a union of disjoint
		  * intervals) and paves each interval in the support.
		  * @param support This argument is ignored.
		  */
		public MixGaussians initial_mix( double[] support )
		{
			int i, j, k;

			try
			{
				effective_support( 1e-8 );	// ignore return value

				int nbumps_per_interval = 5;
				int nbumps = nbumps_per_interval * merged_support.length;

				MixGaussians q = new MixGaussians( 1, nbumps );

				for ( i = 0, k = 0; i < merged_support.length; i++ )
				{
					double x0 = merged_support[i][0];
					double dx = (merged_support[i][1]-merged_support[i][0])/nbumps_per_interval;

					for ( j = 0; j < nbumps_per_interval; j++ )
					{
						double m = x0 + (j+0.5)*dx, s = dx;
						q.components[k++] = new Gaussian( m, s );
					}
				}

				return q;
			}
			catch (RemoteException e)
			{
				throw new RuntimeException( "IntegralCache: unexpected: "+e );
			}
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
				ih = new IntegralHelper( integrand, a, b, is_discrete, new boolean[a.length] );
			}

			public double f( double x ) throws Exception
			{
				xx[0] = x;

				try
				{
System.err.print( "Integral.p: evaluate integral w/ x: "+xx[0]+"... " );
					double px = ih.do_integral();
					return px;
				}
				catch (Exception e)
				{
					throw new RemoteException( "Integral.p: failed:\n\t"+e );
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
					double product = conditional.p( xx, u );
					for ( int i = 0; i < distributions.length; i++ )
					{
						uu[0] = u[i];
						product *= distributions[i].p( uu );
					}

					return product;
				}
			}
		}
	}
}
