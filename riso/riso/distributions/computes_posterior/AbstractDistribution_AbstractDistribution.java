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

		if ( pi instanceof Discrete )
		{
			if ( pi.ndimensions() > 1 ) throw new IllegalArgumentException( "AbstractDistribution_AbstractDistribution.compute_posterior: can't handle variable of "+pi.ndimensions()+" dimensions." );

			Discrete q = (Discrete) pi.remote_clone();
			double[] x1 = new double[1];

if ( ((Discrete)pi).dimensions[0] != q.probabilities.length ) throw new RuntimeException( "AbstractDistribution_AbstractDistribution.compute_posterior: SEVERE CONFUSION !!!" );

			for ( int i = 0; i < ((Discrete)pi).dimensions[0]; i++ )
			{
				x1[0] = i;
				q.probabilities[i] = dp.p( x1 );
			}

			return q;
		}
		else
		{
			MixGaussians q = dp.initial_mix( pi.effective_support( 1e-6 ) );
			double tolerance = 1e-5;
GaussianMixApproximation.debug = true;  // MAY WANT TO TURN OFF ONCE THIS STUFF WORKS !!!
			q = GaussianMixApproximation.do_approximation( (Distribution)dp, q, dp.merged_support, tolerance );

			return q;
		}
	}
}
