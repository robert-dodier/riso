public class IntComparator implements Comparator
{
	public boolean greater( Object a, Object b )
	{
		return ((Integer)a).intValue() > ((Integer)b).intValue();
	}
}
