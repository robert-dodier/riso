package riso.distributions.computes_pi;
import java.rmi.*;
import riso.distributions.*;
import riso.approximation.*;

/** @see PiHelper
  */
public class RegressionDensity_AbstractDistribution implements PiHelper
{
	public Distribution compute_pi( ConditionalDistribution y, Distribution[] pi ) throws Exception
	{
		int i;

		MixGaussians[] pi_approx = new MixGaussians[ pi.length ];

		for ( i = 0; i < pi.length; i++ )
		{
			pi_approx[i] = pi[i].initial_mix( null );	// no suggestion for support
			double[][] pi_support = new double[1][];
			pi_support[0] = pi[i].effective_support( 1e-8 );
			pi_approx[i] = GaussianMixApproximation.do_approximation( pi[i], pi_approx[i], pi_support, 1e-5 );
		}

		return (new RegressionDensity_MixGaussians()).compute_pi( y, pi_approx );
	}
}
