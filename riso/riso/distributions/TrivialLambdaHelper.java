package risotto.distributions;

public class TrivialLambdaHelper implements LambdaHelper
{
	/** In the case there is no diagnostic support, lambda is noninformative.
	  */
	public Distribution compute_lambda( Distribution[] lambda_messages ) throws Exception
	{
		return new Noninformative();
	}
}
