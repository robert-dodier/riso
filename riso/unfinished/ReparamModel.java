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
import numerical.*;

public class ReparamModel extends SquashingNetwork
{
	public double lambda_in = 10;
	public double lambda_out = 0.1;
	public double[] params = null;

	public ReparamModel() { super(); }

	public static double sqr( double x ) { return x*x; }

	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		for ( int i = 0; i <= unit_count[0]; i++ )
			dest.println( leading_ws+"% a["+i+"]: "+params[2*i]+"  b["+i+"]: "+params[2*i+1] );

		super.pretty_output( os, leading_ws );
	}

	public double update( double[][] x, double[][] y, int niter_max, double stopping_criterion, double[] responsibility ) throws Exception
	{
		if ( niter_max == -1 ) niter_max = 100;
		if ( stopping_criterion == -1 ) stopping_criterion = 0.001;

		int nin = unit_count[0];
		int i, nparams = 2*nin+2, m = 5;

		double[] diag = new double[nparams];
		int[] iprint = new int[2];
		iprint[0] = 25;
		iprint[1] = 0;

		boolean diagco = false;
		double xtol = 1e-15;

		params = new double[nparams];
		double[] dEdparams = new double[nparams];

		for ( i = 0; i < nin+1; i++ )
		{
			// params[2*i] = 1 + (Math.random() - 0.5)/100;
			// params[2*i+1] = (Math.random() - 0.5)/100;
			params[2*i] = 1;
			params[2*i+1] = 0;
		}

		double WMSE = compute_error_and_gradient( x, y, params, dEdparams, responsibility );
		System.err.println( "ReparamModel.update: before: WMSE: "+WMSE );

		int[] iflag = new int[1];
		iflag[0] = 0;
		int nfeval;

		for ( nfeval = 0; nfeval < niter_max && (nfeval == 0 || iflag[0] != 0); nfeval = LBFGS.nfevaluations() )
		{
			WMSE = compute_error_and_gradient( x, y, params, dEdparams, responsibility );

			try
			{
				LBFGS.lbfgs( nparams, m, params, WMSE, dEdparams, diagco, diag, iprint, stopping_criterion, xtol, iflag );
			}
			catch (LBFGS.ExceptionWithIflag e)
			{
				System.err.println( "ReparamModel.update: lbfgs failed; use most recent line search result.\n"+e );
				break;
			}
		}

		if ( nfeval > 0 )
		{
			// LBFGS was indeed called and computed something.
			System.arraycopy( LBFGS.solution_cache, 0, params, 0, params.length );
			WMSE = compute_error_and_gradient( x, y, params, dEdparams, responsibility );
			System.err.println( "ReparamModel.update: after: WMSE: "+WMSE );
			incorporate_scaling( params );
		}

		return WMSE;
	}

	void incorporate_scaling( double[] params )
	{
		// First unpack the parameter vector into scaling and translation.
		// Then incorporate new scaling into neural network weights.

		int i, nin = unit_count[0];

		double[] in_xlate = new double[nin];
		double[] in_scale = new double[nin];
		double[] out_xlate = new double[1];
		double[] out_scale = new double[1];

		for ( i = 0; i < nin; i++ )
		{
			in_scale[i] = params[2*i];
			in_xlate[i] = params[2*i+1];
		}

		out_scale[0] = params[2*nin];
		out_xlate[0] = params[2*nin+1];

		super.incorporate_scaling( in_xlate, in_scale, out_xlate, out_scale );
	}

	/** This function computes the total error and gradient of the
	  * total error w.r.t. the scaling and translation parameters.
	  * The error and its gradient are weighted by ``responsibility''
	  * values; if not supplied, these values are all assumed to be 1.
	  */
	double compute_error_and_gradient( double[][] x, double[][] y, double[] params, double[] dEdparams, double[] h ) throws IllegalArgumentException
	{
		int i, j, nin = unit_count[0], nout = unit_count[nlayers-1];

		if ( nout != 1 )
			throw new IllegalArgumentException( "ReparamModel.compute_error_and_gradient: nout: "+nout+" != 1." );
		if ( nin != x[0].length || nout != y[0].length )
			throw new IllegalArgumentException( "ReparamModel.compute_error_and_gradient: data doesn't match size of network;\n\tnin: "+nin+", nout: "+nout+", x[0].length: "+x[0].length+", y[0].length: "+y[0].length );

		for ( i = 0; i < dEdparams.length; i++ )
			dEdparams[i] = 0;

		double[] t = new double[nin];
		double responsibility, sum_responsibility = 0, SSE = 0;

		for ( i = 0; i < x.length; i++ )
		{
			for ( j = 0; j < nin; j++ )
			{
				t[j] = x[i][j] * params[2*j] + params[2*j+1];
			}

			double FF = 0;
			double[] gradF = null;

			try { FF = F(t)[0]; gradF = dFdx(t)[0]; }
			catch (Exception e)
			{
				System.err.println( "unexpected: "+e );
				System.exit(1);
			}

			double ypred = FF * params[2*nin] + params[2*nin+1];
			double err = ypred - y[i][0];

			if ( h == null )
				responsibility = 1;
			else
				responsibility = h[i];

			sum_responsibility += responsibility;
			SSE += responsibility*err*err;

			for ( j = 0; j < nin; j++ )
			{
				dEdparams[2*j] += responsibility * 2 * err * params[2*nin] * gradF[j] * x[i][j];
				dEdparams[2*j+1] += responsibility * 2 * err * params[2*nin] * gradF[j];
			}

			dEdparams[2*nin] += responsibility * 2 * err * FF;
			dEdparams[2*nin+1] += responsibility * 2 * err;
		}

		double WMSE = SSE/sum_responsibility;
		for ( j = 0; j < dEdparams.length; j++ )
			dEdparams[j] /= sum_responsibility;

		double penalty = 0;
		for ( i = 0; i < nin; i++ )
		{
			penalty += lambda_in * ( sqr( params[2*i] - 1 ) + sqr( params[2*i+1] - 0 ) );
			dEdparams[2*i] += 2 * lambda_in * (params[2*i] - 1);
			dEdparams[2*i+1] += 2 * lambda_in * (params[2*i+1] - 0);
		}

		penalty += lambda_out * ( sqr( params[2*nin] - 1 ) + sqr( params[2*nin+1] - 0 ) );
		dEdparams[2*nin] += 2 * lambda_out * (params[2*nin] - 1);
		dEdparams[2*nin+1] += 2 * lambda_out * (params[2*nin+1] - 0);

		return WMSE+penalty;
	}
}
