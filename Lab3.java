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
    } catch (InterruptedException e) {} // Wait until ferry terminates.
    
    System.out.println("Ferry stopped.");
    // Stop other threads.
    for (i = 0; i < NUM_CARS; i++) {
      automobile[i].interrupt(); // Let's stop the auto threads.
    }
    ambulance.interrupt(); // Stop the ambulance thread.
  }
}

class Auto extends Thread { // Class for the auto threads.

  private int id_auto;
  private int port;
  private Ferry fry;

  public Auto(int id, int prt, Ferry ferry) {
    this.id_auto = id;
    this.port = prt;
    this.fry = ferry;
  }
  // modified code
  public void run() {
    try {
        while (true) {
            fry.ferryAtPort[port].acquire(); 
            fry.mutex.acquire();
            if (fry.canBoard(false)) {
                fry.boardVehicle(false);
                System.out.println("Auto " + id_auto + " boards the ferry at port " + port);
                fry.boardQueue.release();
                fry.mutex.release();

                fry.unboardQueue.acquire(); 
                System.out.println("Auto " + id_auto + " disembarks at port " + (1 - port));
                fry.unboardingDone.release();
                port = 1 - port;
                Thread.sleep(1000);
            } else {
                fry.mutex.release();
            }
        }
    } catch (InterruptedException e) {
        System.out.println("Auto " + id_auto + " terminated.");
    }
}
// modified code

class Ambulance extends Thread { // the Class for the Ambulance thread

  private int port;
  private Ferry fry;

  public Ambulance(int prt, Ferry ferry) {
    this.port = prt;
    this.fry = ferry;
  }

  public void run() {
    while (true) {
      // Attente
      try {
        sleep((int) (1000 * Math.random()));
      } catch (Exception e) {
        break;
      }
      System.out.println("Ambulance arrives at port " + port);

      // Board
      System.out.println("Ambulance boards the ferry at port " + port);
      fry.addLoad(); // increment the load

      // Arrive at the next port
      port = 1 - port;

      // Disembarkment
      System.out.println("Ambulance disembarks the ferry at port " + port);
      fry.reduceLoad(); // Reduce load

      // Terminate
      if (isInterrupted()) {
        break;
      }
    }
    System.out.println("Ambulance terminates.");
  }
}

class Ferry extends Thread { // The ferry Class

  private int port = 0; // Start at port 0
  private int load = 0; // Load is zero
  private int numCrossings; // number of crossings to execute
  
  // Semaphores

  public Semaphore semBoardPort0;  //semaphore for vehicle loading at port 0
	public Semaphore semBoardPort1;  //semaphore for vehicle loading at port 1
	public Semaphore semDisembark;  //semaphore for vehicles to disembark the ferry
	public Semaphore semDepart;    //semaphore for the departure of the ferry


  public Ferry(int prt, int nbtours) {
    this.port = prt;
    numCrossings = nbtours;

    //semaphores allow for first-come, first-serve access to board/disembark
    semBoardPort0 = new Semaphore(0,true); 
		semBoardPort1 = new Semaphore(0,true);
		semDisembark = new Semaphore(0,true);
		semDepart = new Semaphore(0,true);

  }

  public void run() {
    int i;
    System.out.println("Start at port " + port + " with a load of " + load + " vehicles");

    semBoardPort0.release();  //signal vehicles to begin boarding

    // numCrossings crossings in our day
    for (i = 0; i < numCrossings; i++) {

      semDepart.acquireUninterruptibly();  //wait for departure signal

      // The crossing
      System.out.println("Departure from port " + port + " with a load of " + load + " vehicles");
      System.out.println("Crossing " + i + " with a load of " + load + " vehicles");
      port = 1 - port;
      try {
        sleep((int) (100 * Math.random()));
      } catch (Exception e) {
      }
      // Arrive at port
      System.out.println("Arrive at port " + port + " with a load of " + load + " vehicles");
      
      // Disembarkment et loading

      semDisembark.release(); //release disembark semaphore to allow vehicles to leave the ferry

    }
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
