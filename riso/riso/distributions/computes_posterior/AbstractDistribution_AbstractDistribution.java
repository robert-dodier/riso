package riso.distributions.computes_posterior;
import riso.distributions.*;
import riso.approximation.*;

public class AbstractDistribution_AbstractDistribution extends AbstractPosteriorHelper
{
	public Distribution compute_posterior( Distribution pi, Distribution lambda ) throws Exception
	{
		Distribution[] p_l = new Distribution[2];
		p_l[0] = pi;
		p_l[1] = lambda;
		DistributionProduct dp = new DistributionProduct( pi instanceof Discrete,  p_l );

		MixGaussians q = dp.initial_mix( pi.effective_support( 1e-6 ) );
		double tolerance = 1e-5;
		q = GaussianMixApproximation.do_approximation( (Distribution)dp, q, dp.merged_support, tolerance );

		return q;
	}
}
