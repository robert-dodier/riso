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

public class Ratio extends FunctionalRelation
{
	/** Do-nothing constructor for a ratio.
	  */
	public Ratio() {}

	/** Return the number of dimensions of the parent variables, which is always 2.
	  */
	public int ndimensions_parent() 
	{
		return 2;
	}

	/** Returns <tt>c[0]/c[1]</tt>.
	  */
	public double F( double[] c )
	{
		return c[0]/c[1];
	}
	
	/** Returns a two-element array with components <tt>1/c[1]</tt> and <tt>-c[0]/(c[1]*c[1])</tt>.
	  */
	public double[] dFdx( double[] c )
	{
		double[] grad = new double[2];
		grad[0] = 1/c[1];
		grad[1] = -c[0]/(c[1]*c[1]);
		return grad;
	}
}
