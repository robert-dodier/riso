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
import java.util.*;
import riso.numerical.*;
import riso.general.*;

class CallSin implements FunctionCaller, Cloneable, Serializable
{
	public double call_function( double x ) { return Math.sin(x); }
	public double call_derivative( double x ) { return Math.cos(x); }
}

class CallCos implements FunctionCaller, Cloneable, Serializable
{
	public double call_function( double x ) { return Math.cos(x); }
	public double call_derivative( double x ) { return -Math.sin(x); }
}

class CallTanh implements FunctionCaller, Cloneable, Serializable
{
	public double call_function( double x )
	{
		if ( x < 0 ) return -call_function(-x); 
		if ( x > 18 ) return 1;
		double ex = Math.exp(x);
		return (ex-1/ex)/(ex+1/ex);
	}
	public double call_derivative( double x ) { double y = call_function(x); return 1-y*y; }
}

class CallSigmoid implements FunctionCaller, Cloneable, Serializable
{
	public double call_function( double x ) { return 1/(1+Math.exp(-x)); }
	public double call_derivative( double x ) { double y = call_function(x); return y*(1-y); }
}

class CallSoftmax implements FunctionCaller, Cloneable, Serializable
{
	public double call_function( double x ) { return Math.exp(x); }
	public double call_derivative( double x ) { throw new RuntimeException(); }
}

class CallLinear implements FunctionCaller, Cloneable, Serializable
{
	public double call_function( double x ) { return x; }
	public double call_derivative( double x ) { return 1; }
}

/** Squashing network, a.k.a. multilayer perceptron.
  */
public class SquashingNetwork implements RegressionModel, Serializable
{
	public static final int LINEAR_OUTPUT = 0x1;
	public static final int SHORTCUTS = 0x2;
	public static final int BATCH_UPDATE = 0x4;
	public static final int SIGMOIDAL_OUTPUT = 0x8;
	public static final int SOFTMAX_OUTPUT = 0x10;
	public static final int SIGMOIDAL_HIDDEN = 0x20;
	public static final int SIN_OUTPUT = 0x40;
	public static final int COS_OUTPUT = 0x80;

    public boolean normalize_random_weights = false;
    public Random rng = new Random();

	public int flags;				// flags for whistles and bells
	public String activation_spec;	// list of activation functions, one for each layer. OVERRIDES FLAGS !!! SHOULD REMOVE FLAGS !!!

	protected int nlayers;	// how many layers in net
	protected int[] unit_count;	// how many units in each layer
	protected boolean[][] is_connected;	// tell what layer connects to what
	public double[][] activity;		// activation of each unit, 1 row per layer
	public double[][] netin;		// net input for each unit, 1 row per layer
	public double[][] delta;        // grad of error w.r.t net inputs

	protected int[][][][] weight_index;	// weights, 1 matrix per layer pair
	protected int[][] bias_index;		// biases, 1 row per layer

	protected double[] weights_unpacked;	// includes both weights and biases
	protected double[] dEdw_unpacked;	// includes both weights and biases

	protected int nwts = 0;	// total # wts and biases -- helpful summary info

	public String comment_leader = "%";

	public FunctionCaller[] activation_function;	// this includes the derivative function

	public int get_nunits( int layer )	{ return unit_count[layer]; }
	public int get_nlayers() { return nlayers; }

	/** Construct an empty network; parameters are read from a file.
	  */
	public SquashingNetwork() {}

	/** Construct a network with one squashing hidden layer and a linear
	  * output layer. If <code>nhidden</code> is zero, the network is
	  * a multiple linear regression model.
	  */
	public SquashingNetwork( int ninputs, int nhidden, int noutputs )
	{
		flags = LINEAR_OUTPUT;

		if ( nhidden == 0 )
		{
			nlayers = 2;
			unit_count = new int[ nlayers ];
			unit_count[0] = ninputs;
			unit_count[1] = noutputs;

			is_connected = new boolean[nlayers][nlayers];
			
			is_connected[0][0] = false;
			is_connected[1][0] = true;
			is_connected[0][1] = false;
			is_connected[1][1] = false;

			activation_function = new FunctionCaller[2];
			activation_function[0] = new CallLinear();
			activation_function[1] = new CallLinear();
		}
		else
		{
			nlayers = 3;
			unit_count = new int[ nlayers ];
			unit_count[0] = ninputs;
			unit_count[1] = nhidden;
			unit_count[2] = noutputs;

			is_connected = new boolean[nlayers][nlayers];
			for ( int to = 0; to < nlayers; to++ )
				for ( int from = 0; from < nlayers; from++ )
					is_connected[to][from] = false;
			
			is_connected[1][0] = true;
			is_connected[2][1] = true;

			activation_function = new FunctionCaller[3];
			activation_function[0] = new CallLinear();
			activation_function[1] = new CallTanh();
			activation_function[2] = new CallLinear();
		}

		allocate_weights_etc();
	}

	protected void allocate_weights_etc()
	{
		int i, j;

		activity = new double[nlayers][];
		netin = new double[nlayers][];
		delta = new double[nlayers][];
		bias_index = new int[nlayers][];

		for ( i = 0; i < nlayers; i++ )
		{
			activity[i] = new double[ unit_count[i] ];
			netin[i] = new double[ unit_count[i] ];
			delta[i] = new double[ unit_count[i] ];

			bias_index[i] = new int[ unit_count[i] ];
		}

		weight_index = new int[nlayers][nlayers][][];
		for ( i = 0; i < nlayers; i++ )
			for ( j = 0; j < nlayers; j++ )
				if ( is_connected[i][j] )
					weight_index[i][j] = new int[ unit_count[i] ][ unit_count[j] ];
				else
					weight_index[i][j] = null;

		assign_indices();

		weights_unpacked = new double[ nweights() ];
		dEdw_unpacked = new double[ nwts ];

        randomize_weights();
    }

    /** Assign random weights to this squashing network.
      * The weights are normalized such that the sum of the absolute
      * values of the weights leading into a hidden or output unit
      * is equal to 2.
      */
    public void randomize_weights()
    {
		for ( int i = 0; i < nweights(); i++ )
			weights_unpacked[i] = rng.nextGaussian()/1e4;

        if ( ! normalize_random_weights ) return;

		for ( int to_layer = 1; to_layer < nlayers; to_layer++ )
		{
			int[] b = bias_index[to_layer];

			for ( int i = 0; i < unit_count[to_layer]; i++ )
			{
				double sum_abs = Math.abs( weights_unpacked[ b[i] ] );

				for ( int from_layer = 0; from_layer < nlayers; from_layer++ )
				{
					int[][] w = weight_index[to_layer][from_layer];
					if ( w == null )
						continue;

					for ( int j = 0; j < unit_count[from_layer]; j++ )
						sum_abs += Math.abs( weights_unpacked[ w[i][j] ] );
				}

				weights_unpacked[ b[i] ] *= 1/sum_abs;

				for ( int from_layer = 0; from_layer < nlayers; from_layer++ )
				{
					int[][] w = weight_index[to_layer][from_layer];
					if ( w == null )
						continue;

					for ( int j = 0; j < unit_count[from_layer]; j++ )
						weights_unpacked[ w[i][j] ] *= 2/sum_abs;
				}
			}
		}
	}

	/** Compute the network's output at <code>x</code>.
	  * @see RegressionModel.F
	  */
	public double[] F( double[] x ) throws Exception
	{
		for ( int i = 0; i < unit_count[0]; i++ )
			activity[0][i] = netin[0][i] = x[i];
	
		// Gather net inputs to each unit.
		// Then put net input through a squashing function.
		// Skip gathering for the 0'th (input) layer.

		for ( int to_layer = 1; to_layer < nlayers; to_layer++ )
		{
			int[] b = bias_index[to_layer];
			double[] a2 = activity[to_layer], n2 = netin[to_layer];
			for ( int i = 0; i < unit_count[to_layer]; i++ )
			{
				n2[i] = weights_unpacked[ b[i] ];
				for ( int from_layer = 0; from_layer < nlayers; from_layer++ )
				{
					int[][] w = weight_index[to_layer][from_layer];
					if ( w == null )
						continue;

					double[] a1 = activity[from_layer];
					for ( int j = 0; j < unit_count[from_layer]; j++ )
						n2[i] += a1[j] * weights_unpacked[ w[i][j] ];
				}

				a2[i] = activation_function[to_layer].call_function( n2[i] );
			}
		}

		if ( (flags & SOFTMAX_OUTPUT) != 0 )	// carry out normalization; ADJUST netin ???
		{
			double sum = 0;
			for ( int i = 0; i < activity[nlayers-1].length; i++ )
				sum += activity[nlayers-1][i];
			for ( int i = 0; i < activity[nlayers-1].length; i++ )
				activity[nlayers-1][i] /= sum;
		}

		return (double[]) activity[nlayers-1].clone();
	}

	/** Compute the derivative of the output of a network w.r.t. its inputs,
	  * evaluated at a specified input. A matrix is returned; this matrix has
	  * #outputs rows, and #inputs columns, and the (i,j) entry is dy_i/dx_j,
	  * where y is the output and x is the input.
	  * @see RegressionModel.dFdx
	  * @param x Point at which to evaluate derivative.
	  */
	public double[][] dFdx( double[] x ) throws Exception
	{
		int	nin = unit_count[0], nout = unit_count[nlayers-1];
		int	i, j, k, ii, jj;

		F(x);		// this sets output layer activations
		
		double[][][] Dy = new double[nlayers][][];
		
		// Dy_0 == (dx/dx) == I.

		Dy[0] = new double[nin][nin];
		double[][] pd0 = Dy[0];
		
		for ( i = 0; i < nin; i++ )
			for ( j = 0; j < nin; j++ )
				pd0[i][j] = ( i == j ? 1 : 0 );

		// Work toward output layer, composing jacobians as we go.

		for ( i = 1; i < nlayers; i++ )
		{
			// Compute Dy_i.
			// In general, y_i = y_i( y_{i-1}, y_{i-2},... y_1, x ).

			Dy[i] = new double[unit_count[i]][nin];

			for ( j = i-1; j >= 0; j-- )
				if ( is_connected[i][j] )
				{
					// Compute (dy_i/dy_j), and multiply by (dy_j/dx) at
					// the same time.
					
					int[][] w = weight_index[i][j];
					double[][] Dyj = Dy[j];
					double[][] Dyi = Dy[i];

					for ( ii = 0; ii < unit_count[i]; ii++ )
					{
						double yprime, net = netin[i][ii];
						yprime = activation_function[i].call_derivative(net);

						for ( jj = 0; jj < nin; jj++ )
						{
							double	sum = 0;
							for ( k = 0; k < unit_count[j]; k++ )
								sum += weights_unpacked[ w[ii][k] ] * yprime * Dyj[k][jj];
							Dyi[ii][jj] += sum;
						}
					}
				}
		}

		// Now Dy[nlayers-1] is (dy/dx) for output y and input x.

		return Dy[nlayers-1];
	}

	/** Update the network's parameters by minimizing the sum of squared
	  * errors of prediction.
	  * @see RegressionModel.update
	  * @throws Exception If the LBFGS code fails.
	  */
	public double update( double[][] x, double[][] y, int niter_max, double stopping_criterion, double[] responsibility ) throws Exception
	{
		if ( responsibility != null )
			throw new Exception( "SquashingNetwork.update: don't know how to deal with responsibility yet." );

		int ndata = x.length;
		int	m = 5;		// m is #recent updates to keep for LBFGS

		double[] diag = new double[ nweights() ];

		int[] iprint = new int[2];
		iprint[0] = 1;						// give output on every iteration
		iprint[1] = 0;

		boolean diagco = false;
		double eps = stopping_criterion;
		double xtol = 1e-16;				// double precision machine epsilon
		int[] iflag = new int[1];
		iflag[0] = 0;
		int icall;

		System.err.println( "SquashingNetwork.update: before: MSE == "+OutputError(x,y)/ndata );

		for ( icall = 0; icall < niter_max && (icall == 0 || iflag[0] != 0); ++icall )
		{
			// Compute SSE and gradient of SSE w.r.t. weights.

			int j;
			for ( j = 0; j < nwts; j++ )
				dEdw_unpacked[j] = 0;
			double MSE = 0;
			for ( j = 0; j < ndata; j++ )
			{
				double sqr_error = compute_dEdw( x[j], y[j] );
				MSE += sqr_error;
			}

			// Now fudge the error and gradient so that we get MSE and
			// the gradient is gradient of MSE (not SSE) w.r.t weights.

			MSE /= ndata;
			for ( j = 0; j < nwts; j++ )
				dEdw_unpacked[j] /= ndata;

			try
			{
				LBFGS.lbfgs( nwts, m, weights_unpacked, MSE, dEdw_unpacked, diagco, diag, iprint, eps, xtol, iflag);
			}
			catch (LBFGS.ExceptionWithIflag e)
			{
				throw new Exception( "SquashingNetwork: update() failed with exception:\n"+e );
			}
		}

		double final_mse = OutputError(x,y)/ndata;
		System.err.println( "SquashingNetwork.update: at end of training, MSE == "+final_mse );

		return final_mse;
	}

    /** Carry out cross validation on this squashing network.
      */
    public output_pair[] cross_validation( double[][] x, double[][] y, int nfolds, int niter_max, double stopping_criterion, double[] responsibility ) throws Exception
    {
        if ( responsibility != null )
            throw new IllegalArgumentException( "SquashingNetwork.cross_validation: responsibility argument is nonnull; too lazy to handle this." );

        int n = x.length;

        int[] perm = new int[n];
        for ( int i = 0; i < n; i++ )
            perm[i] = i;

        for ( int i = 1; i < perm.length; i++ )
        {
            int j = (rng.nextInt() & 0x7fffffff) % (i+1);
            int t = perm[i];
            perm[i] = perm[j];
            perm[j] = t;
        }

        int n_per_fold = n/nfolds;

        output_pair[] output = new output_pair[n];
        for ( int i = 0; i < n; i++ )
            output[i] = new output_pair();

        for ( int m = 0; m < nfolds; m++ )
        {
            int i0 = m*n_per_fold;
            int i1 = m+1 == nfolds ? n : (m+1)*n_per_fold;

            int ntest = i1 -i0;
            int ntrain = n - ntest;

            double[][] x_train = new double[ntrain][];
            double[][] y_train = new double[ntrain][];
            double[][] x_test = new double[ntest][];
            double[][] y_test = new double[ntest][];

            for ( int i = 0; i < i0; i++ )
            {
                x_train[i] = x[ perm[i] ];
                y_train[i] = y[ perm[i] ];
            }

            for ( int i = i0; i < i1; i++ )
            {
                x_test[i-i0] = x[ perm[i] ];
                y_test[i-i0] = y[ perm[i] ];
            }

            for ( int i = i1; i < n; i++ )
            {
                x_train[i0+(i-i1)] = x[ perm[i] ];
                y_train[i0+(i-i1)] = y[ perm[i] ];
            }

            randomize_weights();    // CLOBBER EXISTING WEIGHTS !!!
            update( x_train, y_train, niter_max, stopping_criterion, null );

            for ( int i = 0; i < ntest; i++ )
            {
                output[ perm[i+i0] ].output = F( x_test[i] );
                output[ perm[i+i0] ].target = y_test[i];
            }
        }

        return output;
    }

	/** Parse a string containing a description of a squashing network.
	  * The description is contained within curly braces, which are
	  * included in the string.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Create a description of this regression model as a string.
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

result += more_leading_ws+"% activations, java types: ";
for ( int i = 0; i < nlayers; i++ ) try { result += activation_function[i].getClass().getName()+" "; } catch (Exception e) { result += "act["+i+"]: "+e+" "; }
result += "\n";
		if ( activation_spec == null )
		{
			result += more_leading_ws+"linear-output "+((flags & LINEAR_OUTPUT)!=0)+"\n";
			result += more_leading_ws+"cos-output "+((flags & COS_OUTPUT)!=0)+"\n";
			result += more_leading_ws+"sin-output "+((flags & SIN_OUTPUT)!=0)+"\n";
			result += more_leading_ws+"sigmoidal-output "+((flags & SIGMOIDAL_OUTPUT)!=0)+"\n";
			result += more_leading_ws+"sigmoidal-hidden "+((flags & SIGMOIDAL_HIDDEN)!=0)+"\n";
			result += more_leading_ws+"softmax-output "+((flags & SOFTMAX_OUTPUT)!=0)+"\n";
		}
		else 
		{
			result += more_leading_ws+"activations "+activation_spec+"\n";
		}

        result += more_leading_ws+"normalize-random-weights "+normalize_random_weights+"\n";
		result += more_leading_ws+"shortcuts "+((flags & SHORTCUTS)!=0)+"\n";
		result += more_leading_ws+"nlayers "+nlayers+"\n";
		result += more_leading_ws+"nunits ";
		for ( int i = 0; i < nlayers; i++ ) result += unit_count[i]+" ";
		result += "\n"+more_leading_ws+"weights"+"\n";
		result += format_weights_string( more_leading_ws );

		result += leading_ws+"}"+"\n";
		return result;
	}

	/** Read a network's architecture and weights from a human-readable file.
	  * @see RegressionModel.pretty_input
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "SquashingNetwork.pretty_input: input doesn't have opening bracket; tokenizer state: "+st );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "linear-output" ) )
				{
					st.nextToken();
					flags |= (st.sval.equals("true") ? LINEAR_OUTPUT : 0);
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "cos-output" ) )
				{
					st.nextToken();
					flags |= (st.sval.equals("true") ? COS_OUTPUT : 0);
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "sin-output" ) )
				{
					st.nextToken();
					flags |= (st.sval.equals("true") ? SIN_OUTPUT : 0);
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "shortcuts" ) )
				{
					st.nextToken();
					flags |= (st.sval.equals("true") ? SHORTCUTS : 0);
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "sigmoidal-hidden" ) )
				{
					st.nextToken();
					flags |= (st.sval.equals("true") ? SIGMOIDAL_HIDDEN : 0);
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "sigmoidal-output" ) )
				{
					st.nextToken();
					flags |= (st.sval.equals("true") ? SIGMOIDAL_OUTPUT : 0);
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "softmax-output" ) )
				{
					st.nextToken();
					flags |= (st.sval.equals("true") ? SOFTMAX_OUTPUT : 0);
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "activations" ) )
				{
					st.nextBlock();
					activation_spec = st.sval; // will be parsed later
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "normalize-random-weights" ) )
				{
					st.nextToken();
					normalize_random_weights = st.sval.equals("true");
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "nlayers" ) )
				{
					st.nextToken();
					nlayers = Integer.parseInt( st.sval );
					unit_count = new int[nlayers];
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "nunits" ) )
				{
					int i, j;

					for ( i = 0; i < nlayers; i++ )
					{
						st.nextToken();
						unit_count[i] = Integer.parseInt( st.sval );
					}

					is_connected = new boolean[nlayers][nlayers];
					for ( i = 0; i < nlayers; i++ )
						for ( j = 0; j < nlayers; j++ )
							is_connected[i][j] = ( i == j+1 || ((flags & SHORTCUTS) != 0 && j == 0 && i == nlayers-1) );

					allocate_weights_etc();
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "weights" ) )
					pretty_input_weights( st );
				else if ( st.ttype == StreamTokenizer.TT_WORD )
				{
					throw new IOException( "SquashingNetwork.pretty_input: unknown keyword: "+st.sval );
				}
				else if ( st.ttype == '}' )
				{
					found_closing_bracket = true;
					break;
				}
				else
				{
					throw new IOException( "SquashingNetwork.pretty_input: parser failure; tokenizer state: "+st );
				}
			}
		}
		catch (IOException e)
		{
			throw new IOException( "SquashingNetwork.pretty_input: attempt to read network failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "SquashingNetwork.pretty_input: no closing bracket on input; tokenizer state: "+st );

		activation_function = new FunctionCaller[nlayers];
		activation_function[0] = new CallLinear();

		if ( activation_spec == null )
		{
			// Use flags to figure out which activation function to use for each layer.
			for ( int i = 1; i < nlayers-1; i++ )
				if ( (flags & SIGMOIDAL_HIDDEN) != 0 )
					activation_function[i] = new CallSigmoid();
				else
					activation_function[i] = new CallTanh();
			
			if ( (flags & LINEAR_OUTPUT) != 0 )
				activation_function[nlayers-1] = new CallLinear();
			else if ( (flags & SIGMOIDAL_OUTPUT) != 0 )
				activation_function[nlayers-1] = new CallSigmoid();
			else if ( (flags & SOFTMAX_OUTPUT) != 0 )
				activation_function[nlayers-1] = new CallSoftmax();
			else if ( (flags & COS_OUTPUT) != 0 )
				activation_function[nlayers-1] = new CallCos();
			else if ( (flags & SIN_OUTPUT) != 0 )
				activation_function[nlayers-1] = new CallSin();
			else
				activation_function[nlayers-1] = new CallTanh();
		}
		else
		{
			// activation_spec contains a list of function names, one for each layer.

			try
			{
				SmarterTokenizer st2 = new SmarterTokenizer( new StringReader( activation_spec ) );
				st2.nextToken(); // eat left curly brace
				for ( int i = 0; i < nlayers; i++ )
				{
					st2.nextToken();
					if ( "linear".equals( st2.sval ) ) activation_function[i] = new CallLinear();
					else if ( "sigmoid".equals(st2.sval) || "sigmoidal".equals(st2.sval) ) activation_function[i] = new CallSigmoid();
					else if ( "tanh".equals( st2.sval ) ) activation_function[i] = new CallTanh();
					else if ( "cos".equals( st2.sval ) ) activation_function[i] = new CallCos();
					else if ( "sin".equals( st2.sval ) ) activation_function[i] = new CallSin();
					else if ( "softmax".equals( st2.sval ) ) { activation_function[i] = new CallSoftmax(); flags |= SOFTMAX_OUTPUT; } // need to remember to normalize output units
					else { System.err.println( "SquashingNetwork.pretty_input: what is "+st2.sval+" activation function?" ); } // will die later; OK.
				}
			}
			catch (IOException e)
			{
				throw new IOException( "SquashingNetwork.pretty_input: attempt to parse activation list failed:\n"+e );
			}
		}
	}

	protected void pretty_input_weights( StreamTokenizer st ) throws IOException
	{
		for ( int to_layer = 0; to_layer < nlayers; to_layer++ )
		{
			for ( int from_layer = 0; from_layer < nlayers; from_layer++ )
			{
				int[][] w = weight_index[to_layer][from_layer];
				if ( w == null )
					continue;
				int[] b = bias_index[to_layer];

				for ( int i = 0; i < unit_count[to_layer]; i++ )
				{
					st.nextToken();
					weights_unpacked[ b[i] ] = Double.parseDouble( st.sval );

					for ( int j = 0; j < unit_count[from_layer]; j++ )
					{
						st.nextToken();
						weights_unpacked[ w[i][j] ] = Double.parseDouble( st.sval );
					}
				}
			}
		}
	}

	/** Write a network's architecture and weights to a file in a human-
	  * readable format.
	  * @see RegressionModel.pretty_output
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	/** Format the weights of the network. Each block corresponds to one
	  * layer-to-layer connection. Each row contains to the weights leading
	  * into a unit in the ``to'' layer. The unit's bias is the first number
	  * on the line; the weights follow. In case a layer has connections from
	  * more than one other layer, the block corresponding to the earlier 
	  * layer (closer to the input) comes first.
	  */
	String format_weights_string( String leading_ws )
	{
		String result = "";

		for ( int to_layer = 0; to_layer < nlayers; to_layer++ )
		{
			for ( int from_layer = 0; from_layer < nlayers; from_layer++ )
			{
				int[][] w = weight_index[to_layer][from_layer];
				if ( w == null )
					continue;

				result += leading_ws+comment_leader+" from layer["+from_layer+"] to layer["+to_layer+"]"+"\n";

				int[] b = bias_index[to_layer];
				for ( int i = 0; i < unit_count[to_layer]; i++ )
				{
					result += leading_ws+weights_unpacked[b[i]]+" ";
					for ( int j = 0; j < unit_count[from_layer]; j++ )
						result += weights_unpacked[w[i][j]]+" ";
					result += "\n";
				}
				result += "\n";
			}
		}
		
		return result;
	}

	/** Return the number of input units of the network.
	  * @throws IllegalArgumentException If the network is not usable.
	  * @see RegressionModel.ndimensions_in
	  */
	public int ndimensions_in()
	{
		return unit_count[0];
	}

	/** Return the number of output units of the network.
	  * @throws IllegalArgumentException If the network is not usable.
	  * @see RegressionModel.ndimensions_out
	  */
	public int ndimensions_out()
	{
		return unit_count[nlayers-1];
	}

	/** Compute the gradient of the net input of the output unit w.r.t.
	  * the weights. This is computed from <tt>da/dw ==
	  * (dE/dw)/(dE/do do/da)</tt>. <tt>dE/do</tt> is <tt>-2(y-F(x))</tt>
	  * for <tt>o == F(x)</tt>, and <tt>do/da</tt> is given by the
	  * <tt>call_derivative</tt> method of the output unit's activation
	  * function. </p>
	  * 
	  * <p> The gradient is computed at the current weight vector and the
	  * given input vector <tt>x</tt>. </p>
	  *
	  * <p> <tt>dEdw_unpacked</tt> is stomped.
	  */
	public double[] compute_dadw( double[] x ) throws Exception
	{
		if ( ndimensions_out() > 1 ) throw new Exception( "SquashingNetwork.compute_dadw: works only for one output." );

		for ( int i = 0; i < dEdw_unpacked.length; i++ ) dEdw_unpacked[i] = 0;

		double[] y = F(x);
		y[0] += 1; // y can be anything, so long as it's not F(x).

		compute_dEdw( x, y );

		FunctionCaller a = activation_function[nlayers-1];
		for ( int i = 0; i < dEdw_unpacked.length; i++ )
			// dE/do == -2(y-F(x)) == -2; do/da given by call_derivative.
			dEdw_unpacked[i] /= -2 * a.call_derivative(netin[nlayers-1][0]);

		return (double[]) dEdw_unpacked.clone();
	}

	/** Compute the gradient of the output w.r.t. the weights.
	  * This is computed from <tt>dF/dw == dE/dw /(-2(y-F(x)))</tt>.
	  * The gradient is computed at the current weight vector and the
	  * given input vector <tt>x</tt>. </p>
	  *
	  * <p> <tt>dEdw_unpacked</tt> is stomped.
	  */
	public double[] compute_dFdw( double[] x ) throws Exception
	{
		if ( ndimensions_out() > 1 ) throw new Exception( "SquashingNetwork.compute_dFdw: works only for one output." );

		for ( int i = 0; i < dEdw_unpacked.length; i++ ) dEdw_unpacked[i] = 0;

		double[] y = F(x);
		y[0] += 1; // y can be anything, so long as it's not F(x).

		compute_dEdw( x, y );
		for ( int i = 0; i < dEdw_unpacked.length; i++ ) dEdw_unpacked[i] /= -2; // since y-F(x) == 1

		return (double[]) dEdw_unpacked.clone();
	}

	/** Compute the gradient of the output error w.r.t. the weights. 
	  * The output error is the squared error <code>||target - F(input)||^2</code>,
	  * not one-half the squared error.
	  * @return The output error for the given input/target pair.
	  */
	public double compute_dEdw( double[] input, double[] target ) throws Exception
	{
		double sqr_err = compute_deltas( input, target );

		for ( int to_layer = 1; to_layer < nlayers; to_layer++ )
		{
			int[] b = bias_index[to_layer];
			for ( int i = 0; i < unit_count[to_layer]; i++ )
				dEdw_unpacked[ b[i] ] += delta[to_layer][i] * 1;

			for ( int from_layer = 0; from_layer < nlayers; from_layer++ )
			{
				int[][] w = weight_index[to_layer][from_layer];
				if ( w == null )
					continue;

				for ( int i = 0; i < unit_count[to_layer]; i++ )
				{
					for ( int j = 0; j < unit_count[from_layer]; j++ )
					{
						dEdw_unpacked[ w[i][j] ] += delta[to_layer][i] * activity[from_layer][j];
					}
				}
			}
		}

		return sqr_err;
	}

	/** For each unit <code>j</code> in each layer <code>i</code>, compute
	  * <code>delta[i][j]</code> == d(output error)/d(unit ij net input).
	  * The output error is the squared error <code>||target - F(input)||^2</code>,
	  * not one-half the squared error.
	  * @return The output error for the given input/target pair.
	  */
	public double compute_deltas( double[] input, double[] target ) throws Exception
	{
		F( input );			// compute activations

		double sqr_err = 0;
		for ( int i = 0; i < unit_count[nlayers-1]; i++ )
		{
			double D = target[i];
			double O = activity[nlayers-1][i], net = netin[nlayers-1][i];
			delta[nlayers-1][i] =
				-2*(D-O)*activation_function[nlayers-1].call_derivative(net);
			sqr_err +=  (D-O)*(D-O);
		}

		for ( int from_layer = nlayers-2; from_layer > 0; from_layer-- )
		{
			for ( int to_layer = nlayers-1; to_layer > from_layer; to_layer-- )
			{
				int[][] w = weight_index[to_layer][from_layer];
				if ( w == null )
					continue;
				for ( int j = 0; j < unit_count[from_layer]; j++ )
				{
					delta[from_layer][j] = 0;
					for ( int i = 0; i < unit_count[to_layer]; i++ )
						delta[from_layer][j] += weights_unpacked[w[i][j]] * delta[to_layer][i];
					double O = activity[from_layer][j], net = netin[from_layer][j];
					delta[from_layer][j] *= activation_function[from_layer].call_derivative(net);
				}
			}
		}

		return sqr_err;
	}

	/** Modify the weights from the input to first hidden layer
	  * to account for input scaling and translation, and the weights
	  * from the last hidden layer to the output to account for
	  * output scaling and translation. This does work correctly
	  * if there are no hidden layers. Each input and output is
	  * transformed according to <tt>s*x+t</tt> where <tt>x</tt>
	  * denotes the input or output variable, <tt>s</tt> is the
	  * scaling factor, and <tt>t</tt> is the translation. <p>
	  *
	  * If one is fixing up the weights to account for a transformation which
	  * subtracts the mean <tt>m</tt> and divides by the standard
	  * deviation <tt>sd</tt>, then the scaling factor is <tt>1/sd</tt>
	  * and the translation is <tt>-m/sd</tt> on the inputs, and on the outputs
	  * the scaling is <tt>sd</tt> and the translation is <tt>m</tt>. <p>
	  *
	  * @param in_xlate An array with number of elements equal to the number
	  *   of inputs, containing the constant for translation of each input.
	  * @param in_scale An array with number of elements equal to the number
	  *   of inputs, containing the multiplicative constant for scaling each input.
	  * @param out_xlate An array with number of elements equal to the number
	  *   of outputs, containing the constant for translation of each output.
	  * @param out_scale An array with number of elements equal to the number
	  *   of outputs, containing the multiplicative constant for scaling each output.
	  * @throws IllegalArgumentException If this network has shortcuts.
	  */
	public void incorporate_scaling( double[] in_xlate, double[] in_scale, double[] out_xlate, double[] out_scale )
	{
		int nin = unit_count[0], nout = unit_count[nlayers-1];
		
		if ( (flags & SHORTCUTS) != 0 )
			throw new IllegalArgumentException( "SquashingNetwork.incorporate_scaling: can't handle shortcuts." );

		int nhidden1 = unit_count[1], nhidden2 = unit_count[nlayers-2];

		int[] hidden1_bias_index = bias_index[1];
		int[] out_bias_index = bias_index[nlayers-1];
		int[][] in_wts_index = weight_index[1][0];
		int[][] out_wts_index = weight_index[nlayers-1][nlayers-2];

		double[][] in_wts_scaled = new double[nhidden1][nin];
		double[][] out_wts_scaled = new double[nout][nhidden2];
		double[] hidden1_bias_scaled = new double[nhidden1], out_bias_scaled = new double[nout];

		int	i, j;

		// Calculate new input->hidden weights.

		for ( i = 0; i < nhidden1; i++ )
		{
			double sum = 0;
			for ( j = 0; j < nin; j++ )
			{
				in_wts_scaled[i][j] = weights_unpacked[ in_wts_index[i][j] ] * in_scale[j];
				sum += weights_unpacked[ in_wts_index[i][j] ] * in_xlate[j];
			}
			hidden1_bias_scaled[i] = weights_unpacked[ hidden1_bias_index[i] ] + sum;
		}

		// Update input->hidden weights. If there is no hidden layer, this actually
		// changes the input->output weights.

		for ( i = 0; i < nhidden1; i++ )
		{
			weights_unpacked[ hidden1_bias_index[i] ] = hidden1_bias_scaled[i];
			for ( j = 0; j < nin; j++ )
				weights_unpacked[ in_wts_index[i][j] ] = in_wts_scaled[i][j];
		}

		// Calculate new hidden->output weights. If there is no hidden layer, this further
		// modifies the input->output weights.

		for ( i = 0; i < nout; i++ )
		{
			out_bias_scaled[i] = weights_unpacked[ out_bias_index[i] ] * out_scale[i] + out_xlate[i];
			for ( j = 0; j < nhidden2; j++ )
				out_wts_scaled[i][j] = weights_unpacked[ out_wts_index[i][j] ] * out_scale[i];
		}

		// Update hidden->output weights.

		for ( i = 0; i < nout; i++ )
		{
			weights_unpacked[ out_bias_index[i] ] = out_bias_scaled[i];
			for ( j = 0; j < nhidden2; j++ )
				weights_unpacked[ out_wts_index[i][j] ] = out_wts_scaled[i][j];
		}
	}

	/** Return the number of weights and biases in this network.
	  */
	public int nweights()
	{
		if ( nwts > 0 )
			return nwts;

		// 1st time through, carry out computation.

		int	i, j, sum = 0;
		for ( i = 0; i < nlayers; i++ )
			for ( j = 1; j < nlayers; j++ )
				if ( is_connected[j][i] )
					// Count connections.
					sum += unit_count[i]*unit_count[j];

		for ( i = 1; i < nlayers; i++ )
			// Count biases.
			sum += unit_count[i];

		nwts = sum;
		return nwts;
	}

	protected void assign_indices()
	{
		int i, j, k, l, m = 0;

		for ( i = 1; i < nlayers; i++ )
			for ( j = 0; j < unit_count[i]; j++ )
				bias_index[i][j] = m++;
		
		for ( i = 0; i < nlayers; i++ )
			for ( j = 0; j < nlayers; j++ )
				if ( is_connected[i][j] )
					for ( k = 0; k < unit_count[i]; k++ )
						for ( l = 0; l < unit_count[j]; l++ )
							weight_index[i][j][k][l] = m++;

		if ( m != nweights() )	// !!!
			throw new Error( "PANIC: m != nwts in assign_indices()" );
	}

	/** Compute the squared error for one input/target pair. 
	  * Note that even if there is only one output, <code>target</code>
	  * must still be an vector -- in this case, with just one element.
	  * @param input Vector of inputs.
	  * @param target Vector of target outputs.
	  * @return <code>||target - F(input)||^2</code>.
	  */
	public double OutputError( double[] input, double[] target ) throws Exception
	{
		int i, nout = unit_count[nlayers-1];
		double sqr_err = 0, output[] = activity[nlayers-1];

		F(input);
		for ( i = 0; i < nout; i++ )
		{
			double err = target[i] - output[i];
			sqr_err += err*err;
		}

		return sqr_err;
	}

	/** Compute the sum of squared errors for the given list of inputs
	  * and targets. This function calls <code>OutputError(double[],double[])
	  * </code> for each row of <code>inputs</code> and <code>targets</code>,
	  * and adds up the errors.
	  */
	public double OutputError( double[][] inputs, double[][] targets ) throws Exception
	{
		double sqr_err = 0;
		for ( int i = 0; i < inputs.length; i++ )
			sqr_err += OutputError( inputs[i], targets[i] );
		
		return sqr_err;
	}

	/** Make a deep copy of this squashing network and return it.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		int i, j, k;
		SquashingNetwork copy;

		try { copy = (SquashingNetwork) this.getClass().newInstance(); }
		catch (Exception e) { throw new CloneNotSupportedException( this.getClass().getName()+": clone failed; "+e ); }

		copy.flags = flags;
		copy.nlayers = nlayers;
		copy.unit_count = (int[]) unit_count.clone();

		copy.is_connected = (boolean[][]) is_connected.clone();
		for ( i = 0; i < is_connected.length; i++ )
			copy.is_connected[i] = (boolean[]) is_connected[i].clone();

		copy.activity = Matrix.copy( activity );
		copy.netin = Matrix.copy( netin );
		copy.delta = Matrix.copy( delta );

		copy.weight_index = (int[][][][]) weight_index.clone();
		for ( i = 0; i < weight_index.length; i++ )
		{
			copy.weight_index[i] = (int[][][]) weight_index[i].clone();
			for ( j = 0; j < weight_index[i].length; j++ )
			{
				if ( weight_index[i][j] != null )
				{
					copy.weight_index[i][j] = (int[][]) weight_index[i][j].clone();

					for ( k = 0; k < weight_index[i][j].length; k++ )
						copy.weight_index[i][j][k] = (int[]) weight_index[i][j][k].clone();
				}
			}
		}
					
		copy.bias_index = (int[][]) bias_index.clone();
		for ( i = 0; i < bias_index.length; i++ )
			copy.bias_index[i] = (int[]) bias_index[i].clone();

		copy.weights_unpacked = (double[]) weights_unpacked.clone();
		copy.dEdw_unpacked = (double[]) dEdw_unpacked.clone();

		copy.nwts = nwts;
		copy.activation_function = (FunctionCaller[]) activation_function.clone();

		return copy;
	}

	/** Reads a squashing network description from a file, then reads stdin for inputs and
	  * computes outputs.
	  */
	public static void main( String[] args )
	{
		boolean do_update = false, do_cv = false, binary_input = false, do_normalize = false;
		int ndata = -1, nfolds = 4, seed = 0;
        double eps = 1e-4;

		String filename = "";
		for ( int i = 0; i < args.length; i++ )
		{
			switch ( args[i].charAt(1) )
			{
            case 'b':
                binary_input = true;
                break;
            case 'c':
                do_cv = true;
                break;
            case 'e':
                eps = Double.parseDouble( args[++i] );
				break;
			case 'f':
				filename = args[++i];
				break;
            case 'm':
                nfolds = Integer.parseInt( args[++i] );
                break;
			case 'n':
				ndata = Integer.parseInt( args[++i] );
				break;
            case 's':
                seed = Integer.parseInt( args[++i] ); 
                break;
			case 'u':
				do_update = true;
				break;
            case 'z':
                do_normalize = true;
                break;
			}
		}

        System.err.println( "SquashingNetwork.main: filename: "+filename+", do_cv: "+do_cv+", do_update: "+do_update+", binary_input: "+binary_input+", do_normalize: "+do_normalize+", nfolds: "+nfolds+", ndata: "+ndata+", eps: "+eps+", seed: "+seed );

		try
		{
			Reader r = new InputStreamReader( new FileInputStream( filename ) );
			SmarterTokenizer st = new SmarterTokenizer(r);
			st.nextToken();
			SquashingNetwork net = (SquashingNetwork) Class.forName(st.sval).newInstance();
            if (seed != 0) net.rng = new Random(seed);
			net.pretty_input(st);
			
            DataInputStream is = null;
            if ( binary_input )
                is = new DataInputStream (System.in);
            else
            {
			    r = new InputStreamReader( System.in );
			    st = new SmarterTokenizer(r);
            }

			int nin = net.ndimensions_in(), nout = net.ndimensions_out();
			double[][] X = new double[ndata][nin], Y = new double[ndata][nout];

			for ( int i = 0; i < ndata; i++ )
			{
				for ( int j = 0; j < nin; j++ )
                    if ( binary_input )
                    {
                        X[i][j] = is.readDouble();
                    }
                    else
                    {
                        st.nextToken();
                        X[i][j] = Double.parseDouble( st.sval );
                    }
				
				for ( int j = 0; j < nout; j++ )
                    if ( binary_input )
                    {
                        Y[i][j] = is.readDouble();
                    }
                    else
                    {
                        st.nextToken();
                        Y[i][j] = Double.parseDouble( st.sval );
                    }

                if ( ndata % 10000 == 0 )
                    System.err.println( "SquashingNetwork.main: read "+i+" records so far." );
			}

            if ( do_normalize )
                normalize (X);

            boolean is_rescaled = net.maybe_rescale_output( Y );

            if ( do_cv )
            {
                output_pair[] output = net.cross_validation( X, Y, nfolds, 1000, eps, null );

                for ( int i = 0; i < output.length; i++ )
                {
                    Matrix.pretty_output( output[i].output, System.out, " " );
                    Matrix.pretty_output( output[i].target, System.out, " " );
                    System.out.print("\n");
                }
            }
            else 
            {
                if ( do_update )
                {
                    net.update( X, Y, 1000, eps, null );
                    net.pretty_output( new FileOutputStream(filename), "\t" );
                }
                
                for ( int i = 0; i < ndata; i++ )
                {
                    double[] yhat = net.F( X[i] );
                    for ( int j = 0; j < yhat.length; j++ )
                        System.err.print( " "+ yhat[j] );
                    System.err.println("");
                }
            }
		}
		catch (Exception e) { e.printStackTrace(); }
	}

    static void normalize( double[][] X )
    {
        if ( X.length == 0 ) return;

        int m = X[0].length, n = X.length;
        double[] sum = new double [m];
        double[] sum2 = new double [m];
        double[] mean = new double [m];
        double[] sd = new double [m];

        for ( int i = 0; i < n; i++ )
            for ( int j = 0; j < m; j++ )
            {
                sum[j] += X[i][j];
                sum2[j] += X[i][j]*X[i][j];
            }

        for ( int j = 0; j < m; j++ )
        {
            mean[j] = sum[j]/n;
            double var = sum2[j]/n - mean[j]*mean[j];
            if ( var > 0 )
                sd[j] = Math.sqrt(var);
            else
                sd[j] = 0;

            System.err.println( "mean["+j+"]: "+mean[j]+", sd["+j+"]: "+sd[j] );
        }

        for ( int i = 0; i < n; i++ )
            for ( int j = 0; j < m; j++ )
                if ( sd[j] > 0 )
                    X[i][j] = (X[i][j] - mean[j])/sd[j];
    }

    boolean maybe_rescale_output( double[][] Y )
    {
        if ( activation_function[ nlayers-1 ] instanceof CallTanh )
        {
            double min_y = 1e40, max_y = -1e40;

            for ( int i = 0; i < Y.length; i++ )
                for ( int j = 0; j < Y[i].length; j++ )
                {
                    if ( Y[i][j] < min_y ) min_y = Y[i][j];
                    if ( Y[i][j] > max_y ) max_y = Y[i][j];
                }

            if ( 0 <= min_y && max_y <= 1 )
            {
                System.err.println( "SquashingNetwork.maybe_rescale_output: min_y, max_y: "+min_y+", "+max_y+" and tanh output; rescale [0,1] to [-1,1]" );
                for ( int i = 0; i < Y.length; i++ )
                    for ( int j = 0; j < Y[i].length; j++ )
                        Y[i][j] = 2*Y[i][j] -1;
                
                return true;
            }
        }

        return false;
    }
}
