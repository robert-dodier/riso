package risotto.belief_nets;

import java.util.*;
import java.io.*;
import java.rmi.*;
import java.rmi.registry.*;
import SmarterTokenizer;

/** This class contains global static data related to belief networks.
  * The list of search paths for belief networks is here, as is the list
  * of belief networks for which references are known.
  * There are some other data as well.
  */
public class BeliefNetworkContext
{
	/** The port number from which belief networks in this context 
	  * are exported. This is the port on which the RMI registry must run.
	  * The RMI registry must run on the local host, so host is not
	  * a variable here (since it can't be changed).
	  */
	static public int registry_port = Registry.REGISTRY_PORT;

	/** In this table, the key is the name (a string) of a belief network
	  * and the value is a reference to the belief network. The value can
	  * be a reference to a remote belief network.
	  */
	static Hashtable reference_table = new Hashtable();

	/** This is a list of directories in which we can look for belief
	  * network files.
	  */
	static Vector path_list = new Vector();

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
	static AbstractBeliefNetwork add_rmi_reference( String host_bn_name ) throws UnknownNetworkException
	{
System.err.println( "AbstractBeliefNetwork.add_rmi_reference: "+host_bn_name );

		AbstractBeliefNetwork bn;
		try { bn = (AbstractBeliefNetwork) Naming.lookup( "rmi://"+host_bn_name ); }
		catch (Exception e)
		{
			throw new UnknownNetworkException( "attempt to look up "+host_bn_name+" failed:\n"+e );
		}

		reference_table.put(host_bn_name,bn);
		return bn;
	}

	/** This function searches the path list to locate the belief network
	  * file. The filename must have the form "<tt>something.riso</tt>".
	  * The name of the belief network in this case is "<tt>something</tt>".
	  * @param bn_name Name of the belief network; "<tt>.riso</tt>" is added
	  *   by this function.
	  * @return A reference to the belief network loaded into memory.
	  * @throws UnknownNetworkException If the network cannot be located.
	  * @throws IOException If the network can be located, but not read in
	  *   successfully.
	  */
	public static AbstractBeliefNetwork load_network( String bn_name ) throws UnknownNetworkException, IOException
	{
System.err.println( "AbstractBeliefNetwork.load_network: "+bn_name );
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
			throw new UnknownNetworkException( "can't load "+bn_name+": not found on path list." );

		SmarterTokenizer st = new SmarterTokenizer(bn_fr);
		BeliefNetwork bn;

		try
		{
			st.nextToken();
			Class bn_class = Class.forName(st.sval);
			bn = (BeliefNetwork) bn_class.newInstance();
		}
		catch (ClassNotFoundException e)
		{
			throw new IOException( "can't load belief network class: "+st.sval );
		}
		catch (ClassCastException e)
		{
			throw new IOException( "can't load belief network: "+st.sval+" isn't a belief network class." );
		}
		catch (Exception e)
		{
			throw new IOException( "can't load belief network:\n"+e );
		}
		
		// Put a reference to the new belief network into the list of belief networks --
		// this prevents indefinite recursions if two belief networks refer to each other.

		reference_table.put(bn_name,bn);

		try { bn.pretty_input(st); }
		catch (IOException e)
		{
			reference_table.remove( bn_name );
			e.fillInStackTrace();
			throw e;
		}

		return bn;
	}

	public static AbstractBeliefNetwork parse_network( String description ) throws RemoteException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		BeliefNetwork bn;

		try
		{
			st.nextToken();
			Class bn_class = Class.forName(st.sval);
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
			throw new RemoteException( "BeliefNetworkContext.parse_network: attempt to parse "+bn.get_name()+" failed." );
		}

		return bn;
	}

	/** Given the name of a belief network, this method returns a reference
	  * to that belief network. If the belief network is not already loaded,
	  * it is loaded.
	  */
	public static AbstractBeliefNetwork get_reference( String bn_name ) throws UnknownNetworkException, IOException
	{
		AbstractBeliefNetwork bn = (AbstractBeliefNetwork) reference_table.get( bn_name );
		if ( bn != null )
			return bn;
		else
			return load_network( bn_name );
	}

	/** Add a path to the list of paths for this belief network context.
	  * If the path is already on the list, don't add anything.
	  */
	public static void add_path( String path )
	{
		path_list.addElement( path );
	}
}

