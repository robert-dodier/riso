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
import java.rmi.*;
import java.util.*;
import riso.distributions.*;
import riso.belief_nets.*;
import riso.approximation.*;
import riso.numerical.*;
import riso.general.*;

/** This class implements a lambda message helper for a variable <tt>x</tt> with
  * one parents. The pi message from the parent is ignored; it should be null.
  * This helper simply returns a helper from the more general helper class
  * which handles variables with two or more parents.
  */
public class AbstractConditionalDistribution_AbstractDistribution_ implements LambdaMessageHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>AbstractConditionalDistribution</tt>
	  * followed by one <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.AbstractConditionalDistribution", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", 1 );
		description_array = s;
	}

	public Distribution compute_lambda_message( ConditionalDistribution pxu, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
if ( pi_messages.length != 1 || pi_messages[0] != null ) throw new Exception( "AbsCondDist_AbsDist_.compute_lambda_message: pi_messages not 1 null msg." );
		return new IntegralCache( pxu, lambda, pi_messages );
	}
}

