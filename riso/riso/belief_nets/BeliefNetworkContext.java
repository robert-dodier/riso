package belief_nets;
import java.util.*;
import java.io.*;
import java.rmi.*;
import SmarterTokenizer;

class BeliefNetworkContext
{
	static Hashtable reference_table = null;
	static String[] path_list = null;

	static AbstractBeliefNetwork add_rmi_reference( String bn_rmi_url ) throws UnknownNetworkException
	{
		int i = bn_rmi_url.lastIndexOf("/");
		String bn_name = bn_rmi_url.substring(i+1);

		AbstractBeliefNetwork bn;
		try { bn = (AbstractBeliefNetwork) Naming.lookup(bn_rmi_url); }
		catch (Exception e)
		{
			throw new UnknownNetworkException( "attempt to look up "+bn_rmi_url+" failed: "+e );
		}

		reference_table.put(bn_rmi_url,bn);
		return bn;
	}

	static AbstractBeliefNetwork load_network( String bn_name ) throws UnknownNetworkException
	{
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
			catch (FileNotFoundException e) {
			System.err.println("not found: "+long_filename);
			continue; }

			// If we fall out here, we successfully opened the file.
			found = true;
			System.err.println( "found: "+long_filename );
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
			throw new UnknownNetworkException( "can't load belief network class: "+st.sval );
		}
		catch (ClassCastException e)
		{
			throw new UnknownNetworkException( "can't load belief network: "+st.sval+" isn't a belief network class." );
		}
		catch (Exception e)
		{
			throw new UnknownNetworkException( "can't load belief network: "+e );
		}
		
		try { bn.pretty_input(st); }
		catch (IOException e)
		{
			throw new UnknownNetworkException( "can't load belief network: "+e );
		}

		reference_table.put(bn_name,bn);
		return bn;
	}
}

