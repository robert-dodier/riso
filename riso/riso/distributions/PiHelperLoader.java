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
	// SHOLD PROBABLY MAIMTAIM REFS TO SEVERAL COMTEXTS -- BOTH LOCAL AMD !!!
	// OME OR MORE REMOTE !!!
	static AbstractBeliefNetworkContext bnc = null;

	public static PiHelper load_pi_helper( ConditionalDistribution px, Distribution[] pi_messages ) throws Exception
	{
		if ( pi_messages.length == 0 )
			return new TrivialPiHelper();

long t0 = System.currentTimeMillis();
		if ( bnc != null ) // make sure the reference is still alive
			try { bnc.get_name(); } catch (RemoteException e) { bnc = null; }

		if ( bnc == null ) // need to locate a context
		{
			String cb = System.getProperty( "java.rmi.server.codebase", "http://localhost" );
long tt0 = System.currentTimeMillis();
			bnc = BeliefNetworkContext.locate_context( new URL(cb).getHost() );
long tt1 = System.currentTimeMillis();
System.err.println( "load_pi_helper: obtained context: "+bnc.get_name()+"; elapsed: "+((tt1-tt0)/1000.0)+" [s]" );
		}

		String[] helperlist = bnc.get_helper_names( "pi" );

		Vector seq = new Vector();
		seq.addElement( px.getClass() );
		for ( int i = 0; i < pi_messages.length; i++ )
			seq.addElement( pi_messages[i].getClass() );

		int[] class_score1 = new int[1], count_score1 = new int[1];
		int max_class_score = -1, max_count_score = -1;
		int argmax_class_score = -1;

		for ( int i = 0; i < helperlist.length; i++ )
		{
			try
			{
				Class c = RMIClassLoader.loadClass( helperlist[i] );
				SeqTriple[] sm = (SeqTriple[]) invoke_description(c);
				if ( sm == null ) continue; // apparently not a helper class
				if ( MatchClassPattern.matches( sm, seq, class_score1, count_score1 ) )
				{
System.err.print( "load_pi_helper: seq accepted by "+helperlist[i] );
System.err.println( "; class score: "+class_score1[0]+", count score: "+count_score1[0] );
					if ( class_score1[0] > max_class_score || (class_score1[0] == max_class_score && count_score1[0] > max_count_score) )
					{
						argmax_class_score = i;
						max_class_score = class_score1[0];
						max_count_score = count_score1[0];
					}
				}
			}
			catch (Exception e2)
			{
				System.err.println( "PiHelperLoader: attempt to load "+helperlist[i]+" failed; "+e2 );
			}
		}

		if ( argmax_class_score == -1 )
			throw new Exception( "PiHelperLoader: no helper for "+px.getClass().getName()+", etc." );
		
		// FOR NOW IGNORE THE POSSIBILITY OF TWO OR MORE MATCHES !!!
		Class c = RMIClassLoader.loadClass( helperlist[argmax_class_score] );
long t1 = System.currentTimeMillis();
System.err.println( "load_pi_helper: load "+c.getName()+"; elapsed: "+((t1-t0)/1000.0)+" [s]" );
		return (PiHelper) c.newInstance();
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
