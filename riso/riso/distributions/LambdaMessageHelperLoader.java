package risotto.distributions;

public class LambdaMessageHelperLoader
{
	/** Puts together a name of the form <tt>ComputesLambdaMessage_P_Q_R</tt>
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

		int iperiod;
		String class_name;

		class_name = px.getClass().getName();
		iperiod = class_name.lastIndexOf('.');
		String px_name = class_name.substring( iperiod+1 );

		class_name = lambda.getClass().getName();
		iperiod = class_name.lastIndexOf('.');
		String lambda_name = class_name.substring( iperiod+1 );

		String pi_names_with_counts = PiHelperLoader.make_classname_list( pi_messages, true );
		String helper_name_with_counts = "risotto.distributions.ComputesLambdaMessage_"+px_name+"_"+lambda_name+"_"+pi_names_with_counts;

		try
		{
			Class helper_class = Class.forName( helper_name_with_counts );
			LambdaMessageHelper lmh = (LambdaMessageHelper) helper_class.newInstance();
System.err.println( "LambdaMessageHelperLoader.load_lambda_message_helper: load helper:" );
System.err.println( "  "+helper_name_with_counts );
			return lmh;
		}
		catch (ClassNotFoundException e1)
		{
System.err.println( "LambdaMessageHelperLoader.load_lambda_message_helper: helper not found:" );
System.err.println( "  "+helper_name_with_counts );
			String pi_names_without_counts = PiHelperLoader.make_classname_list( pi_messages, false );
			String helper_name_without_counts = "risotto.distributions.ComputesLambdaMessage_"+px_name+"_"+lambda_name+"_"+pi_names_without_counts;

			try
			{
				Class helper_class = Class.forName( helper_name_without_counts );
				LambdaMessageHelper lmh = (LambdaMessageHelper) helper_class.newInstance();
System.err.println( "LambdaMessageHelperLoader.load_lambda_message_helper: load helper:" );
System.err.println( "  "+helper_name_without_counts );
				return lmh;
			}
			catch (ClassNotFoundException e2)
			{
System.err.println( "LambdaMessageHelperLoader.load_lambda_message_helper: helper not found:" );
System.err.println( "  "+helper_name_without_counts );
				return null;
			}
		}
	}
}
