package riso.approximation;
import java.io.*;
import java.util.*;
import riso.distributions.*;
import numerical.*;
import SmarterTokenizer;
import ShellSort;
import Comparator;

public class Intervals
{
	/** An instance of this class is thrown when the scale of a search
	  * for the support of a function appears to be much too large.
	  */
	public static class ScaleTooBigException extends Exception
	{
		public ScaleTooBigException() { super(); }
		public ScaleTooBigException( String s ) { super(s); }
	}

	public static double[][] union_merge_intervals( double[][] intervals )
	{
		// Sort intervals by left endpoint, in ascending order.

		ShellSort.do_sort( (Object[])intervals, 0, intervals.length-1, new IntervalComparator() );

		// Now merge overlapping intervals.
		// If two intervals overlap, we take the union.

		Vector merged = new Vector();
		double[] current_interval = (double[]) intervals[0].clone();
		int i;

		for ( i = 1; i < intervals.length; i++ )
		{
			if ( intervals[i][0] <= current_interval[1] )
			{
				if ( intervals[i][1] > current_interval[1] )
					// intervals[i] overlaps and right end is farther right;
					// extend current interval.
					current_interval[1] = intervals[i][1];
			}
			else
			{
				// intervals[i] is disjoint from current_interval.
				// Put current_interval onto the list of merged intervals,
				// and make intervals[i] the current_interval.

				merged.addElement( current_interval );
				current_interval = (double[]) intervals[i].clone();
			}
		}

		merged.addElement( current_interval );

		double[][] merged_intervals = new double[ merged.size() ][2];
		merged.copyInto( merged_intervals );

		return merged_intervals;
	}

	public static double[][] intersection_merge_intervals( double[][] intervals )
	{
		// Sort intervals by left endpoint, in ascending order.

		ShellSort.do_sort( (Object[])intervals, 0, intervals.length-1, new IntervalComparator() );

		// Now merge overlapping intervals. 
		// We take the intersection of all the intervals on the list.

		Vector merged = new Vector();
		double[] current_interval = (double[]) intervals[ intervals.length-1 ].clone();
		int i;

		for ( i = intervals.length-2; i >= 0; i-- )
		{
			if ( intervals[i][1] > current_interval[0] )
			{
				if ( intervals[i][1] < current_interval[1] )
					// intervals[i] overlaps and right end is not as far right;
					// restrict current interval.
					current_interval[1] = intervals[i][1];
			}
			else
			{
				// intervals[i] is disjoint from current_interval.
				// The intersection is empty.

				return null;
			}
		}

		double[][] merged_intervals = new double[1][2];
		merged_intervals[0] = current_interval;

		return merged_intervals;
	}

	/** Returns an approximation to the support of a function. The target
	  * function may not be normalized; the function is nonnegative and
	  * smooth. 
	  *
	  * <p> This method attempts to approximation the integral of the
	  * target function over a very large interval, and then to find a
	  * smaller interval (contained in the larger) such that the smaller
	  * interval contains almost all the mass in the larger one.
	  *
	  * <p> The hard part of the problem is searching out the peaks in
	  * the target function, without any clues about their location. 
	  * This method sprinkles a lot of points on a wide interval centered
	  * on zero and tries to find evidence of peaks. So the time required
	  * for this approach is proportional to the time required to evaluate
	  * the target function.
	  *
	  * @param f Target function.
	  * @param scale Rough estimate of characteristic scale of the target;
	  *   for example, 1, 1000, or 0.001. Algorithm employed here searches
	  *   an interval <tt>(-100*scale,+100*scale)</tt> at a resolution
	  *   equal to <tt>scale</tt>.
	  * @param tolerance How much of the mass of the larger interval is not
	  *   contained in the smaller interval (which is the return value).
	  *   Typically a little more than 0, e.g. 0.01, 0.000001.
	  * @returns An interval containing mass equal approximately to 
	  *   <tt>tolerance*I</tt> where <tt>I</tt> is the mass estimated in
	  *   the largest interval searched.
	  * @throws ScaleTooBigException If the support appears to be much smaller
	  *   than the smallest interval searched, and we have no idea where to
	  *   look for the support.
	  * @throws SupportNotWellDefinedException If the largest interval
	  *   searched does not seem to contain all of the integral.
	  * @throws IllegalArgumentException If <tt>tolerance</tt> is not in the
	  *   range 0 to 1, exclusive, or if the integration fails.
	  */
	static public double[] effective_support( Callback_1d f, double scale, double tolerance ) throws IllegalArgumentException, SupportNotWellDefinedException, ScaleTooBigException
	{
		double[] larger_interval = new double[2];

		larger_interval[0] = -100*scale;
		larger_interval[1] =  100*scale;

		return effective_support( f, larger_interval, tolerance );
	}

	/** Like the other version of <tt>effective_support</tt>, except
	  * that the largest interval to be search is specified instead
	  * being constructed from the <tt>scale</tt> argument.
	  */
	static public double[] effective_support( Callback_1d f, double[] larger_interval, double tolerance ) throws IllegalArgumentException, SupportNotWellDefinedException, ScaleTooBigException
	{
		if ( tolerance <= 0 || tolerance >= 1 )
			throw new IllegalArgumentException( "Intervals.effective_support: improper tolerance: "+tolerance );

		// ninterior is the number of points _within_ the larger interval; there are n+2 points altogether,
		// counting the endpoints as well.

		int i, ninterior = 200;
		double[] x = new double[ ninterior+2 ], F = new double[ ninterior+2 ];
		double[] smaller_interval = new double[2];

		double dx = (larger_interval[1]-larger_interval[0])/ninterior;

		x[0] = larger_interval[0];
		for ( i = 1; i <= ninterior; i++ )
			x[i] = larger_interval[0] + (i-1)*dx + 0.05*dx + 0.9*Math.random()*dx;
		x[ninterior+1] = larger_interval[1];

		F[0] = 0;
		for ( i = 1; i <= ninterior+1; i++ )
			try { F[i] = F[i-1] + ExtrapolationIntegral.do_integral1d( false, x[i-1], x[i], f, 1e-2 ); }
			catch (ExtrapolationIntegral.DifficultIntegralException e)
			{
				System.err.println( "Intervals.effective_support: WARNING: difficult integral of "+f.getClass()+"; accept best guess: "+e.best_approx );
				F[i] = F[i-1] + e.best_approx;
			}
			catch (Exception e2)
			{
				throw new IllegalArgumentException( "Intervals.effective_support: integration failed:\n"+e2 );
			}

System.err.println( "Intervals.effective_support: F[n+1]: "+F[ninterior+1] );
		// If the scale is much too large, the function will be evaluated as
		// zero at every x[i]. We have indication of where to focus the search.

		if ( F[ninterior+1] == 0 )
		{
			throw new ScaleTooBigException( "Intervals.effective_support: interval ["+x[0]+", "+x[x.length-1]+"] appears much too large." );
		}

		// Now find the smallest subinterval that contains most of the mass.

		int separation, i0, i1;

		for ( separation = 1; separation < ninterior+1; separation++ )
		{
			for ( i0 = 0, i1 = separation; i1 <= ninterior+1; i0++, i1++ )
				if ( F[i1] - F[i0] > F[ninterior+1]*(1-tolerance) )
				{
System.err.println( "Intervals.effective_support: found subinterval; i0: "+i0+" i1: "+i1 );
					// If we are at either extreme of the largest interval
					// searched, there is probably more mass outside.
					if ( i0 == 0 || i1 == ninterior+1 ) 
						throw new SupportNotWellDefinedException( "Intervals.effective_support: appears to be more mass "+(i0==0?"below "+x[i0]:"above "+x[i1]) );

					smaller_interval[0] = x[ i0 ];
					smaller_interval[1] = x[ i1 ];

					// If the support appears very small, the scale is wrong;
					// reduce the largest interval and try again.

					if ( i1 == i0+1 )
					{
						double old_scale = dx, new_scale = dx/ninterior;
						System.err.println( "Intervals.effective_support: WARNING: scale appears to be too large; reduce from "+old_scale+" to "+new_scale+" and try again." );
						try { return effective_support( f, smaller_interval, tolerance ); }
						catch (SupportNotWellDefinedException e)
						{
							System.err.println( "Intervals.effective_support: WARNING: failed attempt to refine scale; return at original scale: "+old_scale );
							return smaller_interval;
						}
					}

					return smaller_interval;
				}
// System.err.println( "Intervals.effective_support: separation isn't enough: "+separation );
		}

		// If we fall out here, no subinterval smaller than the largest
		// interval contains most of the mass. This probably means that
		// the support is bigger than the largest interval.

		throw new SupportNotWellDefinedException( "Intervals.effective_support: support appears to be larger than ["+larger_interval[0]+", "+larger_interval[1]+"]." );
	}

	public static void main( String[] args )
	{
		try
		{
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );
			System.err.print( "give number of intervals: " );
			st.nextToken();
			int N = Format.atoi( st.sval );
			System.err.println( "give "+N+" intervals: " );
			double[][] intervals = new double[N][2];

			for ( int i = 0; i < N; i++ )
			{
				st.nextToken(); intervals[i][0] = Format.atof( st.sval );
				st.nextToken(); intervals[i][1] = Format.atof( st.sval );
			}

			double[][] merged = intersection_merge_intervals( intervals );

			System.err.println( "intersection merged intervals:" );
			Matrix.pretty_output( merged, System.out, " " );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
