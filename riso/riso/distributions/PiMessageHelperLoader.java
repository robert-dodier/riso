package risotto.distributions;

public class PiMessageHelperLoader
{
	/** @return <tt>null</tt> if an appropriate message helper class cannot be located.
	  */
	public static PiMessageHelper load_pi_message_helper( Distribution pi,  Distribution[] lambda_messages ) throws Exception
	{
		// First check to see if there are any lambda messages to take into account.
		// If not, load the trivial pi message helper.

		boolean trivial = true;
		for ( int i = 0; i < lambda_messages.length; i++ )
			if ( lambda_messages[i] != null )
			{
				trivial = false;
				break;
			}

		if ( trivial ) return new TrivialPiMessageHelper();

		int iperiod;
		String pi_name, class_name = pi.getClass().getName();

		iperiod = class_name.lastIndexOf('.');
		pi_name = class_name.substring( iperiod+1 );	// works even if lastIndexOf returns -1

		String lambda_names_with_counts = PiHelperLoader.make_classname_list( lambda_messages, true );
		String helper_name_with_counts = "risotto.distributions.ComputesPiMessage_"+pi_name+"_"+lambda_names_with_counts;

		try
		{
			Class helper_class = Class.forName( helper_name_with_counts );
			PiMessageHelper pmh = (PiMessageHelper) helper_class.newInstance();
System.err.println( "PiMessageHelperLoader.load_pi_message_helper: load helper:" );
System.err.println( "  "+helper_name_with_counts );
			return pmh;
		}
		catch (ClassNotFoundException e1)
		{
System.err.println( "PiMessageHelperLoader.load_pi_message_helper: helper not found:" );
System.err.println( "  "+helper_name_with_counts );
			String lambda_names_without_counts = PiHelperLoader.make_classname_list( lambda_messages, false );
			String helper_name_without_counts = "risotto.distributions.ComputesPiMessage_"+pi_name+"_"+lambda_names_without_counts;

			try
			{
				Class helper_class = Class.forName( helper_name_without_counts );
				PiMessageHelper pmh = (PiMessageHelper) helper_class.newInstance();
System.err.println( "PiMessageHelperLoader.load_pi_message_helper: load helper:" );
System.err.println( "  "+helper_name_with_counts );
				return pmh;
			}
			catch (ClassNotFoundException e2)
			{
System.err.println( "PiMessageHelperLoader.load_pi_message_helper: helper not found:" );
System.err.println( "  "+helper_name_without_counts );
				return null;
			}
		}
	}
}
