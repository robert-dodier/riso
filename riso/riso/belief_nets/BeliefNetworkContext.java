package belief_nets;
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
class BeliefNetworkContext
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
	static String[] path_list = null;

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
	  * file. The filename must have the form "<tt>something.bn</tt>".
	  * The name of the belief network in this case is "<tt>something</tt>".
	  * @param bn_name Name of the belief network; "<tt>.bn</tt>" is added
	  *   by this function.
	  * @return A reference to the belief network loaded into memory.
	  * @throws UnknownNetworkException If the network cannot be located.
	  * @throws IOException If the network can be located, but not read in
	  *   successfully.
	  */
	static AbstractBeliefNetwork load_network( String bn_name ) throws UnknownNetworkException, IOException
	{
System.err.println( "AbstractBeliefNetwork.load_network: "+bn_name );
		// Search the path list to locate the belief network file.
		// The filename must have the form "something.bn".

		if ( path_list == null )
			throw new UnknownNetworkException( "can't load "+bn_name+": null path." );

		String filename = bn_name+".bn";
		FileReader bn_fr = null;
		boolean found = false;

		for ( int i = 0; i < path_list.length; i++ )
		{
			String long_filename = path_list[i]+"/"+filename;

			try { bn_fr = new FileReader(long_filename); }
			catch (FileNotFoundException e) { continue; }

			// If we fall out here, we successfully opened the file.
			found = true;
			break;
		}

		if ( !found )
			throw new UnknownNetworkException( "can't load "+bn_name+": not found on path list." );

		SmarterTokenizer st = new SmarterTokenizer(bn_fr);
		AbstractBeliefNetwork bn;

		try
		{
			st.nextToken();
			Class bn_class = Class.forName(st.sval);
			bn = (AbstractBeliefNetwork) bn_class.newInstance();
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
		
		try { bn.pretty_input(st); }
		catch (IOException e)
		{
			throw new IOException( "can't load belief network:\n"+e );
		}

		reference_table.put(bn_name,bn);
		return bn;
	}
}

