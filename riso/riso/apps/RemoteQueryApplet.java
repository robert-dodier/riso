package riso.apps;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import SmarterTokenizer;

public class RemoteQueryApplet extends Applet
{
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
