package koordinator;

import ggTProcess.GgTProcess;
import ggTProcess.GgTProcessHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import org.omg.CosNaming.NamingContextExt;
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
	protected static boolean algorithmRunning;
	protected static boolean shutdownAlgorithm;
	private Semaphore running;
	private Semaphore startTermAlgorithm;
	private int currentSeqNr;
	protected int terminiert;
	protected int ergebnis;

	/**
     * 
     */
	private ArrayList<StarterData> starterList;
	private ArrayList<String> prozessList;
	private HashMap<String, Boolean> terminierteProzesse;
	private NamingContextExt nc;

	public KoordinatorImpl(String name, NamingContextExt nc) {
		this.name = name;
		this.nc = nc;
		this.starterList = new ArrayList<StarterData>();
		this.prozessList = new ArrayList<String>();
		KoordinatorImpl.algorithmRunning = false;
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
			} else {
				prozessList.add(prozessId);
				// add prozess to bridge between starter and prozess (1:n)
				starterList.get(starterPos).put(prozessId);
				startProzesse();
			}
		} else {
			System.err.println("Invalid process ID");
		}
	}

	private void startProzesse() {
		for (StarterData starter : starterList) {
			if (starter.getSize() != starter.getAnzahlProzesse()) {
				// TODO: timeout for if not all anz processes registered
				System.out.println("not all processes registered yet");
				return;
			}
		}
		System.out.println("all processes registered");
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
				process = GgTProcessHelper.narrow(this.nc
						.resolve_str(prozessId));
				process.setStartwerte(linkeId, rechteId, startwertMi,
						delayZeit, monitorId);
				System.out.println("setStartWerte for " + prozessId + " with "
						+ linkeId + "," + rechteId);
			} catch (NotFound | CannotProceed | InvalidName e) {
				e.printStackTrace();
			}
			startzahlen[i] = startwertMi;
		}
		// inform monitor
		String[] ring = new String[prozessList.size()];
		for (int i = 0; i < prozessList.size(); i++) {
			ring[i] = prozessList.get(i);
		}
		monitor.ring(ring);
		monitor.startzahlen(startzahlen);

		startThreeLowestProcesses(startzahlen);
		System.out.println("algorithm started");
		algorithmRunning = true;
		startTermAlgorithm.release(2); // 2, since there is a check before and
										// after the bool for shut down
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
				System.out.println("starting processes' algorithm");
				process = GgTProcessHelper.narrow(this.nc
						.resolve_str(prozessList.get(indexLowest3)));
				System.out.println("received process corba object");
				process.rechnen(name, lowest3);
				System.out.println("process " + prozessList.get(indexLowest3)
						+ " started");
				process = GgTProcessHelper.narrow(this.nc
						.resolve_str(prozessList.get(indexLowest2)));
				process.rechnen(name, lowest2);
				System.out.println("process " + prozessList.get(indexLowest2)
						+ " started");
				process = GgTProcessHelper.narrow(this.nc
						.resolve_str(prozessList.get(indexLowest1)));
				process.rechnen(name, lowest1);
				System.out.println("process " + prozessList.get(indexLowest1)
						+ " started");
			} catch (NotFound | CannotProceed | InvalidName e) {
				e.printStackTrace();
			}
		} else if (indexLowest2 != -1) { // start only two processes
			try {
				process = GgTProcessHelper.narrow(this.nc
						.resolve_str(prozessList.get(indexLowest2)));
				process.rechnen(name, lowest2);
				process = GgTProcessHelper.narrow(this.nc
						.resolve_str(prozessList.get(indexLowest1)));
				process.rechnen(name, lowest1);
			} catch (NotFound | CannotProceed | InvalidName e) {
				e.printStackTrace();
			}
		} else if (indexLowest1 != -1) { // start only one process
			try {
				process = GgTProcessHelper.narrow(this.nc
						.resolve_str(prozessList.get(indexLowest1)));
				process.rechnen(name, lowest1);
			} catch (NotFound | CannotProceed | InvalidName e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("Algorithm didn't start - no lowest indices.");
		}
	}

	@Override
	public synchronized void informieren(String prozessId, int sequenzNr,
			boolean termStatus, int letzteZahl) {
		// handle only current sequenzNumbers
		if (currentSeqNr == sequenzNr) {
			System.out.println("got new info from: " + prozessId + ","
					+ sequenzNr + "," + termStatus + "," + letzteZahl);
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
					ergebnis = letzteZahl;
					algorithmRunning = false;
					System.out.println("Calculation terminated. Result: "
							+ ergebnis);
					terminierteProzesse.clear();
				}
			}
		} else if (currentSeqNr < sequenzNr) {
			System.err
					.println("higher sequenz number received than current sequenz number!");
		}

	}

	@Override
	public String[] getStarterIds() {
		/*
		 * if (algorithmRunning) { System.out
		 * .println("Algorithm still running - no starter available"); String[]
		 * result = new String[0]; return result; }
		 */
		String ret[] = new String[starterList.size()];
		int idx = 0;
		for (; idx < starterList.size(); idx++) {
			ret[idx] = starterList.get(idx).getName();
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
		StarterData starterData = null;
		try {
			monitor = MonitorHelper.narrow(this.nc.resolve_str(monitorId));
			Starter starter;
			int anzahlProzesse;
			for (int i = 0; i < starterList.size(); i++) {
				starterData = starterList.get(i);
				starter = StarterHelper.narrow(this.nc.resolve_str(starterData
						.getName()));
				anzahlProzesse = getRandomBetween(anzahlGgtLower,
						anzahlGgtUpper);
				starter.setAnzahlProzesse(anzahlProzesse);
				starterData.setAnzahlProzesse(anzahlProzesse);
			}
		} catch (NotFound | CannotProceed | InvalidName e) {
			if (starterData != null) {
				System.out.println(starterData.getName());
			}
			e.printStackTrace();
		}
	}

	@Override
	public void beenden(String prozessIdAbsender) {
		Starter starter;
		for (StarterData starterData : starterList) {
			try {
				starter = StarterHelper.narrow(this.nc.resolve_str(starterData
						.getName()));
				starter.beenden(name);
			} catch (NotFound | CannotProceed | InvalidName e) {
				e.printStackTrace();
			}
		}
		System.out.println("Starter shut down, shutting down.");
		running.release();
	}

	public void run() {
		shutdownAlgorithm = false;
		Thread termAlgorithm = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					startTermAlgorithm.acquire();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				ArrayList<StarterData> removeFromProcesses = new ArrayList<StarterData>();
				while (!shutdownAlgorithm) {
					try {
						startTermAlgorithm.acquire();
						int prozessIndex;
						GgTProcess prozess;
						boolean algorithmRunning = KoordinatorImpl.algorithmRunning;
						while (algorithmRunning) {
							// terminierungs algorithm start
							prozessIndex = (int) (Math.random() * prozessList
									.size());
							try {
								prozess = GgTProcessHelper
										.narrow(KoordinatorImpl.this.nc
												.resolve_str(prozessList
														.get(prozessIndex)));
								synchronized (KoordinatorImpl.class) {
									currentSeqNr++;
									prozess.markerAuswerten(currentSeqNr, name);
								}
								System.out.println("sent marker: "
										+ prozessList.get(prozessIndex)
										+ currentSeqNr);
							} catch (NotFound | CannotProceed | InvalidName e) {
								e.printStackTrace();
							}
							synchronized (this) {
								wait(KoordinatorImpl.this.termAbfragePeriode);
							}
							synchronized (KoordinatorImpl.this) {
								algorithmRunning = KoordinatorImpl.algorithmRunning;
							}
						}
						// algorithm terminated
						System.out.println("sending result to monitor.");
						if (monitor != null) {
							System.out.println("sent result to monitor.");
							monitor.ergebnis(name, ergebnis);
						}
						Starter starter;

						for (StarterData starterData : starterList) {
							try {
								starter = StarterHelper
										.narrow(KoordinatorImpl.this.nc
												.resolve_str(starterData
														.getName()));
								starter.beendeProzesse(name);
								starterData.clear();
								starterData.setAnzahlProzesse(0);
							} catch (NotFound | CannotProceed | InvalidName e) {
								System.out.println("Starter "
										+ starterData.getName()
										+ " not accessible, removing from list.");
								removeFromProcesses.add(starterData);
							}
						}
						for (StarterData starterData : removeFromProcesses) {
							starterList.remove(starterData);
						}
						prozessList.clear();
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
		System.out.println("running acquired");
		synchronized (this) {
			shutdownAlgorithm = true;
			algorithmRunning = false;
		}
		startTermAlgorithm.release(2);
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
