package riso.distributions;
import java.io.*;
import riso.regression.*;
import numerical.*;
import SmarterTokenizer;

/** An instance of this class represents a classification model
  * based on a neural network. The neural network is set up to squash
  * its outputs so that the outputs are in the range (0,1).
  */
public class SquashingNetworkClassifier extends Classifier
{
	SquashingNetwork squashing_network;
	boolean exclusive = true;

	public Object remote_clone() throws CloneNotSupportedException
	{
		SquashingNetworkClassifier copy;
		try { copy = (SquashingNetworkClassifier) getClass().newInstance(); }
		catch (Exception e) { throw new CloneNotSupportedException( "failed: "+e ); }
		copy.squashing_network = this.squashing_network;
		return copy;
	}

	/** Return the number of dimensions of the child variable.
	  */
	public int ndimensions_child() { return 1; }

	/** Return the number of dimensions of the parent variables.
	  * If there is more than one parent, this is the sum of the dimensions
	  * of the parent variables.
	  */
	public int ndimensions_parent() { return squashing_network.ndimensions_in(); }

	/** For a given value <code>c</code> of the parents, return a distribution
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  */
	public Distribution get_density( double[] c ) throws Exception
	{
		int[] dimensions = new int[1];
		dimensions[0] = squashing_network.ndimensions_out();
		Discrete dd = new Discrete( dimensions );
		double[] p = squashing_network.F(c);
		int[] ii = new int[ p.length ];
		
		for ( int i = 0; i < p.length; i++ )
		{
			ii[0] = i;
			dd.assign_p( ii, p[i] );
		}

System.err.println( "SquashingNetworkClassifier.get_density: dd: "+dd.format_string("----") );
		return dd;
	}

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  * @param c Values of parent variables.
	  */
	public double p( double[] x, double[] c ) throws Exception
	{
		double[] p = squashing_network.F(c);
		return p[ (int)x[0] ];
	}

	/** Return an instance of a random variable from this distribution.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws Exception
	{
		return get_density(c).random();
	}

	/** Parse a string containing a description of a variable. The description
	  * is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Create a description of this distribution model as a string.
	  * This is a full description, suitable for printing, containing
	  * newlines and indents.
	  *
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		int i, j;
		String result = "", more_leading_ws = leading_ws+"\t", still_more_ws = leading_ws+"\t\t";

		result += this.getClass()+" "+squashing_network.format_string(more_leading_ws);
		return result;
	}

	/** Read in a <tt>SquashingNetworkClassifier</tt> from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param st Stream tokenizer to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		st.nextToken();
		try { squashing_network = (SquashingNetwork) java.rmi.server.RMIClassLoader.loadClass(st.sval).newInstance(); }
		catch (Exception e) { throw new IOException( "SquashingNetworkClassifier.pretty_input: "+e ); }
		st.nextBlock();
		squashing_network.parse_string(st.sval);
		squashing_network.flags |= SquashingNetwork.SOFTMAX_OUTPUT;
	}

	public int ncategories() { return squashing_network.ndimensions_out(); }
}
