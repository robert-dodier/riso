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
import java.rmi.*;

/** An item of this class represents a ``noninformative'' lambda
  * message or lambda function, that is, one for a variable which is
  * not evidence and for which there is no downstream evidence.
  */
public class Noninformative extends AbstractDistribution
{
	/** Computes the density at the point <code>x</code>. 
	  * Always returns 1.
	  */
	public double p( double[] x ) { return 1; }

	/** The support for a noninformative lambda or lambda message is unbounded.
	  * @throws SupportNotWellDefinedException Always thrown.
	  */
	public double[] effective_support( double epsilon ) throws Exception
	{
		throw new SupportNotWellDefinedException( "Noninformative: support is not well-defined." );
	}
}
