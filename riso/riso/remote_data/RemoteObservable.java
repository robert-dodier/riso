package riso.remote_data;
 
import java.rmi.*;

public interface RemoteObservable extends Remote
{
	/** Adds an observer to the list of observers watching a particular object, 
	  * <tt>of_interest</tt>, within this observable.
	  */
	public void add_observer( RemoteObserver o, Object of_interest ) throws RemoteException;

	/** Removes an observer from the list of observers watching a particular 
	  * item of interest.
	  */
	public void delete_observer( RemoteObserver o, Object of_interest ) throws RemoteException;

	/** Removes an observer from all the lists of observers watching items within
	  * this observable.
	  */
	public void delete_observer( RemoteObserver o ) throws RemoteException;

	/** Removes all observers.
	  */
	public void delete_all_observers() throws RemoteException;

	/** Notifies any observers watching the object <tt>of_interest</tt> within this
	  * observable. 
	  *
	  * <p> If an <tt>update</tt> call fails with a <tt>RemoteException</tt>,
	  * the observer is removed from the list of observers for this observable.
	  *
	  * @param of_interest An object within this observable. This parameter must not be <tt>null</tt>.
	  * @param arg The argument sent to the observer in the <tt>update</tt> call.
	  */
	public void notify_observers( Object of_interest, Object arg ) throws RemoteException;

	/** Notifies any observers watching the object <tt>of_interest</tt> within this
	  * observable. The argument sent to the observer in the <tt>update</tt> call is <tt>null</tt>.
	  *
	  * <p> If an <tt>update</tt> call fails with a <tt>RemoteException</tt>,
	  * the observer is removed from the list of observers for this observable.
	  *
	  * @param of_interest An object within this observable. This parameter must not be <tt>null</tt>.
	  */
	public void notify_observers( Object of_interest ) throws RemoteException;

	/** Notifies all observers watching something within this observable.
	  * <p>If an <tt>update</tt> call fails with a <tt>RemoteException</tt>,
	  * the observer is removed from the list of observers for this observable.
	  */
	public void notify_all_observers() throws RemoteException;
}

