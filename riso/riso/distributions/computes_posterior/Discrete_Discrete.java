package riso.distributions;

public class ComputesPosterior_Discrete_Discrete extends AbstractPosteriorHelper
{
	public Distribution compute_posterior( Distribution pi_in, Distribution lambda_in ) throws Exception
	{
		Discrete pi = (Discrete) pi_in;
		Discrete lambda = (Discrete) lambda_in;

		Discrete posterior = new Discrete();
		posterior.ndims = pi.ndims;
		posterior.dimensions = (int[]) pi.dimensions.clone();
		posterior.probabilities = new double[ pi.probabilities.length ];

		int k;
		double sum = 0;

		for ( k = 0; k < posterior.probabilities.length; k++ )
			sum += (posterior.probabilities[k] = pi.probabilities[k] * lambda.probabilities[k]);
		
		for ( k = 0; k < posterior.probabilities.length; k++ )
			posterior.probabilities[k] /= sum;

		return posterior;
	}
}
