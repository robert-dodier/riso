package riso.render;

import java.rmi.*;
import java.util.*;
import riso.belief_nets.*;
import riso.distributions.*;
import riso.remote_data.*;

public class TextObserver extends RemoteObserverImpl
{
	public TextObserver() throws RemoteException {}

	/** This method is called by the variable being watched after 
	  * the variable has changed. When that happens, print out a report
	  * about the current posterior distribution of the variable.
	  */
	public void update( RemoteObservable o, Object of_interest, Object arg ) throws RemoteException
	{
		try
		{
			AbstractVariable x = (AbstractVariable) of_interest;
			Distribution p = (Distribution) arg;
			System.out.println( "local time: "+(new Date())+" "+x.get_fullname()+" mean: "+p.expected_value()+", stddev: "+p.sqrt_variance() );
		}
		catch (Exception e)
		{
			System.err.println( "TextObserver.update: barf: "+e );
		}
	}

	/** Register this object as an observer interested in the
	  * variables listed on the command line by <tt>-v</tt> options.
	  */
	public static void main( String args[] )
	{
		try
		{
			BeliefNetworkContext bnc = new BeliefNetworkContext(null);
			Remote bn = null;
			String bn_name = null;

			TextObserver to = new TextObserver();

			for ( int i = 0; i < args.length; i++ )
			{
				if ( args[i].charAt(0) != '-' ) continue;

				switch ( args[i].charAt(1) )
				{
				case 'b':
					bn_name = args[++i];
					try { bn = bnc.get_reference( bn_name ); }
					catch (Exception e)
					{
						System.err.println( "TextObserver: can't get reference to "+bn_name+"; give up." );
						e.printStackTrace();
						System.exit(1);
					}
					break;
				case 'v':
					String xname = args[++i];
					if ( bn_name == null )
						System.err.println( "TextObserver: can't get reference to "+xname+"; don't know belief network yet." );
					else
					{
						try
						{
							AbstractVariable x = ((AbstractBeliefNetwork)bn).name_lookup( xname );
							((RemoteObservable)bn).add_observer( to, x );
						}
						catch (Exception e)
						{
							System.err.println( "TextObserver: attempt to process "+xname+" failed." );
							e.printStackTrace();
						}
					}
					break;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
