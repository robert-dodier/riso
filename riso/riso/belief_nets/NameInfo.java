package riso.belief_nets;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.*;
import numerical.Format;

public class NameInfo
{
	String host_name = null, beliefnetwork_name = null, variable_name = null;
	int rmi_port = Registry.REGISTRY_PORT;

	InetAddress host = null;
	Remote beliefnetwork = null;
	AbstractVariable variable = null;

	public void resolve_host() throws Exception
	{
		if ( host != null ) return;
		String host_address = InetAddress.getByName(host_name).getHostAddress();
		host = InetAddress.getByName(host_address);
		host_name = host.getHostName();
System.err.println( "NameInfo.resolve_host: host: "+host+", host_name: "+host_name );
	}

	public void resolve_beliefnetwork() throws Exception
	{
		if ( beliefnetwork != null ) return;
		if ( host == null ) resolve_host();
		String url = "rmi://"+host_name+":"+rmi_port+"/"+beliefnetwork_name;
System.err.println( "NameInfo.resolve_beliefnetwork: url: "+url );
		beliefnetwork = Naming.lookup( url );
	}

	public void resolve_variable() throws Exception
	{
		if ( variable != null ) return;
		if ( beliefnetwork == null ) resolve_beliefnetwork();
		variable = ((AbstractBeliefNetwork)beliefnetwork).name_lookup(variable_name);
System.err.println( "NameInfo.resolve_variable: variable.get_fullname: "+variable.get_fullname() );
	}

	public static NameInfo parse_variable( String name, BeliefNetworkContext context )
	{
		return parse( name, context, true );
	}

	public static NameInfo parse_beliefnetwork( String name, BeliefNetworkContext context )
	{
		return parse( name, context, false );
	}

	public static NameInfo parse( String name, BeliefNetworkContext context, boolean is_variable )
	{
		int slash_index = name.indexOf("/"), colon_index = name.indexOf(":");

		// This next snippet will change if I ever implement nested namespaces.
		int period_index = name.lastIndexOf(".");

		NameInfo info = new NameInfo();

		if ( slash_index == -1 )
		{
			// No host specified; assume the registry host of the context.
			// No RMI port specified; assume the registry port of the context.

			if ( context != null )
			{
				info.host_name = context.registry_host;
				info.rmi_port = context.registry_port;
			}
		}
		else
		{
			if ( colon_index == -1 )
			{
				// Extract specified host.
				info.host_name = name.substring(0,slash_index);

				// No RMI port specified; assume that of the context.
				if ( context != null )
					info.rmi_port = context.registry_port;
			}
			else
			{
				// Extract specified host.
				info.host_name = name.substring(0,colon_index);

				// Extract specified RMI port.
				info.rmi_port = Format.atoi( name.substring(0,slash_index).substring(colon_index+1) );
			}
		}

		if ( period_index == -1 )
		{
			if ( slash_index == -1 )
			{
				// Simple name.
				if ( is_variable )
				{
					// beliefnetwork_name remains null.
					info.variable_name = name;
				}
				else
				{
					info.beliefnetwork_name = name;
					// variable_name remains null.
				}
			}
			else
			{
				// Must be a belief network name -- "something/something-else".
				info.beliefnetwork_name = name.substring(slash_index+1);
				// variable_name remains null.
			}
		}
		else
		{
			// Extract belief network name and variable name.
			// Next line works correctly when slash_index == -1.
			info.beliefnetwork_name = name.substring(slash_index+1,period_index);
			info.variable_name = name.substring(period_index+1);
		}

System.err.println( "NameInfo.parse: name: "+name+", variable?  "+(is_variable?"YES":"NO") );
System.err.println( "\t"+"host_name: "+info.host_name );
System.err.println( "\t"+"rmi_port: "+info.rmi_port );
System.err.println( "\t"+"beliefnetwork_name: "+info.beliefnetwork_name );
System.err.println( "\t"+"variable_name: "+info.variable_name );
		return info;
	}

	public static void main( String[] args )
	{
		try
		{
			NameInfo i = NameInfo.parse( args[1], null, "v".equals(args[0]) );
			if ( "b".equals(args[0]) )
				i.resolve_beliefnetwork();
			else
				i.resolve_variable();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
