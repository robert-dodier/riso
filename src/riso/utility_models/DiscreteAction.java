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
package riso.utility_models;
import java.io.*;
import java.rmi.*;
import java.util.*;
import riso.distributions.*;
import riso.general.SmarterTokenizer;

public class DiscreteAction extends AbstractUtilityModel
{
	public Lottery[] lotteries;

	/** Create a new object, and don't fill in any of the member data.
	  */
	public DiscreteAction() {}

	/** Make a deep copy of this object and return it.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		DiscreteAction copy = (DiscreteAction) super.clone();

		copy.lotteries = (Lottery[]) new Lottery[ this.lotteries.length ];
		for ( int i = 0; i < this.lotteries.length; i++ )
			copy.lotteries[i] = (Lottery) this.lotteries [i].clone();

		return copy;
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
		String result = "";

		result += this.getClass().getName()+"\n"+leading_ws+"{"+"\n";
		String more_leading_ws = "\t"+leading_ws;
		String still_more_ws = "\t"+more_leading_ws;

		for ( int i = 0; i < lotteries.length; i++ )
		{
			result += more_leading_ws+"% Lottery["+i+"]\n";
			result += more_leading_ws+lotteries[i].format_string(more_leading_ws)+"\n";
		}

		result += leading_ws+"}\n";
		return result;
	}

	/** Read a description of this distribution from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		Vector lotteries_vector = new Vector(10);

		try
		{
			st.nextToken(); // eat the opening brace

			// Read a list of lotteries and plop them into a vector; we'll convert to array later.

			for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == '}' ) break;

				Lottery l = null;
				try { l = (Lottery) Class.forName( st.sval ).newInstance(); }
				catch (Exception e) { e.printStackTrace(); continue; }

				st.nextBlock();
				l.parse_string( st.sval );
				lotteries_vector.addElement(l);
			}
		}
		catch (IOException e)
		{
			throw new IOException( "DiscreteAction.pretty_input: attempt to read object failed:\n"+e );
		}

		lotteries = new Lottery[ lotteries_vector.size() ];
		lotteries_vector.copyInto( lotteries );
	}

	public static void main( String[] args )
	{
		try
		{
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );
			st.nextToken();
			DiscreteAction da = (DiscreteAction) Class.forName( st.sval ).newInstance();
			st.nextBlock();
			da.parse_string( st.sval );
			System.out.println( "utility model:\n"+da.format_string("") );
			
			st.nextToken();
			Distribution d = (Distribution) Class.forName( st.sval ).newInstance();
			st.nextBlock();
			d.parse_string( st.sval );
			System.out.println( "distribution: "+d.format_string("") );

			for ( int i = 0; i < da.lotteries.length; i++ )
				System.err.println( "expected value of lotteries["+i+"]: "+da.lotteries[i].expected_value(d) );
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
