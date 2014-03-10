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

public class Square extends FunctionalRelation
{
	/** Do-nothing constructor.
	  */
	public Square() {}

	/** Returns the number of dimensions of the parent variables, which is always 1.
	  */
	public int ndimensions_parent() { return 1; }

	/** Returns the square of the argument.
	  */
	public double F( double[] x ) { return x[0]*x[0]; }

	/** Returns the derivative of the square function.
	  */
	public double[] dFdx( double[] x ) { double[] grad = new double[1]; grad[0] = 2*x[0]; return grad; }

	/** Creates a description of this distribution as a string.
	  * @param leading_ws This argument is ignored.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		return this.getClass().getName()+" { }\n";
	}
}
