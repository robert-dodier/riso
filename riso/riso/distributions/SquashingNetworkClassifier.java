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
	public int ndimensions_child() { return squashing_network.ndimensions_out(); }

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
		int[] dimensions = new int[ squashing_network.ndimensions_out() ];
		for ( int i = 0; i < dimensions.length; i++ ) dimensions[i] = 2;
		Discrete dd = new Discrete( dimensions );
		double[] p = squashing_network.F(c);
		int[] ii = new int[ p.length ];

		if ( exclusive )
		{
			for ( int i = 0; i < p.length; i++ )
			{
				ii[i] = 1;
				dd.assign_p( ii, p[i] );
				ii[i] = 0;
			}
		}
		else
		{
			if ( p.length > 30 ) throw new Exception("SquashingNetworkClassifier.get_density: #dimensions "+p.length+" is too many." );
			int n = 1 << p.length;
			for ( int i = 0; i < n; i++ )
			{
				int mask = 1;
				double q = 1;

				for ( int j = 0; j < p.length; j++ )
				{
					ii[j] = ((i & mask) == 0 ? 0 : 1);
					q *= (ii[j] == 0 ? (1-p[j]) : p[j]);
					mask <<= 1;
				}

				dd.assign_p( ii, q );
			}
		}

		dd.normalize_p();
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

		if ( exclusive )
		{
			int n_ones = 0, i_one = 0;
			for ( int i = 0; i < x.length; i++ )
				if ( x[i] == 1 )
				{
					i_one = i;
					++n_ones;
				}

			if ( n_ones == 1 )
				return p[ i_one ];
			else
				return 0;
		}
		else
		{
			double q = 1;
			for ( int i = 0; i < x.length; i++ )
				q *= (x[i] == 0 ? (1-p[i]) : p[i]);
			return q;
		}
	}

	/** Return an instance of a random variable from this distribution.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws Exception
	{
System.err.println( "SquashingNetworkClassifier.random: VERY SLOW IMPLEMENTATION!!!" );
		return get_density( c ).random();
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

		result += this.getClass()+"\n"+leading_ws+"{"+"\n";
		
		result += more_leading_ws+"exclusive "+exclusive+"\n";
		result += more_leading_ws+"model "+squashing_network.format_string(more_leading_ws);
		result += leading_ws+"}\n";
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
		boolean found_closing_bracket = false;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "SquashingNetworkClassifier.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "exclusive" ) )
				{
					st.nextToken();
					exclusive = "true".equals(st.sval);
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "model" ) )
				{
					st.nextToken();
					try { squashing_network = (SquashingNetwork) java.rmi.server.RMIClassLoader.loadClass(st.sval).newInstance(); }
					catch (Exception e) { throw new IOException( "SquashingNetworkClassifier.pretty_input: "+e ); }
					st.nextBlock();
					squashing_network.parse_string(st.sval);
					squashing_network.flags |= SquashingNetwork.SIGMOIDAL_OUTPUT;
				}
				else if ( st.ttype == '}' )
				{
					found_closing_bracket = true;
					break;
				}
			}
		}
		catch (IOException e)
		{
			throw new IOException( "SquashingNetworkClassifier.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "SquashingNetworkClassifier.pretty_input: no closing bracket on input." );
	}
}
