
public class qpsrt
{
	int limit, last, maxerr, nrmax;
	int [ ] iord;
	double ermax;
	double [ ] elist;

	public void dqpsrt ( )
	{
		double errmax, errmin;
		int i = 0, ibeg, ido, isucc, j, jbnd, jupbn, k;
		if ( ! ( last > 2 ) )
		{
			iord [ 1 -1 ] = 1;
			iord [ 2 -1 ] = 2;
			maxerr = iord [ nrmax -1 ];
			ermax = elist [ maxerr -1 ];
			return;
		}
		errmax = elist [ maxerr -1 ];
		if ( ! ( nrmax == 1 ) )
		{
			ido = nrmax-1;
			for ( i = 1 ; i <= ido ; i += 1 )
			{
				isucc = iord [ nrmax - 1 -1 ];
				if ( errmax <= elist [ isucc -1 ] ) break;
				iord [ nrmax -1 ] = isucc;
				nrmax = nrmax-1;
			}
		}
		jupbn = last;
		if ( last > ( limit / 2 + 2 ) ) jupbn = limit + 3 - last;
		errmin = elist [ last -1 ];
		jbnd = jupbn-1;
		ibeg = nrmax+1;
		boolean goto60 = false;
		if ( ! ( ibeg > jbnd ) )
		{
			for ( i = ibeg ; i <= jbnd ; i += 1 )
			{
				isucc = iord [ i -1 ];
				if ( errmax >= elist [ isucc -1 ] )
				{
					goto60 = true;
					break;
				}
				iord [ i - 1 -1 ] = isucc;
			}
		}
		if ( !goto60 )
		{
			iord [ jbnd -1 ] = maxerr;
			iord [ jupbn -1 ] = last;
			maxerr = iord [ nrmax -1 ];
			ermax = elist [ maxerr -1 ];
			return;
		}
		iord [ i - 1 -1 ] = maxerr;
		k = jbnd;
		boolean goto80 = false;
		for ( j = i ; j <= jbnd ; j += 1 )
		{
			isucc = iord [ k -1 ];
			if ( errmin < elist [ isucc -1 ] )
			{
				goto80 = true;
				break;
			}
			iord [ k + 1 -1 ] = isucc;
			k = k-1;
		}
		if ( !goto80 )
		{
			iord [ i -1 ] = last;
			maxerr = iord [ nrmax -1 ];
			ermax = elist [ maxerr -1 ];
			return;
		}
		iord [ k + 1 -1 ] = last;
		maxerr = iord [ nrmax -1 ];
		ermax = elist [ maxerr -1 ];
		return;
	}
}
