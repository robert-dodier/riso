package risotto.distributions;

public class PiHelperLoader
{
	public static PiHelper load_pi_helper( ConditionalDistribution px, Distribution[] pi_messages ) throws Exception
	{
		int iperiod;
		String class_name = px.getClass().getName();
		iperiod = class_name.lastIndexOf('.');
		String px_name = class_name.substring( iperiod+1 );

		String pi_names_with_counts = make_classname_list( pi_messages, true );
		String helper_name_with_counts = "risotto.distributions.ComputesPi_"+px_name+"_"+pi_names_with_counts;

		try
		{
			Class helper_class = Class.forName( helper_name_with_counts );
			return (PiHelper) helper_class.newInstance();
		}
		catch (Exception e1)
		{
System.err.println( "PiHelperLoader.load_pi_helper: helper not found:" );
System.err.println( "  "+helper_name_with_counts );
			String pi_names_without_counts = make_classname_list( pi_messages, false );
			String helper_name_without_counts = "risotto.distributions.ComputesPi_"+px_name+"_"+pi_names_without_counts;

			try
			{
				Class helper_class = Class.forName( helper_name_without_counts );
				return (PiHelper) helper_class.newInstance();
			}
			catch (Exception e2)
			{
System.err.println( "PiHelperLoader.load_pi_helper: helper not found:" );
System.err.println( "  "+helper_name_without_counts );
				return null;
			}
		}
	}

	public static String make_classname_list( Object[] items, boolean insert_counts )
	{
		if ( items == null || items.length == 0 )
			return "";

		int i, iperiod, nthis_kind = 0;
		String class_name, prev_name = "", this_name, items_names = "";
		boolean first_time = true;

		for ( i = 0; i < items.length; i++ )
		{
			if ( items[i] == null ) continue;

			if ( first_time )
			{
				class_name = items[i].getClass().getName();
				if ( (iperiod = class_name.lastIndexOf('.')) == -1 )
					prev_name = class_name;
				else
					prev_name = class_name.substring( iperiod+1 );
				nthis_kind = 1;
				first_time = false;
			}

			class_name = items[i].getClass().getName();
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
}
