/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999-2001, Robert Dodier.
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
import java.util.*;
import riso.numerical.*;
import riso.general.*;

/** An instance of this class represents an outer product of distributions,
  * that is, a distribution which has a density of the form
  * <pre>
  *   p(x) = \prod_i p_i(x[j_i])
  * </pre>
  * where <tt>j_i</tt> is a subset of the indices <tt>0,1,2,...,x.length-1</tt>,
  * with all subsets disjoint.
  */
public class OuterProduct extends AbstractDistribution
{
	int[][] subsets;

	Distribution[] distributions;

	/** Constructs an empty outer product of distributions.
	  */
	public OuterProduct() {}

	/** Returns the number of dimensions in which this distribution lives.
	  */
	public int ndimensions() { throw new RuntimeException( "OuterProduct.ndimensions: not implemented." ); }

	/** Compute the density at the point <tt>x</tt>.
	  * @param x Point at which to evaluate density; this is an array of length <tt>ndimensions()</tt>.
	  */
	public double p( double[] x ) throws Exception
	{
		double prod = 1;
		for ( int i = 0; i < subsets.length; i++ )
		{
			// Load up a new array with just the elements in the i'th subset.
			double[] xi = new double[ subsets[i].length ];
			for ( int j = 0; j < subsets[i].length; j++ )
				xi[j] = x[ subsets[i][j] ];

			prod *= distributions[i].p(xi);
		}

		return prod;
	}

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, assuming some prior distribution has been 
	  * established. This may not be meaningful.
	  */
	public double log_prior() throws Exception
	{
		throw new Exception( "OuterProduct.log_prior: not implemented." );
	}

	/** Returns an instance of a random variable from this distribution.
	  */
	public double[] random() throws Exception
	{
		throw new Exception( "OuterProduct.random: not implemented." );
	}

	/** Uses data to modify the parameters of the distribution.
	  * Split up the data by column subsets, and hand off each subset to the 
	  * corresponding component to do its own update.
	  *
	  * @param x The data to be fitted. One row per case.
	  * @param responsibility Each component of this vector 
	  *   <tt>responsibility[i]</tt> is a scalar telling the probability
	  *   that this distribution produced the corresponding datum <tt>x[i]</tt>.
	  * @param niter_max Maximum number of iterations of the update algorithm.
	  *   This value is passed through to the components of this object.
	  * @param stopping_criterion A number which describes when to stop the
	  *   update algorithm. This value is passed through to the components of this object.
	  * @return Negative log-likelihood at end of update.
	  * @throws Exception If the update algorithm fails; if no exception is
	  *   thrown, the algorithm succeeded.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		for ( int i = 0; i < distributions.length; i++ )
		{
			double[][] xi = new double[ x.length ][ subsets[i].length ];

			for ( int j = 0; j < x.length; j++ )
				for ( int k = 0; k < subsets[i].length; k++ )
					xi[j][k] = x[j][ subsets[i][k] ];

			distributions[i].update( xi, responsibility, niter_max, stopping_criterion );

System.err.println( "OuterProduct.update: end of iteration "+i+", nll: "+weighted_nll(x,responsibility) );
		}

		return weighted_nll( x, responsibility );
	}

	/** Computes the negative log likelihood, weighting each case by the responsibility.
	  */
	public double weighted_nll( double[][] x, double[] responsibility ) throws Exception
	{
		double nll = 0;

		if ( responsibility == null )
			for ( int i = 0; i < x.length; i++ )
				nll += -log_p(x[i]);
		else
			for ( int i = 0; i < x.length; i++ )
				nll += responsibility[i] * -log_p(x[i]);

		return nll;
	}

	/** Returns the expected value of this distribution.
	  */
	public double expected_value() throws Exception
	{
		throw new Exception( "OuterProduct.expected_value: not implemented." );
	}

	/** Returns the square root of the variance of this distribution.
	  */
	public double sqrt_variance() throws Exception
	{
		throw new Exception( "OuterProduct.sqrt_variance: not implemented." );
	}

	/** Returns an interval which contains almost all of the mass of this 
	  * distribution. NOT IMPLEMENTED !!!
	  *
	  * @throws Exception Always, since this method is not implemented.
	  */
	public double[] effective_support( double epsilon ) throws Exception
	{
		throw new Exception( "OuterProduct.effective_support: not implemented." );
	}

	/** Formats a string representation of this distribution.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		String result = "", more_leading_ws = leading_ws+"\t", still_more_ws = more_leading_ws+"\t";

		result += this.getClass().getName()+"\n"+leading_ws+"{\n";

		for ( int i = 0; i < distributions.length; i++ )
		{
			String s1 = "", s2 = "";
			for ( int j = 0; j < subsets[i].length; j++ ) s1 += subsets[i][j]+" ";
			s2 = distributions[i].format_string( more_leading_ws+"\t" );

			result += more_leading_ws+"component"+"\n"+more_leading_ws+"{"+"\n"+still_more_ws+"subset { "+s1+"}\n";
			result += still_more_ws+"distribution"+" "+s2+more_leading_ws+"}"+"\n";
		}

		result += leading_ws+"}\n";

		return result;
	}

	/** Read an instance of this distribution from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param st Stream tokenizer to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		Vector subsets = new Vector(), distributions = new Vector();

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "OuterProduct.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "component" ) )
				{
					pretty_input_component( st, subsets, distributions );
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
			throw new IOException( "OuterProduct.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "OuterProduct.pretty_input: no closing bracket on input." );

		this.subsets = new int[ subsets.size() ][];
		for ( int i = 0; i < this.subsets.length; i++ )
		{
			Vector subset = (Vector) ((Vector)subsets).elementAt(i);
			this.subsets[i] = new int[ subset.size() ];
			for ( int j = 0; j < this.subsets[i].length; j++ )
				this.subsets[i][j] = Integer.parseInt( (String) subset.elementAt(j) );
		}

		this.distributions = new Distribution[ distributions.size() ];
		for ( int i = 0; i < distributions.size(); i++ )
			this.distributions[i] = (Distribution) distributions.elementAt(i);
	}

	public void pretty_input_component( SmarterTokenizer st, Vector subsets, Vector distributions ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "OuterProduct.pretty_input_component: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "subset" ) )
				{
					Vector subset = new Vector();

					st.nextToken();
					if ( st.ttype != '{' )
						throw new IOException( "OuterProduct.pretty_input_component: ``subset'' lacks opening bracket." );
					for ( st.nextToken(); st.ttype == StreamTokenizer.TT_WORD && st.ttype != '}'; st.nextToken() )
					{
						subset.addElement(st.sval);
					}

					subsets.addElement(subset);
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "distribution" ) )
				{
					Distribution d;

					// The next token must be the name of a class.
					try
					{
						st.nextToken();
						Class c = java.rmi.server.RMIClassLoader.loadClass( st.sval );
						d = (Distribution) c.newInstance();
					}
					catch (Exception e)
					{
						throw new IOException( "OuterProduct.pretty_input_component: attempt to create component failed:\n"+e );
					}

					// Set the associated variable for each component to be the
					// same as for the container distribution.
					d.set_variable( associated_variable );

					st.nextBlock();
					d.parse_string( st.sval );

					distributions.addElement(d);
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
			throw new IOException( "OuterProduct.pretty_input_component: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "OuterProduct.pretty_input_component: no closing bracket on input." );
	}

	public static void main( String[] args )
	{
		try
		{
			OuterProduct op = new OuterProduct();
			
			int[][] s = new int[2][1];
			s[0][0] = 0;
			s[1][0] = 1;

			Distribution[] d = new Distribution[2];
			d[0] = new Lognormal(2,3);
			d[1] = new Mises(1,10);

			op.subsets = s;
			op.distributions = d;

			String fmt = op.format_string("\t");
			System.out.println( "OuterProduct.main: op:\n\t"+fmt );

			SmarterTokenizer st = new SmarterTokenizer( new StringReader(fmt) );
			st.nextToken();
			AbstractDistribution op2 = (AbstractDistribution) Class.forName(st.sval).newInstance();
			op2.pretty_input(st);

			System.out.println( "OuterProduct.main: op2:\n\t"+op2.format_string("\t") );

			double[] x = new double[2], x0 = new double[1], x1 = new double[1];
			x[0] = 1;
			x[1] = 3;
			x0[0] = x[0];
			x1[0] = x[1];

			System.out.println( "OuterProduct.main: d[0].p(x), d[1].p(x), op2.p(x): "+d[0].p(x0)+", "+d[1].p(x1)+", "+op2.p(x) );
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
