package riso.distributions.computes_pi;
import java.rmi.*;
import riso.distributions.*;

public class MixConditionalGaussians_MixGaussians implements PiHelper
{
	public Distribution compute_pi( ConditionalDistribution pxu, Distribution[] pi_messages ) throws Exception
	{
		int i, n = 1;

		for ( i = 0; i < pi_messages.length; i++ ) n *= ((MixGaussians)pi_messages[i]).ncomponents();

		MixGaussians pi = new MixGaussians( 1, n );

		int[] k = new int[ mixtures.length ], l = new int[1];
		product_inner_loop( pxu, pi_messages, product, k, l, pi_messages.length-1 );

// CHECK SUM OF MIX COEFFS. SHOULD BE 1 !!!
double sum = 0; 
for ( i = 0; i < product.ncomponents; i++ ) sum += product.mix_proportions[i];
System.err.println( "computes_pi.MixCondGauss_MixGauss: mix coeffs sum: "+sum );

		// REDUCE MIXTURE ??? WILL EVENTUALLY WANT THAT !!!
		return mix;
	}

	static void product_inner_loop( ConditionalDistribution pxu, Distribution[] pi_messages, MixGaussians product, int[] k, int[] l, int m ) throws Exception
	{
		if ( m == -1 )
		{
			// Recursion has bottomed out.
			compute_one_product( pxu, pi_messages, product, k, l );
		}
		else
		{
			for ( int i = 0; i < pi_messages[m].ncomponents; i++ )
			{
				k[m] = i;
				product_inner_loop( pxu, pi_messages, product, k, l, m-1 );
			}
		}
	}

	static void compute_one_product( ConditionalDistribution pxu, Distribution[] pi_messages, MixGaussians product, int[] k, int[] l ) throws Exception
	{
		double mix_coeff_product = 1, mu_px = b, s2_px = s2x;

		for ( int i = 0; i < mixtures.length; i++ )
		{
			Gaussian pu = (Gaussian)((MixGaussians)pi_messages[i]).components[ k[i] ];
			double m = pu.mu[0], s2 = pi.Sigma[0][0];
			mu_px += a[i] * m;
			s2_px += a[i]*a[i] * s2;

			mix_coeff_product *= mixtures[i].mix_proportions[ k[i] ];
		}

		product.components[ l[0] ] = Gaussian.densities_product( mix_combo, ignored_scale );
		product.mix_proportions[ l[0] ] = mix_coeff_product;
		++l[0];

		// SHOULD WE TRY TO SET REGULARIZATION PARAMETERS TOO ???
	}
}
