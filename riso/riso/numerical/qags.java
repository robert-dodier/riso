import numerical.*;

public class qags
{
	 Callback_1d f;
	 double a , b , epsabs , epsrel , result , abserr;
	 int neval ,ier ,    limit ,lenw ,last ,iwork;
	 double[] work;
	public void qags()
	{

	ier = 6;
	neval = 0;
	last = 0;
	result = 0;
	abserr = 0;
	if  ( limit < 1 || lenw < limit * 4 )
	{
	l1 = limit+1;
	l2 = limit+l1;
	l3 = limit+l2;
	qagse ( f , a , b , epsabs , epsrel , limit , result , abserr , neval , ier , work [ 1 -1 ] , work [ l1 -1 ] , work [ l2 -1 ] , work [ l3 -1 ] , iwork , last );
	lvl = 0;
	}
	if  ( ier  ==  6 )  lvl  =  1;
	if  ( ier !=  0 )
		throw new Exception( xerror( "abnormal return from qags",ier,lvl)  );

	return;
	}

	public static String xerror( String message, int errno, int level )
	{
		String level_msg;

		switch (level)
		{
		case -1: level_msg = "(one-time warning)"; break;
		case  0: level_msg = "(warning)"; break;
		case  1: level_msg = "(recoverable error)"; break;
		case  2: level_msg = "(fatal error)"; break;
		default: level_msg = "(unknown level: "+level+")";
		}

		return message+", error number: "+errno+" "+level_msg;
	}
}


