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
}
