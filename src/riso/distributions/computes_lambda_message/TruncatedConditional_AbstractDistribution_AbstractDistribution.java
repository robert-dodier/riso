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
import riso.distributions.*;
import riso.general.*;

public class TruncatedConditional_AbstractDistribution_AbstractDistribution implements LambdaMessageHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>TruncatedConditional</tt>
	  * followed by one <tt>AbstractDistribution</tt>, followed by any number
	  * of <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[3];
		s[0] = new SeqTriple( "riso.distributions.TruncatedConditional", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", 1 );
		s[2] = new SeqTriple( "riso.distributions.AbstractDistribution", -1 );
		description_array = s;
	}

	/** Punt: form a new lambda by factoring in a uniform distribution on the interval on which the
	  * truncated conditional is defined, then look for a lambda message helper for the
	  * new lambda, the pi_messages, and the <tt>ConditionalDistribution</tt> contained by the
	  * truncated conditional. Return the result computed by the helper so found.
	  */
	public Distribution compute_lambda_message( ConditionalDistribution py_in, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
		TruncatedConditional py = (TruncatedConditional) py_in;

		Distribution[] lambda2 = new Distribution[2];
		lambda2[0] = lambda;
		lambda2[1] = new Uniform( py.left, py.right );

        // IS CACHED HELPER MEANINGFUL HERE ???
		LambdaHelper lh = LambdaHelperLoader.load_lambda_helper( null, lambda2 );
		lambda = lh.compute_lambda( lambda2 );

		LambdaMessageHelper lmh = LambdaMessageHelperLoader.load_lambda_message_helper( py.cd, lambda, pi_messages );
		return lmh.compute_lambda_message( py.cd, lambda, pi_messages );
	}
}
