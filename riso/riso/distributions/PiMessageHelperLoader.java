package riso.distributions;
import java.util.*;

public class PiMessageHelperLoader
{
	/** @return <tt>null</tt> if an appropriate message helper class cannot be located.
	  */
	public static PiMessageHelper load_pi_message_helper( Distribution pi,  Distribution[] lambda_messages ) throws Exception
	{
		// Before constructing the list of names of lambda message classes,
		// strike out any Noninformative messages. If all messages are Noninformative,
		// then load a trivial helper.

		int ninformative = 0;
		Distribution[] remaining_lambda_messages = new Distribution[ lambda_messages.length ];
		for ( int i = 0; i < lambda_messages.length; i++ )
			if ( lambda_messages[i] != null && ! (lambda_messages[i] instanceof Noninformative) )
			{
				++ninformative;
				remaining_lambda_messages[i] = lambda_messages[i];
			}

		if ( ninformative == 0 )
			return new TrivialPiMessageHelper();

		Vector seq = new Vector();
		seq.addElement( pi.getClass() );
		for ( int i = 0; i < remaining_lambda_messages.length; i++ )
			if ( remaining_lambda_messages[i] != null )
				seq.addElement( remaining_lambda_messages[i].getClass() );

		Class c = PiHelperLoader.find_helper_class( seq, "pi_message" );
		return (PiMessageHelper) c.newInstance();
	}
}
