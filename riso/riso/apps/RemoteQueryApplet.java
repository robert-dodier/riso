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
package riso.apps;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.rmi.*;
import SmarterTokenizer;

class RemoteQueryHostFrame extends Frame
{
	TextField host_input = new TextField(80);
	List bn_list = new List();

	RemoteQueryHostFrame( String title )
	{
		super(title);

		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.weighty = 0;
		setLayout(gbl);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.insets = new Insets( 15, 15, 15, 15 );
		gbl.setConstraints( host_input, gbc );
		add(host_input);

		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1;
		gbl.setConstraints( bn_list, gbc );
		add(bn_list);
	}

	public boolean keyDown( Event e, int key )
	{
		if ( key == Event.ENTER )
		{
			try
			{
				String host_name = host_input.getText();
				String[] names = Naming.list( "rmi://"+host_name );
				for ( int i = 0; i < names.length; i++ )
					bn_list.add( names[i] );
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				System.err.println( "RemoteQueryHostFrame.keyDown: stagger forward." );
			}
		}

		return false;
	}
}

public class RemoteQueryApplet extends Applet
{
	Frame host_frame = new RemoteQueryHostFrame( "Select Host and Belief Network" );
	TextField input = new TextField(128);
	TextArea output = new TextArea( "", 10, 128 );
	PrintStream textarea_pstream = new PrintStream( new TextAreaOutputStream( output ) );

	public void init()
	{
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.weighty = 0;
		setLayout(gbl);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.insets = new Insets( 15, 15, 15, 15 );
		gbl.setConstraints( input, gbc );
		add(input);

		output.setEditable(false);

		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1;
		gbl.setConstraints( output, gbc );
		add(output);

		textarea_pstream.println( "RISO Remote Query Applet." );

		host_frame.show();
	}

	public boolean keyDown( Event e, int key )
	{
		if ( key == Event.ENTER )
		{
			SmarterTokenizer st = new SmarterTokenizer( new StringReader( input.getText() ) );
			textarea_pstream.println( "\n"+"INPUT: "+input.getText()+"\n"+"OUTPUT:" );
			try { riso.apps.RemoteQuery.parse_input( st, textarea_pstream ); }
			catch (Exception ex) { textarea_pstream.println( "Failed: "+ex ); }
			input.setText("");
		}

		return false;
	}
}

class TextAreaOutputStream extends OutputStream
{
	TextArea textarea;

	public TextAreaOutputStream( TextArea a ) { textarea = a; }

	public void write( int b ) throws IOException
	{
		if ( b == '\t' ) b = ' ';	// translate tab to space; probably should become multiple spaces.
		textarea.appendText( ""+(byte)b );
	}

	public void write(byte b[]) throws IOException
	{
		for ( int i = 0; i < b.length; i++ ) if ( b[i] == '\t' ) b[i] = ' ';
		textarea.appendText( ""+(new String(b)) );
	}

	public void write(byte b[], int off, int len) throws IOException
	{
		for ( int i = off; i < off+len; i++ ) if ( b[i] == '\t' ) b[i] = ' ';
		textarea.appendText( new String(b,off,len) );
	}
}
