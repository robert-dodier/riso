import numerical.*;

public class qagse
{
	public static void qagse ( Callback_1d f , double a , double b , double epsabs , double epsrel , int limit , double[] result , double[] abserr , int[] neval , int[] ier , double[] alist , double[] blist , double[] rlist , double[] elist , int[] iord , int[] last ) throws Exception // SHOULD USE ier EXCLUSIVELY OR EXCEPTIONS EXCLUSIVELY, NOT BOTH !!!
	{
		double area, area12, a1, a2, b1, b2, correc = -999, dres, epmach, erlarg = -999, erlast, errbnd, erro12, errsum, ertest = -999, oflow, small = -999, uflow;
		double[] res3la = new double [ 3 ], rlist2 = new double [ 52 ];
		double[] defabs = new double[1], resabs = new double[1];
		double[] area1 = new double[1], area2 = new double[1];
		double[] error1 = new double[1], error2 = new double[1];
		double[] defab1 = new double[1], defab2 = new double[1];
		double[] reseps = new double[1], abseps = new double[1];
		double[] errmax = new double[1];
		int id, ierro, iroff1, iroff2, iroff3, jupbnd, k, ksgn, ktmin;
		int[] maxerr = new int[1], nrmax = new int[1], numrl2 = new int[1], nres = new int[1];
		boolean extrap,noext;
		epmach = qk21.D1MACH [ 4-1 ];
		ier[0] = 0;
		neval[0] = 0;
		last[0] = 0;
		result[0] = 0;
		abserr[0] = 0;
		alist [ 1 -1 ] = a;
		blist [ 1 -1 ] = b;
		rlist [ 1 -1 ] = 0;
		elist [ 1 -1 ] = 0;
		if ( epsabs <= 0 && epsrel < Math.max ( 50 * epmach , 0.5e-28 ) ) ier[0] = 6;
		if ( ier[0] == 6 ) return;
		uflow = qk21.D1MACH [ 1-1 ];
		oflow = qk21.D1MACH [ 2-1 ];
		ierro = 0;
		qk21.qk21 ( f , a , b , result , abserr , defabs , resabs );
		dres = Math.abs ( result[0] );
		errbnd = Math.max ( epsabs , epsrel * dres );
		last[0] = 1;
		rlist [ 1 -1 ] = result[0];
		elist [ 1 -1 ] = abserr[0];
		iord [ 1 -1 ] = 1;
		if ( abserr[0] <= 100 * epmach * defabs[0] && abserr[0] > errbnd ) ier[0] = 2;
		if ( limit == 1 ) ier[0] = 1;
		if ( ier[0] != 0 || ( abserr[0] <= errbnd && abserr[0] != resabs[0] ) || abserr[0] == 0 )
		{
			neval[0] = 42*last[0]-21;
			return;
		}
		rlist2 [ 1 -1 ] = result[0];
		errmax[0] = abserr[0];
		maxerr[0] = 1;
		area = result[0];
		errsum = abserr[0];
		abserr[0] = oflow;
		nrmax[0] = 1;
		nres[0] = 0;
		numrl2[0] = 2;
		ktmin = 0;
		extrap = false;
		noext = false;
		iroff1 = 0;
		iroff2 = 0;
		iroff3 = 0;
		ksgn = -1;
		if ( dres >= ( 1 - 50 * epmach ) * defabs[0] ) ksgn = 1;
		for ( last[0] = 2 ; last[0] <= limit ; last[0] += 1 )
		{
			a1 = alist [ maxerr[0] -1 ];
			b1 = 0.5 * ( alist [ maxerr[0] -1 ] + blist [ maxerr[0] -1 ] );
			a2 = b1;
			b2 = blist [ maxerr[0] -1 ];
			erlast = errmax[0];
			qk21.qk21 ( f , a1 , b1 , area1 , error1 , resabs , defab1 );
			qk21.qk21 ( f , a2 , b2 , area2 , error2 , resabs , defab2 );
			area12 = area1[0]+area2[0];
			erro12 = error1[0]+error2[0];
			errsum = errsum+erro12-errmax[0];
			area = area + area12 - rlist [ maxerr[0] -1 ];
			if ( ! ( defab1[0] == error1[0] || defab2[0] == error2[0] ) )
			{
				if ( ! ( Math.abs ( rlist [ maxerr[0] -1 ] - area12 ) > 1e-5 * Math.abs ( area12 ) || erro12 < 0.99 * errmax[0] ) )
				{
					if ( extrap ) iroff2 = iroff2 + 1;
					if ( ! extrap ) iroff1 = iroff1 + 1;
				}
				if ( last[0] > 10 && erro12 > errmax[0] ) iroff3 = iroff3 + 1;
			}
			rlist [ maxerr[0] -1 ] = area1[0];
			rlist [ last[0] -1 ] = area2[0];
			errbnd = Math.max ( epsabs , epsrel * Math.abs ( area ) );
			if ( iroff1 + iroff2 >= 10 || iroff3 >= 20 ) ier[0] = 2;
			if ( iroff2 >= 5 ) ierro = 3;
			if ( last[0] == limit ) ier[0] = 1;
			if ( Math.max ( Math.abs ( a1 ) , Math.abs ( b2 ) ) <= ( 1 + 100 * epmach ) * ( Math.abs ( a2 ) + 1000 * uflow ) ) ier[0] = 4;
			if ( ! ( error2[0] > error1[0] ) )
			{
				alist [ last[0] -1 ] = a2;
				blist [ maxerr[0] -1 ] = b1;
				blist [ last[0] -1 ] = b2;
				elist [ maxerr[0] -1 ] = error1[0];
				elist [ last[0] -1 ] = error2[0];
			}
			else
			{
				alist [ maxerr[0] -1 ] = a2;
				alist [ last[0] -1 ] = a1;
				blist [ last[0] -1 ] = b1;
				rlist [ maxerr[0] -1 ] = area2[0];
				rlist [ last[0] -1 ] = area1[0];
				elist [ maxerr[0] -1 ] = error2[0];
				elist [ last[0] -1 ] = error1[0];
			}
			qpsrt.qpsrt ( limit , last[0] , maxerr , errmax , elist , iord , nrmax );
			if ( errsum <= errbnd )
			{
				result[0] = 0;
				for ( k = 1 ; k <= last[0] ; k += 1 )
				{
					result[0] = result[0] + rlist [ k -1 ];
				}
				abserr[0] = errsum;
				if ( ier[0] > 2 ) ier[0] = ier[0] - 1;
				neval[0] = 42*last[0]-21;
				return;
			}
			if ( ier[0] != 0 ) break;
			if ( ! ( last[0] == 2 ) )
			{
				if ( noext ) break;
if ( erlarg == -999 ) throw new Exception( "qagse: erlarg NOT DEFINED!!!" );
				erlarg = erlarg-erlast;
if ( small == -999 ) throw new Exception( "qagse: small NOT DEFINED!!!" );
				if ( Math.abs ( b1 - a1 ) > small ) erlarg = erlarg + erro12;
				if ( ! ( extrap ) )
				{
					if ( Math.abs ( blist [ maxerr[0] -1 ] - alist [ maxerr[0] -1 ] ) > small ) break;
					extrap = true;
					nrmax[0] = 2;
				}
if ( ertest == -999 ) throw new Exception( "qagse: ertest NOT DEFINED!!!" );
				if ( ! ( ierro == 3 || erlarg <= ertest ) )
				{
					id = nrmax[0];
					jupbnd = last[0];
					if ( last[0] > ( 2 + limit / 2 ) ) jupbnd = limit + 3 - last[0];
					boolean goto90 = false;
					for ( k = id ; k <= jupbnd ; k += 1 )
					{
						maxerr[0] = iord [ nrmax[0] -1 ];
						errmax[0] = elist [ maxerr[0] -1 ];
						if ( Math.abs ( blist [ maxerr[0] -1 ] - alist [ maxerr[0] -1 ] ) > small )
						{
							goto90 = true;
							break;
						}
						nrmax[0] = nrmax[0]+1;
					}
					if ( goto90 ) break;
				}
				numrl2[0] = numrl2[0]+1;
				rlist2 [ numrl2[0] -1 ] = area;
				qelg.qelg( numrl2 , rlist2 , reseps , abseps , res3la , nres );
				ktmin = ktmin+1;
				if ( ktmin > 5 && abserr[0] < 1e-3 * errsum ) ier[0] = 5;
				if ( ! ( abseps[0] >= abserr[0] ) )
				{
					ktmin = 0;
					abserr[0] = abseps[0];
					result[0] = reseps[0];
					correc = erlarg;
					ertest = Math.max ( epsabs , epsrel * Math.abs ( reseps[0] ) );
					if ( abserr[0] <= ertest ) break;
				}
				if ( numrl2[0] == 1 ) noext = true;
				if ( ier[0] == 5 ) break;
				maxerr[0] = iord [ 1 -1 ];
				errmax[0] = elist [ maxerr[0] -1 ];
				nrmax[0] = 1;
				extrap = false;
				small = small*0.5;
				erlarg = errsum;
				break;
			}
			small = Math.abs ( b - a ) * 0.375;
			erlarg = errsum;
			ertest = errbnd;
			rlist2 [ 2 -1 ] = area;
		}
		if ( abserr[0] == oflow )
		{
			result[0] = 0;
			for ( k = 1 ; k <= last[0] ; k += 1 )
			{
				result[0] = result[0] + rlist [ k -1 ];
			}
			abserr[0] = errsum;
			if ( ier[0] > 2 ) ier[0] = ier[0] - 1;
			neval[0] = 42*last[0]-21;
			return;
		}
		if ( ier[0] + ierro == 0 )
		{
if ( correc == -999 ) throw new Exception( "qagse: correc NOT DEFINED!!!" );
			if ( ierro == 3 ) abserr[0] = abserr[0] + correc;
			if ( ier[0] == 0 ) ier[0] = 3;
			if ( result[0] != 0 && area != 0 )
			{
				if ( abserr[0] / Math.abs ( result[0] ) > errsum / Math.abs ( area ) )
				{
					result[0] = 0;
					for ( k = 1 ; k <= last[0] ; k += 1 )
					{
						result[0] = result[0] + rlist [ k -1 ];
					}
					abserr[0] = errsum;
					if ( ier[0] > 2 ) ier[0] = ier[0] - 1;
					neval[0] = 42*last[0]-21;
					return;
				}
				if ( ksgn == ( - 1 ) && Math.max ( Math.abs ( result[0] ) , Math.abs ( area ) ) <= defabs[0] * 0.01 )
				{
					if ( ier[0] > 2 ) ier[0] = ier[0] - 1;
					neval[0] = 42*last[0]-21;
					return;
				}
				if ( 0.01 > ( result[0] / area ) || ( result[0] / area ) > 100 || errsum > Math.abs ( area ) ) ier[0] = 6;
				if ( ier[0] > 2 ) ier[0] = ier[0] - 1;
				neval[0] = 42*last[0]-21;
				return;
				// NOT REACHED ??? result[0] = 0;
				// NOT REACHED ??? for ( k = 1 ; k <= last[0] ; k += 1 )
				// NOT REACHED ??? {
					// NOT REACHED ??? result[0] = result[0] + rlist [ k -1 ];
				// NOT REACHED ??? }
				// NOT REACHED ??? abserr[0] = errsum;
				// NOT REACHED ??? if ( ier[0] > 2 ) ier[0] = ier[0] - 1;
				// NOT REACHED ??? neval[0] = 42*last[0]-21;
				// NOT REACHED ??? return;
			}
			if ( abserr[0] > errsum )
			{
				result[0] = 0;
				for ( k = 1 ; k <= last[0] ; k += 1 )
				{
					result[0] = result[0] + rlist [ k -1 ];
				}
				abserr[0] = errsum;
				if ( ier[0] > 2 ) ier[0] = ier[0] - 1;
				neval[0] = 42*last[0]-21;
				return;
			}
			if ( area == 0 )
			{
				if ( ier[0] > 2 ) ier[0] = ier[0] - 1;
				neval[0] = 42*last[0]-21;
				return;
			}
		}
		if ( ksgn == ( - 1 ) && Math.max ( Math.abs ( result[0] ) , Math.abs ( area ) ) <= defabs[0] * 0.01 )
		{
			if ( ier[0] > 2 ) ier[0] = ier[0] - 1;
			neval[0] = 42*last[0]-21;
			return;
		}
		if ( 0.01 > ( result[0] / area ) || ( result[0] / area ) > 100 || errsum > Math.abs ( area ) ) ier[0] = 6;
		if ( ier[0] > 2 ) ier[0] = ier[0] - 1;
		neval[0] = 42*last[0]-21;
		return;
		// NOT REACHED ??? result[0] = 0;
		// NOT REACHED ??? for ( k = 1 ; k <= last[0] ; k += 1 )
		// NOT REACHED ??? {
			// NOT REACHED ??? result[0] = result[0] + rlist [ k -1 ];
		// NOT REACHED ??? }
		// NOT REACHED ??? abserr[0] = errsum;
		// NOT REACHED ??? if ( ier[0] > 2 ) ier[0] = ier[0] - 1;
		// NOT REACHED ??? neval[0] = 42*last[0]-21;
		// NOT REACHED ??? return;
	}
}
