package regression;

public class SquashingNetTest extends SquashingNetwork
{
	public void compute_dEdw_finite_difference( double[] input, double[] target )
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

	public void compute_dFdx_finite_difference( double[] x )
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

	SquashingNetTest( int nin, int nhidden, int nout )
	{
		super( nin, nhidden, nout );
	}

	SquashingNetTest() { super(); }

	public static void main( String args[] )
	{
		int nin;
		int nhid;
		int nout;
		int i;

		SquashingNetTest net;

		if ( "-f".equals(args[0]) )
		{
			net = new SquashingNetTest();
			try { net.pretty_input( System.in ); }
			catch (java.io.IOException e)
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

		try { net.pretty_output( System.out, " " ); }
		catch (java.io.IOException e) { System.out.println( "exception: "+e ); }
	}
}
