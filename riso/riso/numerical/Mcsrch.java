package numerical;

public class Mcsrch
{
	public static int mp = 6, lp = 6;
	public static double gtol = 0.9, stpmin = 1e-20, stpmax = 1e20;

	private static int infoc[] = new int[1], j = 0;
	private static double dg = 0, dgm = 0, dginit = 0, dgtest = 0, dgx[] = new double[1], dgxm[] = new double[1], dgy[] = new double[1], dgym[] = new double[1], finit = 0, ftest1 = 0, fm = 0, fx[] = new double[1], fxm[] = new double[1], fy[] = new double[1], fym[] = new double[1], p5 = 0, p66 = 0, stx[] = new double[1], sty[] = new double[1], stmin = 0, stmax = 0, width = 0, width1 = 0, xtrapf = 0;
	private static boolean brackt[] = new boolean[1], stage1 = false;

	/** Minimize a function along a search direction. This code is
	  * a Java translation of the function <code>MCSRCH</code> from
	  * <code>lbfgs.f</code>, which in turn is a slight modification of
	  * the subroutine <code>CSRCH</code> of More' and Thuente.
	  * The changes are to allow reverse communication, and do not affect
	  * the performance of the routine. This function, in turn, calls
	  * <code>mcstep</code>.<p>
	  *
	  * The Java translation was effected mostly mechanically, with some
	  * manual clean-up; in particular, array indices start at 0 instead of 1.
	  * Most of the comments from the Fortran code have been pasted in here
	  * as well.<p>
	  *
	  * The purpose of <code>mcsrch</code> is to find a step which satisfies
	  * a sufficient decrease condition and a curvature condition.<p>
	  *
	  * At each stage this function updates an interval of uncertainty with
	  * endpoints <code>stx</code> and <code>sty</code>. The interval of
	  * uncertainty is initially chosen so that it contains a
	  * minimizer of the modified function
	  * <pre>
	  *      f(x+stp*s) - f(x) - ftol*stp*(gradf(x)'s).
	  * </pre>
	  * If a step is obtained for which the modified function
	  * has a nonpositive function value and nonnegative derivative,
	  * then the interval of uncertainty is chosen so that it
	  * contains a minimizer of <code>f(x+stp*s)</code>.<p>
	  *
	  * The algorithm is designed to find a step which satisfies
	  * the sufficient decrease condition
	  * <pre>
	  *       f(x+stp*s) &lt;= f(X) + ftol*stp*(gradf(x)'s),
	  * </pre>
	  * and the curvature condition
	  * <pre>
	  *       abs(gradf(x+stp*s)'s)) &lt;= gtol*abs(gradf(x)'s).
	  * </pre>
	  * If <code>ftol</code> is less than <code>gtol</code> and if, for example,
	  * the function is bounded below, then there is always a step which
	  * satisfies both conditions. If no step can be found which satisfies both
	  * conditions, then the algorithm usually stops when rounding
	  * errors prevent further progress. In this case <code>stp</code> only
	  * satisfies the sufficient decrease condition.<p>
	  *
	  * @author Original Fortran version by Jorge J. More' and David J. Thuente
	  *		as part of the Minpack project, June 1983, Argonne National 
	  *		Laboratory.<p>
	  * @author Java translation by Robert Dodier, August 1997.
	  *
	  * @param n The number of variables.<p>
	  *
	  * @param x On entry this contains the base point for the line search.
	  *		On exit it contains <code>x + stp*s</code>.<p>
	  *
	  * @param f On entry this contains the value of the objective function
	  *		at <code>x</code>. On exit it contains the value of the objective
	  *		function at <code>x + stp*s</code>.<p>
	  *
	  * @param g On entry this contains the gradient of the objective function
	  *		at <code>x</code>. On exit it contains the gradient at
	  *		<code>x + stp*s</code>.<p>
	  *
	  *	@param s The search direction.<p>
	  *
	  * @param stp On entry this contains an initial estimate of a satifactory
	  *		step length. On exit <code>stp</code> contains the final estimate.<p>
	  *
	  *	@param ftol Tolerance for the sufficient decrease condition.<p>
	  *
	  * @param xtol Termination occurs when the relative width of the interval
	  *		of uncertainty is at most <code>xtol</code>.<p>
	  *
	  *	@param maxfev Termination occurs when the number of evaluations of
	  *		the objective function is at least <code>maxfev</code> by the end
	  *		of an iteration.<p>
	  *
	  *	@param info This is an output variable, which can have these values:
	  *		<ul>
	  *		<li><code>info = 0</code> Improper input parameters.
	  *		<li><code>info = -1</code> A return is made to compute the function and gradient.
	  *		<li><code>info = 1</code> The sufficient decrease condition and
	  *			the directional derivative condition hold.
	  *		<li><code>info = 2</code> Relative width of the interval of uncertainty
	  *			is at most <code>xtol</code>.
	  *		<li><code>info = 3</code> Number of function evaluations has reached <code>maxfev</code>.
	  *		<li><code>info = 4</code> The step is at the lower bound <code>stpmin</code>.
	  *		<li><code>info = 5</code> The step is at the upper bound <code>stpmax</code>.
	  *		<li><code>info = 6</code> Rounding errors prevent further progress.
	  *			There may not be a step which satisfies the
	  *			sufficient decrease and curvature conditions.
	  *			Tolerances may be too small.
	  *		</ul><p>
	  *
	  *	@param nfev On exit, this is set to the number of function evaluations.<p>
	  *
	  *	@param wa Temporary storage array, of length <code>n</code>.<p>
	  */

	public static void mcsrch ( int n , double[] x , double f , double[] g , double[] s , int is0 , double[] stp , double ftol , double xtol , int maxfev , int[] info , int[] nfev , double[] wa )
	{
		p5 = 0.5;
		p66 = 0.66;
		xtrapf = 4;

		if ( info[0] != - 1 )
		{
			infoc[0] = 1;
			if ( n <= 0 || stp[0] <= 0 || ftol < 0 || gtol < 0 || xtol < 0 || stpmin < 0 || stpmax < stpmin || maxfev <= 0 ) 
				return;

			// Compute the initial gradient in the search direction
			// and check that s is a descent direction.

			dginit = 0;

			for ( j = 1 ; j <= n ; j += 1 )
			{
				dginit = dginit + g [ j -1] * s [ is0+j -1];
			}


			if ( dginit >= 0 )
			{
				System.out.println( "The search direction is not a descent direction." );
				return;
			}

			brackt[0] = false;
			stage1 = true;
			nfev[0] = 0;
			finit = f;
			dgtest = ftol*dginit;
			width = stpmax - stpmin;
			width1 = width/p5;

			for ( j = 1 ; j <= n ; j += 1 )
			{
				wa [ j -1] = x [ j -1];
			}

			// The variables stx, fx, dgx contain the values of the step,
			// function, and directional derivative at the best step.
			// The variables sty, fy, dgy contain the value of the step,
			// function, and derivative at the other endpoint of
			// the interval of uncertainty.
			// The variables stp, f, dg contain the values of the step,
			// function, and derivative at the current step.

			stx[0] = 0;
			fx[0] = finit;
			dgx[0] = dginit;
			sty[0] = 0;
			fy[0] = finit;
			dgy[0] = dginit;
		}

		while ( true )
		{
			if ( info[0] != -1 )
			{
				// Set the minimum and maximum steps to correspond
				// to the present interval of uncertainty.

				if ( brackt[0] )
				{
					stmin = Math.min ( stx[0] , sty[0] );
					stmax = Math.max ( stx[0] , sty[0] );
				}
				else
				{
					stmin = stx[0];
					stmax = stp[0] + xtrapf * ( stp[0] - stx[0] );
				}

				// Force the step to be within the bounds stpmax and stpmin.

				stp[0] = Math.max ( stp[0] , stpmin );
				stp[0] = Math.min ( stp[0] , stpmax );

				// If an unusual termination is to occur then let
				// stp be the lowest point obtained so far.

				if ( ( brackt[0] && ( stp[0] <= stmin || stp[0] >= stmax ) ) || nfev[0] >= maxfev - 1 || infoc[0] == 0 || ( brackt[0] && stmax - stmin <= xtol * stmax ) ) stp[0] = stx[0];

				// Evaluate the function and gradient at stp
				// and compute the directional derivative.
				// We return to main program to obtain F and G.

				for ( j = 1 ; j <= n ; j += 1 )
				{
					x [ j -1] = wa [ j -1] + stp[0] * s [ is0+j -1];
				}

				info[0]=-1;
				return;
			}

			info[0]=0;
			nfev[0] = nfev[0] + 1;
			dg = 0;

			for ( j = 1 ; j <= n ; j += 1 )
			{
				dg = dg + g [ j -1] * s [ is0+j -1];
			}

			ftest1 = finit + stp[0]*dgtest;

			// Test for convergence.

			if ( ( brackt[0] && ( stp[0] <= stmin || stp[0] >= stmax ) ) || infoc[0] == 0 ) info[0] = 6;

			if ( stp[0] == stpmax && f <= ftest1 && dg <= dgtest ) info[0] = 5;

			if ( stp[0] == stpmin && ( f > ftest1 || dg >= dgtest ) ) info[0] = 4;

			if ( nfev[0] >= maxfev ) info[0] = 3;

			if ( brackt[0] && stmax - stmin <= xtol * stmax ) info[0] = 2;

			if ( f <= ftest1 && Math.abs ( dg ) <= gtol * ( - dginit ) ) info[0] = 1;

			// Check for termination.

			if ( info[0] != 0 ) return;

			// In the first stage we seek a step for which the modified
			// function has a nonpositive value and nonnegative derivative.

			if ( stage1 && f <= ftest1 && dg >= Math.min ( ftol , gtol ) * dginit ) stage1 = false;

			// A modified function is used to predict the step only if
			// we have not obtained a step for which the modified
			// function has a nonpositive function value and nonnegative
			// derivative, and if a lower function value has been
			// obtained but the decrease is not sufficient.

			if ( stage1 && f <= fx[0] && f > ftest1 )
			{
				// Define the modified function and derivative values.

				fm = f - stp[0]*dgtest;
				fxm[0] = fx[0] - stx[0]*dgtest;
				fym[0] = fy[0] - sty[0]*dgtest;
				dgm = dg - dgtest;
				dgxm[0] = dgx[0] - dgtest;
				dgym[0] = dgy[0] - dgtest;

				// Call cstep to update the interval of uncertainty
				// and to compute the new step.

				Mcstep.mcstep ( stx , fxm , dgxm , sty , fym , dgym , stp , fm , dgm , brackt , stmin , stmax , infoc );

				// Reset the function and gradient values for f.

				fx[0] = fxm[0] + stx[0]*dgtest;
				fy[0] = fym[0] + sty[0]*dgtest;
				dgx[0] = dgxm[0] + dgtest;
				dgy[0] = dgym[0] + dgtest;
			}
			else
			{
				// Call mcstep to update the interval of uncertainty
				// and to compute the new step.

				Mcstep.mcstep ( stx , fx , dgx , sty , fy , dgy , stp , f , dg , brackt , stmin , stmax , infoc );
			}

			// Force a sufficient decrease in the size of the
			// interval of uncertainty.

			if ( brackt[0] )
			{
				if ( Math.abs ( sty[0] - stx[0] ) >= p66 * width1 ) stp[0] = stx[0] + p5 * ( sty[0] - stx[0] );
				width1 = width;
				width = Math.abs ( sty[0] - stx[0] );
			}
		}
	}
}
