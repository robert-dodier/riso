package numerical;
import java.io.*;

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
}
