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

public class LinearCombination extends FunctionalRelation
{
	public double[] a;

	/** Do-nothing constructor.
	  */
	public LinearCombination() {}

	/** Return the number of dimensions of the parent variables.
	  */
	public int ndimensions_parent()
	{
		try { return associated_variable.get_parents().length; }
		catch (java.rmi.RemoteException e) { throw new RuntimeException( "LinearCombination.ndimensions_parent: failed, "+e ); }
	}

	/** Return the linear combination
	  * <pre>
	  *   a[0] x[0] + a[1] x[1] + ... + a[n-1] x[n-1]
	  * </pre>
	  * where <tt>n == x.length</tt>.
	  */
	public double f( double[] x )
	{
		double sum = 0;
		for ( int i = 0; i < x.length; i++ )
			sum += a[i]*x[i];
		return sum;
	}

	/** Create a description of this distribution as a string.
	  * @param leading_ws This argument is ignored.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		String s = this.getClass().getName()+" { ";
		for ( int i = 0; i < a.length; i++ )
			s += a[i]+" ";
		return s+"}\n";
	}

	/** Parse a description with this format:
	  * <pre>
	  *   { float float float ... }
	  * </pre>
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			java.util.Vector alist = new java.util.Vector();

			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "LinearCombination.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == '}' )
				{
					found_closing_bracket = true;
					break;
				}
				else
				{
					alist.addElement( st.sval );
				}
			}

			a = new double[ alist.size() ];
			for ( int i = 0; i < alist.size(); i++ )
				a[i] = numerical.Format.atof( (String) alist.elementAt(i) );
		}
		catch (IOException e)
		{
			throw new IOException( "LinearCombination.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "LinearCombination.pretty_input: no closing bracket on input." );
	}
}
