package riso.distributions;
import java.io.*;
import java.rmi.*;
import numerical.*;
import SmarterTokenizer;

/** This class implements a probability distribution over integers 0, 1, 2, ....
  * This is an unconditional distribution.
  */
public class Discrete extends AbstractDistribution
{
	public double[] probabilities;
	public int[] dimensions;
	public int ndims;

	/** Create a new object, and don't fill in any of the member data.
	  */
	public Discrete() {}

	/** Create a new object, but don't fill in the probabilities.
	  */
	public Discrete( int[] dimensions )
	{
		ndims = dimensions.length;
		this.dimensions = (int[]) dimensions.clone();
		int i, size = 1;
		for ( i = 0; i < ndims; i++ )
			size *= dimensions[i];
		probabilities = new double[size];
	}

	/** Make a deep copy of this discrete distribution object and return it.
	  */
	public Object remote_clone() throws CloneNotSupportedException
	{
		Discrete copy;
		copy = new Discrete();

		copy.probabilities = (double[]) probabilities.clone();
		copy.dimensions = (int[]) dimensions.clone();
		copy.ndims = ndims;

		return copy;
	}

	/** Return the number of dimensions in which this distribution lives.
	  */
	public int ndimensions() { return ndims; }

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  */
	public double p( double[] x ) throws Exception
	{
		// Compute indexing polynomial, then return table value.

		int i, ii = 0;

		for ( i = 0; i < ndims-1; i++ )
			ii = dimensions[i+1] * (ii + (int) x[i]);
		ii += (int) x[ndims-1];

		return probabilities[ii];
	}

	/** Return an instance of a random variable from this distribution.
	  */
	public double[] random() throws Exception
	{
		double[] x = new double[ndims];
		double r = Math.random(), s = 0;
		int i, j;

		for ( i = 0; i < probabilities.length-1; i++ )
			if ( r < (s += probabilities[i]) )
				break;
		
		for ( j = ndims-1; j >= 0; j-- )
		{
			x[j] = i % dimensions[j];
			i -= (int) x[j];
			i /= dimensions[j];
		}

		return x;
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
		String result = "";
		int i, j;

		result += this.getClass().getName()+"\n"+leading_ws+"{"+"\n";
		String more_leading_ws = "\t"+leading_ws;
		String still_more_ws = "\t"+more_leading_ws;

		if ( ndims != 1 )
			result += more_leading_ws+"ndimensions "+ndims+"\n";
		result += more_leading_ws+"dimensions { ";
		for ( i = 0; i < ndims; i++ )
			result += dimensions[i]+" ";
		result += "}"+"\n";

		int[] block_sizes = new int[ndims];
		block_sizes[ndims-1] = 1;
		for ( i = ndims-2; i >= 0; i-- )
			block_sizes[i] = block_sizes[i+1]*dimensions[i+1];

		result += more_leading_ws+"probabilities"+"\n"+more_leading_ws+"{";
		for ( i = 0; i < probabilities.length; i++ )
		{
			if ( ndims > 2 && i % block_sizes[ndims-3] == 0 )
			{
				result += "\n\n"+still_more_ws+"/* probabilities";
				for ( j = 0; j < ndims-2; j++ )
					result += "["+(i/block_sizes[j])%dimensions[j]+"]";
				result += "[][] */"+"\n"+still_more_ws;
			}
			else if ( ndims > 1 && i % block_sizes[ndims-2] == 0 )
				result += "\n"+still_more_ws;
			else if ( ndims == 1 && i == 0 )
				result += "\n"+still_more_ws;

			result += probabilities[i]+" ";
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

		// Assume number of dimensions is 1, unless told otherwise.
		ndims = 1;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "Discrete.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "ndimensions" ) )
				{
					st.nextToken();
					ndims = Format.atoi( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "dimensions" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' ) throw new IOException( "Discrete.pretty_input: ``dimensions'' lacks opening bracket." );

					dimensions = new int[ndims];
					for ( int i = 0; i < ndims; i++ )
					{
						st.nextToken();
						dimensions[i] = Format.atoi( st.sval );
					}

					st.nextToken();
					if ( st.ttype != '}' ) throw new IOException( "Discrete.pretty_input: ``dimensions'' lacks closing bracket." );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "probabilities" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' ) throw new IOException( "Discrete.pretty_input: ``probabilities'' lacks opening bracket." );

					int i, size = 1;
					for ( i = 0; i < ndims; i++ )
						size *= dimensions[i];
					probabilities = new double[size];

					for ( i = 0; i < size; i++ )
					{
						st.nextToken();
						probabilities[i] = Format.atof( st.sval );
					}

					st.nextToken();
					if ( st.ttype != '}' ) throw new IOException( "Discrete.pretty_input: ``probabilities'' lacks closing bracket." );
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
			throw new IOException( "Discrete.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "Discrete.pretty_input: no closing bracket on input." );
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
		throw new Exception( "Discrete.update: not implemented." );
	}

	/** Returns the expected value of this distribution.
	  */
	public double expected_value() throws Exception
	{
		if ( ndims > 1 ) throw new IllegalArgumentException( "Discrete.expected_value: not meaningful for #dimensions == "+ndims );

		double sum = 0;
		for ( int i = 0; i < probabilities.length; i++ )
			sum += i*probabilities[i];
		return sum;
	}

	/** Returns the square root of the variance of this distribution.
	  */
	public double sqrt_variance() throws Exception
	{
		if ( ndims > 1 ) throw new IllegalArgumentException( "Discrete.sqrt_variance: not meaningful for #dimensions == "+ndims );

		double sum = 0, sum2 = 0;
		for ( int i = 0; i < probabilities.length; i++ )
		{
			sum += i*probabilities[i];
			sum2 += i*i*probabilities[i];
		}

		double var = sum2 - sum*sum;
		return Math.sqrt(var);
	}

	/** Returns <tt>{0, n-1}</tt> where <tt>n</tt> is the number of elements
	  * in the support of this distribution. 
	  * @param epsilon This argument is ignored.
	  * @throws IllegalArgumentException If the number of dimensions is more than 1.
	  */
	public double[] effective_support( double epsilon ) throws IllegalArgumentException
	{
		if ( ndims > 1 )
			throw new IllegalArgumentException( "Gaussian.effective_support: can't handle "+ndims+" dimensions." );
		
		double[] support = new double[2];

		support[0] = 0;
		support[1] = dimensions[0]-1;

		return support;
	}

	public double assign_p( int[] ix, double q ) throws Exception
	{
		// Compute indexing polynomial, then set table value; return old value.

		int i, ii = 0;

		for ( i = 0; i < ndims-1; i++ )
			ii = dimensions[i+1] * (ii + ix[i]);
		ii += ix[ndims-1];

		double old = probabilities[ii];
		probabilities[ii] = q;
		return old;
	}

	public void normalize_p() 
	{
		double sum = 0;
		for ( int i = 0; i < probabilities.length; i++ )
			sum += probabilities[i];
		for ( int i = 0; i < probabilities.length; i++ )
			probabilities[i] /= sum;
	}
}
