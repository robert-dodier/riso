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

		Vector pi_classes = PiHelperLoader.get_local_superclasses( pi );

		Vector lambda_names = new Vector();
		PiHelperLoader.make_classname_list( lambda_names, remaining_lambda_messages, false, null, 0 );

		// Outer loop is over class names of lambda messages;
		// inner loop is over class names of pi.

		for ( Enumeration enum = lambda_names.elements(); enum.hasMoreElements(); )
		{
			String s = (String) enum.nextElement();

			for ( Enumeration enum2 = pi_classes.elements(); enum2.hasMoreElements(); )
			{
				String class_name = ((Class)enum2.nextElement()).getName();
				String pi_name = class_name.substring( class_name.lastIndexOf('.')+1 );

				String helper_name = "riso.distributions.computes_pi_message."+pi_name+"_"+s;
				
				try
				{
					Class helper_class = java.rmi.server.RMIClassLoader.loadClass( helper_name );
					PiMessageHelper pmh = (PiMessageHelper) helper_class.newInstance();
					return pmh;
				}
				catch (ClassNotFoundException e2)
				{
// System.err.println( "PiMessageHelperLoader.load_pi_message_helper: helper not found:" );
// System.err.println( "  "+helper_name );
				}
			}
		}
	
		// If we fall out here, we weren't able to locate an appropriate helper.
		return null;
	}
}
