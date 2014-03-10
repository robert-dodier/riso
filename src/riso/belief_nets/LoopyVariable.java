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

import java.rmi.*;
import riso.distributions.*;

public class LoopyVariable extends Variable
{
	public LoopyVariable() throws RemoteException {}

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

		pi = null;
		pi_messages[ parent_index ] = null;
		posterior = null;

		notify_observers( "pi", this.pi );
		notify_observers( "posterior", this.posterior );
	}
}
