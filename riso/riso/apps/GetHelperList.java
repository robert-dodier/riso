package riso.apps;
import java.net.*;
import java.rmi.*;
import java.lang.reflect.*;
import riso.belief_nets.*;
import SeqTriple;

public class GetHelperList
{
	public static void main( String[] args )
	{
		try
		{
			String cb = System.getProperty( "java.rmi.server.codebase" );
			System.err.println( "codebase: "+cb );

			AbstractBeliefNetworkContext bnc = (new BeliefNetworkContext(null)).locate_context( new URL(cb).getHost() );
			System.err.println( "obtained context: "+bnc.get_name() );

			String[] helperlist = bnc.get_helper_names( args[0] );

			System.err.println( args[0]+" helpers: " );
			for ( int i = 0; i < helperlist.length; i++ )
			{
				System.err.print( helperlist[i]+" " );
				try
				{
					Class c = java.rmi.server.RMIClassLoader.loadClass( helperlist[i] );
					System.err.println( "(OK)" );
					SeqTriple[] a = (SeqTriple[]) invoke_description(c);
					if ( a == null ) continue;
					for ( int j = 0; j < a.length; j++ )
						System.err.println( "\t"+a[j] );
				}
				catch (Exception e2) { System.err.println( "(NOT OK)" ); }
			}
		}
		catch (Exception e) { e.printStackTrace(); }

		System.exit(1);
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
