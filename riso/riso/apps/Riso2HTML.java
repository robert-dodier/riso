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
package riso.apps;

import java.io.*;
import riso.belief_nets.*;
import SmarterTokenizer;

public class Riso2HTML
{
	static public String header = "<head><title>RISO Belief Network</title></head>\n";

	static public String format_string( AbstractBeliefNetwork bn ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( bn.format_string() ) );
		st.ordinaryChar( '\t' );
		st.ordinaryChar( '%' );
		st.eolIsSignificant(true);

		String s = header+"<body><pre>\n";
		boolean just_saw_variable = false;
		
		for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
		{
			if ( st.ttype == StreamTokenizer.TT_WORD )
			{
				if ( just_saw_variable )
				{
					s += "<a href=\"dbn://"+bn.get_fullname()+"."+st.sval+"\">"+st.sval+"</a>"+" ";
					just_saw_variable = false;
				}
				else
				{
					s += st.sval+" ";
					if ( st.sval.indexOf("Variable") != -1 ) just_saw_variable = true;
				}
			}
			else if ( st.ttype == StreamTokenizer.TT_EOL )
				s += "\n";
			else
				s += ((char)st.ttype)+" ";
		}

		s += "</pre></body>";
		return s;
	}

	public static void main( String[] args )
	{
		try
		{
			AbstractBeliefNetwork bn = (AbstractBeliefNetwork) java.rmi.Naming.lookup( "rmi://"+args[0] );
			System.out.println( format_string(bn) );
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
