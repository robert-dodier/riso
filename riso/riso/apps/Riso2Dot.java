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
import riso.belief_nets.*;
import SmarterTokenizer;

public class Riso2Dot
{
	public static void main( String[] args )
	{
		try
		{
			// SECURITY MANAGER BARFS ON Naming.lookup !!! WHY WAS THIS LINE PUT IN ???
			// System.setSecurityManager( new java.rmi.RMISecurityManager() );
			BeliefNetworkContext bnc = new BeliefNetworkContext(null);
			AbstractBeliefNetwork bn;
			
			try
			{
				String s = "rmi://"+args[0];
				Object o = Naming.lookup(s);
				bn = (AbstractBeliefNetwork) o;
			}
			catch (NotBoundException e) { bn = bnc.load_network( args[0] ); }

			System.out.print( bn.dot_format() );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		System.exit(1);
	}
}
