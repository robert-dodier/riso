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
public class XorGate_Discrete implements PiHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>XorGate</tt>
	  * followed by any number of <tt>Discrete</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.XorGate", 1 );
		s[1] = new SeqTriple( "riso.distributions.Discrete", -1 );
		description_array = s;
	}

	public Distribution compute_pi( ConditionalDistribution py_in, Distribution[] pi_messages ) throws Exception
	{
		XorGate py = (XorGate) py_in;

		// The zero'th element of each incoming pi-message is interpreted
		// as a "special" state. All the others are lumped together.
		// The output of the xor-gate should be understood as the probability
		// that an odd number of inputs are non-zero.

		double p1 = 0;
		for ( int i = 0; i < pi_messages.length; i++ )
		{
			double p1_i = 1 - ((Discrete)pi_messages[i]).probabilities[0];
			p1 += p1_i*(1-2*p1);
		}

		Discrete pye_density = new Discrete();
		pye_density.ndims = 1;
		pye_density.dimensions = new int[1];
		pye_density.dimensions[0] = 2;
		pye_density.probabilities = new double[2];
		pye_density.probabilities[0] = 1-p1;
		pye_density.probabilities[1] = p1;

		return pye_density;
	}
}
