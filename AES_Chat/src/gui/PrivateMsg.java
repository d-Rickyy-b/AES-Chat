package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

class PrivateMsg
  extends JFrame
{
  private static final long serialVersionUID = 1L;
  private JTextField txtMsg;
  private JTextArea txtArea;
  private JScrollPane scrollArea;
  private Point point;
  private ServerClient serverClient;
  private int toID;
  private int myID;
  private String myNick;
  private final String requestPrivate = "requestPrivate_";
  
  public PrivateMsg(int toID, String title, String myNick, int myID, Dimension screenSize, ServerClient serverClient, Point point)
  {
    this.point = point;
    this.toID = toID;
    this.myID = myID;
    this.myNick = myNick;
    this.serverClient = serverClient;
    initUI(title, screenSize);
  }
  
  private void initUI(String title, Dimension screenSize)
  {
    try
    {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception localException) {}
    setTitle("Chat with: " + title);
    setDefaultCloseOperation(2);
    setSize(screenSize.width / 4, screenSize.height / 3);
    if ((this.point.x <= 0) && (this.point.y <= 0)) {
      setLocationRelativeTo(null);
    } else {
      setLocation(this.point.x + 32, this.point.y + 64);
    }
    setResizable(true);
    setFocusable(true);
    toFront();
    
    setLayout(new BorderLayout());
    
    initComponents();
    

    addWindowListener(new WindowAdapter()
    {
      public void windowOpened(WindowEvent event)
      {
        PrivateMsg.this.txtMsg.requestFocus();
      }
      
      public void windowClosing(WindowEvent event)
      {
        PrivateMsg.this.serverClient.setPrivateUserIsOn(PrivateMsg.this.toID);
      }
    });
    addComponentListener(new ComponentAdapter()
    {
      public void componentResized(ComponentEvent event)
      {
        PrivateMsg.this.repaint();
      }
    });
  }
  
  private void initComponents()
  {
    this.txtMsg = new JTextField();
    this.txtMsg.addKeyListener(new KeyAdapter()
    {
      public void keyPressed(KeyEvent event)
      {
        if (event.getKeyCode() == 10)
        {
          PrivateMsg.this.txtArea.append(PrivateMsg.this.myNick + ":" + PrivateMsg.this.txtMsg.getText() + "\n");
          if (PrivateMsg.this.myID == 0) {
            PrivateMsg.this.serverClient.serverSendPrivate("requestPrivate_" + PrivateMsg.this.toID + "_" + PrivateMsg.this.myID + "_" + PrivateMsg.this.myNick + ":" + PrivateMsg.this.txtMsg.getText(), PrivateMsg.this.toID);
          } else {
            PrivateMsg.this.serverClient.sendMessageClient("requestPrivate_" + PrivateMsg.this.toID + "_" + PrivateMsg.this.myID + "_" + PrivateMsg.this.myNick + ":" + PrivateMsg.this.txtMsg.getText());
          }
          PrivateMsg.this.txtArea.setCaretPosition(PrivateMsg.this.txtArea.getText().length());
          PrivateMsg.this.txtMsg.setText("");
        }
      }
    });
    getContentPane().add(this.txtMsg, "South");
    

    this.txtArea = new JTextArea();
    this.txtArea.setEditable(false);
    

    this.scrollArea = new JScrollPane(this.txtArea, 20, 31);
    this.scrollArea.getVerticalScrollBar().setUnitIncrement(15);
    getContentPane().add(this.scrollArea, "Center");
  }
  
  public void setNewMessage(String msg)
  {
    this.txtArea.append(msg + "\n");
    this.txtArea.setCaretPosition(this.txtArea.getText().length());
  }
}
