package app;

import gui.entry.DirectoryEntry;
import gui.entry.Entry;
import gui.props.UIEntryProps;
import gui.props.variable.IntVariable;
import gui.props.variable.StringVariable;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import process.NonStandardProcess;
import process.ProcessManager;
import statics.GUIUtils;
import ui.log.LogDialog;
import ui.log.LogFileSiphon;

/**
 * @author Daniel J. Rivers
 *         2013
 *
 * Created: Aug 24, 2013, 6:14:01 PM
 */
public class FlatDirectory extends JFrame {

	private static final long serialVersionUID = 1L;

	private UIEntryProps props = new UIEntryProps();
	
	private Flatten flat;

	public FlatDirectory() {
		ImageIcon icon = new ImageIcon( getClass().getResource( "dir.png" ) );
		this.setTitle( "Flat Directory" );
		this.setIconImage( icon.getImage() );
		this.setSize( new Dimension( 420, 160 ) );
		this.setDefaultCloseOperation( EXIT_ON_CLOSE );
		this.setLayout( new BorderLayout() );
		
		props.addVariable( "sourceDir", new StringVariable( "D:/Source/" ) );
		props.addVariable( "logName", new StringVariable( "Flatten-Log" ) );
		props.addVariable( "levels", new IntVariable( 1 ) );
		props.addVariable( "process", new StringVariable( "flatten" ) );
		
		flat = new Flatten( props.getString( "process" ) );
		this.add( dirPanel(), BorderLayout.CENTER );
		JButton b = new JButton( "Run Flatten" );
		b.addActionListener( e -> flat.execute() );
		this.add( b, BorderLayout.SOUTH );
		this.setVisible( true );
	}

	private JPanel dirPanel() {
		JPanel p = new JPanel();
		p.setLayout( new BoxLayout( p, BoxLayout.Y_AXIS ) );
		p.add( new DirectoryEntry( "Source Dir:", props.getVariable( "sourceDir" ) ) );
		GUIUtils.spacer( p );
		p.add( new Entry( "Levels:", props.getVariable( "levels" ), new Dimension( GUIUtils.SHORT ) ) );
		GUIUtils.spacer( p );		
		p.add( new Entry( "Log Name:", props.getVariable( "logName" ), new Dimension( GUIUtils.SHORT ) ) );
		return p;
	}

	public static void main( String args[] ) {
		try {
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		} catch ( ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e ) {
			System.err.println( "Critical JVM Failure!" );
			e.printStackTrace();
		}
		new FlatDirectory();
	}
	
	private class Flatten extends NonStandardProcess {

		public Flatten( String name ) {
			super( name );
		}
		
		public void execute() {
			try {
				String runName = props.getString( "process" );
				LogFileSiphon log = new LogFileSiphon( runName, props.getString( "sourceDir" ) + props.getString( "logName" ) + ".log" ) {
					public void skimMessage( String name, String s ) {
						try {
							fstream.write( "[" + sdf.format( new Date( System.currentTimeMillis() ) ) + "]:  " + s );
							fstream.newLine();
							fstream.flush();
						} catch ( IOException e ) {
							e.printStackTrace();
						}
					}
				};
				new LogDialog( FlatDirectory.this, runName, false );
				File f = new File( props.getString( "sourceDir" ) );
				flatten( null, f, 0 );
				sendMessage( "Completed Flattening " + f.getAbsolutePath() + " with " + props.getString( "levels" ) + " levels" );
				log.notifyProcessEnded( name );
				ProcessManager.getInstance().removeAll( name );
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}
		
		private void flatten( File parent, File f, int i ) {
			if ( i != ( (Integer)( (IntVariable)props.getVariable( "levels" ) ).getValue() ) ) {
				for ( File d : f.listFiles() ) {
					if ( d.isDirectory() ) {
						this.sendMessage( "Flatten: " + d.getAbsolutePath() );
						flatten( f, d, i + 1 );
					}
				}
				if ( parent != null ) {
					for ( File d : f.listFiles() ) {
						if ( d.isFile() ) {
							File n = new File( parent.getAbsolutePath() + "/" + d.getName() );
							sendMessage( "Moving: " + d.getAbsolutePath() + " ---> " + n.getAbsolutePath() );
							d.renameTo( n );
						}
					}
				}
			} else {
				for ( File d : f.listFiles() ) {
					if ( d.isFile() ) {
						File n = new File( parent.getAbsolutePath() + "/" + d.getName() );
						sendMessage( "Moving: " + d.getAbsolutePath() + " ---> " + n.getAbsolutePath() );
						d.renameTo( n );
					}
				}
			}
		}
	}
}