package riso.approximation;
import java.io.*;
import java.rmi.*;
import java.util.*;
import riso.distributions.*;
import numerical.*;
import SmarterTokenizer;

public class GaussianMixApproximation
{
	public static boolean debug = false;

	public static MixGaussians do_approximation( Distribution target, MixGaussians approximation, double[][] supports, double tolerance ) throws Exception
	{
System.err.println( "GaussianMixApproximation.do_approximation: need approx. to class: "+target.getClass() );

		// Take care of a couple of trivial cases first.

		if ( target instanceof MixGaussians )
			return (MixGaussians) target.remote_clone();

		if ( target instanceof Gaussian )
		{
			approximation = new MixGaussians( target.ndimensions(), 1 );
			approximation.components[0] = (Gaussian) target.remote_clone();
			return approximation;
		}

		// Now the real work begins.

		int i, j, k, max_iterations = 10;		// CHANGE !!!

		double[] new_alpha = new double[ approximation.ncomponents() ];
		double[] new_mu = new double[ approximation.ncomponents() ];
		double[] new_sigma2 = new double[ approximation.ncomponents() ];

		CrossEntropyIntegrand cei = new CrossEntropyIntegrand( target, approximation );
		EntropyIntegrand ei = new EntropyIntegrand( target );

		if ( debug )
		{
System.err.println( "do_approximation: compute entropy." );
			double e = integrate_over_intervals( supports, ei, tolerance );
System.err.println( "do_approximation: compute cross-entropy." );
			double ce = integrate_over_intervals( supports, cei, tolerance );
			
			System.err.println( "entropy of target: "+e );
			System.err.println( "initial cross entropy: "+ce );
		}

		MixingProportionIntegrand[] mpi = new MixingProportionIntegrand[  approximation.ncomponents()  ];
		MeanIntegrand[] mi = new MeanIntegrand[  approximation.ncomponents()  ];
		VarianceIntegrand[] vi = new VarianceIntegrand[  approximation.ncomponents()  ];

		for ( i = 0; i <  approximation.ncomponents(); i++ )
		{
			mpi[i] = new MixingProportionIntegrand( i, target, approximation );
			mi[i] = new MeanIntegrand( i, target, approximation );
			vi[i] = new VarianceIntegrand( i, target, approximation );
		}

		double S[][] = new double[1][1];

		for ( k = 0; k < max_iterations; k++ )
		{
			for ( i = 0; i <  approximation.ncomponents(); i++ )
			{
				new_alpha[i] = integrate_over_intervals( supports, mpi[i], tolerance );
				new_mu[i] = integrate_over_intervals( supports, mi[i], tolerance ) / new_alpha[i];
				new_sigma2[i] = integrate_over_intervals( supports, vi[i], tolerance ) / new_alpha[i];
			}

			double suma = 0;
			for ( i = 0; i <  approximation.ncomponents(); i++ )
				suma += new_alpha[i];
			for ( i = 0; i <  approximation.ncomponents(); i++ )
				new_alpha[i] /= suma;

			for ( i = 0; i <  approximation.ncomponents(); i++ )
			{
				approximation.mix_proportions[i] = new_alpha[i];
				((Gaussian)approximation.components[i]).mu[0] = new_mu[i];
				S[0][0] = new_sigma2[i];
				((Gaussian)approximation.components[i]).set_Sigma( S );
			}

			if ( debug )
			{
				System.err.println( "iteration: "+k );
				System.err.print( "approximation mixing proportions: " ); 
				Matrix.pretty_output( approximation.mix_proportions, System.err, " " );
				System.err.println("");

				System.err.print( "approximation means: " );
				for ( i = 0; i <  approximation.ncomponents(); i++ )
					System.err.print( ((Gaussian)approximation.components[i]).mu[0]+" " );
				System.err.println("");

				System.err.print( "approximation std devs: " );
				for ( i = 0; i <  approximation.ncomponents(); i++ )
					System.err.print( Math.sqrt( ((Gaussian)approximation.components[i]).get_Sigma()[0][0] )+" " );
				System.err.println("");

				double ce = integrate_over_intervals( supports, cei, tolerance );
				System.err.println( "cross entropy: "+ce+"\n" );
			}

			// Here's an easy step toward simplification:
			// throw out mixture components which have very small weight.

			final double MIN_MIX_PROPORTION = 1e-6;
			Vector too_light = new Vector();

			for ( i = 0; i < approximation.ncomponents(); i++ )
			{
				if ( approximation.mix_proportions[i] < MIN_MIX_PROPORTION )
					too_light.addElement( approximation.components[i] );
			}

			approximation.remove_components( too_light );

			// Here's another easy one: throw out a component if it
			// appears to be nearly the same as some other component.

			Vector duplicates = new Vector();

			for ( i = 0; i < approximation.ncomponents(); i++ )
			{
				if ( duplicates.indexOf( approximation.components[i] ) != -1 )
					// components[i] has already been put on duplicates list; skip it.
					continue;

				double m_i = approximation.components[i].expected_value();
				double s_i = approximation.components[i].sqrt_variance();

				for ( j = i+1; j < approximation.ncomponents(); j++ )
				{
					if ( duplicates.indexOf( approximation.components[j] ) != -1 )
						// components[i] has already been put on duplicates list; skip it.
						continue;

					double m_j = approximation.components[j].expected_value();
					double s_j = approximation.components[j].sqrt_variance();

					if ( s_i == 0 || s_j == 0 ) continue;

					double dm = Math.abs(m_i-m_j), rs = s_i/s_j, s_ij = Math.sqrt( 1/( 1/(s_i*s_i) + 1/(s_j*s_j) ) );

					if ( dm/s_ij < 1e-1 && rs > 1 - 1e-1 && rs < 1 + 1e-1 )
					{
System.err.println( "do_approximation: comp["+j+"] same as ["+i+"]; m_i: "+m_i+" m_j: "+m_j+" s_i: "+s_i+" s_j: "+s_j );
						duplicates.addElement( approximation.components[j] );
					}
				}
			}

			approximation.remove_components( duplicates );
		}

		return approximation;
	}

	/** Computes an integral over a set of disjoint intervals.
	  * This is just the sum of the integrals on each interval.
	  * The set of intervals is represented as an array with number of
	  * rows equal to the number of intervals, and 2 columns.
	  * The first column corresponds to the left endpoint, and the
	  * second corresponds to the right endpoint. The endpoints are NOT
	  * swapped if they are not in order.
	  *
	  * <p> To make the integration a little easier, each interval is
	  * divided up into a number of disjoint subintervals, and the integral
	  * over each is added up to get the integral over the whole interval.
	  */
	public static double integrate_over_intervals( double[][] intervals, Callback_nd integrand, double tolerance ) throws ExtrapolationIntegral.DifficultIntegralException, Exception
	{
		double sum = 0;
		double[] a = new double[1], b = new double[1];
		boolean[] is_discrete = new boolean[1];
		is_discrete[0] = false;

		for ( int i = 0; i < intervals.length; i++ )
		{
			// Divide up interval[i] into a number of subintervals, 
			// and integrate over each one.

			final int NSUBINTERVALS = 50;
			double left = intervals[i][0], right = intervals[i][1];

			for ( int j = 0; j < NSUBINTERVALS; j++ )
			{
				a[0] = left + j*(right-left)/(double)NSUBINTERVALS;
				b[0] = left + (j+1)*(right-left)/(double)NSUBINTERVALS;
				sum += ExtrapolationIntegral.do_integral( 1, null, is_discrete, a, b, integrand, tolerance, null, null );
			}
		}

		return sum;
	}

	/** Returns a generic initial mixture approximation, based on the
	  * mean and standard deviation of the target distribution <tt>p</tt>.
	  * The initial approximation needs to be further refined before it
	  * can be used to compute probabilities and other quantities.
	  */
	static public MixGaussians initial_mix( Distribution p )
	{
		try
		{
			if ( p instanceof GaussianDelta )
			{
				// Return 1-component mixture containing a copy of p.
				MixGaussians q = new MixGaussians( 1, 1 );
				q.components[0] = (Distribution) p.remote_clone();
				return q;
			}

			int ndimensions = 1;	// should verify all messages are same dimension -- well, forget it. !!!

			int ncomponents = 3;	// heuristic !!!

			MixGaussians q = new MixGaussians( ndimensions, ncomponents ); 

			double[][] Sigma = new double[1][1];

			double m = p.expected_value();
			double s = p.sqrt_variance();
			Sigma[0][0] = s*s;

			((Gaussian)q.components[ 0 ]).mu[0] = m;
			((Gaussian)q.components[ 0 ]).set_Sigma( Sigma );

			((Gaussian)q.components[ 1 ]).mu[0] = m-s;
			((Gaussian)q.components[ 1 ]).set_Sigma( Sigma );

			((Gaussian)q.components[ 2 ]).mu[0] = m+s;
			((Gaussian)q.components[ 2 ]).set_Sigma( Sigma );

			return q;
		}
		catch (Exception e)
		{
			throw new RuntimeException( "GaussianMixApproximation.initial_mix: unexpected: "+e );
		}
	}

	/** Returns a generic initial mixture approximation to a function, which
	  * may not be normalized; the function will be nonnegative and smooth.
	  * The initial approximation needs to be further refined before it
	  * can be used to compute integrals or other quantities.
	  *
	  * <p> The hard part of the problem is searching out the peaks in
	  * the target function, without any clues about their location. 
	  * This method sprinkles a lot of points on a wide interval centered
	  * on zero and tries to find evidence of peaks. So the time required
	  * for this approach is proportional to the time required to evaluate
	  * the target function.
	  *
	  * @param f Target function.
	  * @param scale Rough estimate of characteristic scale of the target;
	  *   for example, 1, 1000, or 0.001. Algorithm employed here searches
	  *   an interval <tt>(-1000*scale,+1000*scale)</tt> at a resolution
	  *   equal to <tt>scale</tt>.
	  * @returns A Gaussian mixture, not necessarily normalized, 
	  */
	static public MixGaussians initial_mix( Callback_1d f, double scale )
	{
		return null;
	}

	public static void main( String[] args )
	{
		System.err.println( "target file: "+args[0] );
		System.err.println( "initial approx file: "+args[1] );
		System.err.println( "target support: ["+args[2]+", "+args[3]+"]" );

		try
		{
			int i;
			FileInputStream fis = new FileInputStream( args[0] );
			SmarterTokenizer p_st = new SmarterTokenizer( new BufferedReader( new InputStreamReader( fis ) ) );
			Distribution p = null;

			try 
			{
				p_st.nextToken();
				Class p_class = Class.forName( p_st.sval );
				p = (Distribution) p_class.newInstance();
				p_st.nextBlock();
				p.parse_string( p_st.sval );
			}
			catch (Exception e)
			{
				System.err.println( "GaussianMixApproximation.main: attempt to construct target failed: "+e );
				System.exit(1);
			}

			fis.close(); fis = new FileInputStream( args[1] );
			SmarterTokenizer q_st = new SmarterTokenizer( new BufferedReader( new InputStreamReader( fis ) ) );
			MixGaussians q = null;

			try
			{
				q_st.nextToken();
				Class q_class = Class.forName( q_st.sval );
				q = (MixGaussians) q_class.newInstance();
				q_st.nextBlock();
				q.parse_string( q_st.sval );
			}
			catch (Exception e)
			{
				System.err.println( "GaussianMixApproximation.main: attempt to construct approximation failed: "+e );
				System.exit(1);
			}

			double[][] support = new double[1][2];
			support[0][0] = Format.atof( args[2] );
			support[0][1] = Format.atof( args[3] );

			GaussianMixApproximation.debug = true;

			try { q = GaussianMixApproximation.do_approximation( p, q, support, 1e-5 ); }
			catch (ExtrapolationIntegral.DifficultIntegralException e1)
			{
				// Widen the tolerance and try again.
				q = GaussianMixApproximation.do_approximation( p, q, support, 1e-3 );
			}
			catch (Exception e)
			{
				System.err.println( "GaussianMixApproximation.main: do_approximation failed; "+e );
				System.exit(1);
			}

			System.out.print( "final approximation:\n"+q.format_string("") );
			double x[] = new double[1];
			for ( i = 0; i < 50; i++ )
			{
				x[0] = support[0][0]+i*(support[0][1]-support[0][0])/50.0;
				System.out.println( "x: "+x[0]+" p: "+p.p(x)+" q: "+q.p(x)+" q/p: "+ q.p(x)/p.p(x) );
			}
		}
		catch (Exception e)
		{
			System.err.println( "GaussianMixApproximation.main: something went ker-blooey: "+e );
		}

		System.exit(1);
	}
}

class MixingProportionIntegrand implements Callback_nd
{
	int q_index;
	Distribution target;
	MixGaussians approximation;

	MixingProportionIntegrand( int q_index, Distribution target, MixGaussians approximation )
	{
		this.q_index = q_index;
		this.target = target;
		this.approximation = approximation;
	}

	public double f( double[] x ) throws Exception
	{
		return target.p( x ) * approximation.responsibility( q_index, x );
	}
}

class MeanIntegrand implements Callback_nd
{
	int q_index;
	Distribution target;
	MixGaussians approximation;

	MeanIntegrand( int q_index, Distribution target, MixGaussians approximation )
	{
		this.q_index = q_index;
		this.target = target;
		this.approximation = approximation;
	}

	public double f( double[] x ) throws Exception
	{
		return x[0] * target.p(x) * approximation.responsibility( q_index, x );
	}
}

class VarianceIntegrand implements Callback_nd
{
	int q_index;
	Distribution target;
	MixGaussians approximation;

	VarianceIntegrand( int q_index, Distribution target, MixGaussians approximation )
	{
		this.q_index = q_index;
		this.target = target;
		this.approximation = approximation;
	}

	public double f( double[] x ) throws Exception
	{
		double mu = ((Gaussian)approximation.components[q_index]).mu[0];
		double dx = x[0] - mu;
		return dx*dx * target.p(x) * approximation.responsibility(q_index,x);
	}
}

class CrossEntropyIntegrand implements Callback_nd
{
	Distribution target, approximation;

	CrossEntropyIntegrand( Distribution target, Distribution approximation )
	{
		this.target = target;
		this.approximation = approximation;
	}

	public double f( double[] x ) throws Exception
	{
		double px = target.p(x), qx = approximation.p(x);

		if ( px == 0 && qx == 0 )
			return 0;
		else
			return -px*Math.log(qx);
	}
}

class EntropyIntegrand implements Callback_nd
{
	Distribution target;

	EntropyIntegrand( Distribution target )
	{
		this.target = target;
	}

	public double f( double[] x ) throws Exception
	{
		double px = target.p(x);

		if ( px == 0 )
			return 0;
		else
			return -px*Math.log(px);
	}
}
