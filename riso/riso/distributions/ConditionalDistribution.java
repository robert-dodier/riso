package riso.distributions;
import java.io.*;
import java.rmi.*;
import riso.belief_nets.*;
import SmarterTokenizer;

/** Interface for all conditional distribution models. 
  */
public interface ConditionalDistribution
{
	/** Return a deep copy of this object. If this object is remote,
	  * <tt>clone</tt> will create a new remote object.
	  */
	public Object clone() throws CloneNotSupportedException;

	/** Return the number of dimensions of the child variable.
	  */
	public int ndimensions_child();

	/** Return the number of dimensions of the parent variables.
	  * If there is more than one parent, this is the sum of the dimensions
	  * of the parent variables.
	  */
	public int ndimensions_parent();

	/** For a given value <code>c</code> of the parents, return a distributn
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  * @param c Values of parent variables.
	  */
	public Distribution get_density( double[] c ) throws Exception;

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  */
	public double p( double[] x, double[] c ) throws Exception;

	/** Return an instance of a random variable from this distribution.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws Exception;

	/** Parse a string containing a description of a variable. The description
	  * is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException;

	/** Create a description of this distribution model as a string.
	  * This is a full description, suitable for printing, containing
	  * newlines and indents.
	  *
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public String format_string( String leading_ws ) throws IOException;

	/** Cache a reference to the variable with which this conditional distribution
	  * is associated.
	  */
	public void set_variable( Variable x );
}
