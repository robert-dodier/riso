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
import java.io.*;
import java.rmi.*;
import java.util.*;
import riso.distributions.*;
import riso.approximation.*;
import riso.numerical.*;
import riso.general.*;

public class AbstractDistribution implements LambdaHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely any number of <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[1];
		s[0] = new SeqTriple( "riso.distributions.AbstractDistribution", -1 );
		description_array = s;
	}

	/** Compute the likelihood function, called ``lambda'', for a variable.
	  * This is defined as <code>p(``e below''|x)</code>, and can be
	  * computed in terms of the lambda-messages coming up from
	  * child variables as
	  * <pre>
	  *   p(``e below''|x) = \prod_{i=1}^{n_children} lambda_i(x)
	  * </pre>
	  * That is, lambda is just the product of the incoming lambda messages.
	  *
	  * <p> This helper can handle any kind of continuous lambda messages.
	  * The lambda is approximated by a mixture of Gaussians. Other handlers
	  * for specific types should be tried before this one, since the result
	  * is at best an approximation, and since the approximation involves
	  * a numerical integration, the approximation may fail altogether.
	  *
	  * @param lambda_messages Likelihood messages from each child; some might
	  *   be noninformative -- those are skipped. However, all messages must
	  *   be continuous -- none can be <tt>instanceof Discrete</tt>.
	  * @return The product of the incoming likelihood messages.
	  * @throws IllegalArgumentException If some message is <tt>Discrete</tt>.
	  */
	public Distribution compute_lambda( Distribution[] lambda_messages ) throws Exception
	{
		// Some of the lambda messages may noninformative; skip over those.
		// Construct a list containing only the informative lambda messages.

		int i, j, ninformative = 0;

		for ( i = 0; i < lambda_messages.length; i++ )
		{
			if ( lambda_messages[i] instanceof Discrete )
				throw new IllegalArgumentException( "riso.distributions.computes_lambda.AbstractDistribution.compute_lambda: at least one lambda message is Discrete; can't handle it." );

			if ( !( lambda_messages[i] instanceof Noninformative ) )
				++ninformative;
		}
			
		Distribution[] informative_lambdas = new Distribution[ ninformative ];

		for ( i = 0, j = 0; i < lambda_messages.length; i++ )
		{
			if ( lambda_messages[i] == null || lambda_messages[i] instanceof Noninformative )
				continue;

			informative_lambdas[j++] = lambda_messages[i];
		}

		if ( j == 1 ) return informative_lambdas[0];

		// WHATS THE HANGUP HERE WITH DISCRETE VARIABLES ???
		DistributionProduct dp = new DistributionProduct( true, false, informative_lambdas );	// ASSUME NON-DISCRETE !!!
		return dp;
	}
}
