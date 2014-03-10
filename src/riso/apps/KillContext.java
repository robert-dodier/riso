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

import java.rmi.*;
import riso.belief_nets.*;
import riso.remote_data.*;

public class KillContext
{
	public static void main( String[] args )
	{
		if ( args.length != 1 )
		{
			System.err.println( "usage: jre riso.apps.KillContext context-name" );
			System.exit(1);
		}

		try
		{
			String context_url = "rmi://"+args[0];
			System.err.println( "KillContext: context_url: "+context_url );
			AbstractBeliefNetworkContext bnc = (AbstractBeliefNetworkContext) Naming.lookup( context_url );
			bnc.exit();
		}
		catch (UnmarshalException e)
		{
			System.err.println( "exception: "+e+"\n\t(As expected, since remote process died.)" );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
