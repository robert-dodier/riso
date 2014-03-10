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
import java.rmi.*;
import java.util.*;
import riso.belief_nets.*;
import riso.remote_data.*;
import riso.general.*;

public class PublishNetworkString 
{
	public static void main( String[] args )
	{
		boolean bind_all = false;
		String context_name = "";
		char op = 'b';

		for ( int i = 0; i < args.length; i++ )
		{
			if ( args[i].charAt(0) != '-' ) continue;

			switch ( args[i].charAt(1) )
			{
			case 'c':
				context_name = args[++i];
				break;
			case 'b':
				op = 'b';
				break;
			case 'r':
				op = 'r';
				break;
			case 'a':
				bind_all = true;
				System.err.println( "PublishNetworkString: bind or rebind all belief networks in input stream." );
				break;
			}
		}

		AbstractBeliefNetworkContext bnc = null;
		AbstractBeliefNetwork bn = null;

		try
		{
			if ( "".equals(context_name) )
			{
				bnc = new BeliefNetworkContext(null);
			}
			else
			{
				String context_url = "rmi://"+context_name;
				System.err.println( "PublishNetworkString: context_url: "+context_url );
				bnc = (AbstractBeliefNetworkContext) Naming.lookup( context_url );
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}

		try
		{
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );

			boolean first_time = true;
			for ( st.nextBlock(); st.ttype != StreamTokenizer.TT_EOF; st.nextBlock() )
			{
				if ( (bn = (AbstractBeliefNetwork) bnc.parse_network(st.sval)) == null ) break;

				if ( first_time || bind_all )
				{
					switch ( op )
					{
					case 'b':
						System.err.println( "PublishNetworkString: bind belief net: "+bn.get_fullname() );
						bnc.bind(bn);
						break;
					case 'r':
						System.err.println( "PublishNetworkString: bind or rebind belief net: "+bn.get_fullname() );
						bnc.rebind(bn);
						break;
					}

					first_time = false;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
