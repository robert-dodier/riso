package risotto.remote_data;
 
import java.rmi.*;

public interface RemoteObservable extends Remote
{
	public void addRemoteObserver( RemoteObserver o, Object of_interest ) throws RemoteException;
	public void deleteRemoteObserver( RemoteObserver o, Object of_interest ) throws RemoteException;
	public void deleteRemoteObserver( RemoteObserver o ) throws RemoteException;
	public void deleteRemoteObservers() throws RemoteException;
	public void notifyRemoteObservers( Object of_interest, Object arg ) throws RemoteException;
	public void notifyRemoteObservers( Object of_interest ) throws RemoteException;
	public void notifyRemoteObservers() throws RemoteException;
	public boolean hasChanged( Object of_interest ) throws RemoteException;
	public void setChanged( Object of_interest ) throws RemoteException;
	public void clearChanged( Object of_interest ) throws RemoteException;
}

