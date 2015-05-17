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
		// TODO id?
		koordinator.anmelden(0, m_name);

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
		} catch (NotFound e) {
			e.printStackTrace();
		} catch (CannotProceed e) {
			e.printStackTrace();
		} catch (InvalidName e) {
			e.printStackTrace();
		}
	}

	@Override
	public void rechnen(String prozessIdAbsender, int num) {
		// TODO Auto-generated method stub

	}

	@Override
	public void markerAuswerten(int seqNr, String prozessIdAbsender) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beenden(String prozessIdAbsender) {
		//TODO prozess id abfragen?
		running.release();
	}
}
