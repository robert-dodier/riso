package riso.distributions;
import java.io.*;
import java.rmi.*;
import riso.belief_nets.*;
import numerical.*;
import SmarterTokenizer;

/** An instance of this class represents a conditional Gaussian distribution.
  * The dependence enters only through the mean, which is a linear combination
  * the parents plus an offset. The variance is constant.
  *
  * <p> Writing the marginal means of the child and parent variables,
  * respectively, as <tt>mu(1)</tt> and <tt>mu(2)</tt>, and the respective
  * marginal variances as <tt>Sigma(11)</tt> and <tt>Sigma(22)</tt>, and the
  * covariance as <tt>Sigma(12)</tt>, then the conditional mean <tt>mu(1|2)</tt>
  * and conditional variance <tt>Sigma(1|2)</tt> are as follows.
  * <pre>
  *     mu(1|2) = mu(1) + Sigma(12) Sigma(22)^{-1} (X(2)-mu(2))
  *     Sigma(1|2) = Sigma(11) - Sigma(12) Sigma(22)^{-1} Sigma(21)
  * </pre>
  * where the parent variables appear as <tt>X(2)</tt>.
  * These parameters are named as follows in the description for an object
  * of this type:
  * <pre>
  *     conditional-mean-multiplier == Sigma(12) Sigma(22)^{-1}
  *     conditional-mean-offset     == mu(1) - Sigma(12) Sigma(22)^{-1} mu(2)
  *     conditional-variance        == Sigma(1|2)
  * </pre>
  * In the code, these three parameters are called <tt>a_mu_1c2</tt>,
  * <tt>b_mu_1c2</tt>, and <tt>Sigma_1c2</tt>, respectively.
  */
public class ConditionalGaussian extends AbstractConditionalDistribution
{
	double[][] Sigma_1c2_inverse = null;
	double det_Sigma_1c2 = 0;

	// These strings contain vectors and matrices; in order to parse these,
	// we have to wait until we know how many parents there are.

	String Sigma_1c2_string = null;
	String a_mu_1c2_string = null;
	String b_mu_1c2_string = null;
	
	/** Offset for conditional mean calculation. The conditional mean is calculated as
	  * <tt>a_mu_1c2 * x2 + b_mu_1c2</tt>, where <tt>x2</tt> is the vector of variables
	  * on which we are conditioning.
	  */
	public double[] b_mu_1c2;

	/** Multiplier for conditional mean calculation.
	  */
	public double[][] a_mu_1c2;

	/** Covariance matrix of the conditional distribution.
	  * This matrix has number of rows and columns equal to the dimension of
	  * the child.
	  */
	public double[][] Sigma_1c2;
	
	/** Return a deep copy of this object. If this object is remote,
	  * <tt>remote_clone</tt> will create a new remote object.
	  */
	public Object remote_clone() throws CloneNotSupportedException
	{
		ConditionalGaussian copy = new ConditionalGaussian();
		copy.b_mu_1c2 = (b_mu_1c2 == null ? null : (double[])b_mu_1c2.clone());
		copy.a_mu_1c2 = (a_mu_1c2 == null ? null : (double[][])a_mu_1c2.clone());
		copy.Sigma_1c2 = (Sigma_1c2 == null ? null : (double[][])Sigma_1c2.clone());

		return copy;
	}

	/** Return the number of dimensions of the child variable.
	  */
	public int ndimensions_child()
	{
		try { check_matrices(); }
		catch (Exception e) { throw new RuntimeException( "ConditionalGaussian.ndimensions_child: failed:\n\t"+e ); }
		return a_mu_1c2.length;
	}

	/** Return the number of dimensions of the parent variables.
	  * If there is more than one parent, this is the sum of the dimensions
	  * of the parent variables.
	  */
	public int ndimensions_parent()
	{
		try { check_matrices(); }
		catch (Exception e) { throw new RuntimeException( "ConditionalGaussian.ndimensions_parent: failed:\n\t"+e ); }
		return a_mu_1c2[0].length;
	}

	/** For a given value <code>c</code> of the parents, return a distribution
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  */
	public Distribution get_density( double[] c ) throws Exception
	{
		check_matrices();
		double[] mu = (double[]) b_mu_1c2.clone();
		Matrix.add( mu, Matrix.multiply( a_mu_1c2, c ) );
		return new Gaussian( mu, Sigma_1c2 );
	}

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  * @param c Values of parent variables.
	  */
	public double p( double[] x, double[] c ) throws Exception
	{
		check_matrices();
		double[] mu = (double[]) b_mu_1c2.clone();
		Matrix.add( mu, Matrix.multiply( a_mu_1c2, c ) );

		if ( Sigma_1c2_inverse == null ) Sigma_1c2_inverse = Matrix.inverse( Sigma_1c2 );
		if ( det_Sigma_1c2 == 0 ) det_Sigma_1c2 = Matrix.determinant( Sigma_1c2 );

		return Gaussian.g( x, mu, Sigma_1c2_inverse, det_Sigma_1c2 );
	}

	/** Return an instance of a random variable from this distribution.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws Exception
	{
		check_matrices();
System.err.println( "ConditionalGaussian.random: VERY SLOW IMPLEMENTATION!!!" );
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
		
		result += more_leading_ws+"conditional-mean-multiplier";
		if ( a_mu_1c2.length == 1 && a_mu_1c2[0].length == 1 )
			result += " { "+a_mu_1c2[0][0]+" }\n";
		else
		{
			result += "\n"+more_leading_ws+"{\n";
			for ( i = 0; i < a_mu_1c2.length; i++ )
			{
				result += still_more_ws;
				for ( j = 0; j < a_mu_1c2[i].length; j++ )
					result += a_mu_1c2[i][j]+" ";
				result += "\n";
			}
			result += more_leading_ws+"}\n";
		}

		result += more_leading_ws+"conditional-mean-offset { ";
		for ( i = 0; i < b_mu_1c2.length; i++ )
			result += b_mu_1c2[i]+" ";
		result += "}\n";

		result += more_leading_ws+"conditional-variance";
		if ( Sigma_1c2.length == 1 )
			result += " { "+Sigma_1c2[0][0]+" }\n";
		else
		{
			result += "\n"+more_leading_ws+"{\n";
			for ( i = 0; i < Sigma_1c2.length; i++ )
			{
				result += still_more_ws;
				for ( j = 0; j < Sigma_1c2[i].length; j++ )
					result += Sigma_1c2[i][j]+" ";
				result += "\n";
			}
			result += more_leading_ws+"}\n";
		}

		result += leading_ws+"}\n";
		return result;
	}

	/** Read in a <tt>ConditionalGaussian</tt> from an input stream. This is intended
	  * for input from a human-readable source; this is different from object serialization.
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
				throw new IOException( "ConditionalGaussian.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "conditional-mean-multiplier" ) )
				{
					st.nextBlock();
					a_mu_1c2_string = st.sval;
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "conditional-mean-offset" ) )
				{
					st.nextBlock();
					b_mu_1c2_string = st.sval;
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "conditional-variance" ) )
				{
					st.nextBlock();
					Sigma_1c2_string = st.sval;
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
			throw new IOException( "Gaussian.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "Gaussian.pretty_input: no closing bracket on input." );
	}

	/** If vectors and matrices descriptions have not yet been parsed,
	  * do so now. If they are already parsed, do nothing.
	  *
	  * @throws IOException If the description parsing fails.
	  * @throws RemoteException If the attempt to reference parents fails.
	  */
	public void check_matrices() throws IOException, RemoteException
	{
		int nchild = 1;	// THIS IS THE ONLY PLACE THE CHILD DIMENSION IS RESTRICTED; CHANGE ??? !!!

		if ( Sigma_1c2 == null )
		{
			// First figure out how many elements there are in the a_mu_1c2 description;
			// this is equal to nchild*nparents, so set nparents = nelements/nchild.
			
			int nelements = -2;	// don't count the left and right parentheses.
			SmarterTokenizer st = new SmarterTokenizer( new StringReader(a_mu_1c2_string) );
			for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
				++nelements;
			int nparents = nelements/nchild;
System.err.println( "check_matrices: count "+nelements+" in a_mu_1c2; nparents: "+nparents );

			Sigma_1c2 = parse_matrix( Sigma_1c2_string, nchild, nchild );
System.err.println( "Sigma_1c2: " );
numerical.Matrix.pretty_output( Sigma_1c2, System.err, "\t" );
			a_mu_1c2 = parse_matrix( a_mu_1c2_string, nchild, nparents );
System.err.println( "a_mu_1c2: " );
numerical.Matrix.pretty_output( a_mu_1c2, System.err, "\t" );
			b_mu_1c2 = parse_vector( b_mu_1c2_string, nchild );
System.err.print( "b_mu_1c2: " );
numerical.Matrix.pretty_output( b_mu_1c2, System.err, " " );
System.err.println("");
		}
	}

	static double[][] parse_matrix( String s, int nrows, int ncols ) throws IOException
	{
		double[][] A = new double[nrows][ncols];
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( s ) );
System.err.println( "parse_matrix: s: "+s );
		
		st.nextToken();
		if ( st.ttype != '{' )
			throw new IOException( "ConditionalGaussian.parse_matrix: input doesn't have opening bracket." );

		for ( int i = 0; i < nrows; i++ )
			for ( int j = 0; j < ncols; j++ )
			{
				st.nextToken();
				A[i][j] = Format.atof( st.sval );
			}

		st.nextToken();
		if ( st.ttype != '}' )
			throw new IOException( "ConditionalGaussian.parse_matrix: input doesn't have closing bracket." );

		return A;
	}

	static double[] parse_vector( String s, int n ) throws IOException
	{
		double[] x = new double[n];
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( s ) );
System.err.println( "parse_vector: s: "+s );

		st.nextToken();
		if ( st.ttype != '{' )
			throw new IOException( "ConditionalGaussian.parse_matrix: input doesn't have opening bracket." );

		for ( int j = 0; j < n; j++ )
		{
			st.nextToken();
			x[j] = Format.atof( st.sval );
		}

		st.nextToken();
		if ( st.ttype != '}' )
			throw new IOException( "ConditionalGaussian.parse_matrix: input doesn't have closing bracket." );
		
		return x;
	}
}
