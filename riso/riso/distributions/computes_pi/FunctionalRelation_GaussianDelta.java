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
public class FunctionalRelation_GaussianDelta implements PiHelper
{
	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>FunctionalRelation</tt>
	  * followed by any number of <tt>GaussianDelta</tt>.
	  */
	public static SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.FunctionalRelation", 1 );
		s[1] = new SeqTriple( "riso.distributions.GaussianDelta", -1 );
		return s;
	}

	/** All the incoming pi-messages are delta functions, so pi is also a delta function.
	  */
	public Distribution compute_pi( ConditionalDistribution pyx, Distribution[] pi_messages ) throws Exception
	{
		double[] x = new double[ pi_messages.length ];
		for ( int i = 0; i < x.length; i++ ) x[i] = ((Delta)pi_messages[i]).get_support()[0];

		return new GaussianDelta( ((FunctionalRelation)pyx).F(x) );
	}
}
