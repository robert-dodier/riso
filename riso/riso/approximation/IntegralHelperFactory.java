package riso.approximation;
import numerical.*;

public class IntegralHelperFactory
{
	/** Choose an appropriate kind of integral helper for computing integrals in RISO.
	  * In one dimension, the helper is a QAGS helper. 
	  * Otherwise (in zero or in two or more dimensions), it's a quasi Monte Carlo helper.
	  */
	public static IntegralHelper make_helper( Callback_nd fn, double[] a, double[] b, boolean[] is_discrete, boolean[] skip_integration )
	{
		int n = a.length, nintegration = 0;
		for ( int i = 0; i < n; i++ )
			if ( !is_discrete[i] && !skip_integration[i] )
				++nintegration;

		if ( nintegration == 1 )
			return new QAGS_IntegralHelper( fn, a, b, is_discrete, skip_integration );
		else
			return new QuasiMC_IntegralHelper( fn, a, b, is_discrete, skip_integration );
	}

	public class qk21_HelperWrapper implements IntegralHelper
	{
		qk21_IntegralHelper1d qk21_helper;
		double[] x;
		int k;

		qk21_HelperWrapper( int k, Callback_nd fn, double[] a, double[] b, boolean[] is_discrete, boolean[] skip_integration )
		{
			double[][] intervals = new double[a.length][2];
			for ( int i = 0; i < a.length; i++ )
			{
				intervals[i][0] = a[i];
				intervals[i][1] = b[i];
			}

			qk21_helper = new qk21_IntegralHelper1d( new Callback(fn), intervals, is_discrete[k] );
			x = new double[ a.length ];
			this.k = k;
		}

		public double do_integral( double[] x_in ) throws Exception
		{
			if ( x_in != null ) System.arraycopy( x_in, 0, x, 0, x.length );
			return qk21_helper.do_integral();
		}
			
		public double do_integral() throws Exception { return qk21_helper.do_integral(); }

		public class Callback implements Callback_1d
		{
			public double f( double xx ) throws Exception
			{
				x[k] = xx;
				return fn.f(x);
			}
		}
	}
}
