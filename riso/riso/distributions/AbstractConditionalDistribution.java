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
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import riso.belief_nets.*;

/** Abstract base class for conditional distributions.
  * This class implements only a few methods; most of the methods from the
  * <tt>ConditionalDistribution</tt> interface are not implemented,
  * and so must be provided by subclasses.
  *
  * <p> This classs is helpful in part because message-passing algorithms can be
  * formulated as generic for all conditional distributions -- handlers
  * are named only by classes, not by interfaces.
  */
public abstract class AbstractConditionalDistribution implements ConditionalDistribution, Serializable
{
	/** This conditional distribution is associated with the belief network variable <tt>associated_variable</tt>.
	  * This reference is necessary for some distributions, and generally useful for debugging.
	  */
	public AbstractVariable associated_variable;

	/** Cache a reference to the variable with which this conditional distribution
	  * is associated.
	  */
	public void set_variable( Variable x ) { associated_variable = x; }

	/** Return a copy of this conditional distribution.
	  */
	public abstract Object clone() throws CloneNotSupportedException;
}
