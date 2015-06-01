package ggTProcess;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

import koordinator.Koordinator;
import monitor.Monitor;
import monitor.MonitorHelper;

import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;

public class GgTProcessImpl extends GgTProcessPOA {

    private static int PROZESS_ID = 1;
    private String m_name;
    private Queue<IJob> m_jobs;
    private int calcZahl;
    private int delayZeit;
    private GgTProcess links;
    private String linksName;
    private GgTProcess rechts;
    private String rechtsName;
    private Monitor monitor;
    private Semaphore running;
    private Semaphore newJob;

    private Thread m_thread;
    protected Koordinator koordinator;
    protected volatile boolean runningBool;
    protected String koordinatorName;

    GgTProcessImpl(final String name, String koordinatorName) {
        m_name = name;
        this.koordinatorName = koordinatorName;
        m_jobs = new LinkedList<IJob>();
        running = new Semaphore(0);
        runningBool = true;
        newJob = new Semaphore(0, true);

        m_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean markerVonRechts = false;
                boolean markerVonLinks = false;
                boolean merkerTerminiere = false;
                int seqNr = -1;
                int alteSeqNr = -1;
                String absender;
                int tries = 0;
                IJob job;
                while (runningBool) {
                    try {
                        newJob.acquire();
                        tries = 0;
                        do {
                            job = m_jobs.poll();
                            if (tries > 0) {
                                System.out.println("received null job");
                                synchronized (this) {
                                    wait(10);
                                }
                            }
                            tries++;
                        } while (job == null && tries <= 10 && runningBool);

                        if (job instanceof Marker) {
                            Marker markJob = (Marker) job;
                            seqNr = markJob.getSeqNr();
                            absender = markJob.getProzessIdAbsender();
                            if (seqNr > alteSeqNr) {
                                markerVonRechts = false;
                                markerVonLinks = false;
                                merkerTerminiere = true;
                                alteSeqNr = seqNr;
                                links.markerAuswerten(seqNr, m_name);
                                rechts.markerAuswerten(seqNr, m_name);
                            }
                            if (absender.equals(linksName)) {
                                markerVonLinks = true;
                            }
                            if (absender.equals(rechtsName)) {
                                markerVonRechts = true;
                            }
                            if (markerVonLinks && markerVonRechts) {
                                koordinator.informieren(m_name, alteSeqNr,
                                        merkerTerminiere, calcZahl);
                            }
                            monitor.terminieren(m_name, absender,
                                    merkerTerminiere);
                        } else if (job instanceof Calculation) {
                            Calculation calcJob = (Calculation) job;
                            System.out.println("job: " + calcJob.getNum() + ","
                                    + calcJob.getProzessIdAbsender() + ",seq"
                                    + seqNr);
                            synchronized (this) {
                                wait(delayZeit); // simulate advanced
                                                 // calculation
                            }
                            int newcalcZahl = calc(calcJob.getNum());
                            if (newcalcZahl != -1
                                    || calcJob.getProzessIdAbsender().equals(
                                            koordinatorName)) {
                                links.rechnen(m_name, calcZahl);
                                rechts.rechnen(m_name, calcZahl);
                                merkerTerminiere = false;
                            }
                            monitor.rechnen(m_name,
                                    calcJob.getProzessIdAbsender(),
                                    calcJob.getNum());
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        m_thread.start();
    }

    protected synchronized int calc(int num) {
        if (num < calcZahl) {
            calcZahl = ((calcZahl - 1) % num) + 1;
            return calcZahl;
        } else {
            return -1;
        }
    }

    public void run(NamingContextExt nc, Koordinator koordinator) {
        this.koordinator = koordinator;
        koordinator.anmelden(PROZESS_ID, m_name);
        try {
            running.acquire();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

    }

    @Override
    public synchronized void setStartwerte(String linkeProzessId,
            String rechteProzessId, int startwertMi, int delayZeit,
            String monitorId) {
        calcZahl = startwertMi;
        this.delayZeit = delayZeit;
        try {
            linksName = linkeProzessId;
            links = GgTProcessHelper.narrow(GgTProcessMain.nc
                    .resolve_str(linkeProzessId));
            rechtsName = rechteProzessId;
            rechts = GgTProcessHelper.narrow(GgTProcessMain.nc
                    .resolve_str(rechteProzessId));
            monitor = MonitorHelper.narrow(GgTProcessMain.nc
                    .resolve_str(monitorId));
        } catch (NotFound | CannotProceed | InvalidName e) {
            e.printStackTrace();
        }
    }

    @Override
    public void rechnen(String prozessIdAbsender, int num) {
        m_jobs.add(new Calculation(prozessIdAbsender, num));
        newJob.release();
    }

    @Override
    public void markerAuswerten(int seqNr, String prozessIdAbsender) {
        m_jobs.add(new Marker(prozessIdAbsender, seqNr));
        newJob.release();
    }

    @Override
    public void beenden(String prozessIdAbsender) {
        if (prozessIdAbsender.startsWith("STARTER")) {
            System.out
                    .println("Received regular shutdown command, shutting down.");
        } else {
            System.out
                    .println("Received irregular shutdown processes command by "
                            + prozessIdAbsender + ", shutting down.");
        }
        runningBool = false;
        newJob.release();
        try {
            m_thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        running.release();
    }
}
