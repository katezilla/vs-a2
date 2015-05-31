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
    private GgTProcess rechts;
    private Monitor monitor;
    private Semaphore running;

    private Thread m_thread;

    GgTProcessImpl(final String name) {
        m_name = name;
        m_jobs = new SynchronousQueue<IJob>();
        running = new Semaphore(0);

        m_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        running.acquire();
                        IJob job = m_jobs.take();
                        if (job instanceof Marker) {
                            // TODO marker an nachbarn senden
                        } else {
                            // TODO rechnen
                        }
                    } catch (InterruptedException e) {

                        e.printStackTrace();
                    }
                }
            }
        });
        m_thread.start();
    }

    public void run(NamingContextExt nc, Koordinator koordinator) {
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
            links = GgTProcessHelper.narrow(GgTProcessMain.nc
                    .resolve_str(linkeProzessId));
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
        // TODO Auto-generated method stub

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
