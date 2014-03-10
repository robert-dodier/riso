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
package riso.approximation;
import java.io.*;
import riso.distributions.*;
import riso.numerical.*;
import riso.general.*;

/** This class contains a public static method to construct a monotone spline approximation
  * to a one-dimensional unconditional distribution.
  */
public class SplineApproximation
{
	/** Constructs a monotone spline approximation to a one-dimensional unconditional distribution.
	  * A list of pairs <tt>(x, target.p(x))</tt> is constructed, and the spline constructor is called.
	  */
	public static SplineDensity do_approximation( Distribution target, double[][] supports, double[] mass ) throws Exception
	{
System.err.println( "SplineApproximation.do_approximation: need approx. to "+target.getClass() );

		// IF suppports.length > 1 SHOULD RETURN A MIXTURE, ONE SPLINE COMPONENT PER INTERVAL !!!
		if ( supports.length > 1 ) throw new Exception( "SplineDensity.do_approximation: "+supports.length+" is too many support intervals!!!" );

		double x0 = supports[0][0], x1 = supports[0][1];
		FunctionCache cached_target = new FunctionCache( (x1-x0)/1e3, -1, new DistributionCallback(target) );
		IntegralHelper1d cth = new IntegralHelper1d( cached_target, supports, false );
		if ( mass != null ) mass[0] = cth.do_integral();
		else cth.do_integral();
		
		double[][] x_px = cached_target.dump();
		double[] x = new double[ x_px.length ], px = new double[ x_px.length ];
		for ( int i = 0; i < x_px.length; i++ )
		{
			x[i] = x_px[i][0];
			px[i] = x_px[i][1];
		}

		return new SplineDensity( x, px );
	}

	/** A little test program.
	  */
	public static void main( String[] args )
	{
		System.err.println( "target file: "+args[0] );
		System.err.println( "target support: ["+args[1]+", "+args[2]+"]" );

		try
		{
			int i;
			FileInputStream fis = new FileInputStream( args[0] );
			SmarterTokenizer p_st = new SmarterTokenizer( new BufferedReader( new InputStreamReader( fis ) ) );
			Distribution p = null;

			p_st.nextToken();
			Class p_class = java.rmi.server.RMIClassLoader.loadClass( p_st.sval );
			p = (Distribution) p_class.newInstance();
			p_st.nextBlock();
			p.parse_string( p_st.sval );

			double[][] support = new double[1][2];
			support[0][0] = Double.parseDouble( args[1] );
			support[0][1] = Double.parseDouble( args[2] );

			Distribution q = SplineApproximation.do_approximation( p, support, null );

			System.out.print( "approximation:\n"+q.format_string("") );
			double x[] = new double[1];
			System.out.println( "x\tp(x)\tq(x)" );
			for ( i = 0; i < 500; i++ )
			{
				x[0] = support[0][0]+i*(support[0][1]-support[0][0])/500.0;
				System.out.println( x[0]+"\t"+p.p(x)+"\t"+q.p(x) );
			}

			System.out.println( "q.expected_value: "+q.expected_value() );
			System.out.println( "q.sqrt_variance: "+q.sqrt_variance() );
		}
		catch (Exception e)
		{
			System.err.println( "SplineApproximation.main: something went ker-blooey." );
			e.printStackTrace();
		}
	}
}

class DistributionCallback implements Callback_1d
{
	Distribution target;
	double[] x1 = new double[1];

	DistributionCallback( Distribution target )
	{
		this.target = target;
	}

	public double f( double x ) throws Exception
	{
		x1[0] = x;
		return target.p(x1);
	}
}
