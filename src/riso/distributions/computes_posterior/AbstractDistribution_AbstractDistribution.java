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
import riso.approximation.*;
import riso.general.*;

public class AbstractDistribution_AbstractDistribution implements PosteriorHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions 
	  * accepted by this helper -- namely, two <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[1];
		s[0] = new SeqTriple( "riso.distributions.AbstractDistribution", 2 );
		description_array = s;
	}

	public Distribution compute_posterior( Distribution pi, Distribution lambda ) throws Exception
	{
		Distribution[] p_l = new Distribution[2];
		p_l[0] = pi;
		p_l[1] = lambda;
		DistributionProduct dp = new DistributionProduct( false, pi instanceof Discrete,  p_l );

		SplineDensity q = SplineApproximation.do_approximation( (Distribution)dp, dp.support, null );

		return q;
	}
}
