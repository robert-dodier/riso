package riso.distributions.computes_lambda;
import java.util.*;
import riso.distributions.*;

public class Discrete implements LambdaHelper
{
	/** Compute the likelihood function for a variable. This is defined
	  * as <code>p(``e below''|x)</code> ... NEEDS WORK !!!
	  */
	public Distribution compute_lambda( Distribution[] lambda_messages ) throws Exception
	{
		// Some of the lambda messages may be null or noninformative; skip over those.

		riso.distributions.Discrete p = null;
		boolean first_informative = true;

		for ( int i = 0; i < lambda_messages.length; i++ )
		{
			if ( lambda_messages[i] == null || lambda_messages[i] instanceof Noninformative )
				continue;

			if ( first_informative )
			{
				p = (riso.distributions.Discrete) lambda_messages[i].remote_clone();
				first_informative = false;
				continue;
			}

			riso.distributions.Discrete q = (riso.distributions.Discrete) lambda_messages[i];
			if ( q.probabilities.length != p.probabilities.length )
				throw new IllegalArgumentException( "computes_lambda.Discrete.compute_lambda: some lambda messages have different lengths." );

			for ( int j = 0; j < p.probabilities.length; j++ )
				p.probabilities[j] *= q.probabilities[j];
		}

		return p;
	}
}
