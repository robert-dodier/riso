package riso.utility_models;
import java.io.*;
import riso.belief_nets.*;
import riso.general.SmarterTokenizer;

public abstract class AbstractUtilityModel implements UtilityModel
{
	public AbstractVariable associated_variable;

	/** Return a copy of this conditional distribution.
	  * This implementation copies the <tt>associated_variable</tt> reference (i.e., the
	  * reference is not cloned).
	  * A subclass <tt>clone</tt> method should call this one.
	  * The method should look like this:
	  * <pre>
	  * public Object clone() throws CloneNotSupportedException
	  * {
	  *     Subclass copy = (Subclass) super.clone();
	  *	[subclass-specific code goes here]
	  *     return copy;
	  * }
	  * </pre>
	  */
	public Object clone() throws CloneNotSupportedException 
	{
		try
		{
			AbstractUtilityModel copy = (AbstractUtilityModel) this.getClass().newInstance();
			copy.associated_variable = this.associated_variable;
			return copy;
		}
		catch (Exception e) { throw new CloneNotSupportedException( this.getClass().getName()+".clone failed; nested: "+e ); }
	}

	/** Create a description of this distribution model as a string.
	  * For subclasses, this is a full description, suitable for printing, containing
	  * newlines and indents. 
	  * For this abstract class, this method prints only the class name and an empty pair
	  * of braces. The <tt>leading_ws</tt> argument is ignored by this method.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		return this.getClass().getName()+" { }\n";
	}

	/** Cache a reference to the variable with which this conditional distribution
	  * is associated.
	  */
	public void set_variable( Variable x ) { associated_variable = x; }

	/** Parse a string containing a description of an instance of this distribution.
	  * The description is contained within curly braces, which are included in the string.
	  *
	  * <p> This method forms a tokenizer from the string and passes off the job to
	  * <tt>pretty_input</tt>. 
	  * This implementation is sufficient for many derived classes.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Read an instance of this distribution from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  *
	  * <p> The implementation of <tt>pretty_input</tt> in this class does nothing;
	  * this is sufficient for several distribution types which have no parameters
	  * or other descriptive information.
	  *
	  * @param st Stream tokenizer to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException { return; }

	/** Write an instance of this distribution to an output stream.
	  * This method forms a <tt>PrintStream</tt> from the <tt>OutputStream</tt> given,
	  * and passess off the job to <tt>format_string</tt>. 
	  * This implementation is sufficient for many derived classes.
	  *
	  * @param os The output stream to print on.
	  * @param leading_ws Since the representation is only one line of output, 
	  *   this argument is ignored.
	  * @throws IOException If the output fails; this is possible, but unlikely.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	/** Return a string representation of this object.
	  */
	public String toString()
	{
		try { return format_string(""); }
		catch (IOException e) { return this.getClass().getName()+".toString failed; "+e; }
	}

	public static void main( String[] args )
	{
		try
		{
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );
			st.nextToken();
			UtilityModel um = (UtilityModel) Class.forName( st.sval ).newInstance();
			st.nextBlock();
			um.parse_string( st.sval );
			System.out.println( "utility model:\n"+um.format_string("") );
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
