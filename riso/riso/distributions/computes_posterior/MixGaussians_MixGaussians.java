package riso.distributions.computes_posterior;
import riso.distributions.*;

public class MixGaussians_MixGaussians extends AbstractPosteriorHelper
{
	/** @see PosteriorHelper.compute_posterior
	  * @see MixGaussians.product_mixture
	  */
	public Distribution compute_posterior( Distribution pi, Distribution lambda ) throws Exception
	{
		MixGaussians[] mixtures = new MixGaussians[2];
		mixtures[0] = (MixGaussians) pi;
		mixtures[1] = (MixGaussians) lambda;

		MixGaussians product = MixGaussians.product_mixture( mixtures );
		product.reduce_mixture( 100, 0.01 );	// FOR REAL ???

		return product;
	}
}
