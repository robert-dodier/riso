package riso.distributions.computes_lambda;
import java.io.*;
import java.util.*;
import numerical.*;
import SmarterTokenizer;
import ShellSort;
import Comparator;

public class Intervals
{
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
