package riso.approximation;
import java.io.*;
import java.rmi.*;
import riso.distributions.*;
import riso.distributions.computes_lambda.*;
import numerical.*;
import SmarterTokenizer;

public class DistributionProduct extends riso.distributions.AbstractDistribution implements Callback_nd
{
	public double Z;
	public Distribution[] distributions;
	public double[][] merged_support;

	public DistributionProduct( Distribution[] distributions ) throws RemoteException
	{
		super();
		int i;

		this.distributions = distributions;
		
		double[][] supports = new double[ distributions.length ][];
		for ( i = 0; i < distributions.length; i++ )
			supports[i] = distributions[i].effective_support( 1e-12 );

		merged_support = Intervals.intersection_merge_intervals( supports );	// SHOULD BE UNION ???

		double tolerance = 1e-5;

		Z = 1;	// IMPORTANT !!! This must be set before trying to evaluate integrals !!!

		try
		{
			try { Z = GaussianMixApproximation.integrate_over_intervals( merged_support, this, tolerance ); }
			catch (ExtrapolationIntegral.DifficultIntegralException e)
			{
				System.err.println( "DistributionProduct: warning: difficult integral; widen tolerance and try again." );
				try { Z = GaussianMixApproximation.integrate_over_intervals( merged_support, this, 100*tolerance ); }
				catch (ExtrapolationIntegral.DifficultIntegralException e2)
				{
					System.err.println( "DistributionProduct: error: increased tolerance, but integration still fails." );
					throw new RemoteException( "DistributionProduct: attempt to compute normalizing constant failed:\n"+e2 );
				}
			}
		}
		catch (Exception e)
		{
			throw new RemoteException( "DistributionProduct: exception: "+e );
		}

System.err.println( "DistributionProduct: Z: "+Z );
	}

	public double f( double[] x ) throws Exception { return p(x); }

	public double p( double[] x ) throws RemoteException
	{
		double product = 1/Z;
		for ( int i = 0; i < distributions.length; i++ )
			product *= distributions[i].p(x);
		return product;
	}

	public int ndimensions() throws RemoteException { return 1; }

	/** Formats a string representation of this distribution.
	  */
	public String format_string( String leading_ws ) throws RemoteException
	{
		int i;

		String result = "";
		result += this.getClass().getName()+" { ";

		String more_ws = leading_ws+"\t";
		result += more_ws+"normalizing-constant "+Z+"\n";
		result += more_ws+"merged-support ";
		result += more_ws+"ndistributions "+distributions.length+"\n";

		result += more_ws+"merged-support { ";
		for ( i = 0; i < merged_support.length; i++ )
			result += merged_support[i][0]+" "+merged_support[i][1]+" ";
		result += "}"+"\n";

		String still_more_ws = more_ws+"\t";
		result += more_ws+"distributions"+"\n"+more_ws+"{"+"\n"+still_more_ws;
		for ( i = 0; i < distributions.length; i++ )
		{
			result += still_more_ws+"% distributions["+i+"]"+"\n";
			result += still_more_ws+distributions[i].format_string( still_more_ws );
		}
		result += more_ws+"}"+"\n";

		result += leading_ws+"}"+"\n";
		return result;
	}

	/** Parse a string containing a description of an instance of this distribution.
	  * The description is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Write an instance of this distribution to an output stream.
	  *
	  * @param os The output stream to print on.
	  * @param leading_ws Since the representation is only one line of output, 
	  *   this argument is ignored.
	  * @throws IOException If the output fails; this is possible, but unlikely.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	public MixGaussians initial_mix()
	{
		try
		{
			int ndimensions = 1;	// should verify all messages are same dimension -- well, forget it. !!!

			int ncomponents = 3*distributions.length;	// heuristic !!!

			MixGaussians q = new MixGaussians( ndimensions, ncomponents ); 

			double[][] Sigma = new double[1][1];

			for ( int i = 0; i < distributions.length; i++ )
			{
				double m = distributions[i].expected_value();
				double s = distributions[i].sqrt_variance();
				Sigma[0][0] = s*s;

				((Gaussian)q.components[ 3*i ]).mu[0] = m;
				((Gaussian)q.components[ 3*i ]).set_Sigma( Sigma );

				((Gaussian)q.components[ 3*i+1 ]).mu[0] = m-s;
				((Gaussian)q.components[ 3*i+1 ]).set_Sigma( Sigma );

				((Gaussian)q.components[ 3*i+2 ]).mu[0] = m+s;
				((Gaussian)q.components[ 3*i+2 ]).set_Sigma( Sigma );
			}

			return q;
		}
		catch (RemoteException e)
		{
			throw new RuntimeException( "DistributionProduct: unexpected: "+e );
		}
	}
}
