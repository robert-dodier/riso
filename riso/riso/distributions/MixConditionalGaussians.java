package riso.distributions;
import java.io.*;
import java.rmi.*;
import riso.belief_nets.*;
import SmarterTokenizer;

/** An instance of this class represents a mixture of conditional Gaussian
  * distributions. The dependence on the parents enters through the
  * mean, which is assumed to be a linear combination of the parents plus
  * an offset, and through the mixing coefficients. The variance of each
  * component does not depend on the parents.
  */
public class MixConditionalGaussians extends AbstractConditionalDistribution
{
	/** List of the components of this mixture.
	  */
	public ConditionalGaussian[] components;

	/** Marginal distribution of the parents of this conditional distribution;
	  * note that this is stored as a <tt>MixGaussians</tt> and not as
	  * an array of distributions, so this object contains both the components
	  * and their mixing coefficients.
	  */
	public MixGaussians parent_marginal;

	/** This default constructors exists just to throw the
	  * <tt>RemoteException</tt>.
	  */
	public MixConditionalGaussians() throws RemoteException {}

	/** Return a deep copy of this object. If this object is remote,
	  * <tt>remote_clone</tt> will create a new remote object.
	  */
	public Object remote_clone() throws CloneNotSupportedException, RemoteException
	{
		MixConditionalGaussians copy = new MixConditionalGaussians();
		copy.components = (ConditionalGaussian[]) components.clone();
		for ( int i = 0; i < components.length; i++ )
			copy.components[i] = (ConditionalGaussian) components[i].remote_clone();
		copy.parent_marginal = (MixGaussians) parent_marginal.remote_clone();
		return copy;
	}

	/** Return the number of dimensions of the child variable.
	  */
	public int ndimensions_child() throws RemoteException
	{
		throw new RuntimeException( "MixConditionalGaussians.ndimensions_child: not implemented." );
	}

	/** Return the number of dimensions of the parent variables.
	  * If there is more than one parent, this is the sum of the dimensions
	  * of the parent variables.
	  */
	public int ndimensions_parent() throws RemoteException 
	{
		throw new RuntimeException( "MixConditionalGaussians.ndimensions_parent: not implemented." );
	}

	/** For a given value <code>c</code> of the parents, return a distribution
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  *
	  * @return An unconditional mixture of Gaussians density. SHOULD WE
	  *   PRUNE OUT LOW-WEIGHT COMPONENTS ???
	  */
	public Distribution get_density( double[] c ) throws RemoteException
	{
		if ( components == null || components.length == 0 ) return null;

		MixGaussians mix = new MixGaussians( components[0].ndimensions_child(), components.length );

		for ( int i = 0; i < components.length; i++ )
		{
			mix.components[i] = components[i].get_density( c );
			mix.mix_proportions[i] = parent_marginal.responsibility( i, c );
		}
		
		return mix;
	}

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  * @param c Values of parent variables.
	  */
	public double p( double[] x, double[] c ) throws RemoteException
	{
		double pxc = 0;

		for ( int i = 0; i < components.length; i++ )
		{
			pxc += parent_marginal.responsibility( i, c ) * components[i].p( x, c );
		}

		return pxc;
	}

	/** Return an instance of a random variable from this distribution.
	  * A component is selected according to the mixing proportions,
	  * then a random variable is generated from that component.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws RemoteException
	{
		double sum = 0, r = Math.random();
		for ( int i = 0; i < components.length-1; i++ )
		{
			sum += parent_marginal.responsibility( i, c );
			if ( r < sum )
				return components[i].random( c );
		}

		return components[components.length-1].random( c );
	}

	/** Parse a string containing a description of a variable. The description
	  * is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException, RemoteException
	{
		throw new RuntimeException( "MixConditionalGaussians.parse_string: not implemented." );
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
		result += this.getClass().getName()+"\n"+leading_ws+"{"+"\n";
		String more_leading_ws = leading_ws+"\t";
		String still_more_ws = more_leading_ws+"\t";

		result += more_leading_ws+"components"+"\n"+more_leading_ws+"{"+"\n";

		for ( int i = 0; i < components.length; i++ )
		{
			result += still_more_ws+"% conditional mixture component "+i+"\n";
			result += still_more_ws+components[i].format_string( still_more_ws );
			result += "\n";
		}

		result += more_leading_ws+"}"+"\n\n";
		result += more_leading_ws+"% parent marginal mixture"+"\n";
		result += more_leading_ws+parent_marginal.format_string( more_leading_ws );

		result += leading_ws+"}"+"\n";
		return result;
	}

	/** Write a description of this distribution to an output stream.
	  * The description is human-readable; this is different from object
	  * serialization. 
	  * @param os Output stream to write to.
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}
}
