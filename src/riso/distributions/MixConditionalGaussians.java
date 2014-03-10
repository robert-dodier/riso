/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999, Robert Dodier.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA, 02111-1307, USA,
 * or visit the GNU web site, www.gnu.org.
 */
package riso.distributions;
import java.io.*;
import java.rmi.*;
import riso.general.*;

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

	/** Return a deep copy of this object. 
	  */
	public Object clone() throws CloneNotSupportedException
	{
		MixConditionalGaussians copy = (MixConditionalGaussians) super.clone();
		copy.components = (ConditionalGaussian[]) components.clone();
		for ( int i = 0; i < components.length; i++ )
			copy.components[i] = (ConditionalGaussian) components[i].clone();
		copy.parent_marginal = (MixGaussians) parent_marginal.clone();
		return copy;
	}

	/** Return the number of dimensions of the child variable.
	  */
	public int ndimensions_child()
	{
		throw new RuntimeException( "MixConditionalGaussians.ndimensions_child: not implemented." );
	}

	/** Return the number of dimensions of the parent variables.
	  * If there is more than one parent, this is the sum of the dimensions
	  * of the parent variables.
	  */
	public int ndimensions_parent()
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
	public Distribution get_density( double[] c ) throws Exception
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
	public double p( double[] x, double[] c ) throws Exception
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
	public double[] random( double[] c ) throws Exception
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
		result += this.getClass().getName()+"\n"+leading_ws+"{"+"\n";
		String more_leading_ws = leading_ws+"\t";
		String still_more_ws = more_leading_ws+"\t";

		result += more_leading_ws+"ncomponents "+components.length+"\n";
		result += more_leading_ws+"components"+"\n"+more_leading_ws+"{"+"\n";

		for ( int i = 0; i < components.length; i++ )
		{
			result += still_more_ws+"% conditional mixture component "+i+"\n";
			result += still_more_ws+components[i].format_string( still_more_ws );
			result += "\n";
		}

		result += more_leading_ws+"}"+"\n\n";
		result += more_leading_ws+"parent-marginal "+parent_marginal.format_string( more_leading_ws );

		result += leading_ws+"}"+"\n";
		return result;
	}

	/** Read in a <tt>MixConditionalGaussians</tt> from an input stream. This is intended
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
				throw new IOException( "MixConditionalGaussians.pretty_input: input doesn't have opening bracket; tokenizer state: "+st );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "ncomponents" ) )
				{
					st.nextToken();
					int n = Integer.parseInt(st.sval);
					components = new ConditionalGaussian[n];
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "components" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' )
						throw new IOException( "MixConditionalGaussians.pretty_input: ``components'' lacks opening bracket." );

					for ( int i = 0; i < components.length; i++ )
					{
						st.nextToken(); // eat class name -- should check to see it is ConditionalGaussian !!!
						components[i] = new ConditionalGaussian();

						// Set the associated variable for each component to be the
						// same as for the container distribution.
						((ConditionalDistribution)components[i]).set_variable( associated_variable );

						components[i].pretty_input(st);
					}

					st.nextToken();
					if ( st.ttype != '}' )
						throw new IOException( "MixConditionalGaussians.pretty_input: ``components'' lacks a closing bracket." );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "parent-marginal" ) )
				{
					st.nextToken(); // eat class name -- should check to see it is MixGaussians !!!
					parent_marginal = new MixGaussians();
					parent_marginal.pretty_input(st);
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
			throw new IOException( "MixConditionalGaussians.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "MixConditionalGaussians.pretty_input: no closing bracket on input; tokenizer state: "+st );
	}
}
