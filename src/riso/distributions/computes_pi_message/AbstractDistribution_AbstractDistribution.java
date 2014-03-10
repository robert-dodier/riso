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
package riso.distributions.computes_pi_message;
import riso.distributions.*;
import riso.general.*;

public class AbstractDistribution_AbstractDistribution implements PiMessageHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>AbstractDistribution</tt> (the pi
	  * message) followed by any number of <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.AbstractDistribution", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", -1 );
		description_array = s;
	}

	public Distribution compute_pi_message( Distribution pi,  Distribution[] lambda_messages ) throws Exception
	{
		// Our caller has set one of the lambda messages to null, corresponding
		// to the child which will receive this pi message. From the definition
		// of the pi message, the pi message is just like computing the
		// posterior except one of the lambda messages is missing.

		Distribution[] nonnull_lambda_messages = new Distribution[ lambda_messages.length-1 ];
		for ( int i = 0, j = 0; i < lambda_messages.length; i++ )
			if ( lambda_messages[i] != null ) nonnull_lambda_messages[j++] = lambda_messages[i];

        // IS CACHED HELPER MEANINGFUL HERE ???
		LambdaHelper lh = LambdaHelperLoader.load_lambda_helper( null, nonnull_lambda_messages );
		Distribution partial_lambda = lh.compute_lambda( nonnull_lambda_messages );

        // IS CACHED HELPER MEANINGFUL HERE ???
		PosteriorHelper ph = PosteriorHelperLoader.load_posterior_helper( null, pi, partial_lambda );
		return ph.compute_posterior( pi, partial_lambda );
	}
}
