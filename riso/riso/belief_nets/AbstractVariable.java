package belief_nets;
import java.rmi.*;

public interface AbstractVariable extends Remote
{
	/** Retrieve the name of this variable.
	  */
	public String get_name() throws RemoteException;
}
