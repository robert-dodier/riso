package belief_nets;

import java.io.*;
import java.rmi.*;
import distributions.*;

public interface AbstractBeliefNetwork extends Remote
{
	/** Retrieve the name of this belief network.
	  */
	public String get_name() throws RemoteException;

	/** Retrieve a list of references to the variables contained in this
	  * belief network.
	  */
	public AbstractVariable[] get_variables() throws RemoteException;

	/** Return a reference to the variable of the given name. Returns
	  * <tt>null</tt> if the variable isn't in this belief network.
	  */
	public AbstractVariable name_lookup( String name ) throws RemoteException;

	/** Mark the variable <tt>x</tt> as not observed.
	  */
	public void clear_evidence( AbstractVariable x ) throws RemoteException;

	/** Mark all variables as not observed.
	  */
	public void clear_all_evidence() throws RemoteException;

	/** Assign the value <tt>a</tt> to the variable <tt>x</tt>.
	  * A call to <tt>posterior(x)</tt> will then return a delta function
	  * centered on <tt>a</tt>. Either a continuous or discrete value may
	  * be represented by <tt>a</tt>.
	  */
	public void assign_evidence( AbstractVariable x, double a ) throws RemoteException;

	/** Compute the posterior distribution for <tt>x</tt> given any
	  * evidence set in the belief network.
	  */
	public void compute_posterior( AbstractVariable x ) throws RemoteException;

	/** Compute the posterior distribution for all variables given any
	  * evidence set in the belief network.
	  */
	public void compute_all_posteriors() throws RemoteException;

	/** Compute the mutual information between variables <tt>x</tt> and
	  * <tt>e</tt>, where <tt>e</tt> is an evidence node, given any other
	  * evidence in the belief network. Note that a more general mutual
	  * information computation would not require one node to be evidence;
	  * but that's more difficult and we're not doing that yet.
	  * @throws IllegalArgumentException If <tt>e</tt> is not an evidence node.
	  */
	public double compute_information( AbstractVariable x, AbstractVariable e ) throws RemoteException, IllegalArgumentException;

	/** Retrieve a reference to the marginal posterior distribution for
	  * <tt>x</tt> given the current evidence <tt>e</tt>, <tt>p(x|e)</tt>.
	  * If the posterior has not yet been computed, it is computed.
	  */
	public Distribution posterior( AbstractVariable x ) throws RemoteException;

	/** Retrieve a reference to the marginal posterior distribution for
	  * <tt>x[0],x[1],x[2],...</tt> given the current evidence <tt>e</tt>,
	  * p(x[0],x[1],x[2],...|e)</tt>. If the posterior has not yet been
	  * computed, it is computed.
	  */
	public Distribution posterior( AbstractVariable[] x ) throws RemoteException;

	/** Read a description of this belief network from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  * @throws IOException If the attempt to read the belief network fails.
	  */
	public void pretty_input( StreamTokenizer st ) throws IOException, RemoteException;

	/** Write a description of this belief network to an output stream.
	  * The description is human-readable; this is different from object
	  * serialization. 
	  * @param os Output stream to write to.
	  * @throws IOException If the attempt to write the belief network fails.
	  */
	public void pretty_output( OutputStream os ) throws IOException, RemoteException;
}
