package riso.distributions;
import java.io.*;
import java.rmi.*;
import SmarterTokenizer;

/** Interface for all conditional distribution models. 
  */
public interface ConditionalDistribution extends Remote
{
	/** Return a deep copy of this object. If this object is remote,
	  * <tt>remote_clone</tt> will create a new remote object.
	  */
	public Object remote_clone() throws CloneNotSupportedException, RemoteException;

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

	/** Parse a string containing a description of a variable. The description
	  * is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException, RemoteException;

	/** Create a description of this distribution model as a string.
	  * This is a full description, suitable for printing, containing
	  * newlines and indents.
	  *
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public String format_string( String leading_ws ) throws RemoteException;
}
