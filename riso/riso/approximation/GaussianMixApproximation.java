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
System.err.println( "GaussianMixApproximation.do_approximation: need approx. to "+target.getClass() );

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

		IntegralHelper1d ceih = new IntegralHelper1d( cei, supports, false );
		IntegralHelper1d eih = new IntegralHelper1d( ei, supports, false );

		if ( debug )
		{
System.err.print( "do_approximation: compute entropy... " );
			double e = eih.do_integral();
System.err.println( ""+e );
System.err.println( "do_approximation: compute cross-entropy... " );
			double ce = ceih.do_integral();
System.err.println( ""+ce );
		}

		IntegralHelper1d[] mpih = new IntegralHelper1d[  approximation.ncomponents()  ];
		IntegralHelper1d[] mih = new IntegralHelper1d[  approximation.ncomponents()  ];
		IntegralHelper1d[] vih = new IntegralHelper1d[  approximation.ncomponents()  ];

		for ( i = 0; i <  approximation.ncomponents(); i++ )
		{
			mpih[i] = new IntegralHelper1d( new MixingProportionIntegrand( i, target, approximation ), supports, false );
			mih[i] = new IntegralHelper1d( new MeanIntegrand( i, target, approximation ), supports, false );
			vih[i] = new IntegralHelper1d( new VarianceIntegrand( i, target, approximation ), supports, false );
		}

		double S[][] = new double[1][1];

		for ( k = 0; k < max_iterations; k++ )
		{
			for ( i = 0; i <  approximation.ncomponents(); i++ )
			{
				new_alpha[i] = mpih[i].do_integral();
				new_mu[i] = mih[i].do_integral() / new_alpha[i];
				new_sigma2[i] = vih[i].do_integral() / new_alpha[i];
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

				double ce = ceih.do_integral();
				System.err.println( "cross entropy: "+ce+"\n" );
			}

			// If there is only one component (which could happen after removal
			// of some components), only one update is needed, so stop now.

			if ( approximation.ncomponents() == 1 ) break;

			// Here's an easy step toward simplification:
			// throw out mixture components which have very small weight.

			final double MIN_MIX_PROPORTION = 5e-3;
			Vector too_light = new Vector();

			for ( i = 0; i < approximation.ncomponents(); i++ )
			{
				if ( approximation.mix_proportions[i] < MIN_MIX_PROPORTION )
				{
System.err.println( "do_approx: mix prop["+i+"] == "+approximation.mix_proportions[i]+" is too small." );
					too_light.addElement( new Integer(i) );
				}
			}

			approximation.remove_components( too_light, null );

			// Here's another easy one: throw out a component if it
			// appears to be nearly the same as some other component.

			Vector duplicates = new Vector(), duplicated = new Vector();

			for ( i = 0; i < approximation.ncomponents(); i++ )
			{
				double m_i = approximation.components[i].expected_value();
				double s_i = approximation.components[i].sqrt_variance();

				for ( j = i+1; j < approximation.ncomponents(); j++ )
				{
					double m_j = approximation.components[j].expected_value();
					double s_j = approximation.components[j].sqrt_variance();

					if ( s_i == 0 || s_j == 0 ) continue;

					double dm = Math.abs(m_i-m_j), rs = s_i/s_j, s_ij = Math.sqrt( 1/( 1/(s_i*s_i) + 1/(s_j*s_j) ) );

					if ( dm/s_ij < 2.5e-1 && rs > 1 - 2e-1 && rs < 1 + 2e-1 )
					{
System.err.println( "do_approx: comp["+i+"] duplicates ["+j+"]" );
						duplicates.addElement( new Integer(i) );
						duplicated.addElement( new Integer(j) );
						break; // go on to next i
					}
				}
			}

			approximation.remove_components( duplicates, duplicated );
System.err.println( "--- after remove_components: cross-entropy... " );
			double ce = ceih.do_integral();
System.err.println( ""+ce );
		}

		return approximation;
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
				Class p_class = java.rmi.server.RMIClassLoader.loadClass( p_st.sval );
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
				Class q_class = java.rmi.server.RMIClassLoader.loadClass( q_st.sval );
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

			try { q = GaussianMixApproximation.do_approximation( p, q, support, 1e-2 ); }
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

class MixingProportionIntegrand implements Callback_1d
{
	int q_index;
	Distribution target;
	MixGaussians approximation;
	double[] x1 = new double[1];

	MixingProportionIntegrand( int q_index, Distribution target, MixGaussians approximation )
	{
		this.q_index = q_index;
		this.target = target;
		this.approximation = approximation;
	}

	public double f( double x ) throws Exception
	{
		x1[0] = x;
		return target.p( x1 ) * approximation.responsibility( q_index, x1 );
	}
}

class MeanIntegrand implements Callback_1d
{
	int q_index;
	Distribution target;
	MixGaussians approximation;
	double[] x1 = new double[1];

	MeanIntegrand( int q_index, Distribution target, MixGaussians approximation )
	{
		this.q_index = q_index;
		this.target = target;
		this.approximation = approximation;
	}

	public double f( double x ) throws Exception
	{
		x1[0] = x;
		return x * target.p(x1) * approximation.responsibility( q_index, x1 );
	}
}

class VarianceIntegrand implements Callback_1d
{
	int q_index;
	Distribution target;
	MixGaussians approximation;
	double[] x1 = new double[1];

	VarianceIntegrand( int q_index, Distribution target, MixGaussians approximation )
	{
		this.q_index = q_index;
		this.target = target;
		this.approximation = approximation;
	}

	public double f( double x ) throws Exception
	{
		x1[0] = x;
		double mu = ((Gaussian)approximation.components[q_index]).mu[0];
		double dx = x - mu;
		return dx*dx * target.p(x1) * approximation.responsibility(q_index,x1);
	}
}

class CrossEntropyIntegrand implements Callback_1d
{
	Distribution target, approximation;
	double[] x1 = new double[1];

	CrossEntropyIntegrand( Distribution target, Distribution approximation )
	{
		this.target = target;
		this.approximation = approximation;
	}

	public double f( double x ) throws Exception
	{
		x1[0] = x;
		double px = target.p(x1), qx = approximation.p(x1);

		if ( px == 0 && qx == 0 )
			return 0;
		else
			return -px*Math.log(qx);
	}
}

class EntropyIntegrand implements Callback_1d
{
	Distribution target;
	double[] x1 = new double[1];

	EntropyIntegrand( Distribution target )
	{
		this.target = target;
	}

	public double f( double x ) throws Exception
	{
		x1[0] = x;
		double px = target.p(x1);

		if ( px == 0 )
			return 0;
		else
			return -px*Math.log(px);
	}
}
