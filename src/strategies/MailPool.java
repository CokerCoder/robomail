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
		CautionRobot robot = (CautionRobot) i.next();
		assert(robot.isEmpty());
		// System.out.printf("P: %3d%n", pool.size());
		ListIterator<Item> j = pool.listIterator();
		if (pool.size() > 0) {
			try {
				MailItem nextItem = j.next().mailItem;

				// If it's a fragile item then add to the special arm
				if (nextItem.getFragile()) {
					robot.addToArm(nextItem);
					j.remove();

					if (pool.size() > 0) {
						nextItem = j.next().mailItem;
						// Cannot carry more than one fragile item at a time
						if (nextItem.getFragile()) {
							robot.dispatch();
							i.remove();
							return;
						}
					}
					else {
						robot.dispatch();
						i.remove();
						return;
					}
				}

				robot.addToHand(nextItem); // hand first as we want higher priority delivered first
				j.remove();

				if (pool.size() > 0) {
					nextItem = j.next().mailItem;
					// Cannot carry more than one fragile item at a time
					if (nextItem.getFragile()) {
						robot.dispatch();
						i.remove();
						return;
					}
				} else {
					robot.dispatch();
					i.remove();
					return;
				}

				robot.addToTube(nextItem);
				j.remove();
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
