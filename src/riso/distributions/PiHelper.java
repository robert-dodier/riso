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
package riso.distributions;
import java.util.*;
import riso.general.*;

public interface PiHelper extends java.io.Serializable
{
	/** Compute the predictive support for a variable. This is defined
	  * as follows: This node is <tt>y</tt>, its parents are <tt>x1,
	  * x2,...,xn</tt>, and the evidence on paths above this node is <tt>
	  * ``e above y''<tt>. The predictive support for <tt>y</tt> is
	  * defined as <tt>p(y|``e above y'')</tt>.
	  * <pre>
	  *   p(y|``e above y'') = \int_x1 ... \int_xn p(y|x1,...,xn) \times
	  *      p(x1|``e above y'') ... p(xn|``e above y'') dxn ... dx1
	  *    = \int_x1 p(x1|``e above y'') \int_x2 p(x2|``e above y'')
	  *      ... \int_xn p(xn|``e above y'') p(y|x1,x2,...,xn) dxn ... dx2 dx1
	  * </pre>
	  * In the case of discrete variables, the integrations are summations.
	  * An integration (or summation) can be over more than one dimension.
	  *
	  * @param x Node of interest.
	  * @param pi List of incoming pi messages, <tt>p(xk|``e above y'')</tt>.
	  *   Note that some of the evidence above <tt>y</tt> can be below <tt>xk</tt>.
	  * @return The predictive support, <tt>p(y|``e above y'')</tt>.
	  * @throws Exception If the arguments don't have types which can be
	  *   processed by an implementation of this interface. If the arguments
	  *   have types which match the name schema, then usually a message
	  *   can be computed, although it's not guaranteed that some other
	  *   kind of problem won't arise. 
	  */
	public Distribution compute_pi( ConditionalDistribution y, Distribution[] pi_messages ) throws Exception;

    public SeqTriple[] description();
}
