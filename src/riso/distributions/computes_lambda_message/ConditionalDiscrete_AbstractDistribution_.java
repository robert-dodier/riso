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

public class ConditionalDiscrete_AbstractDistribution_ implements LambdaMessageHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>ConditionalDiscrete</tt>
	  * followed by one <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.ConditionalDiscrete", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", 1 );
		description_array = s;
	}

	/** Ignores <tt>pi_messages</tt>.
	  */
	public Distribution compute_lambda_message( ConditionalDistribution px_in, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
		ConditionalDiscrete px = (ConditionalDiscrete) px_in;

		Discrete lambda_message = new Discrete();
		lambda_message.ndims = px.ndimensions_parent();
		lambda_message.dimensions = (int[]) px.dimensions_parents.clone();
		lambda_message.probabilities = new double[ px.probabilities.length ];

		double[] iu = new double[ px.ndimensions_parent() ];
		lambda_summation( lambda_message.probabilities, lambda, px, iu, 0 );
		return lambda_message;
	}

	void lambda_summation( double[] p, Distribution lambda, ConditionalDiscrete px, double[] iu, int m ) throws Exception
	{
		if ( m == px.ndimensions_parent() )
		{
			// We have iu[] set up with some combination of parent indexes;
			// compute summation over child variable. Then figure out
			// where to put result -- compute indexing polynomial.

			double[] ix = new double[ px.ndimensions_child() ];
			int ii, k;

			for ( ii = 0, k = 0; k < px.ndimensions_parent()-1; k++ )
				ii += px.dimensions_parents[k+1] * (ii + (int) iu[k]);
			ii += (int) iu[ px.ndimensions_parent()-1 ];

			p[ii] = inner_lambda_summation( lambda, px, iu, ix, 0 );
		}
		else
		{
			for ( int k = 0; k < px.dimensions_parents[m]; k++ )
			{
				iu[m] = k;
				lambda_summation( p, lambda, px, iu, m+1 );
			}
		}
	}

	double inner_lambda_summation( Distribution lambda, ConditionalDiscrete px, double[] iu, double[] ix, int n ) throws Exception
	{
		if ( n == px.ndimensions_child() )
		{
			return lambda.p( ix ) * px.p( ix, iu );
		}
		else
		{
			double sum = 0;
			for ( int k = 0; k < px.dimensions_child[n]; k++ )
			{
				ix[n] = k;
				sum += inner_lambda_summation( lambda, px, iu, ix, n+1 );
			}

			return sum;
		}
	}
}
