package numerical;

public class Complex
{
	public double real, imag;

	public Complex() { real = imag = 0; }

	public static void mul( Complex x, Complex y, Complex z )
	{
		z.real = x.real * y.real - x.imag * y.imag;
		z.imag = x.real * y.imag + x.imag * y.real;
	}

	public static void sub( Complex x, Complex y, Complex z )
	{
		z.real = x.real - y.real;
		z.imag = x.imag - y.imag;
	}

	public static void add( Complex x, Complex y, Complex z )
	{
		z.real = x.real + y.real;
		z.imag = x.imag + y.imag;
	}
}
