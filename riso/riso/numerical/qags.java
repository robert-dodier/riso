import numerical.*;

public class qags
{
	public static void qags ( Callback_1d f,double a,double b,double epsabs,double epsrel,double [ ] result,double [ ] abserr,int [ ] neval,int [ ] ier,int limit,int lenw,int[] last,int[] iwork,double [ ] work ) throws Exception
	{
		int lvl = 0,l1,l2,l3;

		ier [ 0 ] = 6;
		neval [ 0 ] = 0;
		last[0] = 0;
		result [ 0 ] = 0;
		abserr [ 0 ] = 0;
		if ( ! ( limit < 1 || lenw < limit * 4 ) )
		{
			l1 = limit+1;
			l2 = limit+l1;
			l3 = limit+l2;

			double[] alist = new double[ limit ];
			double[] blist = new double[ limit ];
			double[] rlist = new double[ limit ];
			double[] elist = new double[ limit ];

			qagse.qagse ( f , a , b , epsabs , epsrel , limit , result , abserr , neval , ier , alist , blist , rlist , elist , iwork , last );

			System.arraycopy( alist, 0, work, 0, limit );
			System.arraycopy( blist, 0, work, limit, limit );
			System.arraycopy( rlist, 0, work, 2*limit, limit );
			System.arraycopy( elist, 0, work, 3*limit, limit );

			lvl = 0;
		}
		if ( ier [ 0 ] == 6 ) lvl = 1;
		if ( ier [ 0 ] != 0 ) throw new Exception ( xerror ( "abnormal return from qags" ,ier [ 0 ] ,lvl ) );
		return;
	}

	public static String xerror ( String message, int errno, int level )
	{
		String level_msg;
		switch ( level )
		{
			case -1:
				level_msg = "(one-time warning)";
				break;
			case 0:
				level_msg = "(warning)";
				break;
			case 1:
				level_msg = "(recoverable error)";
				break;
			case 2:
				level_msg = "(fatal error)";
				break;
			default:
				level_msg = "(unknown level: " +level+ ")";
		}
		return message+ ", error number: " +errno+ " " +level_msg;
	}

	public static void main( String[] args )
	{
		double a, b;
		double epsabs = 1e-6, epsrel = 1e-6;
		double[] result = new double[1];
		double[] abserr = new double[1];
		double[] resabs = new double[1];
		double[] resasc = new double[1];

		a = Format.atof( args[0] );
		b = Format.atof( args[1] );
		System.err.println( "a: "+a+"  b: "+b );
		Callback_1d integrand = new GaussBump();

		int[] neval = new int[1], ier = new int[1], last = new int[1];
		int limit = 4, lenw = 4*limit;
		int[] iwork = new int[ limit ];
		double[] work = new double[ lenw ];

		try
		{
			qags.qags( integrand, a, b, epsabs, epsrel, result, abserr, neval, ier, limit, lenw, last, iwork, work );
		}
		catch (Exception e) { e.printStackTrace(); return; }

		System.err.println( "result: "+result[0] );
		System.err.println( "abserr: "+abserr[0] );
		System.err.println( "neval:  "+neval[0] );
		System.err.println( "ier:    "+ier[0] );

		System.err.println( "limit: "+limit+" last: "+last[0] );
		int i;
		System.err.print( "iwork: " );
		for ( i = 0; i < iwork.length; i++ ) System.err.print( iwork[i]+" " );
		System.err.println("");
		System.err.println( "a\tb\tI\terr  (from work):" );
		for ( i = 0; i < last[0]; i++ )
		{
			System.err.println( work[i]+"\t"+work[limit+i]+"\t"+work[2*limit+i]+"\t"+work[3*limit+i] );
		}
	}
}
