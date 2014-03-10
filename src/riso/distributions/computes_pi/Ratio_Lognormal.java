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
public class Ratio_Lognormal implements PiHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>Ratio</tt>
	  * followed by two of <tt>Lognormal</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.Ratio", 1 );
		s[1] = new SeqTriple( "riso.distributions.Lognormal", 2 );
		description_array = s;
	}

	public Distribution compute_pi( ConditionalDistribution py_in, Distribution[] pi_messages ) throws Exception
	{
		double mu_diff, sigma2_sum;

		Lognormal p = (Lognormal) pi_messages[0], q = (Lognormal) pi_messages[1];
		mu_diff = p.associated_gaussian.expected_value() - q.associated_gaussian.expected_value();
		sigma2_sum = sqr( p.associated_gaussian.sqrt_variance() ) + sqr( q.associated_gaussian.sqrt_variance() );

		if ( sigma2_sum == 0 ) return new GaussianDelta( Math.exp(mu_diff) );

		return new Lognormal( mu_diff, Math.sqrt(sigma2_sum) );
	}

	double sqr( double x ) { return x*x; }
}
