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
//import javax.swing.JComboBox;
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

import encrypter_decrypter.EasyCrypt;

public class ServerClient extends JFrame {
	private static final long serialVersionUID = 1L;
	private Dimension screenSize;
	private JButton btnCreate, btnJoin, btnShareKey;
	private JTextField txtIP, txtPort, txtNick, txtMsg;
	private JTextArea txtArea;
	private JScrollPane scrollArea, scrollList;
	private JList<String> list;
	private DefaultListModel<String> listModel;
	private JToolBar toolBar;
	// private JComboBox<Object> maxClients;
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

	public ServerClient() {
		initUI();
		serverClient = this;
	}

	// Initialisierung des UI
	private void initUI() {
		random = new Random();
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception localException) {
		}
		screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		setTitle("TCP/IP Chat v1.0");
		setDefaultCloseOperation(3);
		setSize(screenSize.width / 4, screenSize.height / 2);
		setResizable(true);
		setLocationRelativeTo(null);
		toFront();
		setLayout(new BorderLayout());
		initComponents();

		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent event) {
				repaint();
			}
		});
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				if (myID == 0) {
					sendMessages("<Server is offline!>", 0);
				}
				try {
					if (outputStreamClient != null) {
						outputStreamClient.close();
					}
					if (inputStreamClient != null) {
						inputStreamClient.close();
					}
					for (int i = 1; i < onlineUsers.length; i++) {
						if (onlineUsers[i] != null) {
							outputStream[i].close();
						}
						if (inputStream[i] != null) {
							inputStream[i].close();
						}
					}
					if (socket != null) {
						socket.close();
					}
					if (serverSocket != null) {
						serverSocket.close();
					}
				} catch (Exception localException) {
				}
			}
		});
	}
	// Ende InitUI

	// Initialisierung versch. Komponenten
	private void initComponents() {
		toolBar = new JToolBar();
		toolBar.setOrientation(0);
		toolBar.setFloatable(false);

		txtIP = new JTextField("127.0.0.1");
		txtIP.setToolTipText("Enter remote/server's ip adress");
		toolBar.add(txtIP);
		toolBar.addSeparator();

		txtPort = new JTextField("9876");
		txtPort.setToolTipText("Port number, default:9876");
		toolBar.add(txtPort);
		toolBar.addSeparator();

		txtNick = new JTextField("nickname");
		txtNick.setToolTipText("Enter your nickname");
		toolBar.add(txtNick);
		toolBar.addSeparator();

		// Object[] comboBox = { "4", "8", "16", "32", "64", "128", "256" };
		// Object[] comboBox = { "2" };
		// maxClients = new JComboBox(comboBox);
		// maxClients.setSelectedIndex(0);
		// maxClients.setToolTipText("Set max clients in room");
		// toolBar.add(maxClients);
		// toolBar.addSeparator();

		btnJoin = new JButton("Join server");
		btnJoin.setToolTipText("Join to remote server");
		toolBar.add(btnJoin);
		toolBar.addSeparator();

		btnCreate = new JButton("Start server");
		btnCreate.setToolTipText("Start server");
		toolBar.add(btnCreate);
		toolBar.addSeparator();

		btnShareKey = new JButton("shareKey");
		btnShareKey.setToolTipText("ShareKey");
		toolBar.add(btnShareKey);

		getContentPane().add(toolBar, "North");

		txtMsg = new JTextField();
		txtMsg.setEditable(false);
		getContentPane().add(txtMsg, "South");

		txtArea = new JTextArea();
		txtArea.setLineWrap(true);
		txtArea.setEditable(false);

		scrollArea = new JScrollPane(txtArea, 20, 31);
		scrollArea.getVerticalScrollBar().setUnitIncrement(15);
		getContentPane().add(scrollArea, "Center");

		listModel = new DefaultListModel<>();
		list = new JList<>(listModel);
		list.setSelectionMode(1);
		list.setLayoutOrientation(0);
		list.setVisibleRowCount(-1);

		scrollList = new JScrollPane(list, 20, 30);
		scrollList.setPreferredSize(new Dimension(128, 0));
		getContentPane().add(scrollList, "East");

		popupMenuKick = new JPopupMenu();
		menuItemKick = new JMenuItem("Kick user");
		popupMenuKick.add(menuItemKick);

		popupMenuClear = new JPopupMenu();
		menuItemClearText = new JMenuItem("Clear text");
		popupMenuClear.add(menuItemClearText);

		addListeners();
	}

	// Setzt privaten user offline
	public void setPrivateUserIsOn(int id) {
		privateUserIsOn[id] = false; // setzt privatenUser offline
	}

	// Hinzufügen der versch. (Button) Listeners
	private void addListeners() {
		// #################################################################
		// Rechtsklick auf TextArea listener
		txtArea.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				if (event.getButton() == 1) {
					popupMenuClear.setVisible(false); // Overlay
														// entfernen
				}
				if (event.getButton() == 3) {
					popupMenuClear.show(event.getComponent(), event.getX(), event.getY());
				}
			}
		});
		// #################################################################
		// Rechtsklick auf Textarea, Area löschen.
		menuItemClearText.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				txtArea.setText("");
				// txtArea.append("visit:
				// youtube.com/defektruke\n");
			}
		});
		// #################################################################
		// User liste -> Kick Listener (Rechtsklick)
		menuItemKick.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				sendMessages("<" + onlineUsers[selectedID] + ", has been kicked by admin!>", 0);
				try {
					outputStream[selectedID].close();
				} catch (Exception localException) {
				}
				try {
					inputStream[selectedID].close();
				} catch (Exception localException1) {
				}
				outputStream[selectedID] = null;
				inputStream[selectedID] = null;
			}
		});
		// #################################################################
		// Linksklick auf Liste Listener
		list.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				if (event.getButton() == 3) {
					if (!list.isSelectionEmpty()) {
						String selected = (String) list.getSelectedValue();
						int id = list.getSelectedIndex();
						if ((id != 0) && (myID == 0)) {
							for (int i = 1; i < onlineUsers.length; i++) {
								if (onlineUsers[i] != null) {
									if (onlineUsers[i].equalsIgnoreCase(selected)) {
										selectedID = i;
										popupMenuKick.show(event.getComponent(), event.getX(), event.getY());
										break;
									}
								}
							}
						}
					}
				}
				if (event.getButton() == 1) {
					popupMenuKick.setVisible(false);
				}
				// Doppelklick
				if ((event.getClickCount() == 2) && (event.getButton() == 1)) {
					String selected = (String) list.getSelectedValue();
					int id = -1;
					try {
						for (int i = 0; i < onlineUsers.length; i++) {
							if ((selected.equalsIgnoreCase(onlineUsers[i])) && (i != myID)) {
								id = i;
								break;
							}
						}
						if ((privateUserIsOn[id] == false) && (id != myID)) {
							Point point = new Point(getLocationOnScreen());
							privateMsg[id] = new PrivateMsg(id, onlineUsers[id], onlineUsers[myID], myID, screenSize,
									serverClient, point);
							privateMsg[id].setVisible(true);
							privateUserIsOn[id] = true;
						}
					} catch (Exception localException) {
					}
				}
			}
		});
		// ###############################################################
		// Einem Server Joinen
		btnJoin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				createRSAKeys();
				btnCreate.setEnabled(false);
				btnJoin.setEnabled(false);
				isClient = true;

				new Thread(new Runnable() {
					public void run() {
						try {
							int setMaxClients = 2;// maxClients.getSelectedItem());
							onlineUsers = new String[setMaxClients];
							privateMsg = new PrivateMsg[onlineUsers.length];
							privateUserIsOn = new boolean[onlineUsers.length];
							for (int i = 0; i < privateUserIsOn.length; i++) {
								privateUserIsOn[i] = false;
							}
							socket = new Socket(txtIP.getText(), Integer.parseInt(txtPort.getText()));
							//
							ServerClient.Check c = new ServerClient.Check(true, "checkIfServerIsFull");
							outputStreamClient = new ObjectOutputStream(socket.getOutputStream());
							outputStreamClient.flush();
							inputStreamClient = new ObjectInputStream(socket.getInputStream());
							c.setSignal(false);

							sendMessageClient(txtNick.getText().trim().replace("@", ""));
							Object obj = inputStreamClient.readObject();
							try {
								myNick = ((String) obj);
							} catch (Exception e) {
								setOnlineUsers(obj);
								obj = inputStreamClient.readObject();
								try {
									myNick = ((String) obj);
								} catch (Exception localException1) {
								}
							}
							new ServerClient.ReceiveMessagesClient("receiveMessagesClient");
							txtMsg.setEditable(true);
						} catch (UTFDataFormatException | ClassCastException e) {
							JOptionPane.showMessageDialog(null, "Error: Verbindungs Fehler!");
							btnCreate.setEnabled(true);
							btnJoin.setEnabled(true);
							myID = -1;
							return;
						} catch (ConnectException e) {
							JOptionPane.showMessageDialog(null, "Error: Server ist offline!");
							btnCreate.setEnabled(true);
							btnJoin.setEnabled(true);
							myID = -1;
							return;
						} catch (Exception e) {

							btnCreate.setEnabled(true);
							btnJoin.setEnabled(true);
							myID = -1;

							return;
						}
					}
				}).start();
			}
		});
		// #################################################################
		// Server Starten Listener
		btnCreate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				createAESKey();
				createRSAKeys();

				btnCreate.setEnabled(false);
				btnJoin.setEnabled(false);

				txtIP.setEnabled(false);
				txtPort.setEnabled(false);
				txtNick.setEnabled(false);

				new Thread(new Runnable() // neuen Thread Starten
				{
					public void run() {
						try {
							isClient = false;
							myID = 0;
							InetAddress thisIp = InetAddress.getLocalHost();
							txtIP.setText(thisIp.getHostAddress().toString());
							System.out.println("Server läuft auf: " + thisIp.getHostAddress().toString() + ":"
									+ txtPort.getText());
							txtArea.setCaretPosition(txtArea.getText().length());

							int setMaxClients = 2;// maxClients.getSelectedItem());
							onlineUsers = new String[setMaxClients];
							privateMsg = new PrivateMsg[onlineUsers.length];
							privateUserIsOn = new boolean[onlineUsers.length];
							for (int i = 0; i < privateUserIsOn.length; i++) {
								privateUserIsOn[i] = false;
							}
							inputStream = new ObjectInputStream[onlineUsers.length];
							outputStream = new ObjectOutputStream[onlineUsers.length];
							serverSocket = new ServerSocket(Integer.parseInt(txtPort.getText()));

							txtArea.append("Creating connection on port:" + txtPort.getText() + "\n"); // Textausgaben:
							txtArea.setCaretPosition(txtArea.getText().length());

							myNick = ("@" + txtNick.getText());
							listModel.addElement(myNick);
							onlineUsers[0] = myNick;
							txtMsg.setEditable(true);
						} catch (BindException e) {
							JOptionPane.showMessageDialog(null, "Error: Port schon benutzt!");
							btnCreate.setEnabled(true);
							btnJoin.setEnabled(true);
							return;
						} catch (Exception e) {
							e.printStackTrace();
						}
						boolean signal = true;
						boolean signal2 = true; // Signal und signal2 sind true
						nachrichtAnzeigen("Auf Clients warten...\n");

						while (signal) {

							try {
								socket = serverSocket.accept(); // Verbindungen
																// erlauben
								for (int i = 1; i < onlineUsers.length; i++) {
									if (onlineUsers[i] == null) {
										inputStream[i] = new ObjectInputStream(socket.getInputStream());
										outputStream[i] = new ObjectOutputStream(socket.getOutputStream());
										outputStream[i].flush();

										String nickname = (String) inputStream[i].readObject();
										for (int j = 0; j < onlineUsers.length; j++) {
											if (nickname.equalsIgnoreCase(onlineUsers[j])) {
												nickname = nickname + "_" + random.nextInt(100000);
											}
										}

										outputStream[i].writeObject(nickname);
										outputStream[i].flush();

										onlineUsers[i] = nickname;
										txtArea.append("<" + nickname + " connected!>\n");

										sendPublicKey(false);

										txtArea.setCaretPosition(txtArea.getText().length());
										listModel.addElement(nickname);

										outputStream[i].writeObject(getOnlineUsers());
										outputStream[i].flush();

										new ServerClient.ReceiveMessages(nickname, i);

										sendMessages("<" + nickname + " connected!>", 0);
										if (!signal2) {
											break;
										}
										Thread.sleep(100L);
										new ServerClient.SendOnlineClients("SendOnlineClients");
										signal2 = false;

										break;
									}
								}
							} catch (Exception e) {
								signal = false;
							}
						}
					}
				}).start();
			}
		});
		// ################################################################
		btnShareKey.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
			}
		});
		// #################################################################
		// Enter button listener -> Nachricht senden
		txtMsg.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.getKeyCode() == 10 && txtMsg.getText().length() > 0) {
					txtArea.append(myNick + ": " + txtMsg.getText() + "\n");
					txtArea.setCaretPosition(txtArea.getText().length());
					if (isClient) {
						sendMessageClient(myNick + ": " + txtMsg.getText());
					} else {
						sendMessages(myNick + ": " + txtMsg.getText(), 0);
					}
					txtMsg.setText("");
				}
			}
		});
		// #################################################################
	}
	// Ende addListeners()

	// Beginn Klasse "Check"
	private class Check implements Runnable {
		private boolean signal;

		public Check(boolean signal, String threadName) {
			this.signal = signal;
			new Thread(this, threadName).start();
		}

		public void setSignal(boolean signal) {
			this.signal = signal;
		}

		public void run() {
			while (signal) {
				for (int i = 0; i < 6; i++) {
					try {
						Thread.sleep(1000L);
					} catch (InterruptedException localInterruptedException) {
					}
				}
				if (signal) {
					System.out.println("<Try later, server is full!>");
					btnCreate.setEnabled(true);
					btnJoin.setEnabled(true);
					myID = -1;
					try {
						outputStreamClient.close();
					} catch (Exception localException) {
					}
					try {
						inputStreamClient.close();
					} catch (Exception localException1) {
					}
					try {
						socket.close();
					} catch (Exception localException2) {
					}
					try {
						serverSocket.close();
					} catch (Exception localException3) {
					}
					// Alle Verbindungen schließen
				}
			}
		}
	}
	// Ende der Klasse "Check"

	private class SendOnlineClients implements Runnable {
		private boolean signal;

		public SendOnlineClients(String threadName) {
			signal = true;
			new Thread(this, threadName).start();
		}

		public void run() {
			while (signal) {
				try {
					Thread.sleep(5000L);
					if (listModel.getSize() > 1) {
						listModel.removeAllElements();
						listModel.addElement(onlineUsers[0]);
						for (int i = 1; i < onlineUsers.length; i++) {
							if (onlineUsers[i] != null) {
								listModel.addElement(onlineUsers[i]);
								Thread.sleep(10L); // 10 sek. schlafen
							}
						}
						for (int i = 1; i < onlineUsers.length; i++) {
							if (onlineUsers[i] != null) {
								outputStream[i].writeObject(getOnlineUsers());
								outputStream[i].flush();
							}
						}
					}
				} catch (SocketException localSocketException) {
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	// Ende Klasse SendOnlineClients

	private String[] getOnlineUsers() {
		String[] helper = new String[onlineUsers.length];
		for (int i = 0; i < onlineUsers.length; i++) {
			helper[i] = onlineUsers[i];
		}
		return helper;
	}

	public void serverSendPrivate(String msg, int toID) {
		try {
			outputStream[toID].writeObject(msg);
			outputStream[toID].flush();
		} catch (Exception e) {
			privateMsg[toID].setNewMessage("<User is offline!>");
		}
	}

	private class ReceiveMessages implements Runnable {
		private boolean signal;
		private int id;
		private String msg;

		public ReceiveMessages(String threadName, int id) {
			System.out.println("receivemessages " + myNick);
			this.id = id;
			signal = true;
			new Thread(this, threadName).start();
		}

		public void run() {
			while (signal) {
				try {
					msg = ((String) inputStream[id].readObject());

					if (msg.startsWith(requestPrivate)) {
						msg = msg.replaceAll(requestPrivate, "");

						String to_ID = "";
						String from_ID = "";
						for (int i = 0; i < msg.length(); i++) {
							if (msg.charAt(i) == '_') {
								break;
							}
							to_ID = to_ID + msg.charAt(i);
						}
						int toID = Integer.parseInt(to_ID);
						msg = msg.replaceAll(to_ID + "_", "");
						for (int i = 0; i < msg.length(); i++) {
							if (msg.charAt(i) == '_') {
								break;
							}
							from_ID = from_ID + msg.charAt(i);
						}
						int fromID = Integer.parseInt(from_ID);
						msg = msg.replaceAll(from_ID + "_", "");
						if (toID == 0) {
							if (privateUserIsOn[fromID] == false) // 0
																	// zu
																	// false
																	// //Wenn
																	// User
																	// offline
							{
								Point point = new Point(getLocationOnScreen());
								privateMsg[fromID] = new PrivateMsg(fromID, onlineUsers[fromID], onlineUsers[myID],
										myID, screenSize, serverClient, point);
								privateMsg[fromID].setVisible(true);
								privateMsg[fromID].setNewMessage(msg);
								privateUserIsOn[fromID] = true; // 1
																// zu
																// true
							} else {
								privateMsg[fromID].setNewMessage(msg);
							}
						} else {
							sendPrivateMsgTo(toID, fromID, msg);
						}
					} else if (msg.startsWith("publicKey_")) {
						String pkey = msg.substring(10);
						System.out.println("PublicKey 2: " + pkey);
						byte[] publicBytes = Base64.getDecoder().decode(pkey);
						X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
						KeyFactory keyFactory = KeyFactory.getInstance("RSA");
						pubKeyPartner = keyFactory.generatePublic(keySpec);

						if (!isClient) {
							sendAESKey();
						}
					} else if (msg.startsWith("key_")) {
						String encryptedAESKey = msg.substring(4);
						EasyCrypt ecPri = new EasyCrypt(privKey, "RSA");
						String decryptedAESKey = ecPri.decrypt(encryptedAESKey);
						byte[] decodedKey = Base64.getDecoder().decode(decryptedAESKey);
						aesKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
					} else {
						txtArea.append(msg + "\n");
						txtArea.setCaretPosition(txtArea.getText().length());
						sendMessages(msg, id);
					}
				} catch (Exception e) {
					txtArea.append("<" + onlineUsers[id] + " disconnected!>\n");
					txtArea.setCaretPosition(txtArea.getText().length());
					sendMessages("<" + onlineUsers[id] + " disconnected!>", 0);
					try {
						outputStream[id].close();
					} catch (Exception localException1) {
					}
					try {
						inputStream[id].close();
					} catch (Exception localException2) {
					}
					onlineUsers[id] = null;
					outputStream[id] = null;
					inputStream[id] = null;
					signal = false;
				}
			}
		}
	}

	// sende Message an jeden Nutzer | id = absenderID
	private void sendMessages(String msg, int id) {
		if (!msg.startsWith("key_") && !msg.startsWith("publicKey_")) {
			msg = encryptMessage(msg);
		}

		for (int i = 1; i < onlineUsers.length; i++) {
			try {
				if ((outputStream[i] != null) && (id != i)) {
					outputStream[i].writeObject(msg);
					outputStream[i].flush();
				}
			} catch (Exception localException) {
			}
		}
	}

	// private Nachricht senden
	private void sendPrivateMsgTo(int toID, int fromID, String msg) {
		try {
			if (outputStream[toID] != null) {
				String packAll = requestPrivate + toID + "_" + fromID + "_" + msg;
				outputStream[toID].writeObject(packAll);
				outputStream[toID].flush();
			}
		} catch (Exception e) {
			try {
				outputStream[fromID].writeObject(requestPrivate + fromID + "_" + toID + "_" + "<User is offline!>");
				outputStream[fromID].flush();
			} catch (Exception localException1) {
			}
		}
	}

	private class ReceiveMessagesClient implements Runnable {
		private boolean signal;
		private Object obj;

		public ReceiveMessagesClient(String threadName) {
			signal = true;
			new Thread(this, threadName).start();
		}

		public void run() {
			try {
				do {
					obj = inputStreamClient.readObject();

					String msg = "";
					try {
						System.out.println(obj.getClass());
						if (obj != null) {
							msg = (String) obj;
						}
					} catch (Exception e) {
						System.out.println("ReceiveMessagesClient Error: " + e.getMessage());
					}
					if (msg.startsWith(requestPrivate)) {
						try {
							msg = msg.replaceAll(requestPrivate, "");

							String to_ID = "";
							String from_ID = "";

							for (int i = 0; i < msg.length(); i++) {
								if (msg.charAt(i) == '_') {
									break;
								}
								to_ID = to_ID + msg.charAt(i);
							}

							msg = msg.replaceAll(to_ID + "_", "");
							for (int i = 0; i < msg.length(); i++) {
								if (msg.charAt(i) == '_') {
									break;
								}
								from_ID = from_ID + msg.charAt(i);
							}
							int fromID = Integer.parseInt(from_ID);
							msg = msg.replaceAll(from_ID + "_", "");
							if (privateUserIsOn[fromID] == false) {
								Point point = new Point(getLocationOnScreen());
								privateMsg[fromID] = new PrivateMsg(fromID, onlineUsers[fromID], onlineUsers[myID],
										myID, screenSize, serverClient, point);
								privateMsg[fromID].setVisible(true);
								privateMsg[fromID].setNewMessage(msg);
								privateUserIsOn[fromID] = true;
							} else {
								privateMsg[fromID].setNewMessage(msg);
							}
						} catch (Exception e) {
							System.out.println("Fehler: " + e.getMessage());
						}
					} else {
						setOnlineUsers(obj);
						try {
							if (!msg.startsWith(requestPrivate)) {
								String encryptedMessage = "LeereNachricht";
								if (msg.startsWith("key_")) {
									String encryptedAESKey = msg.substring(4);
									EasyCrypt ecPri = new EasyCrypt(privKey, "RSA");
									String decryptedAESKey = ecPri.decrypt(encryptedAESKey);
									System.out.println("decryptedKey: " + decryptedAESKey);
									byte[] decodedKey = Base64.getDecoder().decode(decryptedAESKey);
									aesKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
								} else if (msg.startsWith("publicKey_")) {
									String pkey = msg.substring(10);
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
								} else {
									System.out.println("Message = " + (String) obj);
									encryptedMessage = decryptMessage((String) obj);
									nachrichtAnzeigen(encryptedMessage);
								}
							}
						} catch (Exception localException3) {
						}
					}
				} while (signal);
			} catch (EOFException e) {
				signal = false;
				closeClientReceiveThread();
			} catch (ClassNotFoundException | IOException e) {
				signal = false;
				closeClientReceiveThread();
			} catch (Exception e) {
				signal = false;
				closeClientReceiveThread();
				e.printStackTrace();
			}
		}
	}
	// ENDE ReceiveMessagesClient Klasse

	// Alle Verbindungen werden getrennt, Buttons enabled und private user check
	private void closeClientReceiveThread() {
		try {
			outputStreamClient.close();
		} catch (Exception localException) {
		}
		try {
			inputStreamClient.close();
		} catch (Exception localException1) {
		}
		try {
			serverSocket.close();
		} catch (Exception localException2) {
		}
		try {
			socket.close();
		} catch (Exception localException3) {
		}
		listModel.removeAllElements();
		txtMsg.setEditable(false);
		btnCreate.setEnabled(true);
		btnJoin.setEnabled(true);
		for (int i = 0; i < privateUserIsOn.length; i++) {
			if (privateUserIsOn[i] != false) { // 0 zu false
				privateMsg[i].setNewMessage("<Server is offline!>");
			}
		}
	}

	// Client sendet Nachrichten:
	public void sendMessageClient(String msg) {
		try {
			System.out.println("Nachricht sendMessageClient");
			outputStreamClient.writeObject(msg);
			outputStreamClient.flush();
		} catch (Exception localException) {
		}
	}

	// Erstellt online Liste
	private void setOnlineUsers(Object obj) {
		try {
			onlineUsers = ((String[]) obj);
			listModel.removeAllElements();
			for (int i = 0; i < onlineUsers.length; i++) {
				if (onlineUsers[i] != null) {
					listModel.addElement(onlineUsers[i]);
					try {
						Thread.sleep(10L);
					} catch (InterruptedException localInterruptedException) {
					}
					if (myNick == onlineUsers[i]) {
						myID = i;
					}
				}
			}
		} catch (ClassCastException localClassCastException) {
		} catch (ArrayIndexOutOfBoundsException localArrayIndexOutOfBoundsException) {
		}
	}

	private void nachrichtAnzeigen(String text) {
		txtArea.append(text + "\n");
		txtArea.setCaretPosition(txtArea.getText().length());
	}

	private void createAESKey() {
		KeyGenerator keygen;
		try {
			keygen = KeyGenerator.getInstance("AES");
			keygen.init(128);
			aesKey = keygen.generateKey();
			System.out.println("AES-Key generiert");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

	}

	private void createRSAKeys() {
		try {
			KeyPairGenerator keygenRSA = KeyPairGenerator.getInstance("RSA");
			keygenRSA.initialize(1024);
			KeyPair rsaKeys = keygenRSA.genKeyPair();

			privKey = rsaKeys.getPrivate();
			pubKey = rsaKeys.getPublic();
		} catch (Exception e) {
			System.out.println("createRSAKeys Error: " + e.getMessage());
		}
	}

	// Tauscht AES Schlüssel aus
	@SuppressWarnings("unused")
	private void exchangeAESKeys() {
		if (aesKey != null) {
			sendAESKey();
		} else {
			System.out.println("Kein Key zum teilen");
		}

	}

	// Sendet eine AES verschlüsselte Nachricht
	private String encryptMessage(String text) {
		String geheimText = "";
		try {
			EasyCrypt ec = new EasyCrypt(aesKey, "AES");
			geheimText = ec.encrypt(text);
		} catch (Exception e) {
			System.out.println("AES-Exchange Error: " + e.getMessage());
		}
		return geheimText;
	}

	private String decryptMessage(String text) {
		String klartext = "";
		try {
			EasyCrypt ec = new EasyCrypt(aesKey, "AES");
			klartext = ec.decrypt(text); // Verschlüsselt Text mit AES Key

		} catch (Exception e) {
			nachrichtAnzeigen("Nachricht konnte nicht entschlüsselt werden.");
			System.out.println("DecryptError: " + e.getMessage());
		}
		return klartext;
	}

	private void sendPublicKey(boolean client) {
		System.out.println("sendPublicKey");
		String encodedRSAKey = Base64.getEncoder().encodeToString(pubKey.getEncoded());
		if (client) {
			sendMessageClient("publicKey_" + encodedRSAKey);
		} else {
			sendMessages("publicKey_" + encodedRSAKey, 0);
		}
	}

	private void sendAESKey() {
		System.out.println("sendAESKey");
		try {
			EasyCrypt ecPub = new EasyCrypt(pubKeyPartner, "RSA");
			String encodedAESKey = Base64.getEncoder().encodeToString(aesKey.getEncoded());
			String encryptedKey = ecPub.encrypt(encodedAESKey);
			sendMessages("key_" + encryptedKey, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Main Methode
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new ServerClient().setVisible(true);
			}
		});
	}
}
