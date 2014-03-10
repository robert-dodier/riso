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

public class AbstractDistribution_Delta implements PosteriorHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions 
	  * accepted by this helper -- namely, an <tt>AbstractDistribution</tt>
      * followed by a <tt>Delta</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple ("riso.distributions.AbstractDistribution", 1);
		s[1] = new SeqTriple ("riso.distributions.Delta", 1);
		description_array = s;
	}

	public Distribution compute_posterior (Distribution pi, Distribution lambda) throws Exception
	{
        double[] nondelta_support = pi.effective_support (1e-6);
        double delta_support = lambda.effective_support (0)[0];

        if (delta_support < nondelta_support[0] || delta_support > nondelta_support[1])
            throw new SupportNotWellDefinedException ("AbstractDistribution_Delta.computes_posterior: support point ("+delta_support+") of delta is not in support of the other distribution ("+nondelta_support[0]+", "+nondelta_support[1]+")");

		return (Distribution) lambda.clone();
	}
}
