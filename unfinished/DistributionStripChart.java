/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999, Robert Dodier.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA, 02111-1307, USA,
 * or visit the GNU web site, www.gnu.org.
 */
/** <applet code="DistributionStripChart" width=400 height=200> </applet>
  */
import java.applet.*;
import java.awt.*;

public class VariableStripChart extends Applet
{
	AbstractVariable x;
	int[] x, y;
	Rect window, viewport;

	public VariableStripChart( String x_name )
	{
		x = (new BeliefNetworkContext(null)).get_reference( x_name );

		if ( ! x.is_discrete() )
			throw new IllegalArgumentException( "DistributionStripChart: p is "+p.getClass()+"; must be discrete." );

        Dimension s = size();

		this.p = p;
		double[] support = p.effective_support(1e-3);

		int N = 1 + (int) support[1];
		x = new int[N];
		y = new int[N];
		double[] xi = new double[1];
		double yi;

		for ( int i = 0; i <= N; i++ )
		{
			xi[0] = i;
			yi = p.p( xi );
			x[i] = translate_x( xi[0] );
			y[i] = translate_y( yi );
		}
	}

    public void paint(Graphics g)
	{
        Dimension d = size();

        g.setColor(Color.black);

		if ( p.is_discrete() )
		{
			for ( int i = 0; i < x.length; i++ )
			{
				g.drawLine( x[i], 0, x[i], y[i] );
				g.fillOval( x[i], y[i], 3, 3 );
			}
		}
		else
		{
			g.drawPolyline( x, y, x.length );
		}

        g.setColor(Color.lightGray);
        g.drawRect( viewport );
		g.drawLine( viewport.x0, 0, viewport.x1, 0 );
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

