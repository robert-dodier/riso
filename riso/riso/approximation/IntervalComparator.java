package riso.approximation;
import Comparator;

public class IntervalComparator implements Comparator
{
	public boolean greater( Object a, Object b )
	{
		return ((double[])a)[0] > ((double[])b)[0];
	}
}
