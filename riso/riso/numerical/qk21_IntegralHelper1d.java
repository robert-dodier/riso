package numerical;

public class qk21_IntegralHelper1d implements java.io.Serializable
{
	Callback_1d f1;

	boolean is_discrete;

	public double[] a, b;
	public int neval;

	public qk21_IntegralHelper1d( Callback_1d f1, double[][] intervals, boolean is_discrete )
	{
		// If the limits of integration are not yet established,
		// the caller must do so before calling do_integral().
		
		if ( intervals == null )
		{
			a = b = null;
		}
		else
		{
			a = new double[ intervals.length ];
			b = new double[ intervals.length ];
			for ( int i = 0; i < intervals.length; i++ )
			{
				a[i] = intervals[i][0];
				b[i] = intervals[i][1];
			}
		}

		this.f1 = f1;

		this.is_discrete = is_discrete;
	}

	public double do_integral() throws Exception
	{
		neval = 0;

		if ( is_discrete ) 
		{
			double sum = 0;

			for ( int j = 0; j < a.length; j++ )
			{
				int i0 = (a[j] < b[j] ? (int)a[j] : (int)b[j]);
				int i1 = (a[j] < b[j] ? (int)b[j] : (int)a[j]);

				for ( int i = i0; i <= i1; i++ )
					sum += f1.f( (double)i );

				neval += i1-i0+1;
			}

			return sum;
		}
		else
		{
			double[] result = new double[1];
			double sum = 0;

			for ( int j = 0; j < a.length; j++ )
			{
				// Last three arguments are don't-cares for us.
				qk21.do_qk21( f1, a[j], b[j], result, new double[1], new double[1], new double[1] );

				neval += 21;
				sum += result[0];
			}

			return sum;
		}
	}
}
