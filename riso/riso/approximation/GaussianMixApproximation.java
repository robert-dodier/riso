import java.io.*;
import riso.distributions.*;
import numerical.*;

public class GaussianMixApproximation
{
	public static boolean debug = false;

	public static void do_approximation( Distribution target, Mixture approximation, double[] x0, double[] x1 ) throws Exception
	{
		int i, k, max_iterations = 10;		// CHANGE !!!

		double[] new_alpha = new double[ approximation.ncomponents() ];
		double[] new_mu = new double[ approximation.ncomponents() ];
		double[] new_sigma2 = new double[ approximation.ncomponents() ];

		CrossEntropyIntegrand cei = new CrossEntropyIntegrand( target, approximation );
		EntropyIntegrand ei = new EntropyIntegrand( target );

		if ( debug )
		{
			double e = ExtrapolationIntegral.do_integral( 1, x0, x1, ei, null, null );
			double ce = ExtrapolationIntegral.do_integral( 1, x0, x1, cei, null, null );
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
				new_alpha[i] = ExtrapolationIntegral.do_integral( 1, x0, x1, mpi[i], null, null );
				new_mu[i] = ExtrapolationIntegral.do_integral( 1, x0, x1, mi[i], null, null ) / new_alpha[i];
				new_sigma2[i] = ExtrapolationIntegral.do_integral( 1, x0, x1, vi[i], null, null ) / new_alpha[i];
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

				double ce = ExtrapolationIntegral.do_integral( 1, x0, x1, cei, null, null );
				System.err.println( "cross entropy: "+ce+"\n" );
			}
		}
	}

	public static void main( String[] args )
	{
		try
		{
			int i;
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );

			System.err.print( "give name of file describing target distribution: " );
			st.nextToken();
			FileInputStream fis = new FileInputStream( st.sval );
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

			System.err.print( "give name of file describing initial approximation: " );
			st.nextToken();
			fis.close(); fis = new FileInputStream( st.sval );
			SmarterTokenizer q_st = new SmarterTokenizer( new BufferedReader( new InputStreamReader( fis ) ) );
			Mixture q = null;

			try
			{
				q_st.nextToken();
				Class q_class = Class.forName( q_st.sval );
				q = (Mixture) q_class.newInstance();
				q_st.nextBlock();
				q.parse_string( q_st.sval );
			}
			catch (Exception e)
			{
				System.err.println( "GaussianMixApproximation.main: attempt to construct approximation failed: "+e );
				System.exit(1);
			}

			System.err.print( "give lower and upper bounds on effective support of target: " );
			double x0[] = new double[1], x1[] = new double[1];
			st.nextToken();
			x0[0] = Format.atof( st.sval );
			st.nextToken();
			x1[0] = Format.atof( st.sval );

			GaussianMixApproximation.debug = true;
			ExtrapolationIntegral.tolerance = 1e-4;

			try { GaussianMixApproximation.do_approximation( p, q, x0, x1 ); }
			catch (ExtrapolationIntegral.DifficultIntegralException e1)
			{
				// Widen the tolerance and try again.
				ExtrapolationIntegral.tolerance *= 10;
				GaussianMixApproximation.do_approximation( p, q, x0, x1 );
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
				x[0] = x0[0]+i*(x1[0]-x0[0])/50.0;
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
	Mixture approximation;

	MixingProportionIntegrand( int q_index, Distribution target, Mixture approximation )
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
	Mixture approximation;

	MeanIntegrand( int q_index, Distribution target, Mixture approximation )
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
	Mixture approximation;

	VarianceIntegrand( int q_index, Distribution target, Mixture approximation )
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
