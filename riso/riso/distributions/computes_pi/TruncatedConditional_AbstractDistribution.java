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
public class TruncatedConditional_AbstractDistribution implements PiHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>TruncatedConditional/tt>
	  * followed by any number of <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.TruncatedConditional", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", -1 );
		description_array = s;
	}

	/** Punt: look for a pi helper for the <tt>ConditionalDistribution</tt> contained by the
	  * truncated conditional, and then form a <tt>Truncated</tt> from the result.
	  */
	public Distribution compute_pi( ConditionalDistribution py_in, Distribution[] pi_messages ) throws Exception
	{
		TruncatedConditional py = (TruncatedConditional) py_in;

        // IS CACHED HELPER MEANINGFUL HERE ???
		PiHelper ph = PiHelperLoader.load_pi_helper( null, py.cd, pi_messages );

		Distribution pi = ph.compute_pi( py.cd, pi_messages );

		return new Truncated( pi, py.left, py.right );
	}
}
