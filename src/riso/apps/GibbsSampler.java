/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 2004, Robert Dodier.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of version 2 of the GNU General Public License as
 * published by the Free Software Foundation.
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
import riso.approximation.*;
import riso.numerical.*;

public class GibbsSampler
{
	public static void main( String[] args )
	{
		try
		{
			String bn_name = "", host_name = "localhost";
            int n = 1;

            for ( int i = 0; i < args.length; i++ )
            {
                switch (args[i].charAt(1))
                {
                case 'b':
                    bn_name = args[++i];
                    break;
                case 'h':
                    host_name = args[++i];
                    break;
                case 'n':
                    n = Integer.parseInt (args[++i]);
                    break;
                }
            }

            System.err.println( "GibbsSampler: bn_name: "+bn_name+", host_name: "+host_name+", n: "+n );

			Remote remote = Naming.lookup( "rmi://"+host_name+"/"+bn_name );
			AbstractBeliefNetwork bn = (AbstractBeliefNetwork) remote;
			
            AbstractVariable[] all_variables = bn.get_variables();
            Vector nonevidence = new Vector( all_variables.length );

            for ( int i = 0; i < all_variables.length; i++ )
            {
                Distribution p = all_variables[i].get_posterior();

                if ( p != null && p instanceof Delta )
                    System.err.println( "GibbsSampler: "+all_variables[i].get_fullname()+" is evidence." );
                else
                {
                    System.err.println( "GibbsSampler: "+all_variables[i].get_fullname()+" is not evidence." );
                    nonevidence.addElement( all_variables[i] );
                }
            }
                    
            System.err.println( "GibbsSampler: nonevidence.size: "+nonevidence.size() );

            for ( int j = 0; j < nonevidence.size(); j++ )
                System.out.print( " "+((AbstractVariable) nonevidence.elementAt(j)).get_name() );
            System.out.print("\n");

            int step = 1;

            long dt_clear_posterior = 0, dt_get_posterior = 0, dt_random = 0, dt_assign_evidence = 0;
            long t0_all = System.currentTimeMillis (), t0, t1;

			for ( int i = 0; i < n; i++ )
			{
                for ( int j = 0; j < nonevidence.size(); j++ )
                {
                    AbstractVariable x = (AbstractVariable) nonevidence.elementAt (j);

                    t0 = System.currentTimeMillis ();
                    bn.clear_posterior (x);
                    t1 = System.currentTimeMillis ();
                    dt_clear_posterior += t1 - t0;

                    t0 = System.currentTimeMillis ();
                    Distribution p = bn.get_posterior (x);
                    t1 = System.currentTimeMillis ();
                    dt_get_posterior += t1 - t0;

                    t0 = System.currentTimeMillis ();
                    double[] v = p.random ();
                    t1 = System.currentTimeMillis ();
                    dt_random += t1 - t0;

                    t0 = System.currentTimeMillis ();
                    bn.assign_evidence (x, v[0]);
                    t1 = System.currentTimeMillis ();
                    dt_assign_evidence += t1 - t0;

                    System.out.print (" "+v[0]);
                }

                System.out.print("\n");

                if ( (i+1) % step == 0 )
                {
                    System.err.println( "GibbsSampler: completed "+(i+1)+" samples." );

                    if ( i+1 == 10*step )
                        step *= 10;
                }
			}

            long t1_all = System.currentTimeMillis ();
            System.err.println ("GibbsSampler: elapsed: "+((t1_all-t0_all)/1000.0)+" [s]");
            System.err.println ("GibbsSampler: clear_posterior: "+(dt_clear_posterior/1000.0)+" [s]\t(per iteration per variable: "+(dt_clear_posterior/1000.0/n/nonevidence.size())+" [s])");
            System.err.println ("GibbsSampler:   get_posterior: "+(dt_get_posterior/1000.0)+" [s]\t(per iteration per variable: "+(dt_get_posterior/1000.0/n/nonevidence.size())+" [s])");
            System.err.println ("GibbsSampler:          random: "+(dt_random/1000.0)+" [s]\t(per iteration per variable: "+(dt_random/1000.0/n/nonevidence.size())+" [s])");
            System.err.println ("GibbsSampler: assign_evidence: "+(dt_assign_evidence/1000.0)+" [s]\t(per iteration per variable: "+(dt_assign_evidence/1000.0/n/nonevidence.size())+" [s])");
            System.err.println ("GibbsSampler:     unaccounted: "+((t1_all-(t0_all+dt_clear_posterior+dt_get_posterior+dt_random+dt_assign_evidence))/1000.0)+" [s]");

            // Return to the state of the belief network before sampling began.
            for ( int j = 0; j < nonevidence.size(); j++ )
            {
                AbstractVariable x = (AbstractVariable) nonevidence.elementAt(j);
                bn.clear_posterior(x);
            }
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
