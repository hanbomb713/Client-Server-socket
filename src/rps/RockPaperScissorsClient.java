/*
* Student Info: Name=Han Wu, ID=16327 
* Subject: CS532B_W7_Fall_2016
* Author: Han
* Filename: RockPaperScissorsClient.java
* Date and Time: Nov 7, 2016 10:27:35 PM 
*/
package rps;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

/**
 *
 * @author Han
 */
public class RockPaperScissorsClient extends JFrame implements Runnable{

    // Create and initialize a title label
    private JLabel jlblTitle = new JLabel();
    private JLabel jlblSubTitle = new JLabel();
    // Create and initialize a status label
    private JLabel jlblStatus = new JLabel();
    private JLabel jlblImageYou = new JLabel();
    private JLabel jlblImageOpponent = new JLabel();
    private ImageIcon[] imgs = new ImageIcon[4];//last one is for "time-out" will be left null
    
    // Input and output streams from/to server
    private DataInputStream fromServer;
    private DataOutputStream toServer;
    
    private boolean continueToPlay = true;
    // Host name or ip
    private String host = "localhost";
    
    private int lastWinner = RPS.NOONE;
    private int player1WonTimes = 0;
    private int player2WonTimes = 0;
    private int myRole = RPS.PLAYER1;//Am I player 1 or 2
    
    private int clientStatus = RPS.START;
    
    private boolean waiting = true;//whether thread is waiting
    
    private int myThrow = RPS.TIMEOUT;
    private int opponentsThrow = RPS.TIMEOUT;
    private boolean quitFlag = false;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        RockPaperScissorsClient c = new RockPaperScissorsClient();
    }
    
    public RockPaperScissorsClient() {
        
        //load images
        imgs[RPS.ROCK - 1] = new ImageIcon("rock.png");
        imgs[RPS.PAPER - 1] = new ImageIcon("paper.png");
        imgs[RPS.SCISSORS - 1] = new ImageIcon("scissors.png");
        imgs[RPS.TIMEOUT - 1] = null;
        
        // Initialize UI
        jlblTitle.setHorizontalAlignment(JLabel.CENTER);
        jlblTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        jlblSubTitle.setFont(new Font("SansSerif", Font.PLAIN, 18));
        jlblTitle.setBorder(new LineBorder(Color.BLUE, 1));
        JPanel pTitle = new JPanel(new GridLayout(0, 1));
        pTitle.add(jlblTitle);
        pTitle.add(jlblSubTitle);
        pTitle.setBorder(new LineBorder(Color.BLUE, 1));
        add(pTitle, BorderLayout.NORTH);
        
        JPanel pBottom = new JPanel(new GridLayout(0, 1));
        // Buttons
        JPanel pBtns = new JPanel();
        pBottom.add(pBtns);
        
        Button btnReady = new Button("Start");
        Button btnRock = new Button("Rock");
        Button btnPaper = new Button("Paper");
        Button btnScissors = new Button("Scissors");
        Button btnQuit = new Button("End");
        
        btnReady.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // only respond when the server has told me to "get ready"
                if(clientStatus == RPS.READY)
                    waiting = false;
            }
        });
        btnRock.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(clientStatus == RPS.THROW) {
                    myThrow = RPS.ROCK;
                    waiting = false;
                }
            }
        });
        btnPaper.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(clientStatus == RPS.THROW) {
                    myThrow = RPS.PAPER;
                    waiting = false;
                }
            }
        });
        btnScissors.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(clientStatus == RPS.THROW) {
                    myThrow = RPS.SCISSORS;
                    waiting = false;
                }
            }
        });
        btnQuit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(clientStatus == RPS.READY || clientStatus == RPS.THROW) {
                    quitFlag = true;
                    waiting = false;
                }
            }
        });
        
        pBtns.add(btnReady);
        pBtns.add(btnRock);
        pBtns.add(btnPaper);
        pBtns.add(btnScissors);
        pBtns.add(btnQuit);
        
        // Status Bar
        jlblStatus.setBorder(BorderFactory.createLoweredBevelBorder());
        pBottom.add(jlblStatus);
        add(pBottom, BorderLayout.SOUTH);
        
        // image boxes
        Dimension d = new Dimension(imgs[0].getIconWidth(), imgs[0].getIconHeight()); 
        jlblImageYou.setMinimumSize(d);
        jlblImageYou.setHorizontalAlignment(JLabel.CENTER);
        jlblImageYou.setVerticalAlignment(JLabel.CENTER);
//        jlblImageYou.setBorder(BorderFactory.createLineBorder(Color.CYAN, 1));
        jlblImageOpponent.setMinimumSize(d);
        jlblImageOpponent.setHorizontalAlignment(JLabel.CENTER);
        jlblImageOpponent.setVerticalAlignment(JLabel.CENTER);
//        jlblImageOpponent.setBorder(BorderFactory.createLineBorder(Color.CYAN, 1));
        JPanel pImgPanel = new JPanel(new GridLayout(1, 0));
        pImgPanel.add(jlblImageYou);
        pImgPanel.add(jlblImageOpponent);
        add(pImgPanel, BorderLayout.CENTER);
        
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("RPS Client");
        d = new Dimension(800, 600);
        setSize(d);
        setMinimumSize(d);
        //Center on Screen
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
        
        //
        connectToServer();
        
        this.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                if(clientStatus != RPS.QUIT) {
                    try {
                        sendMsg(toServer, RPS.QUIT);
                    } catch (IOException ex) {
                        Logger.getLogger(RockPaperScissorsClient.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    finally {
                        System.exit(0);
                        return;
                    }
                }
                System.exit(0);
            }
        });
        
        setVisible(true);
    }
    
    private void connectToServer() {
        try {
            // Create a socket to connect to the server
            Socket socket;
            socket = new Socket(host, 8000);

            // Create an input stream to receive data from the server
            fromServer = new DataInputStream(socket.getInputStream());

            // Create an output stream to send data to the server
            toServer = new DataOutputStream(socket.getOutputStream());
        } catch (Exception ex) {
            System.err.println(ex);
        }

        // Control the game on a separate thread
        Thread thread = new Thread(this);
        thread.start();
    }
    
    private int getMsg(DataInputStream is) throws IOException {
        return is.readInt();
    }
    private void sendMsg(DataOutputStream os, int msg) throws IOException {
        os.writeInt(msg);
    }
    
    private void displayScores() {
        StringBuilder sb = new StringBuilder();
        if(lastWinner == RPS.DRAW)
            sb.append("Draw Game! ");
        else if(lastWinner == myRole)
            sb.append("You win! ");
        else if(lastWinner != RPS.NOONE)
            sb.append("You lose! ");
        sb.append("( Player 1 won ");
        sb.append(player1WonTimes);
        sb.append(" time(s). Player 2 won ");
        sb.append(player2WonTimes);
        sb.append(" time(s) )");
        
        jlblImageYou.setIcon(imgs[myThrow - 1]);
        jlblImageOpponent.setIcon(imgs[opponentsThrow - 1]);
        jlblImageYou.invalidate();
        jlblImageOpponent.invalidate();
        
        jlblSubTitle.setText(sb.toString());
        
    }
    
    private void waitForReady() throws InterruptedException {
        while (waiting) {
            Thread.sleep(100);
        }

        waiting = true;
    }
    
    // Return that the Whether or not the player throws in time
    private boolean waitForThrow() throws InterruptedException {
        int counter = 0;
        while (waiting) {
            Thread.sleep(100);
            counter++;
            if(counter >= 51)
                return false;
        }
        waiting = true;
        return true;
    }

    @Override
    public void run() {
        try {
            myRole = getMsg(fromServer);
            // Am I Player 1 or 2?
            if(myRole == RPS.PLAYER1) {
                jlblTitle.setText("Player 1");
                jlblStatus.setText("Waiting for another player");
            } else if (myRole == RPS.PLAYER2) {
                jlblTitle.setText("Player 2");
            }
            
            //TODO WRAP INTO A WHILE LOOP
            //REMEMBER TO ADD "BREAK" IN QUIT SITUATIONS!!!!!!!!!!
            while(continueToPlay) {
                displayScores();

                int sig = getMsg(fromServer);
                if(sig != RPS.READY) {
                    clientStatus = RPS.QUIT;
                    jlblStatus.setText("Your opponent leaves the game.");
                    break;
                }
                clientStatus = RPS.READY;
                jlblStatus.setText("Please click \"Start\".");
                waitForReady();
                if(quitFlag) {
                    clientStatus = RPS.QUIT;
                    sendMsg(toServer, RPS.QUIT);
                    jlblStatus.setText("You leave the game");
                    break;
                } else
                    sendMsg(toServer, RPS.READY);
                sig = getMsg(fromServer);
                if(sig != RPS.THROW) {
                    clientStatus = RPS.QUIT;
                    jlblStatus.setText("Your opponent leaves the game.");
                    break;
                }
                clientStatus = RPS.THROW;
                jlblStatus.setText("Show it in 5 seconds.");
                if(!waitForThrow())// if not in time
                    myThrow = RPS.TIMEOUT;

                // TODO PASS TO SERVER
                sendMsg(toServer, myThrow);
                if(quitFlag) {
                    clientStatus = RPS.QUIT;
                    jlblStatus.setText("You leave the game");
                    break;
                }

                // TODO GET RESULT
                lastWinner = getMsg(fromServer);
                opponentsThrow = getMsg(fromServer);
                player1WonTimes = getMsg(fromServer);
                player2WonTimes = getMsg(fromServer);

                //displayScores();

                clientStatus = RPS.START;
                jlblStatus.setText("Please click \"Start\" to play again.");
            }
            
        } catch (IOException ex) {
            Logger.getLogger(RockPaperScissorsClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(RockPaperScissorsClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
}
