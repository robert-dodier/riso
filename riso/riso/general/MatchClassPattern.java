import java.io.*;
import java.util.*;

class SeqTriple
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
}

public class MatchClassPattern
{
	static SeqTriple[] sm1 = {
		new SeqTriple( "riso.distributions.AbstractDistribution", 6 )
	};

	static SeqTriple[] sm2 = {
		new SeqTriple( "riso.distributions.AbstractDistribution", 3 ),
		new SeqTriple( "riso.distributions.Gaussian", 2 ),
		new SeqTriple( "riso.distributions.GaussianDelta", 1 )
	};

	static SeqTriple[] sm3 = {
		new SeqTriple( "riso.distributions.Gamma", 3 ),
		new SeqTriple( "riso.distributions.GaussianDelta", 3 )
	};

	public static void main( String[] args )
	{
		try
		{
			Vector seq = new Vector();
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );
			st.nextToken();

			while ( st.ttype != StreamTokenizer.TT_EOF )
			{
				Class c = Class.forName( st.sval );
				System.err.println( "add: "+c.getName() );
				seq.addElement(c);
				st.nextToken();
			}

			int[] score = new int[1];
			System.err.println( "matches sm1: "+matches(sm1,seq,score) );
			System.err.println( "score: "+score[0] );
			System.err.println( "matches sm2: "+matches(sm2,seq,score) );
			System.err.println( "score: "+score[0] );
			System.err.println( "matches sm3: "+matches(sm3,seq,score) );
			System.err.println( "score: "+score[0] );
		}
		catch (Exception e) { System.err.println( "e: "+e ); }
	}

	public static boolean matches( SeqTriple[] sm, Vector seq, int[] score )
	{
		int ii = 0, n = 0;
		score[0] = 0;

		for ( Enumeration e = seq.elements(); e.hasMoreElements(); )
		{
			Class seqc = (Class) e.nextElement();
			if ( ! sm[ii].c.isAssignableFrom(seqc) )
			{
System.err.println( seqc.getName()+" not instance of "+sm[ii].c.getName() );
				return false;
			}

			score[0] += sm[ii].level;
			if ( ++n == sm[ii].reps )
			{
				n = 0;
				if ( ++ii == sm.length && e.hasMoreElements() )
				{
System.err.println( "reached end of pattern w/ seqc "+seqc.getName() );
					return false;
				}
			}
		}

		if ( ii < sm.length && n != sm[ii].reps ) 
		{
System.err.println( "reached end of pattern w/ n "+n+" (should be "+sm[ii].reps+")" );
			return false;
		}

		return true;
	}
}
