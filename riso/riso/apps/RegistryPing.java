package riso.apps;
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
			String[] entries;
			
			try { entries = Naming.list(url); }
			catch (ConnectException e)
			{
				System.err.println( "RegistryPing: can't connect to "+url );
				return;
			}

			for ( int i = 0; i < entries.length; i++ )
			{
				Remote o;
				AbstractBeliefNetwork bn;
				AbstractBeliefNetworkContext bnc;
				String s;
				
				try { o = Naming.lookup( entries[i] ); }
				catch (Exception e) 
				{
					System.err.println( "unbind "+entries[i]+"; failed lookup: "+e );
					try { Naming.unbind( entries[i] ); }
					catch (Exception e2) {}
					continue;
				}

				bn = null;
				bnc = null;

				try { bn = (AbstractBeliefNetwork) o; }
				catch (Exception e)
				{
					try { bnc = (AbstractBeliefNetworkContext) o; }
					catch (Exception e2)
					{
						System.err.println( entries[i]+" is not a belief network nor a b.n. context." );
						continue;
					}
				}

				try { if ( bn != null ) s = bn.get_name(); else s = bnc.get_name(); }
				catch (Exception e)
				{
					System.err.println( "unbind "+entries[i]+"; appears to be dead: "+e.getClass() );
					try { Naming.unbind( entries[i] ); }
					catch (Exception e2) {}
					continue;
				}

				System.err.println( entries[i]+" appears to be alive." );
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
