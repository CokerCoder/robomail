package automail;

public class DeliveryStat {
    private double totalWeight;
    private int numPackages;
    private double cautionWeight;
    private int numCautionPackages;
    private double totalTime;
    public DeliveryStat(){
        totalTime = 0.0;
        totalWeight = 0.0;
        cautionWeight = 0.0;
        numPackages = 0;
        numCautionPackages = 0;
    }

    public void addTotalTime() {
    	totalTime += 3;
    }
    
    public void addNumCautionPackages(){
        numCautionPackages += 1;
    }

    public void addNumPackages(){
        numPackages += 1;
    }

    public void addTotalWeight(double weight){
        totalWeight += weight;
    }

    public void addCautionWeight(double weight){
        cautionWeight += weight;
    }

    public void addTime(double time){
        totalTime += time;
    }

    public String toString(){
        return String.format("---------- Statistics ----------\nTotal Normal Weight: %f\nTotal Caution Weight: %f\nNumber of Normal Packages: %d\nNumber of Caution Packages: %d\nTotal Wrapping Time: %f\n",
                totalWeight, cautionWeight,numPackages, numCautionPackages, totalTime);
    }

    public int getNumCautionPackages() {
        return numCautionPackages;
    }

    public int getNumPackages() {
        return numPackages;
    }

    public double getCautionWeight() {
        return cautionWeight;
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    public double getTotalTime() {
        return totalTime;
    }
}
