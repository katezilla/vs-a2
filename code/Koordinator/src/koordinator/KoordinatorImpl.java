package koordinator;

import java.util.ArrayList;

import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import starter.Starter;
import starter.StarterHelper;
import monitor.Monitor;
import monitor.MonitorHelper;

public class KoordinatorImpl extends KoordinatorPOA {

    private final static int STARTER_ID = 0;
    private final static int PROZESS_ID = 1;
    private String name;
    private String monitorId;
    private Monitor monitor;

    /**
     * 
     */
    private ArrayList<String> starterList;
    private ArrayList<String> prozessList;

    public KoordinatorImpl(String name) {
        this.name = name;
        this.starterList = new ArrayList<String>();
        this.prozessList = new ArrayList<String>();
    }

    @Override
    public void anmelden(int typId, String prozessId) {
        if (typId == STARTER_ID) {
            starterList.add(prozessId);
            System.out
                    .println("Starter " + prozessId + " hat sich angemeldet.");
        } else if (typId == PROZESS_ID) {
            prozessList.add(prozessId);
            System.out.println("ggT-Prozess " + prozessId
                    + " hat sich angemeldet.");
            // TODO: add to bridge between starter and prozess (1:n)
        } else {
            System.err.println("Invalid process ID"); // TODO: error handling
        }
        // TODO: other functionality?
    }

    @Override
    public void informieren(String prozessId, int sequenzNr,
            boolean termStatus, int letzteZahl) {
        // TODO Auto-generated method stub

    }

    @Override
    public String[] getStarterIds() {
        return (String[]) starterList.toArray();
    }

    @Override
    public void berechnen(String monitorId, int anzahlGgtLower,
            int anzahlGgtUpper, int delayZeitLower, int delayZeitUpper,
            int termAbfragePeriode, int gewuenschterGgt) {
        try {
            this.monitorId = monitorId;
            monitor = MonitorHelper.narrow(KoordinatorMain.nc
                    .resolve_str(monitorId));
            Starter starter;
            int anzahlProzesse;
            for (String starterName : starterList) {
                starter = StarterHelper.narrow(KoordinatorMain.nc
                        .resolve_str(starterName));
                anzahlProzesse = getRandomBetween(anzahlGgtLower,
                        anzahlGgtUpper);
                starter.setAnzahlProzesse(anzahlProzesse);
                // TODO: remember anzahl prozesse for each starter for later
                // check if started?
            }
        } catch (NotFound | CannotProceed | InvalidName e) {
            e.printStackTrace();
        }
    }

    @Override
    public void beenden(String prozessIdAbsender) {
        // TODO Auto-generated method stub

    }

    public void run() {
        // TODO Auto-generated method stub

    }

    /**
     * function to get a random int between lower and upper
     * 
     * @param lower
     * @param upper
     * @return the random int between the two ints
     */
    private int getRandomBetween(int lower, int upper) {
        return lower + (int) (Math.random() * (upper - lower));
    }
}
