package riso.distributions.computes_pi;
import java.rmi.*;
import riso.distributions.*;
import riso.approximation.*;
import numerical.*;

/** @see PiHelper
  */
public class AbstractConditionalDistribution_AbstractDistribution implements PiHelper
{
	public Distribution compute_pi( ConditionalDistribution y_in, Distribution[] pi ) throws Exception
	{
		throw new Exception( "AbstractConditionalDistribution_AbstractDistribution.compute_pi: not implemented." );
	}

	public static class Integral extends AbstractDistribution implements Callback_nd
	{
		public ConditionalDistribution conditional;
		public Distribution[] distributions;
		public double[] x;

		double[] u1;
		boolean support_known = false;
		double[][] merged_support;

		public Integral( ConditionalDistribution conditional, Distribution[] distributions ) throws RemoteException
		{
			super();
			int i;

			x = new double[1];
			u1 = new double[1];

			this.conditional = conditional;
			this.distributions = distributions;

			is_discrete = new boolean[ distributions.length ];
			for ( i = 0; i < distributions.length; i++ )
				is_discrete[i] = (distributions[i] instanceof Discrete);
		}

		public double f( double[] x_in ) throws Exception { return p(x_in); }

		public double p( double[] x_in ) throws RemoteException
		{
			x[0] = x_in[0];

			try
			{
				int n = distributions.length;
				return ExtrapolationIntegral.
	do_integral( n, is_discrete, a, b, integrand, 1e-4, null, null );
			}
			catch (Exception e) { throw new RemoteException( "Integral.p: failed: "+e ); }
		}

		public int ndimensions() throws RemoteException { return 1; }

		/** Computes an approximate support for this distribution. 
		  * The approximation is taken as the union of the supports of 
		  * a large number of cross-sections, evaluated for random 
		  * combinations of parents. The actual effective support is probably
		  * somewhat smaller than what is returned by this method.
		  */ 
		public double[] effective_support( double tolerance )
		{
			int i, j;

			if ( !support_known )
			{
				// Generate (nxsections_per_parent)*(#parents) random combinations.
				// That's not enough if there are many parents -- should !!!
				// grow exponentially !!!

				int nxsections_per_parent = 10;
				int nparents = distributions.length;
				int ncombo = nxsections_per_parent * nparents;

				double[] u = new double[ nparents ];
				double[][] random_supports = new double[ ncombo ][];
				double[][] parent_support = new double[ nparents ];

				for ( j = 0; j < nparents; j++ )
					parent_support[j] = distributions[j].effective_support( tolerance );

				for ( i = 0; i < ncombo; i++ )
				{
					for ( j = 0; j < nparents; j++ )
						if ( is_discrete[j] )
							// Generate random integer in parent support.
							u[j] = uniform_random_integer( parent_support[j] );
						else
							// Generate random floating point in parent support.
							u[j] = uniform_random_float( parent_support[j] );

					Distribution xsection = conditional.get_density( u );
					random_supports[i] = xsection.effective_support( tolerance );
				}

				merged_support = Intervals.union_merge_intervals( random_supports );
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
			int i, j;

			try
			{
				effective_support();	// ignore return value

				int nbumps_per_interval = 5;
				int nbumps = nbumps_per_interval * merged_support.length;

				MixGaussians q = new MixGaussians( 1, nbumps );

				double[] umean = new double[ conditional.ndimensions_parent() ];

				for ( i = 0; i < umean.length; i++ )
					umean[i] = distributions[i].expected_value();

				Distribution xsection = conditional.get_density( umean );

				double mx = xsection.expected_value();
				double sx = xsection.sqrt_variance();

				for ( i = 0; i < nbumps; i++ )
					q.components[i] = new Gaussian( mx+(i-(nbumps-1)/2.0)*sx, sx );

				return q;
			}
			catch (RemoteException e)
			{
				throw new RuntimeException( "Integrand: unexpected: "+e );
			}

			/** Given a range of integers 0, ..., n, generates a random
			  * integer uniformly distributed from 0 to n, inclusive.
			  */
			int uniform_random_integer( double[] range )
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
			double uniform_random_float( double[] range )
			{
				return range[0] + (range[1]-range[0])*Math.random();
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
		public static class Integrand implements Callback_nd
		{
		}
	}
}
