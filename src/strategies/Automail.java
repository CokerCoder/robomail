package strategies;

import automail.IMailDelivery;
import automail.Robot;
import automail.CautionRobot;

public class Automail {
	      
    public Robot[] robots;
    public IMailPool mailPool;
    
    public Automail(IMailPool mailPool, IMailDelivery delivery, int numRobots, Boolean caution) {
    	// Swap between simple provided strategies and your strategies here
    	    	
    	/** Initialize the MailPool */
    	
    	this.mailPool = mailPool;
    	
    	/** Initialize robots */
    	if (caution) {
            robots = new CautionRobot[numRobots];
            for (int i = 0; i < numRobots; i++) robots[i] = new CautionRobot(delivery, mailPool);
        } else {
            robots = new Robot[numRobots];
            for (int i = 0; i < numRobots; i++) robots[i] = new Robot(delivery, mailPool);
        }
    }
    
}
