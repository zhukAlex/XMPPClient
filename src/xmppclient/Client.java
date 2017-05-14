/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xmppclient;

import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.xml.transform.TransformerConfigurationException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

/**
 *
 * @author Алексей
 */
public class Client extends javax.swing.JFrame {

    /**
     * Creates new form Client
     */
    private DataInputStream in;
    private DataOutputStream out;
    private String login;
    private ArrayList<String> friends = new ArrayList<String>();
    private ArrayList<String> messages = new ArrayList<String>();
    
    public Client() throws IOException, org.jdom.JDOMException, TransformerConfigurationException {
        initComponents();  
        jTextArea1.setFocusable(false);
    }
    
    private boolean auth(Document doc) throws IOException{
        boolean result = false;
        Element root = doc.getRootElement();
        
        if(root.getChild("authSuccess", Namespace.getNamespace("XMPP 1.0")) != null){
            result = true;
            List<Element> elements = root.getContent();
            
            for(Element e : elements){
                if( !e.getText().equals("") ){
                    friends.add( e.getText() );
                    System.out.println( e.getText() );
                }
            }
            
            JOptionPane.showMessageDialog(this, "Авторизация прошла успешно.", "Готов", 
                JOptionPane.INFORMATION_MESSAGE);
            System.out.println(true);
        }
        else if(root.getChild("authError", Namespace.getNamespace("XMPP 1.0")) != null){
            JOptionPane.showMessageDialog(this, "Логин или пароль не верен!", "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
        }
        else if(root.getChild("errorCount", Namespace.getNamespace("XMPP 1.0")) != null){
            JOptionPane.showMessageDialog(this, "Лимит пользователей на сервере превышен.", "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
        }
        
        return result;
    }
    
    public void parse(Document doc) throws IOException{
        Element root = doc.getRootElement();
        String from = null;
        String to = null;
        String text = null;

        if(root.getChildText("message", Namespace.getNamespace("XMPP 1.0")) != null){
            from = root.getAttributeValue("from");
            to = root.getAttributeValue("to");
            text = root.getChildText(text,  Namespace.getNamespace("XMPP 1.0")); 
            int index = searchFriend(from);
            if(index != -1){
                messages.set(index, messages.get(index) + ("Вам от " + from + ": " + text + "\n"));
                jTextArea1.setText( messages.get(index) );
                jList1.setSelectedIndex(index);
            }
            
            System.out.println(text);
        }
        else if(root.getChildText("error", Namespace.getNamespace("XMPP 1.0")) != null){
            JOptionPane.showMessageDialog(this, "Сообщение не отправлено, пользователь находится в режиме offline.", "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
        }
        else if(root.getChildText("ack", Namespace.getNamespace("XMPP 1.0")) != null){
            text = "Вы: " + jTextArea2.getText() + "\n";
            messages.set(jList1.getSelectedIndex(), messages.get( jList1.getSelectedIndex() ) + text);
            jTextArea2.setText("");
            jTextArea1.setText(messages.get( jList1.getSelectedIndex() ));
        }
        else if(root.getChild("online", Namespace.getNamespace("XMPP 1.0")) != null){
            List<Element> elements = root.getContent();
            ArrayList<String> statusFriends = new ArrayList<String>();
            int index = 0;
            
            for(String f : friends)
                statusFriends.add(f + " (offline)");
            
            for(Element e : elements){
                if( !e.getText().equals("") ){
                    index = searchFriend( e.getText() );
                    statusFriends.set(index, statusFriends.get(index).substring(0, statusFriends.get(index).indexOf("(") ) + "(online)" );
                }
            }
            
            setFriend(statusFriends);
        }
    }
    
    private int searchFriend(String friend){
        int i = 0;
        int index = -1;
        for(String s : friends){
            if( s.equals(friend) )
                index = i;
            i++;
        }
        
        return index;
    }
        
    private void sendXml(Document doc) throws FileNotFoundException, IOException{ 
        byte[] array = XML.documentToString(doc).getBytes("UTF-8");
        out.writeInt(array.length);
        for(byte a : array)
            out.write(a);
        out.flush();
      
        System.out.println(XML.documentToString(doc));   
    }
    
    private Document sendMessage(String message, String to, String from){
        Document doc = new Document();
        Element element = new Element("Stream", Namespace.getNamespace("XMPP 1.0"));
    
        element.addContent(new Element("message", Namespace.getNamespace("XMPP 1.0")).setText(message));
        element.setAttribute("from", from);
        element.setAttribute("to", to);
        doc.addContent(element);
        
        return doc;
    }
    
    private Document sendAuth(String login, String password){
        Document doc = new Document();
        
        Element element = new Element("Stream", Namespace.getNamespace("XMPP 1.0"));
        element.addContent(new Element("auth", Namespace.getNamespace("XMPP 1.0")));
        
        element.setAttribute("login", login);
        element.setAttribute("password", password);
        
        doc.addContent(element);
        
        return doc;
    }
    
    private Document sendClose(){
        Document doc = new Document();
        
        Element element = new Element("Stream", Namespace.getNamespace("XMPP 1.0"));
        element.addContent(new Element("close", Namespace.getNamespace("XMPP 1.0")));
        doc.addContent(element);
        
        System.out.println(XML.documentToString(doc));
        return doc;
    }
    
    private Document sendOnline(){
        Document doc = new Document();
        
        Element element = new Element("Stream", Namespace.getNamespace("XMPP 1.0"));
        element.addContent(new Element("online", Namespace.getNamespace("XMPP 1.0")));
        doc.addContent(element);
        
        return doc;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jComboBox1 = new javax.swing.JComboBox<>();
        jTextField2 = new javax.swing.JTextField();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jPasswordField1 = new javax.swing.JPasswordField();
        jTextField1 = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jButton3 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList<>();
        jLabel4 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();

        jButton1.setText("jButton1");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jTextField2.setText("jTextField2");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jPasswordField1.setText("jPasswordField1");

        jTextField1.setText("login");

        jButton2.setText("Авторизоваться ");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel1.setText("Логин:");

        jLabel2.setText("Пароль:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(202, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton2)
                    .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(182, 182, 182))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton2)
                .addContainerGap(137, Short.MAX_VALUE))
        );

        jTextField1.getAccessibleContext().setAccessibleName("TextLogin");

        jTabbedPane1.addTab("Авторизация", jPanel1);

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jScrollPane2.setViewportView(jTextArea2);

        jButton3.setText("Отправить");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jLabel3.setText("Друзья:");

        jList1.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jList1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jList1MouseClicked(evt);
            }
        });
        jList1.addInputMethodListener(new java.awt.event.InputMethodListener() {
            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {
                jList1CaretPositionChanged(evt);
            }
            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
            }
        });
        jScrollPane3.setViewportView(jList1);

        jLabel4.setText("Ваш диалог");

        jButton4.setText("Обновить");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane3)
                    .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, 132, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 246, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton3))
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 201, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, 42, Short.MAX_VALUE)
                    .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(23, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Сообщения", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 331, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed

        int serverPort = 51000;
        String address = "localhost";
        try{
            InetAddress ipAddress = InetAddress.getByName(address);
            Socket socket = new Socket(ipAddress, serverPort);

            InputStream sin = socket.getInputStream();
            OutputStream sout = socket.getOutputStream();
            in = new DataInputStream(sin);
            out = new DataOutputStream(sout);
            
            sendXml( sendAuth( jTextField1.getText(), jPasswordField1.getText() ) );
            
            if( auth( XML.readMessage(socket) ) ){
                User user = new User(socket, this); 
                Thread t = new Thread(user); 
                t.start();
                setFriend(friends);
                login = jTextField1.getText();
            }
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JDOMException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        try {
           sendXml( sendClose() );
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);   
        }finally{
            System.exit(0);
        }
    }//GEN-LAST:event_formWindowClosing

    private void jList1CaretPositionChanged(java.awt.event.InputMethodEvent evt) {//GEN-FIRST:event_jList1CaretPositionChanged
        System.out.print(evt.getText());
    }//GEN-LAST:event_jList1CaretPositionChanged

    private void jList1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jList1MouseClicked
        jTextArea1.setText( messages.get( jList1.getSelectedIndex() ) );
        jLabel4.setText("Ваш диалог с " + jList1.getSelectedValue() );
    }//GEN-LAST:event_jList1MouseClicked

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        try {
            sendXml( sendMessage(jTextArea2.getText(), friends.get( jList1.getSelectedIndex() ), login) );
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        try {
            sendXml( sendOnline() );
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButton4ActionPerformed

    private void setFriend(ArrayList<String> friends){
        String [] data = new String[friends.size()];
        int i = 0;
        messages.clear();
        jList1.removeAll();
        for(String s : friends){
            messages.add("");
            data[i] = s;
            i++;
        }
        jList1.setListData( data );
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new Client().setVisible(true);
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                } catch (org.jdom.JDOMException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                } catch (TransformerConfigurationException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JList<String> jList1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    // End of variables declaration//GEN-END:variables
}
