import java.io.*;
import numerical.*;

/** This class contains a static method to sort a list of objects.
  */
public class ShellSort
{
	/** Sorts a list of objects using Shell's algorithm. This code is a
	  * translation of <tt>shl.c</tt> by Thomas Niemann, obtainable as part
	  * a zip file on his web page 
	  * <a href="http://www.geocities.com/SoHo/2167/book.html">
	  * ``Sorting and Searching Algorithms: A Cookbook.''</a>
	  *
	  * @see Comparator
	  * @param a List of objects -- must all be the same comparable by
	  *   the argument <tt>cmp</tt>.
	  * @param lb Lower bound -- set to 0 for top-level call.
	  * @param ub Upper bound -- set to <tt>a.length-1</tt> for top-level call.
	  * @param cmp Instance of a class which knows how to compare the objects
	  *   in the list <tt>a</tt>.
	  */
	public static void do_sort( Object[] a, int lb, int ub, Comparator cmp )
	{
		int n, h, i, j;
		Object t;

		/* compute largest increment */
		n = ub - lb + 1;
		h = 1;
		if (n < 14)
			h = 1;
		else {
			while (h < n) h = 3*h + 1;
			h /= 3;
			h /= 3;
		}

		while (h > 0) {

			/* sort-by-insertion in increments of h */
			for (i = lb + h; i <= ub; i++) {
				t = a[i];
				for (j = i-h; j >= lb && cmp.greater(a[j], t); j -= h)
					a[j+h] = a[j];
				a[j+h] = t;
			}

			/* compute next increment */
			h /= 3;
		}
	}

	/** This function is a test program for the sorting method;
	  * it asks for a list of integers and sorts them.
	  */
	public static void main( String[] args )
	{
		try
		{
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );
			System.err.print( "give number of numbers: " );
			st.nextToken();
			int i, N = Format.atoi( st.sval );
			System.err.println( "give "+N+" numbers: " );

			Integer[] x = new Integer[N];

			for ( i = 0; i < N; i++ )
			{
				st.nextToken();
				x[i] = new Integer( Format.atoi( st.sval ) );
			}

			do_sort( x, 0, x.length-1, new IntComparator() );

			System.err.println( "sorted: " );
			for ( i = 0; i < N; i++ )
				System.err.println( x[i] );

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
