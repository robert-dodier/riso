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
package riso.distributions.computes_lambda_message;
import java.util.*;
import riso.distributions.*;
import riso.general.*;

public class IndexedDistribution_GaussianDelta_ implements LambdaMessageHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>IndexedDistribution</tt>
	  * followed by one <tt>GaussianDelta</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.IndexedDistribution", 1 );
		s[1] = new SeqTriple( "riso.distributions.GaussianDelta", 1 );
		description_array = s;
	}

	/** Ignores <tt>pi_messages</tt>.
	  */
	public Distribution compute_lambda_message( ConditionalDistribution px_in, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
		IndexedDistribution px = (IndexedDistribution) px_in;
		px.check_components();

		Discrete lambda_message = new Discrete();
		lambda_message.ndims = 1;
		lambda_message.dimensions = (int[]) px.index_dimensions.clone();
		lambda_message.probabilities = new double[ lambda_message.dimensions[0] ];

		double[] ii = new double[1], xx = new double[1];
		xx[0] = ((GaussianDelta)lambda).get_support()[0];

		for ( int i = 0; i < lambda_message.probabilities.length; i++ )
		{
			ii[0] = i;
			lambda_message.probabilities[i] = px.p( xx, ii );
		}

		return lambda_message;
	}
}
