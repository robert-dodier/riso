package risotto.distributions;
import java.io.*;
import java.rmi.*;
import numerical.*;
import SmarterTokenizer;

/** This class implements a probability distribution over integers 0, 1, 2, ....
  * This is a conditional distribution; the parents are also discrete.
  */
public class ConditionalDiscrete implements ConditionalDistribution
{
	double[][] probabilities;
	int[] dimensions_child, dimensions_parents;
	int ndims_child, ndims_parents;

	/** Make a deep copy of this distribution object and return it.
	  */
	public Object remote_clone() throws CloneNotSupportedException, RemoteException
	{
		ConditionalDiscrete copy = new ConditionalDiscrete();

		copy.probabilities = Matrix.copy( probabilities );
		copy.dimensions_child = (int[]) dimensions_child.clone();
		copy.dimensions_parents = (int[]) dimensions_parents.clone();
		copy.ndims_child = ndims_child;
		copy.ndims_parents = ndims_parents;

		return copy;
	}

	/** Return the number of dimensions of the child variable.
	  */
	public int ndimensions_child() { return ndims_child; }

	/** Return the number of dimensions of the parent variables.
	  * If there is more than one parent, this is the sum of the dimensions
	  * of the parent variables.
	  */
	public int ndimensions_parent() { return ndims_parents; }

	/** For a given value <code>c</code> of the parents, return a distribution
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  */
	public Distribution get_density( double[] c ) throws RemoteException
	{
		Discrete p = new Discrete();
		p.ndims = ndims_child;
		p.dimensions = (int[]) dimensions_child.clone();

		// Compute indexing polynomial, then return conditional probabilities.
		// Probability table is stored in row-major form, so parent indices
		// come first. Child indices are 0, 0, ..., 0.

		int i, ii = 0;

		for ( i = 0; i < ndims_parents-1; i++ )
			ii = dimensions_parents[i+1] * (ii + (int) c[i]);
		ii += (int) c[ndims_parents-1];

		p.probabilities = (double[]) probabilities[ii].clone();

		return p;
	}

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  * @param c Values of parent variables.
	  */
	public double p( double[] x, double[] c )
	{
		// Compute indexing polynomial, then return table value.
		// Probability table is stored in row-major form, so parent indices
		// come first. 

		int i, ii = 0, jj = 0;

		for ( i = 0; i < ndims_parents-1; i++ )
			ii = dimensions_parents[i+1] * (ii + (int) c[i]);
		ii += (int) c[ndims_parents-1];

		for ( i = 0; i < ndims_child-1; i++ )
			jj = dimensions_child[i+1] * (jj + (int) x[i]);
		jj += (int) x[ndims_child-1];

		return probabilities[ii][jj];
	}

	/** Return an instance of a random variable from this distribution.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c )
	{
		// Compute indexing polynomial to get to base of conditional
		// probability table. 

		int i, j, ii = 0, jj = 0;

		for ( i = 0; i < ndims_parents-1; i++ )
			ii = dimensions_parents[i+1] * (ii + (int) c[i]);
		ii += (int) c[ndims_parents-1];

		double[] pp = probabilities[ii], x = new double[ndims_child];
		double s = 0, r = Math.random();

		for ( i = 0; i < pp.length; i++ )
			if ( r < (s += pp[i]) )
				break;
		
		for ( j = ndims_child-1; j >= 0; j-- )
		{
			x[j] = i % dimensions_child[j];
			i -= (int) x[j];
			i /= dimensions_child[j];
		}

		return x;
	}

	/** Parse a string containing a description of a variable. The description
	  * is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException, RemoteException
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
	public String format_string( String leading_ws ) throws RemoteException
	{
		String result = "";
		int i, j, k;

		result += this.getClass().getName()+"\n"+leading_ws+"{"+"\n";
		String more_leading_ws = "\t"+leading_ws;
		String still_more_ws = "\t"+more_leading_ws;

		if ( ndims_child != 1 )
			result += more_leading_ws+"ndimensions-child "+ndims_child+"\n";
		if ( ndims_parents != 1 )
			result += more_leading_ws+"ndimensions-parents "+ndims_parents+"\n";
		result += more_leading_ws+"dimensions-child { ";
		for ( i = 0; i < ndims_child; i++ )
			result += dimensions_child[i]+" ";
		result += "}"+"\n";
		result += more_leading_ws+"dimensions-parents { ";
		for ( i = 0; i < ndims_parents; i++ )
			result += dimensions_parents[i]+" ";
		result += "}"+"\n";

		int[] parents_block_sizes = new int[ndims_parents];
		int[] child_block_sizes = new int[ndims_child];
		parents_block_sizes[ndims_parents-1] = 1;
		child_block_sizes[ndims_child-1] = 1;

		for ( i = ndims_parents-2; i >= 0; i-- )
			parents_block_sizes[i] = parents_block_sizes[i+1]*dimensions_parents[i+1];
		for ( i = ndims_child-2; i >= 0; i-- )
			child_block_sizes[i] = child_block_sizes[i+1]*dimensions_child[i+1];

		result += more_leading_ws+"probabilities"+"\n"+more_leading_ws+"{";

		boolean following_context = false;

		for ( i = 0; i < probabilities.length; i++ )
		{
			result += "\n\n"+still_more_ws+"/* context";
			for ( k = 0; k < ndims_parents; k++ )
				result += "["+(i/parents_block_sizes[k])%dimensions_parents[k]+"]";
			result += " */"+"\n"+still_more_ws;
			following_context = true;

			for ( j = 0; j < probabilities[i].length; j++ )
			{
				if ( ndims_child > 1 && j % child_block_sizes[ndims_child-2] == 0 )
					result += "\n"+still_more_ws;

				if ( ndims_child > 2 && j % child_block_sizes[ndims_child-3] == 0 )
				{
					if ( following_context )
						following_context = false;
					else
						result += "\n"+still_more_ws;

					result += "/* probabilities";
					for ( k = 0; k < ndims_child-2; k++ )
						result += "["+(j/child_block_sizes[k])%dimensions_child[k]+"]";
					result += "[][] */"+"\n"+still_more_ws;
				}

				result += probabilities[i][j]+" ";
			}
		}

		result += "\n"+more_leading_ws+"}"+"\n"+leading_ws+"}"+"\n";
		return result;
	}


	/** Read a description of this distribution from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		// Unless we're told otherwise, assume number of parent dimensions
		// is 1, likewise with child dimensions.

		ndims_parents = ndims_child = 1;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "ConditionalDiscrete.pretty_input: input doesn't have opening bracket (found "+st+" instead)." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "ndimensions-child" ) )
				{
					st.nextToken();
					ndims_child = Format.atoi( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "ndimensions-parents" ) )
				{
					st.nextToken();
					ndims_parents = Format.atoi( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "dimensions-child" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``dimensions-child'' lacks opening bracket (found "+st.sval+" instead)." );

					dimensions_child = new int[ndims_child];
					for ( int i = 0; i < ndims_child; i++ )
					{
						st.nextToken();
						dimensions_child[i] = Format.atoi( st.sval );
					}

					st.nextToken();
					if ( st.ttype != '}' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``dimensions-child'' lacks closing bracket (found "+st.sval+" instead)." );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "dimensions-parents" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``dimensions-parents'' lacks opening bracket (found "+st.sval+" instead)." );

					dimensions_parents = new int[ndims_parents];
					for ( int i = 0; i < ndims_parents; i++ )
					{
						st.nextToken();
						dimensions_parents[i] = Format.atoi( st.sval );
					}

					st.nextToken();
					if ( st.ttype != '}' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``dimensions-parents'' lacks closing bracket (found "+st.sval+" instead)." );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "probabilities" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``probabilities'' lacks opening bracket (found "+st.sval+" instead)." );

					int i, j, parents_size = 1, child_size = 1;

					for ( i = 0; i < ndims_child; i++ )
						child_size *= dimensions_child[i];
					for ( i = 0; i < ndims_parents; i++ )
						parents_size *= dimensions_parents[i];

					probabilities = new double[parents_size][child_size];

					for ( i = 0; i < parents_size; i++ )
						for ( j = 0; j < child_size; j++ )
						{
							st.nextToken();
							probabilities[i][j] = Format.atof( st.sval );
						}

					st.nextToken();
					if ( st.ttype != '}' ) throw new IOException( "ConditionalDiscrete.pretty_input: ``probabilities'' lacks closing bracket (found "+st.sval+" instead)." );
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
			throw new IOException( "ConditionalDiscrete.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "ConditionalDiscrete.pretty_input: no closing bracket on input." );
	}

	/** Write a description of this distribution to an output stream.
	  * The description is human-readable; this is different from object
	  * serialization. 
	  * @param os Output stream to write to.
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  * @throws IOException If the attempt to write the model fails.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	/** Use data to modify the parameters of the distribution. Classes which
	  * implement this method will typically use maximum likelihood or
	  * a similar approach to fit the parameters to the data.
	  * @param x The data. Each row has a number of components equal to the
	  *   number of dimensions of the model, and the number of rows is the
	  *   number of data.
	  * @param responsibility Each component of this vector 
	  *   <code>responsibility[i]</code> is a scalar telling the probability
	  *   that this distribution produced the corresponding datum <code>x[i]</code>.
	  *   This is mostly intended for fitting mixture distributions, although
	  *   other uses can be imagined.
	  * @param niter_max Maximum number of iterations of the update algorithm,
	  *   if applicable.
	  * @param stopping_criterion A number which describes when to stop the
	  *   update algorithm, if applicable.
	  * @return Some indication of goodness-of-fit, such as MSE or negative
	  *   log-likelihood.
	  * @throws Exception If the update algorithm fails; if no exception is
	  *   thrown, the algorithm succeeded.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "ConditionalDiscrete.update: not implemented." );
	}

}
