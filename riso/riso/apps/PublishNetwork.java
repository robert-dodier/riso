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
import java.util.*;
import riso.belief_nets.*;
import riso.remote_data.*;

class Op
{
	String bn_name, operation;
	Op( String s1, String s2 ) { bn_name = s1; operation = s2; }
}

public class PublishNetwork
{
	public static void main( String[] args )
	{
		String context_name = "";
		Vector bn_operations = new Vector();
		int nloaded = 0;

		for ( int i = 0; i < args.length; i++ )
		{
			if ( args[i].charAt(0) != '-' ) continue;

			switch ( args[i].charAt(1) )
			{
			case 'c':
				context_name = args[++i];
				break;
			case 'b':
				bn_operations.addElement( new Op(args[++i],"b") );
				break;
			case 'u':
				bn_operations.addElement( new Op(args[++i],"u") );
				break;
			case 'r':
				bn_operations.addElement( new Op(args[++i],"r") );
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
				System.err.println( "PublishNetwork: context_url: "+context_url );
				bnc = (AbstractBeliefNetworkContext) Naming.lookup( context_url );
			}
		}
        catch (NotBoundException e)
        {
            System.err.println ("PublishNetwork: no such context: ``"+context_name+"''; exception: "+e);
            System.exit(1);
        }
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}

		for ( int i = 0; i < bn_operations.size(); i++ )
		{
			try
			{
				Op op = (Op) bn_operations.elementAt(i);
				switch ( op.operation.charAt(0) )
				{
				case 'b':
					bn = (AbstractBeliefNetwork) bnc.load_network( op.bn_name );
					if ( ! bn.get_name().equals( op.bn_name ) )
					{
						System.err.println( "PublishNetwork: filename "+op.bn_name+" not same as belief network name "+bn.get_name()+". Do not bind." );
						break;
					}

					System.err.println( "PublishNetwork: bind belief net: "+bn.get_fullname() );
					bnc.bind(bn);
					++nloaded;
					break;
				case 'r':
					bn = (AbstractBeliefNetwork) bnc.load_network( op.bn_name );
					if ( ! bn.get_name().equals( op.bn_name ) )
					{
						System.err.println( "PublishNetwork: filename "+op.bn_name+" not same as belief network name "+bn.get_name()+". Do not rebind." );
						break;
					}

					System.err.println( "PublishNetwork: bind or rebind belief net: "+bn.get_fullname() );
					bnc.rebind(bn);
					++nloaded;
					break;
				case 'u':
					System.err.println( "PublishNetwork: unbind: "+op.bn_name );
					Remote o = Naming.lookup( "rmi://"+op.bn_name );
					((Perishable)o).set_stale();
					Naming.unbind( "rmi://"+op.bn_name );
					break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.err.println( "PublishNetwork: stagger on." );
			}
		}

		if ( nloaded == 0 ) System.exit(0);
	}
}
