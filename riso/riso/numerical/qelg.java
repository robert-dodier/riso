public class qelg
{
	public void qelg ( int n , double[] epstab , double result , double abserr , double[] res3la , int nres )  {
	double delta1, delta2, delta3, epmach, epsinf = -1, error, err1, err2, err3, e0, e1, e1abs, e2, e3, oflow, res, ss = -1, tol1, tol2, tol3;
	int i, ib, ib2, ie, indx, k1, k2, k3, limexp, newelm, num;

	epmach  =  qk21.D1MACH [ 4-1 ] ;
	oflow  =  qk21.D1MACH [ 2-1 ] ;
	nres = nres+1;
	abserr = oflow;
result = epstab [ n -1 ];
	if  ( n < 3 )
	{
  abserr  =  Math.max ( abserr , 5 * epmach * Math.abs ( result )  ) ;
	return;
	}
	limexp = 50;
epstab [ n + 2 -1 ] = epstab [ n -1 ];
	newelm  =   ( n - 1 )  / 2;
epstab [ n -1 ] = oflow;
	num = n;
	k1 = n;
	for  (  i  =  1; i <=  newelm; i  +=  1  )  {
	  k2 = k1-1;
	  k3 = k1-2;
res = epstab [ k1 + 2 -1 ];
e0 = epstab [ k3 -1 ];
e1 = epstab [ k2 -1 ];
	  e2 = res;
	  e1abs  =  Math.abs ( e1 ) ;
	  delta2 = e2-e1;
	  err2  =  Math.abs ( delta2 ) ;
	  tol2  =  Math.max ( Math.abs ( e2 )  , e1abs )  * epmach;
	  delta3 = e1-e0;
	  err3  =  Math.abs ( delta3 ) ;
	  tol3  =  Math.max ( e1abs , Math.abs ( e0 )  )  * epmach;
	  if  ( ! ( err2 > tol2 || err3 > tol3 )  )
	  {
	  result = res;
	  abserr = err2+err3;
  abserr  =  Math.max ( abserr , 5 * epmach * Math.abs ( result )  ) ;
	return;
	}
	e3 = epstab [ k1 -1 ];
epstab [ k1 -1 ] = e1;
	  delta1 = e1-e3;
	  err1  =  Math.abs ( delta1 ) ;
	  tol1  =  Math.max ( e1abs , Math.abs ( e3 )  )  * epmach;
	  boolean goto20 = false;
	  if  ( err1 <=  tol1 || err2 <=  tol2 || err3 <=  tol3 )
	  {
		goto20 = true;
	}
	else
	{
	  ss = 1/delta1+1/delta2-1/delta3;
	  epsinf  =  Math.abs ( ss * e1 ) ;
	}
	  if  ( goto20 || !(epsinf > 1e-4) )
	  {
   n = i+i-1;
	  }
	  else
	  {
   res = e1+1/ss;
epstab [ k1 -1 ] = res;
	  k1 = k1-2;
	  error  =  err2 + Math.abs ( res - e2 )  + err3;
	  if  ( error > abserr )  break;
	  abserr = error;
	  result = res;
   }
   }
    if  ( n  ==  limexp )  n  =  2 *  ( limexp / 2 )  - 1;
	ib = 1;
	if  (  ( num / 2 )  * 2  ==  num )  ib  =  2;
	ie = newelm+1;
	for  (  i  =  1; i <=  ie; i  +=  1  )  {
	  ib2 = ib+2;
epstab [ ib -1 ] = epstab [ ib2 -1 ];
	  ib = ib2;
   }
	if  ( ! ( num  ==  n ) )
	{
	indx = num-n+1;
	for  (  i  =  1; i <=  n; i  +=  1  )  {
epstab [ i -1 ] = epstab [ indx -1 ];
	  indx = indx+1;
   }
   } if ( ! ( nres >=  4 ) )
   {
res3la [ nres -1 ] = result;
	abserr = oflow;
  abserr  =  Math.max ( abserr , 5 * epmach * Math.abs ( result )  ) ;
	return;
}
abserr = Math.abs ( result - res3la [ 3 -1 ] ) + Math.abs ( result - res3la [ 2 -1 ] ) + Math.abs ( result - res3la [ 1 -1 ] );
res3la [ 1 -1 ] = res3la [ 2 -1 ];
res3la [ 2 -1 ] = res3la [ 3 -1 ];
res3la [ 3 -1 ] = result;
  
  abserr  =  Math.max ( abserr , 5 * epmach * Math.abs ( result )  ) ;
	return;
	} 
}
