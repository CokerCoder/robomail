package automail;

import exceptions.BreakingFragileItemException;
import exceptions.ItemTooHeavyException;
import strategies.IMailPool;

public class CautionRobot extends Robot {

    private MailItem arm = null;

    /**
     * Initiates the robot's location at the start to be at the mailroom
     * also set it to be waiting for mail.
     *
     * @param delivery governs the final delivery
     * @param mailPool is the source of mail items
     */
    public CautionRobot(IMailDelivery delivery, IMailPool mailPool) {
        super(delivery, mailPool);
    }

    public void addToArm(MailItem mailItem) throws ItemTooHeavyException {
        assert(arm == null);
        arm = mailItem;
        if (arm.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
    }
    public MailItem getArm(){
        return arm;
    }
}
