/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999, Robert Dodier.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA, 02111-1307, USA,
 * or visit the GNU web site, www.gnu.org.
 */
package riso.distributions.computes_pi;
import java.rmi.*;
import riso.distributions.*;
import SeqTriple;

public class MixConditionalGaussians_MixGaussians implements PiHelper
{
	public static double MIN_MIX_PROPORTION = 5e-3;

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>MixConditionalGaussians</tt>
	  * followed by any number of <tt>MixGaussians</tt>.
	  */
	public static SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.MixConditionalGaussians", 1 );
		s[1] = new SeqTriple( "riso.distributions.MixGaussians", -1 );
		return s;
	}

	public Distribution compute_pi( ConditionalDistribution pxu, Distribution[] pi_messages ) throws Exception
	{
		int i, n = 1;

		// PROMOTE GAUSSIAN PI MESSAGES TO MIX GAUSSIANS !!!

		for ( i = 0; i < pi_messages.length; i++ ) n *= ((MixGaussians)pi_messages[i]).ncomponents();

		MixGaussians pi = new MixGaussians( 1, n );

		int[] k = new int[ mixtures.length ], l = new int[1];
		product_inner_loop( pxu, pi_messages, product, k, l, pi_messages.length-1 );

// CHECK SUM OF MIX COEFFS. SHOULD BE 1 !!!
double sum = 0; 
for ( i = 0; i < product.ncomponents; i++ ) sum += product.mix_proportions[i];
System.err.println( "computes_pi.MixCondGauss_MixGauss: mix coeffs sum: "+sum );

		// Throw out low-mass components.

		Vector too_light = new Vector();
		for ( int i = 0; i < mix.ncomponents(); i++ )
			if ( mix.mix_proportions[i] < MIN_MIX_PROPORTION )
				too_light.addElement( new Integer(i) );

if ( too_light.size() > 0 ) System.err.println( "MixCondGauss_MixGauss.compute_pi: remove "+too_light.size()+" components." );
		mix.remove_components( too_light, null );

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
