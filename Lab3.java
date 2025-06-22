import java.util.Random;
import java.util.concurrent.Semaphore;

public class Lab3 {
    // Configuration
    final static int PORT0 = 0;
    final static int PORT1 = 1;
    final static int MAXLOAD = 5;

    public static void main(String args[]) {
        final int NUM_CARS = 10;
        int i;

        Ferry fer = new Ferry(PORT0, 10);

        Auto[] automobile = new Auto[NUM_CARS];
        for (i = 0; i < 7; i++) {
            automobile[i] = new Auto(i, PORT0, fer);
        }
        for (; i < NUM_CARS; i++) {
            automobile[i] = new Auto(i, PORT1, fer);
        }

        Ambulance ambulance = new Ambulance(PORT0, fer);

        /* Start the threads */
        fer.start(); // Start the ferry thread.
        for (i = 0; i < NUM_CARS; i++) {
            automobile[i].start(); // Start automobile threads
        }
        ambulance.start(); // Start the ambulance thread.

        try {
            fer.join();
        } catch (InterruptedException e) {} 
        
        System.out.println("\n=== Ferry stopped ===");
        for (i = 0; i < NUM_CARS; i++) {
            automobile[i].interrupt(); 
        }
        //stop ambulance thread
        ambulance.interrupt(); 
    }
}

class Auto extends Thread {
    private int id_auto;
    private int port;
    private Ferry fry;

    public Auto(int id, int prt, Ferry ferry) {
        this.id_auto = id;
        this.port = prt;
        this.fry = ferry;
    }

    public void run() {
        Semaphore semBoard;
        while (true) {
            System.out.printf("[Port %d] Auto %d arrives\n", port, id_auto);
            
            semBoard = (port == Lab3.PORT0) ? fry.semBoardPort0 : fry.semBoardPort1;

            // Board
            try {
                semBoard.acquire();
                System.out.printf("[Port %d] Auto %d boards (Load: %d)\n", 
                                port, id_auto, fry.getLoad()+1);
                fry.addLoad();
                
                if(fry.getLoad() == Lab3.MAXLOAD) {
                    System.out.println("Ferry is FULL");
                    fry.semDepart.release();
                } else {
                    semBoard.release();
                }
                
                port = 1 - port;
                semBoard = (port == Lab3.PORT0) ? fry.semBoardPort0 : fry.semBoardPort1;
                
                // Disembark
                fry.semDisembark.acquire();
                System.out.printf("[Port %d] Auto %d disembarks (Load: %d)\n", 
                                 port, id_auto, fry.getLoad()-1);
                fry.reduceLoad();
                
                if(fry.getLoad() == 0) {
                    System.out.println("Ferry is empty");
                    semBoard.release();
                } else {
                    fry.semDisembark.release();
                }
                
            } catch (InterruptedException e) { break; }
            
            if(isInterrupted()) break;
        }
        System.out.printf("Auto %d terminated\n", id_auto);
    }
}

class Ambulance extends Thread {
    private int port;
    private Ferry fry;

    public Ambulance(int prt, Ferry ferry) {
        this.port = prt;
        this.fry = ferry;
    }

    public void run() {
        Semaphore semBoard;
        while (true) {
            try { sleep((int) (1000*Math.random())); } 
            catch (Exception e) { break; }
            
            System.out.printf("\n[Port %d] AMBULANCE arrives\n", port);
            
            semBoard = (port == Lab3.PORT0) ? fry.semBoardPort0 : fry.semBoardPort1;

            try {
                semBoard.acquire();
                System.out.printf("[Port %d] AMBULANCE boards (Load: %d)\n", 
                                 port, fry.getLoad()+1);
                fry.addLoad();
                System.out.println("EMERGENCY DEPARTURE");
                fry.semDepart.release();
                
                // Arrive at next port
                port = 1 - port;
                semBoard = (port == Lab3.PORT0) ? fry.semBoardPort0 : fry.semBoardPort1;
                
                // Disembark
                fry.semDisembark.acquire();
                System.out.printf("[Port %d] AMBULANCE disembarks (Load: %d)\n", 
                                 port, fry.getLoad()-1);
                fry.reduceLoad();
                
                if(fry.getLoad() == 0) {
                    System.out.println("Ferry is EMPTY");
                    semBoard.release();
                } else {
                    fry.semDisembark.release();
                }
                
            } catch (InterruptedException e) { break; }
            
            if(isInterrupted()) break;
        }
        System.out.println("Ambulance terminates.");
    }
}

class Ferry extends Thread {
    private int port = 0; // Start at port 0
    private int load = 0; // Load is zero
    private int numCrossings; // number of crossings to execute
  
    public Semaphore semBoardPort0;  //semaphore for vehicle loading at port 0
	public Semaphore semBoardPort1;  //semaphore for vehicle loading at port 1
	public Semaphore semDisembark;  //semaphore for vehicles to disembark the ferry
	public Semaphore semDepart;    //semaphore for the departure of the ferry

    public Ferry(int prt, int nbtours) {
        this.port = prt;
        numCrossings = nbtours;
        //semaphores allow for first-come, first-serve access to board/disembark
        semBoardPort0 = new Semaphore(0, true); 
        semBoardPort1 = new Semaphore(0, true);
        semDisembark = new Semaphore(0, true);
        semDepart = new Semaphore(0, true);
    }

    public void run() {
        System.out.println("\n=== Ferry starts at port " + port + " ===\n");
        semBoardPort0.release();

        for (int i = 1; i <= numCrossings; i++) {
            semDepart.acquireUninterruptibly();
            
            System.out.printf("\n=== Crossing %d ===\n", i);
            System.out.println("Departure from port " + port + " with a load of " + load + " vehicles");
     
            
            port = 1 - port;
            try { sleep((int) (100 * Math.random())); } 
            catch (Exception e) {}
            
            System.out.println("Arrive at port " + port + " with a load of " + load + " vehicles");
            
            semDisembark.release();
        }
        System.out.println("\n=== Ferry completed all crossings ===");
    }

    // methodes to manipulate the load of the ferry
    public int getLoad() {
        return (load);
    }

    public void addLoad() {
        load = load + 1;
    }

    public void reduceLoad() {
        load = load - 1;
    }
    }