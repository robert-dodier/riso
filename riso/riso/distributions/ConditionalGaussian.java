package riso.distributions;
import java.io.*;
import java.rmi.*;
import riso.belief_nets.*;
import numerical.*;
import SmarterTokenizer;

/** An instance of this class represents a conditional Gaussian distribution.
  * The dependence enters only through the mean, which is a linear combination
  * the parents plus an offset. The variance is constant.
  */
public class ConditionalGaussian extends AbstractConditionalDistribution
{
	double[][] Sigma_1c2_inverse = null;
	double det_Sigma_1c2 = 0;

	/** Covariance matrix of the marginal distribution of the variables
	  * on which we are conditioning.
	  */
	public double[][] Sigma_22;

	/** Offset for conditional mean calculation. The conditional mean is calculated as
	  * <tt>a_mu_1c2 * x2 + b_mu_1c2</tt>, where <tt>x2</tt> is the vector of variables
	  * on which we are conditioning.
	  */
	public double[] b_mu_1c2;

	/** Multiplier for conditional mean calculation.
	  */
	public double[][] a_mu_1c2;

	/** Covariance matrix of the conditional distribution.
	  */
	public double[][] Sigma_1c2;
	
	/** Return a deep copy of this object. If this object is remote,
	  * <tt>remote_clone</tt> will create a new remote object.
	  */
	public Object remote_clone() throws CloneNotSupportedException
	{
		ConditionalGaussian copy = new ConditionalGaussian();
		copy.Sigma_22 = (Sigma_22 == null ? null : (double[][])Sigma_22.clone());
		copy.b_mu_1c2 = (b_mu_1c2 == null ? null : (double[])b_mu_1c2.clone());
		copy.a_mu_1c2 = (a_mu_1c2 == null ? null : (double[][])a_mu_1c2.clone());
		copy.Sigma_1c2 = (Sigma_1c2 == null ? null : (double[][])Sigma_1c2.clone());

		return copy;
	}

	/** Return the number of dimensions of the child variable.
	  */
	public int ndimensions_child() { return b_mu_1c2.length; }

	/** Return the number of dimensions of the parent variables.
	  * If there is more than one parent, this is the sum of the dimensions
	  * of the parent variables.
	  */
	public int ndimensions_parent() { return Sigma_22.length; }

	/** For a given value <code>c</code> of the parents, return a distribution
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  */
	public Distribution get_density( double[] c ) throws Exception
	{
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
System.err.println( "ConditionalGaussian.random: VERY SLOW IMPLEMENTATION!!!" );
		return get_density( c ).random();
	}

	/** Parse a string containing a description of a variable. The description
	  * is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException
	{
		throw new RuntimeException( "ConditionalGaussian.parse_string: not implemented." ); 
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

		result += this.getClass().getName()+"\n"+leading_ws+"{"+"\n";
		
		result += more_leading_ws+"parent-marginal-covariance";
		if ( Sigma_22.length == 1 ) 
			result += " { "+Sigma_22[0][0]+" }\n";
		else
		{
			result += "\n"+more_leading_ws+"{\n";
			for ( i = 0; i < Sigma_22.length; i++ )
			{
				result += still_more_ws;
				for ( j = 0; j < Sigma_22[i].length; j++ )
					result += Sigma_22[i][j]+" ";
				result += "\n";
			}
			result += more_leading_ws+"}\n";
		}

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

		result += more_leading_ws+"conditional-covariance";
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
}
