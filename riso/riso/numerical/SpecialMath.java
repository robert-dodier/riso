package numerical;

/** A couple of the functions from the SpecialMath function library by
  * Mark Hale (http://www.ph.ic.ac.uk/people/halemj). These are the
  * only functions known to be reliable. The incomplete gamma and
  * incomplete beta functions in SpecialMath are known to be erroneous,
  * by comparison of computed F and chi-square values to tables in
  * Mendenhall's _Introduction to Probability and Statistics_.
  * <p>
  * The apparent lack of quality control in the SpecialMath functions
  * casts doubt on the reliability of the rest of the JavaSci software.
  * <p>
  * As no specific mention of copyrights are mentioned in the original
  * JavaSci source or documentation, it is assumed that Hale retains
  * the copyright to this code, but that there are no restrictions or
  * licensing fees for using this code.
  *
  * @author Mark Hale, Jaco van Kooten
  */
public class SpecialMath
{
// ====================================================
// Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
//
// Developed at SunSoft, a Sun Microsystems, Inc. business.
// Permission to use, copy, modify, and distribute this
// software is freely granted, provided that this notice 
// is preserved.
// ====================================================
//
//			     x
//		      2      |\
//     erf(x)  =  ---------  | exp(-t*t)dt
//	 	   sqrt(pi) \| 
//			     0
//
//     erfc(x) =  1-erf(x)
//  Note that 
//		erf(-x) = -erf(x)
//		erfc(-x) = 2 - erfc(x)
//
// Method:
//	1. For |x| in [0, 0.84375]
//	    erf(x)  = x + x*R(x^2)
//          erfc(x) = 1 - erf(x)           if x in [-.84375,0.25]
//                  = 0.5 + ((0.5-x)-x*R)  if x in [0.25,0.84375]
//	   where R = P/Q where P is an odd poly of degree 8 and
//	   Q is an odd poly of degree 10.
//						 -57.90
//			| R - (erf(x)-x)/x | <= 2
//	
//
//	   Remark. The formula is derived by noting
//          erf(x) = (2/sqrt(pi))*(x - x^3/3 + x^5/10 - x^7/42 + ....)
//	   and that
//          2/sqrt(pi) = 1.128379167095512573896158903121545171688
//	   is close to one. The interval is chosen because the fix
//	   point of erf(x) is near 0.6174 (i.e., erf(x)=x when x is
//	   near 0.6174), and by some experiment, 0.84375 is chosen to
// 	   guarantee the error is less than one ulp for erf.
//
//      2. For |x| in [0.84375,1.25], let s = |x| - 1, and
//         c = 0.84506291151 rounded to single (24 bits)
//         	erf(x)  = sign(x) * (c  + P1(s)/Q1(s))
//         	erfc(x) = (1-c)  - P1(s)/Q1(s) if x > 0
//			  1+(c+P1(s)/Q1(s))    if x < 0
//         	|P1/Q1 - (erf(|x|)-c)| <= 2**-59.06
//	   Remark: here we use the taylor series expansion at x=1.
//		erf(1+s) = erf(1) + s*Poly(s)
//			 = 0.845.. + P1(s)/Q1(s)
//	   That is, we use rational approximation to approximate
//			erf(1+s) - (c = (single)0.84506291151)
//	   Note that |P1/Q1|< 0.078 for x in [0.84375,1.25]
//	   where 
//		P1(s) = degree 6 poly in s
//		Q1(s) = degree 6 poly in s
//
//      3. For x in [1.25,1/0.35(~2.857143)], 
//         	erfc(x) = (1/x)*exp(-x*x-0.5625+R1/S1)
//         	erf(x)  = 1 - erfc(x)
//	   where 
//		R1(z) = degree 7 poly in z, (z=1/x^2)
//		S1(z) = degree 8 poly in z
//
//      4. For x in [1/0.35,28]
//         	erfc(x) = (1/x)*exp(-x*x-0.5625+R2/S2) if x > 0
//			= 2.0 - (1/x)*exp(-x*x-0.5625+R2/S2) if -6<x<0
//			= 2.0 - tiny		(if x <= -6)
//         	erf(x)  = sign(x)*(1.0 - erfc(x)) if x < 6, else
//         	erf(x)  = sign(x)*(1.0 - tiny)
//	   where
//		R2(z) = degree 6 poly in z, (z=1/x^2)
//		S2(z) = degree 7 poly in z
//
//      Note1:
//	   To compute exp(-x*x-0.5625+R/S), let s be a single
//	   precision number and s := x; then
//		-x*x = -s*s + (s-x)*(s+x)
//	        exp(-x*x-0.5626+R/S) = 
//			exp(-s*s-0.5625)*exp((s-x)*(s+x)+R/S);
//      Note2:
//	   Here 4 and 5 make use of the asymptotic series
//			  exp(-x*x)
//		erfc(x) ~ ---------- * ( 1 + Poly(1/x^2) )
//			  x*sqrt(pi)
//	   We use rational approximation to approximate
//      	g(s)=f(1/x^2) = log(erfc(x)*x) - x*x + 0.5625
//	   Here is the error bound for R1/S1 and R2/S2
//      	|R1/S1 - f(x)|  < 2**(-62.57)
//      	|R2/S2 - f(x)|  < 2**(-61.52)
//
//      5. For inf > x >= 28
//         	erf(x)  = sign(x) *(1 - tiny)  (raise inexact)
//         	erfc(x) = tiny*tiny (raise underflow) if x > 0
//			= 2 - tiny if x<0
//
//      7. Special case:
//         	erf(0)  = 0, erf(inf)  = 1, erf(-inf) = -1,
//         	erfc(0) = 1, erfc(inf) = 0, erfc(-inf) = 2, 
//	   	erfc/erf(NaN) is NaN
//

	/**
        * Error function.
        * Based on C-code for the error function developed at Sun Microsystems.
        * @author Jaco van Kooten
	*/
        public static double error(double x) {
// Coefficients for approximation to  erf on [0,0.84375]
                double e_efx=1.28379167095512586316e-01;
	  	//double efx8=1.02703333676410069053e00;
	  	double ePp[]={
                        1.28379167095512558561e-01,
                        -3.25042107247001499370e-01,
                        -2.84817495755985104766e-02,
                        -5.77027029648944159157e-03,
                        -2.37630166566501626084e-05};
                double eQq[]={
                        3.97917223959155352819e-01,
                        6.50222499887672944485e-02,
                        5.08130628187576562776e-03,
                        1.32494738004321644526e-04,
                        -3.96022827877536812320e-06};
// Coefficients for approximation to  erf  in [0.84375,1.25] 
                double ePa[]={
                        -2.36211856075265944077e-03,
                        4.14856118683748331666e-01,
                        -3.72207876035701323847e-01,
                        3.18346619901161753674e-01,
                        -1.10894694282396677476e-01,
                        3.54783043256182359371e-02,
                        -2.16637559486879084300e-03};
                double eQa[]={
                        1.06420880400844228286e-01,
                        5.40397917702171048937e-01,
                        7.18286544141962662868e-02,
                        1.26171219808761642112e-01,
                        1.36370839120290507362e-02,
                        1.19844998467991074170e-02};
 	  	double e_erx=8.45062911510467529297e-01;

        	double P,Q,s,retval;
        	double abs_x = (x >= 0.0 ? x : -x);
                if ( abs_x < 0.84375 ) {                               // 0 < |x| < 0.84375
                        if (abs_x < 3.7252902984619141e-9 )     // |x| < 2**-28
                                retval = abs_x + abs_x*e_efx;
                        else {
        			s = x*x;
         			P = ePp[0]+s*(ePp[1]+s*(ePp[2]+s*(ePp[3]+s*ePp[4])));
        			Q = 1.0+s*(eQq[0]+s*(eQq[1]+s*(eQq[2]+s*(eQq[3]+s*eQq[4]))));
        			retval = abs_x + abs_x*(P/Q);
           		}
        	} else if (abs_x < 1.25) {                             // 0.84375 < |x| < 1.25
        		s = abs_x-1.0;
                        P = ePa[0]+s*(ePa[1]+s*(ePa[2]+s*(ePa[3]+s*(ePa[4]+s*(ePa[5]+s*ePa[6])))));
                        Q = 1.0+s*(eQa[0]+s*(eQa[1]+s*(eQa[2]+s*(eQa[3]+s*(eQa[4]+s*eQa[5])))));
                        retval = e_erx + P/Q;
	       } else if (abs_x >= 6.0) 
                        retval = 1.0;
                else                                                    // 1.25 < |x| < 6.0 
                        retval = 1.0-cError(abs_x);
                return (x >= 0.0) ? retval : -retval;
        }
	/**
        * Complementary error function.
        * Based on C-code for the error function developed at Sun Microsystems.
        * @author Jaco van Kooten
	*/
	public static double cError(double x) {
// Coefficients for approximation to  erfc in [1.25,1/.35]
                double eRa[]={
                        -9.86494403484714822705e-03,
                        -6.93858572707181764372e-01,
                        -1.05586262253232909814e01, 
                        -6.23753324503260060396e01, 
                        -1.62396669462573470355e02, 
                        -1.84605092906711035994e02, 
                        -8.12874355063065934246e01, 
                        -9.81432934416914548592e00};
                double eSa[]={
                        1.96512716674392571292e01, 
                        1.37657754143519042600e02,
                        4.34565877475229228821e02,
                        6.45387271733267880336e02,
                        4.29008140027567833386e02,
                        1.08635005541779435134e02,
                        6.57024977031928170135e00,
                        -6.04244152148580987438e-02};
// Coefficients for approximation to  erfc in [1/.35,28]
                double eRb[]={
                        -9.86494292470009928597e-03, 
                        -7.99283237680523006574e-01, 
                        -1.77579549177547519889e01, 
                        -1.60636384855821916062e02, 
                        -6.37566443368389627722e02, 
                        -1.02509513161107724954e03, 
                        -4.83519191608651397019e02};
                double eSb[]={
                        3.03380607434824582924e01, 
                        3.25792512996573918826e02, 
                        1.53672958608443695994e03,
                        3.19985821950859553908e03,
                        2.55305040643316442583e03,
                        4.74528541206955367215e02,
                        -2.24409524465858183362e01};

          	double s,retval,R,S;
                double abs_x =(x>=0.0 ? x : -x);
                if (abs_x < 1.25)
        		retval = 1.0-error(abs_x);
                else if (abs_x > 28.0)
                        retval=0.0;
                else {						// 1.25 < |x| < 28 
                        s = 1.0/(abs_x*abs_x);
                        if (abs_x < 2.8571428) {                // ( |x| < 1/0.35 ) 
                                R=eRa[0]+s*(eRa[1]+s*(eRa[2]+s*(eRa[3]+s*(eRa[4]+s*(eRa[5]+s*(eRa[6]+s*eRa[7]))))));
        	    		S=1.0+s*(eSa[0]+s*(eSa[1]+s*(eSa[2]+s*(eSa[3]+s*(eSa[4]+s*(eSa[5]+s*(eSa[6]+s*eSa[7])))))));
        		} else {	  				// ( |x| > 1/0.35 )
                                R=eRb[0]+s*(eRb[1]+s*(eRb[2]+s*(eRb[3]+s*(eRb[4]+s*(eRb[5]+s*eRb[6])))));
        	    		S=1.0+s*(eSb[0]+s*(eSb[1]+s*(eSb[2]+s*(eSb[3]+s*(eSb[4]+s*(eSb[5]+s*eSb[6]))))));
 			}
                        retval =  Math.exp(-x*x - 0.5625 + R/S)/abs_x;
                }
                return (x >= 0.0) ? retval : 2.0-retval;
	}
}

