package strategies;

import java.util.LinkedList;
import java.util.Comparator;
import java.util.ListIterator;

import automail.CautionRobot;
import automail.MailItem;
import automail.Robot;
import exceptions.BreakingFragileItemException;
import exceptions.ItemTooHeavyException;

public class MailPool implements IMailPool {

	private class Item {
		int destination;
		MailItem mailItem;

		public Item(MailItem mailItem) {
			destination = mailItem.getDestFloor();
			this.mailItem = mailItem;
		}
	}

	public class ItemComparator implements Comparator<Item> {
		@Override
		public int compare(Item i1, Item i2) {
			int order = 0;
			if (i1.destination > i2.destination) {  // Further before closer
				order = 1;
			} else if (i1.destination < i2.destination) {
				order = -1;
			}
			return order;
		}
	}

	private LinkedList<Item> pool;
	private LinkedList<Robot> robots;

	public MailPool(int nrobots){
		// Start empty
		pool = new LinkedList<Item>();
		robots = new LinkedList<Robot>();
	}

	public void addToPool(MailItem mailItem) {
		Item item = new Item(mailItem);
		pool.add(item);
		pool.sort(new ItemComparator());
	}

	@Override
	public void step() throws ItemTooHeavyException, BreakingFragileItemException {
		try{
			ListIterator<Robot> i = robots.listIterator();
			while (i.hasNext()) loadRobot(i);
		} catch (Exception e) {
            throw e;
        }
	}

	private void loadRobot(ListIterator<Robot> i) throws ItemTooHeavyException, BreakingFragileItemException {
		// Cast every robot to caution robot
		Robot robot = i.next();
		assert(robot.isEmpty());
		// System.out.printf("P: %3d%n", pool.size());
		ListIterator<Item> j = pool.listIterator();
		if (pool.size() > 0) {
			try {
				MailItem nextItem;
				// A robot can hold up to three mails
				for (int num=0; num<3; num++) {
					
					if (pool.size()>0) { nextItem = j.next().mailItem; } else {break; }

					if (nextItem.getFragile() && robot instanceof CautionRobot) {
						// Cast to Caution Robot only if fragile items appear
						if (((CautionRobot) robot).getArm() == null) {
							((CautionRobot) robot).addToArm(nextItem);
							j.remove();
							continue;
						} else {
							break;
						}
					}

					if (robot.getDeliveryItem() == null) {
						robot.addToHand(nextItem);
						j.remove();
						continue;
					}

					if (robot.getTube() == null) {
						robot.addToTube(nextItem);
						j.remove();
					}
				}
				robot.dispatch(); // send the robot off if it has any items to deliver
				i.remove(); // remove from mailPool queue
			} catch (Exception e) {
	            throw e;
	        }
		}
	}

	@Override
	public void registerWaiting(Robot robot) { // assumes won't be there already
		robots.add(robot);
	}

}
