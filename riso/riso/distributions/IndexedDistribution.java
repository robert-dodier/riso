package riso.distributions;
import java.io.*;
import java.rmi.*;
import java.util.*;
import riso.belief_nets.*;
import SmarterTokenizer;

/** An instance of this class represents a set of conditional distributions
  * which are indexed by one or more discrete variables.
  */
public class IndexedDistribution extends AbstractConditionalDistribution
{
	/** List of the names of the parents which are indexes.
	  */
	String[] index_names;

	/** List of which parents are indexes. E.g., if the parents are <tt>X</tt>, <tt>Y</tt>,
	  * <tt>Z</tt>, and <tt>W</tt>, and the first and third are indexes, then the list has 2 elements,
	  * namely 0 and 2.
	  */
	public int[] indexes;

	/** List of which parents are NOT indexes. E.g., if the parents are <tt>X</tt>, <tt>Y</tt>,
	  * <tt>Z</tt>, and <tt>W</tt>, and the first and third are indexes, then the list has 2 elements,
	  * namely 1 and 3.
	  */
	public int[] non_indexes;

	/** Dimensions of the parents which are indexes. E.g., if one parent is an index which takes
	  * on 10 values (0 through 9), and another takes on 7 values, and a third index takes on
	  * 13 values, then this list has 3 elements, 7, 10, and 13.
	  */
	public int[] index_dimensions;

	/** List of conditional distributions indexed within this distribution. The list is stored
	  * flat, in row-major order.
	  */
	public ConditionalDistribution[] components;

	/** Put off parsing the components until they are needed, since the
	  * parsing cannot be done before the whole belief network has been
	  * parsed, because we need a pointer to each parent and those pointers
	  * haven't been assigned when Variable.pretty_input is called.
	  */
	private String components_string;

	/** Allocate this only once; contains non-index values for p().
	  */
	private double[] c2;

	/** If the components' descriptions have not yet been parsed,
	  * do so now. If components are already parsed, do nothing.
	  * @throws IllegalArgumentException If index assignment fails.
	  * @throws IOException If the description parsing fails.
	  * @throws RemoteException If the attempt to reference parents or
	  *   belief network fails.
	  */
	public void check_components() throws IOException, IllegalArgumentException
	{
		if ( components == null )
		{
			try { assign_indexes(); }
			catch (IllegalArgumentException e) { throw new IllegalArgumentException( "IndexedDistribution.check_components: attempt to assign indexes failed: "+e ); }
			catch (Exception e) { throw new IllegalArgumentException( "IndexedDistribution.check_components: strange; "+e ); }
			try { parse_components_string(); }
			catch (IOException e) { throw new IOException( "IndexedDistribution.check_components: attempt to parse components string failed:\n"+e ); }
		}
	}

	/** Return a deep copy of this object. If this object is remote,
	  * <tt>remote_clone</tt> will create a new remote object.
	  */
	public Object remote_clone() throws CloneNotSupportedException
	{
		IndexedDistribution copy = new IndexedDistribution();

		copy.associated_variable = associated_variable;

		return copy;
	}

	/** Return the number of dimensions of the child variable.
	  */
	public int ndimensions_child() { return 1; }

	/** Return the number of dimensions of the parent variables.
	  * If there is more than one parent, this is the sum of the dimensions
	  * of the parent variables.
	  */
	public int ndimensions_parent() { return indexes.length+non_indexes.length; }

	/** For a given value <code>c</code> of the parents, return a distribution
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  *
	  * <p> In the case of indexed distributions, we'll use the indexes in 
	  * <tt>c</tt> to obtain the distribution <tt>q</tt> of interest, then use the
	  * rest of the elements (call them <tt>c2</tt> collectively) to obtain
	  * <tt>q.get_density(c2)</tt>.
	  */
	public Distribution get_density( double[] c ) throws Exception
	{
		check_components();

		int i, j;

		for ( i = 0, j = 0; i < indexes.length-1; i++ )
			j = index_dimensions[i+1] * (j + (int) c[ indexes[i] ]);
		j += (int) c[ indexes[ indexes.length-1 ] ];

		ConditionalDistribution q = components[j];

		for ( i = 0; i < non_indexes.length; i++ )
			c2[i] = c[ non_indexes[i] ];

		return q.get_density( c2 );
	}

	/** Compute the density at the point <code>x</code>.
	  *
	  * @param x Point at which to evaluate density.
	  * @param c Values of parent variables. First fish out indexes, use indexes to 
	  *   obtain distribution <tt>q</tt> of interest, and then use the 
	  *   rest of the elements (call them <tt>c2</tt> collectively) to obtain
	  *   <tt>q.p(x,c2)</tt>.
	  */
	public double p( double[] x, double[] c ) throws Exception
	{
		check_components();

		int i, j;

		for ( i = 0, j = 0; i < indexes.length-1; i++ )
			j = index_dimensions[i+1] * (j + (int) c[ indexes[i] ]);
		j += (int) c[ indexes[ indexes.length-1 ] ];

		ConditionalDistribution q = components[j];

		for ( i = 0; i < non_indexes.length; i++ )
			c2[i] = c[ non_indexes[i] ];

		double qpxc2 = q.p( x, c2 );
		return qpxc2;
	}

	/** Return an instance of a random variable from this distribution.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws Exception
	{
		throw new Exception( "IndexedDistribution.random: not implemented." );
	}

	/** Parse a string containing a description of a variable. The description
	  * is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Create a description of this indexed distribution model as a string.
	  * This is a full description, suitable for printing, containing
	  * newlines and indents.
	  *
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		String result = "";
		int i, j;

		result += this.getClass().getName()+"\n"+leading_ws+"{"+"\n";
		String more_leading_ws = "\t"+leading_ws;
		String still_more_ws = "\t"+more_leading_ws;
		
		result += more_leading_ws+"index-variables { ";
		for ( i = 0; i < index_names.length; i++ )
			result += index_names[i]+" ";
		result += "}"+"\n";

		result += more_leading_ws+"components"+"\n";
		String formatted_components;

		try
		{
			// If the attempt to parse and then format this distribution fails,
			// just output the components string itself -- see catch below.

			check_components();
			int[] slab_length = new int[ index_dimensions.length ];
			slab_length[ index_dimensions.length-1 ] = 1;

			for ( i = index_dimensions.length-2; i >= 0; i-- )
				slab_length[i] = slab_length[i+1] * index_dimensions[i+1];

			formatted_components = more_leading_ws+"{"+"\n";
			for ( i = 0; i < components.length; i++ )
			{
				formatted_components += still_more_ws+"% component";
				for ( j = 0; j < index_dimensions.length; j++ )
				{
					int k = (i / slab_length[j]) % index_dimensions[j];
					formatted_components += "["+k+"]";
				}
				formatted_components += "\n"+still_more_ws;
				formatted_components += components[i].format_string( still_more_ws );
			}

			formatted_components += more_leading_ws+"}"+"\n";
		}
		catch (Exception e)
		{
			formatted_components = more_leading_ws+"% components not parsed due to "+e.getClass().getName();
			formatted_components += more_leading_ws+components_string+"\n";
		}

		result += formatted_components;
		result += leading_ws+"}"+"\n";

		return result;
	}

	/** Read a description of this indexed distribution from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param is Input stream to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "IndexedDistribution.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "index-variables" ) )
				{
					st.nextToken();
					if ( st.ttype != '{' ) throw new IOException( "IndexedDistribution.pretty_input: ``index-variables'' lacks opening bracket." );

					Vector names = new Vector();
					for ( st.nextToken(); st.ttype != '}' && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
						names.addElement( st.sval );

					index_names = new String[ names.size() ];
					names.copyInto( index_names );

					if ( st.ttype != '}' ) throw new IOException( "IndexedDistribution.pretty_input: ``index-variables'' lacks closing bracket." );
				}
				else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "components" ) )
				{
					st.nextBlock();
					components_string = st.sval;
				}
				else if ( st.ttype == '}' )
				{
					found_closing_bracket = true;
					break;
				}
			}
		}
		catch (IOException e)
		{
			throw new IOException( "IndexedDistribution.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "IndexedDistribution.pretty_input: no closing bracket on input." );
	}

	/** Parse the string representation of the components of this indexed
	  * distribution. The string was saved by <tt>pretty_input</tt>, since
	  * it could not be parsed then. See <tt>components_string</tt>.
	  */
	void parse_components_string() throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( components_string ) );

		st.nextToken();
		if ( st.ttype != '{' ) throw new IOException( "IndexedDistribution.parse_components_string: ``components'' lacks opening bracket." );

		int i, j, ncomponents = 1;
		for ( i = 0; i < index_dimensions.length; i++ )
			ncomponents *= index_dimensions[i];

		components = new ConditionalDistribution[ ncomponents ];

		for ( i = 0; i < ncomponents; i++ )
		{
			st.nextToken();
			try { components[i] = (ConditionalDistribution) java.rmi.server.RMIClassLoader.loadClass( st.sval ).newInstance(); }
			catch (Exception e) { throw new IOException( "IndexedDistribution.parse_components_string: attempt to instantiate "+st.sval+" failed:\n"+e ); }

			st.nextBlock();
			try { components[i].parse_string( st.sval ); }
			catch (Exception e) { throw new IOException( "IndexedDistribution.parse_components_string: attempt to parse component["+i+"] failed:\n"+e ); }
		}

		st.nextToken();
		if ( st.ttype != '}' ) throw new IOException( "IndexedDistribution.parse_components_string: ``components'' lacks closing bracket." );
	}

	/** Write a description of this indexed distribution to an output stream.
	  * The description is human-readable; this is different from object
	  * serialization. 
	  * @param os Output stream to write to.
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  * @throws IOException If the attempt to write the model fails.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	/** Uses information about parents to cache indexing lists. 
	  * This method can only be called after parent references have been
	  * assigned within the belief network.
	  * @throws IllegalArgumentException If parent references have not yet been assigned.
	  * @throws RemoteException If the attempt to reference parents or
	  *    belief network fails.
	  * @throws Exception If the attempt to count parent states fails.
	  */
	void assign_indexes() throws IllegalArgumentException, RemoteException, Exception
	{
		indexes = new int[ index_names.length ];
		index_dimensions = new int[ index_names.length ];
		AbstractVariable[] parents = null;
		parents = associated_variable.get_parents();

		non_indexes = new int[ parents.length - indexes.length ];
		c2 = new double[ non_indexes.length ];

		int i, j, k;

		for ( i = 0; i < parents.length; i++ )
			if ( parents[i] == null )
				throw new IllegalArgumentException( "IndexedDistribution.assign_indexes: parent["+i+"] is null." );

		for ( i = 0; i < index_names.length; i++ )
		{
			boolean found = false;

			for ( j = 0; j < parents.length; j++ )
			{
				String parent_name;
				if ( parents[j].get_bn() == associated_variable.get_bn() )
					parent_name = parents[j].get_name();
				else
					parent_name = parents[j].get_fullname();
				
				if ( index_names[i].equals( parent_name ) )
				{
					indexes[i] = j;
					ConditionalDistribution q = parents[j].get_distribution();
					if ( q instanceof ConditionalDiscrete )
					{
						int nqparents = parents[j].get_parents().length;
						double[] s = ((Discrete)(q.get_density( new double[nqparents] ))).effective_support( 1e-1 );
						index_dimensions[i] = ((int)s[1])+1;
					}
					else if ( q instanceof Discrete )
					{
						double[] s = ((Discrete)q).effective_support( 1e-1 );
						index_dimensions[i] = ((int)s[1])+1;
					}
					else
						throw new IllegalArgumentException( "IndexedDistribution.assign_indexes: parent["+j+"] is wrong class: "+q.getClass() );

					found = true;
					break;
				}
			}	 

			if ( !found )
				throw new IllegalArgumentException( "IndexedDistribution.assign_indexes: can't find index variable: "+index_names[i] );
		}

		for ( i = 0, k = 0; i < parents.length; i++ )
		{
			boolean found = false;
			for ( j = 0; j < indexes.length; j++ )
				if ( i == indexes[j] )
				{
					found = true;
					break;
				}
			
			if ( !found ) non_indexes[k++] = i;
		}
	}
}
