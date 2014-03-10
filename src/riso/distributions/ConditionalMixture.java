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
import riso.general.*;

/** An instance of this class represents a conditional mixture model.
  * This is similar to an unconditional mixture (represented by the
  * <tt>Mixture</tt> class), but the mixing coefficients can vary with
  * the context (i.e., the parent variables). In the <tt>Mixture</tt> class,
  * mixing coefficients are stored in an array, since they don't change;
  * here, each mixing coefficient is returned by a function that takes the
  * context as an argument. In addition, the mixture components are
  * conditional distributions, not unconditional.
  *
  * <p> This class is declared <tt>abstract</tt> (i.e., it cannot be
  * instantiated) because there is no generic way to compute the mixing
  * coefficient function; each derived class implements that in its own way.
  */
public abstract class ConditionalMixture extends AbstractConditionalDistribution
{
	/** This array contains a reference to each mixture component.
	  * Each component is a conditional distribution.
	  */
	public ConditionalDistribution[] components;

	/** Returns the mixing coefficient for the <tt>i</tt>'th component,
	  * given the parent context <tt>c</tt>.
	  */
	public abstract double mixing_coefficient( int i, double[] c );
}
