package riso.remote_data;

import java.rmi.*;

/** This class encapsulates the notion of a observer watching
  * some object across an RMI link.
  */
public interface RemoteObserver extends Remote
{
	/** This method is called by the object being watched after it has changed.
	  */
	public void update( RemoteObservable o, Object of_interest, Object arg ) throws RemoteException;
}
