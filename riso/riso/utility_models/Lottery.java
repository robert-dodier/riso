package riso.utility_models;
import java.io.*;
import riso.belief_nets.*;

public interface Lottery
{
	/** Return a deep copy of this object.
	  */
	public Object clone() throws CloneNotSupportedException;

	/** Parse a string containing a description of a lottery. The description
	  * is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException;

	/** Create a description of this lottery as a string.
	  * This is a full description, suitable for printing, containing
	  * newlines and indents.
	  *
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public String format_string( String leading_ws ) throws IOException;
}
