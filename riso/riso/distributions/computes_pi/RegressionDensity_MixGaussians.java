package riso.distributions.computes_pi;
import java.rmi.*;
import riso.distributions.*;

/** @see PiHelper
  */
public class RegressionDensity_MixGaussians implements PiHelper
{
	public Distribution compute_pi( ConditionalDistribution y_in, Distribution[] pi ) throws Exception
	{
		int i;

		for ( i = 0; i < pi.length; i++ )
			if ( pi[i].ndimensions() != 1 )
				throw new IllegalArgumentException( "computes_pi.RegressionDensity_MixGaussians.compute_pi: pi-message "+i+" has "+pi[i].ndimensions()+" dimensions (should have 1)"+"\n" );

		RegressionDensity y = (RegressionDensity) y_in;

		if ( y.ndimensions_child() != 1 )
			throw new IllegalArgumentException( "computes_pi.RegressionDensity_MixGaussians.compute_pi: this node has "+y.ndimensions_child()+" dimensions (should have 1)"+"\n" );

		int ncomponents = 1;
		for ( i = 0; i < pi.length; i++ )
			ncomponents *= ((MixGaussians)pi[i]).ncomponents();
		MixGaussians mix = new MixGaussians( 1, ncomponents );

		int[] k = new int[ pi.length ], l = new int[1];
	
		mix_gaussians_pi_approx_inner_loop( mix, pi, y, k, l, pi.length-1, null );
		// REDUCE MIXTURE ??? WILL EVENTUALLY WANT THAT !!!

		return mix;
	}

	public static void mix_gaussians_pi_approx_inner_loop( MixGaussians mix, Distribution[] pi, RegressionDensity y, int[] k, int[] l, int m, Distribution[] pi_combo ) throws Exception
	{
		if ( pi_combo == null ) pi_combo = new Distribution[ pi.length ];

		if ( m == -1 )
		{
			double mix_proportion = 1;
			for ( int i = 0; i < pi.length; i++ )
			{
				mix_proportion *= ((MixGaussians)pi[i]).mix_proportions[ k[i] ];
				pi_combo[i] = ((MixGaussians)pi[i]).components[ k[i] ];
			}

			mix.mix_proportions[ l[0] ] = mix_proportion;
			mix.components[ l[0] ] = riso.distributions.computes_pi.RegressionDensity_Gaussian.one_gaussian_pi_approx( pi_combo, y );
			++l[0];
		}
		else
		{
			for ( int i = 0; i < ((MixGaussians)pi[m]).ncomponents(); i++ )
			{
				k[m] = i;
				mix_gaussians_pi_approx_inner_loop( mix, pi, y, k, l, m-1, pi_combo );
			}
		}
	}
}
