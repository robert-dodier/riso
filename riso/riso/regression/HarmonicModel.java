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
package riso.regression;
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import riso.numerical.*;
import riso.general.*;

/** An instance of this class represents a model which is a sum of
  * harmonic functions. Each component can have an arbitrary
  * amplitude, phase shift, and period.
  */
public class HarmonicModel implements RegressionModel
{
	public double offset;
	public int ncomponents;
	public double[] amplitude, period, shift;

	/** Creates an empty, unusable object. <tt>pretty_input</tt> can be used
	  * read in parameters.
	  */
	public HarmonicModel() { offset = 0; ncomponents = 0; amplitude = period = shift = null; }

	/** Make a deep copy of this object and return a reference to the copy.
	  * If the object is remote, the returned reference is a remote reference.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		HarmonicModel copy = new HarmonicModel();

		copy.offset = offset;
		copy.ncomponents = ncomponents;
		copy.amplitude = (double[]) amplitude.clone();
		copy.period = (double[]) period.clone();
		copy.shift = (double[]) shift.clone();

		return copy;
	}

	/** Returns the output of the sum of harmonics at the specified input.
	  * @param x Input point; should be a 1-element array.
	  * @return Sum of components of this model.
	  */
	public double[] F( double[] x ) 
	{
		double[] sum = new double[1];

		sum[0] = offset;
		for ( int i = 0; i < ncomponents; i++ )
			sum[0] += amplitude[i] * Math.cos( 2*Math.PI*x[0]/period[i] - shift[i] );

		return sum;
	}

	/** Return the Jacobian matrix (i.e., matrix of partial derivatives) 
	  * of the regression function w.r.t. the input.
	  * @param x Input point; should be a 1-element array.
	  * @return Jacobian matrix at <code>x</code>; this will be a 1-by-1 matrix.
	  */
	public double[][] dFdx( double[] x ) 
	{
		double[][] sum = new double[1][1];

		for ( int i = 0; i < ncomponents; i++ )
			sum[0][0] += -amplitude[i] * Math.sin( 2*Math.PI*x[0]/period[i] - shift[i] ) *2*Math.PI /period[i];

		return sum;
	}

	/** Use data to modify the parameters of the regression model. Classes which
	  * implement this method will typically use maximum likelihood or
	  * a similar approach to fit the parameters to the data.
	  * @param x The data. Each row has a number of components equal to the
	  *   number of dimensions of the model, and the number of rows is the
	  *   number of data.
	  * @param responsibility Each component of this vector 
	  *   <code>responsibility[i]</code> is a scalar telling the probability
	  *   that this density produced the corresponding datum <code>x[i]</code>.
	  *   This is mostly intended for fitting mixture densities, although
	  *   other uses can be imagined. If this array is <code>null</code> then
	  *   assume that all responsibilities are 1.
	  * @return Some indication of goodness-of-fit, such as MSE or negative
	  *   log-likelihood.
	  */
	public double update( double[][] x, double[][] y, int niter_max, double stopping_criterion, double[] responsibility ) throws Exception
	{
		throw new Exception( "HarmonicModel.update: not supported." );
	}

	/** Parses a string containing a description of a harmonic model.
	  * The description is contained within curly braces, which are
	  * included in the string.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Creates a description of this harmonic model as a string.
	  * This is a full description, suitable for printing, containing
	  * newlines and indents.
	  *
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public String format_string( String leading_ws )
	{
		String result = "";

		result += this.getClass().getName()+"\n"+leading_ws+"{"+"\n";
		String more_leading_ws = leading_ws+"\t";
		String still_more_ws = more_leading_ws+"\t";

		result += more_leading_ws+"offset "+offset+"\n";
		result += more_leading_ws+"ncomponents "+ncomponents+"\n";
		result += more_leading_ws+"components"+"\n"+more_leading_ws+"{"+"\n";

		for ( int i = 0; i < ncomponents; i++ )
		{
			result += still_more_ws+"% component["+i+"]"+"\n";
			result += still_more_ws+"{ amplitude "+amplitude[i]+"  period "+period[i]+"  phase-shift "+shift[i]+" }\n";
		}

		result += more_leading_ws+"}"+"\n";
		result += leading_ws+"}"+"\n";

		return result;
	}

	/** Reads a harmonic model through a tokenizer.
	  */
	public void pretty_input( StreamTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "HarmonicModel.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "offset" ) )
				{
					st.nextToken();
					offset = Double.parseDouble( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "ncomponents" ) )
				{
					st.nextToken();
					ncomponents = Integer.parseInt( st.sval );

					amplitude = new double[ ncomponents ];
					period = new double[ ncomponents ];
					shift = new double[ ncomponents ];		// this can stay all zeros

					for ( int i = 0; i < ncomponents; i++ )
						amplitude[i] = period[i] = 1;
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "components" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' )
						throw new IOException( "HarmonicModel.pretty_input: ``components'' input doesn't have opening bracket." );
					
					for ( int i = 0; i < ncomponents; i++ )
					{
						st.nextToken();
						if ( st.ttype != '{' )
							throw new IOException( "HarmonicModel.pretty_input: components["+i+"] input doesn't have opening bracket." );

						while ( st.ttype != '}' )
						{
							st.nextToken();
							if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "amplitude" ) )
							{
								st.nextToken();
								amplitude[i] = Double.parseDouble( st.sval );
							}
							else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "period" ) )
							{
								st.nextToken();
								period[i] = Double.parseDouble( st.sval );
							}
							else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "phase-shift" ) )
							{
								st.nextToken();
								shift[i] = Double.parseDouble( st.sval );
							}
							else if ( st.ttype != '}' )
							{
								throw new IOException( "HarmonicModel.pretty_input: unknown stuff in components["+i+"]; parser state: "+st );
							}
						}
					}

					st.nextToken();
					if ( st.ttype != '}' )
						throw new IOException( "HarmonicModel.pretty_input: ``components'' input doesn't have closing bracket." );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD )
				{
					throw new IOException( "HarmonicModel.pretty_input: unknown keyword: "+st.sval );
				}
				else if ( st.ttype == '}' )
				{
					found_closing_bracket = true;
					break;
				}
				else
				{
					throw new IOException( "HarmonicModel.pretty_input: parser failure; tokenizer state: "+st );
				}
			}
		}
		catch (IOException e)
		{
			throw new IOException( "HarmonicModel.pretty_input: attempt to read network failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "HarmonicModel.pretty_input: no closing bracket on input." );
	}

	/** Writes this harmonic model to an output stream; just a front-end
	  * for <tt>format_string</tt> (q.v.).
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	public int ndimensions_in() { return 1; }

	public int ndimensions_out() { return 1; }

    public output_pair[] cross_validation( double[][] x, double[][] y, int nfolds, int niter_max, double stopping_criterion, double[] responsibility ) throws Exception
    {
        throw new Exception ("HarmonicModel.cross_validation: not implemented.");
    }
}
