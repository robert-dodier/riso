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
import riso.belief_nets.*;
import numerical.*;
import SmarterTokenizer;

/** An instance of this class represents a first-order autoregressive (AR1) model.
  */
public class AR1 extends AbstractConditionalDistribution
{
	/** This variable represents the correlation coefficient of this model.
	  * The variable's name must contain "rho".
	  */
	AbstractVariable rho_parent = null;

	/** This variable represents the noise magnitude of this model.
	  * The variable's name must contain "sigma".
	  */
	AbstractVariable sigma_parent = null;

	/** This variable represents the previous value of the state of the autoregressive process.
	  * The variable's name must contain "prev".
	  */
	AbstractVariable prev_parent = null;

	/** Return a copy of this object.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		try 
		{
			AR1 copy = (AR1) this.getClass().newInstance();
			return copy;
		}
		catch (Exception e) { throw new CloneNotSupportedException( this.getClass().getName()+".clone failed: "+e );
	}

	/** Return the number of dimensions of the child variable (always 1).
	  */
	public int ndimensions_child()
	{
		return 1;
	}

	/** Return the number of dimensions of the parent variables.
	  * If there is more than one parent, this is the sum of the dimensions
	  * of the parent variables -- since there are always 2 parents, this method always returns 2.
	  */
	public int ndimensions_parent()
	{
		return 2;
	}

	/** For a given value <code>c</code> of the parents, return a distribution
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  */
	public Distribution get_density( double[] c ) throws Exception
	{
		return new Gaussian( rho*x_prev, sigma_epsilon );
	}

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  * @param c Values of parent variables.
	  */
	public double p( double[] x, double[] c ) throws Exception
	{
		return Gaussian.g1( x, rho*x_prev, sigma_epsilon );
	}

	/** Return an instance of a random variable from this distribution.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws Exception
	{
		return get_density(c).random();
	}

	/** Parse a string containing a description of a variable. The description
	  * is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
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
		int i, j;
		String result = "", more_ws = leading_ws+"\t", still_more_ws = leading_ws+"\t\t";

		result += this.getClass().getName()+"\n"+leading_ws+"{"+"\n";

		// Should catch RemoteException from get_fullname() and print "(unreachable)" in that case. !!!
		result += more_ws+"% rho parent: "+(rho_parent==null?"(null)":rho_parent.get_fullname())+"\n";
		result += more_ws+"% sigma parent: "+(sigma_parent==null?"(null)":sigma_parent.get_fullname())+"\n";
		result += more_ws+"% prev parent: "+(prev_parent==null?"(null)":prev_parent.get_fullname())+"\n";

		result += leading_ws+"}\n";
		return result;
	}

	/** Since an <tt>AR1</tt> has no parameters, this method does nothing.
	  * (The parents which represent the correlation coefficient, the noise magnitude, and the
	  * previous state are specified in the <tt>parents</tt> list of the variable associated with
	  * this <tt>AR1</tt> model.)
	  * @param st Ignored.
	  */
	public void pretty_input( SmarterTokenizer st ) {}
}
