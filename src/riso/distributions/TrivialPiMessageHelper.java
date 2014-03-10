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
import java.util.*;
import riso.general.*;

public class TrivialPiMessageHelper implements PiMessageHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely exactly one <tt>AbstractDistribution</tt>
	  * followed by any number of <tt>Noninformative</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.AbstractDistribution", 1 );
		s[0] = new SeqTriple( "riso.distributions.Noninformative", -1 );
		description_array = s;
	}

	/** In the case that there are no lambda messages to take into account,
	  * i.e. lambda messages coming from children other than the one to which
	  * we are sending the pi message, the pi message is simply <tt>pi</tt>.
	  */
	public Distribution compute_pi_message( Distribution pi,  Distribution[] lambda_messages ) throws Exception
	{
		return pi;
	}
}
