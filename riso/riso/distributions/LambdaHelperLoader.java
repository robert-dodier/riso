package risotto.distributions;

public class LambdaHelperLoader
{
	public static LambdaHelper load_lambda_helper( Distribution[] lambda_messages ) throws Exception
	{
		int iperiod;
		String class_name;

		String lambda_names_with_counts = PiHelperLoader.make_classname_list( lambda_messages, true );
		String helper_name_with_counts = "risotto.distributions.ComputesLambda_"+lambda_names_with_counts;

		try
		{
			Class helper_class = Class.forName( helper_name_with_counts );
			return (LambdaHelper) helper_class.newInstance();
		}
		catch (Exception e1)
		{
System.err.println( "LambdaHelperLoader.load_lambda_helper: helper not found:" );
System.err.println( "  "+helper_name_with_counts );
			String lambda_names_without_counts = PiHelperLoader.make_classname_list( lambda_messages, false );
			String helper_name_without_counts = "risotto.distributions.ComputesLambda_"+lambda_names_without_counts;

			try
			{
				Class helper_class = Class.forName( helper_name_without_counts );
				return (LambdaHelper) helper_class.newInstance();
			}
			catch (Exception e2)
			{
System.err.println( "LambdaHelperLoader.load_lambda_helper: helper not found:" );
System.err.println( "  "+helper_name_without_counts );
				return null;
			}
		}
	}
}
