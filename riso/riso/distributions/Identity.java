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

/** CODE BELOW SUPPORTS ONLY 1-DIMENSIONAL VARIABLES !!! SHOULD EXTEND. OH WELL !!!
  */
public class Identity extends AbstractConditionalDistribution
{
	public Object clone() throws CloneNotSupportedException { return this; }

	public int ndimensions_child() { return 1; }

	public int ndimensions_parent() { return 1; }

	/** For a given value <code>c</code> of the parents, return a distribution
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  * @param c Values of parent variables.
	  */
	public Distribution get_density( double[] c ) throws Exception
	{
		if ( associated_variable != null && associated_variable.type == Variable.VT_DISCRETE )
		{
			int[] dimensions, ic = new int[1];
			ic[0] = (int) c[0];

			if ( associated_variable.distribution instanceof ConditionalDiscrete )
				dimensions = (int[]) ((ConditionalDiscrete)associated_variable.distribution).dimensions_child.clone();
			else if ( associated_variable.distribution instanceof Discrete )
				dimensions = (int[]) ((Discrete)associated_variable.distribution).dimensions.clone();
			else if ( associated_variable.states_names != null && associated_variable.states_names.size() > 0 )
			{
				dimensions = new int[1];
				dimensions[0] = associated_variable.states_names.size();
			}
			else
				throw new Exception( "Identity.get_density: can't figure out number of states of discrete variable "+associated_variable.get_fullname() );

			return new DiscreteDelta( dimensions, ic );
		}
		else
			return new GaussianDelta(c);
	}

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  */
	public double p( double[] x, double[] c ) throws Exception
	{
		Distribution px = get_density(c);
		return px.p(x);
	}

	/** Return an instance of a random variable from this distribution.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws Exception
	{
		return c;
	}

	/** Parse a string containing a description of a variable. This method
	  * does nothing, since there's nothing to parse for this class.
	  */
	public void parse_string( String description ) throws IOException { return; }

	/** Create a description of this distribution model as a string.
	  * This method simply puts the classname into a string.
	  * @param leading_ws This argument is ignored.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		return this.getClass().getName()+"\n";
	}
}
