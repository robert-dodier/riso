package riso.distributions;
import java.util.*;

public class PiHelperLoader
{
	public static PiHelper load_pi_helper( ConditionalDistribution px, Distribution[] pi_messages ) throws Exception
	{
		if ( pi_messages.length == 0 )
			return new TrivialPiHelper();

		Vector pi_names = new Vector();
		make_classname_list( pi_names, pi_messages, false, null, 0 );

		Vector px_classes = get_local_superclasses( px );

		// Outer loop is over class names of pi messages; 
		// inner loop is over class names of the conditional distribution.

		for ( Enumeration enum = pi_names.elements(); enum.hasMoreElements(); )
		{
			String s = (String) enum.nextElement();

			for ( Enumeration enum2 = px_classes.elements(); enum2.hasMoreElements(); )
			{
				String class_name = ((Class)enum2.nextElement()).getName();
				String px_name = class_name.substring( class_name.lastIndexOf('.')+1 );
			
				String helper_name = "riso.distributions.ComputesPi_"+px_name+"_"+s;

				try
				{
					Class helper_class = Class.forName( helper_name );
					return (PiHelper) helper_class.newInstance();
				}
				catch (ClassNotFoundException e)
				{
System.err.println( "PiHelperLoader.load_pi_helper: helper not found:" );
System.err.println( "  "+helper_name );
				}
			}
		}

		// If we fall out here, we weren't able to locate an appropriate helper.
		return null;
	}

	public static void make_classname_list( Vector list, Object[] items, boolean insert_counts, Class[] items_classes, int m )
	{
		if ( items == null || items.length == 0 )
		{
			list.addElement( "" );
			return;
		}

		if ( m == 0 )
			items_classes = new Class[ items.length ];

		if ( m == items.length )
			list.addElement( make_classname_list( items_classes, insert_counts ) );
		else
		{
			if ( items[m] == null )
			{
				// Skip over this item; null items occur as placeholders in pi message lists.
				items_classes[m] = null;
				make_classname_list( list, items, insert_counts, items_classes, m+1 );
			}
			else
			{
				Vector superclasses = get_local_superclasses( items[m] );
				for ( Enumeration e = superclasses.elements(); e.hasMoreElements(); )
				{
					items_classes[m] = (Class) e.nextElement();
					make_classname_list( list, items, insert_counts, items_classes, m+1 );
				}
			}
		}
	}

	public static String make_classname_list( Class[] items_classes, boolean insert_counts )
	{
		int i, iperiod, nthis_kind = 0;
		String class_name, prev_name = "", this_name, items_names = "";
		boolean first_time = true;

		for ( i = 0; i < items_classes.length; i++ )
		{
			if ( items_classes[i] == null ) continue;

			if ( first_time )
			{
				class_name = items_classes[i].getName();
				if ( (iperiod = class_name.lastIndexOf('.')) == -1 )
					prev_name = class_name;
				else
					prev_name = class_name.substring( iperiod+1 );
				nthis_kind = 1;
				first_time = false;
			}

			class_name = items_classes[i].getName();
			if ( (iperiod = class_name.lastIndexOf('.')) == -1 )
				this_name = class_name;
			else
				this_name = class_name.substring( iperiod+1 );

			if ( prev_name.equals( this_name ) )
				++nthis_kind;
			else
			{
				if ( insert_counts )
					items_names = items_names + nthis_kind + prev_name;
				else
					items_names = items_names + prev_name;

				prev_name = this_name;
				nthis_kind = 1;
			}
		}

		if ( insert_counts )
			items_names = items_names + nthis_kind + prev_name;
		else
			items_names = items_names + prev_name;

		return items_names;
	}

	/** Return a list of the class of the object <tt>a</tt> and its
	  * local superclasses. A "local" superclass is one which is in
	  * some <tt>riso</tt> package. The list is constructed with
	  * the class first, then superclasses in reverse order (i.e.,
	  * with the root local class last).
	  */
	static Vector get_local_superclasses( Object a )
	{
		Vector list = new Vector();
		Class c = a.getClass();

		do list.addElement( c ); while ( (c = c.getSuperclass()).getName().startsWith( "riso." ) );

System.err.println( "get_local_superclasses: "+list );
		return list;
	}
}
