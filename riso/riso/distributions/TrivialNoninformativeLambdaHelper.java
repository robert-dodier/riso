package riso.distributions;

public class TrivialNoninformativeLambdaHelper implements LambdaHelper
{
	/** In the case there is no diagnostic support, lambda is noninformative.
	  */
	public Distribution compute_lambda( Distribution[] lambda_messages ) throws Exception
	{
		return new Noninformative();
	}

	public double ignored_scale( Distribution[] lambda_messages )
	{
		// compute_lambda returns exact result.
		return 1;
	}
}
