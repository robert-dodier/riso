/** This class contains code for the limited-memory Broyden-Fletcher-Goldfarb-Shanno
  * (LBFGS) algorithm for large-scale multidimensional minimization problems. 
  * 
  * @author Jorge Nocedal wrote the original Fortran version, including comments
  * (July 1990). Robert Dodier translated to Java (August 1997).
  */

package numerical;

public class LBFGS
{
	/* Specialized exception class for LBFGS; contains the
	 * <code>iflag</code> value returned by <code>lbfgs</code>.
	 */

	public static class ExceptionWithIflag extends Exception
	{
		public int iflag;
		public ExceptionWithIflag( int i, String s ) { super(s); iflag = i; }
		public String toString() { return ((Exception)this).toString()+" (iflag == "+iflag+")"; }
	}

	/** Controls the accuracy of the line search <code>mcsrch</code>. If the
	  * function and gradient evaluations are inexpensive with respect
	  * to the cost of the iteration (which is sometimes the case when
	  * solving very large problems) it may be advantageous to set <code>gtol</code>
	  * to a small value. A typical small value is 0.1.  Restriction:
	  * <code>gtol</code> should be greater than 1e-4.
	  */

	public static double gtol = 0.9;

	/** Specify lower bound for the step in the line search.
	  * The default value is 1e-20. This value need not be modified unless
	  * the exponent is too large for the machine being used, or unless
	  * the problem is extremely badly scaled (in which case the exponent
	  * should be increased).
	  */

	public static double stpmin = 1e-20;

	/** Specify upper bound for the step in the line search.
	  * The default value is 1e20. This value need not be modified unless
	  * the exponent is too large for the machine being used, or unless
	  * the problem is extremely badly scaled (in which case the exponent
	  * should be increased).
	  */

	public static double stpmax = 1e20;

	private static double gnorm = 0, stp1 = 0, ftol = 0, stp[] = new double[1], ys = 0, yy = 0, sq = 0, yr = 0, beta = 0, xnorm = 0;
	private static int iter = 0, nfun = 0, point = 0, ispt = 0, iypt = 0, maxfev = 0, info[] = new int[1], bound = 0, npt = 0, cp = 0, i = 0, nfev[] = new int[1], inmc = 0, iycn = 0, iscn = 0;
	private static boolean finish = false;

	private static double[] w = null;

	// iflag is modified !!!

	/** This subroutine solves the unconstrained minimization problem
	  * <pre>
	  *     min f(x),    x = (x1,x2,...,x_n),
	  * </pre>
	  * using the limited-memory BFGS method. The routine is especially
	  * effective on problems involving a large number of variables. In
	  * a typical iteration of this method an approximation <code>Hk</code> to the
	  * inverse of the Hessian is obtained by applying <code>m</code> BFGS updates to
	  * a diagonal matrix <code>Hk0</code>, using information from the previous M steps.
	  * The user specifies the number <code>m</code>, which determines the amount of
	  * storage required by the routine. The user may also provide the
	  * diagonal matrices <code>Hk0</code> if not satisfied with the default choice.
	  * The algorithm is described in "On the limited memory BFGS method
	  * for large scale optimization", by D. Liu and J. Nocedal,
	  * Mathematical Programming B 45 (1989) 503-528.
	  *
	  * The user is required to calculate the function value <code>f</code> and its
	  * gradient <code>g</code>. In order to allow the user complete control over
	  * these computations, reverse  communication is used. The routine
	  * must be called repeatedly under the control of the parameter
	  * <code>iflag</code>. 
	  *
	  * The steplength is determined at each iteration by means of the
	  * line search routine <code>mcsrch</code>, which is a slight modification of
	  * the routine <code>CSRCH</code> written by More' and Thuente.
	  *
	  * The only variables that are machine-dependent are <code>xtol</code>,
	  * <code>stpmin</code> and <code>stpmax</code>.
	  *
	  *	Progress messages are printed to <code>System.out</code>, and
	  * non-fatal error messages are printed to <code>System.err</code>.
	  * Fatal errors cause exception to be thrown, as listed below.
	  *
	  * @param n The number of variables in the minimization problem.
	  *		Restriction: <code>n &gt; 0</code>.<p>
	  *
	  * @param m The number of corrections used in the BFGS update. 
	  *		Values of <code>m</code> less than 3 are not recommended;
	  *		large values of <code>m</code> will result in excessive
	  *		computing time. <code>3 &lt;= m &lt;= 7</code> is recommended.
	  *		Restriction: <code>m &gt; 0</code>.<p>
	  *
	  * @param x On initial entry this must be set by the user to the values
	  *		of the initial estimate of the solution vector. On exit with
	  *		<code>iflag = 0</code>, it contains the values of the variables
	  *		at the best point found (usually a solution).<p>
	  *
	  * @param f Before initial entry and on a re-entry with <code>iflag = 1</code>,
	  *		it must be set by the user to contain the value of the function
	  *		<code>f</code> at the point <code>x</code>.<p>
	  *
	  * @param g Before initial entry and on a re-entry with <code>iflag = 1</code>,
	  *		it must be set by the user to contain the components of the
	  *		gradient <code>g</code> at the point <code>x</code>.<p>
	  *
	  * @param diagco  Set this to <code>true</code> if the user  wishes to
	  *		provide the diagonal matrix <code>Hk0</code> at each iteration.
	  *		Otherwise it should be set to <code>false</code> in which case
	  *		<code>lbfgs</code> will use a default value described below. If
	  *		<code>diagco</code> is set to <code>true</code> the routine will
	  *		return at each iteration of the algorithm with <code>iflag = 2</code>,
	  *		and the diagonal matrix <code>Hk0</code> must be provided in
	  *		the array <code>diag</code>.<p>
	  *
	  * @param diag If <code>diagco = true</code>, then on initial entry or on
	  *		re-entry with <code>iflag = 2</code>, <code>diag</code>
	  *		must be set by the user to contain the values of the 
	  *		diagonal matrix <code>Hk0</code>. Restriction: all elements of
	  *		<code>diag</code> must be positive.<p>
	  *
	  * @param iprint Specifies output generated by <code>lbfgs</code>.<p>
	  *		<code>iprint[1]</code> specifies the frequency of the output:
	  *		<ul>
	  *		<li> <code>iprint[1] &lt; 0</code>: no output is generated,
	  *		<li> <code>iprint[1] = 0</code>: output only at first and last iteration,
	  *		<li> <code>iprint[1] &gt; 0</code>: output every <code>iprint[1]</code> iterations.
	  *		</ul><p>
	  *
	  *		<code>iprint[2]</code> specifies the type of output generated:
	  *		<ul>
	  *		<li> <code>iprint[2] = 0</code>: iteration count, number of function 
	  *			evaluations, function value, norm of the gradient, and steplength,
	  *		<li> <code>iprint[2] = 1</code>: same as <code>iprint[2]=0</code>, plus vector of
	  *			variables and  gradient vector at the initial point,
	  *		<li> <code>iprint[2] = 2</code>: same as <code>iprint[2]=1</code>, plus vector of
	  *			variables,
	  *		<li> <code>iprint[2] = 3</code>: same as <code>iprint[2]=2</code>, plus gradient vector.
	  *		</ul><p>
	  *
	  *	@param eps Determines the accuracy with which the solution
	  *		is to be found. The subroutine terminates when
	  *		<pre>
	  *            ||G|| &lt; EPS max(1,||X||),
	  *		</pre>
	  *		where <code>||.||</code> denotes the Euclidean norm.<p>
	  *
	  *	@param xtol An estimate of the machine precision (e.g. 10e-16 on a
	  *		SUN station 3/60). The line search routine will terminate if the
	  *		relative width of the interval of uncertainty is less than
	  *		<code>xtol</code>.<p>
	  *
	  * @param iflag This must be set to 0 on initial entry to <code>lbfgs</code>.
	  *		A return with <code>iflag &lt; 0</code> indicates an error,
	  *		and <code>iflag = 0</code> indicates that the routine has
	  *		terminated without detecting errors. On a return with
	  *		<code>iflag = 1</code>, the user must evaluate the function
	  *		<code>f</code> and gradient <code>g</code>. On a return with
	  *		<code>iflag = 2</code>, the user must provide the diagonal matrix
	  *		<code>Hk0</code>.<p>
	  *
	  *		The following negative values of <code>iflag</code>, detecting an error,
	  *		are possible:
	  *		<ul>
	  *		<li> <code>iflag = -1</code> The line search routine
	  *			<code>mcsrch</code> failed. One of the following messages
	  *			is printed:
	  *			<ul>
	  *			<li> Improper input parameters.
	  *			<li> Relative width of the interval of uncertainty is at
	  *				most <code>xtol</code>.
	  *			<li> More than 20 function evaluations were required at the
	  *				present iteration.
	  *			<li> The step is too small.
	  *			<li> The step is too large.
	  *			<li> Rounding errors prevent further progress. There may not
	  *				be  a step which satisfies the sufficient decrease and
	  *				curvature conditions. Tolerances may be too small.
	  *			</ul>
	  *		<li><code>iflag = -2</code> The i-th diagonal element of the diagonal inverse
	  *			Hessian approximation, given in DIAG, is not positive.
	  *		<li><code>iflag = -3</code> Improper input parameters for LBFGS
	  *			(<code>n</code> or <code>m</code> are not positive).
	  *		</ul><p>
	  *
	  *	@throws LBFGS.ExceptionWithIflag 
	  */

	public static void lbfgs ( int n , int m , double[] x , double f , double[] g , boolean diagco , double[] diag , int[] iprint , double eps , double xtol , int[] iflag ) throws ExceptionWithIflag
	{
		boolean execute_entire_while_loop = false;

		if ( w == null || w.length != n*(2*m+1)+2*m+1 )
		{
			w = new double[ n*(2*m+1)+2*m+1 ];			// note the extra element allocated
		}

		if ( iflag[0] == 0 )
		{
			iter = 0;

			if ( n <= 0 || m <= 0 )
			{
				iflag[0]= -3;
				throw new ExceptionWithIflag( iflag[0], "Improper input parameters  (n or m are not positive.)" );
			}

			if ( gtol <= 0.0001 )
			{
				System.err.println( "LBFGS.lbfgs: gtol is less than or equal to 0.0001. It has been reset to 0.9." );
				gtol= 0.9;
			}

			nfun= 1;
			point= 0;
			finish= false;

			if ( diagco )
			{
				for ( i = 1 ; i <= n ; i += 1 )
				{
					if ( diag [ i ] <= 0 )
					{
						iflag[0]=-2;
						throw new ExceptionWithIflag( iflag[0], "The "+i+"-th diagonal element of the inverse hessian approximation is not positive." );
					}
				}
			}
			else
			{
				for ( i = 1 ; i <= n ; i += 1 )
				{
					diag [ i ] = 1;
				}
			}
			ispt= n+2*m;
			iypt= ispt+n*m;

			for ( i = 1 ; i <= n ; i += 1 )
			{
				w [ ispt + i ] = - g [ i ] * diag [ i ];
			}

			gnorm = Math.sqrt ( Ddot.ddot ( n , g , 0, 1 , g , 0, 1 ) );
			stp1= 1/gnorm;
			ftol= 0.0001; 
			maxfev= 20;

			if ( iprint [ 1 ] >= 0 ) Lb1.lb1 ( iprint , iter , nfun , gnorm , n , m , x , f , g , stp , finish );

			execute_entire_while_loop = true;
		}

		while ( true )
		{
			if ( execute_entire_while_loop )
			{
				iter= iter+1;
				info[0]=0;
				bound=iter-1;
				if ( iter != 1 )
				{
					if ( iter > m ) bound = m;
					ys = Ddot.ddot ( n , w , iypt + npt , 1 , w , ispt + npt , 1 );
					if ( ! diagco )
					{
						yy = Ddot.ddot ( n , w , iypt + npt , 1 , w , iypt + npt , 1 );

						for ( i = 1 ; i <= n ; i += 1 )
						{
							diag [ i ] = ys / yy;
						}
					}
					else
					{
						iflag[0]=2;
						return;
					}
				}
			}

			if ( execute_entire_while_loop || iflag[0] == 2 )
			{
				if ( iter != 1 )
				{
					if ( diagco )
					{
						for ( i = 1 ; i <= n ; i += 1 )
						{
							if ( diag [ i ] <= 0 )
							{
								iflag[0]=-2;
								throw new ExceptionWithIflag( iflag[0], "The "+i+"-th diagonal element of the inverse hessian approximation is not positive." );
							}
						}
					}
					cp= point;
					if ( point == 0 ) cp = m;
					w [ n + cp ] = 1 / ys;

					for ( i = 1 ; i <= n ; i += 1 )
					{
						w [ i ] = - g [ i ];
					}

					cp= point;

					for ( i = 1 ; i <= bound ; i += 1 )
					{
						cp=cp-1;
						if ( cp == - 1 ) cp = m - 1;
						sq = Ddot.ddot ( n , w , ispt + cp * n , 1 , w , 0 , 1 );
						inmc=n+m+cp+1;
						iycn=iypt+cp*n;
						w [ inmc ] = w [ n + cp + 1 ] * sq;
						Daxpy.daxpy ( n , - w [ inmc ] , w , iycn , 1 , w , 0 , 1 );
					}

					for ( i = 1 ; i <= n ; i += 1 )
					{
						w [ i ] = diag [ i ] * w [ i ];
					}

					for ( i = 1 ; i <= bound ; i += 1 )
					{
						yr = Ddot.ddot ( n , w , iypt + cp * n , 1 , w , 0 , 1 );
						beta = w [ n + cp + 1 ] * yr;
						inmc=n+m+cp+1;
						beta = w [ inmc ] - beta;
						iscn=ispt+cp*n;
						Daxpy.daxpy ( n , beta , w , iscn , 1 , w , 0 , 1 );
						cp=cp+1;
						if ( cp == m ) cp = 0;
					}

					for ( i = 1 ; i <= n ; i += 1 )
					{
						w [ ispt + point * n + i ] = w [ i ];
					}
				}

				nfev[0]=0;
				stp[0]=1;
				if ( iter == 1 ) stp[0] = stp1;

				for ( i = 1 ; i <= n ; i += 1 )
				{
					w [ i ] = g [ i ];
				}
			}

			Mcsrch.mcsrch ( n , x , f , g , w , ispt + point * n , stp , ftol , xtol , maxfev , info , nfev , diag );

			if ( info[0] == - 1 )
			{
				iflag[0]=1;
				return;
			}

			if ( info[0] != 1 )
			{
				iflag[0]=-1;
				throw new ExceptionWithIflag( iflag[0], "Line search failed. See documentation of routine mcsrch. Error return of line search: info = "+info[0]+" Possible causes: function or gradient are incorrect, or incorrect tolerances." );
			}

			nfun= nfun + nfev[0];
			npt=point*n;

			for ( i = 1 ; i <= n ; i += 1 )
			{
				w [ ispt + npt + i ] = stp[0] * w [ ispt + npt + i ];
				w [ iypt + npt + i ] = g [ i ] - w [ i ];
			}

			point=point+1;
			if ( point == m ) point = 0;

			gnorm = Math.sqrt ( Ddot.ddot ( n , g , 0 , 1 , g , 0 , 1 ) );
			xnorm = Math.sqrt ( Ddot.ddot ( n , x , 0 , 1 , x , 0 , 1 ) );
			xnorm = Math.max ( 1.0 , xnorm );

			if ( gnorm / xnorm <= eps ) finish = true;

			if ( iprint [ 1 ] >= 0 ) Lb1.lb1 ( iprint , iter , nfun , gnorm , n , m , x , f , g , stp , finish );

			if ( finish )
			{
				iflag[0]=0;
					return;
			}

			execute_entire_while_loop = true;		// from now on, execute whole loop
		}
	}
}
