/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999, Robert Dodier.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA, 02111-1307, USA,
 * or visit the GNU web site, www.gnu.org.
 */
package riso.belief_nets;

import java.util.*;
import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import riso.remote_data.*;
import riso.general.*;

/** An instance of this class contains global data related to belief networks.
  * The list of search paths for belief networks is here, as is the list
  * of belief networks for which references are known.
  * There are some other data as well.
  */
public class BeliefNetworkContext extends UnicastRemoteObject implements AbstractBeliefNetworkContext, Serializable, Perishable
{
	/** This flag tells if this object is marked as ``stale.'' If the flag is
	  * set, all remote method invocations should fail; local method calls
	  * will succeed. I wonder if that is a poor design. ???
	  */
	public boolean stale = false;

	/** The host from which belief networks in this context 
	  * are exported. This is the host on which the RMI registry must run.
	  * All belief network contexts within a given Java VM share the
	  * same registry host.
	  */
	public String registry_host = "localhost";

	/** The port on which the RMI registry runs.
	  * All belief network contexts within a given Java VM share the
	  * same registry port.
      * Exported objects listen for method calls on a different port.
	  */
	public int registry_port = Registry.REGISTRY_PORT;

	/** The name to which this context is bound in the RMI registry.
	  */
	String name = "(none)";

	/** In this table, the key is the name (a string) of a belief network
	  * and the value is a reference to the belief network. The value can
	  * be a reference to a remote belief network. Each belief network
	  * context has its own reference table.
	  */
	Hashtable reference_table = new Hashtable();

	/** This is a list of directories in which we can look for belief
	  * network files. Each belief network context has its own path list.
	  */
	Vector path_list = new Vector();

	/** This is a list of belief network context references on remote hosts, as returned by
	  * <tt>locate_context</tt>.
	  * This table should not be modified by any method other than <tt>locate_context</tt>.
	  */
	private static Hashtable locate_context_cache = new Hashtable();

	/** This simple constructor sets the registry host to the local host
	  * and adds the current directory, ".", to the path list.
	  * The <tt>server_name</tt> is the name to which this context is
	  * bound in the RMI registry.
      * The new object listens for remote method calls on the port 
      * number specified by <tt>Global.exported_objects_port</tt>.
	  */
	public BeliefNetworkContext( String server_name ) throws RemoteException
	{
        super (Global.exported_objects_port);

		name = server_name;
		try { registry_host = InetAddress.getLocalHost().getHostName(); }
		catch (java.net.UnknownHostException e) { throw new RemoteException( "BeliefNetworkContext: "+e ); }
		add_path( "." );
	}

	/** This method throws a <tt>StaleReferenceException</tt> if the this 
	  * belief network context is stale.
	  */
	void check_stale( String caller ) throws StaleReferenceException
	{
		if ( this.is_stale() )
			throw new StaleReferenceException("BeliefNetworkContext."+caller+" failed.");
	}

	/** This context is stale if its <tt>stale</tt> flag is set.
	  */
	public boolean is_stale() { return stale; }

	/** Sets the <tt>stale</tt> flag.
	  */
	public void set_stale() { stale = true; }

	/** This method returns the name with which this context is
	  * registered in the RMI registry. The string returned has the form
	  * <tt>host:port/name</tt>, so it can be used as a registry lookup argument.
	  */
	public String get_name() throws RemoteException
	{
		check_stale( "get_name" );
		return registry_host+":"+registry_port+"/"+name;
	}
	
	/** Cause the belief network context process to exit. This is to allow
	  * a remote operator to kill the context. This operation succeeds on
	  * stale contexts -- <tt>check_stale</tt> is not called.
	  */
	public void exit() throws RemoteException
	{
		try { System.err.println( "BeliefNetworkContext.exit: called from "+java.rmi.server.RemoteServer.getClientHost() ); }
		catch (ServerNotActiveException e) { System.err.println( "BeliefNetworkContext.exit: strange, exit anyway: "+e ); }

		System.exit(1);
	}

	/** Binds the given reference in the RMI registry.
	  * The URL is based on the full name of the argument <tt>bn</tt>,
	  * which has the form <tt>host.locale.domain/server-name</tt>, or
	  * <tt>host.local.domain:port/server-name</tt> if the RMI registry
	  * port is different from the default.
	  *
	  * <p> If the RMI URL constructed by this method is already bound to
	  * a belief network, this method will attempt to contact that belief
	  * network; if it is alive, this method throws an 
	  * <tt>AlreadyBoundException</tt>, but otherwise the URL is rebound
	  * to the new belief network <tt>bn</tt>.
	  */
	public void bind( AbstractBeliefNetwork bn ) throws RemoteException
	{
		check_stale( "bind" );

		try
		{
			String url = "rmi://"+bn.get_fullname();
			System.err.print( "BeliefNetworkContext.bind: url: "+url+" ..." );
			long t0 = System.currentTimeMillis();

			try { Naming.bind( url, bn ); }
			catch (AlreadyBoundException e)
			{
				Remote o = Naming.lookup(url);
				if ( o instanceof AbstractBeliefNetwork )
				{
					AbstractBeliefNetwork obn = (AbstractBeliefNetwork) o;
					try
					{
						String name = obn.get_name();
						throw new AlreadyBoundException( name+" is alive." );
					}
					catch (RemoteException e2)
					{
						System.err.println( "BeliefNetworkContext.bind: "+url+" appears to be dead ("+e2.getClass()+"); replace its binding." );
						Naming.rebind( url, bn );
					}
				}
				else
					throw new AlreadyBoundException( o.getClass()+" is not a belief network." );
			}

			long tf = System.currentTimeMillis();
			System.err.println( "success; Naming.bind time elapsed: "+((tf-t0)/1000.0)+" [s]" );
		}
		catch (Exception e)
		{
			throw new RemoteException( "BeliefNetworkContext.bind: failed: "+e );
		}
	}

	/** This method is similar to <tt>bind</tt>, but if there is already a belief network
	  * bound to the name of the given belief network <tt>bn</tt>, that belief network is
	  * marked <tt>stale</tt> and the name is rebound.
	  */
	public void rebind( AbstractBeliefNetwork bn ) throws RemoteException
	{
		check_stale( "rebind" );

		try
		{
			String url = "rmi://"+bn.get_fullname();
			System.err.print( "BeliefNetworkContext.rebind: url: "+url+" ..." );
			long t0 = System.currentTimeMillis();

			try { Naming.bind( url, bn ); }
			catch (AlreadyBoundException e)
			{
				Remote o = Naming.lookup(url);
				if ( o instanceof AbstractBeliefNetwork )
				{
					AbstractBeliefNetwork obn = (AbstractBeliefNetwork) o;
					try
					{
						String name = obn.get_name();
						System.err.println( "  "+name+" is alive; mark it stale and rebind." );
						Perishable p = (Perishable) o;
						p.set_stale();
					}
					catch (RemoteException e2)
					{
						System.err.println( "BeliefNetworkContext.rebind: "+url+" appears to be dead ("+e2.getClass()+"); replace its binding." );
					}

					Naming.rebind( url, bn );
				}
				else
					throw new AlreadyBoundException( o.getClass()+" is not a belief network." );
			}

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
	public void add_path( String path ) throws RemoteException
	{
		check_stale( "add_path" );
		System.err.println( "BeliefNetworkContext.add_path: add "+path );
		path_list.addElement( path );
	}

	/** @see AbstractBeliefNetworkContext.load_network
	  */
	public AbstractBeliefNetwork load_network( String bn_name ) throws RemoteException
	{
		check_stale( "load_network" );
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
			throw new RemoteException( "can't load "+bn_name+": "+filename+" not found on path list." );

		SmarterTokenizer st = new SmarterTokenizer( new BufferedReader( bn_fr ) );
		return load_network(st);
	}

	/** Load all belief networks described by the input stream (encapsulated in the tokenizer).
	  * Return the first one found in the input stream.
	  */
	public AbstractBeliefNetwork load_network( SmarterTokenizer st ) throws RemoteException
	{
		AbstractBeliefNetwork bn = load_one_network(st);

		while ( load_one_network(st) != null )
			;

		return bn;
	}

	/** Load the next belief network in the input stream (encapsulated in the input stream)
	  * and return it. If there is nothing in the input stream (i.e., all tokens have already
	  * been exhausted) then return null.
	  */
	public AbstractBeliefNetwork load_one_network( SmarterTokenizer st ) throws RemoteException
	{
		BeliefNetwork bn = null;
		Class bn_class = null;
		
		try
		{
			st.nextToken();
			if ( st.ttype == StreamTokenizer.TT_EOF ) return null;

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
e.printStackTrace();
			throw new RemoteException( "tokenizer state "+st+"; can't load belief network:\n"+e );
		}
		
		// Set the context of the newly-created belief network to be this context.

		bn.belief_network_context = this;

		// Put a reference to the new belief network into the list of belief networks --
		// this prevents indefinite recursions if two belief networks refer to each other.
		// bn.name hasn't been assigned yet, but we can figure out what the
		// full name ought to be; use that as the key for the bn reference.

		try { st.nextToken(); }
		catch (IOException e) { throw new RemoteException( "BeliefNetworkContext.load_one_network: can't obtain belief network name." ); }

		String bn_fullname =  registry_host+":"+registry_port+"/"+st.sval;
		reference_table.put( bn_fullname, bn );

		try
		{
			st.pushBack();	// unget the belief network name
			bn.pretty_input(st);
System.err.println( "BeliefNetworkContext.load_one_network: loaded "+bn_fullname+" successfully." );
		}
		catch (IOException e)
		{
			reference_table.remove( bn_fullname );
			throw new RemoteException( "BeliefNetworkContext.load_one_network: attempt to load "+bn_fullname+" failed:"+"\n"+e );
		}

		return bn;
	}

	/** I suppose this method should harmonize with <tt>load_network</tt>... !!!
	  * Maybe it should simply form a tokenizer for the string and call
	  * <tt>load_network(SmarterTokenizer)</tt>.
	  * @see AbstractBeliefNetworkContext.parse_network
	  */
	public AbstractBeliefNetwork parse_network( String description ) throws RemoteException
	{
		check_stale( "parse_network" );
		SmarterTokenizer st = new SmarterTokenizer( new BufferedReader( new StringReader( description ) ) );
		BeliefNetwork bn;

		try
		{
			st.nextToken();
			if ( st.ttype == StreamTokenizer.TT_EOF ) return null; // no tokens in string; quit early.
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
		// bn.name hasn't been assigned yet, but we can figure out what the
		// full name ought to be; use that as the key for the bn reference.

		try { st.nextToken(); }
		catch (IOException e) { throw new RemoteException( "BeliefNetworkContext.parse_network: can't obtain belief network name." ); }

		String bn_fullname =  registry_host+":"+registry_port+"/"+st.sval;
		reference_table.put( bn_fullname, bn );

		try
		{
			st.pushBack();	// unget the belief network name
			bn.pretty_input( st );
		}
		catch (IOException e)
		{
			reference_table.remove( bn_fullname );
			throw new RemoteException( "BeliefNetworkContext.parse_network: attempt to parse "+bn_fullname+" failed:"+"\n\t"+e  );
		}

		return bn;
	}

	/** Given the name of a belief network, this method returns a reference
	  * to that belief network. The belief network name <tt>bn_name</tt>
	  * can have the form <tt>hostname:port/something</tt>, or it might
	  * have only <tt>hostname/something<tt>, or just <tt>something</tt>.
	  * If the hostname is omitted, <tt>registry_host</tt> is prepended,
	  * along with <tt>registry_port</tt> if that is different from the
	  * default.
	  *
	  * <p> First check the list of belief nets loaded into this context,
	  * and return a reference if the b.n. is indeed loaded into this context,
	  * and if that fails then try to obtain a reference from the RMI registry
	  * running on the <tt>hostname</tt>. 
	  *
	  * <p> The reference returned is of type <tt>Remote</tt>, and thus
	  * it can be cast to any of the remote interfaces implemented by
	  * the belief network. The most important of these interfaces is
	  * <tt>AbstractBeliefNetwork</tt>, but <tt>RemoteObservable</tt> is
	  * sometimes useful as well.
	  *
	  * <p> This method does not load the belief network if it is not
	  * yet loaded, nor does it bind the belief network in the RMI registry.
	  * If a cached reference is stale, this method returns <tt>null</tt>.
	  */
	public Remote get_reference( NameInfo i ) throws RemoteException
	{
		check_stale( "get_reference" );
		Remote bn;

		// See if we can skip the host name resolution.

		String bn_name0 = i.host_name+":"+i.rmi_port+"/"+i.beliefnetwork_name;
		bn = (Remote) reference_table.get( bn_name0 );
		if ( bn != null )
		{
			try { ((AbstractBeliefNetwork)bn).get_name(); }
			catch (RemoteException e)
			{
				bn = null;
				reference_table.remove( bn_name0 );
			}
		}

		if ( bn != null ) return bn;

		// Well, see if we can skip the RMI registry lookup.
		// Construct full name of belief network.

		String hostname0 = i.host_name;

		try { i.resolve_host(); }
		catch (Exception e)
		{
e.printStackTrace();
			throw new RemoteException( "BeliefNetworkContext.get_reference: attempt to resolve host "+i.host_name+" failed." );
		}

		String bn_name = i.host_name+":"+i.rmi_port+"/"+i.beliefnetwork_name;
		bn = (Remote) reference_table.get( bn_name );
		if ( bn != null )
		{
			try { ((AbstractBeliefNetwork)bn).get_name(); }
			catch (RemoteException e)
			{
				bn = null;
				reference_table.remove( bn_name );
			}
		}

		if ( bn != null )
		{
			reference_table.put( bn_name0, bn ); // avoid future host resolves
			return bn;
		}

		// Not yet cached (or cache had a stale ref), so try the RMI registry.
		try
		{
			String url = "rmi://"+bn_name;
			bn = Naming.lookup( url );
		}
		catch (Exception e)
		{
			throw new UnknownNetworkException( "BeliefNetworkContext.get_reference: "+bn_name+" is not in this context nor in RMI registry." );
		}
		
		try { ((AbstractBeliefNetwork)bn).get_name(); }
		catch (RemoteException e)
		{
			throw new UnknownNetworkException( "BeliefNetworkContext.get_reference: RMI registry contains stale reference to "+bn_name );
		}

		reference_table.put( bn_name0, bn ); // avoid future host resolves
		reference_table.put( bn_name, bn );	// avoid future RMI lookups
		return bn;
	}

	/** This table is maintained by <tt>get_helper_names</tt> 
	  * as a cache for lists of helper names.
	  */
	protected Hashtable helper_names_table = new Hashtable();

	/** This table contains a list of the times at which the helper names
	  * table was last refreshed. The list is indexed by helper type.
	  */
	protected Hashtable hnt_refresh_times_table = new Hashtable();

	/** If the helper names table is this old or older, refresh the table.
	  * This interval is measured in milliseconds.
	  */
	protected static final long HNT_REFRESH_INTERVAL_MILLIS = 2*60*1000L;

	/** This method gets a list of the names of helper classes known to this context.
	  * The argument <tt>helper_type</tt> is usually one of the following:
	  * <ul>
	  * <li> <tt>computes_pi</tt>
	  * <li> <tt>computes_lambda</tt>
	  * <li> <tt>computes_pi_message</tt>
	  * <li> <tt>computes_lambda_message</tt>
	  * <li> <tt>computes_posterior</tt>
	  * </ul>
	  * but not necessarily, since it's just a string which is pasted into
	  * a directory path.
	  *
	  * <p> The return value is an array of strings. If this context is either a
	  * local context or a context on the codebase host, you can call
	  * <tt>Class.forName</tt> with one of these strings as the argument.
	  */
	public String[] get_helper_names( String helper_type ) throws RemoteException
	{
		String[] classnames_array = (String[]) helper_names_table.get( helper_type );
		Long hnt_refresh_time = (Long) hnt_refresh_times_table.get( helper_type );
		if ( classnames_array != null && hnt_refresh_time != null && System.currentTimeMillis() - hnt_refresh_time.longValue() < HNT_REFRESH_INTERVAL_MILLIS )
			return classnames_array;
		
		Vector classnames = new Vector();

		try
		{
			String cp = System.getProperty( "java.class.path" );
			String rp = System.getProperty( "riso.packages", "riso" );
			String ps = System.getProperty( "path.separator" );
			String fs = System.getProperty( "file.separator" );

			// Parse classpath, then attempt to list helpers.

			int i;
			Vector pathdirs = new Vector(), pkgnames = new Vector();

			while ( (i = cp.indexOf(ps)) != -1 )
			{
				pathdirs.addElement( cp.substring(0,i) );
				cp = cp.substring(i+1);
			}

			pathdirs.addElement(cp);

			while ( (i = rp.indexOf(ps)) != -1 )
			{
				pkgnames.addElement( rp.substring(0,i) );
				rp = rp.substring(i+1);
			}

			pkgnames.addElement(rp);

			for ( Enumeration e = pathdirs.elements(); e.hasMoreElements(); )
			{
				String classdir = (String)e.nextElement();

				for ( Enumeration e2 = pkgnames.elements(); e2.hasMoreElements(); )
				{
					String helperdirname = null;

					try
					{
						String pn = (String) e2.nextElement();
						helperdirname = classdir+fs+pn+fs+"distributions"+fs+"computes_"+helper_type;
						File helperdir = new File( helperdirname );
						String[] filenames = helperdir.list();

						for ( i = 0; i < filenames.length; i++ )
						{
							String cn = pn+".distributions.computes_"+helper_type+"."
									+filenames[i].substring( 0, filenames[i].lastIndexOf(".") );
							classnames.addElement(cn);
						}
					}
					catch (RMISecurityException e3)
					{
						// File.list failed because we're not allowed to read the local filesystem;
						// try to locate a context on the global RISO server and return that list.

						URL codebase = new URL( System.getProperty( "java.rmi.server.codebase" ) );
						String codebase_host = codebase.getHost();
						System.err.println( "BeliefNetworkContext.get_helper_names: IGNORE PRECEDING SECURITY EXCEPTION; get list from "+codebase_host );
						return locate_context(codebase_host).get_helper_names(helper_type);
					}
					catch (Exception e4) { /* no helpers in helperdirname */ }
				}
			}
			
		}
		catch (Exception e)
		{
			e.printStackTrace(); 
			throw new RemoteException( "BeliefNetworkContext.get_helper_names: "+e );
		}

		classnames_array = new String[ classnames.size() ];
		classnames.copyInto(classnames_array);
		helper_names_table.put( helper_type, classnames_array );
		hnt_refresh_times_table.put( helper_type, new Long(System.currentTimeMillis()) );
// System.err.println( "get_helper_names: refresh "+helper_type+" list at "+(System.currentTimeMillis()/1000L)+" [s]" );

		return classnames_array;
	}

	public static AbstractBeliefNetworkContext locate_context( String hostname ) throws Exception
	{
		AbstractBeliefNetworkContext c;
		if ( (c = (AbstractBeliefNetworkContext) locate_context_cache.get(hostname)) != null )
		{
			try { c.get_name(); return c; }
			catch (RemoteException e) {}	// if we fall through here, we keep going with new lookup
			locate_context_cache.remove(hostname);
		}

		String url = "rmi://"+hostname;
		String[] names;

        try { names = Naming.list(url); }
        catch (Exception e) { e.printStackTrace(); return null; }

        for ( int i = 0; i < names.length; i++ )
        {
            Remote o;
            try { o = Naming.lookup( names[i] ); }
            catch (Exception e)
            {
System.err.println( "locate_context: lookup failed on "+names[i] );
                continue;
            }

            if ( o instanceof AbstractBeliefNetworkContext ) 
            {
				String s;
				try { s = ((AbstractBeliefNetworkContext)o).get_name(); }
				catch (RemoteException e) { continue; }
System.err.println( "locate_context: found live context: "+s );
				locate_context_cache.put(hostname,o);
                return (AbstractBeliefNetworkContext) o;
            }
else
System.err.println( "locate_context: "+names[i]+" is not a bnc." );
        }

        System.err.println( "locate_context: can't find a context in "+url );
        throw new Exception( "locate_context failed: "+url );
	}

	/** Execute the <tt>main</tt> method in class <tt>class_name</tt>,
	  * with command-line arguments <tt>args</tt>.
	  */
	public void execute_app( String class_name, String[] args ) throws RemoteException
	{
		try { RemoteApp.invokeMain( Class.forName(class_name), args ); }
		catch (Exception e) { throw new RemoteException( "BeliefNetworkContext.execute_app: failed; "+e ); }
	}

	/** Creates a belief network context and makes it remotely visible.
	  * This method takes some command line arguments:
	  * <ul>
	  * <li><tt>-c context-name</tt> The name by which the belief network
	  *   context is remotely visible. The context name must be specified.
	  * <li><tt>-pa path-list</tt> List of paths in the style of CLASSPATH,
	  *   i.e. concatenated together, separated by colons.
	  * <li><tt>-po-registry port</tt> Port number on which <tt>rmiregistry</tt> is
	  *   listening; by default the registry port is 1099.
      * <li><tt>-po-objects</tt> Port number on which exported objects listen for calls.
      *   The default exported object port number is specified in the <tt>Global</tt> class.
      * <li><tt>-v</tt> Increase the global debugging level. More v's sets the level
      *   higher, e.g., <tt>-vvv</tt> sets the debugging level to 3.
      * <li><tt>-q</tt> Decrease the global debugging level. More q's sets the level
      *   lower, e.g., <tt>-qq</tt> sets the debugging level to -2.
	  * </ul>
	  */
	public static void main(String args[])
	{
		String server = "(none)", paths = "", host = "localhost";
		int i, registry_port = 1099;

		for ( i = 0; i < args.length; i++ )
		{
			if ( args[i].charAt(0) != '-' )
				continue;

			switch ( args[i].charAt(1) )
			{
			case 'h':
				host = args[++i];
				break;
			case 'p':
				switch ( args[i].charAt(2) )
				{
				case 'a':
					paths = args[++i];
					break;
				case 'o':
                    if ("-po".equals(args[i]) || "-po-registry".equals(args[i])) // "-po" for backwards compatibility !!!
					    registry_port = Integer.parseInt( args[++i] );
                    else if ("-po-objects".equals(args[i]))
                        Global.exported_objects_port = Integer.parseInt (args[++i]);
                    else
                        System.err.println ("BeliefNetworkContext.main: ``"+args[i]+"'' not recognized; stagger forward.");
					break;
				}
				break;
			case 'c':
				server = args[++i];
				break;
            case 'v':
                for (int j = 1; j < args[i].length() && args[i].charAt(j) == 'v'; j++)
                    ++Global.debug;
                break;
            case 'q':
                for (int j = 1; j < args[i].length() && args[i].charAt(j) == 'q'; j++)
                    --Global.debug;
                break;
			}
		}

		if ( "(none)".equals(server) )
		{
			System.err.println( "BeliefNetworkContext.main: must specify server." );
			System.exit(1);
		}

        System.err.println ("BeliefNetworkContext.main: Global.debug: "+Global.debug);
        System.err.println ("BeliefNetworkContext.main: Global.exported_objects_port: "+Global.exported_objects_port);

		try
		{
			if ( "localhost".equals(host) )
				host = InetAddress.getLocalHost().getHostName();
		
			BeliefNetworkContext bnc = new BeliefNetworkContext(server);

			bnc.registry_host = host;
			bnc.registry_port = registry_port;

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

			String url = "rmi://"+bnc.registry_host+":"+bnc.registry_port+"/"+server;
			System.err.println( "BeliefNetworkContext.main: url: "+url );

			long t0 = System.currentTimeMillis();
			try { Naming.bind( url, bnc ); }
			catch (AlreadyBoundException e)
			{
				Remote o = Naming.lookup(url);
				if ( o instanceof AbstractBeliefNetworkContext )
				{
					AbstractBeliefNetworkContext abnc = (AbstractBeliefNetworkContext) o;
					try
					{
						abnc.get_name();
						throw new AlreadyBoundException( url+" is bound and it is a belief network context." );
					}
					catch (RemoteException e2)
					{
						System.err.println( "  "+url+" seems to be a dead context; rebind." );
						Naming.rebind( url, bnc );
					}
				}
				else
					throw new AlreadyBoundException( url+" is bound and it is not a belief network context." );
			}

			riso.distributions.PiHelperLoader.bnc = bnc; // so helper loaders don't look go looking for a context !!!

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
