package riso.distributions.computes_lambda;
import java.io.*;
import java.rmi.*;
import java.util.*;
import riso.distributions.*;
import riso.approximation.*;
import numerical.*;
import SmarterTokenizer;

public class AbstractDistribution implements LambdaHelper
{
	/** Compute the likelihood function, called ``lambda'', for a variable.
	  * This is defined as <code>p(``e below''|x)</code>, and can be
	  * computed in terms of the lambda-messages coming up from
	  * child variables as
	  * <pre>
	  *   p(``e below''|x) = \prod_{i=1}^{n_children} lambda_i(x)
	  * </pre>
	  * That is, lambda is just the product of the incoming lambda messages.
	  *
	  * <p> This helper can handle any kind of continuous lambda messages.
	  * The lambda is approximated by a mixture of Gaussians. Other handlers
	  * for specific types should be tried before this one, since the result
	  * is at best an approximation, and since the approximation involves
	  * a numerical integration, the approximation may fail altogether.
	  *
	  * @param lambda_messages Likelihood messages from each child; some might
	  *   be noninformative -- those are skipped. However, all messages must
	  *   be continuous -- none can be <tt>instanceof Discrete</tt>.
	  * @return The product of the incoming likelihood messages.
	  * @throws IllegalArgumentException If some message is <tt>Discrete</tt>.
	  * @throws ExtrapolationIntegral.DifficultIntegralException If the
	  *   approximation can't be computed.
	  */
	public Distribution compute_lambda( Distribution[] lambda_messages ) throws Exception
	{
		// Some of the lambda messages may noninformative; skip over those.
		// Construct a list containing only the informative lambda messages.

		int i, j, ninformative = 0;

		for ( i = 0; i < lambda_messages.length; i++ )
		{
			if ( lambda_messages[i] instanceof Discrete )
				throw new IllegalArgumentException( "riso.distributions.computes_lambda.AbstractDistribution.compute_lambda: at least one lambda message is Discrete; can't handle it." );

			if ( !( lambda_messages[i] instanceof Noninformative ) )
				++ninformative;
		}
			
		Distribution[] informative_lambdas = new Distribution[ ninformative ];

		for ( i = 0, j = 0; i < lambda_messages.length; i++ )
		{
			if ( lambda_messages[i] instanceof Noninformative )
				continue;

			informative_lambdas[j++] = lambda_messages[i];
		}

		LambdaProduct lp = new LambdaProduct( informative_lambdas );

		// Should verify all messages are same dimension -- well, forget it. !!!
		int ndimensions = lambda_messages[0].ndimensions();

		int ncomponents = 3*lambda_messages.length;	// heuristic !!!

		MixGaussians q = new MixGaussians( ndimensions, ncomponents );

		double[][] Sigma = new double[1][1];

		for ( i = 0; i < lambda_messages.length; i++ )
		{
			double m = lambda_messages[i].expected_value();
			double s = lambda_messages[i].sqrt_variance();
			Sigma[0][0] = s*s;

			((Gaussian)q.components[ 3*i ]).mu[0] = m;
			((Gaussian)q.components[ 3*i ]).set_Sigma( Sigma );

			((Gaussian)q.components[ 3*i+1 ]).mu[0] = m-s;
			((Gaussian)q.components[ 3*i+1 ]).set_Sigma( Sigma );

			((Gaussian)q.components[ 3*i+2 ]).mu[0] = m+s;
			((Gaussian)q.components[ 3*i+2 ]).set_Sigma( Sigma );
		}
System.err.println( "computes_lambda.AbstractDistribution: initial approx:\n"+q.format_string("\t") );

		double tolerance = 1e-5;
GaussianMixApproximation.debug = true;	// MAY WANT TO TURN OFF ONCE THIS STUFF WORKS !!!
		GaussianMixApproximation.do_approximation( (Distribution)lp, q, lp.merged_support, tolerance );

		return q;
	}
}

class LambdaProduct extends riso.distributions.AbstractDistribution implements Callback_nd
{
	double Z;
	Distribution[] lambdas;
	double[][] merged_support;

	public LambdaProduct( Distribution[] lambdas ) throws RemoteException
	{
		super();
		int i;

		this.lambdas = lambdas;
		
		double[][] supports = new double[ lambdas.length ][];
		for ( i = 0; i < lambdas.length; i++ )
			supports[i] = lambdas[i].effective_support( 1e-12 );

		merged_support = Intervals.intersection_merge_intervals( supports );	// SHOULD BE UNION ???

		double tolerance = 1e-5;

		Z = 1;	// IMPORTANT !!! This must be set before trying to evaluate integrals !!!

		try
		{
			try { Z = GaussianMixApproximation.integrate_over_intervals( merged_support, this, tolerance ); }
			catch (ExtrapolationIntegral.DifficultIntegralException e)
			{
				System.err.println( "LambdaProduct: warning: difficult integral; widen tolerance and try again." );
				try { Z = GaussianMixApproximation.integrate_over_intervals( merged_support, this, 100*tolerance ); }
				catch (ExtrapolationIntegral.DifficultIntegralException e2)
				{
					System.err.println( "LambdaProduct: error: increased tolerance, but integration still fails." );
					throw new RemoteException( "LambdaProduct: attempt to compute normalizing constant failed:\n"+e2 );
				}
			}
		}
		catch (Exception e)
		{
			throw new RemoteException( "LambdaProduct: exception: "+e );
		}

System.err.println( "LambdaProduct: Z: "+Z );
	}

	public double f( double[] x ) throws Exception { return p(x); }

	public double p( double[] x ) throws RemoteException
	{
		double product = 1/Z;
		for ( int i = 0; i < lambdas.length; i++ )
			product *= lambdas[i].p(x);
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
		result += more_ws+"nlambdas "+lambdas.length+"\n";

		result += more_ws+"merged-support { ";
		for ( i = 0; i < merged_support.length; i++ )
			result += merged_support[i][0]+" "+merged_support[i][1]+" ";
		result += "}"+"\n";

		String still_more_ws = more_ws+"\t";
		result += more_ws+"lambdas"+"\n"+more_ws+"{"+"\n"+still_more_ws;
		for ( i = 0; i < lambdas.length; i++ )
		{
			result += still_more_ws+"% lambdas["+i+"]"+"\n";
			result += still_more_ws+lambdas[i].format_string( still_more_ws );
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
}
