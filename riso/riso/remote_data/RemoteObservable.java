package risotto.remote_data;
 
import java.rmi.*;

public interface RemoteObservable extends Remote
{
	/** Return a list of the items which are contained within this observable.
	  */
	public Object[] known_items() throws RemoteException;

	/** Adds an observer to the list of observers watching this observable as a whole
	  * (and not any particular object within this observable).
	  */
	public void add_observer( RemoteObserver o ) throws RemoteException;

	/** Adds an observer to the list of observers watching a particular object, 
	  * <tt>of_interest</tt>, within
	  * this observable.
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
	  * <p> The item <tt>of_interest</tt> can be a reference to this observable; that means
	  * that the observer is updated if any item within it has changed.
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
	  * <p> The item <tt>of_interest</tt> can be a reference to this observable; that means
	  * that the observer is updated if any item within it has changed.
	  *
	  * <p> If an <tt>update</tt> call fails with a <tt>RemoteException</tt>,
	  * the observer is removed from the list of observers for this observable.
	  *
	  * @param of_interest An object within this observable. This parameter must not be <tt>null</tt>.
	  */
	public void notify_observers( Object of_interest ) throws RemoteException;

	/** Notifies observers watching this observable as a whole (i.e., the ones
	  * which called <tt>add_observer</tt> with <tt>of_interest</tt> equal
	  * to <tt>this</tt>) and those watching some item within this observable.
	  * The observers watching the observable as a whole are called last.
	  *
	  * <p>If an <tt>update</tt> call fails with a <tt>RemoteException</tt>,
	  * the observer is removed from the list of observers for this observable.
	  */
	public void notify_all_observers() throws RemoteException;

	/** Tells whether the item <tt>of_interest</tt> has changed.
	  */
	public boolean has_changed( Object of_interest ) throws RemoteException;

	/** Mark the item <tt>of_interest</tt> as "changed."
	  * Future calls to <tt>has_changed</tt> will return <tt>true</tt> until
	  * <tt>clear_changed</tt> is called.
	  */
	public void set_changed( Object of_interest ) throws RemoteException;

	/** Mark the item <tt>of_interest</tt> as "not changed."
	  * Future calls to <tt>has_changed</tt> will return <tt>false</tt> until
	  * <tt>set_changed</tt> is called.
	  */
	public void clear_changed( Object of_interest ) throws RemoteException;
}

