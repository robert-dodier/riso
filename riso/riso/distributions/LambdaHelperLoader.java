package risotto.distributions;
import java.util.*;

public class LambdaHelperLoader
{
	public static LambdaHelper load_lambda_helper( Distribution[] lambda_messages ) throws Exception
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
			return new TrivialLambdaHelper();

		Vector lambda_names = new Vector();
		PiHelperLoader.make_classname_list( lambda_names, remaining_lambda_messages, false, null, 0 );

		for ( Enumeration enum = lambda_names.elements(); enum.hasMoreElements(); )
		{
			String s = (String) enum.nextElement();
			String helper_name = "risotto.distributions.ComputesLambda_"+s;

			try
			{
				Class helper_class = Class.forName( helper_name );
				return (LambdaHelper) helper_class.newInstance();
			}
			catch (Exception e)
			{
System.err.println( "LambdaHelperLoader.load_lambda_helper: helper not found:" );
System.err.println( "  "+helper_name );
			}
		}
		
		// If we fall out here, we weren't able to locate an appropriate helper.
		return null;
	}
}
