package belief_nets;
import java.rmi.*;

public interface AbstractVariable extends Remote
{
	/** Retrieve the name of this variable.
	  */
	public String get_name() throws RemoteException;

	/** Retrieve a list of references to the parent variables of this variable.
	  */
	public AbstractVariable[] get_parents() throws RemoteException;

	/** Retrieve a list of references to the child variables of this variable.
	  */
	public AbstractVariable[] get_children() throws RemoteException;

}
