package riso.approximation;
import numerical.*;

public class SupportTest
{
	public static void main( String[] args )
	{
		double scale = 1, tolerance = 0.99;

		if ( args.length > 0 )
		{	
			tolerance = Format.atof( args[0] );
			if ( args.length > 1 )
				scale = Format.atof( args[1] );
		}

		System.err.println( "SupportTest: scale: "+scale+"  tolerance: "+tolerance );

		try
		{
			SomeFunction f = new SomeFunction();
			double[] support = Intervals.effective_support( f, scale, tolerance );
			System.err.println( "SupportTest: support: "+support[0]+", "+support[1] );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}

class SomeFunction implements Callback_1d
{
	public double f( double x )
	{
		double absx = Math.abs(x), absx2 = Math.abs( x-100 );
		return Math.exp( -absx ) + Math.exp( -absx2 );
	}
}

