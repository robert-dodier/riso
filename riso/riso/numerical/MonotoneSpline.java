package numerical;
import java.io.*;
import SmarterTokenizer;

public class MonotoneSpline implements Callback_1d
{
	int icache = 0;
	double[] x, f, d, alpha2, alpha3;

	public MonotoneSpline( double[] x, double[] f )
	{
		this.x = (double[]) x.clone();
		this.f = (double[]) f.clone();

		d = new double[ x.length ];
		alpha2 = new double[ x.length ];
		alpha3 = new double[ x.length ];

		double[] df = new double[ x.length ];
		double[] dx = new double[ x.length ];
		
		for ( int i = 0; i < x.length-1; i++ )
		{
			dx[i] = x[i+1] - x[i];
			df[i] = f[i+1] - f[i];
		}

		d[0] = d[x.length-1] = 0;
		for ( int i = 1; i < x.length-1; i++ )
		{
			double S1 = df[i-1]/dx[i-1];
			double S2 = df[i]/dx[i];
			
			double alpha = (1 + dx[i]/(dx[i-1]+dx[i]))/3;

			if ( S1*S2 > 0 )
				d[i] = S1*S2/(alpha*S2+(1-alpha)*S1);
			else
				d[i] = 0;
		}

		for ( int i = 0; i < x.length-1; i++ )
		{
			double dxi2 = dx[i]*dx[i], dxi3 = dx[i]*dx[i]*dx[i];
			alpha2[i] = 3*df[i]/dxi2 - (2*d[i]+d[i+1])/dx[i];
			alpha3[i] = -2*df[i]/dxi3 + (d[i]+d[i+1])/dxi2;
		}
System.err.println( "MonotoneSpline:\n\t"+"x\t"+"f\t"+"d\t"+"a2\t"+"a3" );
for ( int i = 0; i < x.length; i++ )
System.err.println( "\t"+x[i]+"\t"+f[i]+"\t"+d[i]+"\t"+alpha2[i]+"\t"+alpha3[i] );
	}

	public double compute_spline( double x, int i )
	{
		double xx = x - this.x[i];
		return f[i] + xx*(d[i] + xx*(alpha2[i] + xx*alpha3[i]));
	}

	public double f( double x ) throws Exception
	{
		// See if x is within the most-recently accessed interval.
		// If so, compute spline function. If not, search for interval,
		// then compute spline function.

		if ( this.x[icache] <= x && x <= this.x[icache+1] )
			return compute_spline( x, icache );
		else
		{
			// Do binary search on intervals.

			int ilow = 0, ihigh = this.x.length-1;
			while ( ihigh - ilow > 1 )
			{
				int i = (ilow+ihigh)/2;
				if ( x < this.x[i] )
					ihigh = i;
				else if ( x > this.x[i] )
					ilow = i;
				else // x == this.x[i]
				{
					ilow = i;
					break;
				}
			}

			icache = ilow;
System.err.println( "MonotoneSpline.f: "+this.x[icache]+" = x["+icache+"] < x = "+x+" < x["+(icache+1)+"] = "+this.x[icache+1] );
			return compute_spline( x, icache );
		}
	}

	static double[] example_x = { 0, 2, 3, 5, 6, 8, 9, 11, 12, 14 };
	static double[] example_f = { 0.0001, 0.0010, 0.0100, 0.1000, 1, 1.1, 2, 1.9, 0.8, 0.9};

	public static void main( String[] args )
	{
		try
		{
			double[] x = new double[100], f = new double[100];
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( new FileInputStream( args[0] ) ) );
			int i = 0;

			for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				x[i] = Format.atof( st.sval );
				st.nextToken();
				f[i] = Format.atof( st.sval );
				++i;
			}
			
			double[] xx = new double[i], ff = new double[i];
			System.arraycopy( x, 0, xx, 0, i );
			System.arraycopy( f, 0, ff, 0, i );

			MonotoneSpline s = new MonotoneSpline( xx, ff );

			st = new SmarterTokenizer( new InputStreamReader( System.in ) );

			for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				double u = Format.atof( st.sval );
				System.err.println( "\t"+u+"\t"+s.f(u) );
			}
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
