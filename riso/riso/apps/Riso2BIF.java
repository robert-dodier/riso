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
import riso.general.*;

public class Riso2BIF
{
	static String header = 
		"<?XML VERSION=\"1.0\"?>\n"+
		"<!-- Bayesian net in Fabio Cozman's BayesNet Interchange Format.\n"+
	 	"     Produced by Riso2BIF translator by Robert Dodier. -->\n"+
		"<!-- DTD for the BIF format -->\n"+
		"<!DOCTYPE BIF [\n"+
		"	<!ELEMENT PROPERTY (#PCDATA)>\n"+
		"	<!ELEMENT TYPE (#PCDATA)>\n"+
		"	<!ELEMENT VALUE (#PCDATA)>\n"+
		"	<!ELEMENT NAME (#PCDATA)>\n"+
		"	<!ELEMENT NETWORK\n"+
		"		( NAME, ( PROPERTY | VARIABLE | PROBABILITY )* )>\n"+
		"	<!ELEMENT VARIABLE ( NAME, TYPE, ( VALUE |  PROPERTY )* ) >\n"+
		"	<!ELEMENT PROBABILITY\n"+
		"		( FOR | GIVEN | TABLE | ENTRY | DEFAULT | PROPERTY )* >\n"+
		"	<!ELEMENT TABLE (#PCDATA)>\n"+
		"	<!ELEMENT DEFAULT (TABLE)>\n"+
		"	<!ELEMENT ENTRY ( VALUE* , TABLE )>\n"+
		"]>\n"+
		"<BIF>\n"+
		"<NETWORK>\n";

	public static void main( String[] args )
	{
		int i, j, k;
		AbstractVariable[] u;

		try
		{

			BeliefNetworkContext bnc = new BeliefNetworkContext(null);
			AbstractBeliefNetwork bn;
			
			try
			{
				String s = "rmi://"+args[0];
				Object o = Naming.lookup(s);
				bn = (AbstractBeliefNetwork) o;
			}
			catch (NotBoundException e) { bn = bnc.load_network( args[0] ); }

			System.out.print( header );
			System.out.println( "<NAME>"+bn.get_name()+"</NAME>" );

			System.out.println( "<!-- Variables -->" );

			u = bn.get_variables();
			for ( k = 0; k < u.length; k++ )
			{
				AbstractVariable x = u[k];
				System.out.println( "<VARIABLE>" );
				System.out.println( "\t<NAME>"+x.get_name()+"</NAME>" );
				System.out.println( "\t<TYPE>discrete</TYPE>" );

				int nstates = 0;
				ConditionalDistribution p = x.get_distribution();
				if ( p.ndimensions_child() > 1 )
					throw new Exception( "variable "+x.get_name()+" has "+p.ndimensions_child()+" dimensions." );

				if ( p instanceof ConditionalDiscrete )
					nstates = ((ConditionalDiscrete)p).dimensions_child[0];
				else if ( p instanceof Discrete )
					nstates = ((Discrete)p).dimensions[0];
				else
					throw new Exception( "variable "+x.get_name()+" is not discrete." );

				for ( i = 0; i < nstates; i++ )
					System.out.println( "\t<VALUE>"+i+"</VALUE>" );

				System.out.println( "</VARIABLE>" );
			}

			System.out.println( "<!-- Probability Tables -->" );

			for ( k = 0; k < u.length; k++ )
			{
				AbstractVariable x = u[k];
				System.out.println( "<PROBABILITY>" );
				System.out.println( "\t<FOR>"+x.get_name()+"</FOR>" );

				String[] xparents = x.get_parents_names();
				for ( i = 0; i < xparents.length; i++ )
					System.out.println( "\t<GIVEN>"+xparents[i]+"</GIVEN>" );

				System.out.print( "\t<TABLE>" );

				if ( x.get_distribution() instanceof ConditionalDiscrete )
				{
					ConditionalDiscrete p = (ConditionalDiscrete) x.get_distribution();
					for ( i = 0; i < p.dimensions_child[0]; i++ )
					{
						for ( j = 0; j < p.probabilities.length; j++ )
							System.out.print( " "+p.probabilities[j][i] );	
					}
				}
				else // p must be Discrete; we tested for this above
				{
					Discrete p = (Discrete) x.get_distribution();
					for ( i = 0; i < p.probabilities.length-1; i++ )
						System.out.print( p.probabilities[i]+" " );
					System.out.print( p.probabilities[i] );
				}

				System.out.println( "</TABLE>" );
				System.out.println( "</PROBABILITY>" );
			}

			System.out.println( "</BIF>" );
			System.exit(0);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
