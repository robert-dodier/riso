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
package riso.distributions.computes_lambda;
import java.util.*;
import riso.distributions.*;
import riso.general.*;

public class Discrete implements LambdaHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely any number of <tt>Discrete</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[1];
		s[0] = new SeqTriple( "riso.distributions.Discrete", -1 );
		description_array = s;
	}

	/** Compute the likelihood function for a variable. This is defined
	  * as <code>p(``e below''|x)</code> ... NEEDS WORK !!!
	  */
	public riso.distributions.Distribution compute_lambda( riso.distributions.Distribution[] lambda_messages ) throws Exception
	{
		int i, j;

		// Some of the lambda messages may be null or noninformative; skip over those.

		riso.distributions.Discrete p = null;
		boolean first_informative = true;

		for ( i = 0; i < lambda_messages.length; i++ )
		{
			if ( lambda_messages[i] == null || lambda_messages[i] instanceof Noninformative )
				continue;

			if ( first_informative )
			{
				p = (riso.distributions.Discrete) lambda_messages[i].clone();
				first_informative = false;
				continue;
			}

			riso.distributions.Discrete q = (riso.distributions.Discrete) lambda_messages[i];
			if ( q.probabilities.length != p.probabilities.length )
				throw new IllegalArgumentException( "computes_lambda.Discrete.compute_lambda: some lambda messages have different lengths." );

			for ( j = 0; j < p.probabilities.length; j++ )
				p.probabilities[j] *= q.probabilities[j];
		}

		// Strictly speaking, we don't have to normalize the result,
		// but this code may be called from someplace that needs it normalized.

		double sum = 0;
		for ( i = 0; i < p.probabilities.length; i++ ) sum += p.probabilities[i];
		for ( i = 0; i < p.probabilities.length; i++ ) p.probabilities[i] /= sum;
		
		return p;
	}
}
