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
import numerical.Format;
import SmarterTokenizer;

public class Ratio extends AbstractConditionalDistribution
{
	/** Do-nothing constructor for a ratio.
	  */
	public Ratio() {}

	/** Return a copy of this object; the <tt>associated_variable</tt> reference
	  * is copied -- this method does not clone the referred-to variable.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		try
		{
			Ratio copy = (Ratio) this.getClass().newInstance();
			copy.associated_variable = this.associated_variable;
			return copy;
		}
		catch (Exception e) { throw new CloneNotSupportedException( "Ratio.clone failed: "+e ); }
	}

	/** Return the number of dimensions of the child variable.
	  * @return The value returned is always 1.
	  */
	public int ndimensions_child() { return 1; }

	/** Return the number of dimensions of the parent variables, which is always 2.
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
		return new GaussianDelta( c[0]/c[1] );
	}

	/** @param c Values of parent variables.
	  */
	public double p( double[] x, double[] c ) throws Exception
	{
		if ( x[0] == c[0]/c[1] )
			return Double.POSITIVE_INFINITY;
		else
			return 0;
	}

	/** Always returns <tt>c[0]/c[1]</tt>, since the cross-section is concentrated on that point.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws Exception
	{
		double[] x = new double[1];
		x[0] = c[0]/c[1];
		return x;
	}

	/** Parse a string containing a description of this distribution. The description
	  * is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Read in a <tt>Ratio</tt> from an input stream. This is intended for
	  * input from a human-readable source; this is different from object serialization.
	  * The input looks like this: 
	  * <pre>
	  *   { }
	  * </pre>
	  * @param st Stream tokenizer to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
			{
				System.err.println( "Ratio.pretty_input: no description; accept default parameters." );
				st.pushBack();
				return;
			}

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == '}' )
				{
					found_closing_bracket = true;
					break;
				}
			}
		}
		catch (IOException e)
		{
			throw new IOException( "Ratio.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "Ratio.pretty_input: no closing bracket on input; tokenizer state: "+st );
	}

	/** Create a description of this distribution as a string.
	  * @param leading_ws This argument is ignored.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		String result = this.getClass().getName()+" { }\n";
		return result;
	}
}
