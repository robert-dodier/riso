package riso.distributions;
import java.rmi.*;
import java.rmi.server.*;
import riso.belief_nets.*;

/** Abstract base class for conditional distributions.
  * This class implements only a few methods; most of the methods from the
  * <tt>ConditionalDistribution</tt> interface are not implemented,
  * and so must be provided by subclasses.
  *
  * <p> This classs is helpful in part because message-passing algorithms can be
  * formulated as generic for all conditional distributions -- handlers
  * are named only by classes, not by interfaces.
  */
public abstract class AbstractConditionalDistribution extends UnicastRemoteObject implements ConditionalDistribution
{
	/** Default constructor for this class just calls super().
	  * It's declared here to show that it can throw a remote exception.
	  */
	public AbstractConditionalDistribution() throws RemoteException { super(); }

	/** Cache a reference to the variable with which this conditional distribution
	  * is associated. Subclasses can ignore this method if they don't need to know.
	  */
	public void set_variable( Variable x ) throws RemoteException {}
}
