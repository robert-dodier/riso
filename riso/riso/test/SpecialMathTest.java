package numerical;
import java.io.*;
import SmarterTokenizer;

public class SpecialMathTest
{
	public static void main( String[] args )
	{
		try
		{
			double x;
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );

			for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				x = Format.atof( st.sval );
				System.out.println( " "+x+" "+SpecialMath.error(x)+" "+SpecialMath.cError(x) );
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
