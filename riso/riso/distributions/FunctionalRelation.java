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
import riso.belief_nets.*;
import SmarterTokenizer;

/** This class is the superclass of all class which represent functional relations.
  * A functional relation is a conditional distribution which has a conditional
  * distribution which is a delta function. The location of the delta is determined
  * by the <tt>f</tt> variable.
  */
public abstract class FunctionalRelation extends AbstractConditionalDistribution
{
	/** The function which defines the functional relation.
	  * Subclasses must override this method.
	  */
	public abstract double f( double[] x ) throws Exception;

	/** Return a copy of this object; the <tt>associated_variable</tt> reference
	  * is copied -- this method does not clone the referred-to variable.
	  * Subclasses which have instance data aside from the <tt>associated_variable</tt>
	  * should override this method.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		try
		{
			FunctionalRelation copy = (FunctionalRelation) this.getClass().newInstance();
			copy.associated_variable = this.associated_variable;
			return copy;
		}
		catch (Exception e) { throw new CloneNotSupportedException( getClass().getName()+".clone failed: "+e ); }
	}

	/** Return the number of dimensions of the child variable. Always returns 1.
	  */
	public int ndimensions_child() { return 1; }

	/** For a given value <code>c</code> of the parents, return a Gaussian delta distribution
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  * @param c Values of parent variables.
	  */
	public Distribution get_density( double[] c ) throws Exception { return new GaussianDelta(f(c)); }

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  */
	public double p( double[] x, double[] c ) throws Exception { return f(c); }

	/** Return an instance of a random variable from this distribution.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws Exception
	{
		double[] x = new double[1];
		x[0] = f(c);
		return x;
	}

	/** Parse a string containing a description of a variable. The description
	  * is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException
	{
		throw new IOException( getClass().getName()+".parse_string: not implemented." );
	}

	/** Create a description of this distribution model as a string.
	  * This is a full description, suitable for printing, containing
	  * newlines and indents.
	  *
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		throw new IOException( getClass().getName()+".format_string: not implemented." );
	}
}
