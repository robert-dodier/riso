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
  * exponentials. See E. Driver and D. Morrell, ``A new method for implementing
  * hybrid Bayesian networks,'' unpublished technical report.
  */
public class RadarCrossSection implements RegressionModel
{
	public double A, B, C;

	/** Creates an empty object. <tt>pretty_input</tt> can be used
	  * read in parameters.
	  */
	public RadarCrossSection() { A = B = C = 0; }

	/** Make a copy of this object and return a reference to the copy.
	  * If the object is remote, the returned reference is a remote reference.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		RadarCrossSection copy = new RadarCrossSection();

		copy.A = A;
		copy.B = B;
		copy.C = C;

		return copy;
	}

	/** Returns the output of radar cross section function at the specified input.
	  * The form of the function is
	  * <pre>
	  *   RCS(theta) = A exp( -B (theta-pi/2)^2 ) + A exp( -B (theta-3pi/2)^2 ) - C
	  * </pre>
	  *
	  * @param theta Input point; should be a 1-element array.
	  * @throws IllegalArgumentException If <tt>theta</tt> is outside the range
	  *   of values for which this function is defined.
	  */
	public double[] F( double[] theta ) throws Exception
	{
		if ( theta[0] < -5 || theta[0] > 11 ) throw new IllegalArgumentException( "RadarCrossSection.F: theta "+theta[0]+" is not in allowable range." );

		double[] sum = new double[1];

		sum[0] = -C;
		sum[0] += A * Math.exp( -B*(theta[0] - Math.PI/2)*(theta[0] - Math.PI/2) );
		sum[0] += A * Math.exp( -B*(theta[0] - 3*Math.PI/2)*(theta[0] - 3*Math.PI/2) );

		return sum;
	}

	/** Return the Jacobian matrix (i.e., matrix of partial derivatives) 
	  * of the regression function w.r.t. the input.
	  * @param x Input point; should be a 1-element array.
	  * @return Jacobian matrix at <code>x</code>; this will be a 1-by-1 matrix.
	  */
	public double[][] dFdx( double[] theta )
	{
		double[][] sum = new double[1][1];

		double e1 = theta[0] - Math.PI/2, e2 = theta[0] - 3*Math.PI/2;
		sum[0][0] += -2*B*e1 * A * Math.exp( -B*e1*e1 );
		sum[0][0] += -2*B*e2 * A * Math.exp( -B*e2*e2 );

		return sum;
	}

	/** @throws Exception This method is not implemented.
	  */
	public double update( double[][] x, double[][] y, int niter_max, double stopping_criterion, double[] responsibility ) throws Exception
	{
		throw new Exception( "RadarCrossSection.update: not supported." );
	}

	/** Parses a string containing a description of an RCS model.
	  * The description is contained within curly braces, which are
	  * included in the string.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Creates a description of this RCS model as a string.
	  * @param leading_ws Leading whitespace.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		String result = "";

		result += this.getClass().getName()+" { ";
		result += "A "+A+"  B "+B+"  C "+C+" }"+"\n";

		return result;
	}

	/** Reads an RCS model through a tokenizer.
	  */
	public void pretty_input( StreamTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "RadarCrossSection.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "A" ) )
				{
					st.nextToken();
					A = Double.parseDouble( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "B" ) )
				{
					st.nextToken();
					B = Double.parseDouble( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "C" ) )
				{
					st.nextToken();
					C = Double.parseDouble( st.sval );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD )
				{
					throw new IOException( "RadarCrossSection.pretty_input: unknown keyword: "+st.sval );
				}
				else if ( st.ttype == '}' )
				{
					found_closing_bracket = true;
					break;
				}
				else
				{
					throw new IOException( "RadarCrossSection.pretty_input: parser failure; tokenizer state: "+st );
				}
			}
		}
		catch (IOException e)
		{
			throw new IOException( "RadarCrossSection.pretty_input: attempt to read network failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "RadarCrossSection.pretty_input: no closing bracket on input." );
	}

	/** Writes this RCS model to an output stream; just a front-end
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
        throw new Exception ("RadarCrossSection.cross_validation: not implemented.");
    }
};
