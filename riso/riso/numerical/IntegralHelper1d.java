package numerical;

public class IntegralHelper1d implements java.io.Serializable
{
	Callback_1d f1;

	boolean is_discrete;

	public double[] a, b;
	public double epsabs = 1e-6, epsrel = 1e-6;
	public int limit = 30;
	public int npanels = 10;	// split each interval into this many pieces
	public int neval;

	qags q = new qags();

	public IntegralHelper1d( Callback_1d f1, double[][] intervals, boolean is_discrete )
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
			boolean some_ier = false;

			double sum = 0;

			for ( int j = 0; j < a.length; j++ )
			{
				double total_result = 0, h = (b[j]-a[j])/npanels, aa, bb;

				for ( int i = 0; i < npanels; i++ )
				{
					aa = a[j] + i*h;
					bb = aa + h;
					q.qags( f1, aa, bb, epsabs/npanels, epsrel, result, abserr, ier, limit );
					total_result += result[0];
					neval += q.neval[0];

					if ( ier[0] != 0 ) 
						some_ier = true;
				}

				if ( some_ier && q.verbose_errors )
					System.err.println( "IntegralHelper1d.do_integral: WARNING: ier != 0 for at least one of "+npanels+" panels." );

				sum += total_result;
			}

			return sum;
		}
	}
}
