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
import java.util.*;
import riso.distributions.*;
import SeqTriple;

/** @see PiHelper
  */
public class Sum_MixGaussians implements PiHelper
{
	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>Sum</tt>
	  * followed by any number of <tt>MixGaussians</tt>.
	  */
	public static SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.Sum", 1 );
		s[1] = new SeqTriple( "riso.distributions.MixGaussians", -1 );
		return s;
	}

	public Distribution compute_pi( ConditionalDistribution py_in, Distribution[] pi_messages ) throws Exception
	{
		int n = 1;
		int[] ncomponents = new int[ pi_messages.length ];

		for ( int i = 0; i < pi_messages.length; i++ ) 
		{
			ncomponents[i] = ((MixGaussians)pi_messages[i]).ncomponents();
			n *= ncomponents[i];
		}

		MixGaussians mix = new MixGaussians( 1, n );

		for ( int i = 0; i < n; i++ )
		{
			double prod_alpha = 1, sum_mu = 0, sum_sigma2 = 0;

			int ii = i;
			for ( int j = 0; j < pi_messages.length; j++ )
			{
				// Figure out the permutation of indexes corresponding to i.
				int i_j = ii % ncomponents[j];
				ii = ii/ncomponents[j];
				MixGaussians mix_j = (MixGaussians)pi_messages[j];
				sum_mu += mix_j.components[i_j].expected_value();
				sum_sigma2 += sqr( mix_j.components[i_j].sqrt_variance() );
				prod_alpha *= mix_j.mix_proportions[i_j];
			}

			mix.components[i] = new Gaussian( sum_mu, Math.sqrt(sum_sigma2) );
			mix.mix_proportions[i] = prod_alpha;
		}

		return mix;
	}

	double sqr( double x ) { return x*x; }
}