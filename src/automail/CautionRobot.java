package automail;

import java.util.Arrays;

import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import strategies.IMailPool;

public class CautionRobot extends Robot {


    private enum RobotState { DELIVERING, WAITING, RETURNING, WRAPPING, UNWRAPPING }
    private RobotState current_state;
    
    private MailItem arm = null;
    private static final int WRAPPING_TIME = 2;
    private static final int UNWRAPPING_TIME = 1;
    private int timer = 0;

    /**
     * Initiates the robot's location at the start to be at the mailroom
     * also set it to be waiting for mail.
     *
     * @param delivery governs the final delivery
     * @param mailPool is the source of mail items
     */
    public CautionRobot(IMailDelivery delivery, IMailPool mailPool) {
        super(delivery, mailPool);
        this.current_state = RobotState.RETURNING;
        Building.FLOOR_WITH_ROBOT.add(Building.MAILROOM_LOCATION);
    }

    public void addToArm(MailItem mailItem) throws ItemTooHeavyException {
        assert(arm == null);
        arm = mailItem;
        if (arm.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
    }
    public MailItem getArm(){
        return arm;
    }

    @Override
    protected void setRoute() {
        // Set destination of fragile item
        if (arm!=null) {    
            destination_floor = arm.destination_floor;
        } else {
            destination_floor = deliveryItem.getDestFloor();
        }
    }

    @Override
    protected void moveTowards(int destination) {
        if(current_floor < destination){
            if(Building.FLOOR_WITH_CAUTION_ROBOT.contains(current_floor+1)) return;
            Building.FLOOR_WITH_ROBOT.remove((Object)current_floor);
            current_floor++;
        }
        else {
            if(Building.FLOOR_WITH_CAUTION_ROBOT.contains(current_floor-1)) return;
            Building.FLOOR_WITH_ROBOT.remove((Object)current_floor);
            current_floor--;
        }
        Building.FLOOR_WITH_ROBOT.add(current_floor);
    }

    
    private void changeState(RobotState nextState) {
        assert(!(deliveryItem == null && tube != null));
    	if (current_state != nextState) {
            System.out.printf("T: %3d > %7s changed from %s to %s%n", Clock.Time(), getIdTube(), current_state, nextState);
    	}
    	current_state = nextState;
    	if(nextState == RobotState.DELIVERING){
            if (arm!=null) {
                System.out.printf("T: %3d > %9s-> [%s]%n", Clock.Time(), getIdTube(), arm.toString());
            }
            else if (deliveryItem!=null) {
                System.out.printf("T: %3d > %9s-> [%s]%n", Clock.Time(), getIdTube(), deliveryItem.toString());
            }
    	}
    }

    @Override
    public boolean isEmpty() {
        return (deliveryItem == null && tube == null && arm == null);
    }

    @Override
    public void step() throws ExcessiveDeliveryException {
        
        //System.out.println(Building.FLOOR_WITH_CAUTION_ROBOT.toString());
        //System.out.println(Building.FLOOR_WITH_ROBOT.toString());
        switch(current_state) {
    		/** This state is triggered when the robot is returning to the mailroom after a delivery */
    		case RETURNING:
    			/** If its current position is at the mailroom, then the robot should change state */
                if(current_floor == Building.MAILROOM_LOCATION){
                	if (tube != null) {
                		mailPool.addToPool(tube);
                        System.out.printf("T: %3d >  +addToPool [%s]%n", Clock.Time(), tube.toString());
                        tube = null;
                	}
        			/** Tell the sorter the robot is ready */
        			mailPool.registerWaiting(this);
                	changeState(RobotState.WAITING);
                } else {
                	/** If the robot is not at the mailroom floor yet, then move towards it! */
                    moveTowards(Building.MAILROOM_LOCATION);
                	break;
                }
    		case WAITING:
    			if (arm != null && receivedDispatch) {
    				receivedDispatch = false;
                	deliveryCounter = 0; // reset delivery counter
        			this.setRoute();
                	changeState(RobotState.WRAPPING);
    			}
    			
                /** If the StorageTube is ready and the Robot is waiting in the mailroom then start the delivery */
    			if(!isEmpty() && receivedDispatch) {
                	receivedDispatch = false;
                	deliveryCounter = 0; // reset delivery counter
        			this.setRoute();
                	changeState(RobotState.DELIVERING);
                }
                break;
            case DELIVERING:
    			if(current_floor == destination_floor){ // If already here drop off either way
                    /** Delivery complete, report this to the simulator! */
                    if (deliveryItem != null && current_floor == deliveryItem.getDestFloor()) {
                        delivery.deliver(deliveryItem);
                        deliveryItem = null;
                    }
                    else if (arm != null && current_floor == arm.destination_floor) {
                        // Needs to be unwrapped first
                        // Check if there are other robots on the same floor
                        if(!Building.FLOOR_WITH_CAUTION_ROBOT.contains(arm.destination_floor)) {
                            Building.FLOOR_WITH_CAUTION_ROBOT.add(arm.destination_floor);
                        }
                    	if (timer != UNWRAPPING_TIME) {
                    	    Building.FLOOR_WITH_ROBOT.remove((Object)arm.destination_floor);
                    	    if(!Building.FLOOR_WITH_ROBOT.contains(arm.destination_floor)){
                    	        changeState(RobotState.UNWRAPPING);// To avoid changing state again
                    	    }
                            Building.FLOOR_WITH_ROBOT.add(arm.destination_floor);
                    	    break;
                    	} else {
                            delivery.deliver(arm);
                            Building.FLOOR_WITH_CAUTION_ROBOT.remove((Object)arm.destination_floor);
                            arm = null;
                            timer = 0; // Reset timer
                    	}
                    }
                    
                    deliveryCounter++;
                    
                    if(deliveryCounter > 3){  // Implies a simulation bug
                    	throw new ExcessiveDeliveryException();
                    }
                    if (arm != null) {
                        this.setRoute();
                        changeState(RobotState.DELIVERING);
                    }
                    else if (deliveryItem != null) {
                    	super.setRoute();
                    	changeState(RobotState.DELIVERING);
                    }
                    else if (tube != null) {
                        /** If there is another item, set the robot's route to the location to deliver the item */
                        deliveryItem = tube;
                        tube = null;
                        super.setRoute();
                        changeState(RobotState.DELIVERING);
                    }

                    else {
                        changeState(RobotState.RETURNING);
                    }
    			} 
    			else {
	        		/** The robot is not at the destination yet, move towards it! */
	                moveTowards(destination_floor);
    			}
                break;
            case UNWRAPPING:
            	timer++;
            	if (timer == UNWRAPPING_TIME) {
            		changeState(RobotState.DELIVERING);
            	}
                break;
            case WRAPPING:
            	timer++;
            	if (timer == WRAPPING_TIME) {
            		changeState(RobotState.DELIVERING);
            		timer = 0; // Reset timer
            	}
                break;
    	}
    }
}
