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
			System.setSecurityManager(new java.rmi.RMISecurityManager());
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );
			st.nextToken();
			Class bn_class = java.rmi.server.RMIClassLoader.loadClass( st.sval );
			BeliefNetwork bn = (BeliefNetwork) bn_class.newInstance();
			bn.pretty_input( st );

			System.out.print( bn.dot_format() );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		System.exit(1);
	}
}
