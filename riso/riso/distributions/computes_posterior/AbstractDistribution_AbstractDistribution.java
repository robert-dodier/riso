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
		DistributionProduct dp = new DistributionProduct( p_l );
		MixGaussians q = dp.initial_mix();

		double tolerance = 1e-5;
GaussianMixApproximation.debug = true;  // MAY WANT TO TURN OFF ONCE THIS STUFF WORKS !!!
		GaussianMixApproximation.do_approximation( (Distribution)dp, q, dp.merged_support, tolerance );

		return q;
	}
}
