package riso.distributions.computes_lambda;
import java.io.*;
import riso.distributions.*;

/** Yeah, it's sort of confusing that this class has the same name as
  * in <tt>riso.distributions</tt>, but that's a consequence of the 
  * naming scheme used to locate message helpers. This class implements
  * a helper which can handle a list of <tt>riso.distributions.MixGaussians</tt>
  * messages.
  */
public class MixGaussians implements LambdaHelper
{
	/** @return The product of the incoming likelihood messages, which is
	  *   again a <tt>riso.distributions.MixGaussians</tt>.
	  * @see LambdaHelper.compute_lambda
	  * @see riso.distributions.MixGaussians.product_mixture
	  */
	public Distribution compute_lambda( Distribution[] lambda_messages ) throws Exception
	{
		// Some of the lambda messages may noninformative; skip over those.
		// Construct a list containing only the informative lambda messages.

		int i, j, ninformative = 0;

		for ( i = 0; i < lambda_messages.length; i++ )
		{
			if ( !( lambda_messages[i] instanceof Noninformative ) )
				++ninformative;
		}
			
		riso.distributions.MixGaussians[] informative_lambdas = new riso.distributions.MixGaussians[ ninformative ];

		for ( i = 0, j = 0; i < lambda_messages.length; i++ )
		{
			if ( lambda_messages[i] instanceof Noninformative )
				continue;

			informative_lambdas[j++] = (riso.distributions.MixGaussians) lambda_messages[i];
		}

		riso.distributions.MixGaussians q = riso.distributions.MixGaussians.product_mixture( informative_lambdas );

		return q;
	}
}
