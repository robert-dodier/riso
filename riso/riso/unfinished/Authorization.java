package riso.belief_nets;
import java.io.*;
import java.rmi.*;
import java.util.*;
import gnu.rex.*;
import SmarterTokenizer;

/** This class holds authorization info for RISO belief networks. 
  * A method in the class tests whether or not a specified remote belief network
  * is allowed to send data to the belief network holding this authorization object.
  */
public class Authorization
{
	Vector authorize = new Vector();

	public void accept( String regexpr ) throws RegExprSyntaxException
	{
		authorize.addElement( new AuthorizePair( AuthorizePair.ACCEPT, regexpr ) );
	}

	public void reject( String regexpr ) throws RegExprSyntaxException
	{
		authorize.addElement( new AuthorizePair( AuthorizePair.REJECT, regexpr ) );
	}

	public boolean allow( BeliefNetwork bn, AbstractBeliefNetwork remote_bn ) throws RemoteException
	{
		if ( remote_bn == bn ) return true; // bn is always allowed to send to itself.

		return allow( remote_bn.get_fullname() );
	}

	public boolean allow( String name_string )
	{
		// THIS TEST IS A TERRIBLE HACK -- THE REMOTE BN COULD EASILY SPOOF. !!!
		
		char[] name_chars = new char[ name_string.length() ];
		name_string.getChars( 0, name_chars.length, name_chars, 0 );

		boolean accepted = false;

		for ( Enumeration e = authorize.elements(); e.hasMoreElements(); )
		{
			AuthorizePair p = (AuthorizePair) e.nextElement();
			if ( p.mode == AuthorizePair.ACCEPT )
			{
				RexResult r = p.remote_pattern.match( name_chars, 0, name_chars.length );
System.err.print( "allow: in ACCEPT branch: "+r );
if ( r != null ) System.err.println( ": "+String.copyValueOf(name_chars, r.offset(),r.length()) );
else System.err.println("");
				accepted |= (r != null);
			}
			else // assume REJECT
			{
				RexResult r = p.remote_pattern.match( name_chars, 0, name_chars.length );
System.err.print( "allow: in REJECT branch: "+r );
if ( r != null ) System.err.println( ": "+String.copyValueOf(name_chars, r.offset(),r.length()) );
else System.err.println("");
				if ( r != null ) return false; // rejected
			}
		}

		return accepted;
	}

	public static void main( String[] args )
	{
		try
		{
			Authorization authorize = new Authorization();

			for ( int i = 0; i < args.length; i++ )
			{
				switch ( args[i].charAt(1) )
				{
				case 'a':
					authorize.accept( args[++i] );
					break;
				case 'r':
					authorize.reject( args[++i] );
					break;
				}
			}

			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );
			for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				System.out.println( (authorize.allow(st.sval)?"ACCEPT":"REJECT")+" "+st.sval );
			}
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}

class AuthorizePair
{
	static int ACCEPT = 1, REJECT = 2;
	int mode;
	Rex remote_pattern;

	AuthorizePair( int mode, String regexpr ) throws RegExprSyntaxException
	{
		this.mode = mode;
		remote_pattern = Rex.build( regexpr );
	}
}
