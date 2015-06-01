package koordinator;

import ggTProcess.GgTProcess;
import ggTProcess.GgTProcessHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

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
    private int delayZeitLower;
    private int delayZeitUpper;
    private int termAbfragePeriode;
    private int gewuenschterGgt;
    private boolean algorithmRunning;
    protected boolean shutdownAlgorithm;
    private Semaphore running;
    private Semaphore startTermAlgorithm;
    private int currentSeqNr;
    protected int terminiert;

    /**
     * 
     */
    private ArrayList<StarterData> starterList;
    private ArrayList<String> prozessList;
    private HashMap<String, Boolean> terminierteProzesse;

    public KoordinatorImpl(String name) {
        this.name = name;
        this.starterList = new ArrayList<StarterData>();
        this.prozessList = new ArrayList<String>();
        this.algorithmRunning = false;
        running = new Semaphore(0);
        startTermAlgorithm = new Semaphore(0);
        currentSeqNr = 0;
        terminierteProzesse = new HashMap<String, Boolean>();
    }

    @Override
    public synchronized void anmelden(int typId, String prozessId) {
        if (typId == STARTER_ID) {
            starterList.add(new StarterData(prozessId));
            System.out
                    .println("Starter " + prozessId + " hat sich angemeldet.");
        } else if (typId == PROZESS_ID) {
            System.out.println("ggT-Prozess " + prozessId
                    + " hat sich angemeldet.");
            // search for belonging starter
            int starterPos = -1;
            for (int i = 0; i < starterList.size(); i++) {
                if (prozessId.startsWith(starterList.get(i).getName())) {
                    starterPos = i;
                    break;
                }
            }
            if (starterPos < 0) {
                System.err
                        .println("Invalid process ID beginning or no starter registered");
                // TODO: error handling
            } else {
                prozessList.add(prozessId);
                // add prozess to bridge between starter and prozess (1:n)
                starterList.get(starterPos).put(prozessId);
                startProzesse();
            }
        } else {
            System.err.println("Invalid process ID"); // TODO: error handling
        }
        // TODO: other functionality?
    }

    private void startProzesse() {
        for (StarterData starter : starterList) {
            if (starter.getSize() != starter.getAnzahlProzesse()) {
                // TODO: timeout for if not all anz processes registered
                return;
            }
        }
        // if this is being executed, all prozesses of all starters have
        // registered --> start algorithm
        Collections.shuffle(prozessList); // randomize order of the prozesses

        GgTProcess process;
        String prozessId;
        String linkeId;
        String rechteId;
        int startwertMi;
        int delayZeit;
        int prozessAnzahl = prozessList.size();
        int[] startzahlen = new int[prozessAnzahl];
        for (int i = 0; i < prozessAnzahl; i++) {
            prozessId = prozessList.get(i);
            linkeId = prozessList.get((i - 1 + prozessAnzahl) % prozessAnzahl);
            rechteId = prozessList.get((i + 1) % prozessAnzahl);
            startwertMi = gewuenschterGgt * ((int) (Math.random() * 99) + 1)
                    * ((int) (Math.random() * 99) + 1);
            delayZeit = getRandomBetween(delayZeitLower, delayZeitUpper);
            try {
                process = GgTProcessHelper.narrow(KoordinatorMain.nc
                        .resolve_str(prozessId));
                process.setStartwerte(linkeId, rechteId, startwertMi,
                        delayZeit, monitorId);
            } catch (NotFound | CannotProceed | InvalidName e) {
                e.printStackTrace();
            }
            startzahlen[i] = startwertMi;
        }
        // inform monitor
        monitor.ring((String[]) prozessList.toArray());
        monitor.startzahlen(startzahlen);

        startThreeLowestProcesses(startzahlen);
        algorithmRunning = true;
        startTermAlgorithm.release();
    }

    private void startThreeLowestProcesses(int[] startzahlen) {
        GgTProcess process;
        // start three lowest prozesse
        int lowest1 = Integer.MAX_VALUE;
        int lowest2 = Integer.MAX_VALUE;
        int lowest3 = Integer.MAX_VALUE;
        int indexLowest1 = -1;
        int indexLowest2 = -1;
        int indexLowest3 = -1;
        for (int i = 0; i < startzahlen.length; i++) {
            if (startzahlen[i] < lowest1) {
                lowest3 = lowest2;
                indexLowest3 = indexLowest2;
                lowest2 = lowest1;
                indexLowest2 = indexLowest1;
                lowest1 = startzahlen[i];
                indexLowest1 = i;
            } else if (startzahlen[i] < lowest2) {
                lowest3 = lowest2;
                indexLowest3 = indexLowest2;
                lowest2 = startzahlen[i];
                indexLowest2 = i;
            } else if (startzahlen[i] < lowest3) {
                lowest3 = startzahlen[i];
                indexLowest3 = i;
            }
        }
        if (indexLowest3 != -1) { // start all three processes
            try {
                process = GgTProcessHelper.narrow(KoordinatorMain.nc
                        .resolve_str(prozessList.get(indexLowest3)));
                process.rechnen(name, lowest3);
                process = GgTProcessHelper.narrow(KoordinatorMain.nc
                        .resolve_str(prozessList.get(indexLowest2)));
                process.rechnen(name, lowest2);
                process = GgTProcessHelper.narrow(KoordinatorMain.nc
                        .resolve_str(prozessList.get(indexLowest1)));
                process.rechnen(name, lowest1);
            } catch (NotFound | CannotProceed | InvalidName e) {
                e.printStackTrace();
            }
        } else if (indexLowest2 != -1) { // start only two processes
            try {
                process = GgTProcessHelper.narrow(KoordinatorMain.nc
                        .resolve_str(prozessList.get(indexLowest2)));
                process.rechnen(name, lowest2);
                process = GgTProcessHelper.narrow(KoordinatorMain.nc
                        .resolve_str(prozessList.get(indexLowest1)));
                process.rechnen(name, lowest1);
            } catch (NotFound | CannotProceed | InvalidName e) {
                e.printStackTrace();
            }
        } else if (indexLowest1 != -1) { // start only one process
            try {
                process = GgTProcessHelper.narrow(KoordinatorMain.nc
                        .resolve_str(prozessList.get(indexLowest1)));
                process.rechnen(name, lowest1);
            } catch (NotFound | CannotProceed | InvalidName e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Algorithm didn't start - no lowest indices.");
            // TODO: error handling
        }
    }

    @Override
    public synchronized void informieren(String prozessId, int sequenzNr,
            boolean termStatus, int letzteZahl) {
        // handle only current sequenzNumbers
        if (currentSeqNr == sequenzNr) {
            if (terminierteProzesse.containsKey(prozessId)) {
                terminierteProzesse.remove(prozessId);
                terminierteProzesse.put(prozessId, termStatus);
            } else { // add the prozess
                terminierteProzesse.put(prozessId, termStatus);
            }
            // check, if all prozesses are terminated
            if (terminierteProzesse.size() == prozessList.size()) {
                terminiert = 0;
                for (String key : terminierteProzesse.keySet()) {
                	if (terminierteProzesse.get(key)) {
                		terminiert += 1;
                	}
                }
                if (terminiert == prozessList.size()) {
                    // algorithmus terminated!
                    algorithmRunning = false;
                }
            }
        } else if (currentSeqNr < sequenzNr) {
            System.err
                    .println("higher sequenz number received than current sequenz number!");
        }

    }

    @Override
    public String[] getStarterIds() {
        /*if (algorithmRunning) {
            System.out
                    .println("Algorithm still running - no starter available");
            String[] result = new String[0];
            return result;
        }*/
        String ret[] = new String[starterList.size()];
        int idx = 0;
        for (StarterData s : starterList) {
        	ret[idx] = s.getName();
        }
        return ret;
    }

    @Override
    public void berechnen(String monitorId, int anzahlGgtLower,
            int anzahlGgtUpper, int delayZeitLower, int delayZeitUpper,
            int termAbfragePeriode, int gewuenschterGgt) {
        if (algorithmRunning) {
            System.err
                    .println("calculation ignored - Koordinator still running");
            return;
        }
        this.monitorId = monitorId;
        this.delayZeitLower = delayZeitLower;
        this.delayZeitUpper = delayZeitUpper;
        this.termAbfragePeriode = termAbfragePeriode;
        this.gewuenschterGgt = gewuenschterGgt;
        try {
            monitor = MonitorHelper.narrow(KoordinatorMain.nc
                    .resolve_str(monitorId));
            Starter starter;
            int anzahlProzesse;
            for (StarterData starterData : starterList) {
                starter = StarterHelper.narrow(KoordinatorMain.nc
                        .resolve_str(starterData.getName()));
                anzahlProzesse = getRandomBetween(anzahlGgtLower,
                        anzahlGgtUpper);
                starter.setAnzahlProzesse(anzahlProzesse);
                starterData.setAnzahlProzesse(anzahlProzesse);
            }
        } catch (NotFound | CannotProceed | InvalidName e) {
            e.printStackTrace();
        }
    }

    @Override
    public void beenden(String prozessIdAbsender) {
        Starter starter;
        for (StarterData starterData : starterList) {
            try {
                starter = StarterHelper.narrow(KoordinatorMain.nc
                        .resolve_str(starterData.getName()));
                starter.beenden(name);
            } catch (NotFound | CannotProceed | InvalidName e) {
                e.printStackTrace();
            }
        }
        //TODO: beenden self
    }

    public void run() {
        shutdownAlgorithm = false;
        Thread termAlgorithm = new Thread(new Runnable() {

            @Override
            public void run() {
                while (!shutdownAlgorithm) {
                    try {
                        startTermAlgorithm.acquire();
                        int prozessIndex;
                        GgTProcess prozess;
                        boolean algorithmRunning = true;
                        while (algorithmRunning) {
                            // terminierungs algorithm start
                            prozessIndex = (int) (Math.random() * prozessList
                                    .size());
                            try {
                                prozess = GgTProcessHelper
                                        .narrow(KoordinatorMain.nc
                                                .resolve_str(prozessList
                                                        .get(prozessIndex)));
                                prozess.markerAuswerten(currentSeqNr, name);
                                synchronized (KoordinatorImpl.class) {
                                    currentSeqNr++;
                                }
                            } catch (NotFound | CannotProceed | InvalidName e) {
                                e.printStackTrace();
                            }
                            wait(termAbfragePeriode);
                            synchronized (KoordinatorImpl.class) {
                                algorithmRunning = KoordinatorImpl.this.algorithmRunning;
                            }
                        }
                        Starter starter;
                        for (StarterData starterData : starterList) {
                            try {
                                starter = StarterHelper
                                        .narrow(KoordinatorMain.nc
                                                .resolve_str(starterData
                                                        .getName()));
                                starter.beendeProzesse(name);
                            } catch (NotFound | CannotProceed | InvalidName e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        try {
            termAlgorithm.start();
            running.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        shutdownAlgorithm = true;
        try {
            termAlgorithm.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
