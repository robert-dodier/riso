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
import numerical.Matrix;
import SmarterTokenizer;

public class SquashingNetTest extends SquashingNetwork
{
	public void compute_dEdw_finite_difference( double[] input, double[] target ) throws Exception
	{
		int i;

		compute_dEdw( input, target );
		double[] computed_gradient = (double[]) dEdw_unpacked.clone();
		double[] est_gradient = new double[ nweights() ];

		double EPS = 1e-6;
		double error = OutputError( input, target );

		for ( i = 0; i < nwts; i++ )
		{
			double save_w = weights_unpacked[i];
			weights_unpacked[i] += EPS;
			double modified_error = OutputError( input, target );
			est_gradient[i] = (modified_error-error)/EPS;
			weights_unpacked[i] = save_w;
		}

		System.out.println( "computed_gradient, est_gradient:" );
		for ( i = 0; i < nwts; i++ )
			System.out.println( computed_gradient[i]+"\t"+est_gradient[i] );

		for ( i = 1; i < nlayers; i++ )
			for ( int j = 0; j < unit_count[i]; j++ )
				System.out.println( "delta["+i+"]["+j+"] == "+delta[i][j] );
	}

	public void compute_dFdx_finite_difference( double[] x ) throws Exception
	{
		int i, j;

		double[][] computed_dFdx = dFdx(x);
		double[][] est_dFdx = new double[unit_count[nlayers-1]][unit_count[0]];

		double EPS = 1e-6;
		double[] output = F(x);
		for ( i = 0; i < unit_count[0]; i++ )
		{
			double save_x = x[i];
			x[i] += EPS;
			double[] modified_output = F(x);
			for ( j = 0; j < unit_count[nlayers-1]; j++ )
				est_dFdx[j][i] = (modified_output[j]-output[j])/EPS;
			x[i] = save_x;
		}

		System.out.println( "computed_dFdx:" );
		for ( i = 0; i < computed_dFdx.length; i++ )
		{
			for ( j = 0; j < computed_dFdx[i].length; j++ )
				System.out.print( computed_dFdx[i][j]+" " );
			System.out.println("");
		}

		System.out.println( "est_dFdx:" );
		for ( i = 0; i < est_dFdx.length; i++ )
		{
			for ( j = 0; j < est_dFdx[i].length; j++ )
				System.out.print( est_dFdx[i][j]+" " );
			System.out.println("");
		}
	}

	SquashingNetTest( int nin, int nhidden, int nout ) throws Exception
	{
		super( nin, nhidden, nout );
	}

	SquashingNetTest() { super(); }

	public static void main( String args[] )
	{
		int nin;
		int nhid;
		int nout;
		int i, j;

		SquashingNetTest net;

		try
		{
			if ( "-f".equals(args[0]) )
			{
				net = new SquashingNetTest();
				try
				{
					Reader r = new InputStreamReader( System.in );
					SmarterTokenizer st = new SmarterTokenizer( r );
					net.pretty_input( st );
				}
				catch (IOException e)
				{
					System.err.println( "exception: "+e );
					return;
				}

				nin = net.ndimensions_in();
				nout = net.ndimensions_out();
			}
			else
			{
				nin = Integer.parseInt(args[0]);
				nhid = Integer.parseInt(args[1]);
				nout = Integer.parseInt(args[2]);
				net = new SquashingNetTest( nin, nhid, nout );
				for ( i = 0; i < net.nweights(); i++ )
					net.weights_unpacked[i] = (double) i;
			}

			double[] x = new double[nin], y = new double[nout];
			for ( i = 0; i < nin; i++ ) x[i] = 0.1 + i/10.0;
			for ( i = 0; i < nout; i++ ) y[i] = -1 -i/10.0;

			net.compute_dFdx_finite_difference( x );
			net.compute_dEdw_finite_difference( x, y );

			try { net.pretty_output( System.out, "" ); }
			catch (IOException e) { System.out.println( "exception: "+e ); }

			try
			{
				SquashingNetwork copy = (SquashingNetwork) ((SquashingNetwork)net).clone();
				copy.pretty_output( System.out, "" );
			}
			catch (Exception e)
			{
				System.out.println( "problem w/ clone: "+e );
				e.printStackTrace();
				return;
			}

			double[][] xx = null, yy = null;
			int ndata = 0;

			Reader r = new BufferedReader(new InputStreamReader(System.in));
			StreamTokenizer st = new StreamTokenizer(r);

			try
			{
				st.nextToken();
				ndata = (int) st.nval;
				xx = new double[ndata][nin];
				yy = new double[ndata][nout];

				for ( i = 0; i < ndata; i++ )
				{
					for ( j = 0; j < nin; j++ )
					{
						st.nextToken();
						xx[i][j] = st.nval;
					}

					for ( j = 0; j < nout; j++ )
					{
						st.nextToken();
						yy[i][j] = st.nval;
					}
				}
			}
			catch (IOException e) { System.out.println( "exception: "+e ); }

			java.util.Random random = new java.util.Random();
			for ( i = 0; i < net.nweights(); i++ )
				net.weights_unpacked[i] = random.nextGaussian()/1e4;

			try { net.update( xx, yy, 50, 1e-5, null ); }
			catch (Exception e) { System.out.println( "exception: "+e ); }

			try { net.pretty_output( System.out, "" ); }
			catch (IOException e) { System.out.println( "exception: "+e ); }

			for ( i = 0; i < ndata; i++ )
			{
				System.out.print( "in: " );
				Matrix.pretty_output( xx[i], System.out, " " );
				System.out.print( "target: " );
				Matrix.pretty_output( yy[i], System.out, " " );
				System.out.print( "pred: " );
				Matrix.pretty_output( net.F(xx[i]), System.out, " " );
				System.out.println("");
			}
		}
		catch (Exception e)
		{
			System.err.println( "exception: "+e );
			System.exit(1);
		}
	}
}
