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
public class Sum_Gaussian implements PiHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>Sum</tt>
	  * followed by any number of <tt>Gaussian</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.Sum", 1 );
		s[1] = new SeqTriple( "riso.distributions.Gaussian", -1 );
		description_array = s;
	}

	public Distribution compute_pi( ConditionalDistribution py_in, Distribution[] pi_messages ) throws Exception
	{
		double mu_sum = 0, sigma2_sum = 0;

		for ( int i = 0; i < pi_messages.length; i++ )
		{
			Gaussian p = (Gaussian) pi_messages[i];
			mu_sum += p.expected_value();
			sigma2_sum += sqr( p.sqrt_variance() );
		}

		if ( sigma2_sum == 0 )
		{
			double[] support = new double[1];
			support[0] = mu_sum;
			return new GaussianDelta( support );
		}

		return new Gaussian( mu_sum, Math.sqrt(sigma2_sum) );
	}

	double sqr( double x ) { return x*x; }
}
