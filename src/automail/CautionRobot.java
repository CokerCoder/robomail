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
        // Deliver fragile first
        if (arm!=null) {    
            destination_floor = arm.getDestFloor();
        } else {
            destination_floor = deliveryItem.getDestFloor();
        }
    }

    @Override
    protected void moveTowards(int destination) {
        
        int current = current_floor;
        int current_status = Building.FLOOR_STATUS.get(current);
        
        if (current < destination) {
            int upstairs = Building.FLOOR_STATUS.get(current+1);
            if (upstairs < 0) {
                return;
            }
            Building.FLOOR_STATUS.put(current+1, upstairs+1);
            
            current_floor++;
        }
        else {
            int downstairs = Building.FLOOR_STATUS.get(current-1);
            if (downstairs < 0) {
                return;
            }
            Building.FLOOR_STATUS.put(current-1, downstairs+1);
            
            current_floor--;
        }


        if (current_status < 0) {
            Building.FLOOR_STATUS.put(current, current_status+1);
        } else if (current_status > 0) {
            Building.FLOOR_STATUS.put(current, current_status-1);
        }

        if (arm!=null && current_floor==arm.getDestFloor()) {
            // lock current floor as soon as a cautionRobot arrives its destination
            Building.FLOOR_STATUS.put(current_floor, -Building.FLOOR_STATUS.get(current_floor));
        }

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
        
        // System.out.println(Arrays.asList(Building.FLOOR_STATUS));

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
                    
                    if (Building.FLOOR_STATUS.get(1) > 0) {
                        Building.FLOOR_STATUS.put(1, Building.FLOOR_STATUS.get(1)-1); // Return to the mailroom
                    } else if (Building.FLOOR_STATUS.get(1) < 0) {
                        Building.FLOOR_STATUS.put(1, Building.FLOOR_STATUS.get(1)+1);
                    }
                    
                } else {
                	/** If the robot is not at the mailroom floor yet, then move towards it! */
                    this.moveTowards(Building.MAILROOM_LOCATION);
                }

                break;

            case WAITING:

                // If the mailroom floor is locked, keep the robot waiting in the mailroom
                if (Building.FLOOR_STATUS.get(1) < 0) {
                    return;
                }
                
                if (receivedDispatch) {
                    receivedDispatch = false;
                    deliveryCounter = 0;
                    this.setRoute();

                    // Wrapping is allowed anytime, happens inside mailroom
                    if (arm != null) {
                        changeState(RobotState.WRAPPING);
                        break;
                    }
                    
                    // If level 1 is not locked, let the robot go through
                    else {
                        Building.FLOOR_STATUS.put(1, Building.FLOOR_STATUS.get(1)+1);
                        // The robot will be on level 1 as soon as it turns into delivery mode
                        changeState(RobotState.DELIVERING); 
                    }
                }

                break;


            case DELIVERING:

    			if (current_floor == destination_floor) { 

                    /** Delivery complete, report this to the simulator! */
                    if (arm != null && current_floor == arm.getDestFloor()) {

                        // Needs to be unwrapped first
                        // Check if there are other robots on the same floor
                        if (Math.abs(Building.FLOOR_STATUS.get(current_floor)) != 1) {
                            break;
                        }

                        // othereise start unwrapping
                    	if (timer != UNWRAPPING_TIME) {
                            changeState(RobotState.UNWRAPPING);
                    	    break;
                    	} else {
                            delivery.deliver(arm);
                            Building.FLOOR_STATUS.put(current_floor, 1); // unlock current floor after delivered
                            arm = null;
                            timer = 0; // Reset timer
                    	}
                    }
                    
                    else if (deliveryItem != null && current_floor == deliveryItem.getDestFloor()) {
                        delivery.deliver(deliveryItem);
                        deliveryItem = null;
                    }
                    
                    deliveryCounter++;
                    
                    if(deliveryCounter > 3){  // Implies a simulation bug
                    	throw new ExcessiveDeliveryException();
                    }


                    if (deliveryItem != null) {
                    	this.setRoute();
                    	changeState(RobotState.DELIVERING);
                    }
                    else if (tube != null) {
                        /** If there is another item, set the robot's route to the location to deliver the item */
                        deliveryItem = tube;
                        tube = null;
                        this.setRoute();
                        changeState(RobotState.DELIVERING);
                    }
                    else {
                        changeState(RobotState.RETURNING);
                    }
    			} 
    			else {
	        		/** The robot is not at the destination yet, move towards it! */
	                this.moveTowards(destination_floor);
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
                    
                    // If mailroom floor is locked, keep the robot waiting in the mailroom
                    if (Building.FLOOR_STATUS.get(1) < 0) {
                        timer--;
                        return;
                    }
                    
                    // otherwise let the robot pass
                    Building.FLOOR_STATUS.put(1, Building.FLOOR_STATUS.get(1)+1);
                    
                    // lock the mailroom floor if deliver to this floor
                    if (arm.getDestFloor() == 1) {
                        Building.FLOOR_STATUS.put(1, -Building.FLOOR_STATUS.get(1));
                    }
                    
                    timer = 0; // Reset timer
                    
                    changeState(RobotState.DELIVERING);
                    break;
                }

                break;
    	}
    }
}
