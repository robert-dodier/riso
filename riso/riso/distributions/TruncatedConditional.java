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
import java.util.*;
import numerical.*;
import SmarterTokenizer;

/** An instance of this class represents a truncated conditional distribution.
  */
public class TruncatedConditional extends AbstractConditionalDistribution
{
	/** The left end of the interval of truncation.
	  */
	double left;

	/** The right end of the interval of truncation.
	  */
	double right;

	/** Empty constructor so objects can be constructed from description files.
	  */
	public TruncatedConditional() {}

	/** Returns the number of dimensions of the child variable which has this distribution.
	  * Always returns 1.
	  */
	public int ndimensions_child() { return 1; }

	/** Returns the number of parents of the child which has this distribution.
	  * Always returns 1 -- only a single parent is allowed.
	  */
	public int ndimensions_parent() { return 1; }

	/** Return a copy of this object. <tt>super.clone</tt> handles the generic copy,
	  * and this method copies only the class-specific data.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		TruncatedConditional copy = (TruncatedConditional) super.clone();
		copy.left = this.left;
		copy.right = this.right;
		return copy;
	}

	/** For a given value <code>c</code> of the parents, return a distribution
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  * 
	  * <p> This method finds the conditional distribution <tt>pcd</tt> of the parent of the variable
	  * associated with this truncated conditional distribution, and returns a <tt>Truncated</tt>
	  * constructed from the object returned by <tt>pcd.get_density</tt>.
	  *
	  * @param c Values of parent variables.
	  */
	public Distribution get_density( double[] c ) throws Exception
	{
		riso.belief_nets.AbstractVariable x;

		try { x = associated_variable.get_parents()[0]; }
		catch (Exception e) { throw new Exception( this.getClass().getName()+".get_density: can't find parent's distribution; nested: "+e ); }

		Truncated t = new Truncated( x.get_distribution().get_density(c) );
		t.left = this.left;
		t.right = this.right;
		return t;
	}

	/** Compute the density at the point <code>x</code>.
	  * This method is simply <tt>return get_density(c).p(x)</tt>, so if the conditional
	  * density is to be evaluated at many points, it will be faster to call <tt>get_density</tt>,
	  * cache the result (say <tt>r</tt>), and evaluate <tt>r.p</tt>.
	  *
	  * @param x Point at which to evaluate density.
	  */
	public double p( double[] x, double[] c ) throws Exception
	{
		return get_density(c).p(x);
	}

	/** Return an instance of a random variable from this distribution.
	  * The method body is just <tt>return get_density(c).random()</tt>, so calling
	  * <tt>get_density</tt> directly, caching the result (say <tt>r</tt>),
	  * and calling <tt>r.random()</tt> will be much faster.
	  *
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws Exception
	{
		return get_density(c).random();
	}

	/** Formats a string representation of this distribution.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		String result = "";
		result += this.getClass().getName()+" ";
		result += left+" "+right+" { }\n";
		return result;
	}

	/** Read an instance of this distribution from an input stream.
	  * @param st Stream tokenizer to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;
		Vector plist = new Vector();

		try
		{
			st.nextToken();
			left = Format.atof( st.sval );
			st.nextToken();
			right = Format.atof( st.sval );

			st.nextBlock(); // eat the empty "{ }" which must follow
		}
		catch (Exception e)
		{
			throw new IOException( this.getClass().getName()+".pretty_input: attempt to read object failed:\n"+e );
		}
	}
}
