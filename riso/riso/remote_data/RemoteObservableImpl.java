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
package riso.remote_data;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

/** An instance of this class is a remote object which will notify observers about events.
  */
public class RemoteObservableImpl extends UnicastRemoteObject implements RemoteObservable, Serializable
{
	protected Vector observer_list = new Vector();

    /** This constructor calls the superclass (<tt>UnicastRemoteObject</tt>) 
      * constructor, with the global <tt>exported_objects_port</tt> as the argument.
      */
	public RemoteObservableImpl() throws RemoteException
    {
        super (riso.belief_nets.Global.exported_objects_port);
    }

	/** Adds an observer to the list of observers watching a particular object, 
	  * <tt>of_interest</tt> within this observable.
	  */
	public synchronized void add_observer( RemoteObserver o, Object of_interest )
	{
		RemoteObserverPair p = new RemoteObserverPair( o, of_interest );
		if ( ! observer_list.contains( p ) )
			observer_list.addElement( p );
	}

	/** Removes an observer from the list of observers watching a particular 
	  * item of interest.
	  */
	public synchronized void delete_observer( RemoteObserver o, Object of_interest )
	{
		RemoteObserverPair p = new RemoteObserverPair( o, of_interest );
		if ( ! observer_list.removeElement(p) )
			System.err.println( "RemoteObservableImpl.delete_observer: not found: "+p );
	}

	/** Removes an observer from all the lists of observers watching items within
	  * this observable.
	  */
	public synchronized void delete_observer( RemoteObserver o )
	{
		int i, n = observer_list.size();

		RemoteObserverPair p;
		for ( i = n-1; i >= 0; i-- )
		{
			p = (RemoteObserverPair) observer_list.elementAt(i);

			if (p.observer.equals(o))
			{
				observer_list.removeElementAt(i);
			}
		}
	}

	/** Removes all observers.
	  */
	public synchronized void delete_all_observers()
	{
		System.err.println( "RemoteObservableImpl.delete_all_observers: delete all observers" );
		observer_list.removeAllElements();
	}

	/** Notifies any observers watching the object <tt>of_interest</tt> within this
	  * observable. 
	  *
	  * <p> If an <tt>update</tt> call fails with a <tt>RemoteException</tt>,
	  * the observer is removed from the list of observers for this observable.
	  *
	  * @param of_interest An object within this observable. This parameter must not be <tt>null</tt>.
	  * @param arg The argument sent to the observer in the <tt>update</tt> call.
	  */
	public synchronized void notify_observers( Object of_interest, Object arg )
	{
		int i, n = observer_list.size();

		for ( i = n-1; i >= 0; i-- )
		{
			RemoteObserverPair p = (RemoteObserverPair) observer_list.elementAt(i);
			if ( of_interest.equals( p.of_interest ) )
			{
				try 
				{
					p.observer.update( this, of_interest, arg );
				}
				catch (RemoteException e)
				{
					delete_observer( p.observer, of_interest );
				}
			}
		}
	}

	/** Notifies any observers watching the object <tt>of_interest</tt> within this
	  * observable. The argument sent to the observer in the <tt>update</tt> call is <tt>null</tt>.
	  *
	  * <p> If an <tt>update</tt> call fails with a <tt>RemoteException</tt>,
	  * the observer is removed from the list of observers for this observable.
	  *
	  * @param of_interest An object within this observable. This parameter must not be <tt>null</tt>.
	  */
	public synchronized void notify_observers( Object of_interest ) throws RemoteException
	{
		int i, n = observer_list.size();

		for ( i = n-1; i >= 0; i-- )
		{
			RemoteObserverPair p = (RemoteObserverPair) observer_list.elementAt(i);
			if ( of_interest.equals( p.of_interest ) )
			{
				try 
				{
					p.observer.update( this, of_interest, null );
				}
				catch (RemoteException e)
				{
					delete_observer( p.observer, of_interest );
				}
			}
		}
	}

	/** Notifies all observers watching this observable.
	  * <p>If an <tt>update</tt> call fails with a <tt>RemoteException</tt>,
	  * the observer is removed from the list of observers for this observable.
	  */
	public synchronized void notify_all_observers() throws RemoteException
	{
		int i, n = observer_list.size();

		for ( i = n-1; i >= 0; i-- )
		{
			RemoteObserverPair p = (RemoteObserverPair) observer_list.elementAt(i);

			try 
			{
				p.observer.update( this, p.of_interest, null );
			}
			catch (RemoteException e)
			{
				delete_observer( p.observer, p.of_interest );
			}
		}
	}

	public synchronized void register( String host, String server ) throws Exception
	{
		String url = "rmi://"+host+"/"+server;
		System.out.print( "RemoteObservableImpl.register: url: "+url+", call Naming.bind... " );
		long t0 = System.currentTimeMillis();
		java.rmi.Naming.bind( url, this );
		long tf = System.currentTimeMillis();
		System.out.println( server+" bound in registry; time elapsed: "+((tf-t0)/1000.0)+" [s]" );
	}
}


/** Encapsulates an observer and item of interest pair.
  */
class RemoteObserverPair
{
	RemoteObserver observer;
	Object of_interest;

	RemoteObserverPair( RemoteObserver o, Object oi )
	{
		observer = o;
		of_interest = oi;
	}

	/** Two <tt>RemoteObserverPair</tt>'s are equal if they refer to the same
	  * <tt>RemoteObserver</tt> and the items of interest are equal, i.e. <tt>equals</tt>
	  * returns <tt>true</tt>.
	  */
	public boolean equals( Object another )
	{
		if ( another instanceof RemoteObserverPair )
		{
			RemoteObserverPair another_pair = (RemoteObserverPair) another;

			boolean are_eq = (this.observer.equals(another_pair.observer) && this.of_interest.equals(another_pair.of_interest));
			return are_eq;
		}
		else
			return false;
	}

	public String toString()
	{
		return "[observer: "+observer+", of_interest: "+of_interest+"]";
	}
}
