package numerical;
import java.io.*;
import SmarterTokenizer;

public class FFT
{
	public static final int FORWARD = 1; 	// do forward (ordinary) transform
	public static final int INVERSE = 2;	// do inverse transform		

	/** Compute complex discrete fast Fourier transform of input
	  * array y[]. Transform is done in-place. Algorithm is taken from
	  * C. Balogh's class notes, Mth 553, Spr 1984.
	  * The fast transform is equivalent (performs the same mapping)
	  * as the mapping given below which defines the Fourier coefficients in
	  * terms of the signal data:
	  *
	  * <pre>
	  *     Y[n] = 1/N Sig(k=0,N-1) y[k] exp( -2 PI n k i /N ),  n=0, 1, ...N-1.
	  * </pre>
	  *
	  * where N is the number of data and i is the imaginary unit. A
	  * straightforward encoding of this summation leads to the "slow" transform.
	  *
	  * @param y on entry: data points; on exit: Fourier coefficients.
	  * @param N number of data points --assume it's a power of 2.
	  */
	public static void fft( Complex[] y, int N )
	{
		int	g, l, k, CTRLDNP, count;

		g = ilog2( N );	/* 2^g == N */
		count = N;		/* remember N before changing it */
		l = 1;
		k = 0;

		while ( l <= g )
		{
			do {
				CTRLDNP = 0;
				do { 
					++CTRLDNP;
					/* compute dual-node pair */
					dual( FORWARD, y, g, N, k, l, count );
					++k;
				}
				while ( CTRLDNP < N/2 );
				/* Skip */
				k += N/2;
			}
			while ( k < count -1 );	/* k == count -1 is end of column */

			/* get ready for next column */
			++l;
			N /= 2;
			k  = 0;
		}

		/* unscramble results */
		unscram( y, count, g );

		/* ...and scale by factor of 1/N */
		for ( k =0; k < count; ++k ) {
			y[k].real /= count;
			y[k].imag /= count;
		}

		/* y[] now contains Fourier coefficients */
	}

	/** Compute inverse complex discrete fast Fourier transform of input
	  * array y[]. Transform is done in-place. Algorithm is taken from
	  * C. Balogh's class notes, Mth 553, Spr 1984.
	  *	The fast inverse transform is equivalent (performs the same mapping)
	  * as the mapping given below which defines the signal data in terms of the
	  * Fourier coefficients:
	  * 
	  * <pre>
	  *	    y[n] = Sig(k=0,N-1) Y[k] exp( 2 PI n k i /N ),  n=0, 1, ...N-1.
	  * </pre>
	  *
	  * where N is the number of data and i is the imaginary unit. A
	  * straightforward encoding of this summation leads to the "slow" transform.
	  *
	  * @param Y on entry: Fourier coefficients; on exit: data points.
	  * @param N number of data points --assume it's a power of 2.
	  */
	public static void invfft( Complex[] Y, int N )
	{
		int	g, l, k, CTRLDNP, count;

		g = ilog2( N );	/* 2^g == N */
		count = N;		/* remember N before changing it */
		l = 1;
		k = 0;

		while ( l <= g ) {
			do {
				CTRLDNP = 0;
				do { 
					++CTRLDNP;
					/* compute dual-node pair */
					dual( INVERSE, Y, g, N, k, l, count );
					++k;
				}
				while ( CTRLDNP < N/2 );
				/* Skip */
				k += N/2;
			}
			while ( k < count -1 );	/* k == count -1 is end of column */

			/* get ready for next column */
			++l;
			N /= 2;
			k  = 0;
		}

		/* unscramble results */
		unscram( Y, count, g );

		/* Y[] now contains signal data */
	}

	/** Compute the dual node function.
	  * @param flag Is this forward or inverse transform?
	  * @param x Array in process of being transformed.
	  * @param g Bits used for indexes, ie 2^g = count of data.
	  * @param N ???
	  * @param k ???
	  * @param l ???
	  * @param count Number of data.
	  */
	public static void dual( int flag, Complex[] x, int g, int N, int k, int l, int count )
	{
		Complex Wp = new Complex(), temp = new Complex();
		double twopin;
		int	m, p;

		twopin = 2 * Math.PI / (double) count;

		/* compute W^p, where W is exp(-i2PI/n) or exp(i2PI/n), n is count */
		m = k >> (g -l);
		p = bitrev( m, g );
		Wp.real = Math.cos( twopin * p );

		switch ( flag )
		{
		case FORWARD: Wp.imag = -Math.sin( twopin * p ); break;
		case INVERSE: Wp.imag =  Math.sin( twopin * p ); break;
		default: throw new RuntimeException( "dual: what is flag "+flag+" ?" );
		}

		/* now compute node pair */
		Complex.mul( Wp,   x[k+N/2], temp );
		Complex.sub( x[k], temp,     x[k+N/2] );
		Complex.add( x[k], temp,     x[k] );
	}

	public static void unscram( Complex[] x, int N, int g )
	{
		Complex temp = new Complex();
		int	k, BR;

		k = 0;
		do
		{
			BR = bitrev( k, g );
			if ( BR > k )
			{
				/* swap */
				temp.real = x[k].real;
				temp.imag = x[k].imag;
				x[k].real = x[BR].real;
				x[k].imag = x[BR].imag;
				x[BR].real= temp.real;
				x[BR].imag= temp.imag;
			}
			++k;
		}
		while ( k < N );
	}

	static int bitrev( int n, int len )
	{
		int	rev, j;

		rev = 0;
		for ( j =0; j < len; ++j ) {
			rev = (rev << 1) + (n % 2);
			n  /= 2;
		}
		return( rev );
	}

	static int ilog2( int k )
	{
		int	pow;

		pow = 0;
		while ( k > 1 ) {
			++pow;
			k >>= 1;
		}

		return pow;
	}

	/** Read some data and apply the FFT or inverse FFT, as specified.
	  */
	public static void main( String[] args )
	{
		try
		{
			int N = 0;
			boolean do_inverse = false, complex_input = false;

			for ( int i = 0; i < args.length; i++ )
			{
				if ( args[i].charAt(0) != '-' ) continue;
				switch (args[i].charAt(1))
				{
				case 'N':
					N = Format.atoi( args[++i] );
					break;
				case 'c':
					complex_input = true;
					break;
				case 'i':
					do_inverse = true;
					break;
				}
			}

			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );

			Complex[] x = new Complex[N];
			for ( int i = 0; i < N; i++ )
			{
				x[i] = new Complex();

				st.nextToken();
				x[i].real = Format.atof( st.sval );

				if ( complex_input ) 
				{
					st.nextToken();
					x[i].imag = Format.atof( st.sval );
				}
				// else imaginary part is zero.
			}

			if ( do_inverse )
				invfft( x, N );
			else
				fft( x, N );

			for ( int i = 0; i < N; i++ )
			{
				System.out.println( x[i].real+"  "+x[i].imag );
			}
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
