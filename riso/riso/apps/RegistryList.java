package riso.apps;
import java.rmi.*;

public class RegistryList
{
	public static void main( String[] args )
	{
		String host, port = "1099";

		host = args[0];
		if ( args.length > 1 )
			port = args[1];

		try
		{
			String url = "rmi://"+host+":"+port+"/";
			System.err.println( "url: "+url );
			String[] names = Naming.list( url );
			for ( int i = 0; i < names.length; i++ )
				System.out.println( "in rmi registry: "+names[i] );

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
