package automail;

public class Building {
	
	
    /** The number of floors in the building **/
    public static int FLOORS;
    
    /** Represents the ground floor location */
    public static final int LOWEST_FLOOR = 1;
    
    /** Represents the mailroom location */
    public static final int MAILROOM_LOCATION = 1;

    // Number of robots on the floor
    public static int[] NUM_ROBOTS;
    // If the floor need to have sole access, 0 for no, 1 for yes
    public static int[] CAUTION_FLOORS;


    public static void resetFloors() {
        NUM_ROBOTS = new int[FLOORS];
        CAUTION_FLOORS = new int[FLOORS];
    }
    
}
