package gui;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UTFDataFormatException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.Keymap;

public class mainChatWindow extends JFrame {

	private JPanel contentPane, panel_1;
	private JTextArea textArea, chatTextArea;
	private JTextField txtIP, txtPort, txtNick;
	private JButton btnCreate;
	private JButton btnJoin;
	private ServerSocket serverSocket = null;
	private Socket socket = null;
	private String[] onlineUsers;
	private boolean[] privateUserIsOn;
	private ObjectInputStream[] inputStream;
	private ObjectOutputStream[] outputStream;
	private ObjectInputStream inputStreamClient = null;
	private ObjectOutputStream outputStreamClient = null;
	private int myID = -1;
	private String myNick;	
	private boolean isServer, isClient, serverIsRunning;
	
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					mainChatWindow frame = new mainChatWindow("AES - Chat");
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public mainChatWindow(String name) {
		try
	    {
	      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    }catch (Exception localException) {}
		
		setTitle(name);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 400, 450);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		JButton btnTest = new JButton("Test");
		
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		
		txtIP = new JTextField("127.0.0.1");
	    txtIP.setToolTipText("Bitte IP Adresse eingeben");
	    toolBar.add(txtIP);
	    toolBar.addSeparator();
	    
	    txtPort = new JTextField("9876");
	    txtPort.setToolTipText("Port, standard:9876");
	    toolBar.add(txtPort);
	    toolBar.addSeparator();
	    
	    txtNick = new JTextField("nickname");
	    txtNick.setToolTipText("Enter your nickname");
	    toolBar.add(txtNick);
	    toolBar.addSeparator();
	    
	    btnJoin = new JButton("Join server");
	    btnJoin.setToolTipText("Join to remote server");
	    toolBar.add(btnJoin);
	    toolBar.addSeparator();
	    
	    btnCreate = new JButton("Start server");
	    btnCreate.setToolTipText("Start server");
	    toolBar.add(btnCreate);
		contentPane.add(toolBar, BorderLayout.NORTH);
		
		panel_1 = new JPanel();
		panel_1.setBorder(new EmptyBorder(5, 0, 0, 0));
		contentPane.add(panel_1, BorderLayout.SOUTH);
		panel_1.setLayout(new BorderLayout(0, 0));
		panel_1.setMaximumSize(new Dimension(374,70));
		
		textArea = new JTextArea();
		textArea.setBorder(new LineBorder(new Color(0, 0, 0)));
		textArea.setFont(new Font("Calibri", Font.PLAIN, 13));
		textArea.setLineWrap(true);
		textArea.addKeyListener(new KeyAdapter() {
			public void keyDown(KeyEvent evt){
				if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
					textArea.setText("");
				}
			}
			
			public void keyPressed(KeyEvent evt)
	        {
	            if(evt.getKeyCode() == KeyEvent.VK_ENTER && evt.getKeyCode() != KeyEvent.VK_SHIFT)
	            {	
	            	String text = textArea.getText();
	            	int length = text.length();
	            	sendMessage();
	            }
	        }
			
			public void keyReleased(KeyEvent evt){
				if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
					textArea.setText("");
				}
			}
			
			
		});
		panel_1.add(textArea, BorderLayout.CENTER);
		
		chatTextArea = new JTextArea();
		chatTextArea.setEditable(false);
		chatTextArea.setBorder(new LineBorder(new Color(0, 0, 0)));
		contentPane.add(chatTextArea, BorderLayout.CENTER);
		
		JButton btnNewButton = new JButton("Senden");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendMessage();
			}
		});
		panel_1.add(btnNewButton, BorderLayout.EAST);
		
	}
	

private void sendMessage(){
		if (!textArea.getText().equals("")) {
			System.out.println(panel_1.getWidth() + " | " + panel_1.getHeight());
			Calendar cal = Calendar.getInstance();
	        SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm:ss] - ");
	        String timeStamp = sdf.format(cal.getTime());
	        String textFieldContent = textArea.getText();
	        
	        while (textFieldContent.startsWith(" ") || textFieldContent.startsWith("\n")) {
				textFieldContent = textFieldContent.substring(1);
				System.out.println("um eins gekürzt");
			}
	        
	        if (textFieldContent.length() > 0) {
	        	String text = timeStamp + "Rico: " + textFieldContent.replace("\n", "")+"\n";
				textArea.setText("");
				chatTextArea.append(text);
			}
			
			
		}
	}

private void sendMessages(String text){
	
}

}
