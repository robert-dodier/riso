package riso.apps;
import java.net.*;
import java.rmi.*;
import riso.belief_nets.*;

public class GetHelperList
{
	public static void main( String[] args )
	{
		try
		{
			String cb = System.getProperty( "java.rmi.server.codebase" );
			System.err.println( "codebase: "+cb );

			AbstractBeliefNetworkContext bnc = locate_context( new URL(cb).getHost() );
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
				}
				catch (Exception e2) { System.err.println( "(NOT OK)" ); }
			}
		}
		catch (Exception e) { e.printStackTrace(); }
	}

	public static AbstractBeliefNetworkContext locate_context( String hostname ) throws Exception
	{
		String url = "rmi://"+hostname;
		String[] names;

        try { names = Naming.list(url); }
        catch (Exception e) { e.printStackTrace(); return null; }

        for ( int i = 0; i < names.length; i++ )
        {
            Remote o;
            try { o = Naming.lookup( names[i] ); }
            catch (Exception e) { continue; }

            if ( o instanceof AbstractBeliefNetworkContext ) 
            {
                return (AbstractBeliefNetworkContext) o;
            }
        }

        System.err.println( "locate_context: can't find a context in "+url );
        throw new Exception( "locate_context failed: "+url );
	}
}
