/** <applet code="PlotDistribution" width=400 height=200>
  * <param name=beliefnet value="host/something">
  * <param name=variable value="somethingelse">
  * </applet>
  */
import java.applet.*;
import java.awt.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import riso.belief_nets.*;
import riso.distributions.*;
import riso.remote_data.*;

public class PlotDistribution extends Applet implements RemoteObserver
{
	AbstractVariable variable;
	int[] x, y;
	double win_x0, win_y0, win_width, win_height;
	int vpt_x0, vpt_y0, vpt_width, vpt_height;
	Dimension size;

	public PlotDistribution() throws RemoteException
	{
		UnicastRemoteObject.exportObject( this );
	}

	/** Translate x from window coordinate system to viewport coordinates.
	  * <tt>(win_x0,win_y0) |-> (vpt_x0,vpt_y0)</tt>, and
	  * <tt>(win_x0+win_width,win_y0+win_height) |-> (vpt_x0+vpt_width,vpt_y0+vpt_height)</tt>.
	  */
	public int translate_x( double x )
	{
		double xscale = vpt_width/win_width;
		return vpt_x0 + (int) ((x-win_x0)*xscale);
	}

	/** Translate y from window coordinate system to viewport coordinates.
	  * <tt>(win_x0,win_y0) |-> (vpt_x0,vpt_y0)</tt>, and
	  * <tt>(win_x0+win_width,win_y0+win_height) |-> (vpt_x0+vpt_width,vpt_y0+vpt_height)</tt>.
	  */
	public int translate_y( double y )
	{
		double yscale = vpt_height/win_height;
		int yy = vpt_y0 + (int) ((y-win_y0)*yscale);
		return size.height - yy;
	}

	public void init()
	{
		String bn_name = getParameter( "beliefnet" );
		String variable_name = getParameter( "variable" );
System.err.println( "bn_name: "+bn_name+", variable_name: "+variable_name );

		try
		{
			BeliefNetworkContext bnc = new BeliefNetworkContext();
			Remote bn = bnc.get_reference( bn_name );
			variable = ((AbstractBeliefNetwork)bn).name_lookup( variable_name );
			((RemoteObservable)bn).add_observer( this, variable );
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
			throw new RuntimeException( "PlotDistribution.init: "+e );
		}
	}

	/** This method is called by the variable being watched after 
	  * the variable has changed. When that happens, update the display of
	  *  the current posterior distribution of the variable.
	  */
	public void update( RemoteObservable o, Object of_interest, Object arg ) throws RemoteException
	{
		try
		{
			AbstractVariable xx = (AbstractVariable) of_interest;
			Distribution p = (Distribution) arg;
System.out.println( "update: local time: "+(new Date())+" "+xx.get_fullname() );
System.out.println( "\t"+" mean: "+p.expected_value()+", stddev: "+p.sqrt_variance() );

			double[] support = p.effective_support(1e-3);

			size = getSize();
			vpt_x0 = 25;
			vpt_y0 = 25;
			vpt_width = size.width-2*25;
			vpt_height = size.height-2*25;

			win_x0 = support[0];
			win_y0 = 0;
			win_width = support[1]-support[0];
			// Set win_height based on maximum y value, below.

			double y_max = -1;

			// Store the points to be plotted in window coordinate;
			// translate to viewport coords after fudging window parameters.
			double[] win_x, win_y;

			if ( variable.is_discrete() )
			{
System.err.println( "\t"+variable.get_fullname()+" is discrete." );
				y_max = 1; // set maximum for discrete probabilities
				int N = 1 + (int) support[1];
				win_x = new double[N];
				win_y = new double[N];
				double[] xi = new double[1];

				for ( int i = 0; i < N; i++ )
				{
					xi[0] = i;
					win_x[i] = xi[0];
					win_y[i] = p.p( xi );
				}
			}
			else
			{
System.err.println( "\t"+variable.get_fullname()+" is NOT discrete." );
				int N = 100;
				double x0 = support[0], x1 = support[1];
				double dx = (x1-x0)/N;

				win_x = new double[N];
				win_y = new double[N];
				double[] xi = new double[1];
				double yi;

				for ( int i = 0; i < N; i++ )
				{
					xi[0] = x0 + (i+0.5)*dx;
					yi = p.p( xi );
					if ( yi > y_max ) y_max = yi;
					win_x[i] = xi[0];
					win_y[i] = yi;
				}
			}

			win_height = y_max;

			// Now fudge the window parameters so that plotted stuff
			// doesn't touch the edges of the viewport.

			double win_xmargin = win_width*0.1, win_ymargin = win_height*0.1;
			win_x0 -= win_xmargin;
			win_y0 -= win_ymargin;
			win_width += 2*win_xmargin;
			win_height += 2*win_ymargin;

			// Now translate window coords to viewport coords.

			x = new int[ win_x.length ];
			y = new int[ win_y.length ];

			for ( int i = 0; i < win_x.length; i++ )
			{
				x[i] = translate_x( win_x[i] );
				y[i] = translate_y( win_y[i] );
			}

System.err.println( "update: win_x0, win_y0: "+win_x0+", "+win_y0 );
System.err.println( "\twin_width, win_height: "+win_width+", "+win_height );
System.err.println( "\tvpt_x0, vpt_y0: "+vpt_x0+", "+vpt_y0 );
System.err.println( "\tvpt_width, vpt_height: "+vpt_width+", "+vpt_height );
System.err.println( "\tsize: "+size );
for ( int i = 0; i < x.length; i++ )
System.err.println( "\tx["+i+"], y["+i+"]: "+x[i]+", "+y[i] );
			repaint();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException( "PlotDistribution.update: "+e );
		}
	}

    public void paint(Graphics g)
	{
        Dimension d = size();


		boolean is_discrete;
		try { is_discrete = variable.is_discrete(); }
		catch (RemoteException e) { throw new RuntimeException( "PlotDistribution.paint: "+e ); }

		if ( x == null ) return; // no data to plot yet
System.err.println( "paint: x.length: "+x.length+", is_discrete: "+is_discrete );

        g.setColor(Color.white);
		g.fillRect( vpt_x0, vpt_y0, vpt_width, vpt_height );

        g.setColor(Color.black);

		if ( is_discrete )
		{
			int y0 = translate_y(0);
			for ( int i = 0; i < x.length; i++ )
			{
				g.drawLine( x[i], y0, x[i], y[i] );
				g.fillOval( x[i]-2, y[i]-2, 5, 5 );
			}
		}
		else
		{
			g.drawPolyline( x, y, x.length );
		}

        g.setColor(Color.darkGray);
        g.drawRect( vpt_x0, vpt_y0, vpt_width, vpt_height );
		int y0 = translate_y(0);
		g.drawLine( translate_x(win_x0), y0, translate_x(win_x0+win_width), y0 );
    }

    public boolean mouseUp(java.awt.Event evt, int x, int y)
	{
        // RESET SCALE AND TRANSLATION ???
        repaint();
        return true;
    }

    public boolean mouseDrag( java.awt.Event evt, int x, int y )
    {
        // SET RUBBER BOX ???
        repaint();
        return true;
    }
}

