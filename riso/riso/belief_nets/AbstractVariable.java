package risotto.belief_nets;

import java.io.*;
import java.rmi.*;
import java.util.*;
import risotto.distributions.*;

public interface AbstractVariable extends Remote
{
	/** Retrieve just the name of this variable alone; doesn't
	  * include the name of the belief network.
	  */
	public String get_name() throws RemoteException;

	/** Retrieve the name of this variable, including the name of the
	  * belief network which contains it.
	  */
	public String get_fullname() throws RemoteException;

	/** Retrieve a list of the names of the parent variables of this variable.
	  */
	public Enumeration get_parents_names() throws RemoteException;

	/** Retrieve a list of references to the parent variables of this variable.
	  */
	public Enumeration get_parents() throws RemoteException;

	/** Retrieve a list of the names of the child variables of this variable.
	  */
	public Enumeration get_childrens_names() throws RemoteException;

	/** Retrieve a list of references to the child variables of this variable.
	  */
	public Enumeration get_children() throws RemoteException;

	/** Retrieve a reference to the conditional distribution of this variable given its parents.
	  * The reference is null if no distribution has yet been specified for this variable.
	  */
	public ConditionalDistribution get_distribution() throws RemoteException;

	/** Retrieve a reference to the posterior distribution of this variable given 
	  * any evidence variables. The reference is null if the posterior has not been
	  * computed given the current evidence.
	  */
	public Distribution get_posterior() throws RemoteException;

	/** Tell this variable to add another to its list of children.
	  * Since the other variable may be remote, we need a method to
	  * do this, since we can't access the children list directly.
	  */
	public void add_child( String child_name, AbstractVariable x ) throws RemoteException;

	/** Read a description of this variable from an input stream
	  * (as represented by a <tt>StreamTokenizer</tt>.)
	  */
	public void pretty_input( StreamTokenizer st ) throws IOException, RemoteException;

	/** Write a description of this variable to an output stream.
	  * This is slightly asymmetric w.r.t. to <tt>pretty_input</tt>:
	  * this function writes the class name onto the output stream before
	  * writing the variable name and any descriptive data; whereas
	  * <tt>pretty_input</tt> expects that the class name has been stripped
	  * from the input stream and the variable name is the first token.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException, RemoteException;
}
