package riso.remote_data;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

public class RemoteObservableImpl extends UnicastRemoteObject implements RemoteObservable, Serializable
{
	protected Vector observer_list = new Vector();

	public RemoteObservableImpl() throws RemoteException {}

	/** Adds an observer to the list of observers watching a particular object, 
	  * <tt>of_interest</tt> within this observable.
	  */
	public void add_observer( RemoteObserver o, Object of_interest )
	{
		RemoteObserverPair p = new RemoteObserverPair( o, of_interest );
		if ( ! observer_list.contains( p ) )
		{
			System.err.println( "RemoteObservableImpl.add_observer: "+p );
			observer_list.addElement( p );
		}
		else
		{
			System.err.println( "RemoteObservableImpl.add_observer: already in observer_list: "+p );
		}
	}

	/** Removes an observer from the list of observers watching a particular 
	  * item of interest.
	  */
	public void delete_observer( RemoteObserver o, Object of_interest )
	{
		RemoteObserverPair p = new RemoteObserverPair( o, of_interest );
		if ( observer_list.removeElement(p) )
			System.err.println( "RemoteObservableImpl.delete_observer: deleted: "+p );
		else
			System.err.println( "RemoteObservableImpl.delete_observer: not found: "+p );
	}

	/** Removes an observer from all the lists of observers watching items within
	  * this observable.
	  */
	public void delete_observer( RemoteObserver o )
	{
		int i, n = observer_list.size();
		RemoteObserverPair p;
		for ( i = n-1; i >= 0; i-- )
		{
			p = (RemoteObserverPair) observer_list.elementAt(i);
			if ( p.observer == o )
			{
				System.err.println( "RemoteObservableImpl.delete_observer: delete: "+p );
				observer_list.removeElementAt(i);
			}
		}
	}

	/** Removes all observers.
	  */
	public void delete_all_observers()
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
	public void notify_observers( Object of_interest, Object arg )
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
					System.err.println( "RemoteObservableImpl.notify_observers: "+e );
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
	public void notify_observers( Object of_interest ) throws RemoteException
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
					System.err.println( "RemoteObservableImpl.notify_observers: "+e );
					delete_observer( p.observer, of_interest );
				}
			}
		}
	}

	/** Notifies all observers watching this observable.
	  * <p>If an <tt>update</tt> call fails with a <tt>RemoteException</tt>,
	  * the observer is removed from the list of observers for this observable.
	  */
	public void notify_all_observers() throws RemoteException
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
				System.err.println( "RemoteObservableImpl.notify_all_observers: "+e );
				delete_observer( p.observer, p.of_interest );
			}
		}
	}

	public void register( String host, String server ) throws Exception
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

			if ( this.observer.equals(another_pair.observer) && this.of_interest.equals(another_pair.of_interest) )
				return true;
			else	
				return false;
		}
		else
			return false;
	}

	public String toString()
	{
		return "[observer: "+observer+", of_interest: "+of_interest+"]";
	}
}
