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
}
