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
  * amplitude, phase shift, and period. The parameters are parent variables:
  * first time, then offset*2, then triples of cosine multiplier, sine multiplier, and
  * period.
  */
public class HarmonicWithParametersAsParents implements RegressionModel
{
	/** Number of terms in harmonic series.
	  */
	public int m;

	double sqr( double x ) { return x*x; }

	/** Do-nothing construct, so <tt>Class.forName</tt> works.
	  */
	public HarmonicWithParametersAsParents() {}

	/** Make a deep copy of this object and return a reference to the copy.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		HarmonicWithParametersAsParents copy = new HarmonicWithParametersAsParents();
		return copy;
	}

	/** Returns the output of the sum of harmonics at the specified input.
	  * @param x Should be an array with 2 + 3N elements.
	  * @return Sum of components of this model.
	  */
	public double[] F( double[] x ) 
	{
		double[] sum = new double[1];
		double t = x[0];

		sum[0] = x[1]/2;
		for ( int i = 2; i < x.length; i += 3 )
			sum[0] += x[i] * Math.cos( 2*Math.PI*t/x[i+2] ) + x[i+1] * Math.sin( 2*Math.PI*t/x[i+2] );

		return sum;
	}

	/** Return the Jacobian matrix (i.e., matrix of partial derivatives) 
	  * of the regression function w.r.t. the input.
	  * @param x Input point; should be a 1-element array.
	  * @return Jacobian matrix at <code>x</code>; this will be a 1-by-1 matrix.
	  */
	public double[][] dFdx( double[] x ) 
	{
		double[][] dF = new double[1][x.length];

		double t = x[0];
		
		for ( int i = 2; i < x.length; i += 3 )
		{
			dF[0][0] +=  -x[i]*Math.sin( 2*Math.PI*t/x[i+2] ) *  2*Math.PI/x[i+2];
			dF[0][0] += x[i+1]*Math.cos( 2*Math.PI*t/x[i+2] ) *  2*Math.PI/x[i+2];
		}

		dF[0][1] = 1/2.0;

		for ( int i = 2; i < x.length; i += 3 )
		{
			dF[0][i]   = Math.cos( 2*Math.PI*t/x[i+2] );
			dF[0][i+1] = Math.sin( 2*Math.PI*t/x[i+2] );
			dF[0][i+2]  =    x[i]*Math.sin( 2*Math.PI*t/x[i+2] ) * 2*Math.PI*t/sqr(x[i+2]);
			dF[0][i+2] += -x[i+1]*Math.cos( 2*Math.PI*t/x[i+2] ) * 2*Math.PI*t/sqr(x[i+2]);
		}

		return dF;
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
		throw new Exception( "HarmonicWithParametersAsParents.update: not supported." );
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
		return this.getClass().getName()+" { m "+m+" }\n";
	}

	/** Format is <tt>{ m [integer] }</tt>.
	  */
	public void pretty_input( StreamTokenizer st ) throws IOException
	{
		st.nextToken(); // eat open brace
		st.nextToken(); // eat m
		st.nextToken();
		m = Integer.parseInt(st.sval);
		st.nextToken(); // eat close brace
	}

	/** Writes this harmonic model to an output stream; just a front-end
	  * for <tt>format_string</tt> (q.v.).
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	public int ndimensions_in() { return 2+3*m; }

	public int ndimensions_out() { return 1; }

    public output_pair[] cross_validation( double[][] x, double[][] y, int nfolds, int niter_max, double stopping_criterion, double[] responsibility ) throws Exception
    {
        throw new Exception ("HarmonicWithParametersAsParents.cross_validation: not implemented.");
    }
}
