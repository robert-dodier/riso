
public class qagse
{

	public void qagse ( f , a , b , epsabs , epsrel , limit , result , abserr , neval , ier , alist , blist , rlist , elist , iord , last )
	{
		double a, abseps, abserr, alist, area, area1, area12, area2, a1, a2, b, blist, b1, b2, correc, dabs, defabs, defab1, defab2, dres, elist, epmach, epsabs, epsrel, erlarg, erlast, errbnd, errmax, error1, error2, erro12, errsum, ertest, f, oflow, resabs, reseps, result, res3la, rlist, rlist2, small, uflow;
		int id, ier, ierro, iord, iroff1, iroff2, iroff3, jupbnd, k, ksgn, ktmin, last, limit, maxerr, neval, nres, nrmax, numrl2;
		boolean extrap,noext;
		res3la = new double [ 3 ];
		rlist2 = new double [ 52 ];
		epmach = qk21.D1MACH [ 4-1 ];
		ier = 0;
		neval = 0;
		last = 0;
		result = 0;
		abserr = 0;
		alist [ 1 -1 ] = a;
		blist [ 1 -1 ] = b;
		rlist [ 1 -1 ] = 0;
		elist [ 1 -1 ] = 0;
		if ( epsabs <= 0 && epsrel < Math.max ( 50 * epmach , 0.5e-28 ) ) ier = 6;
		if ( ier == 6 ) return;
		uflow = qk21.D1MACH [ 1-1 ];
		oflow = qk21.D1MACH [ 2-1 ];
		ierro = 0;
		qk21.qk21 ( f , a , b , result , abserr , defabs , resabs );
		dres = Math.abs ( result );
		errbnd = Math.max ( epsabs , epsrel * dres );
		last = 1;
		rlist [ 1 -1 ] = result;
		elist [ 1 -1 ] = abserr;
		iord [ 1 -1 ] = 1;
		if ( abserr <= 100 * epmach * defabs && abserr > errbnd ) ier = 2;
		if ( limit == 1 ) ier = 1;
		if ( ier != 0 || ( abserr <= errbnd && abserr != resabs ) || abserr == 0 )
		{
			neval = 42*last-21;
			return;
		}
		rlist2 [ 1 -1 ] = result;
		errmax = abserr;
		maxerr = 1;
		area = result;
		errsum = abserr;
		abserr = oflow;
		nrmax = 1;
		nres = 0;
		numrl2 = 2;
		ktmin = 0;
		extrap = false;
		noext = false;
		iroff1 = 0;
		iroff2 = 0;
		iroff3 = 0;
		ksgn = -1;
		if ( dres >= ( 1 - 50 * epmach ) * defabs ) ksgn = 1;
		for ( last = 2 ; last <= limit ; last += 1 )
		{
			a1 = alist [ maxerr -1 ];
			b1 = 0.5 * ( alist [ maxerr -1 ] + blist [ maxerr -1 ] );
			a2 = b1;
			b2 = blist [ maxerr -1 ];
			erlast = errmax;
			qk21.qk21 ( f , a1 , b1 , area1 , error1 , resabs , defab1 );
			qk21.qk21 ( f , a2 , b2 , area2 , error2 , resabs , defab2 );
			area12 = area1+area2;
			erro12 = error1+error2;
			errsum = errsum+erro12-errmax;
			area = area + area12 - rlist [ maxerr -1 ];
			if ( ! ( defab1 == error1 || defab2 == error2 ) )
			{
				if ( ! ( Math.abs ( rlist [ maxerr -1 ] - area12 ) > 1e-5 * Math.abs ( area12 ) || erro12 < 0.99 * errmax ) )
				{
					if ( extrap ) iroff2 = iroff2 + 1;
					if ( ! extrap ) iroff1 = iroff1 + 1;
				}
				if ( last > 10 && erro12 > errmax ) iroff3 = iroff3 + 1;
			}
			rlist [ maxerr -1 ] = area1;
			rlist [ last -1 ] = area2;
			errbnd = Math.max ( epsabs , epsrel * Math.abs ( area ) );
			if ( iroff1 + iroff2 >= 10 || iroff3 >= 20 ) ier = 2;
			if ( iroff2 >= 5 ) ierro = 3;
			if ( last == limit ) ier = 1;
			if ( Math.max ( Math.abs ( a1 ) , Math.abs ( b2 ) ) <= ( 1 + 100 * epmach ) * ( Math.abs ( a2 ) + 1000 * uflow ) ) ier = 4;
			if ( ! ( error2 > error1 ) )
			{
				alist [ last -1 ] = a2;
				blist [ maxerr -1 ] = b1;
				blist [ last -1 ] = b2;
				elist [ maxerr -1 ] = error1;
				elist [ last -1 ] = error2;
			}
			else
			{
				alist [ maxerr -1 ] = a2;
				alist [ last -1 ] = a1;
				blist [ last -1 ] = b1;
				rlist [ maxerr -1 ] = area2;
				rlist [ last -1 ] = area1;
				elist [ maxerr -1 ] = error2;
				elist [ last -1 ] = error1;
			}
			qpsrt ( limit , last , maxerr , errmax , elist , iord , nrmax );
			if ( errsum <= errbnd )
			{
				result = 0;
				for ( k = 1 ; k <= last ; k += 1 )
				{
					result = result + rlist [ k -1 ];
				}
				abserr = errsum;
				if ( ier > 2 ) ier = ier - 1;
				neval = 42*last-21;
				return;
			}
			if ( ier != 0 ) break;
			if ( ! ( last == 2 ) )
			{
				if ( noext ) break;
				erlarg = erlarg-erlast;
				if ( Math.abs ( b1 - a1 ) > small ) erlarg = erlarg + erro12;
				if ( ! ( extrap ) )
				{
					if ( Math.abs ( blist [ maxerr -1 ] - alist [ maxerr -1 ] ) > small ) break;
					extrap = true;
					nrmax = 2;
				}
				if ( ! ( ierro == 3 || erlarg <= ertest ) )
				{
					id = nrmax;
					jupbnd = last;
					if ( last > ( 2 + limit / 2 ) ) jupbnd = limit + 3 - last;
					boolean goto90 = false;
					for ( k = id ; k <= jupbnd ; k += 1 )
					{
						maxerr = iord [ nrmax -1 ];
						errmax = elist [ maxerr -1 ];
						if ( Math.abs ( blist [ maxerr -1 ] - alist [ maxerr -1 ] ) > small )
						{
							goto90 = true;
							break;
						}
						nrmax = nrmax+1;
					}
					if ( goto90 ) break;
				}
				numrl2 = numrl2+1;
				rlist2 [ numrl2 -1 ] = area;
				qelg ( numrl2 , rlist2 , reseps , abseps , res3la , nres ) ktmin = ktmin+1;
				if ( ktmin > 5 && abserr < 1e-3 * errsum ) ier = 5;
				if ( ! ( abseps >= abserr ) )
				{
					ktmin = 0;
					abserr = abseps;
					result = reseps;
					correc = erlarg;
					ertest = Math.max ( epsabs , epsrel * Math.abs ( reseps ) );
					if ( abserr <= ertest ) break;
				}
				if ( numrl2 == 1 ) noext = true if ( ier == 5 ) break;
				maxerr = iord [ 1 -1 ];
				errmax = elist [ maxerr -1 ];
				nrmax = 1;
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
		if ( abserr == oflow )
		{
			result = 0;
			for ( k = 1 ; k <= last ; k += 1 )
			{
				result = result + rlist [ k -1 ];
			}
			abserr = errsum;
			if ( ier > 2 ) ier = ier - 1;
			neval = 42*last-21;
			return;
		}
		if ( ier + ierro == 0 )
		{
			if ( ierro == 3 ) abserr = abserr + correc;
			if ( ier == 0 ) ier = 3;
			if ( result != 0 && area != 0 )
			{
				if ( abserr / Math.abs ( result ) > errsum / Math.abs ( area ) )
				{
					result = 0;
					for ( k = 1 ; k <= last ; k += 1 )
					{
						result = result + rlist [ k -1 ];
					}
					abserr = errsum;
					if ( ier > 2 ) ier = ier - 1;
					neval = 42*last-21;
					return;
				}
				if ( ksgn == ( - 1 ) && Math.max ( Math.abs ( result ) , Math.abs ( area ) ) <= defabs * 0.01 )
				{
					if ( ier > 2 ) ier = ier - 1;
					neval = 42*last-21;
					return;
				}
				if ( 0.01 > ( result / area ) || ( result / area ) > 100 || errsum > Math.abs ( area ) ) ier = 6;
				if ( ier > 2 ) ier = ier - 1;
				neval = 42*last-21;
				return;
				result = 0;
				for ( k = 1 ; k <= last ; k += 1 )
				{
					result = result + rlist [ k -1 ];
				}
				abserr = errsum;
				if ( ier > 2 ) ier = ier - 1;
				neval = 42*last-21;
				return;
			}
			if ( abserr > errsum )
			{
				result = 0;
				for ( k = 1 ; k <= last ; k += 1 )
				{
					result = result + rlist [ k -1 ];
				}
				abserr = errsum;
				if ( ier > 2 ) ier = ier - 1;
				neval = 42*last-21;
				return;
			}
			if ( area == 0 )
			{
				if ( ier > 2 ) ier = ier - 1;
				neval = 42*last-21;
				return;
			}
		}
		if ( ksgn == ( - 1 ) && Math.max ( Math.abs ( result ) , Math.abs ( area ) ) <= defabs * 0.01 )
		{
			if ( ier > 2 ) ier = ier - 1;
			neval = 42*last-21;
			return;
		}
		if ( 0.01 > ( result / area ) || ( result / area ) > 100 || errsum > Math.abs ( area ) ) ier = 6;
		if ( ier > 2 ) ier = ier - 1;
		neval = 42*last-21;
		return;
		result = 0;
		for ( k = 1 ; k <= last ; k += 1 )
		{
			result = result + rlist [ k -1 ];
		}
		abserr = errsum;
		if ( ier > 2 ) ier = ier - 1;
		neval = 42*last-21;
		return;
	}
}
