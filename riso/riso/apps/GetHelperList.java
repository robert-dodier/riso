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
import java.net.*;
import java.rmi.*;
import java.lang.reflect.*;
import riso.belief_nets.*;
import riso.general.*;

public class GetHelperList
{
	public static void main( String[] args )
	{
		try
		{
			String cb = System.getProperty( "java.rmi.server.codebase" );
			System.err.println( "codebase: "+cb );

			AbstractBeliefNetworkContext bnc = (new BeliefNetworkContext(null)).locate_context( new URL(cb).getHost() );
			System.err.println( "obtained context: "+bnc.get_name() );

			String[] helperlist = bnc.get_helper_names( args[0] );

			System.err.println( args[0]+" helpers: " );
			for ( int i = 0; i < helperlist.length; i++ )
			{
				System.err.print( helperlist[i]+" " );
				try
				{
					Class c = java.rmi.server.RMIClassLoader.loadClass( helperlist[i] );
					System.err.println( "(OK)" );
					SeqTriple[] a = (SeqTriple[]) invoke_description(c);
					if ( a == null ) continue;
					for ( int j = 0; j < a.length; j++ )
						System.err.println( "\t"+a[j] );
				}
				catch (Exception e2) { System.err.println( "(NOT OK)" ); }
			}
		}
		catch (Exception e) { e.printStackTrace(); }

		System.exit(1);
	}

	public static Object invoke_description( Class c )
	{
		try
		{
			Method m = c.getMethod ("description", new Class[] {});

			// Since "description" is a static method, supply null as the object.
			try { return m.invoke(null, null); }
			catch (InvocationTargetException ite)
			{
				System.err.println( "invoke_description: invocation failed; " );
				ite.getTargetException().printStackTrace();
			}
			catch (Exception e) { e.printStackTrace(); }
		}
		catch (NoSuchMethodException nsme) {} // eat the exception; apparently c is not a helper

		return null;
	}
}
