package riso.distributions;
import java.io.*;
import java.rmi.*;
import numerical.*;
import SmarterTokenizer;

public class ClassloaderTest extends Gamma
{
	public ClassloaderTest() {}

	public int ndimensions() { return 1; }

	public double p( double[] x )
	{
		throw new RuntimeException( "ClassloaderTest.p: not implemented." );
	}

	public double log_prior() throws Exception
	{
		throw new Exception( "ClassloaderTest.log_prior: not implemented." );
	}

	public double[] random() throws Exception
	{
		throw new Exception( "ClassloaderTest.random: not implemented." );
	}

	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "ClassloaderTest.update: not implemented." );
	}

	public double expected_value()
	{
		throw new RuntimeException( "ClassloaderTest.expected_value: not implemented." );
	}

	public double sqrt_variance()
	{
		throw new RuntimeException( "ClassloaderTest.sqrt_variance: not implemented." );
	}

	public double[] effective_support( double epsilon ) throws Exception
	{
		throw new Exception( "ClassloaderTest.effective_support: not implemented." );
	}

	public String format_string( String leading_ws )
	{
		String result = "";
		result += this.getClass().getName()+"\n";
		return result;
	}

	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		return;
	}
}
