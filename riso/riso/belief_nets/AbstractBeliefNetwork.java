package riso.belief_nets;

import java.io.*;
import java.rmi.*;
import java.util.*;
import riso.distributions.*;

public interface AbstractBeliefNetwork extends Remote
{
	/** Retrieve the name of this belief network.
	  */
	public String get_name() throws RemoteException;

	/** Retrieve the full name of this belief network.
	  * This includes the name of the registry host from which this
	  * belief network may be retrieved, and the registry port number,
	  * if different from the default port number.
	  */
	public String get_fullname() throws RemoteException;

	/** Retrieve a list of references to the variables contained in this
	  * belief network.
	  */
	public AbstractVariable[] get_variables() throws RemoteException;

	/** Return a simplified representation of the object which implements
	  * this interface. This is especially useful for debugging.
	  */
	public String remoteToString() throws RemoteException;

	/** Return a reference to the variable of the given name. Returns
	  * <tt>null</tt> if the variable isn't in this belief network.
	  */
	public AbstractVariable name_lookup( String name ) throws RemoteException;

	/** Clear the posterior of <tt>some_variable</tt> but do not recompute it. This method also
	  * clears the pi and lambda for this variable. Notify remote observers
	  * that the posterior for this variable is no longer know (if it ever was).
	  */
	public void clear_posterior( AbstractVariable some_variable ) throws RemoteException;

	/** Assign the value <tt>a</tt> to the variable <tt>x</tt>.
	  * A call to <tt>get_posterior(x)</tt> will then return a delta function
	  * centered on <tt>a</tt>. Either a continuous or discrete value may
	  * be represented by <tt>a</tt>.
	  */
	public void assign_evidence( AbstractVariable x, double a ) throws RemoteException;

	public Distribution compute_lambda_message( AbstractVariable parent, AbstractVariable child ) throws RemoteException;

	public Distribution compute_pi_message( AbstractVariable parent, AbstractVariable child ) throws RemoteException;

	/** Compute the mutual information between variables <tt>x</tt> and
	  * <tt>e</tt>, where <tt>e</tt> is an evidence node, given any other
	  * evidence in the belief network. Note that a more general mutual
	  * information computation would not require one node to be evidence;
	  * but that's more difficult and we're not doing that yet.
	  *
	  * @throws IllegalArgumentException If <tt>e</tt> is not an evidence node.
	  */
	public double compute_information( AbstractVariable x, AbstractVariable e ) throws RemoteException, IllegalArgumentException;

	/** Retrieve a reference to the marginal posterior distribution for
	  * <tt>x</tt> given the current evidence <tt>e</tt>, <tt>p(x|e)</tt>.
	  * If the posterior has not yet been computed, it is computed.
	  */
	public Distribution get_posterior( AbstractVariable x ) throws RemoteException;

	/** Retrieve a reference to the marginal posterior distribution for
	  * <tt>x[0],x[1],x[2],...</tt> given the current evidence <tt>e</tt>,
	  * p(x[0],x[1],x[2],...|e)</tt>. If the posterior has not yet been
	  * computed, it is computed.
	  */
	public Distribution get_posterior( AbstractVariable[] x ) throws RemoteException;

	/** Write a description of this belief network to a string,
	  * using the format required by the "dot" program. All the probabilistic
	  * information is thrown away; only the names of the variables and
	  * the parent-to-child relations are kept.
	  * See <a href="http://www.research.att.com/sw/tools/graphviz/">
	  * the Graphviz homepage</a> for information about "dot" and other
	  * graph visualization software.
	  *
	  * @return A string containing the belief network in "dot" format. 
	  * @throws RemoteException If this belief network is remote and something
	  *   strange happens.
	  */
	public String dot_format() throws RemoteException;

	/** Parse a string containing a description of a belief network. The description
	  * is contained within curly braces, which are included in the string.
	  * The content in curly braces is preceded by the name of the belief network.
	  */
	public void parse_string( String description ) throws IOException, RemoteException;

	/** Create a description of this belief network as a string. 
	  * This is a full description, suitable for printing, containing
	  * newlines and indents.
	  */
	public String format_string() throws RemoteException;
}
