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
package riso.distributions;
import riso.general.*;

public class TrivialPiHelper implements PiHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely exactly one <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[1];
		s[0] = new SeqTriple( "riso.distributions.AbstractDistribution", 1 );
		description_array = s;
	}

	/** This helper is called only when there are no parents of a variable;
	  * in that case, the marginal distribution is just the distribution
	  * conditional on the parents, and pi is the marginal distribution.
	  * So this method just returns the variable's conditional distribution.
	  */
	public Distribution compute_pi( ConditionalDistribution px, Distribution[] pi_messages ) throws Exception
	{
		return (Distribution) px;
	}
}
