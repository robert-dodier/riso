package risotto.remote_data;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;

public class RemoteObserverImpl extends UnicastRemoteObject implements RemoteObserver, Serializable
{
	public void update( RemoteObservable o, Object of_interest, Object arg ) throws RemoteException
	{
		System.out.println( "RemoteObserverImpl.update: observable: "+o+" of_interest: "+of_interest+" arg: "+arg );
	}

	public RemoteObserverImpl() throws RemoteException {}
}
