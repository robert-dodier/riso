package regression;

import java.io.*;
import java.util.*;

/** Java doesn't support the notion of a pointer to a function, so...
  */
interface FunctionCaller
{
	public double call_function( double x );
	public double call_derivative( double y );
}

class CallTanh implements FunctionCaller
{
	public double call_function( double x ) { double ex = Math.exp(x); return (ex-1/ex)/(ex+1/ex); }
	public double call_derivative( double y ) { return 1-y*y; }
}

class CallSigmoid implements FunctionCaller
{
	public double call_function( double x ) { return 1/(1+Math.exp(-x)); }
	public double call_derivative( double y ) { return y*(1-y); }
}

class CallLinear implements FunctionCaller
{
	public double call_function( double x ) { return x; }
	public double call_derivative( double y ) { return 1; }
}

/** Squashing network, a.k.a. multilayer perceptron.
  */
public class SquashingNetwork implements RegressionModel, Cloneable, Serializable
{
	public final int LINEAR_OUTPUT = 1;
	public final int SHORTCUTS = 2;
	public final int BATCH_UPDATE = 4;
	public final int SIGMOIDAL = 8;

	protected boolean is_ok;		// was net created successfully?
	protected int flags;		// flags for whistles and bells
	protected int nlayers;	// how many layers in net
	protected int[] unit_count;	// how many units in each layer
	protected boolean[][] is_connected;	// tell what layer connects to what
	protected double[][] activity;		// activations, 1 row per layer
	protected double[][] bias;		// biases, 1 row per layer
	protected double[][][][] weights;	// weights, 1 matrix per layer pair
	protected double[][] delta;        // deltas, 1 row per layer

	protected int nwts = 0;	// total # wts and biases -- helpful summary info

	FunctionCaller activation_function;	// this includes the derivation function

	/** Construct an empty network; parameters are read from a file.
	  */
	public SquashingNetwork() { is_ok = false; 	}

	/** Construct a network with one squashing hidden layer and a linear
	  * output layer. If <code>nhidden</code> is zero, the network is
	  * a multiple linear regression model.
	  */
	public SquashingNetwork( int ninputs, int nhidden, int noutputs )
	{
		is_ok = false;
		flags = LINEAR_OUTPUT;
		activation_function = new CallTanh();

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
		}

		allocate_weights_etc();
		is_ok = true;
	}


	protected void allocate_weights_etc()
	{
		int i, j;

		activity = new double[nlayers][];
		bias = new double[nlayers][];
		delta = new double[nlayers][];

		for ( i = 0; i < nlayers; i++ )
		{
			activity[i] = new double[ unit_count[i] ];
			bias[i] = new double[ unit_count[i] ];
			delta[i] = new double[ unit_count[i] ];

			set_random_bias( bias[i] );
		}

		weights = new double[nlayers][nlayers][][];
		for ( i = 0; i < nlayers; i++ )
			for ( j = 0; j < nlayers; j++ )
				if ( is_connected[i][j] )
				{
					weights[i][j] = new double[ unit_count[i] ][ unit_count[j] ];
					set_random_weights( weights[i][j] );
				}
				else
					weights[i][j] = null;
	}


	void set_random_bias( double[] bias_vector )
	{
		int i, n = bias_vector.length;
		Random random = new Random();

		for ( i = 0; i < n; i++ )
			bias_vector[i] = random.nextGaussian()/1e4;
	}

	
	void set_random_weights( double[][] weight_matrix )
	{
		int i, j, m = weight_matrix.length, n = weight_matrix[0].length;
		Random random = new Random();

		for ( i = 0; i < m; i++ )
			for ( j = 0; j < n; j++ )
				weight_matrix[i][j] = random.nextGaussian()/1e4;
	}


	/** Compute the network's output at <code>x</code>.
	  * @see RegressionModel.F
	  */
	public double[] F( double[] x )
	{
		for ( int i = 0; i < unit_count[0]; i++ )
			activity[0][i] = x[i];
	
		// Gather net inputs to each unit.
		// Then put net input through a squashing function.
		// Skip gathering for the 0'th (input) layer.

		for ( int to_layer = 1; to_layer < nlayers; to_layer++ )
		{
			double[] b = bias[to_layer];
			double[] a2 = activity[to_layer];
			for ( int i = 0; i < unit_count[to_layer]; i++ )
			{
				double	netin = b[i];
				for ( int from_layer = 0; from_layer < nlayers; from_layer++ )
				{
					double[][] W = weights[to_layer][from_layer];
					if ( W == null )
						continue;

					double[] a1 = activity[from_layer];
					for ( int j = 0; j < unit_count[from_layer]; j++ )
						netin += a1[j] * W[i][j];
				}

				if ( (flags & LINEAR_OUTPUT) != 0 )
					a2[i] = to_layer == nlayers-1 ? netin : activation_function.call_function( netin );
				else
					a2[i] = activation_function.call_function( netin );
			}
		}

		return (double[]) activity[nlayers-1];
	}

	/** Compute the Jacobian of the network at <code>x</code>.
	  * @see RegressionModel.dFdx
	  */
	public double[][] dFdx( double[] x )
	{
		int	nin = unit_count[0], nout = unit_count[nlayers-1];
		int	i, j, k, ii, jj;

		F( x );		// this sets output layer activations
		
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
					
					double[][] W = weights[i][j];
					double[][] Dyj = Dy[j];
					double[][] Dyi = Dy[i];

					for ( ii = 0; ii < unit_count[i]; ii++ )
					{
						double yprime, y = activity[i][ii];
						
						if ( i == nlayers-1 && (flags & LINEAR_OUTPUT) != 0 )
							yprime = 1;
						else
							if ( (flags & SIGMOIDAL) != 0 )
								yprime = y*(1-y);
							else
								yprime = 1 - y*y;

						for ( jj = 0; jj < nin; jj++ )
						{
							double	sum = 0;
							for ( k = 0; k < unit_count[j]; k++ )
								sum += W[ii][k] * yprime * Dyj[k][jj];
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
	  */
	public boolean update( double[][] x, double[][] y, boolean[] is_x_present, boolean[] is_y_present, int niter_max ) 
	{
		return false;
	}

	/** Read a network's architecture and weights from a human-readable file.
	  * @see RegressionModel.pretty_input
	  */
	public void pretty_input( InputStream is ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			Reader r = new BufferedReader(new InputStreamReader(is));
			StreamTokenizer st = new StreamTokenizer(r);
			st.wordChars( '$', '%' );
			st.wordChars( '?', '@' );
			st.wordChars( '[', '_' );
			st.ordinaryChar('/');
			st.slashStarComments(true);
			st.slashSlashComments(true);

			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "SquashingNetwork.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				System.out.println( "st: "+st );
				System.out.println( "st.ttype: "+st.ttype );
				System.out.println( "st.sval: "+st.sval );
				System.out.println( "st.nval: "+st.nval );

				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "linear_output" ) )
				{
					st.nextToken();
					flags |= (st.sval.equals("true") ? LINEAR_OUTPUT : 0);
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "shortcuts" ) )
				{
					st.nextToken();
					flags |= (st.sval.equals("true") ? SHORTCUTS : 0);
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "sigmoidal" ) )
				{
					st.nextToken();
					flags |= (st.sval.equals("true") ? SIGMOIDAL : 0);
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "nlayers" ) )
				{
					st.nextToken();
					nlayers = (int) st.nval;
					unit_count = new int[nlayers];
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "nunits" ) )
				{
					int i, j;

					for ( i = 0; i < nlayers; i++ )
					{
						st.nextToken();
						unit_count[i] = (int) st.nval;
					}

					is_connected = new boolean[nlayers][nlayers];
					for ( i = 0; i < nlayers; i++ )
						for ( j = 0; j < nlayers; j++ )
							is_connected[i][j] = ( i == j+1 || ((flags & SHORTCUTS) != 0 && j == 0 && i == nlayers-1) );

					allocate_weights_etc();
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "weights" ) )
					pretty_input_weights( st );
				else if ( st.ttype == '}' )
				{
					System.out.println( "found the freaking closing bracket." );
					found_closing_bracket = true;
					break;
				}
			}
		}
		catch (IOException e)
		{
			throw new IOException( "SquashingNetwork.pretty_input: attempt to read network failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "SquashingNetwork.pretty_input: no closing bracket on input." );

		if ( (flags & SIGMOIDAL) != 0 )
			activation_function = new CallSigmoid();
		else
			activation_function = new CallTanh();

		is_ok = true;
	}


	protected void pretty_input_weights( StreamTokenizer st ) throws IOException
	{
		System.out.println( "pretty_input_weights: nlayers: "+nlayers );
		System.out.println( "nunits in: "+unit_count[0]+"  out: "+unit_count[nlayers-1] );

		for ( int to_layer = 0; to_layer < nlayers; to_layer++ )
		{
			for ( int from_layer = 0; from_layer < nlayers; from_layer++ )
			{
				double[][] W = weights[to_layer][from_layer];
				if ( W == null )
					continue;
				double[] b = bias[to_layer];

				for ( int i = 0; i < unit_count[to_layer]; i++ )
				{
					st.nextToken();
					b[i] = st.nval;

					for ( int j = 0; j < unit_count[from_layer]; j++ )
					{
						st.nextToken();
						W[i][j] = st.nval;
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
		System.out.println( "hello from SquashingNetwork.pretty_output..." );

		if ( !OK() ) 
			throw new IOException( "SquashingNetwork.pretty_output: attempt to write a network before it is set up." );

		System.out.println( "looks like we're OK..." );

		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.println( leading_ws+this.getClass().getName()+"\n"+leading_ws+"{" );
		String more_leading_ws = leading_ws+"\t";

		dest.println( more_leading_ws+"linear_output "+((flags & LINEAR_OUTPUT)!=0) );
		dest.println( more_leading_ws+"shortcuts "+((flags & SHORTCUTS)!=0) );
		dest.println( more_leading_ws+"sigmoidal "+((flags & SIGMOIDAL)!=0) );
		dest.println( more_leading_ws+"nlayers "+nlayers );
		dest.print( more_leading_ws+"nunits " );
		for ( int i = 0; i < nlayers; i++ ) dest.print( unit_count[i]+" " );
		dest.println("\n"+more_leading_ws+"weights");
		pretty_output_weights( dest, more_leading_ws );

		dest.println( leading_ws+"}" );
	}


	/** Print out the weights of the network. Each block corresponds to one
	  * layer-to-layer connection. Each row contains to the weights leading
	  * into a unit in the ``to'' layer. The unit's bias is the first number
	  * on the line; the weights follow. In case a layer has connections from
	  * more than one other layer, the block corresponding to the earlier 
	  * layer (closer to the input) comes first.
	  */
	void pretty_output_weights( PrintStream dest, String leading_ws )
	{
		for ( int to_layer = 0; to_layer < nlayers; to_layer++ )
		{
			for ( int from_layer = 0; from_layer < nlayers; from_layer++ )
			{
				double[][] W = weights[to_layer][from_layer];
				if ( W == null )
					continue;

				dest.println( leading_ws+"/* from layer["+from_layer+"] to layer["+to_layer+"] */" );

				double[] b = bias[to_layer];
				for ( int i = 0; i < unit_count[to_layer]; i++ )
				{
					dest.print( leading_ws+b[i]+" " );
					for ( int j = 0; j < unit_count[from_layer]; j++ )
						dest.print( W[i][j]+" " );
					dest.println("");
				}
				dest.println("");
			}
		}
	}

	/** Return the number of input units of the network.
	  * @throws IllegalArgumentException If the network is not usable.
	  * @see RegressionModel.ndimensions_in
	  */
	public int ndimensions_in() 
	{
		if ( !OK() )
			throw new IllegalArgumentException( "SquashingNetwork.ndimensions_in: network is not usable." );
		return unit_count[0];
	}

	/** Return the number of output units of the network.
	  * @throws IllegalArgumentException If the network is not usable.
	  * @see RegressionModel.ndimensions_out
	  */
	public int ndimensions_out()
	{
		if ( !OK() )
			throw new IllegalArgumentException( "SquashingNetwork.ndimensions_out: network is not usable." );
		return unit_count[nlayers-1];
	}

	public void dEdw( double[] input, double[] desired_output ) {}

	/** Compute delta[i][j] == ...
	  */
	public void compute_deltas( double[] input, double[] desired_output )
	{
		F( input );			// compute activations

		double sqr_err = 0;
		for ( int i = 0; i < unit_count[nlayers-1]; i++ )
		{
			double D = desired_output[i];
			double O = activity[nlayers-1][i];
			delta[nlayers-1][i] = (D-O)*activation_function.call_derivative(O);
			sqr_err +=  (D-O)*(D-O);
		}

		for ( int from_layer = nlayers-2; from_layer > 0; from_layer-- )
		{
			for ( int to_layer = nlayers-1; to_layer > from_layer; to_layer-- )
			{
				double[][] W = weights[to_layer][from_layer];
				if ( W == null )
					continue;
				for ( int j = 0; j < unit_count[from_layer]; j++ )
				{
					double d = delta[from_layer][j];
					d = 0;
					for ( int i = 0; i < unit_count[to_layer]; i++ )
						d += W[i][j] * delta[to_layer][i];
					double O = activity[from_layer][j];
					d *= activation_function.call_derivative(O);
				}
			}
		}
	}

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

	public int get_nunits( int layer )	{ return unit_count[layer]; }
	public int get_nlayers() { return nlayers; }
}
