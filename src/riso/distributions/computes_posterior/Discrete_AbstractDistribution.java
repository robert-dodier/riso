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
import riso.distributions.Discrete;
import riso.distributions.*;
import riso.distributions.computes_lambda.*;
import riso.general.*;

public class Discrete_AbstractDistribution implements PosteriorHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions 
	  * accepted by this helper -- namely, one <tt>Discrete</tt> and 
	  * one <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.Discrete", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", 1 );
		description_array = s;
	}

	public Distribution compute_posterior( Distribution pi_in, Distribution lambda ) throws Exception
	{
		Discrete p = new Discrete(), pi = (Discrete) pi_in;
		p.ndims = pi.ndims;
		p.dimensions = (int[]) pi.dimensions.clone();
		p.probabilities = new double[ pi.probabilities.length ];
		
		double[] x = new double[1];
		double sum = 0;

		for ( int i = 0; i < pi.probabilities.length; i++ )
		{
			x[0] = i;
			double lp = lambda.p(x), pp = pi.probabilities[i] * lp;
			p.probabilities[i] = pp;
System.err.println( "compvte_posterior: pi.p, lambda.p, prodvct: "+pi.probabilities[i]+", "+lp+", "+pp );
			sum += pp;
		}

		for ( int i = 0; i < p.probabilities.length; i++ )
			p.probabilities[i] /= sum;

		return p;
	}
}
