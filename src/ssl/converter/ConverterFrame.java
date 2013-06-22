package ssl.converter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

public class ConverterFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	/** Launches a new instance of this frame */
	public static void main(final String[] args) {
		new ConverterFrame();
	}

	private static final String TITLE = "SSL Key/Certificate Converter v1.0";
	private static final int WIDTH = 800, HEIGHT = 800;

	// Error messages
	private static final String ERROR_CERT_NOT_FOUND = "Bad certificate file.";
	private static final String ERROR_KEY_NOT_FOUND = "Bad key file.";
	private static final String ERROR_UNKNOWN = "Unknown error.";

	// Parser execution strings
	private static final String COMMAND_PARSE = "\"C:\\OpenSSL-Win32\\bin\\openssl.exe\" asn1parse -in \"";
	private static final String COMMAND_FILE = "cert.bin";
	private static final String COMMAND_SUFFIX = "\" -out \""+COMMAND_FILE+"\"";

	// Output string constants
	private static final String OUTPUT_CERT_PRE = "ROM BYTE SSL_CERT[] __attribute__ ((aligned(4))) = {";
	private static final String OUTPUT_KEY_PRE1 = "ROM BYTE ";
	private static final String OUTPUT_KEY_PRE2 = "[] __attribute__ ((aligned(4))) = {";
	private static final String OUTPUT_SUF = "\n};\n\n";

	private static final String OUTPUT_VAR1 = "SSL_P";
	private static final String OUTPUT_VAR2 = "SSL_Q";
	private static final String OUTPUT_VAR3 = "SSL_dP";
	private static final String OUTPUT_VAR4 = "SSL_dQ";
	private static final String OUTPUT_VAR5 = "SSL_qInv";

	// Output formatting
	private static final int OUTPUT_COLS_KEY = 8;
	private static final int OUTPUT_COLS_CERT = 16;


	// UI Components
	private final JLabel		keyLabel		= new JLabel("Key:");
	private final JTextField	key				= new JTextField();
	private final JButton		browseKey		= new JButton("Browse");
	private final JLabel		certLabel		= new JLabel("Certificate:");
	private final JTextField	cert			= new JTextField();
	private final JButton		browseCert		= new JButton("Browse");
	private final JButton		generateButton	= new JButton("Generate");
	private final JTextArea		keyOut			= new JTextArea();
	private final JTextArea		certOut			= new JTextArea();


	// File chooser constant (so that it remembers the directory)
	private final JFileChooser	fileChooser		= new JFileChooser();

	// Browse button listener class (for filtering the correct file type)
	private class FileListener implements ActionListener{

		private final JTextField field;

		public FileListener(final JTextField field){
			this.field = field;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			fileChooser.setFileFilter(new FileFilter(){

				@Override
				public boolean accept(final File file) {
					return file.isDirectory() || file.getPath().endsWith(field==key?".key":".crt");
				}

				@Override
				public String getDescription() {
					return field==key?"SSL Keys":"SSL Certificates";
				}
			});
			if(fileChooser.showOpenDialog(ConverterFrame.this)==JFileChooser.APPROVE_OPTION){
				field.setText(fileChooser.getSelectedFile().getAbsolutePath());
			}
		}
	}

	public ConverterFrame(){

		final JPanel content	= new JPanel();
		final JPanel keyPanel	= new JPanel();
		final JPanel certPanel	= new JPanel();

		content.setLayout(new GridBagLayout());
		keyPanel.setLayout(new BoxLayout(keyPanel, BoxLayout.X_AXIS));
		certPanel.setLayout(new BoxLayout(certPanel, BoxLayout.X_AXIS));

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;

		gbc.weightx = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		content.add(keyLabel, gbc);
		gbc.gridy = 1;
		content.add(keyPanel, gbc);
		gbc.gridy = 2;
		content.add(certLabel, gbc);
		gbc.gridy = 3;
		content.add(certPanel, gbc);
		gbc.gridy = 4;
		content.add(generateButton, gbc);
		gbc.gridy = 5;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1;
		content.add(new JScrollPane(keyOut), gbc);
		gbc.gridy = 6;
		content.add(new JScrollPane(certOut), gbc);

		keyPanel.add(key);
		keyPanel.add(browseKey);
		certPanel.add(cert);
		certPanel.add(browseCert);

		// Browse buttons launch file chooser
		browseKey.addActionListener(new FileListener(key));
		browseCert.addActionListener(new FileListener(cert));

		generateButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				generateKeyOutput();
				generateCertOutput();
			}
		});


		// Outputs are not editable, and select all when clicked (for easy copying)
		keyOut.setEditable(false);
		certOut.setEditable(false);
		certOut.addMouseListener(new MouseListener(){
			@Override
			public void mouseClicked(final MouseEvent arg0) {
				certOut.setSelectionStart(0);
				certOut.setSelectionEnd(certOut.getText().length());
			}
			@Override
			public void mouseEntered(final MouseEvent arg0) {}
			@Override
			public void mouseExited(final MouseEvent arg0) {}
			@Override
			public void mousePressed(final MouseEvent arg0) {}
			@Override
			public void mouseReleased(final MouseEvent arg0) {}
		});
		keyOut.addMouseListener(new MouseListener(){
			@Override
			public void mouseClicked(final MouseEvent arg0) {
				keyOut.setSelectionStart(0);
				keyOut.setSelectionEnd(keyOut.getText().length());
			}
			@Override
			public void mouseEntered(final MouseEvent arg0) {}
			@Override
			public void mouseExited(final MouseEvent arg0) {}
			@Override
			public void mousePressed(final MouseEvent arg0) {}
			@Override
			public void mouseReleased(final MouseEvent arg0) {}
		});


		// Window properties
		setTitle(TITLE);
		setSize(WIDTH,HEIGHT);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setContentPane(content);
		setVisible(true);
	}

	private void generateCertOutput(){
		try{
			certOut.setText("");

			// Run Asn1 parser and open output file
			final String command = COMMAND_PARSE+cert.getText()+COMMAND_SUFFIX;
			System.out.println(command);
			final ProcessBuilder pb = new ProcessBuilder(command);
			final Process proc = pb.start();
			proc.getInputStream().close(); // Must close input stream for the output file to be created properly
			proc.waitFor();
			final File file = new File(COMMAND_FILE);
			final FileInputStream fileIn = new FileInputStream(file);

			// Parse output file
			certOut.setText(OUTPUT_CERT_PRE);
			while(fileIn.available()>0){
				certOut.append("\n\t");
				for(int i=0;i<OUTPUT_COLS_CERT && fileIn.available()>0;i++){
					String hex = Integer.toString(fileIn.read(),16).toUpperCase();
					if(hex.length()==1){
						hex = "0"+hex;
					}
					certOut.append("0x"+hex+", ");
				}
			}
			certOut.append(OUTPUT_SUF);
			fileIn.close();
			proc.destroy();
			file.delete();
		}catch(final FileNotFoundException e){
			certOut.setText(ERROR_CERT_NOT_FOUND);
			e.printStackTrace();
		}catch(final Exception e){
			certOut.setText(ERROR_UNKNOWN);
			e.printStackTrace();
		}
	}

	private void generateKeyOutput() {
		try{
			keyOut.setText("");

			// Run Asn1 parser and scan response
			final ProcessBuilder pb = new ProcessBuilder(COMMAND_PARSE+key.getText()+"\"");
			final Process proc = pb.start();
			final Scanner in = new Scanner(proc.getInputStream());

			try{
				// Skip the first 5 lines
				for(int i=0;i<5 && in.hasNext();i++){
					in.nextLine();
				}

				// Generate 5 byte arrays
				makeByteArray(in.nextLine(), OUTPUT_VAR1);
				makeByteArray(in.nextLine(), OUTPUT_VAR2);
				makeByteArray(in.nextLine(), OUTPUT_VAR3);
				makeByteArray(in.nextLine(), OUTPUT_VAR4);
				makeByteArray(in.nextLine(), OUTPUT_VAR5);
			}catch(final NoSuchElementException e){
				keyOut.setText(ERROR_KEY_NOT_FOUND);
				e.printStackTrace();
			}finally{
				proc.destroy();
			}
		}catch(final Exception e){
			keyOut.setText(ERROR_UNKNOWN);
			e.printStackTrace();
		}
	}


	private void makeByteArray(final String line, final String var){

		final String hex = line.substring(line.lastIndexOf(':')+1);

		keyOut.append(OUTPUT_KEY_PRE1+var+OUTPUT_KEY_PRE2);
		for(int i=hex.length();i>1;){
			keyOut.append("\n\t");
			for(int j=0;j<OUTPUT_COLS_KEY&&i>1;j++,i-=2){
				keyOut.append("0x"+hex.substring(i-2, i)+", ");
			}

		}
		keyOut.append(OUTPUT_SUF);
	}

}
