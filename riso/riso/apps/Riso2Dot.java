package riso.apps;

import java.io.*;
import riso.belief_nets.*;
import SmarterTokenizer;

public class Riso2Dot
{
	public static void main( String[] args )
	{
		try
		{
			System.setSecurityManager( new java.rmi.RMISecurityManager() );
			BeliefNetworkContext bnc = new BeliefNetworkContext(null);
			AbstractBeliefNetwork bn = bnc.load_network( args[0] );
			System.out.print( bn.dot_format() );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		System.exit(1);
	}
}
