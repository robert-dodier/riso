package riso.distributions;
import java.util.*;

public class LambdaMessageHelperLoader
{
	/** Puts together a name of the form <tt>computes_lambda_message.P_Q_R</tt>
	  * where <tt>P</tt> is the name of the class of the conditional
	  * distribution of the message's sender, <tt>Q</tt> is the name of
	  * the class of lambda (i.e., p(e+_X|x)) for the message sender, and
	  * <tt>R</tt> is a list of the names of pi-messages coming in from
	  * parents other than the recipient of this lambda-message.
	  *
	  * @return <tt>null</tt> if an appropriate message helper class cannot be located.
	  */
	public static LambdaMessageHelper load_lambda_message_helper( ConditionalDistribution px, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
		if ( lambda instanceof Noninformative )
			return new TrivialLambdaMessageHelper();

		Vector pi_names = new Vector();
		PiHelperLoader.make_classname_list( pi_names, pi_messages, false, null, 0 );

		Vector px_classes = PiHelperLoader.get_local_superclasses( px );
		Vector lambda_classes = PiHelperLoader.get_local_superclasses( lambda );

		// Outer loop is over class names of pi messages; next loop is over class
		// names of lambda; innermost loop is over class names of the conditional distribution.

		for ( Enumeration enum = pi_names.elements(); enum.hasMoreElements(); )
		{
			String s = (String) enum.nextElement();

			for ( Enumeration enum2 = lambda_classes.elements(); enum2.hasMoreElements(); )
			{
				String class_name = ((Class)enum2.nextElement()).getName();
				String lambda_name = class_name.substring( class_name.lastIndexOf('.')+1 );

				for ( Enumeration enum3 = px_classes.elements(); enum3.hasMoreElements(); )
				{
					class_name = ((Class)enum3.nextElement()).getName();
					String px_name = class_name.substring( class_name.lastIndexOf('.')+1 );

					String helper_name = "riso.distributions.computes_lambda_message."+px_name+"_"+lambda_name+"_"+s;

					try
					{
						Class helper_class = java.rmi.server.RMIClassLoader.loadClass( helper_name );
						LambdaMessageHelper lmh = (LambdaMessageHelper) helper_class.newInstance();
						return lmh;
					}
					catch (ClassNotFoundException e)
					{
System.err.println( "LambdaMessageHelperLoader.load_lambda_message_helper: helper not found:" );
System.err.println( "\t"+helper_name );
					}
				}
			}
		}
		
		// If we fall out here, we weren't able to locate an appropriate helper.
		return null;
	}
}
