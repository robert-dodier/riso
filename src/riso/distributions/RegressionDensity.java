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
import riso.regression.*;
import riso.general.*;

/** This class represents a conditional distribution based on a regression
  * function and a noise model.
  */
public class RegressionDensity extends AbstractConditionalDistribution
{
	protected int ndimensions_child, ndimensions_parent;

	/** Conditional distribution given the independent variable of the regression.
	  */
	public Distribution noise_model;

	/** Model which tells the mean response of the dependent variable
	  * given the independent variables.
	  */
	public RegressionModel regression_model;

	/** Create an empty regression distribution. Need to set the noise and
	  * regression models to get something interesting.
	  */
	public RegressionDensity() { regression_model = null; noise_model = null; }

	/** Return the number of dimensions of the dependent variable.
	  */
	public int ndimensions_child() { return ndimensions_child; }

	/** Return the number of dimensions of the independent variables.
	  * If there is more than one independent variable, this is the sum
	  * of the dimensions of the independent variables.
	  */
	public int ndimensions_parent() { return ndimensions_parent; }

	/** Return a cross-section through the regression distribution at <code>c</code>.
	  * @param c Point at which to return the cross-section.
	  * @return A <code>Distribution</code> which represents a cross-section through
	  *   the regression distribution.
	  * @see ConditionalDistribution.get_density
	  */
	public Distribution get_density( double[] c ) throws Exception
	{
		double[] y = regression_model.F(c);
		LocationScaleDensity cross_section;

		try
		{
			cross_section = (LocationScaleDensity) noise_model.clone();
		}
		catch (Exception e)
		{
			System.err.println( "RegressionDensity.get_density: return null due to exception:\n"+e );
			return null;
		}

		cross_section.set_location(y);
		return cross_section;
	}

	/** Computes the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  * @param c Values of parent variables.
	  * @return If the conditional probability is not defined for the
	  *   given value of <tt>c</tt>, return zero. Otherwise, return
	  *   <tt>p(x|c) = p_epsilon(x-F(c))</tt>.
	  */
	public double p( double[] x, double[] c ) throws Exception
	{
		double[] y;

		try { y = regression_model.F(c); }
		catch (ConditionalNotDefinedException e) { return 0; }

		double[] residual = (double[]) x.clone();
		for ( int i = 0; i < ndimensions_child; i++ )
			residual[i] -= y[i];

// System.err.print( "RegressionDensity.p: x == (" );
// for ( int i = 0; i < x.length; i++ ) System.err.print( x[i]+"," ); System.err.print( "); F(" );
// for ( int i = 0; i < c.length; i++ ) System.err.print( c[i]+"," ); System.err.print( ") == " );
// for ( int i = 0; i < y.length; i++ ) System.err.print( y[i]+"," ); System.err.println( ")" );
		return noise_model.p( residual );
	}

	/** Return an instance of a random variable from this distribution.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws Exception
	{
		double[] epsilon = noise_model.random();
		double[] y = regression_model.F(c);
		for ( int i = 0; i < ndimensions_child; i++ )
			y[i] += epsilon[i];

		return y;
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

		result += more_leading_ws+"regression-model ";
		result += regression_model.format_string( more_leading_ws );

		result += more_leading_ws+"noise-model ";
		result += noise_model.format_string( more_leading_ws );

		result += leading_ws+"}"+"\n";
		return result;
	}

	/** Read a description of this distribution model from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param st Stream tokenizer to read from.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "RegressionDensity.pretty_input: input doesn't have opening bracket; found "+st+" instead." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "regression-model" ) )
				{
					try
					{
						st.nextToken();
						Class regression_class = java.rmi.server.RMIClassLoader.loadClass( st.sval );
						regression_model = (RegressionModel) regression_class.newInstance();
						st.nextBlock();
						regression_model.parse_string( st.sval );
						ndimensions_parent = regression_model.ndimensions_in();
						ndimensions_child = regression_model.ndimensions_out();
					}
					catch (Exception e)
					{
						throw new IOException( "RegressionDensity.pretty_input: attempt to create regression model failed:\n"+e );
					}
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "noise-model" ) )
				{
					try
					{
						st.nextToken();
						Class noise_class = java.rmi.server.RMIClassLoader.loadClass( st.sval );
						noise_model = (Distribution) noise_class.newInstance();

						// Set the associated variable for the noise model to be
						// the same as for the container distribution.
						noise_model.set_variable( associated_variable );

						st.nextBlock();
						noise_model.parse_string( st.sval );
					}
					catch (Exception e)
					{
						throw new IOException( "RegressionDensity.pretty_input: attempt to create noise model failed:\n"+e );
					}
				}
				else if ( st.ttype == '}' )
				{
					found_closing_bracket = true;
					break;
				}
				else 
				{
					throw new IOException( "RegressionDensity.pretty_input: unknown token: "+st );
				}
			}

			if ( ! found_closing_bracket )
				throw new IOException( "RegressionDensity.pretty_input: no closing bracket." );
		}
		catch (IOException e)
		{
			throw new IOException( "RegressionDensity.pretty_input: attempt to read regression density failed:\n"+e );
		}
	}

	/** Use data to modify the parameters of the distribution. Classes which
	  * implement this method will typically use maximum likelihood or
	  * a similar approach to fit the parameters to the data.
	  * @param x The child data. Each row has a number of components equal
	  *   to ndimensions_child(), and the number of rows is the number of data.
	  * @param c The parent data. Each row has a number of components equal
	  *   to ndimensions_parent(), and the number of rows is the same
	  *   as for <code>x</code>.
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
	  */
	public double update( double[][] x, double[][] c, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		// Pass the update request on to the regression model.
		// This is not quite correct -- if we change the noise model, we should
		// take that information into account when we do the update. Oh well.

		System.err.println( "RegressionDensity.update: HACK: pass update on to regression model." );

		return regression_model.update( c, x, niter_max, stopping_criterion, responsibility );
	}

	/** Return a deep copy of this regression distribution object.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		RegressionDensity copy = (RegressionDensity) super.clone();
		copy.ndimensions_child = ndimensions_child;
		copy.ndimensions_parent = ndimensions_parent;
		copy.noise_model = (Distribution) noise_model.clone();
		copy.regression_model = (RegressionModel) regression_model.clone();

		return copy;
	}
}
