import java.io.*;
import java.rmi.*;
import riso.belief_nets.*;

public class RegistryPing
{
	public static void main(String args[])
	{
		try
		{
			String name = "sonero", port = "1099";
			if ( args.length == 1 ) name = args[0];
			if ( args.length == 2 ) port = args[1];
			String url = "rmi://"+name+":"+port;

			System.out.println( "url: "+url );
			String[] entries = Naming.list(url);

			for ( int i = 0; i < entries.length; i++ )
			{
				System.out.println( "entry: "+entries[i] );

				Remote o;
				AbstractBeliefNetwork bn;
				String s;
				
				try { o = Naming.lookup( entries[i] ); }
				catch (Exception e) 
				{
					System.err.println( "unbind "+entries[i]+"; failed lookup: "+e );
					try { Naming.unbind( entries[i] ); }
					catch (Exception e2) {}
					continue;
				}

				try { bn = (AbstractBeliefNetwork) o; }
				catch (Exception e)
				{
					System.err.println( "failed class cast: "+e );
					continue;
				}

				try { s = bn.get_name(); }
				catch (Exception e)
				{
					System.err.println( "unbind "+entries[i]+"; failed get_name: "+e.getClass() );
					try { Naming.unbind( entries[i] ); }
					catch (Exception e2) {}
					continue;
				}

				System.err.println( "appears to be alive; name: "+s );
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
