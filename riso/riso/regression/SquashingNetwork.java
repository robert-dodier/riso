package riso.regression;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import numerical.*;
import SmarterTokenizer;

class CallTanh implements FunctionCaller, Cloneable
{
	public double call_function( double x )
	{
		if ( x < 0 ) return -call_function(-x); 
		if ( x > 18 ) return 1;
		double ex = Math.exp(x);
		return (ex-1/ex)/(ex+1/ex);
	}
	public double call_derivative( double y ) { return 1-y*y; }
}

class CallSigmoid implements FunctionCaller, Cloneable
{
	public double call_function( double x ) { return 1/(1+Math.exp(-x)); }
	public double call_derivative( double y ) { return y*(1-y); }
}

class CallSoftmax implements FunctionCaller, Cloneable
{
	public double call_function( double x ) { return Math.exp(x); }
	public double call_derivative( double y ) { throw new RuntimeException(); }
}

class CallLinear implements FunctionCaller, Cloneable
{
	public double call_function( double x ) { return x; }
	public double call_derivative( double y ) { return 1; }
}

/** Squashing network, a.k.a. multilayer perceptron.
  */
public class SquashingNetwork implements RegressionModel
{
	public static final int LINEAR_OUTPUT = 1;
	public static final int SHORTCUTS = 2;
	public static final int BATCH_UPDATE = 4;
	public static final int SIGMOIDAL_OUTPUT = 8;
	public static final int SOFTMAX_OUTPUT = 16;
	public int flags;				// flags for whistles and bells

	protected boolean is_ok;		// was net created successfully?
	protected int nlayers;	// how many layers in net
	protected int[] unit_count;	// how many units in each layer
	protected boolean[][] is_connected;	// tell what layer connects to what
	protected double[][] activity;		// activations, 1 row per layer
	protected double[][] delta;        // grad of error w.r.t net inputs

	protected int[][][][] weight_index;	// weights, 1 matrix per layer pair
	protected int[][] bias_index;		// biases, 1 row per layer

	protected double[] weights_unpacked;	// includes both weights and biases
	protected double[] dEdw_unpacked;	// includes both weights and biases

	protected int nwts = 0;	// total # wts and biases -- helpful summary info

	public String comment_leader = "%";

	FunctionCaller[] activation_function;	// this includes the derivative function

	public int get_nunits( int layer )	{ return unit_count[layer]; }
	public int get_nlayers() { return nlayers; }

	/** Construct an empty network; parameters are read from a file.
	  */
	public SquashingNetwork() { is_ok = false; }

	/** Construct a network with one squashing hidden layer and a linear
	  * output layer. If <code>nhidden</code> is zero, the network is
	  * a multiple linear regression model.
	  */
	public SquashingNetwork( int ninputs, int nhidden, int noutputs )
	{
		is_ok = false;
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
			activation_function[1] = new CallTanh();
			activation_function[2] = new CallLinear();
		}

		allocate_weights_etc();
		is_ok = true;
	}

	protected void allocate_weights_etc()
	{
		int i, j;

		activity = new double[nlayers][];
		delta = new double[nlayers][];
		bias_index = new int[nlayers][];

		for ( i = 0; i < nlayers; i++ )
		{
			activity[i] = new double[ unit_count[i] ];
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

		Random random = new Random();
		for ( i = 0; i < nwts; i++ )
			weights_unpacked[i] = random.nextGaussian()/1e4;
	}

	/** Compute the network's output at <code>x</code>.
	  * @see RegressionModel.F
	  */
	public double[] F( double[] x ) throws Exception
	{
		for ( int i = 0; i < unit_count[0]; i++ )
			activity[0][i] = x[i];
	
		// Gather net inputs to each unit.
		// Then put net input through a squashing function.
		// Skip gathering for the 0'th (input) layer.

		for ( int to_layer = 1; to_layer < nlayers; to_layer++ )
		{
			int[] b = bias_index[to_layer];
			double[] a2 = activity[to_layer];
			for ( int i = 0; i < unit_count[to_layer]; i++ )
			{
				double	netin = weights_unpacked[ b[i] ];
				for ( int from_layer = 0; from_layer < nlayers; from_layer++ )
				{
					int[][] w = weight_index[to_layer][from_layer];
					if ( w == null )
						continue;

					double[] a1 = activity[from_layer];
					for ( int j = 0; j < unit_count[from_layer]; j++ )
						netin += a1[j] * weights_unpacked[ w[i][j] ];
				}

				a2[i] = activation_function[to_layer].call_function( netin );
			}
		}

		if ( (flags & SOFTMAX_OUTPUT) != 0 )	// carry out normalization
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
						double yprime, y = activity[i][ii];
						yprime = activation_function[i].call_derivative(y);

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

		result += more_leading_ws+"linear-output "+((flags & LINEAR_OUTPUT)!=0)+"\n";
		result += more_leading_ws+"shortcuts "+((flags & SHORTCUTS)!=0)+"\n";
		result += more_leading_ws+"sigmoidal-output "+((flags & SIGMOIDAL_OUTPUT)!=0)+"\n";
		result += more_leading_ws+"softmax-output "+((flags & SOFTMAX_OUTPUT)!=0)+"\n";
		result += more_leading_ws+"nlayers "+nlayers+"\n";
		result += more_leading_ws+"nunits "+"\n";
		for ( int i = 0; i < nlayers; i++ ) result += unit_count[i]+" ";
		result += "\n"+more_leading_ws+"weights"+"\n";
		result += format_weights_string( more_leading_ws );

		result += leading_ws+"}"+"\n";
		return result;
	}

	/** Read a network's architecture and weights from a human-readable file.
	  * @see RegressionModel.pretty_input
	  */
	public void pretty_input( StreamTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "SquashingNetwork.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "linear-output" ) )
				{
					st.nextToken();
					flags |= (st.sval.equals("true") ? LINEAR_OUTPUT : 0);
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "shortcuts" ) )
				{
					st.nextToken();
					flags |= (st.sval.equals("true") ? SHORTCUTS : 0);
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
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "nlayers" ) )
				{
					st.nextToken();
					nlayers = Format.atoi( st.sval );
					unit_count = new int[nlayers];
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "nunits" ) )
				{
					int i, j;

					for ( i = 0; i < nlayers; i++ )
					{
						st.nextToken();
						unit_count[i] = Format.atoi( st.sval );
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
			throw new IOException( "SquashingNetwork.pretty_input: no closing bracket on input." );

		activation_function = new FunctionCaller[nlayers];

		for ( int i = 1; i < nlayers-1; i++ )
			activation_function[i] = new CallTanh();
		
		if ( (flags & LINEAR_OUTPUT) != 0 )
			activation_function[nlayers-1] = new CallLinear();
		else if ( (flags & SIGMOIDAL_OUTPUT) != 0 )
			activation_function[nlayers-1] = new CallSigmoid();
		else if ( (flags & SOFTMAX_OUTPUT) != 0 )
			activation_function[nlayers-1] = new CallSoftmax();
		else
			activation_function[nlayers-1] = new CallTanh();

		is_ok = true;
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
					weights_unpacked[ b[i] ] = Format.atof( st.sval );

					for ( int j = 0; j < unit_count[from_layer]; j++ )
					{
						st.nextToken();
						weights_unpacked[ w[i][j] ] = Format.atof( st.sval );
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
			double O = activity[nlayers-1][i];
			delta[nlayers-1][i] =
				-2*(D-O)*activation_function[nlayers-1].call_derivative(O);
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
					double O = activity[from_layer][j];
					delta[from_layer][j] *= activation_function[from_layer].call_derivative(O);
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



	/** Return the "OK" flag. IS THIS REALLY NEEDED ??? IT'S NOT
	  * CONSISTENTLY USED, PERHAPS DROP IT ???
	  */
	public boolean OK() { return is_ok; }

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
	public Object remote_clone() throws CloneNotSupportedException
	{
		int i, j, k;
		SquashingNetwork copy;

		copy = new SquashingNetwork();

		copy.flags = flags;
		copy.nlayers = nlayers;
		copy.unit_count = (int[]) unit_count.clone();

		copy.is_connected = (boolean[][]) is_connected.clone();
		for ( i = 0; i < is_connected.length; i++ )
			copy.is_connected[i] = (boolean[]) is_connected[i].clone();

		copy.activity = Matrix.copy( activity );
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
		copy.is_ok = is_ok;

		return copy;
	}
}
