package riso.distributions;
import java.util.*;

public class TrivialLambdaMessageHelper implements LambdaMessageHelper
{
	/** In the case there is no diagnostic support, the lambda message
	  * is noninformative.
	  */
	public Distribution compute_lambda_message( ConditionalDistribution px, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
		if ( lambda instanceof Noninformative )
			return new Noninformative();

		throw new IllegalArgumentException( "TrivialLambdaMessageHelper.compute_lambda_message: lambda is "+lambda.getClass().getName()+", not Noninformative." );
	}
}
