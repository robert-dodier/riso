package numerical;

public class IntegrandHelper1d
{
	double[] a, b;
	Callback_1d f1;

	boolean is_discrete;

	double epsabs = 1e-3, epsrel = 1e-3;
	int limit;
	int neval;

	qags q;

	public IntegrandHelper1d( Callback_1d f1, double[][] intervals, boolean is_discrete )
	{
		a = new double[ intervals.length ];
		b = new double[ intervals.length ];
		for ( int i = 0; i < intervals.length; i++ )
		{
			a[i] = intervals[i][0];
			b[i] = intervals[i][1];
		}

		this.f1 = f1;

		this.is_discrete = is_discrete;
	}

	public double do_integral() throws Exception
	{
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
			double[] result = new double[1], abserr = new double[1];
			int[] ier = new int[1];

			double sum = 0;

			for ( int j = 0; j < a.length; j++ )
			{
				q.qags( f1, a[j], b[j], epsabs, epsrel, result, abserr, ier, limit );
				neval += q.neval[0];

				if ( ier[0] != 0 ) 
					System.err.println( "IntegrandHelper1d.do_integral: WARNING: ier=="+ier[0]+"; return result=="+result[0]+", abserr=="+abserr[0] );

				sum += result[0];
			}

			return sum;
		}
	}
}
