package numerical;
import java.io.*;
import SmarterTokenizer;

public class Convolve
{
	/** Read two columns of real data, convolve them, and print the result.
	  */
	public static void main( String[] args )
	{
		try
		{
			int N = 0;

			for ( int i = 0; i < args.length; i++ )
			{
				if ( args[i].charAt(0) != '-' ) continue;
				switch (args[i].charAt(1))
				{
				case 'N':
					N = Format.atoi( args[++i] );
					break;
				}
			}

			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );

			int Npadded;
			if ( (1 << FFT.ilog2(N)) == N )
				// N is a power of two; number of padding zeros will be equal to N.
				Npadded = 2*N;
			else
				// N is not a power of two; double N, then round up to next power of 2.
				Npadded = 1 << (FFT.ilog2(2*N)+1);

			Complex[] x = new Complex[ Npadded ];

			for ( int i = 0; i < N; i++ )
			{
				x[i] = new Complex();

				st.nextToken();
				x[i].real = Format.atof( st.sval );
				st.nextToken();
				x[i].imag = Format.atof( st.sval );
			}

			for ( int i = N; i < Npadded; i++ ) x[i] = new Complex();

			FFT.invfft( x, Npadded );		// compute FFT on both columns at once.

System.err.println( "FFT(x):" );
for ( int i = 0; i < Npadded; i++ ) System.err.println( x[i].real+"  "+x[i].imag );

			// Compute product of FFT's of each column of data.
			// First reconstruct the FFT coefficients from FFT of both columns,
			// then multiply.

			Complex[] xx = new Complex[ Npadded ];

System.err.println( "\n"+"untangle two FFT's:" );
			for ( int i = 0; i < Npadded; i++ )
			{
				// The FFT coefficient of the first column is (R1,I1), and 
				// that of the second is (R2,-I2).

				double R1 = (x[i].real + x[Npadded-1-i].real)/2;
				double I1 = (x[i].imag - x[Npadded-1-i].imag)/2;
				double R2 = (x[i].imag + x[Npadded-1-i].imag)/2;
				double I2 = (x[i].real - x[Npadded-1-i].real)/2;
System.err.println( "("+R1+","+I1+"), ("+R2+","+(-I2)+")" );

				// Multiply the coefficients to obtain the convolution.
				xx[i] = new Complex();
				xx[i].real = R1*R2 + I1*I2;
				xx[i].imag = -R1*I2 + I1*R2;
			}

System.err.println( "\n"+"product of two FFT's:" );
for ( int i = 0; i < Npadded; i++ ) System.err.println( xx[i].real+"  "+xx[i].imag );

			FFT.fft( xx, Npadded );

System.err.println( "\n"+"convolution:" );
			for ( int i = 0; i < 2*N-1; i++ )
			{
				System.out.println( xx[i].real+"  "+xx[i].imag );
			}
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
