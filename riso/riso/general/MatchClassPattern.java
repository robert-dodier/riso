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
	static SeqTriple[] sm0 = {
		new SeqTriple( "riso.distributions.AbstractConditionalDistribution", 1 ),
		new SeqTriple( "riso.distributions.AbstractDistribution", -1 )
	};

	static SeqTriple[] sm1 = {
		new SeqTriple( "riso.distributions.AbstractConditionalDistribution", 1 ),
		new SeqTriple( "riso.distributions.AbstractDistribution", 6 )
	};

	static SeqTriple[] sm2 = {
		new SeqTriple( "riso.distributions.AbstractConditionalDistribution", 1 ),
		new SeqTriple( "riso.distributions.AbstractDistribution", 5 ),
		new SeqTriple( "riso.distributions.Gaussian", -1 ),
		new SeqTriple( "riso.distributions.GaussianDelta", -1 )
	};

	static SeqTriple[] sm3 = {
		new SeqTriple( "riso.distributions.AbstractConditionalDistribution", 1 ),
		new SeqTriple( "riso.distributions.Gamma", -1 ),
		new SeqTriple( "riso.distributions.Gaussian", 4 ),
		new SeqTriple( "riso.distributions.GaussianDelta", 4 )
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

			int[] class_specific_score = new int[1], count_specific_score = new int[1];
			boolean does_match = matches(sm0,seq,class_specific_score,count_specific_score);
			System.err.println( "matches sm0: "+does_match+(does_match?(", class score: "+class_specific_score[0]+", count score: "+count_specific_score[0]):"") );
			does_match = matches(sm1,seq,class_specific_score,count_specific_score);
			System.err.println( "matches sm1: "+does_match+(does_match?(", class score: "+class_specific_score[0]+", count score: "+count_specific_score[0]):"") );
			does_match = matches(sm2,seq,class_specific_score,count_specific_score);
			System.err.println( "matches sm2: "+does_match+(does_match?(", class score: "+class_specific_score[0]+", count score: "+count_specific_score[0]):"") );
			does_match = matches(sm3,seq,class_specific_score,count_specific_score);
			System.err.println( "matches sm3: "+does_match+(does_match?(", class score: "+class_specific_score[0]+", count score: "+count_specific_score[0]):"") );
		}
		catch (Exception e) { System.err.println( "e: "+e ); }
	}

	public static boolean matches( SeqTriple[] sm, Vector seq, int[] class_specific_score, int[] count_specific_score )
	{
		int ii = 0, n = 0;
		class_specific_score[0] = 0;
		count_specific_score[0] = 0;

		for ( Enumeration e = seq.elements(); e.hasMoreElements(); )
		{
			Class seqc = (Class) e.nextElement();
			class_specific_score[0] += sm[ii].level;

			if ( ! sm[ii].c.isAssignableFrom(seqc) )
				if ( sm[ii].reps == -1 )
				{
					n = 0;
					if ( ++ii == sm.length )
					{
System.err.println( "reached end of pattern w/ seqc "+seqc.getName() );
						return false;
					}
				}
				else
				{
System.err.println( seqc.getName()+" not instance of "+sm[ii].c.getName()+"; found "+n+" before failing" );
					return false;
				}

			if ( sm[ii].c.isAssignableFrom(seqc) )
			{
				if ( ++n == sm[ii].reps )
				{
System.err.println( "reached end of "+sm[ii].reps+" "+sm[ii].c.getName() );
					n = 0;
					if ( ++ii == sm.length && e.hasMoreElements() )
					{
System.err.println( "reached end of pattern w/ seqc "+seqc.getName() );
						return false;
					}
					++count_specific_score[0];
				}
			}
			else
			{
System.err.println( seqc.getName()+" not instance of "+sm[ii].c.getName()+"; found "+n+" before failing" );
				return false;
			}
		}

System.err.println( "fell out at bottom, ii: "+ii+", n: "+n );
		if ( ii == sm.length && n == sm[ii-1].reps )
			++count_specific_score[0];

		if ( ii < sm.length && n != sm[ii].reps && sm[ii].reps != -1 ) 
		{
System.err.println( "reached end of pattern w/ n "+n+" (should be "+sm[ii].reps+")" );
			return false;
		}

		return true;
	}
}
