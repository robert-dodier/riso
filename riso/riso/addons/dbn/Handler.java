package riso.addons.dbn;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.rmi.*;
import riso.belief_nets.*;

public class Handler extends URLStreamHandler
{
	protected URLConnection openConnection(URL u) throws IOException
	{
System.err.println( this.getClass().getName()+": URL: "+u );
		return new dbnURLConnection(u);
	}
}

class dbnURLConnection extends URLConnection
{
	URL u;
	AbstractBeliefNetwork bn;

	dbnURLConnection( URL u ) { super(u); this.u = u; }

	public void connect() throws IOException
	{
		if ( connected ) return;

		int port = (u.getPort() == -1 ? 1099 : u.getPort());

		if ( u.getFile().equals("/") )
		{
			// No belief network name given. Later on we'll list the contents of the
			// registry on the specified host, but for now there's nothing to do.

			connected = true; // well, not really -- the connection hasn't been tested.
			return;
		}

		String bn_name = u.getFile();
		if ( bn_name.indexOf(".") != -1 ) bn_name = bn_name.substring(0,bn_name.indexOf("."));

		String rmi_url = "rmi://"+u.getHost()+":"+port+bn_name;
System.err.println( this.getClass().getName()+": rmi_url: "+rmi_url );
		try { bn = (AbstractBeliefNetwork) Naming.lookup( rmi_url ); }
		catch (NotBoundException e) { throw new IOException( this.getClass().getName()+": can't find "+rmi_url+"; nested: "+e ); }

System.err.println( this.getClass().getName()+": connected." );
		connected = true;
	}

	public InputStream getInputStream() throws IOException
	{
		if ( !connected ) connect();

		String s;
		int n;

		if ( u.getFile().equals("/") ) 
		{
			int port = (u.getPort() == -1 ? 1099 : u.getPort());
			s = "<head><title>Registry list of "+u.getHost()+":"+port+"</title></head>\n";
			s += "<body>\n";
			s += registry_ping();
			s += "</body>\n";
		}
		else if ( (n = u.getFile().indexOf(".")) == -1 )
		{
			// Belief network name only, no variable name given.
System.err.println( this.getClass().getName()+": return stream for "+bn.get_fullname()+"." );
			s = riso.apps.Riso2HTML.format_string(bn);
		}
		else
		{
			// A variable name is given. See if there is an additional "get_<something>";
			// if so formulate the appropriate query.

			String query_name = null, x_name = u.getFile().substring(n+1);

			if ( (n = x_name.lastIndexOf(".")) != -1 )
			{
				query_name = x_name.substring(n+1);
				x_name = x_name.substring(0,n);
			}

			AbstractVariable x = (AbstractVariable) bn.name_lookup( x_name );
System.err.println( this.getClass().getName()+": return stream for "+x.get_fullname()+"." );

			if ( query_name == null )
			{
				// Show links to query functions, as well as a description of the variable.
				s = "<head><title>Description of variable "+x.get_fullname()+"</title></head>";
				s += "<body>";
				s += "Get <a href=\"dbn://"+x.get_fullname()+".posterior\">"+x.get_fullname()+".posterior"+"</a><br>\n";
				s += "Get <a href=\"dbn://"+x.get_fullname()+".pi\">"+x.get_fullname()+".pi"+"</a><br>\n";
				s += "Get <a href=\"dbn://"+x.get_fullname()+".lambda\">"+x.get_fullname()+".lambda"+"</a><br>\n";
				s += "<hr>Description:<br>\n";
				s += "<pre>\n"+x.format_string("")+"</pre>\n";
				s += "</body>";
			}
			else
			{
				s = "<head><title>Query result for "+x.get_fullname()+"</title></head>";
				s += "<body>";
				s += "<pre><h3>"+x.get_fullname()+"."+query_name+":</h3></pre><br><hr>\n";
				s += "<pre>"+invoke_query(x,query_name)+"</pre>\n";
				s += "</body>";
			}
		}

		return new ByteArrayInputStream( s.getBytes() );
	}

	public String getContentType() { return "text/html"; }

	public Object invoke_query( Object o, String query_name )
	{
		try
		{
			Class c = o.getClass();
			Method m = c.getMethod( "get_"+query_name, new Class[] {} );

			try { return m.invoke( o, null ); }
			catch (InvocationTargetException ite)
			{
				System.err.println( this.getClass().getName()+".invoke_query: invocation failed; " );
				ite.getTargetException().printStackTrace();
				return ite;
			}
			catch (Exception e) { return e; }
		}
		catch (NoSuchMethodException nsme) { return nsme; }
	}

	public String registry_ping()
	{
		String s = "<ul>\n";
		String[] entries;
		int port = (u.getPort() == -1 ? 1099 : u.getPort());

		try { entries = Naming.list( "rmi://"+u.getHost()+":"+port ); }
		catch (Exception e) { return "Registry list failed: "+e; }

		for ( int i = 0; i < entries.length; i++ )
		{
			Remote o;
			
			try { o = Naming.lookup( entries[i] ); }
			catch (Exception e) { continue; } // eat it, stagger on.

			AbstractBeliefNetwork bn = null;
			AbstractBeliefNetworkContext bnc = null;

			try { bn = (AbstractBeliefNetwork) o; }
			catch (Exception e)
			{
				try { bnc = (AbstractBeliefNetworkContext) o; }
				catch (Exception e2) { continue; } // neither b.n. nor b.n.c.
			}

			s += "<li>"+(o instanceof AbstractBeliefNetwork?"Belief network":"Belief network context")+" ";
			s += entries[i]+" ";

			try { if ( bn != null ) bn.get_name(); else bnc.get_name(); }
			catch (Exception e)
			{
				s += "(DEAD)\n";
				continue;
			}

			if ( o instanceof AbstractBeliefNetwork )
				try { s += "maps to <a href=\"dbn://"+bn.get_fullname()+"\">"+bn.get_fullname()+"</a><br>\n"; }
				catch (Exception e) { s += " (OOPS; "+e+")<br>\n"; }
			else
				s += "<br>\n";
		}

		return s+"</ul>";
	}
}
