package numerical;

public class IntegralHelper implements Callback_1d
{
	int n;
	Callback_nd fn;

	boolean[] is_discrete, skip_integration;

	public double[] x, a, b;
	public double epsabs = 1e-3, epsrel = 1e-3;
	public int limit;
	public int[] neval;	// counts function evaluations in each dimension

	qags[] q;		// one context for each level; don't share work variables!

	public IntegralHelper( Callback_nd fn, double[] a, double[] b, boolean[] is_discrete, boolean[] skip_integration )
	{
		this.fn = fn;
		this.a = (double[]) a.clone();
		this.b = (double[]) b.clone();

		n = a.length;
System.err.println( "IntegralHelper: set up "+n+"-dimensional integral, fn: "+fn );
		x = new double[n];
		neval = new int[n];

		this.is_discrete = (boolean[]) is_discrete.clone();
		this.skip_integration = (boolean[]) skip_integration.clone();

		// Count up the number of dimensions in which we are computing a
		// integration over a continuous variable.

		int i, nintegration = 0, ndiscrete = 0, nskip = 0;
		for ( i = 0; i < n; i++ )
			if ( ! is_discrete[i] && ! skip_integration[i] )
				++nintegration;
			else
			{
				// "is discrete" and "skip" are not mutually exclusive.
				if ( is_discrete[i] ) ++ndiscrete;
				if ( skip_integration[i] ) ++nskip;
			}
System.err.println( "IntegralHelper: #integrations: "+nintegration+"; #discrete "+ndiscrete+", #skip: "+nskip );

		switch ( nintegration )
		{
		case 1: limit = 30; break;
		
		case 2: limit = 5; break;
		case 3: limit = 3; break;
		case 4: limit = 2; break;
		case 5: limit = 2; break;
		default: // anything beyond 5
			limit = 1;
		}

		q = new qags[n];
		for ( i = 0; i < n; i++ ) q[i] = new qags();

		--n;	// now n == next dimension to integrate over
	}

	public double f( double x1 ) throws Exception
	{
		x[n] = x1;

		if ( n == 0 ) 
		{
			// Recursion has bottomed out -- return integrand value.
//System.err.print( "IntegralHelper.f: x: (" ); for(int i=0;i<x.length;i++) System.err.print(x[i]+","); 
			double fnx = fn.f(x);
//System.err.println("), fn(x): "+fnx );
			return fnx;
		}
		else
		{
			double fx;

			--n;
			fx = do_integral();
			++n;

			return fx;
		}
	}

	public double do_integral( double[] x_in ) throws Exception
	{
		System.arraycopy( x_in, 0, x, 0, x.length );
		return do_integral();
	}
		
	public double do_integral() throws Exception
	{
		if ( skip_integration[n] )
		{
			// Assume that x[n] was set by do_integral's caller.

			if ( n == 0 ) 
			{
				++neval[n];
				return fn.f(x);
			}
			else
			{
				double fx;

				--n;
				fx = do_integral();
				++n;

				++neval[n];
				return fx;
			}
		}

		if ( is_discrete[n] ) 
		{
			// Compute the summation over x[n].
			double sum = 0;
			int i0 = (a[n] < b[n] ? (int)a[n] : (int)b[n]);
			int i1 = (a[n] < b[n] ? (int)b[n] : (int)a[n]);
// System.err.println( "variable "+n+" is discrete; i0: "+i0+", i1: "+i1 );

			for ( int i = i0; i <= i1; i++ )
				sum += f( (double)i );

			neval[n] += i1-i0+1;

			return sum;
		}
		else
		{
			double[] result = new double[1], abserr = new double[1];
			int[] ier = new int[1];

			q[n].qags( this, a[n], b[n], epsabs, epsrel, result, abserr, ier, limit );
			neval[n] += q[n].neval[0];

			if ( ier[0] != 0 ) 
				System.err.println( "IntegralHelper.do_integral: integrate over variable "+n+". WARNING: ier=="+ier[0]+"; return result=="+result[0]+", abserr=="+abserr[0] );

			return result[0];
		}
	}

	public static void main( String[] args )
	{
		try
		{
			double[] a = new double[3], b = new double[3];

			int i;

			for ( i = 0; i < 3; i++ )
			{
				a[i] = Format.atof( args[i] );
				b[i] = Format.atof( args[3+i] );
				System.err.println( "a["+i+"]: "+a[i]+"  b["+i+"]: "+b[i] );
			}
		
			boolean[] is_discrete = new boolean[3];
			boolean[] skip_integration = new boolean[3];

			String s1 = args[6];
			for ( i = 0; i < 3; i++) 
				is_discrete[i] = (s1.charAt(i) == 'y');

			String s2 = args[7];
			for ( i = 0; i < 3; i++) 
				skip_integration[i] = (s2.charAt(i) == 'y');

			IntegralHelper ih = new IntegralHelper( new ThreeD(), a, b, is_discrete, skip_integration );

			for ( i = 0; i < 3; i++ )
				if ( skip_integration[i] )
					ih.x[i] = (a[i]+b[i])/2;

			System.err.println( "ih.do_integral: "+ih.do_integral() );

			for ( i = 0; i < 3; i++ )
				System.err.println( "neval["+i+"]: "+ih.neval[i] );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}

class ThreeD implements Callback_nd
{
	public double f( double[] x )
	{
		// double b0 = Bickley.bickley( x[0], 0 );
		// double b1 = Bickley.bickley( x[1], 0 );
		// double b2 = Bickley.bickley( x[2], 0 );
		// double fx = b0*b1*b2;

		double fx = x[0]*x[1]*x[2];
		return fx;
	}
}
