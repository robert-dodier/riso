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
		String host_address = InetAddress.getByName(host_name).getHostAddress();
		host = InetAddress.getByName(host_address);
		host_name = host.getHostName();
System.err.println( "NameInfo.resolve_host: host: "+host+", host_name: "+host_name );
	}

	public void resolve_beliefnetwork() throws Exception
	{
		if ( host == null ) resolve_host();
		String url = "rmi://"+host_name+":"+rmi_port+"/"+beliefnetwork_name;
System.err.println( "NameInfo.resolve_beliefnetwork: url: "+url );
		beliefnetwork = Naming.lookup( url );
	}

	public void resolve_variable() throws Exception
	{
		if ( beliefnetwork == null ) resolve_beliefnetwork();
		variable = ((AbstractBeliefNetwork)beliefnetwork).name_lookup(variable_name);
System.err.println( "NameInfo.resolve_variable: variable.get_fullname: "+variable.get_fullname() );
	}

	public NameInfo( String name, BeliefNetwork context_bn )
	{
		int slash_index = name.indexOf("/"), colon_index = name.indexOf(":");

		// This next snippet will change if I ever implement nested namespaces.
		int period_index = name.lastIndexOf(".");

		if ( slash_index == -1 )
		{
			// No host specified; assume the registry host of the context bn.
			// No RMI port specified; assume default.

			if ( context_bn != null )
			{
				host_name = context_bn.belief_network_context.registry_host;
				rmi_port = context_bn.belief_network_context.registry_port;
			}
		}
		else
		{
			if ( colon_index == -1 )
			{
				// Extract specified host.
				host_name = name.substring(0,slash_index);

				// No RMI port specified; assume default.
				if ( context_bn != null )
					rmi_port = context_bn.belief_network_context.registry_port;
			}
			else
			{
				// Extract specified host.
				host_name = name.substring(0,colon_index);

				// Extract specified RMI port.
				rmi_port = Format.atoi( name.substring(0,slash_index).substring(colon_index+1) );
			}
		}

		if ( period_index == -1 )
		{
			if ( slash_index == -1 )
			{
				// No enclosing namespace specified; assume name is a variable.
				if ( context_bn != null )
					beliefnetwork_name = context_bn.name;
				variable_name = name;
			}
			else
			{
				beliefnetwork_name = name.substring(slash_index+1);
				// variable_name remains null.
			}
		}
		else
		{
			// Extract belief network name and variable name.
			// Next line works correctly when slash_index == -1.
			beliefnetwork_name = name.substring(slash_index+1,period_index);
			variable_name = name.substring(period_index+1);
		}

System.err.println( "NameInfo: name: "+name );
System.err.println( "\t"+"host_name: "+host_name );
System.err.println( "\t"+"rmi_port: "+rmi_port );
System.err.println( "\t"+"beliefnetwork_name: "+beliefnetwork_name );
System.err.println( "\t"+"variable_name: "+variable_name );
	}

	public static void main( String[] args )
	{
		try
		{
			NameInfo i = new NameInfo( args[0], null );
			i.resolve_variable();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
