package risotto.remote_data;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

public class RemoteObservableImpl extends UnicastRemoteObject implements RemoteObservable, Serializable
{
	private boolean is_changed = false;
	protected Vector observer_table = new Vector();
	protected Hashtable interests_table = new Hashtable();

	public RemoteObservableImpl() throws RemoteException {}

	protected void add_interest( Object of_interest )
	{
		interests_table.put( of_interest, Boolean.FALSE );
	}

	/** Two objects of this type are equal iff they are the same object.
	  * That is, they are equal if their references are equal.
	  */
	public boolean equals( Object another )
	{
		if ( another instanceof RemoteObservableImpl )
			return this == (RemoteObservableImpl) another;
		else
			return false;
	}

	/** Adds an observer to the list of observers watching this observable as a whole
	  * (and not any particular object within this observable).
	  */
	public void add_observer( RemoteObserver o )
	{
		add_observer( o, this );
	}

	/** Adds an observer to the list of observers watching a particular object, 
	  * <tt>of_interest</tt>, within
	  * this observable.
	  */
	public void add_observer( RemoteObserver o, Object of_interest )
	{
		RemoteObserverPair p = new RemoteObserverPair( o, of_interest );
		if ( ! observer_table.contains( p ) )
		{
			System.err.println( "RemoteObservableImpl.add_observer: "+p );
			observer_table.addElement( p );
		}
		else
		{
			System.err.println( "RemoteObservableImpl.add_observer: already in observer_table: "+p );
		}
	}

	/** Removes an observer from the list of observers watching a particular 
	  * item of interest.
	  */
	public void delete_observer( RemoteObserver o, Object of_interest )
	{
		RemoteObserverPair p = new RemoteObserverPair( o, of_interest );
		if ( observer_table.removeElement(p) )
			System.err.println( "RemoteObservableImpl.delete_observer: deleted: "+p );
		else
			System.err.println( "RemoteObservableImpl.delete_observer: not found: "+p );
	}

	/** Removes an observer from all the lists of observers watching items within
	  * this observable.
	  */
	public void delete_observer( RemoteObserver o )
	{
		int i, n = observer_table.size();
		RemoteObserverPair p;
		for ( i = n-1; i >= 0; i-- )
			if ( (p = (RemoteObserverPair)observer_table.elementAt(i)).observer == o )
			{
				System.err.println( "RemoteObservableImpl.delete_observer: delete: "+p );
				observer_table.removeElementAt(i);
			}
	}

	/** Removes all observers.
	  */
	public void delete_all_observers()
	{
		System.err.println( "RemoteObservableImpl.delete_all_observers: delete all observers" );
		observer_table.removeAllElements();
	}

	/** Notifies any observers watching the object <tt>of_interest</tt> within this
	  * observable. 
	  *
	  * <p> The item <tt>of_interest</tt> can be a reference to this observable; that means
	  * that the observer is updated if any item within it has changed.
	  *
	  * <p> If an <tt>update</tt> call fails with a <tt>RemoteException</tt>,
	  * the observer is removed from the list of observers for this observable.
	  *
	  * @param of_interest An object within this observable. This parameter must not be <tt>null</tt>.
	  * @param arg The argument sent to the observer in the <tt>update</tt> call.
	  */
	public void notify_observers( Object of_interest, Object arg )
	{
		int i, n = observer_table.size();

		if ( has_changed( of_interest ) )
		{
			for ( i = n-1; i >= 0; i-- )
			{
				RemoteObserverPair p = (RemoteObserverPair) observer_table.elementAt(i);
				if ( of_interest.equals( p.of_interest ) )
				{
					System.err.println( "RemoteObservableImpl.notify_observers: notify: "+p );
					try 
					{
						p.observer.update( this, of_interest, arg );
					}
					catch (RemoteException e)
					{
						System.err.println( "RemoteObservableImpl.notify_observers: exception for "+p+": "+e );
						System.err.println( "RemoteObservableImpl.notify_observers: remove observer." );
						delete_observer( p.observer );
					}
				}
			}

			clear_changed( of_interest );
		}
	}

	/** Notifies any observers watching the object <tt>of_interest</tt> within this
	  * observable. The argument sent to the observer in the <tt>update</tt> call is <tt>null</tt>.
	  *
	  * <p> The item <tt>of_interest</tt> can be a reference to this observable; that means
	  * that the observer is updated if any item within it has changed.
	  *
	  * <p> If an <tt>update</tt> call fails with a <tt>RemoteException</tt>,
	  * the observer is removed from the list of observers for this observable.
	  *
	  * @param of_interest An object within this observable. This parameter must not be <tt>null</tt>.
	  */
	public void notify_observers( Object of_interest ) throws RemoteException
	{
		int i, n = observer_table.size();

		if ( has_changed( of_interest ) )
		{
			for ( i = n-1; i >= 0; i-- )
			{
				RemoteObserverPair p = (RemoteObserverPair) observer_table.elementAt(i);
				if ( of_interest.equals( p.of_interest ) )
				{
					System.err.println( "RemoteObservableImpl.notify_observers: notify: "+p );
					try 
					{
						p.observer.update( this, of_interest, null );
					}
					catch (RemoteException e)
					{
						System.err.println( "RemoteObservableImpl.notify_observers: exception for "+p+": "+e );
						System.err.println( "RemoteObservableImpl.notify_observers: remove observer." );
						delete_observer( p.observer );
					}
				}
			}

			clear_changed( of_interest );
		}
	}

	/** Notifies observers watching this observable as a whole (i.e., the ones
	  * which called <tt>add_observer</tt> with <tt>of_interest</tt> equal
	  * to <tt>this</tt>) and those watching some item within this observable.
	  * The observers watching the observable as a whole are called last.
	  *
	  * <p>If an <tt>update</tt> call fails with a <tt>RemoteException</tt>,
	  * the observer is removed from the list of observers for this observable.
	  */
	public void notify_all_observers() throws RemoteException
	{
		int i, n = observer_table.size();

		for ( i = n-1; i >= 0; i-- )
		{
			RemoteObserverPair p = (RemoteObserverPair) observer_table.elementAt(i);

			if ( p.of_interest == this )
				// Hold off on updating the observers watching the whole observable;
				// we'll get to them later.
				continue;

			if ( has_changed( p.of_interest ) )
			{
				System.err.println( "RemoteObservableImpl.notify_all_observers: notify: "+p );

				try 
				{
					p.observer.update( this, p.of_interest, null );
				}
				catch (RemoteException e)
				{
					System.err.println( "RemoteObservableImpl.notify_all_observers: exception for "+p+": "+e );
					System.err.println( "RemoteObservableImpl.notify_all_observers: remove observer." );
					delete_observer( p.observer );
				}
			}

			clear_changed( p.of_interest );
		}

		// Now update all the observers watching this observable as a whole.

		if ( has_changed( this ) )
		{
			n = observer_table.size();
			for ( i = n-1; i >= 0; i-- )
			{
				RemoteObserverPair p = (RemoteObserverPair) observer_table.elementAt(i);

				if ( p.of_interest != this )
					continue;

				System.err.println( "RemoteObservableImpl.notify_all_observers: notify: "+p );

				try 
				{
					p.observer.update( this, p.of_interest, null );
				}
				catch (RemoteException e)
				{
					System.err.println( "RemoteObservableImpl.notify_all_observers: exception for "+p+": "+e );
					System.err.println( "RemoteObservableImpl.notify_all_observers: remove observer." );
					delete_observer( p.observer );
				}
			}

			clear_changed( this );
		}
	}

	/** Tells whether the item <tt>of_interest</tt> has changed.
	  */
	public boolean has_changed( Object of_interest )
	{
		Boolean b = (Boolean) interests_table.get( of_interest );
		if ( b == null )
		{
			System.err.println( "RemoteObservableImpl.has_changed: don't know about "+of_interest );
			return false;
		}
		else
			return b.booleanValue();
	}

	/** Mark the item <tt>of_interest</tt> as "changed."
	  * Future calls to <tt>has_changed</tt> will return <tt>true</tt> until
	  * <tt>clear_changed</tt> is called.
	  * In addition to marking <tt>of_interest</tt> "changed," this observable
	  * as a whole is also marked "changed."
	  */
	public void set_changed( Object of_interest )
	{
		interests_table.put( of_interest, Boolean.TRUE );
		interests_table.put( this, Boolean.TRUE );
	}

	/** Mark the item <tt>of_interest</tt> as "not changed."
	  * Future calls to <tt>has_changed</tt> will return <tt>false</tt> until
	  * <tt>set_changed</tt> is called.
	  * This observable as a whole is NOT marked "not changed."
	  */
	public void clear_changed( Object of_interest ) { interests_table.put( of_interest, Boolean.FALSE ); }

	public void register( String host, String server )
	{
        try
        {
            java.rmi.Naming.rebind("//"+host+"/"+server, this);
            System.out.println( "RemoteObservableImpl.register: "+server+" bound in registry");
        }
        catch (Exception e)
        {
            System.out.println("RemoteObservableImpl.register: exception: "+e );
            e.printStackTrace();
        }
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

			if ( this.observer == another_pair.observer && this.of_interest.equals(another_pair.of_interest) )
				return true;
			else	
				return false;
		}
		else
			return false;
	}

	public String toString()
	{
		return "observer: "+observer+" of_interest: "+of_interest;
	}
}
