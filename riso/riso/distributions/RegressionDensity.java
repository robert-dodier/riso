package distributions;
import java.io.*;
import java.rmi.*;
import regression.*;

/** This class represents a conditional distribution based on a regression
  * function and a noise model.
  */
public class RegressionDensity implements ConditionalDistribution
{
	protected int ndimensions_child, ndimensions_parent;

	/** Conditional distribution given the independent variable of the regression.
	  */
	Distribution noise_model;

	/** Model which tells the mean response of the dependent variable
	  * given the independent variables.
	  */
	RegressionModel regression_model;

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
	public Distribution get_density( double[] c ) throws RemoteException
	{
		double[] y = regression_model.F(c);
		LocationScaleDensity cross_section;

		try
		{
			cross_section = (LocationScaleDensity) noise_model.remote_clone();
		}
		catch (Exception e)
		{
			System.err.println( "RegressionDensity.get_density: return null due to exception:\n"+e );
			return null;
		}

		cross_section.set_location(y);
		return cross_section;
	}

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  * @param c Values of parent variables.
	  */
	public double p( double[] x, double[] c ) throws RemoteException
	{
		double[] y = regression_model.F(c);
		double[] residual = (double[]) x.clone();
		for ( int i = 0; i < ndimensions_child; i++ )
			residual[i] -= y[i];

		return noise_model.p( residual );
	}

	/** Return an instance of a random variable from this distribution.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws RemoteException
	{
		double[] epsilon = noise_model.random();
		double[] y = regression_model.F(c);
		for ( int i = 0; i < ndimensions_child; i++ )
			y[i] += epsilon[i];

		return y;
	}

	/** Read a description of this distribution model from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param st Stream tokenizer to read from.
	  */
	public void pretty_input( StreamTokenizer st ) throws IOException
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
						Class regression_class = Class.forName( st.sval );
						regression_model = (RegressionModel) regression_class.newInstance();
						regression_model.pretty_input(st);
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
						Class noise_class = Class.forName( st.sval );
						noise_model = (Distribution) noise_class.newInstance();
						noise_model.pretty_input(st);
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

	/** Write a description of this distribution model to an output stream.
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
		dest.println( leading_ws+this.getClass().getName()+"\n"+leading_ws+"{" );
		String more_leading_ws = leading_ws+"\t";

		dest.println( more_leading_ws+"regression-model " );
		regression_model.pretty_output( os, more_leading_ws );

		dest.println( more_leading_ws+"noise-model " );
		noise_model.pretty_output( os, more_leading_ws );

		dest.println( leading_ws+"}" );
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
		throw new Exception( "RegressionModel.update: not implemented." );
	}

	/** Return a deep copy of this regression distribution object.
	  */
	public Object remote_clone() throws CloneNotSupportedException, RemoteException
	{
		RegressionDensity copy = new RegressionDensity();
		copy.ndimensions_child = ndimensions_child;
		copy.ndimensions_parent = ndimensions_parent;
		copy.noise_model = (Distribution) noise_model.remote_clone();
		copy.regression_model = (RegressionModel) regression_model.remote_clone();

		return copy;
	}
}
