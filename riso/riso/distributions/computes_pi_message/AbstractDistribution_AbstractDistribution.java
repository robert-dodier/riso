package riso.distributions;

public class ComputesPiMessage_AbstractDistribution_AbstractDistribution implements PiMessageHelper
{
	public Distribution compute_pi_message( Distribution pi,  Distribution[] lambda_messages ) throws Exception
	{
		// Our caller has set one of the lambda messages to null, corresponding
		// to the child which will receive this pi message. From the definition
		// of the pi message, the pi message is just like computing the
		// posterior except one of the lambda messages is missing.

		LambdaHelper lh = LambdaHelperLoader.load_lambda_helper( lambda_messages );
		Distribution partial_lambda = lh.compute_lambda( lambda_messages );
		PosteriorHelper ph = PosteriorHelperLoader.load_posterior_helper( pi, partial_lambda );
		return ph.compute_posterior( pi, partial_lambda );
	}
}
