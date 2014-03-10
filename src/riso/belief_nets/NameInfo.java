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
package riso.belief_nets;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.*;

/** An instance of this class stores information related to passing messages
  * to a particular remote variable via the Internet.
  */
public class NameInfo implements java.io.Serializable
{
	public String host_name = "localhost", beliefnetwork_name = null, variable_name = null;
	public int rmi_port = Registry.REGISTRY_PORT;

	public InetAddress host = null;
	public Remote beliefnetwork = null;
	public AbstractVariable variable = null;

	public String toString() 
	{
		return host_name+":"+rmi_port+"/"+beliefnetwork_name+"."+variable_name;
	}

	public void resolve_host() throws Exception
	{
		if ( host != null ) return;
long t0 = System.currentTimeMillis();
		String host_address = InetAddress.getByName(host_name).getHostAddress();
		host = InetAddress.getByName(host_address);
		host_name = host.getHostName();
long t1 = System.currentTimeMillis();
System.err.println( "NameInfo.resolve_host: "+((t1-t0)/1000.0)+" [s] elapsed." );
	}

	public void resolve_beliefnetwork() throws Exception
	{
		if ( beliefnetwork != null ) return;
		if ( host == null ) resolve_host();
		String url = "rmi://"+host_name+":"+rmi_port+"/"+beliefnetwork_name;
		beliefnetwork = Naming.lookup( url );
	}

	public void resolve_variable() throws Exception
	{
		if ( variable != null ) return;
		if ( beliefnetwork == null ) resolve_beliefnetwork();
		variable = (AbstractVariable) ((AbstractBeliefNetwork)beliefnetwork).name_lookup(variable_name);
	}

	public static NameInfo parse_variable( String name, BeliefNetworkContext context )
	{
		return parse( name, context, true );
	}

	public static NameInfo parse_beliefnetwork( String name, BeliefNetworkContext context )
	{
		return parse( name, context, false );
	}

	public static NameInfo parse( String name, BeliefNetworkContext context, boolean is_variable )
	{
		int slash_index = name.indexOf("/"), colon_index = name.indexOf(":");

		// This next bit works correctly for nested and non-nested bn names.
		// name.substring(slash_index+1,period_index) yields the top-level name,
		// if there is a period after the slash.

		int period_index = name.substring(slash_index+1).indexOf(".");

		NameInfo info = new NameInfo();

		if ( slash_index == -1 )
		{
			// No host specified; assume the registry host of the context.
			// No RMI port specified; assume the registry port of the context.

			if ( context != null )
			{
				info.host_name = context.registry_host;
				info.rmi_port = context.registry_port;
			}
		}
		else
		{
			if ( colon_index == -1 )
			{
				// Extract specified host.
				info.host_name = name.substring(0,slash_index);

				// No RMI port specified; assume default.
				info.rmi_port = Registry.REGISTRY_PORT;
			}
			else
			{
				// Extract specified host.
				info.host_name = name.substring(0,colon_index);

				// Extract specified RMI port.
				info.rmi_port = Integer.parseInt( name.substring(0,slash_index).substring(colon_index+1) );
			}
		}

		if ( period_index == -1 )
		{
			if ( slash_index == -1 )
			{
				// Simple name.
				if ( is_variable )
				{
					// beliefnetwork_name remains null.
					info.variable_name = name;
				}
				else
				{
					info.beliefnetwork_name = name;
					// variable_name remains null.
				}
			}
			else
			{
				// Must be a belief network name -- "something/something-else".
				info.beliefnetwork_name = name.substring(slash_index+1);
				// variable_name remains null.
			}
		}
		else
		{
			// Extract belief network name and variable name.
			// Next line works correctly when slash_index == -1.
			info.beliefnetwork_name = name.substring(slash_index+1).substring(0,period_index);
			info.variable_name = name.substring(slash_index+1).substring(period_index+1);
		}

		return info;
	}

	public static void main( String[] args )
	{
		try
		{
			NameInfo i = NameInfo.parse( args[1], null, "v".equals(args[0]) );
			if ( "b".equals(args[0]) )
				i.resolve_beliefnetwork();
			else
				i.resolve_variable();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
