package distributions;
import java.io.*;
import java.rmi.*;

/** Interface for all conditional distribution models. 
  */
public interface ConditionalDistribution extends Remote
{
	/** Make a deep copy of this distribution object and return it. Note that we
	  * can't say "<code>... extends Cloneable</code>" and get the same
	  * effect, since (1) <code>Object.clone</code> is protected, not public, and
	  * (2) this operation needs to throw <code>RemoteException</code>,
	  * and <code>Object.clone</code> doesn't.
	  */
	public Object remote_clone() throws CloneNotSupportedException, RemoteException;

	/** This is the doodad that has the name generation algorithms to
	  * load the helper classes that knows how to generate likelihood and
	  * prediction messages, given the type of this variable and the types
	  * of incoming messages.
	  */
	public final static MessageHelperLoader message_helper_loader = new MessageHelperLoader();

	/** Return the number of dimensions of the child variable.
	  */
	public int ndimensions_child() throws RemoteException;

	/** Return the number of dimensions of the parent variables.
	  * If there is more than one parent, this is the sum of the dimensions
	  * of the parent variables.
	  */
	public int ndimensions_parent() throws RemoteException;

	/** For a given value <code>c</code> of the parents, return a distribution
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  */
	public Distribution get_density( double[] c ) throws RemoteException;

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  * @param c Values of parent variables.
	  */
	public double p( double[] x, double[] c ) throws RemoteException;

	/** Return an instance of a random variable from this distribution.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws RemoteException;

	/** Read a description of this distribution model from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  */
	public void pretty_input( StreamTokenizer st ) throws IOException, RemoteException;

	/** Write a description of this distribution model to an output stream.
	  * The description is human-readable; this is different from object
	  * serialization. 
	  * @param os Output stream to write to.
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException, RemoteException;
}
