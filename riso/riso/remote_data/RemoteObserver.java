package risotto.remote_data;

import java.rmi.*;

public interface RemoteObserver extends Remote
{
	public void update( RemoteObservable o, Object of_interest, Object arg ) throws RemoteException;
}
