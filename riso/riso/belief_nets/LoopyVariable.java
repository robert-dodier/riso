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
package riso.belief_nets;

import java.io.*;
import java.rmi.*;
import riso.distributions.*;
import riso.general.*;

public class LoopyVariable extends Variable
{
	public LoopyVariable() throws RemoteException {}

	/** Set the likelihood function for this variable.
	  * This method will send ``invalid lambda message'' to the parents of this variable,
	  * and ``invalid pi message'' to the children of this variable.
	  * All lambda messages are cleared, and the posterior is cleared.
	  */
	public void set_lambda( Distribution p ) throws RemoteException
	{
		check_stale( "set_lambda" );

		for ( int i = 0; i < lambda_messages.length; i++ ) lambda_messages[i] = null;

		notify_all_invalid_pi_message();
		notify_all_invalid_lambda_message();

		lambda = p;
		posterior = null;

		notify_observers( "lambda", this.lambda );
		notify_observers( "posterior", this.posterior );
	}

	/** Set the predictive distribution for this variable.
	  * This method will send ``invalid lambda message'' to the parents of this variable,
	  * and ``invalid pi message'' to the children of this variable.
	  * All pi messages are cleared, and the posterior is cleared.
	  */
	public void set_pi( Distribution p ) throws RemoteException
	{
		check_stale( "set_pi" );

		for ( int i = 0; i < pi_messages.length; i++ ) pi_messages[i] = null;

		notify_all_invalid_pi_message();
		notify_all_invalid_lambda_message();

		pi = p;
		posterior = null;

		notify_observers( "pi", this.pi );
		notify_observers( "posterior", this.posterior );
	}
	
	/** Set the posterior distribution for this variable.
	  * This method will send ``invalid lambda message'' to the parents of this variable,
	  * and ``invalid pi message'' to the children of this variable.
	  * All pi and lambda messages are cleared. <tt>pi</tt> is set to the argument <tt>p</tt>,
	  * and <tt>lambda</tt> is set to <tt>Noninformative</tt>.
	  * THIS METHOD SHOULD SPECIAL-CASE <tt>p instanceof Delta</tt> !!!
	  */
	public void set_posterior( Distribution p ) throws RemoteException
	{
		check_stale( "set_posterior" );

		posterior = p;
		pi = p;
		lambda = new Noninformative();

		notify_all_invalid_lambda_message();
		notify_all_invalid_pi_message();

		notify_observers( "pi", pi );
		notify_observers( "lambda", lambda );
		notify_observers( "posterior", posterior );
	}

	public void notify_all_invalid_lambda_message() throws RemoteException
	{
		check_stale( "notify_all_invalid_lambda_message" );

		try
		{
			for ( int i = 0; i < parents.length; i++ )
			{
				if ( parents[i] == null ) continue;

				try { parents[i].invalid_lambda_message_notification( this ); }
				catch (RemoteException e)
				{
System.err.println( "notify_all_invalid_lambda_message: "+e );
					try { reconnect_parent(i); }
					catch (java.rmi.ConnectException e2) { continue; } // parent isn't notified -- no big deal.
					parents[i].invalid_lambda_message_notification( this );
				}
			}
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
			System.err.println( "Variable.notify_all_invalid_lambda_message: stagger forward." );
			// SOMETHING MORE INFORMATIVE HERE ???
		}
	}

	public void notify_all_invalid_pi_message() throws RemoteException
	{
		check_stale( "notify_all_invalid_pi_message" );

		int i = 0;
		while ( true )
		{
			AbstractVariable child = null;

			try 
			{
				for ( ; i < children.length; i++ )
				{
					child = children[i];
					try { child.invalid_pi_message_notification( this ); }
					catch (ServerException e) { throw e.detail; }
				}

				return;
			}
			catch (java.rmi.ConnectException e) { remove_child( child ); }
			catch (StaleReferenceException e) { remove_child( child ); }
			catch (Throwable t)
			{
				System.err.println( "invalid_pi_message_notification: skip child ["+i+"]; "+t );
				++i;
			}
		}
	}

	/** This method is called by a child to notify this variable that the lambda-message
	  * from the child is no longer valid. This parent variable must clear its lambda
	  * function and, in turn, notify other variables that lambda- and pi-messages
	  * originating from this variable are no longer valid.
	  */
	public void invalid_lambda_message_notification( AbstractVariable child ) throws RemoteException
	{
		check_stale( "invalid_lambda_message_notification" );

		int i, child_index = -1;
		for ( i = 0; i < children.length; i++ )
			if ( child.equals( children[i] ) )
			{
				child_index = i;
				break;
			}

		if ( child_index == -1 ) throw new RemoteException( "Variable.invalid_lambda_message_notification: "+child.get_fullname()+" is apparently not a child of "+this.get_fullname() );

		if ( lambda_messages[ child_index ] == null )
		{	
			// Nothing to do -- we haven't received any information from the child.
			return;
		}

		lambda_messages[ child_index ] = null;
		if ( posterior instanceof Delta ) return; // nothing further to do

		lambda = null;
		posterior = null;

		notify_observers( "lambda", this.lambda );
		notify_observers( "posterior", this.posterior );

		for ( i = 0; i < parents.length; i++ )
		{
			if ( parents[i] == null ) continue;

			try { parents[i].invalid_lambda_message_notification( this ); }
			catch (RemoteException e)
			{
System.err.println( "invalid_lambda_message_notification: "+e );
				try { reconnect_parent(i); }
				catch (java.rmi.ConnectException e2) { continue; } // parent isn't notified -- no big deal.
				parents[i].invalid_lambda_message_notification( this );
			}
		}

		i = 0;
		while ( true )
		{
			AbstractVariable some_child = null;

			try 
			{
				for ( ; i < children.length; i++ )
					if ( i != child_index )
					{
						some_child = children[i];
						try { some_child.invalid_pi_message_notification( this ); }
						catch (ServerException e) { throw e.detail; }
					}

				break;
			}
			catch (java.rmi.ConnectException e)
			{
				remove_child( some_child );
				if ( i < child_index ) --child_index; // shift down one
			}
			catch (StaleReferenceException e)
			{
				remove_child( some_child );
				if ( i < child_index ) --child_index; // shift down one
			}
			catch (Throwable t)
			{
				System.err.println( "invalid_lambda_message_notification: skip child ["+i+"]; "+t );
				++i;
			}
		}
	}

	/** This method is called by a parent to notify this variable that the pi-message
	  * from the parent is no longer valid. This child variable must clear its pi
	  * distribution and, in turn, notify other variables that lambda- and pi-messages
	  * originating from this variable are no longer valid.
	  */
	public void invalid_pi_message_notification( AbstractVariable parent ) throws RemoteException
	{
		check_stale( "invalid_pi_message_notification" );

		int i, parent_index = -1;
		for ( i = 0; i < parents.length; i++ )
			if ( parent.equals( parents[i] ) )
			{
				parent_index = i;
				break;
			}

		if ( parent_index == -1 ) throw new RemoteException( "Variable.invalid_pi_message_notification: "+parent.get_fullname()+" is apparently not a parent of "+this.get_fullname() );

		if ( pi_messages[ parent_index ] == null )
		{	
			// Nothing to do -- we haven't received any information from the parent.
			return;
		}

		if ( posterior instanceof Delta )
		{
			// The posterior, pi, and pi messages (except for the one we now
			// know is invalid) for this variable remain valid, but
			// outgoing lambda messages are now invalid. 

			pi_messages[ parent_index ] = null;
			for ( i = 0; i < parents.length; i++ )
				if ( i != parent_index )
					try { parents[i].invalid_lambda_message_notification( this ); }
					catch (RemoteException e)
					{	
System.err.println( "invalid_pi_message_notification: "+e );
						try { reconnect_parent(i); }
						catch (java.rmi.ConnectException e2) { continue; } // parent isn't notified -- no big deal.
						parents[i].invalid_lambda_message_notification( this );
					}

			return;
		}

		pi = null;
		pi_messages[ parent_index ] = null;
		posterior = null;

		notify_observers( "pi", this.pi );
		notify_observers( "posterior", this.posterior );

		if ( lambda == null || !(lambda instanceof Noninformative) )
		{
			for ( i = 0; i < parents.length; i++ )
				if ( i != parent_index )
					try { parents[i].invalid_lambda_message_notification( this ); }
					catch (RemoteException e)
					{	
System.err.println( "invalid_pi_message_notification: "+e );
						try { reconnect_parent(i); }
						catch (java.rmi.ConnectException e2) { continue; } // parent isn't notified -- no big deal.
						parents[i].invalid_lambda_message_notification( this );
					}
		}

		notify_all_invalid_pi_message();
	}
}
