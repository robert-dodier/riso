package riso.remote_data;
import java.rmi.*;

public interface Perishable extends Remote
{
	public boolean is_stale() throws RemoteException;
	public void set_stale() throws RemoteException;
}
