/*
* Student Info: Name=Han Wu, ID=16327 
* Subject: CS532B_W7_Fall_2016
* Author: Han
* Filename: RockPaperScissorsServer.java
* Date and Time: Nov 7, 2016 10:27:35 PM 
*/
package rps;

/**
 *
 * @author Han
 */
public class RPS {

    public static final int NOONE = 0;
    public static final int PLAYER1 = 1;
    public static final int PLAYER2 = 2;
    public static final int DRAW = 3;

    public static final int QUIT = -1;
    public static final int START = 0;
    public static final int READY = 1;
    public static final int THROW = 2;
    
    public static final int ROCK = 1;
    public static final int PAPER = 2;
    public static final int SCISSORS = 3;
    public static final int TIMEOUT = 4;
    
    public static int getWinner(int p1, int p2) {
        if(p1 == p2)
            return DRAW;
        
        if(p1 == TIMEOUT)
            return PLAYER2;
        else if(p2 == TIMEOUT)
            return PLAYER1;
        
        if(p1 == p2 % 3 + 1)
            return PLAYER1;
        else
            return PLAYER2;
    }
}
