package riso.distributions.computes_lambda;
import java.rmi.*;
import java.util.*;
import riso.distributions.*;
import riso.approximation.*;
import numerical.*;

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

		double tolerance = 1e-5;
		GaussianMixApproximation.do_approximation( (Distribution)lp, q, lp.merged_supports, tolerance );

		return q;
	}
}

class LambdaProduct extends riso.distributions.AbstractDistribution implements Callback_nd
{
	double Z;
	Distribution[] lambdas;
	double[][] merged_supports;

	public LambdaProduct( Distribution[] lambdas ) throws RemoteException
	{
		super();
		int i;

		this.lambdas = lambdas;
		
		double[][] supports = new double[ lambdas.length ][];
		for ( i = 0; i < lambdas.length; i++ )
			supports[i] = lambdas[i].effective_support( 1e-8 );

		double[][] merged_supports = Intervals.merge_intervals( supports );

		Z = 1;
		double sum = 0, tolerance = 1e-5;
		double[] x0 = new double[1], x1 = new double[1];

		for ( i = 0; i < merged_supports.length; i++ )
		{
			x0[0] = merged_supports[i][0];
			x1[0] = merged_supports[i][1];

			try
			{
				try { sum += ExtrapolationIntegral.do_integral( 1, x0, x1, this, tolerance, null, null ); }
				catch (ExtrapolationIntegral.DifficultIntegralException e)
				{
					System.err.println( "LambdaProduct: warning: difficult integral; widen tolerance and try again." );
					try { sum += ExtrapolationIntegral.do_integral( 1, x0, x1, this, 100*tolerance, null, null ); }
					catch (ExtrapolationIntegral.DifficultIntegralException e2)
					{
						System.err.println( "LambdaProduct: error: difficult integral; increased tolerance, but integration still fails." );
						throw new RemoteException( "LambdaProduct: attempt to compute normalizing constant failed." );
					}
				}
			}
			catch (Exception e)
			{
				throw new RemoteException( "LambdaProduct: exception: "+e );
			}
		}

		Z = sum;
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
}
