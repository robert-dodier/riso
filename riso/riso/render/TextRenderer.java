package riso.render;
import java.io.*;
import riso.distributions.*;
import numerical.*;
import SmarterTokenizer;

public class TextRenderer implements RenderDistribution
{
	public int npoints = 50;
	public PrintStream ps;

	public void do_render( Distribution q ) throws Exception
	{
		if ( q.ndimensions() > 1 )
			throw new IllegalArgumentException( "TextRenderer.do_render: "+q.ndimensions()+" is too many dimensions." );

		double[] support = q.effective_support( 1e-6 ), x = new double[1];
		Format gfmt = new Format( "%.8g" );

		if ( q instanceof Discrete )
		{
			for ( int i = 0; i <= (int)support[1]; i++ )
			{	
				x[0] = i;
				ps.println( i+"  "+gfmt.form( q.p(x) ) );
			}
		}
		else
		{
			double dx = (support[1] - support[0])/npoints;
			for ( int i = 0; i < npoints; i++ )
			{
				x[0] = support[0] + (i+0.5)*dx;
				ps.println( gfmt.form( x[0] )+"  "+gfmt.form( q.p(x) ) );
			}
		}
	}

	public static void main( String[] args )
	{
		String description_filename = "";
		int npoints = 50;

		for ( int i = 0; i < args.length; i++ )
		{
			switch ( args[i].charAt(1) )
			{
			case 'f':
				description_filename = args[++i];
				break;
			case 'n':
				npoints = Format.atoi( args[++i] );
				break;
			}
		}

		try
		{
			FileInputStream fis = new FileInputStream( description_filename );
			SmarterTokenizer st = new SmarterTokenizer( new BufferedReader( new InputStreamReader( fis ) ) );
			
			st.nextToken();
			System.err.println( "TextRenderer: distribution class: "+st.sval );

			AbstractDistribution q = (AbstractDistribution) java.rmi.server.RMIClassLoader.loadClass( st.sval ).newInstance();

			q.pretty_input( st );

			TextRenderer tr = new TextRenderer();
			tr.npoints = npoints;
			tr.ps = System.out;

			tr.do_render( q );

			System.exit(1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
