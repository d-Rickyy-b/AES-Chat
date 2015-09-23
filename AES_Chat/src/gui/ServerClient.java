package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UTFDataFormatException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Random;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import encrypter_decrypter.*;
import gui.ServerClient;

public class ServerClient
  extends JFrame
{
  private static final long serialVersionUID = 1L;
  private Dimension screenSize;
  private JButton btnCreate;
  private JButton btnJoin;
  private JButton btnShareKey;
  private JTextField txtIP;
  private JTextField txtPort;
  private JTextField txtNick;
  private JTextField txtMsg;
  private JTextArea txtArea;
  private JScrollPane scrollArea;
  private JScrollPane scrollList;
  private JList<String> list;
  private DefaultListModel<String> listModel;
  private JToolBar toolBar;
  private JComboBox<Object> maxClients;
  private JPopupMenu popupMenuKick;
  private JPopupMenu popupMenuClear;
  private JMenuItem menuItemKick;
  private JMenuItem menuItemClearText;
  private int selectedID;
  private ServerSocket serverSocket = null;
  private Socket socket = null;
  private String[] onlineUsers;
  private boolean[] privateUserIsOn;
  private ObjectInputStream[] inputStream;
  private ObjectOutputStream[] outputStream;
  private ObjectInputStream inputStreamClient = null;
  private ObjectOutputStream outputStreamClient = null;
  private boolean isClient;
  private int myID = -1;
  private String myNick;
  private ServerClient serverClient;
  private final String requestPrivate = "requestPrivate_";
  private PrivateMsg[] privateMsg;
  private Random random;
  
  private PrivateKey privKey;
  private PublicKey pubKey;
  private PublicKey pubKeyPartner;
  private SecretKey aesKey;
  
  public ServerClient()
  {
    initUI();
    this.serverClient = this;
  }
  
  //Initialisierung des UI
  private void initUI()
  {
    this.random = new Random();
    try
    {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception localException) {}
    this.screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    
    setTitle("TCP/IP Chat v1.0");
    setDefaultCloseOperation(3);
    setSize(this.screenSize.width / 4, this.screenSize.height / 2);
    setResizable(true);
    setLocationRelativeTo(null);
    toFront();
    
    setLayout(new BorderLayout());
    
    initComponents();
    
    addComponentListener(new ComponentAdapter()
    {
      public void componentResized(ComponentEvent event)
      {
        ServerClient.this.repaint();
      }
    });
    addWindowListener(new WindowAdapter()
    {
      public void windowClosing(WindowEvent event)
      {
        if (ServerClient.this.myID == 0) {
          ServerClient.this.sendMessages("<Server is offline!>", 0);	//Bei schließen des Fensters, "Server is offline" senden
        }
        try
        {
          if (ServerClient.this.outputStreamClient != null) {			//Sockets/Streams schließen
            ServerClient.this.outputStreamClient.close();
          }
          if (ServerClient.this.inputStreamClient != null) {
            ServerClient.this.inputStreamClient.close();
          }
          for (int i = 1; i < ServerClient.this.onlineUsers.length; i++)
          {
            if (ServerClient.this.onlineUsers[i] != null) {
              ServerClient.this.outputStream[i].close();
            }
            if (ServerClient.this.inputStream[i] != null) {
              ServerClient.this.inputStream[i].close();
            }
          }
          if (ServerClient.this.socket != null) {
            ServerClient.this.socket.close();
          }
          if (ServerClient.this.serverSocket != null) {
            ServerClient.this.serverSocket.close();
          }
        }
        catch (Exception localException) {}
      }
    });
  }
  //Ende InitUI
  
  //Initialisierung versch. Komponenten
  private void initComponents()
  {
    this.toolBar = new JToolBar();
    this.toolBar.setOrientation(0);
    this.toolBar.setFloatable(false);
    
    
    this.txtIP = new JTextField("127.0.0.1");
    this.txtIP.setToolTipText("Enter remote/server's ip adress");
    this.toolBar.add(this.txtIP);
    this.toolBar.addSeparator();
    
    this.txtPort = new JTextField("9876");
    this.txtPort.setToolTipText("Port number, default:9876");
    this.toolBar.add(this.txtPort);
    this.toolBar.addSeparator();
    
    this.txtNick = new JTextField("nickname");
    this.txtNick.setToolTipText("Enter your nickname");
    this.toolBar.add(this.txtNick);
    this.toolBar.addSeparator();
    
    //Object[] comboBox = { "4", "8", "16", "32", "64", "128", "256" };
    Object[] comboBox = {"2"};
    this.maxClients = new JComboBox(comboBox);
    this.maxClients.setSelectedIndex(0);
    this.maxClients.setToolTipText("Set max clients in room");
    this.toolBar.add(this.maxClients);
    this.toolBar.addSeparator();
    
    
    this.btnJoin = new JButton("Join server");
    this.btnJoin.setToolTipText("Join to remote server");
    this.toolBar.add(this.btnJoin);
    this.toolBar.addSeparator();
    
    this.btnCreate = new JButton("Start server");
    this.btnCreate.setToolTipText("Start server");
    this.toolBar.add(this.btnCreate);
    this.toolBar.addSeparator();
    
    this.btnShareKey = new JButton("shareKey");
    this.btnShareKey.setToolTipText("ShareKey");
    this.toolBar.add(this.btnShareKey);
    
    
    getContentPane().add(this.toolBar, "North");
    
    
    this.txtMsg = new JTextField();
    this.txtMsg.setEditable(false);
    getContentPane().add(this.txtMsg, "South");
    
    
    
    this.txtArea = new JTextArea();
    this.txtArea.setLineWrap(true);
    this.txtArea.setEditable(false);
    //this.txtArea.append("visit: youtube.com/defektruke\n");
    
    
    this.scrollArea = new JScrollPane(this.txtArea, 20, 31);
    this.scrollArea.getVerticalScrollBar().setUnitIncrement(15);
    getContentPane().add(this.scrollArea, "Center");
    
    this.list = new JList();
    this.list.setSelectionMode(1);
    this.list.setLayoutOrientation(0);
    this.list.setVisibleRowCount(-1);
    
    this.listModel = new DefaultListModel();
    this.list = new JList(this.listModel);
    
    
    this.scrollList = new JScrollPane(this.list, 20, 30);
    this.scrollList.setPreferredSize(new Dimension(128, 0));
    getContentPane().add(this.scrollList, "East");
    
    
    this.popupMenuKick = new JPopupMenu();
    this.menuItemKick = new JMenuItem("Kick user");
    this.popupMenuKick.add(this.menuItemKick);
    
    this.popupMenuClear = new JPopupMenu();
    this.menuItemClearText = new JMenuItem("Clear text");
    this.popupMenuClear.add(this.menuItemClearText);
    
    
    addListeners();
  }
  
  //Setzt privaten user offline
  public void setPrivateUserIsOn(int id)
  {
    this.privateUserIsOn[id] = false; //setzt privatenUser offline
  }
  
  //Hinzufügen der versch. (Button) Listeners
  private void addListeners()
  {
	//#################################################################
	//Rechtsklick auf TextArea listener
  	this.txtArea.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent event)
      {
        if (event.getButton() == 1) {
          ServerClient.this.popupMenuClear.setVisible(false); //Overlay entfernen
      }
        if (event.getButton() == 3) {
          ServerClient.this.popupMenuClear.show(event.getComponent(), event.getX(), event.getY()); //Overlay hinzufügen an stelle x,y
        }
      }
    });
    //#################################################################
    //Rechtsklick auf Textarea, Area löschen.
    this.menuItemClearText.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        ServerClient.this.txtArea.setText("");
        //ServerClient.this.txtArea.append("visit: youtube.com/defektruke\n");
      }
    });
    //#################################################################
    //User liste -> Kick Listener (Rechtsklick)
    this.menuItemKick.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        ServerClient.this.sendMessages("<" + ServerClient.this.onlineUsers[ServerClient.this.selectedID] + ", has been kicked by admin!>", 0);
        try
        {
          ServerClient.this.outputStream[ServerClient.this.selectedID].close();
        }
        catch (Exception localException) {}
        try
        {
          ServerClient.this.inputStream[ServerClient.this.selectedID].close();
        }
        catch (Exception localException1) {}
        ServerClient.this.outputStream[ServerClient.this.selectedID] = null;
        ServerClient.this.inputStream[ServerClient.this.selectedID] = null;
      }
    });
    //#################################################################
    //Linksklick auf Liste Listener
    this.list.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent event)
      {
        if (event.getButton() == 3) {
          if (!ServerClient.this.list.isSelectionEmpty())
          {
            String selected = (String)ServerClient.this.list.getSelectedValue();
            int id = ServerClient.this.list.getSelectedIndex();
            if ((id != 0) && (ServerClient.this.myID == 0)) {
              for (int i = 1; i < ServerClient.this.onlineUsers.length; i++) {
                if (ServerClient.this.onlineUsers[i] != null) {
                  if (ServerClient.this.onlineUsers[i].equalsIgnoreCase(selected))
                  {
                    ServerClient.this.selectedID = i;
                    ServerClient.this.popupMenuKick.show(event.getComponent(), event.getX(), event.getY());
                    break;
                  }
                }
              }
            }
          }
        }
        if (event.getButton() == 1) {
          ServerClient.this.popupMenuKick.setVisible(false);
        }
        //Doppelklick
        if ((event.getClickCount() == 2) && (event.getButton() == 1))
        {
          String selected = (String)ServerClient.this.list.getSelectedValue();
          int id = -1;
          try
          {
            for (int i = 0; i < ServerClient.this.onlineUsers.length; i++) {
              if ((selected.equalsIgnoreCase(ServerClient.this.onlineUsers[i])) && (i != ServerClient.this.myID)) //Wenn User online & nicht ich selbst
              {
                id = i;
                break;
              }
            }
            if ((ServerClient.this.privateUserIsOn[id] == false) && (id != ServerClient.this.myID)) //0 zu false
            {
              Point point = new Point(ServerClient.this.getLocationOnScreen());
              ServerClient.this.privateMsg[id] = new PrivateMsg(id, ServerClient.this.onlineUsers[id], ServerClient.this.onlineUsers[ServerClient.this.myID], ServerClient.this.myID, ServerClient.this.screenSize, ServerClient.this.serverClient, point);
              ServerClient.this.privateMsg[id].setVisible(true); //TODO Private Message Fenster wird aufgerufen
              ServerClient.this.privateUserIsOn[id] = true; //geändert von 1 nach true
            }
          }
          catch (Exception localException) {}
        }
      }
    });
    //###############################################################
    //Einem Server Joinen
    this.btnJoin.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
    	createRSAKeys();
        ServerClient.this.btnCreate.setEnabled(false); 	//Buttons ausgrauen
        ServerClient.this.btnJoin.setEnabled(false);
        ServerClient.this.isClient = true;				//wir sind Client
        
        new Thread(new Runnable()
        {
          public void run()
          {
            try
            {
              int setMaxClients = Integer.parseInt((String)ServerClient.this.maxClients.getSelectedItem()); //TODO nicht verstanden
              ServerClient.this.onlineUsers = new String[setMaxClients];
              ServerClient.this.privateMsg = new PrivateMsg[ServerClient.this.onlineUsers.length];
              ServerClient.this.privateUserIsOn = new boolean[ServerClient.this.onlineUsers.length];
              for (int i = 0; i < ServerClient.this.privateUserIsOn.length; i++) {
                ServerClient.this.privateUserIsOn[i] = false; //0 zu false
              }
              ServerClient.this.socket = new Socket(ServerClient.this.txtIP.getText(), Integer.parseInt(ServerClient.this.txtPort.getText()));
              //
              ServerClient.Check c = new ServerClient.Check(true, "checkIfServerIsFull");	//prüfen ob server voll
              ServerClient.this.outputStreamClient = new ObjectOutputStream(ServerClient.this.socket.getOutputStream());
              ServerClient.this.outputStreamClient.flush();
              ServerClient.this.inputStreamClient = new ObjectInputStream(ServerClient.this.socket.getInputStream());
              c.setSignal(false);
              
              //Sende Meinen Namen an Server
              ServerClient.this.sendMessageClient(ServerClient.this.txtNick.getText().trim().replace("@", "")); //replace @ mit "" -> @ darf nur Host 
              																									//&& entfernt Leerzeichen vor erstem und nach letztem Buchstaben
              Object obj = ServerClient.this.inputStreamClient.readObject();
              try
              {
                ServerClient.this.myNick = ((String)obj);
              }
              catch (Exception e)
              {
                ServerClient.this.setOnlineUsers(obj);
                obj = ServerClient.this.inputStreamClient.readObject();
                try
                {
                  ServerClient.this.myNick = ((String)obj);
                }
                catch (Exception localException1) {}
              }
              new ServerClient.ReceiveMessagesClient("receiveMessagesClient");
              ServerClient.this.txtMsg.setEditable(true);
            }
            catch (UTFDataFormatException|ClassCastException e)
            {
              JOptionPane.showMessageDialog(null, "Error: Connecting error!");
              ServerClient.this.btnCreate.setEnabled(true);
              ServerClient.this.btnJoin.setEnabled(true);
              ServerClient.this.myID = -1;
              return;
            }
            catch (ConnectException e)
            {
              JOptionPane.showMessageDialog(null, "Error: Server is offline!");
              ServerClient.this.btnCreate.setEnabled(true);
              ServerClient.this.btnJoin.setEnabled(true);
              ServerClient.this.myID = -1;
              return;
            }
            catch (Exception e)
            {
            	
              ServerClient.this.btnCreate.setEnabled(true);
              ServerClient.this.btnJoin.setEnabled(true);
              ServerClient.this.myID = -1;
              
              return;
            }
          }
        })
        .start();
      }
    });
    //#################################################################
    //Server Starten Listener
    this.btnCreate.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
	    createAESKey();
	    createRSAKeys();
        ServerClient.this.btnCreate.setEnabled(false); //Buttons disablen
        ServerClient.this.btnJoin.setEnabled(false);
        
        new Thread(new Runnable() //neuen Thread Starten
        {
          public void run()
          {
            try
            {
              ServerClient.this.isClient = false; 																//Wir sind kein Client (mehr)!
              ServerClient.this.myID = 0; 																		//myID = 0 -> host?!
              InetAddress thisIp = InetAddress.getLocalHost(); 													//HostIP = localhost (ip)
              ServerClient.this.txtIP.setText(thisIp.getHostAddress().toString()); 								//IP Feld = eigene IP adresse
              System.out.println("Starting server on "+thisIp.getHostAddress().toString() + ":" + ServerClient.this.txtPort.getText());//ServerClient.this.txtArea.append("Starting server...\n"); //TODO Ausgabe "starting server"
              
              ServerClient.this.txtArea.setCaretPosition(ServerClient.this.txtArea.getText().length());
              
              int setMaxClients = Integer.parseInt((String)ServerClient.this.maxClients.getSelectedItem()); 	//maximale Clients setzen von Auswahlbox
              ServerClient.this.onlineUsers = new String[setMaxClients];										//Neues Array der länge der max. User erstellen.
              ServerClient.this.privateMsg = new PrivateMsg[ServerClient.this.onlineUsers.length];				//neues objekt der klasse privateMsg, übergebe max clients
              ServerClient.this.privateUserIsOn = new boolean[ServerClient.this.onlineUsers.length];
              for (int i = 0; i < ServerClient.this.privateUserIsOn.length; i++) {
                ServerClient.this.privateUserIsOn[i] = false; //geändert von 0 nach false						//alle user offline "setzen"
              }
              ServerClient.this.inputStream = new ObjectInputStream[ServerClient.this.onlineUsers.length];		//Neuer Inputstream
              ServerClient.this.outputStream = new ObjectOutputStream[ServerClient.this.onlineUsers.length];	//Neuer Outputstream
              
              ServerClient.this.serverSocket = new ServerSocket(Integer.parseInt(ServerClient.this.txtPort.getText()));	//Setting up serverSocket
              
              ServerClient.this.txtArea.append("Creating connection on port:" + ServerClient.this.txtPort.getText() + "\n");	//Textausgaben:
              ServerClient.this.txtArea.setCaretPosition(ServerClient.this.txtArea.getText().length());
              
              ServerClient.this.myNick = ("@" + ServerClient.this.txtNick.getText());							//Meinen Nicknamen setzen
              ServerClient.this.listModel.addElement(ServerClient.this.myNick);									//der Liste Meinen Namen hinzufügen
              ServerClient.this.onlineUsers[0] = ServerClient.this.myNick;										//Ich bin erster Eintrag in Array
              ServerClient.this.txtMsg.setEditable(true);
            }
            catch (BindException e)
            {
              JOptionPane.showMessageDialog(null, "Error: Port already in use!");								//Wenn Port schon benutzt
              ServerClient.this.btnCreate.setEnabled(true);
              ServerClient.this.btnJoin.setEnabled(true);
              return;
            }
            catch (Exception e)																					//catcht exception
            {
              e.printStackTrace();
            }
            boolean signal = true;boolean signal2 = true;														//Signal und signal2 sind true
            ServerClient.this.txtArea.append("Waiting for client...\n");
            ServerClient.this.txtArea.setCaretPosition(ServerClient.this.txtArea.getText().length());			//Curser / Beobachter Punkt ans Ende der Area machen, alles im Blick
            
            while (signal) {
            	
              try
              {
                ServerClient.this.socket = ServerClient.this.serverSocket.accept();								//Verbindungen erlauben
                for (int i = 1; i < ServerClient.this.onlineUsers.length; i++) {
                  if (ServerClient.this.onlineUsers[i] == null)													//wenn Stelle in Array frei
                  {
                    ServerClient.this.inputStream[i] = new ObjectInputStream(ServerClient.this.socket.getInputStream());
                    ServerClient.this.outputStream[i] = new ObjectOutputStream(ServerClient.this.socket.getOutputStream());
                    ServerClient.this.outputStream[i].flush();
                    //in/outStream[i]... bestimmte streams
                  
                    
                  String nickname = (String)ServerClient.this.inputStream[i].readObject();
                  for (int j = 0; j < ServerClient.this.onlineUsers.length; j++) {
                    if (nickname.equalsIgnoreCase(ServerClient.this.onlineUsers[j])) { //System.out.println(nickname); System.out.println(ServerClient.this.onlineUsers[j]);
                      nickname = nickname + "_" + ServerClient.this.random.nextInt(100000);
                    }
                  }  
                    
                    ServerClient.this.outputStream[i].writeObject(nickname);
                    ServerClient.this.outputStream[i].flush();
                    
                    ServerClient.this.onlineUsers[i] = nickname;
                    ServerClient.this.txtArea.append("<" + nickname + " connected!>\n");
                    
                    sendPublicKey(false);
                    //ServerClient.this.sendMessages("key_" + Base64.getEncoder().encodeToString(aesKey.getEncoded()), 0);
                    ServerClient.this.txtArea.setCaretPosition(ServerClient.this.txtArea.getText().length());
                    ServerClient.this.listModel.addElement(nickname);
                    
                    
                    
                    ServerClient.this.outputStream[i].writeObject(ServerClient.this.getOnlineUsers());
                    ServerClient.this.outputStream[i].flush();
                    
                    new ServerClient.ReceiveMessages(nickname, i);
                    
                    ServerClient.this.sendMessages("<" + nickname + " connected!>", 0);
                    if (!signal2) {
                      break;
                    }
                    Thread.sleep(100L);
                    new ServerClient.SendOnlineClients("SendOnlineClients");
                    signal2 = false;
                    
                    
                    break;
                  }
              }
          }
              catch (Exception e)
              {
                signal = false;
                System.out.println("signal == false");
              }
            }
          }
        })
        .start(); //Starten des Threads
      }
    });
    //################################################################
    this.btnShareKey.addActionListener(new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			//TODO Exchange RSA public Keys
			exchangeKeys();			
		}
	});
    //#################################################################
    //Enter button listener -> Nachricht senden
    this.txtMsg.addKeyListener(new KeyAdapter()
    {
      public void keyPressed(KeyEvent event)
      {
        if (event.getKeyCode() == 10 && txtMsg.getText().length() > 0)
        {
          ServerClient.this.txtArea.append(ServerClient.this.myNick + ": " + ServerClient.this.txtMsg.getText() + "\n");
          ServerClient.this.txtArea.setCaretPosition(ServerClient.this.txtArea.getText().length());
          if (ServerClient.this.isClient) {
            ServerClient.this.sendMessageClient(ServerClient.this.myNick + ": " + ServerClient.this.txtMsg.getText());
          } else {
            ServerClient.this.sendMessages(ServerClient.this.myNick + ": " + ServerClient.this.txtMsg.getText(), 0);
          }
          ServerClient.this.txtMsg.setText("");
        }
      }
    });
    //#################################################################
  } 
  //Ende addListeners()
  
  //Beginn Klasse "Check"
  private class Check
  implements Runnable
  {
    private boolean signal;
    
    public Check(boolean signal, String threadName)
    {
      this.signal = signal;
      new Thread(this, threadName).start();
    }
    
    public void setSignal(boolean signal)
    {
      this.signal = signal;
    }
    
    public void run()
    {
      while (this.signal)
      {
        for (int i = 0; i < 6; i++) {
          try
          {
            Thread.sleep(1000L);
          }
          catch (InterruptedException localInterruptedException) {}
        }
        if (this.signal)
        {
          System.out.println("<Try later, server is full!>");//ServerClient.this.txtArea.append("<Try later, server is full!>\n"); //Message dass Server Voll ist
          ServerClient.this.btnCreate.setEnabled(true);
          ServerClient.this.btnJoin.setEnabled(true);
          ServerClient.this.myID = -1; 		//Meine ID = -1 -> nicht in der Liste (da Liste von 0->x geht)
          try
          {
            ServerClient.this.outputStreamClient.close();
          }
          catch (Exception localException) {}
          try
          {
            ServerClient.this.inputStreamClient.close();
          }
          catch (Exception localException1) {}
          try
          {
            ServerClient.this.socket.close();
          }
          catch (Exception localException2) {}
          try
          {
            ServerClient.this.serverSocket.close();
          }
          catch (Exception localException3) {}
          //Alle Verbindungen schließen
        }
      }
    }
  } 
  //Ende der Klasse "Check"
  
  private class SendOnlineClients
  implements Runnable
  {
    private boolean signal;
    
    public SendOnlineClients(String threadName)
    {
      this.signal = true;
      new Thread(this, threadName).start();
    }
    
    public void run()
    {
      while (this.signal) {
        try
        {
          Thread.sleep(5000L);
          if (ServerClient.this.listModel.getSize() > 1)
          {
            ServerClient.this.listModel.removeAllElements();
            ServerClient.this.listModel.addElement(ServerClient.this.onlineUsers[0]);
            for (int i = 1; i < ServerClient.this.onlineUsers.length; i++) {				//von 1 bis onl.users.länge loopen -> 0 = Host
              if (ServerClient.this.onlineUsers[i] != null)									//Wenn onlineUser[i] existiert, dann
              {
                ServerClient.this.listModel.addElement(ServerClient.this.onlineUsers[i]);	//User der Liste hinzufügen
                Thread.sleep(10L);															//10 sek. schlafen
              }
            }
            for (int i = 1; i < ServerClient.this.onlineUsers.length; i++) {
              if (ServerClient.this.onlineUsers[i] != null)
              {
                ServerClient.this.outputStream[i].writeObject(ServerClient.this.getOnlineUsers());
                ServerClient.this.outputStream[i].flush();
              }
            }
          }
        }
        catch (SocketException localSocketException) {}catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }
  }
  //Ende Klasse SendOnlineClients
  
  private String[] getOnlineUsers()
  {
    String[] helper = new String[this.onlineUsers.length];
    for (int i = 0; i < this.onlineUsers.length; i++) {
      helper[i] = this.onlineUsers[i];
    }
    return helper;
  }
  
  public void serverSendPrivate(String msg, int toID)
  {
    try
    {
      this.outputStream[toID].writeObject(msg);
      this.outputStream[toID].flush();
    }
    catch (Exception e)
    {
      this.privateMsg[toID].setNewMessage("<User is offline!>");
    }
  }
  
  private class ReceiveMessages
  implements Runnable
  {
    private boolean signal;
    private int id;
    private String msg;
    
    public ReceiveMessages(String threadName, int id)
    {
	  System.out.println("receivemessages " + myNick);
      this.id = id;
      this.signal = true;
      new Thread(this, threadName).start();
    }
    
    public void run()
    {
      while (this.signal) {
        try
        {
          this.msg = ((String)ServerClient.this.inputStream[this.id].readObject());
          
          if (this.msg.startsWith("requestPrivate_"))
          {
            this.msg = this.msg.replaceAll("requestPrivate_", "");
            
            String to_ID = "";String from_ID = "";
            for (int i = 0; i < this.msg.length(); i++)
            {
              if (this.msg.charAt(i) == '_') {
                break;
              }
              to_ID = to_ID + this.msg.charAt(i);
            }
            int toID = Integer.parseInt(to_ID);
            this.msg = this.msg.replaceAll(to_ID + "_", "");
            for (int i = 0; i < this.msg.length(); i++)
            {
              if (this.msg.charAt(i) == '_') {
                break;
              }
              from_ID = from_ID + this.msg.charAt(i);
            }
            int fromID = Integer.parseInt(from_ID);
            this.msg = this.msg.replaceAll(from_ID + "_", "");
            if (toID == 0)
            {
              if (ServerClient.this.privateUserIsOn[fromID] == false) //0 zu false //Wenn User offline
              {
                Point point = new Point(ServerClient.this.getLocationOnScreen());
                ServerClient.this.privateMsg[fromID] = new PrivateMsg(fromID, ServerClient.this.onlineUsers[fromID], ServerClient.this.onlineUsers[ServerClient.this.myID], ServerClient.this.myID, ServerClient.this.screenSize, ServerClient.this.serverClient, point);
                ServerClient.this.privateMsg[fromID].setVisible(true);
                ServerClient.this.privateMsg[fromID].setNewMessage(this.msg);
                ServerClient.this.privateUserIsOn[fromID] = true; //1 zu true
              }
              else
              {
                ServerClient.this.privateMsg[fromID].setNewMessage(this.msg);
              }
            }
            else {
              ServerClient.this.sendPrivateMsgTo(toID, fromID, this.msg);
            }
          }
          else if (this.msg.startsWith("publicKey_"))
          {		
        	  
        	  String pkey = msg.substring(10);
        	  System.out.println("PublicKey 2: " + pkey);
        	  byte[] publicBytes = Base64.getDecoder().decode(pkey);
        	  X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        	  KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        	  pubKeyPartner = keyFactory.generatePublic(keySpec);
        	  
        	  if (!isClient) {
				sendAESKey();
			}
          }
          else if (this.msg.startsWith("key_"))
          {		
        	  System.out.println("Jetzt bei kaputt");
        	  //byte[] decodedKey = Base64.getDecoder().decode(msg.substring(4));
        	  //aesKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        	  //TODO irgendwas kaputt
        	  //byte[] decodedKey = Base64.getDecoder().decode(msg.substring(4));
        	  String encryptedAESKey = msg.substring(4);
        	  EasyCrypt ecPri = new EasyCrypt(privKey, "RSA");
        	  String decryptedAESKey = ecPri.decrypt(encryptedAESKey);
        	  byte[] decodedKey = Base64.getDecoder().decode(decryptedAESKey);
        	  aesKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
          }
          else
          {
        	//TODO Decrypt
            ServerClient.this.txtArea.append(this.msg + "\n");
            ServerClient.this.txtArea.setCaretPosition(ServerClient.this.txtArea.getText().length());
            ServerClient.this.sendMessages(this.msg, this.id);
          }
        }
        catch (Exception e)
        {
          ServerClient.this.txtArea.append("<" + ServerClient.this.onlineUsers[this.id] + " disconnected!>\n");
          ServerClient.this.txtArea.setCaretPosition(ServerClient.this.txtArea.getText().length());
          ServerClient.this.sendMessages("<" + ServerClient.this.onlineUsers[this.id] + " disconnected!>", 0);
          try
          {
            ServerClient.this.outputStream[this.id].close();
          }
          catch (Exception localException1) {}
          try
          {
            ServerClient.this.inputStream[this.id].close();
          }
          catch (Exception localException2) {}
          ServerClient.this.onlineUsers[this.id] = null;
          ServerClient.this.outputStream[this.id] = null;
          ServerClient.this.inputStream[this.id] = null;
          this.signal = false;
        }
      }
    }
  }
  
  //sende Message an jeden Nutzer | id = absenderID
  private void sendMessages(String msg, int id)
  {
	if (!msg.startsWith("key_") && !msg.startsWith("publicKey_")) {	//TODO startswith publicKey
		System.out.println("Message Encrypted");
		msg = encryptMessage(msg);
	} else {
		System.out.println("Message not Encrypted: " + msg);
		//sendPublicKey();
	}
	
    for (int i = 1; i < this.onlineUsers.length; i++) {
      try
      {
        if ((this.outputStream[i] != null) && (id != i))
        {
          this.outputStream[i].writeObject(msg);
          this.outputStream[i].flush();
        }
      }
      catch (Exception localException) {}
    }
  }
  
  //private Nachricht senden
  private void sendPrivateMsgTo(int toID, int fromID, String msg)
  {
    try
    {
      if (this.outputStream[toID] != null)
      {
        String packAll = "requestPrivate_" + toID + "_" + fromID + "_" + msg; //String zusammen packen
        this.outputStream[toID].writeObject(packAll); 	//Nachricht senden
        this.outputStream[toID].flush();				//Stream flushen
      }
    }
    catch (Exception e)
    {
      try //wenn Nachricht nicht gesendet werden kann, gib aus "user offline"
      {
        this.outputStream[fromID].writeObject("requestPrivate_" + fromID + "_" + toID + "_" + "<User is offline!>");
        this.outputStream[fromID].flush();
      }
      catch (Exception localException1) {}
    }
  }
  
  private class ReceiveMessagesClient
  implements Runnable
  {
    private boolean signal;
    private Object obj;
    
    public ReceiveMessagesClient(String threadName)
    {
      this.signal = true;
      new Thread(this, threadName).start();
    }
    
    public void run()
    {
      //break label525;
      try
      {
        do //Do while schleife -> bis eine exception auftritt
        {
          this.obj = ServerClient.this.inputStreamClient.readObject(); //Speichere Inputstream in "obj"
          
          String msg = ""; //Deklaration des Strings msg
          try
          {
            msg = (String)this.obj; //String msg wird mit dem obj gefüllt (zu String gecastet)
            //System.out.println("Nachricht1: " + msg);
          }
          catch (Exception localException1) {}
          if (msg.startsWith("requestPrivate_")) //Wenn Nachricht mit "requestPrivate_" anfängt dann ... (private Nachricht)
          {
            try
            {
              msg = msg.replaceAll("requestPrivate_", ""); // ... diesen Teil der Nachricht ersetzen/entfernen
              
              String to_ID = "";String from_ID = ""; //String mit Sender/Empfänger
              
              //Empfänger herausfinden
              for (int i = 0; i < msg.length(); i++)	//Nachricht beschneiden (Aufbau = requestPrivate_|to_ID|_|from_ID|_Message)
              {
                if (msg.charAt(i) == '_') {	//wenn Buchstabe an stelle i = _ dann schleife verlassen
                  break;
                }
                to_ID = to_ID + msg.charAt(i); //to_ID = die ersten zahlen nach requestPrivate_ (z.b. 1)
               
              }
              int toID = Integer.parseInt(to_ID); //Integer aus herausgelesenem String machen
              msg = msg.replaceAll(to_ID + "_", ""); //neue Nachricht = alte Nachricht ohne EmpfängerID_ (msg = from_ID_ + msg)
              
              //Sender herausfinden
              for (int i = 0; i < msg.length(); i++) //Nachricht durchlaufen
              {
                if (msg.charAt(i) == '_') { //wenn Buchstabe an stelle i = _ dann schleife verlassen
                  break;
                }
                from_ID = from_ID + msg.charAt(i);
              }
              int fromID = Integer.parseInt(from_ID);
              msg = msg.replaceAll(from_ID + "_", ""); //Message ist jetzt nur noch die Message ohne Empfänger/Sender
              if (ServerClient.this.privateUserIsOn[fromID] == false) //0 zu false //wenn Sender(from_ID) offline ist
              {
                Point point = new Point(ServerClient.this.getLocationOnScreen());
                ServerClient.this.privateMsg[fromID] = new PrivateMsg(fromID, ServerClient.this.onlineUsers[fromID], ServerClient.this.onlineUsers[ServerClient.this.myID], ServerClient.this.myID, ServerClient.this.screenSize, ServerClient.this.serverClient, point);
                ServerClient.this.privateMsg[fromID].setVisible(true); 		//Fenster sichtbar machen
                ServerClient.this.privateMsg[fromID].setNewMessage(msg);	//Fenster Nachricht = msg
                ServerClient.this.privateUserIsOn[fromID] = true; 			//1 zu true //User ist On!
              }
              else
              {
                ServerClient.this.privateMsg[fromID].setNewMessage(msg);
              }
            }
            catch (Exception localException2) {}
          }
          else // wenn Nachricht nicht Privat:
          {
            ServerClient.this.setOnlineUsers(this.obj);
            try
            {
              if (!msg.startsWith("requestPrivate_"))//erneute Prüfung ob nicht privat
              {
            	  String encryptedMessage="LeereNachricht";
            	if (msg.startsWith("key_")) {
            		System.out.println("MSG starts with key: " + msg.substring(4));
              	  	//TODO decrypt RSA
//            		EasyCrypt ecPri = new EasyCrypt(privKey, "RSA");
//            		String encryptedKey = msg.substring(4);
//            		String decryptedKey = ecPri.decrypt(encryptedKey);
//            		
//            		System.out.println("AES-Key: " + decryptedKey);
//            		byte[] decodedKey = Base64.getDecoder().decode(decryptedKey);
//            		aesKey  = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
              	  	//String encodedAESKey = Base64.getEncoder().encodeToString(decodedKey);
              	  	//aesKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
            		System.out.println("AES-Key: " + msg);
            		String encryptedAESKey = msg.substring(4);
		        	EasyCrypt ecPri = new EasyCrypt(privKey, "RSA");
		        	String decryptedAESKey = ecPri.decrypt(encryptedAESKey);
		        	System.out.println("decryptedKey: " + decryptedAESKey);
		        	byte[] decodedKey = Base64.getDecoder().decode(decryptedAESKey);
		        	aesKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
              	  	
              	  	//encryptedMessage = (String)this.obj;
				} else if (msg.startsWith("publicKey_")) {
					String pkey = msg.substring(10);
		        	  //System.out.println("PublicKey 1: " + pkey);
		        	  System.out.println("publicKey received");
					  byte[] publicBytes = Base64.getDecoder().decode(pkey);
		        	  X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
		        	  KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		        	  pubKeyPartner = keyFactory.generatePublic(keySpec);
		        	  
		        	  if (!isClient) {
		        		  System.out.println();
						sendAESKey();
		        	  } else {
		        		  
		        		  sendPublicKey(true);
		        	  }
				}else {
					System.out.println("Message = " + (String) this.obj);
					encryptedMessage = decryptMessage((String)this.obj);
					nachrichtAnzeigen(encryptedMessage);
					//encryptedMessage = (String) this.obj;
					
				}
            	
                
              }
            }
            catch (Exception localException3) {}
          }
        } while (this.signal); //Signal = false wenn eine exception auftritt
      }
      catch (EOFException e)
      {
        this.signal = false;
        ServerClient.this.closeClientReceiveThread();
      }
      catch (ClassNotFoundException|IOException e)
      {
        this.signal = false;
        ServerClient.this.closeClientReceiveThread();
      }
      catch (Exception e)
      {
        this.signal = false;
        ServerClient.this.closeClientReceiveThread();
        e.printStackTrace();
      }
      //label525:
    }
  } 
  //ENDE ReceiveMessagesClient Klasse
  
  //Alle Verbindungen werden getrennt, Buttons enabled und private user checken.
  private void closeClientReceiveThread()
  { 
    try
    {
      this.outputStreamClient.close();
    }
    catch (Exception localException) {}
    try
    {
      this.inputStreamClient.close();
    }
    catch (Exception localException1) {}
    try
    {
      this.serverSocket.close();
    }
    catch (Exception localException2) {}
    try
    {
      this.socket.close();
    }
    catch (Exception localException3) {}
    this.listModel.removeAllElements();
    this.txtMsg.setEditable(false);
    this.btnCreate.setEnabled(true);
    this.btnJoin.setEnabled(true);
    for (int i = 0; i < this.privateUserIsOn.length; i++) {
      if (this.privateUserIsOn[i] != false) { //0 zu false
        this.privateMsg[i].setNewMessage("<Server is offline!>");
      }
    }
  }
  
  //TODO Sendet Name an Server?! 
  public void sendMessageClient(String msg)
  {
    try
    {
	  System.out.println("Nachricht sendMessageClient gesendet");
      this.outputStreamClient.writeObject(msg); //Sende eine Nachricht per StreamClient
      this.outputStreamClient.flush();
    }
    catch (Exception localException) {}
  }
  
  //Erstellt online Liste
  private void setOnlineUsers(Object obj)
  {
    try
    {
      this.onlineUsers = ((String[])obj);
      this.listModel.removeAllElements(); //Alle elemente aus der Userlist entfernen
      for (int i = 0; i < this.onlineUsers.length; i++) {
        if (this.onlineUsers[i] != null)
        {
          this.listModel.addElement(this.onlineUsers[i]);
          try
          {
            Thread.sleep(10L); //10 sekunden schlafen -> Akt.intervall alle 10 sekunden.
          }
          catch (InterruptedException localInterruptedException) {}
          if (this.myNick == this.onlineUsers[i]) { //wenn mein Nick = der name bei onlineUsers[i], dann myID = i
            this.myID = i;
          }
        }
      }
    }
    catch (ClassCastException localClassCastException) {}catch (ArrayIndexOutOfBoundsException localArrayIndexOutOfBoundsException) {}
  }
  
  private void nachrichtAnzeigen(String text){
	  ServerClient.this.txtArea.append(text + "\n"); //An txtArea das zum String gecastete obj. anhängen
      ServerClient.this.txtArea.setCaretPosition(ServerClient.this.txtArea.getText().length()); //Curserposition setzen
  }
  
  private void testmethode(){
	  KeyGenerator keygen;
		try {
			// zufaelligen Schluessel erzeugen
	        KeyPairGenerator keygenRSA = KeyPairGenerator.getInstance("RSA");
	        keygenRSA.initialize(1024);
	        KeyPair rsaKeys = keygenRSA.genKeyPair();	//generiert Schlüsselpaar

	        String encodedAESKey = Base64.getEncoder().encodeToString(aesKey.getEncoded());
		    
		    PrivateKey privK = rsaKeys.getPrivate();
	        PublicKey pubK = rsaKeys.getPublic();
	       
	        EasyCrypt ecPri = new EasyCrypt(privK, "RSA");
	        EasyCrypt ecPub = new EasyCrypt(pubK, "RSA"); //TODO PubKeyPartner
		       
		    // Text ver- und entschluesseln
		    String encryptedKey = ecPub.encrypt(encodedAESKey); //encrypted Key kann nun versendet werden 
		    
		    String erg2 = ecPri.decrypt(encryptedKey);
		    
		    System.out.println("encryptedKey: " + encryptedKey);
		    System.out.println("\nKlartext Key: " + erg2);      
		    System.out.println("\n");
		    
		    byte[] decodedKey = Base64.getDecoder().decode(erg2);
		    SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
		    
		    EasyCrypt ec2 = new EasyCrypt(originalKey, "AES");
		    //String klartext = ec.decrypt(geheim);
		    
		       
		           
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  }
  
  private void createAESKey(){
	  KeyGenerator keygen;
		try {
			keygen = KeyGenerator.getInstance("AES");
			keygen.init(128);
			aesKey = keygen.generateKey();
			System.out.println("AES-Key: " + Base64.getEncoder().encodeToString(aesKey.getEncoded()));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	  
  }
  
  private void createRSAKeys(){
	  try {
			KeyPairGenerator keygenRSA = KeyPairGenerator.getInstance("RSA");
	        keygenRSA.initialize(1024);
	        KeyPair rsaKeys = keygenRSA.genKeyPair();	//generiert Schlüsselpaar

	        privKey = rsaKeys.getPrivate();
	        pubKey = rsaKeys.getPublic();
		} catch (Exception e) {
			// TODO: handle exception
		}
  }
  
  //Tauscht RSA Schlüssel aus
  private void exchangeKeys(){
	  if (aesKey != null) {
		  //sendMessages("key_"+ Base64.getEncoder().encodeToString(aesKey.getEncoded()), 0);
		  sendAESKey();
	  } else {
		System.out.println("Kein Key zum teilen");
	}
	  
  }
  
  //Sendet eine AES verschlüsselte Nachricht
  private void sendMessage(String text){
	  
	  String encodedAESKey = Base64.getEncoder().encodeToString(aesKey.getEncoded());
	  try {
		  EasyCrypt ec = new EasyCrypt(aesKey, "AES");
		  String geheim = ec.encrypt(text);	//Verschlüsselt Text mit AES Key
		  
	  } catch (Exception e) {
		// TODO: handle exception
	  }
	  
	  
  }
  
  
  private String encryptMessage(String text){
	  String encodedAESKey = Base64.getEncoder().encodeToString(aesKey.getEncoded());
	  String geheim = "";
	  try {
		  EasyCrypt ec = new EasyCrypt(aesKey, "AES");
		  geheim = ec.encrypt(text);	//Verschlüsselt Text mit AES Key
		  
	  } catch (Exception e) {
		  System.out.println(e.getMessage());
		// TODO: handle exception
	  }
	  return geheim;
  }
  
  private String decryptMessage(String text){
	  //String encodedAESKey = Base64.getEncoder().encodeToString(aesKey.getEncoded());
	  System.out.println("Decrypt");
	  String klartext = "";
	  try {
		  EasyCrypt ec = new EasyCrypt(aesKey, "AES");
		  klartext = ec.decrypt(text);	//Verschlüsselt Text mit AES Key
		  
	  } catch (Exception e) {
		  nachrichtAnzeigen("Nachricht konnte nicht entschlüsselt werden.");
		  System.out.println(e.getMessage());
		// TODO: handle exception
	  }
	  return klartext;
  }
  
  
  private void sendPublicKey(boolean client){
	  System.out.println("sendPublicKey");
	  String encodedRSAKey = Base64.getEncoder().encodeToString(pubKey.getEncoded());
	  System.out.println("endocedRSA: " + encodedRSAKey);
	  
	  if (client) {
		sendMessageClient("publicKey_" + encodedRSAKey);
	  } else {
		  sendMessages("publicKey_" + encodedRSAKey,0);
	  }
	  
	  //System.out.println("publicKey_" + pubKey);
	  //System.out.println("publicKey_" + encodedRSAKey);
	  
  }
  
  private void sendAESKey(){
	  System.out.println("sendAESKey");
	    // Text ver- und entschluesseln
	    try {
	    	EasyCrypt ecPub = new EasyCrypt(pubKeyPartner, "RSA"); //TODO PubKeyPartner
	  	  	String encodedAESKey = Base64.getEncoder().encodeToString(aesKey.getEncoded());
			String encryptedKey = ecPub.encrypt(encodedAESKey);
			sendMessages("key_" + encryptedKey, 0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //encrypted Key kann nun versendet werden 
  }
  
  private void receiveAESKey(String encryptedAESKey){
	  
	  //pubKeyPartner = "";
  }
  
  //Main Methode
  public static void main(String[] args)
  {
    SwingUtilities.invokeLater(new Runnable() //Once all current events on the queue are processed, the run() method of the object will be called
    {
      public void run()
      {
        new ServerClient().setVisible(true);
      }
    });
  }
}

