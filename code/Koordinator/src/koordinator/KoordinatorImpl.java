package koordinator;

import java.util.ArrayList;

public class KoordinatorImpl extends KoordinatorPOA {

    private static int STARTER_ID = 0;
    private static int PROZESS_ID = 1;
    
    /**
     * 
     */
    private ArrayList<String> starterList;
    private ArrayList<String> prozessList;

    @Override
    public void anmelden(int typId, String prozessId) {
        if (typId == STARTER_ID) {
            starterList.add(prozessId);
        } else if (typId == PROZESS_ID) {
            prozessList.add(prozessId);
        } else {
            System.err.println("Invalid process ID"); // TODO: error handling
        }
        //TODO: other functionality?
    }

    @Override
    public void informieren(String prozessId, int sequenzNr,
            boolean termStatus, int letzteZahl) {
        // TODO Auto-generated method stub

    }

    @Override
    public String[] getStarterIds() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void berechnen(String monitorId, int anzahlGgtLower,
            int anzahlGgtUpper, int delayZeitLower, int delayZeitUpper,
            int termAbfragePeriode, int gewuenschterGgt) {
        // TODO Auto-generated method stub

    }

    @Override
    public void beenden(String prozessIdAbsender) {
        // TODO Auto-generated method stub

    }

}
