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

		DistributionProduct dp = new DistributionProduct( informative_lambdas );
		return dp;

		// MAYBE WE DON'T NEED THIS APPROXIMATION YET !!!
		// PUT OFF APPROXIMATION UNTIL POSTERIOR COMPUTATION !!!

		// riso.distributions.MixGaussians q = dp.initial_mix();

// System.err.println( "computes_lambda.AbstractDistribution: initial approx:\n"+q.format_string("\t") );

		// double tolerance = 1e-5;
// GaussianMixApproximation.debug = true;	// MAY WANT TO TURN OFF ONCE THIS STUFF WORKS !!!
		// q = GaussianMixApproximation.do_approximation( (Distribution)dp, q, dp.merged_support, tolerance );

		// return q;
	}
}
