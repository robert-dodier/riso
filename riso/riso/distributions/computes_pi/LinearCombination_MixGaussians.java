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
import riso.general.*;

/** @see PiHelper
  */
public class LinearCombination_MixGaussians implements PiHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	public static double MIN_MIX_PROPORTION = 5e-3;

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>LinearCombination</tt>
	  * followed by any number of <tt>MixGaussians</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.LinearCombination", 1 );
		s[1] = new SeqTriple( "riso.distributions.MixGaussians", -1 );
		description_array = s;
	}

	/** Some pi messages might be <tt>Gaussian</tt> -- promote them to <tt>MixGaussian</tt>.
	  * All other pi messages must be <tt>MixGaussian</tt>. 
	  * Rescale each mixture of Gaussians, then call <tt>Sum_MixGaussians.compute_pi0</tt> to
	  * carry out the summation.
	  */
	public Distribution compute_pi( ConditionalDistribution py_in, Distribution[] pi_messages ) throws Exception
	{
		LinearCombination py = (LinearCombination) py_in;
		Distribution[] pi_msgs_w_scaling = new Distribution[ pi_messages.length ];

		for ( int i = 0; i < pi_messages.length; i++ )
		{
			if ( pi_messages[i] instanceof Gaussian )
			{
				double mu = pi_messages[i].expected_value(), sigma = pi_messages[i].sqrt_variance();
				pi_msgs_w_scaling[i] = new MixGaussians( new Gaussian( mu*py.a[i], sigma*Math.abs(py.a[i]) ) );
			}
			else
			{
				MixGaussians pi_msg = (MixGaussians)pi_messages[i], pi_msg_scaled = new MixGaussians( 1, pi_msg.ncomponents() );
				for ( int j = 0; j < pi_msg.ncomponents(); j++ )
				{
					double mu = pi_msg.components[j].expected_value(), sigma = pi_msg.components[j].sqrt_variance();
					pi_msg_scaled.mix_proportions[j] = pi_msg.mix_proportions[j];
					pi_msg_scaled.components[j] = new Gaussian( mu*py.a[i], sigma*Math.abs(py.a[i]) );
				}

				pi_msgs_w_scaling[i] = pi_msg_scaled;
			}
		}

		return (new Sum_MixGaussians()).compute_pi0( py_in, pi_msgs_w_scaling );
	}
}
