package riso.belief_nets;
import java.rmi.*;

/** This interface defines methods, which are remotely visible, for 
  * loading and looking up belief networks. Note that networks loaded
  * by these methods are running on the machine containing the implementation
  * of this interface. This provides a means of starting up a belief
  * network on remote machine.
  */
public interface AbstractBeliefNetworkContext extends Remote
{
	/** This method searches the path list to locate the belief network
	  * file. The filename must have the form "<tt>something.riso</tt>".
	  * The name of the belief network in this case is "<tt>something</tt>".
	  * This method does not rebind the belief network in the RMI registry;
	  * call <tt>AbstractBeliefNetworkContext.rebind</tt> for that.
	  *
	  * @param bn_name Name of the belief network; does not include
	  *   "<tt>.riso</tt>".
	  * @return A reference to the belief network -- may be a remote reference.
	  * @throws RemoteException If the network cannot be located, or cannot
	  *   be read in successfully.
	  * @see AbstractBeliefNetwork.pretty_input
	  */
	public AbstractBeliefNetwork load_network( String bn_name ) throws RemoteException;

	/** This method is similar to <tt>load_network</tt>, except that the
	  * belief network description is supplied as a string instead of a file.
	  * If the string is successfully parsed, a new belief network is 
	  * created and a reference is placed in the reference table. The name
	  * of the belief network is found in the description.
	  * This method does not rebind the belief network in the RMI registry;
	  * call <tt>AbstractBeliefNetworkContext.rebind</tt> for that.
	  *
	  * @param description Description of a belief network; the format is
	  *   the same as for loading a belief network from a file.
	  * @return A reference to the belief network -- may be a remote reference.
	  * @throws RemoteException If the description cannot be parsed.
	  * @see AbstractBeliefNetwork.pretty_input
	  */
	public AbstractBeliefNetwork parse_network( String description ) throws RemoteException;

	/** Given the name of a belief network, this method returns a reference
	  * to that belief network. If the belief network is not already loaded,
	  * it is loaded from the local filesystem.
	  * This method does not rebind the belief network in the RMI registry;
	  * call <tt>AbstractBeliefNetworkContext.rebind</tt> for that.
	  */
	public AbstractBeliefNetwork get_reference( String bn_name ) throws RemoteException;

	/** Rebinds the given reference in the RMI registry.
	  * The URL is based on the full name of the argument <tt>bn</tt>,
	  * which has the form <tt>host.locale.domain/server-name</tt>, or
	  * <tt>host.local.domain:port/server-name</tt> if the RMI registry
	  * port is different from the default.
	  */
	public void rebind( AbstractBeliefNetwork bn ) throws RemoteException;
}

