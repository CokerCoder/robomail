package automail;

import java.util.HashMap;

public class Building {


    // Floor status of each floor, 0 for empty floor, n for having some robots, -n for locked (fragile handling)
    public static HashMap<Integer, Integer> FLOOR_STATUS = new HashMap<>();

    /** The number of floors in the building **/
    public static int FLOORS;
    
    /** Represents the ground floor location */
    public static final int LOWEST_FLOOR = 1;
    
    /** Represents the mailroom location */
    public static final int MAILROOM_LOCATION = 1;


    public static void initializeFloors() {
        for (int i = 1; i < FLOORS+1; i++) {
            FLOOR_STATUS.put(i, 0);
        }
    }

}
