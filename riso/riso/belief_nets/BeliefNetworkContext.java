package riso.belief_nets;

import java.util.*;
import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import numerical.Format;
import SmarterTokenizer;

/** This class contains global data related to belief networks.
  * The list of search paths for belief networks is here, as is the list
  * of belief networks for which references are known.
  * There are some other data as well.
  */
public class BeliefNetworkContext extends UnicastRemoteObject implements AbstractBeliefNetworkContext, Serializable
{
	/** The host from which belief networks in this context 
	  * are exported. This is the host on which the RMI registry must run.
	  * All belief network contexts within a given Java VM share the
	  * same registry host.
	  */
	static public String registry_host = "localhost";

	/** The port number from which belief networks in this context 
	  * are exported. This is the port on which the RMI registry must run.
	  * All belief network contexts within a given Java VM share the
	  * same registry port.
	  */
	static public int registry_port = Registry.REGISTRY_PORT;

	/** In this table, the key is the name (a string) of a belief network
	  * and the value is a reference to the belief network. The value can
	  * be a reference to a remote belief network. Each belief network
	  * context has its own reference table.
	  */
	transient Hashtable reference_table = new Hashtable();

	/** This is a list of directories in which we can look for belief
	  * network files. Each belief network context has its own path list.
	  */
	Vector path_list = new Vector();

	/** This simple constructor sets the registry host to the local host
	  * and adds the current directory, ".", to the path list.
	  */
	public BeliefNetworkContext() throws RemoteException
	{
		try { registry_host = InetAddress.getLocalHost().getHostName(); }
		catch (java.net.UnknownHostException e) { throw new RemoteException( "BeliefNetworkContext: "+e ); }
		add_path( "." );
	}

	/** This function tries to obtain a reference to a remote belief
	  * network, and if successful puts the name and reference in the
	  * reference table.
	  * @param host_bn_name The host name and belief network name of the
	  *   remote network; it should have the form <tt>host/network-name</tt>
	  *   where host can include a site and domain (separated by dots,
	  *   as usual). To do the lookup, "<tt>rmi://</tt>" is prepended.
	  * @return A reference to the remote belief network, if successful.
	  * @throws UnknownNetworkException If for any reason the reference
	  *   cannot be obtained.
	  * @see reference_table
	  */
	AbstractBeliefNetwork add_lookup_reference( String host_bn_name ) throws UnknownNetworkException
	{
System.err.println( "BeliefNetworkContext.add_lookup_reference: "+host_bn_name );

		AbstractBeliefNetwork bn;
		try { bn = (AbstractBeliefNetwork) Naming.lookup( "rmi://"+host_bn_name ); }
		catch (Exception e)
		{
			throw new UnknownNetworkException( "attempt to look up "+host_bn_name+" failed:\n"+e );
		}

		reference_table.put(host_bn_name,bn);
		return bn;
	}

	/** Rebinds the given reference in the RMI registry.
	  * The URL is based on the full name of the argument <tt>bn</tt>,
	  * which has the form <tt>host.locale.domain/server-name</tt>, or
	  * <tt>host.local.domain:port/server-name</tt> if the RMI registry
	  * port is different from the default.
	  * This method does not modify any local tables or other data structures.
	  */
	public void rebind( AbstractBeliefNetwork bn ) throws RemoteException
	{
		try
		{
			String url = "rmi://"+bn.get_fullname();
			System.err.print( "BeliefNetworkContext.rebind: url: "+url+" ..." );
			long t0 = System.currentTimeMillis();
			Naming.rebind( url, bn );
			long tf = System.currentTimeMillis();
			System.err.println( "success; Naming.rebind time elapsed: "+((tf-t0)/1000.0)+" [s]" );
		}
		catch (Exception e)
		{
			throw new RemoteException( "BeliefNetworkContext.rebind: failed: "+e );
		}
	}

	/** Adds a path to the list of paths for this belief network context.
	  * If the path is already on the list, don't add anything.
	  */
	public void add_path( String path )
	{
		System.err.println( "BeliefNetworkContext.add_path: add "+path );
		path_list.addElement( path );
	}

	/** @see AbstractBeliefNetworkContext.load_network
	  */
	public AbstractBeliefNetwork load_network( String bn_name ) throws RemoteException
	{
System.err.println( "AbstractBeliefNetwork.load_network: "+bn_name+", codebase: "+System.getProperty( "java.rmi.server.codebase" ) );
		// Search the path list to locate the belief network file.
		// The filename must have the form "something.riso".

		// Make sure there's at least one reasonable place to look.
		path_list.addElement( "." );

		String filename = bn_name+".riso";
		FileReader bn_fr = null;
		boolean found = false;

		for ( Enumeration p = path_list.elements(); p.hasMoreElements(); )
		{
			String long_filename = ((String)p.nextElement())+"/"+filename;

			try { bn_fr = new FileReader(long_filename); }
			catch (FileNotFoundException e) { continue; }

			// If we fall out here, we successfully opened the file.
			found = true;
			break;
		}

		if ( !found )
			throw new RemoteException( "can't load "+bn_name+": not found on path list." );

		SmarterTokenizer st = new SmarterTokenizer( new BufferedReader( bn_fr ) );
		BeliefNetwork bn;
		Class bn_class = null;
		
		try
		{
			st.nextToken();
			bn_class = java.rmi.server.RMIClassLoader.loadClass( st.sval );
			bn = (riso.belief_nets.BeliefNetwork) bn_class.newInstance();
		}
		catch (ClassNotFoundException e)
		{
			throw new RemoteException( "can't load belief network class: "+st.sval+"; nested exception:\n"+e );
		}
		catch (ClassCastException e)
		{
			throw new RemoteException( "can't load belief network: "+bn_class+" isn't a belief network class; nested exception:\n"+e );
		}
		catch (Exception e)
		{
			throw new RemoteException( "can't load belief network:\n"+e );
		}
		
		// Set the context of the newly-created belief network to be this context.

		bn.belief_network_context = this;

		// Put a reference to the new belief network into the list of belief networks --
		// this prevents indefinite recursions if two belief networks refer to each other.

		reference_table.put(bn_name,bn);

		try { bn.pretty_input(st); }
		catch (IOException e)
		{
			reference_table.remove( bn_name );
			throw new RemoteException( "BeliefNetworkContext.load_network: attempt to load "+bn.get_fullname()+" failed:"+"\n"+e );
		}

		return bn;
	}

	/** @see AbstractBeliefNetworkContext.parse_network
	  */
	public AbstractBeliefNetwork parse_network( String description ) throws RemoteException
	{
		SmarterTokenizer st = new SmarterTokenizer( new BufferedReader( new StringReader( description ) ) );
		BeliefNetwork bn;

		try
		{
			st.nextToken();
			Class bn_class = java.rmi.server.RMIClassLoader.loadClass( st.sval );
			bn = (BeliefNetwork) bn_class.newInstance();
		}
		catch (ClassNotFoundException e)
		{
			throw new RemoteException( "BeliefNetworkContext.parse_network: can't find belief network class: "+st.sval );
		}
		catch (ClassCastException e)
		{
			throw new RemoteException( "BeliefNetworkContext.parse_network: can't load belief network: "+st.sval+" isn't a belief network class." );
		}
		catch (Exception e)
		{
			throw new RemoteException( "BeliefNetworkContext.parse_network: can't load belief network:\n"+e );
		}
		
		// Set the context of the newly-created belief network to be this context.

		bn.belief_network_context = this;

		// Put a reference to the new belief network into the list of belief
		// networks -- this prevents indefinite recursions if two belief
		// networks refer to each other.

		String bn_name = "";

		try
		{
			st.nextToken();
			bn_name = st.sval;
			reference_table.put( bn_name, bn );
			st.pushBack();	// unget the belief network name
			bn.pretty_input( st );
		}
		catch (IOException e)
		{
			if ( ! "".equals(bn_name) ) reference_table.remove( bn_name );
			throw new RemoteException( "BeliefNetworkContext.parse_network: attempt to parse "+bn.get_fullname()+" failed:"+"\n"+e  );
		}

		return bn;
	}

	/** Given the name of a belief network, this method returns a reference
	  * to that belief network. The belief network name <tt>bn_name</tt>
	  * has the form <tt>something</tt> or <tt>qualified-hostname/something</tt>
	  * -- if the former, first check the list of belief nets loaded into
	  * this context, and return a reference if the b.n. is indeed loaded
	  * into this context, and if that fails then try to obtain a reference
	  * from the RMI registry running on the local host; if the latter,
	  * a reference is sought in the RMI registry running on the named host.
	  *
	  * <p> The reference returned is of type <tt>Remote</tt>, and thus
	  * it can be cast to any of the remote interfaces implemented by
	  * the belief network. The most important of these interfaces is
	  * <tt>AbstractBeliefNetwork</tt>, but <tt>RemoteObservable</tt> is
	  * sometimes useful as well.
	  *
	  * <p> This method does not load the belief network if it is not
	  * yet loaded, nor does it bind the belief network in the RMI registry.
	  */
	public Remote get_reference( String bn_name ) throws RemoteException
	{
		Remote bn;

		int sindex = bn_name.lastIndexOf( '/' );
		if ( sindex == -1 )
		{
			// No host specified; look in this context's list of b.n.'s.
			bn = (Remote) reference_table.get( bn_name );
			if ( bn != null ) return bn;

			// Try the local host's RMI registry.
			try
			{
				String url = "rmi://"+registry_host+"/"+bn_name;
				bn = Naming.lookup( url );
				reference_table.put( bn_name, bn );
				return bn;
			}
			catch (Exception e)
			{
				throw new UnknownNetworkException( "BeliefNetworkContext.get_reference: "+bn_name+" is not in this context nor in local RMI registry." );
			}
		}
		else
		{
			// Try the remote host's RMI registry.
			try
			{
				String url = "rmi://"+bn_name;	// bn_name already has host+"/"
				bn = Naming.lookup( url );
				return bn;
			}
			catch (Exception e)
			{
				throw new UnknownNetworkException( "BeliefNetworkContext.get_reference: "+bn_name+" is not in remote RMI registry." );
			}
		}
	}

	/** Creates a belief network context and makes it remotely visible.
	  * This method takes some command line arguments:
	  * <ul>
	  * <li><tt>-h host</tt> The name of the host on which <tt>rmiregistry</tt>
	  *   is running. The host must be specified.
	  * <li><tt>-s server</tt> The name by which the belief network
	  *   context is remotely visible. The server name must be specified.
	  * <li><tt>-pa path-list</tt> List of paths in the style of CLASSPATH,
	  *   i.e. concatenated together, separated by colons.
	  * <li><tt>-po port</tt> Port number on which <tt>rmiregistry</tt> is
	  *   listening; by default the port is 1099.
	  * </ul>
	  */
	public static void main(String args[])
	{
		System.setSecurityManager(new RMISecurityManager());

		String server = "(none)", paths = ".";
		int i;

		for ( i = 0; i < args.length; i++ )
		{
			if ( args[i].charAt(0) != '-' )
				continue;

			switch ( args[i].charAt(1) )
			{
			case 'h':
				BeliefNetworkContext.registry_host = args[++i];
				break;
			case 'p':
				switch ( args[i].charAt(2) )
				{
				case 'a':
					paths = args[++i];
					break;
				case 'o':
					BeliefNetworkContext.registry_port = Format.atoi( args[++i] );
					break;
				}
				break;
			case 's':
				server = args[++i];
				break;
			}
		}

		if ( "(none)".equals(server) )
		{
			System.err.println( "BeliefNetworkContext.main: must specify server." );
			System.exit(1);
		}

		try
		{
			if ( "localhost".equals(registry_host) )
				registry_host = InetAddress.getLocalHost().getHostName();
		
			BeliefNetworkContext bnc = new BeliefNetworkContext();

			while ( paths.length() > 0 )
			{
				int colon_index;
				if ( (colon_index = paths.indexOf(":")) == -1 )
				{
					// This is the last or sole path in the list; add it, then list becomes empty.
					bnc.add_path( paths );
					paths = "";
				}
				else
				{
					String path = paths.substring( 0, colon_index );
					bnc.add_path( path );
					paths = paths.substring( colon_index+1 );	// the rest of the list
				}
			}

			String url = "rmi://"+BeliefNetworkContext.registry_host+":"+BeliefNetworkContext.registry_port+"/"+server;
			System.err.println( "BeliefNetworkContext.main: url: "+url );
			long t0 = System.currentTimeMillis();
			Naming.rebind( url, bnc );
			long tf = System.currentTimeMillis();
			System.err.println( "BeliefNetworkContext.main: "+server+" bound in registry; time elapsed: "+((tf-t0)/1000.0)+" [s]" );
		}
		catch (Exception e)
		{
			System.err.println("BeliefNetworkContext.main: exception:");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
