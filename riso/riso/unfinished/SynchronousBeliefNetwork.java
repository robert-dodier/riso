/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 2003, Robert Dodier.
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

import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.*;
import riso.distributions.*;
import riso.remote_data.*;
import riso.general.*;

public class SynchronousBeliefNetwork extends BeliefNetwork
{
	public SynchronousBeliefNetwork() throws RemoteException {}

	public void get_all_lambda_messages( Variable x ) throws Exception
	{
		check_stale( "get_all_lambda_messages" );

long t0 = System.currentTimeMillis();

		int i = 0, nmsgs = 0;

		while ( true )
		{
			AbstractVariable child = null;

			try 
			{
				for ( ; i < x.children.length; i++ )
				{
					child = x.children[i];
					AbstractBeliefNetwork child_bn;

					try { child_bn = child.get_bn(); }
					catch (ServerException e) { throw e.detail; }

					if ( x.lambda_messages[i] == null )
					{
						if ( child_bn != this && !accept_remote_child_evidence )
							x.lambda_messages[i] = new Noninformative();
						else
						{
							x.lambda_messages[i] = compute_lambda_message( x, child );
							++nmsgs;
						}
					}
					// else we don't need a lambda message; the get_bn() above checks for a stale child.
				}

				break;
			}
			catch (StaleReferenceException e) { x.remove_child( child ); }
			catch (java.rmi.ConnectException e) { x.remove_child( child ); }
			catch (Throwable t)
			{
				System.err.println( "get_all_lambda_messages: skip child["+i+"]; "+t );
				++i;
			}
		}
long t1 = System.currentTimeMillis();
System.err.println( "get_all_lambda_messages: computed "+nmsgs+" messages for "+x.get_fullname()+"; elapsed: "+((t1-t0)/1000.0)+" [s]" );
	}

	public void get_all_pi_messages( Variable x ) throws Exception
	{
		check_stale( "get_all_pi_messages" );
		int nmsgs = 0;

long t0 = System.currentTimeMillis();

		for ( int i = 0; i < x.parents.length; i++ )
		{
			AbstractBeliefNetwork parent_bn = null;

			if ( x.parents[i] != null ) 
			{
				try { parent_bn = x.parents[i].get_bn(); }
				catch (RemoteException e)
				{
					try { x.reconnect_parent(i); parent_bn = x.parents[i].get_bn(); }
					catch (RemoteException e2) {}
				}
			}

			if ( parent_bn == null )
			{
System.err.println( "get_all_pi_messages: use prior for "+x.get_fullname()+".parents["+i+"]" );
				x.pi_messages[i] = x.parents_priors[i];
			}
			else
			{
				x.pi_messages[i] = compute_pi_message( x.parents[i], x );
				++nmsgs;
			}
		}

long t1 = System.currentTimeMillis();
System.err.println( "get_all_pi_messages: computed "+nmsgs+" messages for "+x.get_fullname()+"; elapsed: "+((t1-t0)/1000.0)+" [s]" );
	}
}
