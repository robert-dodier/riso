package riso.utility_models;
import java.io.*;
import java.util.*;
import riso.belief_nets.*;
import SmarterTokenizer;

public class DiscreteLottery implements Lottery
{
	public double[] payoffs = new double[0];

	public DiscreteLottery() {}

	/** Return a deep copy of this object.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		DiscreteLottery copy = (DiscreteLottery) super.clone();
		copy.payoffs = (double[]) this.payoffs.clone();
		return copy;
	}

	/** Parse a string containing a description of a lottery. The description
	  * is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Create a description of this lottery as a string.
	  * @param leading_ws This argument is ignored.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		String result = this.getClass().getName()+" { ";
		for ( int i = 0; i < payoffs.length; i++ ) result += payoffs[i]+" ";
		result += "}\n";
		return result;
	}

	/** Read a description of this distribution from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		Vector payoffs_vector = new Vector(10);

		try
		{
			st.nextToken(); // eat the opening brace

			// Read a list of payoffs and plop them into a vector; we'll convert to array later.

			for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == '}' ) break;

				payoffs_vector.addElement( st.sval );
			}
		}
		catch (IOException e)
		{
			throw new IOException( "DiscreteLottery.pretty_input: attempt to read object failed:\n"+e );
		}

		payoffs = new double[ payoffs_vector.size() ];
		for ( int i = 0; i < payoffs.length; i++ ) payoffs[i] = numerical.Format.atof( (String) payoffs_vector.elementAt(i) );
	}

	/** MOVE THIS METHOD INTO AbstractLottery CLASS ??? (It seems like a good idea to have a basic test
	  * <tt>main</tt> which could be used for all derived classes...)
	  */
	public static void main( String[] args )
	{
		try
		{
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );
			st.nextToken();
			Lottery l = (Lottery) Class.forName( st.sval ).newInstance();
			st.nextBlock();
			l.parse_string( st.sval );
			System.out.println( "lottery: "+l.format_string("") );
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
