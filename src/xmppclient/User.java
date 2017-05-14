/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xmppclient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerConfigurationException;
import org.jdom.Document;
import org.jdom.JDOMException;

/**
 *
 * @author Алексей
 */
public class User implements Runnable {

    DataInputStream in;
    Socket socket;
    Client client;
    
    public User(Socket socket, Client client) throws IOException{
        InputStream sin = socket.getInputStream();
        in = new DataInputStream(sin);
        this.socket = socket;
        this.client = client;
    }

    @Override
    public void run() {
        while(true){
            try {
                client.parse( XML.readMessage(socket) );
            } catch (JDOMException ex) {
                Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TransformerConfigurationException ex) {
                Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
