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
import riso.approximation.*;
import riso.numerical.*;
import riso.general.*;

/** @see PiHelper
 */
public class Classifier_AbstractDistribution implements PiHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>Classifier</tt>
	  * followed by any number of <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.Classifier", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", -1 );
		description_array = s;
	}

	public Distribution compute_pi( ConditionalDistribution pxu, Distribution[] pi_messages ) throws Exception
	{
		IntegralCache integral_cache = new IntegralCache( pxu, pi_messages );

		int[] dimensions = new int[1], ii = new int[1];
		dimensions[0] = ((Classifier)pxu).ncategories();
		Discrete dd = new Discrete( dimensions );

		for ( int i = 0; i < dimensions[0]; i++ )
		{
			ii[0] = i;
			double q = integral_cache.f(i);
			dd.assign_p( ii, q );
		}

		dd.ensure_normalization();
		return dd;
	}
}
