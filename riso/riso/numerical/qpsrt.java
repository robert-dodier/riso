package riso.numerical;

/** qagse.java, qags.java, qelg.java, qk21.java, and qpsrt.java are
  * derivative works (translations) of Fortran code by Robert Piessens
  * and Elise de Doncker. The web site (www.netlib.org) from which I 
  * obtained the source code, and the source code itself does not state
  * any license. I have emailed (Jan. 26, 2002) Profs. Piessens and Doncker
  * to ask about license terms, but I haven't received a response. 
  * The terms under which these five files are released with RISO are
  * the same as the terms under they are released from Netlib, whatever
  * those terms may be.
  */
public class qpsrt implements java.io.Serializable
{
	public static void do_qpsrt ( int limit, int last, int[] maxerr, double[] ermax, double[] elist, int[] iord, int[] nrmax )
	{
		double errmax, errmin;
		int i = 0, ibeg, ido, isucc, j, jbnd, jupbn, k;
		if ( ! ( last > 2 ) )
		{
			iord [ 1 -1 ] = 1;
			iord [ 2 -1 ] = 2;
			maxerr[0] = iord [ nrmax[0] -1 ];
			ermax[0] = elist [ maxerr[0] -1 ];
			return;
		}
		errmax = elist [ maxerr[0] -1 ];
		if ( ! ( nrmax[0] == 1 ) )
		{
			ido = nrmax[0]-1;
			for ( i = 1 ; i <= ido ; i += 1 )
			{
				isucc = iord [ nrmax[0] - 1 -1 ];
				if ( errmax <= elist [ isucc -1 ] ) break;
				iord [ nrmax[0] -1 ] = isucc;
				nrmax[0] = nrmax[0]-1;
			}
		}
		jupbn = last;
		if ( last > ( limit / 2 + 2 ) ) jupbn = limit + 3 - last;
		errmin = elist [ last -1 ];
		jbnd = jupbn-1;
		ibeg = nrmax[0]+1;
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
			iord [ jbnd -1 ] = maxerr[0];
			iord [ jupbn -1 ] = last;
			maxerr[0] = iord [ nrmax[0] -1 ];
			ermax[0] = elist [ maxerr[0] -1 ];
			return;
		}
		iord [ i - 1 -1 ] = maxerr[0];
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
			maxerr[0] = iord [ nrmax[0] -1 ];
			ermax[0] = elist [ maxerr[0] -1 ];
			return;
		}
		iord [ k + 1 -1 ] = last;
		maxerr[0] = iord [ nrmax[0] -1 ];
		ermax[0] = elist [ maxerr[0] -1 ];
		return;
	}
}
