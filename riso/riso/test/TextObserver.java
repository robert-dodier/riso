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
package riso.render;

import java.rmi.*;
import java.util.*;
import riso.belief_nets.*;
import riso.distributions.*;
import riso.remote_data.*;

/** An instance of this class is an observer which prints a message when
  * the observable it is watching is updated. This is intended for debugging purposes,
  * to verify that observables are updated when we think they ought to be.
  */
public class TextObserver extends RemoteObserverImpl
{
	public TextObserver() throws RemoteException {}

	/** This method is called by the variable being watched after 
	  * the variable has changed. When that happens, print out a report
	  * about the current posterior distribution of the variable.
	  */
	public void update( RemoteObservable o, Object of_interest, Object arg ) throws RemoteException
	{
		try
		{
			AbstractVariable x = (AbstractVariable) of_interest;
			Distribution p = (Distribution) arg;
			System.out.println( "local time: "+(new Date())+" "+x.get_fullname()+" mean: "+p.expected_value()+", stddev: "+p.sqrt_variance() );
		}
		catch (Exception e)
		{
			System.err.println( "TextObserver.update: barf: "+e );
		}
	}

	/** Registers this object as an observer interested in the
	  * variables listed on the command line by <tt>-v</tt> options.
	  * Usage:
	  * <pre>
	  *   java riso.render.TextObserver -b belief-network -v variable1 -v variable2 ...
	  * </pre>
	  */
	public static void main( String args[] )
	{
		try
		{
			BeliefNetworkContext bnc = new BeliefNetworkContext(null);
			Remote bn = null;
			String bn_name = null;

			TextObserver to = new TextObserver();

			for ( int i = 0; i < args.length; i++ )
			{
				if ( args[i].charAt(0) != '-' ) continue;

				switch ( args[i].charAt(1) )
				{
				case 'b':
					bn_name = args[++i];
					try { bn = bnc.get_reference( NameInfo.parse_beliefnetwork(bn_name,bnc) ); }
					catch (Exception e)
					{
						System.err.println( "TextObserver: can't get reference to "+bn_name+"; give up." );
						e.printStackTrace();
						System.exit(1);
					}
					break;
				case 'v':
					String xname = args[++i];
					if ( bn_name == null )
						System.err.println( "TextObserver: can't get reference to "+xname+"; don't know belief network yet." );
					else
					{
						try
						{
							AbstractVariable x = (AbstractVariable) ((AbstractBeliefNetwork)bn).name_lookup( xname );
							((RemoteObservable)bn).add_observer( to, x );
						}
						catch (Exception e)
						{
							System.err.println( "TextObserver: attempt to process "+xname+" failed." );
							e.printStackTrace();
						}
					}
					break;
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
