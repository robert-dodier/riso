package riso.distributions.computes_posterior;
import riso.distributions.*;
import SeqTriple;

public class MixGaussians_MixGaussians extends AbstractPosteriorHelper
{
	/** Returns a description of the sequences of distributions 
	  * accepted by this helper -- namely, two <tt>MixGaussians</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[1];
		s[0] = new SeqTriple( "riso.distributions.MixGaussians", 2 );
		description_array = s;
	}

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

		if ( product.ncomponents() == 1 )
			return product.components[0];
		else
			return product;
	}
}
