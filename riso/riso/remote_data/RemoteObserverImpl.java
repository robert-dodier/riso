package riso.remote_data;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;

/** This class provides a debugging functionality for the <tt>RemoteObserver</tt> interface.
  */
public class RemoteObserverImpl extends UnicastRemoteObject implements RemoteObserver, Serializable
{
	/** Prints out the observable, item of interest, and the argument, if any.
	  */
	public void update( RemoteObservable o, Object of_interest, Object arg ) throws RemoteException
	{
		System.err.println( "RemoteObserverImpl.update: observable: "+o+" of_interest: "+of_interest+" arg: "+arg );
	}

	public RemoteObserverImpl() throws RemoteException {}
}
