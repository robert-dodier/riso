package riso.addons.dbn;
import java.io.*;
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

		if ( u.getFile().indexOf(".") == -1 )
		{
System.err.println( this.getClass().getName()+": return stream for "+bn.get_fullname()+"." );
			s = riso.apps.Riso2HTML.format_string(bn);
		}
		else
		{
			String x_name = u.getFile().substring( u.getFile().indexOf(".")+1 );
			AbstractVariable x = (AbstractVariable) bn.name_lookup( x_name );
System.err.println( this.getClass().getName()+": return stream for "+x.get_fullname()+"." );
			s = "<pre>\n"+x.format_string("")+"</pre>\n";
		}

		return new ByteArrayInputStream( s.getBytes() );
	}

	public String getContentType() { return "text/html"; }
}
