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

/** An object of this class represents an "exactly-one" gate. 
  * The output of the gate is 1 if exactly one of the inputs is 1, and zero otherwise.
  */
public class ExactlyOne extends AbstractConditionalDistribution
{
	/** This is the number of inputs for this "exactly-one" gate.
	  * This is either specified directly (in a constructor) or by counting
	  * the number of parents of the associated variable.
	  */
	int ninputs;

	/** Default constructor for a "exactly-one" gate.
	  * The number of inputs is set to 0; it is assumed that a variable will be
	  * associated with this distribution before any methods that need the number of inputs
	  * are called.
	  */
	public ExactlyOne() { ninputs = 0; }

	/** This constructor sets the number of inputs.
	  */
	public ExactlyOne( int ninputs_in )
	{
		this.ninputs = ninputs_in;
	}

	/** Return a copy of this object. The <tt>associated_variable</tt> field is copied,
	  * not cloned.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		try
		{
			ExactlyOne copy = (ExactlyOne) this.getClass().newInstance();
			copy.associated_variable = this.associated_variable;
			copy.ninputs = this.ninputs;
			return copy;
		}
		catch (Exception e) { throw new CloneNotSupportedException( this.getClass().getName()+".clone failed: "+e ); }
	}

	/** Return the number of dimensions of the child variable.
	  * @return The value returned is always 1.
	  */
	public int ndimensions_child() { return 1; }

	/** Return the number of dimensions of the parent variables.
	  * @return The value returned is equal to the number of parents.
	  * @throws RuntimeException If the number of inputs was not specified in 
	  *   a constructor and this "exactly-one" gate is not associated with a variable,
	  *   or if the attempt to count the parents fails.
	  */
	public int ndimensions_parent() 
	{
		if ( ninputs == 0 )
		{
			if ( associated_variable == null )
			{
				throw new RuntimeException( "ExactlyOne.ndimensions_parents: can't tell how many inputs." );
			}
			else
			{
				try { ninputs = associated_variable.get_parents().length; }
				catch (RemoteException e) { throw new RuntimeException( "ExactlyOne.ndimensions_parents: attempt to count parents failed." ); }
			}
		}

		return ninputs;
	}

	/** For a given value <code>c</code> of the parents, return a distribution
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  */
	public Distribution get_density( double[] c ) throws Exception
	{
		throw new Exception( "ExactlyOne.get_density: not implemented; should return Binomial." );
	}

	/** Compute the probability that the output is 1 (if <tt>x[0]==1</tt>) or
	  * the output is 0 (if <tt>x[0]==0). 
	  * @param c Values of parent variables -- each <tt>c[i]</tt> must be 1 or 0.
	  * @throws IllegalArgumentException If this "exactly-one" gate is not associated
	  *   with a variable, or if the attempt to count parents fails.
	  */
	public double p( double[] x, double[] c ) throws Exception
	{
		if ( ninputs == 0 )
		{
			if ( associated_variable == null )
			{
				throw new IllegalArgumentException( "ExactlyOne.p: can't tell how many inputs." );
			}
			else
			{
				try { ninputs = associated_variable.get_parents().length; }
				catch (RemoteException e) { throw new IllegalArgumentException( "ExactlyOne.p: attempt to count parents failed." ); }
			}
		}

		int nnonzero = 0;

		for ( int i = 0; i < ninputs; i++ )
			if ( c[i] != 0 )
				++nnonzero;
		
		if ( x[0] == 1 )
			return (nnonzero == 1) ? 1 : 0;
		else
			return (nnonzero == 1) ? 0 : 1;
	}

	/** Return an instance of a random variable from this distribution.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws Exception
	{
		throw new Exception( "ExactlyOne.random: not implemented; should implement via Binomial.random." );
	}

	/** Parse a string containing a description of a variable. The description
	  * is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Read in a <tt>ExactlyOne</tt> from an input stream. This is intended for
	  * input from a human-readable source; this is different from object serialization.
	  * The input looks like this: 
	  * <pre>
	  *   { [ninputs ninputs-value] }
	  * </pre>
	  *
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
				System.err.println( "ExactlyOne.pretty_input: no description; accept default parameters." );
				st.pushBack();
				return;
			}

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "ninputs" ) )
				{
					st.nextToken();
					ninputs = Format.atoi( st.sval );
				}
				else if ( st.ttype == '}' )
				{
					found_closing_bracket = true;
					break;
				}
			}
		}
		catch (IOException e)
		{
			throw new IOException( "ExactlyOne.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "ExactlyOne.pretty_input: no closing bracket on input." );
	}

	/** Create a description of this "exactly one" gate as a string.
	  * If this distribution is associated with a variable, the number of inputs
	  * is not put into the description.
	  * @param leading_ws This argument is ignored.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		String result = this.getClass()+" { ";
		if ( associated_variable == null )
			result += "ninputs "+ninputs+" ";
		result += "}\n";
		return result;
	}
}
