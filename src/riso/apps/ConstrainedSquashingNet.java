package riso.apps;
import java.io.*;
import riso.regression.*;
import riso.numerical.*;
import riso.general.*;

public class ConstrainedSquashingNet extends SquashingNetwork
{
	public double epsilon = 1e-2;
	public double beta = 10;
	public String filename = "";
	public double constraint_misclass_rate = 0.5;

	public double sqr( double x ) { return x*x; }
	public double sigma( double x ) { return 1/(1+Math.exp(-x)); }

	/** Compute the correct acceptance (CA) rate.
	  * CA is defined as the proportion of positive examples for which the output is greater than the threshold. 
	  * This method computes a smoothed version --- use the sigmoid function instead of the step function.
	  */
	double compute_CA( double t, double[][] xp ) throws Exception
	{
		double sum = 0;

		for ( int i = 0; i < xp.length; i++ )
			sum += sigma( beta*( F(xp[i])[0] - t ) );

		return sum/xp.length;
	}

	/** Compute the correct rejection (CR) rate.
	  * CR equals the proportion of negative examples for which the output is less than the threshold. 
	  * This method computes a smoothed version --- use the sigmoid function instead of the step function.
	  */
	double compute_CR( double t, double[][] xn ) throws Exception
	{
		double sum = 0;

		for ( int i = 0; i < xn.length; i++ )
			sum += 1 - sigma( beta*( F(xn[i])[0] - t ) );

		return sum/xn.length;
	}

	public double find_threshold( double[][] xp, double constraint_CA ) throws Exception
	{
		// Find a threshold s.t. CA equals the specified acceptance rate.
		// Do binary search.

		double t0 = 0, t1 = 1, t_mid = 0.5;
		double CA0 = 1, CA1 = 0;

		while ( t1 - t0 > 0.00001 )
		{
			t_mid = t0 + (t1-t0)/2;
			double CA_mid = compute_CA( t_mid, xp );
			double diff0 = CA0 - constraint_CA, diff1 = CA1 - constraint_CA, diff_mid = CA_mid - constraint_CA;
			if ( diff0*diff_mid < 0 ) 
			{
				// t0 and t_mid are on different sides of the solution.
				t1 = t_mid;
				CA1 = CA_mid;
			}
			else
			{
				t0 = t_mid;
				CA0 = CA_mid;
			}
		}
		
System.err.println( "final t: "+t_mid );
		return t_mid;
	}

	/** Use gradient descent to minimize error on the training set,
	  * subject to the constraint function.
	  */
	public double update( double t, double[][] xp, double[][] yp, double[][] xn, double[][] yn, int niter_max ) throws Exception
	{
		double[] t_w = new double[1+nwts];
		double[] gr = new double[1+nwts], ga = new double[1+nwts], na = new double[1+nwts];
		int niter = 0;

		// Copy t and w into one vector; it makes the dot product stuff a little simpler.

		t_w[0] = t;
		for ( int i = 0; i < weights_unpacked.length; i++ ) t_w[i+1] = weights_unpacked[i];

		// Now do constrained gradient descent on the training data.
		
		long last_save = System.currentTimeMillis()/1000;

		do
		{
			// Compute CR and gradient of CR w.r.t. weights and threshold.

			gr[0] = compute_dCRdt( xn, yn, t );
			double[] dCRdw = compute_dCRdw( xn, yn, t );
			for ( int i = 0; i < dCRdw.length; i++ ) gr[i+1] = dCRdw[i];

			// Compute gradient of the constraint function.
			// Normalize the constraint function gradient.

			ga[0] = compute_dCAdt( xp, yp, t );
			double[] dCAdw = compute_dCAdw( xp, yp, t );
			for ( int i = 0; i < dCAdw.length; i++ ) ga[i+1] = dCAdw[i];

			double ga_norm2 = Matrix.dot( ga, ga );
			for ( int i = 0; i < nwts; i++ ) na[i] = ga[i] / Math.sqrt(ga_norm2);

			// Project the error gradient onto the plane orthogonal to the constraint gradient.

			double na_dot_gr = Matrix.dot( na, gr );
			double[] hr = (double[]) gr.clone();
			Matrix.axpby( 1, hr, -na_dot_gr, na );

			// Take a step uphill along the projected gradient.
			// Unpack the results into t and w.

			Matrix.axpby( 1, t_w, epsilon, hr );
			t = t_w[0];
			for ( int i = 0; i < weights_unpacked.length; i++ ) weights_unpacked[i] = t_w[i+1];
System.err.println( "iteration: "+niter+"  t: "+t+"  CA: "+compute_CA(t,xp)+"  CR: "+compute_CR(t,xn) );
			t = find_threshold( xp, constraint_misclass_rate );
System.err.println( "\t"+"after projection to constraint surface:  t: "+t+"  CA: "+compute_CA(t,xp)+"  CR: "+compute_CR(t,xn) );

			long elapsed_since_last_save = System.currentTimeMillis()/1000 - last_save;
			if ( elapsed_since_last_save > 300 )
			{
				System.err.println( "\t"+"save current parameters to "+filename );
				FileOutputStream fos = new FileOutputStream(filename);
				pretty_output( fos, "\t" );
				fos.close();
				last_save = System.currentTimeMillis()/1000;
			}
		}
		while ( ++niter < niter_max );

		return 0;
	}
	
	public double compute_dCAdt( double[][] xp, double[][] yp, double t ) throws Exception
	{
		double sum = 0;

		for ( int i = 0; i < xp.length; i++ )
		{
			double u = sigma( beta*( F(xp[i])[0] - t ) );	
			sum += u*(1-u);
		}

		return -(beta*t)*sum/xp.length;
	}

	public double[] compute_dCAdw( double[][] xp, double[][] yp, double t ) throws Exception
	{
		double[] sum = new double[nwts];

		for ( int i = 0; i < xp.length; i++ )
		{
			double u = sigma( beta*( F(xp[i])[0] - t ) );
			double v = F(xp[i])[0];
			double[] dadw = compute_dadw( xp[i] );
			Matrix.axpby( 1, sum, u*(1-u)*v*(1-v), dadw );
		}

		Matrix.axpby( beta/xp.length, sum, 0, null );
		return sum;
	}

	public double compute_dCRdt( double[][] xn, double[][] yn, double t ) throws Exception
	{
		double sum = 0;

		for ( int i = 0; i < xn.length; i++ )
		{
			double u = sigma( beta*( F(xn[i])[0] - t ) );	
			sum += u*(1-u);
		}

		return (beta*t)*sum/xn.length;
	}

	public double[] compute_dCRdw( double[][] xn, double[][] yn, double t ) throws Exception
	{
		double[] sum = new double[nwts];

		for ( int i = 0; i < xn.length; i++ )
		{
			double u = sigma( beta*( F(xn[i])[0] - t ) );
			double v = F(xn[i])[0];
			double[] dadw = compute_dadw( xn[i] );
			Matrix.axpby( 1, sum, u*(1-u)*v*(1-v), dadw );
		}

		Matrix.axpby( -beta/xn.length, sum, 0, null );
		return sum;
	}

	public static void main( String[] args )
	{
		boolean do_p = true, do_initial_training = false, do_calculate_outputs = false, do_random_after_training = false;
		int ndata = -1;
		double constraint_misclass_rate = 0.5, epsilon = 1e-2, beta = 10, magnitude = 0.01;
		String filename = "";

		for ( int i = 0; i < args.length; i++ )
		{
			switch ( args[i].charAt(1) )
			{
			case 'b':
				beta = Double.parseDouble( args[++i] );
				System.err.println( "set beta: "+beta );
				break;
			case 'e':
				epsilon = Double.parseDouble( args[++i] );
				System.err.println( "set epsilon: "+epsilon );
				break;
			case 'f':
				filename = args[++i];
				break;
			case 'N':
				ndata = Integer.parseInt( args[++i] );
				break;
			case 'n':
				do_p = false;
				break;
			case 'o':
				do_calculate_outputs = true;
				break;
			case 'p':
				do_p = true;
				break;
			case 'r':
				do_random_after_training = true;
				do_initial_training = true;
				magnitude = Double.parseDouble( args[++i] );
				System.err.println( "set magnitude: "+magnitude );
				break;
			case 'c':
				constraint_misclass_rate = Double.parseDouble( args[++i] );
				System.err.println( "set constraint_misclass_rate: "+constraint_misclass_rate );
				break;
			case 'i':
				do_initial_training = true;
				break;
			}
		}

		try
		{
			Reader r = new InputStreamReader( new FileInputStream( filename ) );
			SmarterTokenizer st = new SmarterTokenizer(r);
			st.nextToken();
			ConstrainedSquashingNet csn = (ConstrainedSquashingNet) Class.forName(st.sval).newInstance();
			csn.pretty_input(st);
			csn.epsilon = epsilon;
			csn.beta = beta;
			csn.constraint_misclass_rate = constraint_misclass_rate;
			csn.filename = filename;
			
			r = new InputStreamReader( System.in );
			st = new SmarterTokenizer(r);

			int nin = csn.ndimensions_in(), nout = csn.ndimensions_out();
			double[][] Xin = new double[ndata][nin];
			double[][] Yin = new double[ndata][nout];

			for ( int i = 0; i < ndata; i++ )
			{
				try
				{
					for ( int j = 0; j < nin; j++ ) { st.nextToken(); Xin[i][j] = Double.parseDouble( st.sval ); }
					st.nextToken(); Yin[i][0] = Double.parseDouble( st.sval );
				}
				catch (Exception e) { System.err.println( "failed at line "+i+"; st.sval: "+st.sval ); e.printStackTrace(); System.exit(1); }
			}

			// Count up the positive and negative examples. 
			// We'll optimize for one set and use the other for the constraint.

			int pcount = 0;
			for ( int i = 0; i < ndata; i++ ) if ( Yin[i][0] == 1 ) ++pcount;

			double[][] X, CX;
			double[][] Y, CY;

			if ( do_p )
			{
				X = new double[pcount][];
				Y = new double[pcount][1];
				CX = new double[ndata-pcount][];
				CY = new double[ndata-pcount][1];

				int jj = 0, ii = 0;
				for ( int i = 0; i < ndata; i++ )
				{
					if ( Yin[i][0] == 1 )
					{
						X[ii] = (double[]) Xin[i].clone();
						Y[ii][0] = Yin[i][0];
						++ii;
					}
					else
					{
						CX[jj] = (double[]) Xin[i].clone();
						CY[jj][0] = Yin[i][0];
						++jj;
					}
				}
			}
			else
			{
				X = new double[ndata-pcount][];
				Y = new double[ndata-pcount][1];
				CX = new double[pcount][];
				CY = new double[pcount][1];

				int jj = 0, ii = 0;
				for ( int i = 0; i < ndata; i++ )
				{
					if ( Yin[i][0] != 1 )
					{
						X[ii] = (double[]) Xin[i].clone();
						Y[ii][0] = Yin[i][0];
						++ii;
					}
					else
					{
						CX[jj] = (double[]) Xin[i].clone();
						CY[jj][0] = Yin[i][0];
						++jj;
					}
				}
			}

			// System.err.println( "Training data:" );
			// Matrix.pretty_output( X, System.err, "\t" );

			// System.err.println( "Constraint data:" );
			// Matrix.pretty_output( CX, System.err, "\t" );

			if ( do_initial_training ) csn.update( Xin, Yin, 1000, 1e-4, null );

			if ( do_random_after_training )
			{
				for ( int i = 0; i < csn.weights_unpacked.length; i++ )
					csn.weights_unpacked[i] += (Math.random() - 0.5)*magnitude;
			}

			double t = csn.find_threshold( CX, constraint_misclass_rate );

			csn.update( t, CX, CY, X, Y, 100 );
			System.err.println( "save parameters to "+csn.filename );
			csn.pretty_output( new FileOutputStream(csn.filename), "\t" );
			
			if ( do_calculate_outputs )
			{
				System.err.println( "Output on training data (target "+Y[0][0]+"): " );
				for ( int i = 0; i < X.length; i++ )
				{
					double[] yhat = csn.F( X[i] );
					for ( int j = 0; j < yhat.length; j++ )
						System.err.print( " "+ yhat[j] );
					System.err.println("");
				}

				System.err.println( "Output on constraint data (target "+CY[0][0]+"):" );
				for ( int i = 0; i < CX.length; i++ )
				{
					double[] yhat = csn.F( CX[i] );
					for ( int j = 0; j < yhat.length; j++ )
						System.err.print( " "+ yhat[j] );
					System.err.println("");
				}
			}
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
