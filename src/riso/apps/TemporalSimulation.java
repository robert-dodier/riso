/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 2004, Robert Dodier.
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
import riso.distributions.*;

public class TemporalSimulation
{
	/** Generate random values from a temporal belief network.
	  * The command line arguments are:
	  * <pre>
	  *   java riso.remote_data.TemporalSimulation -b bn_name -x variable_name [-x variable_name ...]
	  * </pre>
	  */
	public static void main( String[] args )
	{
		String bn_name = null;
        Vector variable_names = new Vector();

		for (int i = 0; i < args.length; i++)
		{
			switch ( args[i].charAt(1) )
			{
			case 'b':
				bn_name = args[++i];
				break;
			case 'x':
				variable_names.addElement (args[++i]);
				break;
			}
		}

		System.err.println ("TemporalSimulation: bn_name: "+bn_name+"; variable_names:"+variable_names);
        // for (int i = 0; i < variable_names.size(); i++)
            // System.err.print (variable_names.elementAt(i));
        // System.err.println ("");

		try
		{
			String url = "rmi://"+bn_name;
			AbstractTemporalBeliefNetwork bn = (AbstractTemporalBeliefNetwork) Naming.lookup (url);

            AbstractBeliefNetwork[] slices = bn.get_slices();

            System.err.println ("TemporalSimulation: slices:");
            for (int i = 0; i < slices.length; i++)
                System.err.println ("\t"+slices[i].get_fullname());

            for (int i = 0; i < slices.length; i++)
            {
			    for (int j = 0; j < variable_names.size(); j++)
                {
                    AbstractVariable v = (AbstractVariable) slices[i].name_lookup ((String) variable_names.elementAt(j));
			        Distribution p = bn.get_posterior (v);
// System.err.println( "TemporalSimulation: sample from: " );
// System.err.println( p.format_string("\t") );
                    double[] x = p.random();
                    riso.numerical.Matrix.pretty_output (x, System.out, " ");
                }

                System.out.println ("");
            }
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
