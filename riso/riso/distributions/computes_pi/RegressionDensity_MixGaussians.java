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
import java.util.*;
import riso.distributions.*;
import SeqTriple;

/** @see PiHelper
  */
public class RegressionDensity_MixGaussians implements PiHelper
{
	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>RegressionDensity</tt>
	  * followed by any number of <tt>MixGaussians</tt>.
	  */
	public static SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.RegressionDensity", 1 );
		s[1] = new SeqTriple( "riso.distributions.MixGaussians", -1 );
		return s;
	}

	public Distribution compute_pi( ConditionalDistribution y_in, Distribution[] pi ) throws Exception
	{
		int i;

		RegressionDensity y = (RegressionDensity) y_in;

		int ncomponents = 1;
		for ( i = 0; i < pi.length; i++ )
			ncomponents *= ((MixGaussians)pi[i]).ncomponents();
		MixGaussians mix = new MixGaussians( 1, ncomponents );

		int[] k = new int[ pi.length ], l = new int[1];
	
		mix_gaussians_pi_approx_inner_loop( mix, pi, y, k, l, pi.length-1, null );

		// Here's an easy step toward simplification: throw out a component if it
		// appears to be nearly the same as some other component.

		Vector duplicates = new Vector(), duplicated = new Vector();

		for ( i = 0; i < mix.ncomponents(); i++ )
		{
			double m_i = mix.components[i].expected_value();
			double s_i = mix.components[i].sqrt_variance();

			for ( int j = i+1; j < mix.ncomponents(); j++ )
			{
				double m_j = mix.components[j].expected_value();
				double s_j = mix.components[j].sqrt_variance();

				if ( s_i == 0 || s_j == 0 ) continue;

				double dm = Math.abs(m_i-m_j), rs = s_i/s_j, s_ij = Math.sqrt( 1/( 1/(s_i*s_i) + 1/(s_j*s_j) ) );

				if ( dm/s_ij < 2.5e-1 && rs > 1 - 2e-1 && rs < 1 + 2e-1 )
				{
					duplicates.addElement( new Integer(i) );
					duplicated.addElement( new Integer(j) );
					break; // go on to next i
				}
			}
		}

System.err.println( "Reg_MixG.comptes_pi: remove "+duplicates.size()+" duplicate components." );
		mix.remove_components( duplicates, duplicated );

		// Here's another easy one:
		// throw out mixture components which have very small weight.

		final double MIN_MIX_PROPORTION = 5e-3;
		Vector too_light = new Vector();

		for ( i = 0; i < mix.ncomponents(); i++ )
		{
			if ( mix.mix_proportions[i] < MIN_MIX_PROPORTION )
			{
				too_light.addElement( new Integer(i) );
			}
		}

System.err.println( "Reg_MixG.comptes_pi: remove "+too_light.size()+" too-light components." );
		mix.remove_components( too_light, null );

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
