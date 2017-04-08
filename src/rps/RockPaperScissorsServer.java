/*
* Student Info: Name=Han Wu, ID=16327 
* Subject: CS532B_W7_Fall_2016
* Author: Han
* Filename: RockPaperScissorsServer.java
* Date and Time: Nov 7, 2016 10:27:35 PM 
*/
package rps;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListModel;

public class RockPaperScissorsServer extends JFrame{
    
    DefaultListModel<Integer> sessions = new DefaultListModel<>();
    public static void main(String[] args) {
        RockPaperScissorsServer frame = new RockPaperScissorsServer();
    }
    
    public RockPaperScissorsServer() {
        JTextArea jtaLog = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(jtaLog);
        add(scrollPane, BorderLayout.CENTER);
        
        JPanel paneRight = new JPanel(new BorderLayout());
//        paneRight.setPreferredSize(new Dimension(80, 0));// Fixed width
        JList<Integer> listBox = new JList<>(sessions);
        paneRight.add(listBox, BorderLayout.CENTER);
        JLabel rightText = new JLabel("Active Sessions");
        paneRight.add(rightText, BorderLayout.NORTH);
        add(paneRight, BorderLayout.EAST);
        
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 320);
        setTitle("Rock Paper Scissors Server");
        
        //Center on Screen
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
        
        setVisible(true);
        
        //Start to connect
        try {
            ServerSocket serverSocket = new ServerSocket(8000);
            jtaLog.append(new Date() + ": Server started at socket 8000:\n");
            int sessionNo = 1;
            
            //Create sessions for every two players
            while (true) {
                jtaLog.append("Waiting for players to join session " + sessionNo + "\n");
                Socket player1 = serverSocket.accept();
                jtaLog.append(new Date() + ": Welcome Player 1 "
                        + sessionNo + '\n');
                jtaLog.append("Player 1's IP address"
                        + player1.getInetAddress().getHostAddress() + '\n');

                // Notify that the player is Player 1
                new DataOutputStream(player1.getOutputStream()).writeInt(RPS.PLAYER1);
                
                // Connect to player 2
                Socket player2 = serverSocket.accept();

                jtaLog.append(new Date()
                        + ": Welcome Player 2 " + sessionNo + '\n');
                jtaLog.append("Player 2's IP address"
                        + player2.getInetAddress().getHostAddress() + '\n');

                // Notify that the player is Player 2
                sessions.addElement(sessionNo);
                new DataOutputStream(player2.getOutputStream()).writeInt(RPS.PLAYER2);
                
                HandleASession h = new HandleASession(this, player1, player2, sessionNo++);
                new Thread(h).start();
            }
            
        } catch (Exception e) {
        }
    }
    
    public synchronized void RemoveMe(int sessionId) {
        sessions.removeElement(sessionId);
    }
    
}

class HandleASession implements Runnable {

    private RockPaperScissorsServer server = null;
    
    private Socket player1;
    private Socket player2;
    private int sessionId;
    public int getSessionId() {
        return sessionId;
    }
    
    private DataInputStream fromPlayer1;
    private DataOutputStream toPlayer1;
    private DataInputStream fromPlayer2;
    private DataOutputStream toPlayer2;
    
    public HandleASession(RockPaperScissorsServer server, Socket player1, Socket player2, int sessionId) {
        this.server = server;
        this.player1 = player1;
        this.player2 = player2;
        this.sessionId = sessionId;
    }
    
    private int getMsg(DataInputStream is) throws IOException {
        return is.readInt();
    }
    private void sendMsg(DataOutputStream os, int msg) throws IOException {
        os.writeInt(msg);
    }
    
    @Override
    public void run() {
        try {
            // Create data input and output streams
            DataInputStream fromPlayer1 = new DataInputStream(player1.getInputStream());
            DataOutputStream toPlayer1 = new DataOutputStream(player1.getOutputStream());
            DataInputStream fromPlayer2 = new DataInputStream(player2.getInputStream());
            DataOutputStream toPlayer2 = new DataOutputStream(player2.getOutputStream());
            
            int msgPlayer1 = RPS.QUIT;
            int msgPlayer2 = RPS.QUIT;
            
            int player1WonTimes = 0;
            int player2WonTimes = 0;
            
            int quitter = RPS.PLAYER1;
            
            while (true) {
                sendMsg(toPlayer1, RPS.READY);
                sendMsg(toPlayer2, RPS.READY);
                msgPlayer1 = getMsg(fromPlayer1);
                msgPlayer2 = getMsg(fromPlayer2);
                
                if(msgPlayer1 != RPS.READY || msgPlayer2 != RPS.READY) {
                    if(msgPlayer1 == RPS.QUIT) quitter = RPS.PLAYER1;
                    else quitter = RPS.PLAYER2;
                    break;//quit game
                }
                
                sendMsg(toPlayer1, RPS.THROW);
                sendMsg(toPlayer2, RPS.THROW);
                
                msgPlayer1 = getMsg(fromPlayer1);
                msgPlayer2 = getMsg(fromPlayer2);
                
                if(msgPlayer1 == RPS.QUIT || msgPlayer2 == RPS.QUIT) {
                    if(msgPlayer1 == RPS.QUIT) quitter = RPS.PLAYER1;
                    else quitter = RPS.PLAYER2;
                    break;//quit game
                }
                
                int winner = RPS.getWinner(msgPlayer1, msgPlayer2);
                if(winner == RPS.PLAYER1) player1WonTimes++;
                else if(winner == RPS.PLAYER2) player2WonTimes++;
                
                // tell who's the winner to both players
                sendMsg(toPlayer1, winner);
                sendMsg(toPlayer2, winner);
                
                // opponent's throw
                sendMsg(toPlayer2, msgPlayer1);
                sendMsg(toPlayer1, msgPlayer2);
                
                // their won times
                sendMsg(toPlayer1, player1WonTimes);
                sendMsg(toPlayer1, player2WonTimes);
                sendMsg(toPlayer2, player1WonTimes);
                sendMsg(toPlayer2, player2WonTimes);
                
            }
            
            // Send "QUIT" to the other player
            if(quitter == RPS.PLAYER1)
                sendMsg(toPlayer2, RPS.QUIT);
            else if(quitter == RPS.PLAYER2)
                sendMsg(toPlayer1, RPS.QUIT);
            
            server.RemoveMe(sessionId);
        } catch (Exception e) {
            System.err.println(e.getStackTrace());
        }
    }
}
