package riso.distributions.computes_posterior;
import riso.distributions.*;

public class Mixture_Mixture extends AbstractPosteriorHelper
{
	riso.distributions.computes_lambda.Mixture cached_helper = null;

	public double ignored_scale( Distribution pi, Distribution lambda )
	{
		if ( cached_helper == null )
			cached_helper = new riso.distributions.computes_lambda.Mixture();
		
		Mixture[] mixtures = new Mixture[2];
		mixtures[0] = (Mixture) pi;
		mixtures[1] = (Mixture) lambda;

		return cached_helper.ignored_scale( mixtures );
	}

	public Distribution compute_posterior( Distribution pi, Distribution lambda ) throws Exception
	{
		if ( cached_helper == null )
			cached_helper = new riso.distributions.computes_lambda.Mixture();

		Mixture[] mixtures = new Mixture[2];
		mixtures[0] = (Mixture) pi;
		mixtures[1] = (Mixture) lambda;

		return cached_helper.compute_lambda( mixtures );
	}
}
