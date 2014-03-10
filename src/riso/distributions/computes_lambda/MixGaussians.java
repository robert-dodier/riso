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
import riso.distributions.*;
import riso.general.*;

/** Yeah, it's sort of confusing that this class has the same name as
  * in <tt>riso.distributions</tt>, but that's a consequence of the 
  * naming scheme used to locate message helpers. This class implements
  * a helper which can handle a list of <tt>riso.distributions.MixGaussians</tt>
  * messages.
  */
public class MixGaussians implements LambdaHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	public static double MIN_MIX_PROPORTION = 5e-3;

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely any number of <tt>MixGaussians</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[1];
		s[0] = new SeqTriple( "riso.distributions.MixGaussians", -1 );
		description_array = s;
	}

	/** @return The product of the incoming likelihood messages, which is
	  *   again a <tt>riso.distributions.MixGaussians</tt>.
	  * @see LambdaHelper.compute_lambda
	  * @see riso.distributions.MixGaussians.mixture_product
	  */
	public Distribution compute_lambda( Distribution[] lambda_messages ) throws Exception
	{
		// Some of the lambda messages may noninformative; skip over those.
		// (And if we're called for a pi message computation, one will be null as well.)
		// Construct a list containing only the informative lambda messages.

		// Some of the lambda messages might be Gaussian; promote them to MixGaussians.
		// Any messages which are not Gaussian must be MixGaussians.

		int i, j, ninformative = 0;

		for ( i = 0; i < lambda_messages.length; i++ )
		{
			if ( lambda_messages[i] != null && !( lambda_messages[i] instanceof Noninformative ) )
				++ninformative;
		}
			
		riso.distributions.MixGaussians[] informative_lambdas = new riso.distributions.MixGaussians[ ninformative ];

		for ( i = 0, j = 0; i < lambda_messages.length; i++ )
		{
			if ( lambda_messages[i] == null || lambda_messages[i] instanceof Noninformative )
				continue;

			if ( lambda_messages[i] instanceof riso.distributions.Gaussian )
				informative_lambdas[j++] = new riso.distributions.MixGaussians( (riso.distributions.Gaussian) lambda_messages[i] );
			else
				informative_lambdas[j++] = (riso.distributions.MixGaussians) lambda_messages[i];
		}

		riso.distributions.MixGaussians q = riso.distributions.MixGaussians.mixture_product( informative_lambdas );

		return q;
	}
}
