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
import riso.general.*;

public class Product extends FunctionalRelation
{
	/** This is the number of inputs for this product.
	  * This is either specified directly (in a constructor) or by counting
	  * the number of parents of the associated variable.
	  */
	int ninputs;

	/** Default constructor for a product.
	  * The number of inputs is set to 0; it is assumed that a variable will be
	  * associated with this distribution before any methods that need the number of inputs
	  * are called.
	  */
	public Product() { ninputs = 0; }

	/** This constructor sets the number of inputs.
	  */
	public Product( int ninputs_in )
	{
		this.ninputs = ninputs_in;
	}

	/** Return a copy of this object.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		Product copy = (Product) super.clone();
		copy.ninputs = this.ninputs;
		return copy;
	}

	/** Return the number of dimensions of the parent variables.
	  * @return The value returned is equal to the number of parents.
	  * @throws RuntimeException If the number of inputs was not specified in 
	  *   a constructor and this or gate is not associated with a variable,
	  *   or if the attempt to count the parents fails.
	  */
	public int ndimensions_parent() 
	{
		if ( ninputs == 0 )
		{
			if ( associated_variable == null )
			{
				throw new RuntimeException( "Product.ndimensions_parents: can't tell how many inputs." );
			}
			else
			{
				try { ninputs = ((AbstractVariable)associated_variable).get_parents().length; }
				catch (RemoteException e) { throw new RuntimeException( "Product.ndimensions_parents: attempt to count parents failed." ); }
			}
		}

		return ninputs;
	}

	/** Read in a <tt>Product</tt> from an input stream. This is intended for
	  * input from a human-readable source; this is different from object serialization.
	  * The input looks like this: 
	  * <pre>
	  *   { [ninputs ninputs-value] }
	  * </pre>
	  * If <tt>ninputs</tt> is not specified, it is determined by counting parent variables.
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
				System.err.println( "Product.pretty_input: no description; accept default parameters." );
				st.pushBack();
				return;
			}

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "ninputs" ) )
				{
					st.nextToken();
					ninputs = Integer.parseInt( st.sval );
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
			throw new IOException( "Product.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "Product.pretty_input: no closing bracket on input." );
	}

	/** Create a description of this distribution as a string.
	  * If this distribution is associated with a variable, the number of inputs
	  * is not put into the description.
	  * @param leading_ws This argument is ignored.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		String result = this.getClass().getName()+" { ";
		if ( associated_variable == null )
			result += "ninputs "+ninputs+" ";
		result += "}\n";
		return result;
	}

	/** Compute the product of the inputs.
	  */
	public double F( double[] c )
	{
		double product = 1;
		for ( int i = 0; i < c.length; i++ ) product *= c[i];
		return product;
	}
	
	/** Compute the gradient of the product of the inputs.
	  */
	public double[] dFdx( double[] c )
	{
		double[] grad = new double[c.length];
		for ( int i = 0; i < grad.length; i++ )
		{
			grad[i] = 1;
			for ( int j = 0; j < c.length; j++ ) if ( j != i ) grad[i] *= c[j];
		}

		return grad;
	}
}
