package riso.approximation;
import java.io.*;
import riso.distributions.*;
import numerical.*;
import SmarterTokenizer;

public class SplineApproximation
{
	public static SplineDensity do_approximation( Distribution target, double[][] supports ) throws Exception
	{
System.err.println( "SplineApproximation.do_approximation: need approx. to "+target.getClass() );

		// IF suppports.length > 1 SHOULD RETURN A MIXTURE, ONE SPLINE COMPONENT PER INTERVAL !!!
		if ( supports.length > 1 ) throw new Exception( "SplineDensity.do_approximation: "+supports.length+" is too many support intervals!!!" );

		double x0 = supports[0][0], x1 = supports[0][1];
		FunctionCache cached_target = new FunctionCache( (x1-x0)/1e3, -1, new DistributionCallback(target) );
		IntegralHelper1d cth = new IntegralHelper1d( cached_target, supports, false );
		double total = cth.do_integral();
		
		double[][] x_px = cached_target.dump();
		double[] x = new double[ x_px.length ], px = new double[ x_px.length ];
		for ( int i = 0; i < x_px.length; i++ )
		{
			x[i] = x_px[i][0];
			px[i] = x_px[i][1];
		}

		return new SplineDensity( x, px );
	}

	public static void main( String[] args )
	{
		System.err.println( "target file: "+args[0] );
		System.err.println( "target support: ["+args[1]+", "+args[2]+"]" );

		try
		{
			int i;
			FileInputStream fis = new FileInputStream( args[0] );
			SmarterTokenizer p_st = new SmarterTokenizer( new BufferedReader( new InputStreamReader( fis ) ) );
			Distribution p = null;

			p_st.nextToken();
			Class p_class = java.rmi.server.RMIClassLoader.loadClass( p_st.sval );
			p = (Distribution) p_class.newInstance();
			p_st.nextBlock();
			p.parse_string( p_st.sval );

			double[][] support = new double[1][2];
			support[0][0] = Format.atof( args[1] );
			support[0][1] = Format.atof( args[2] );

			Distribution q = SplineApproximation.do_approximation( p, support );

			System.out.print( "approximation:\n"+q.format_string("") );
			double x[] = new double[1];
			System.out.println( "x\tp(x)\tq(x)" );
			for ( i = 0; i < 500; i++ )
			{
				x[0] = support[0][0]+i*(support[0][1]-support[0][0])/500.0;
				System.out.println( x[0]+"\t"+p.p(x)+"\t"+q.p(x) );
			}

			System.out.println( "q.expected_value: "+q.expected_value() );
			System.out.println( "q.sqrt_variance: "+q.sqrt_variance() );
		}
		catch (Exception e)
		{
			System.err.println( "SplineApproximation.main: something went ker-blooey." );
			e.printStackTrace();
		}
	}
}

class DistributionCallback implements Callback_1d
{
	Distribution target;
	double[] x1 = new double[1];

	DistributionCallback( Distribution target )
	{
		this.target = target;
	}

	public double f( double x ) throws Exception
	{
		x1[0] = x;
		return target.p(x1);
	}
}