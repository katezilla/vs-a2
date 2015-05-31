package ggTProcess;

import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;

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
    private SynchronousQueue<IJob> m_jobs;
    private int calcZahl;
    private int delayZeit;
    private GgTProcess links;
    private String linksName;
    private GgTProcess rechts;
    private String rechtsName;
    private Monitor monitor;
    private Semaphore running;

    private Thread m_thread;
    protected Koordinator koordinator;

    GgTProcessImpl(final String name) {
        m_name = name;
        m_jobs = new SynchronousQueue<IJob>();
        running = new Semaphore(0);

        m_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean markerVonRechts = false;
                boolean markerVonLinks = false;
                boolean merkerTerminiere = false;
                int seqNr = -1;
                int alteSeqNr = -1;
                String absender;
                while (true) {
                    try {
                        running.acquire();
                        IJob job = m_jobs.take();
                        if (job instanceof Marker) {
                            Marker markJob = (Marker) job;
                            seqNr = markJob.getSeqNr();
                            absender = markJob.getProzessIdAbsender();
                            if (seqNr > alteSeqNr) {
                                markerVonRechts = false;
                                markerVonLinks = false;
                                merkerTerminiere = true;
                                alteSeqNr = markJob.getSeqNr();
                                links.markerAuswerten(seqNr, absender);
                                rechts.markerAuswerten(seqNr, absender);
                            }
                            if (absender == linksName) {
                                markerVonLinks = true;
                            }
                            if (absender == rechtsName) {
                                markerVonRechts = true;
                            }
                            if (markerVonLinks && markerVonRechts) {
                                koordinator.informieren(m_name, alteSeqNr,
                                        merkerTerminiere, calcZahl);
                            }
                            monitor.terminieren(m_name, absender,
                                    merkerTerminiere);
                        } else {
                            Calculation calcJob = (Calculation) job;
                            wait(delayZeit); // simulate advanced calculation
                            int newcalcZahl = calc(calcJob.getNum());
                            if (newcalcZahl != -1) {
                                links.rechnen(m_name, newcalcZahl);
                                rechts.rechnen(m_name, newcalcZahl);
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
    public void setStartwerte(String linkeProzessId, String rechteProzessId,
            int startwertMi, int delayZeit, String monitorId) {
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
        try {
            m_jobs.put(new Calculation(prozessIdAbsender, num)); // TODO: more
                                                                 // information
                                                                 // of this
                                                                 // time?
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void markerAuswerten(int seqNr, String prozessIdAbsender) {
        try {
            m_jobs.put(new Marker(prozessIdAbsender, seqNr)); // TODO: more
                                                              // information
                                                              // of this
                                                              // time?
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
        running.release();
    }
}
