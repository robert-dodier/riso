public class SeqTriple
{
	int level, reps;
	Class c;

	public SeqTriple( String s, int reps )
	{
		try
		{
			c = Class.forName(s);
			level = nsuperclasses(c);
			this.reps = reps;
		}
		catch (Exception e) { e.printStackTrace(); System.exit(1); } // !!!
	}

	public static int nsuperclasses( Class c ) throws ClassNotFoundException
	{
		int n = 0;
		while ( (c = c.getSuperclass()) != null )
			++n;
		return n;
	}

	public String toString()
	{
		return "["+level+","+c.getName()+","+reps+"]";
	}
}
