package riso.remote_data;

import java.io.*;
import java.rmi.*;
import java.util.*;
import riso.belief_nets.*;
import riso.distributions.*;

/** Assign random values to some variable.
  */
public class RandomEvidence
{
	/** Generate random data and assign it to a variable in a belief network.
	  * The command line arguments are:
	  * <pre>
	  *   java riso.remote_data.RandomEvidence [-h rmi-host] [-s server-name]
	  * </pre>
	  * The <tt>rmi-host</tt> is the name of the host running <tt>rmiregistry</tt>.
	  * The <tt>server-name</tt> is the name by which this data source will be known.
	  */
	public static void main( String[] args )
	{
		String bn_name = null, variable_name = null;
		int i, j;

		for ( i = 0; i < args.length; i++ )
		{
			switch ( args[i].charAt(1) )
			{
			case 'b':
				bn_name = args[++i];
				break;
			case 'x':
				variable_name = args[++i];
				break;
			}
		}

		System.err.println( "RandomEvidence: bn_name: "+bn_name+" variable_name: "+variable_name );

		try
		{
			String url = "rmi://"+bn_name;
			AbstractBeliefNetwork bn = (AbstractBeliefNetwork) Naming.lookup( url );
			AbstractVariable v = bn.name_lookup( variable_name );
			Distribution p = bn.get_posterior( v );
			System.err.println( "RandomEvidence: sample from: " );
			System.err.println( p.format_string("\t") );

			while ( true )
			{
				double[] x = p.random();
				bn.assign_evidence( v, x[0] );
				try { Thread.sleep( 10000 ); } catch(InterruptedException e) {}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
