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
package riso.distributions.computes_posterior;
import riso.distributions.*;
import riso.general.*;

public class Gaussian_Gaussian implements PosteriorHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions 
	  * accepted by this helper -- namely, two <tt>Gaussian</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[1];
		s[0] = new SeqTriple( "riso.distributions.Gaussian", 2 );
		description_array = s;
	}

	/** @see PosteriorHelper.compute_posterior
	  */
	public Distribution compute_posterior( Distribution pi, Distribution lambda ) throws Exception
	{
		double m1 = pi.expected_value(), m2 = lambda.expected_value();
		double s1 = pi.sqrt_variance(), s2 = lambda.sqrt_variance();

		double s = Math.sqrt( 1/(1/(s1*s1)+1/(s2*s2)) );
		double m = (m1/(s1*s1)+m2/(s2*s2))/(1/(s1*s1)+1/(s2*s2));
		
		return new Gaussian( m, s );
	}
}
