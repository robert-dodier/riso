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
import riso.belief_nets.*;
import riso.approximation.*;
import riso.general.*;

/** This class implements a lambda message helper for a variable <tt>x</tt> with
  * one or more parents <tt>u1,...,un</tt>. Except for the parent to which
  * we are sending the lambda message, each parent sends a pi message to
  * <tt>x</tt>. Let us suppose that we are sending the lambda message to
  * parent <tt>uk</tt>. The lambda message is defined as
  * <pre>
  *   p( e \ e_u1(above) | uk )
  *     = \int p( x, e \ e_uk(above) | uk ) dx
  *     = \int p( x, e_x(below) + e_x(above) \ e_uk(above) | uk ) dx
  *     = \int p( e_x(below) | x ) p( x | e_x(above) \ e_uk(above), uk ) p( e_x(above) \ e_uk(above) | uk ) dx
  *     = p( e_x(above) \ e_uk(above) ) \int p( e_x(below) | x )
  *         \int ... \int p( x | u1,...,un ) p( u1,...,un \ uk |  e_x(above) \ e_uk(above), uk ) 
  *         du1 ... du_{k-1} du_{k+1} ... du_n dx
  * </pre>
  * The factor <tt>p( e_x(above) \ e_uk(above) )</tt> doesn't depend on <tt>uk</tt> so we can ignore it
  * (a lambda message need not integrate to anything in particular).
  * Now note that 
  * <pre>
  *    p( u1,...,un \ uk | e_x(above) \ e_uk(above), uk ) = \prod_{j \neq k} p( uj | e_uj(above) )
  * </pre>
  * so finally we have
  * <pre>
  *   p( e \ e_u1(above) | uk )
  *     = \int p( e_x(below) | x ) \int ... \int p( x | u1,...,un )
  *         \prod_{j \neq k} p( uj | e_uj(above) ) du1 ... du_{k-1} du_{k+1} ... du_n dx
  * </pre>
  * In this last equation the lambda function of <tt>x</tt> appears, <tt>p( e_x(below) | x )</tt>,
  * and the pi messages coming to <tt>x</tt> from parents other than <tt>uk</tt>,
  * <tt>p( uj | e_uj(above) ), j \neq k</tt>. The conditional distribution of <tt>x</tt> given
  * all its parents, <tt>p( x | u1,...,un )</tt>, links the other pieces together.
  *
  * <p> Note that this integral is a function of the parent <tt>uk</tt>.
  * The message which is sent up the parent is NOT a Gaussian mixture or other approximation;
  * the message is a direct representation of the integral. The evaluation of the integral is
  * put off until the posterior or lambda of <tt>uk</tt> needs to be computed.
  */
public class AbstractConditionalDistribution_AbstractDistribution_AbstractDistribution implements LambdaMessageHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>AbstractConditionalDistribution</tt>
	  * followed by one <tt>AbstractDistribution</tt>, followed by any number
	  * of <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[3];
		s[0] = new SeqTriple( "riso.distributions.AbstractConditionalDistribution", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", 1 );
		s[2] = new SeqTriple( "riso.distributions.AbstractDistribution", -1 );
		description_array = s;
	}

	public Distribution compute_lambda_message( ConditionalDistribution pxuuu, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
		return new IntegralCache( pxuuu, lambda, pi_messages );
	}
}
