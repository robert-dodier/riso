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

	public void addRemoteObserver( RemoteObserver o, Object of_interest )
	{
		RemoteObserverPair p = new RemoteObserverPair( o, of_interest );
		if ( ! observer_table.contains( p ) )
		{
			System.err.println( "RemoteObservableImpl.addRemoteObserver: "+p );
			observer_table.addElement( p );
		}
		else
		{
			System.err.println( "RemoteObservableImpl.addRemoteObserver: already in observer_table: "+p );
		}
	}

	public void deleteRemoteObserver( RemoteObserver o, Object of_interest )
	{
		RemoteObserverPair p = new RemoteObserverPair( o, of_interest );
		if ( observer_table.removeElement(p) )
			System.err.println( "RemoteObservableImpl.deleteRemoteObserver: deleted: "+p );
		else
			System.err.println( "RemoteObservableImpl.deleteRemoteObserver: not found: "+p );
	}

	public void deleteRemoteObserver( RemoteObserver o )
	{
		int i, n = observer_table.size();
		RemoteObserverPair p;
		for ( i = n-1; i >= 0; i-- )
			if ( (p = (RemoteObserverPair)observer_table.elementAt(i)).observer == o )
			{
				System.err.println( "RemoteObservableImpl.deleteRemoteObserver: delete: "+p );
				observer_table.removeElementAt(i);
			}
	}

	public void deleteRemoteObservers()
	{
		System.err.println( "RemoteObservableImpl.deleteRemoteObservers: delete all observers" );
		observer_table.removeAllElements();
	}

	public void notifyRemoteObservers( Object of_interest, Object arg )
	{
		int i, nobs = observer_table.size();

		if ( hasChanged( of_interest ) )
		{
			for ( i = 0; i < nobs; i++ )
			{
				RemoteObserverPair p = (RemoteObserverPair) observer_table.elementAt(i);
				if ( of_interest.equals( p.of_interest ) )
				{
					System.err.println( "RemoteObservableImpl.notifyRemoteObservers: notify: "+p );
					try 
					{
						p.observer.update( this, of_interest, arg );
					}
					catch (RemoteException e)
					{
						System.err.println( "RemoteObservableImpl.notifyRemoteObservers: exception: "+e+" for: "+p );
					}
				}
			}

			clearChanged( of_interest );
		}
	}

	public void notifyRemoteObservers( Object of_interest ) throws RemoteException
	{
		int i, nobs = observer_table.size();

		if ( hasChanged( of_interest ) )
		{
			for ( i = 0; i < nobs; i++ )
			{
				RemoteObserverPair p = (RemoteObserverPair) observer_table.elementAt(i);
				if ( of_interest.equals( p.of_interest ) )
				{
					System.err.println( "RemoteObservableImpl.notifyRemoteObservers: notify: "+p );
					try 
					{
						p.observer.update( this, of_interest, null );
					}
					catch (RemoteException e)
					{
						System.err.println( "RemoteObservableImpl.notifyRemoteObservers: exception: "+e+" for: "+p );
					}
				}
			}

			clearChanged( of_interest );
		}
	}

	public void notifyRemoteObservers()
	{
		int i, nobs = observer_table.size();

		for ( i = 0; i < nobs; i++ )
		{
			RemoteObserverPair p = (RemoteObserverPair) observer_table.elementAt(i);
			Boolean b = (Boolean) interests_table.get( p.of_interest );
			if ( b == null )
			{
				System.err.println( "RemoteObservableImpl.notifyRemoteObservers: don't know about "+p.of_interest );
				continue;
			}

			if ( b.booleanValue() )	// i.e., of_interest has changed
			{
				System.err.println( "RemoteObservableImpl.notifyRemoteObservers: notify: "+p );

				try 
				{
					p.observer.update( this, p.of_interest, null );
				}
				catch (RemoteException e)
				{
					System.err.println( "RemoteObservableImpl.notifyRemoteObservers: exception: "+e+" for: "+p );
				}
			}

			clearChanged( p.of_interest );
		}
	}

	public boolean hasChanged( Object of_interest )
	{
		Boolean b = (Boolean) interests_table.get( of_interest );
		if ( b == null )
		{
			System.err.println( "RemoteObservableImpl.hasChanged: don't know about "+of_interest );
			return false;
		}
		else
			return b.booleanValue();
	}

	public void setChanged( Object of_interest ) { interests_table.put( of_interest, Boolean.TRUE ); }

	public void clearChanged( Object of_interest ) { interests_table.put( of_interest, Boolean.FALSE ); }

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

	public RemoteObservableImpl() throws RemoteException {}
}


class RemoteObserverPair
{
	RemoteObserver observer;
	Object of_interest;

	RemoteObserverPair( RemoteObserver o, Object oi )
	{
		observer = o;
		of_interest = oi;
	}

	public boolean equals( Object another )
	{
		if ( another instanceof RemoteObserverPair )
		{
			RemoteObserverPair another_pair = (RemoteObserverPair) another;

			// These two pairs are equal if they refer to the same RemoteObserver object,
			// and their two objects of_interest are equal (not necessarily same reference).
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
