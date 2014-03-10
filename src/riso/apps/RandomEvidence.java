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
import riso.distributions.*;

/** Assign random values to some variable.
  */
public class RandomEvidence
{
	/** Generate random data and assign it to a variable in a belief network.
	  * The command line arguments are:
	  * <pre>
	  *   java riso.remote_data.RandomEvidence [-h rmi-host] [-s server-name]
	  * </pre>
	  * The <tt>rmi-host</tt> is the name of the host running <tt>rmiregistry</tt>.
	  * The <tt>server-name</tt> is the name by which this data source will be known.
	  */
	public static void main( String[] args )
	{
		String bn_name = null, variable_name = null;
		int i, j;

		for ( i = 0; i < args.length; i++ )
		{
			switch ( args[i].charAt(1) )
			{
			case 'b':
				bn_name = args[++i];
				break;
			case 'x':
				variable_name = args[++i];
				break;
			}
		}

		System.err.println( "RandomEvidence: bn_name: "+bn_name+" variable_name: "+variable_name );

		try
		{
			String url = "rmi://"+bn_name;
			AbstractBeliefNetwork bn = (AbstractBeliefNetwork) Naming.lookup( url );
			AbstractVariable v = (AbstractVariable) bn.name_lookup( variable_name );
			Distribution p = bn.get_posterior( v );
			System.err.println( "RandomEvidence: sample from: " );
			System.err.println( p.format_string("\t") );

			while ( true )
			{
				double[] x = p.random();
				bn.assign_evidence( v, x[0] );
				try { Thread.sleep( 10000 ); } catch(InterruptedException e) {}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
