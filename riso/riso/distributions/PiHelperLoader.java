/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999, Robert Dodier.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA, 02111-1307, USA,
 * or visit the GNU web site, www.gnu.org.
 */
package riso.distributions;
import java.lang.reflect.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import riso.belief_nets.*;
import MatchClassPattern;
import SeqTriple;

public class PiHelperLoader
{
	/** The helper cache. The keys are instances of <tt>HelperCacheKey</tt>.
	  */
	public static Hashtable helper_cache = new Hashtable();

	/** The last time (in milliseconds since the epoch) the cache was emptied.
	  */
	public static long cache_timestamp = 0;

	/** The helper cache is emptied every <tt>HELPER_CACHE_REFRESH</tt> milliseconds.
	  */
	public static long HELPER_CACHE_REFRESH = 3600000L;

	// SHOLD PROBABLY MAIMTAIM REFS TO SEVERAL COMTEXTS -- BOTH LOCAL AMD !!!
	// OME OR MORE REMOTE !!!
	public static AbstractBeliefNetworkContext bnc = null;

	public static PiHelper load_pi_helper( ConditionalDistribution pxu, Distribution[] pi_messages ) throws Exception
	{
		if ( pi_messages.length == 0 )
			return new TrivialPiHelper();

		Vector seq = new Vector();
		seq.addElement( pxu.getClass() );
		for ( int i = 0; i < pi_messages.length; i++ )
			seq.addElement( pi_messages[i].getClass() );

		Class c = find_helper_class( seq, "pi" );
		return (PiHelper) c.newInstance();
	}

	/** This method returns a <tt>Class</tt> for a helper which can handle the list of
	  * distributions specified by <tt>seq1</tt>. We maintain a cache of recently-loaded helpers,
	  * so check the cache before going to the trouble of searching for a helper. The cache
	  * is blown away every <tt>HELPER_CACHE_REFRESH</tt> seconds.
	  *
	  * <p> If we can't find a helper in the cache, we must search through the list of available
	  * helpers to find an appropriate one. First try to find helper using class sequence as specified
	  * by <tt>seq</tt>. Whether or not that succeeds, promote any <tt>Gaussian</tt> in the sequence 
	  * to <tt>MixGaussians</tt>, and try again. If we get a better match on the second try,
	  * return the helper thus found.
	  */
	public static Class find_helper_class( Vector seq1, String helper_type ) throws ClassNotFoundException
	{
		// Let's see if an appropriate helper is in the cache.
		// If the cache is too old, empty it and search for the helper anew.

		if ( System.currentTimeMillis() - cache_timestamp > HELPER_CACHE_REFRESH )
		{
			helper_cache = new Hashtable();
			cache_timestamp = System.currentTimeMillis();
			// Go on and search for appropriate helper.
		}
		else
		{
			HelperCacheKey key = new HelperCacheKey( helper_type, seq1 );
			Class helper_class = (Class) helper_cache.get(key);
			if ( helper_class != null ) return helper_class;
			// else no luck; we have to search for helper.
		}

		// Well, we didn't find a helper in the cache, so let's go to work.

		Class c1 = null, c2 = null;
		ClassNotFoundException cnfe1 = null, cnfe2 = null;
		int[] class_score1 = new int[1], count_score1 = new int[1];
		int[] class_score2 = new int[1], count_score2 = new int[1];

		try { c1 = find_helper_class0( seq1, helper_type, class_score1, count_score1 ); }
		catch (ClassNotFoundException e) { cnfe1 = e; } // hang on, we may need to re-throw later.

		Class gaussian_class = Class.forName("riso.distributions.Gaussian"); 
		MixGaussians mog = new MixGaussians(1,1);

		Vector seq2 = new Vector( seq1.size() );
		for ( int i = 0; i < seq1.size(); i++ )
			if ( gaussian_class.isAssignableFrom((Class)seq1.elementAt(i)) )
				seq2.addElement( mog.getClass() );
			else
				seq2.addElement( seq1.elementAt(i) );

		try { c2 = find_helper_class0( seq2, helper_type, class_score2, count_score2 ); }
		catch (ClassNotFoundException e) { cnfe2 = e; }

		if ( cnfe1 == null && cnfe2 == null )
		{
			// Both matched; see which one fits better.
			// Break ties in favor of the helper for non-promoted messages.

			if ( class_score1[0] >= class_score2[0] || (class_score1[0] == class_score2[0] && count_score1[0] >= count_score2[0]) )
			{
System.err.println( "\taccept helper "+c1+" for non-promoted classes instead of "+c2 );
System.err.println( "\t\t"+class_score1[0]+", "+class_score2[0]+"; "+count_score1[0]+", "+count_score2[0] );
				helper_cache.put( new HelperCacheKey(helper_type,seq1), c1 );
				return c1;
			}
			else
			{
System.err.println( "\taccept helper "+c2+" for promoted classes instead of "+c1 );
System.err.println( "\t\t"+class_score1[0]+", "+class_score2[0]+"; "+count_score1[0]+", "+count_score2[0] );
				helper_cache.put( new HelperCacheKey(helper_type,seq1), c2 );
				return c2;
			}
		}
		else if ( cnfe1 == null && cnfe2 != null )
		{
			// Only the first try matched, return it.
			helper_cache.put( new HelperCacheKey(helper_type,seq1), c1 );
			return c1;
		}
		else if ( cnfe1 != null && cnfe2 == null )
		{
			// Only the second try matched, return it.
			helper_cache.put( new HelperCacheKey(helper_type,seq1), c2 );
			return c2;
		}
		else
		{
			// Neither try matched. Re-throw the exception generated by the first try.
			throw cnfe1;
		}
	}

	/** Contact a belief network context, get the helper list, and search the list
	  * to see if there's a helper which matches the type sequence specified.
	  * If there's more than one helper which matches, find the ``best fit.''
	  *
	  * <p> The class and count scores of the best-fitting helper class are written
	  * into <tt>max_class_score[0]</tt> and <tt>max_count_score[0]</tt>, respectively.
	  */
	public static Class find_helper_class0( Vector seq, String helper_type, int[] max_class_score, int[] max_count_score ) throws ClassNotFoundException
	{
long t0 = System.currentTimeMillis();
		if ( bnc != null ) // make sure the reference is still alive
			try { bnc.get_name(); } catch (RemoteException e) { bnc = null; }

		if ( bnc == null ) // need to locate a context
		{
			String cb = System.getProperty( "java.rmi.server.codebase", "http://localhost" );
long tt0 = System.currentTimeMillis();
			try { bnc = BeliefNetworkContext.locate_context( new URL(cb).getHost() ); }
			catch (Exception e) { throw new ClassNotFoundException( "nested: "+e ); }
		}

		String[] helperlist;
		try { helperlist = bnc.get_helper_names( helper_type ); }
		catch (RemoteException e) { throw new ClassNotFoundException( "bnc.get_helper_names failed" ); }

		int[] class_score1 = new int[1], count_score1 = new int[1];
		max_class_score[0] = -1;
		max_count_score[0] = -1;
		Class cmax_score = null;

		for ( int i = 0; i < helperlist.length; i++ )
		{
			try
			{
				Class c = RMIClassLoader.loadClass( helperlist[i] );
				SeqTriple[] sm = (SeqTriple[]) invoke_description(c);
				if ( sm == null ) continue; // apparently not a helper class
				if ( MatchClassPattern.matches( sm, seq, class_score1, count_score1 ) )
				{
					if ( class_score1[0] > max_class_score[0] || (class_score1[0] == max_class_score[0] && count_score1[0] > max_count_score[0]) )
					{
						cmax_score = c;
						max_class_score[0] = class_score1[0];
						max_count_score[0] = count_score1[0];
					}
				}
			}
			catch (Exception e2)
			{
				System.err.println( "PiHelperLoader: attempt to load "+helperlist[i]+" failed; "+e2 );
			}
		}

		if ( cmax_score == null )
		{
			System.err.println( "find_helper_class0: failed; helper list:" );
			for ( int i = 0; i < helperlist.length; i++ ) System.err.println( "\t"+helperlist[i] );

			String s = "";
			for ( Enumeration e = seq.elements(); e.hasMoreElements(); )
			{
				try { Class c = (Class) e.nextElement(); s+= c.getName()+","; }
				catch (NoSuchElementException ee) { s += "???"+","; }
			}

			throw new ClassNotFoundException( "no "+helper_type+" helper for sequence ["+s+"]" );
		}
		
		// FOR NOW IGNORE THE POSSIBILITY OF TWO OR MORE MATCHES !!!
		return cmax_score;
	}

	public static Object invoke_description( Class c )
	{
		try
		{
			Method m = c.getMethod ("description", new Class[] {});

			// Since "description" is a static method, supply null as the object.
			try { return m.invoke(null, null); }
			catch (InvocationTargetException ite)
			{
				System.err.println( "invoke_description: invocation failed; " );
				ite.getTargetException().printStackTrace();
			}
			catch (Exception e) { e.printStackTrace(); }
		}
		catch (NoSuchMethodException nsme) {} // eat the exception; apparently c is not a helper

		return null;
	}
}

/** A key for an item in the helper cache consists of the helper type (a string) and 
  * a list of classes.
  */
class HelperCacheKey
{
	String helper_type;
	Vector seq;

	HelperCacheKey( String helper_type, Vector seq ) { this.helper_type = helper_type; this.seq = seq; }

	public boolean equals( Object another )
	{
		if ( another instanceof HelperCacheKey )
		{
			HelperCacheKey another_key = (HelperCacheKey) another;

			if ( ! this.helper_type.equals(another_key.helper_type) ) return false;

			Enumeration e1, e2;
			for ( e1 = this.seq.elements(), e2 = another_key.seq.elements(); e1.hasMoreElements(); )
			{
				Object o1, o2;
				try { o1 = e1.nextElement(); }
				catch (NoSuchElementException ex) { throw new RuntimeException( "HelperCacheKey.equals: should never happen, "+ex ); }
				try { o2 = e2.nextElement(); }
				catch (NoSuchElementException ex) { return false; } // e2 has fewer elements than e2, no match

				if ( ! o1.equals(o2) ) return false;
			}

			if ( e2.hasMoreElements() )
				return false;
			else
				return true;
		}
		else
			return false;
	}

	public int hashCode()
	{
		return helper_type.hashCode(); // one collision list in hash table per helper type; could reduce collisions !!!
	}

	public String toString()
	{
		String s = "["+helper_type+";";
		for ( Enumeration e = seq.elements(); e.hasMoreElements(); )
		{
			try { Class c = (Class) e.nextElement(); s+= c.getName()+","; }
			catch (NoSuchElementException ee) { s += "???"+","; }
		}
		
		return s+"]";
	}
}
