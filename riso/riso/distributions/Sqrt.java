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

public class Sqrt extends AbstractConditionalDistribution
{
	/** Do-nothing constructor.
	  */
	public Sqrt() {}

	/** Return the number of dimensions of the child variable.
	  * @return The value returned is always 1.
	  */
	public int ndimensions_child() { return 1; }

	/** Return the number of dimensions of the parent variables, which is always 1.
	  */
	public int ndimensions_parent() { return 1; }

	/** For a given value <code>c</code> of the parents, return a distribution
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  */
	public Distribution get_density( double[] c ) throws Exception
	{
		return new GaussianDelta( Math.sqrt(c[0]) );
	}

	/** @param c Value of the parent variable.
	  */
	public double p( double[] x, double[] c ) throws Exception
	{
		if ( x[0] == Math.sqrt(c[0]) )
			return Double.POSITIVE_INFINITY;
		else
			return 0;
	}

	/** Always returns <tt>Math.sqrt(c[0])</tt>, since the cross-section is concentrated on that point.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws Exception
	{
		double[] x = new double[1];
		x[0] = Math.sqrt(c[0]);
		return x;
	}

	/** Create a description of this distribution as a string.
	  * @param leading_ws This argument is ignored.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		return this.getClass().getName()+"\n";
	}
}
