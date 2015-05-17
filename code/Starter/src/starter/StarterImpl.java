package starter;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import koordinator.Koordinator;

public class StarterImpl extends StarterPOA{
	public String m_name;
	private Semaphore running;
	private final String command = "java ./../ggTProzess/bin/GgTProcessMain";
	private ArrayList<String> prozesse;
	
	
	public StarterImpl(final String name) {
		m_name = name;
		running = new Semaphore(0);
		prozesse = new ArrayList<String>();
		
	}

	public void run(Koordinator koord) {
		// TODO id?
		koord.anmelden(0, m_name);
		running = new Semaphore(0);
		while(true) {
			try {
				running.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void setAnzahlProzesse(int anzahl) {
		Runtime r = Runtime.getRuntime();
		String name;
		while(anzahl-- >0) {
			name = ManagementFactory.getRuntimeMXBean().getName().split("@")[0] +"-" + m_name + "-" + anzahl;
			prozesse.add(name);
			String arg = command + " --name=" +  name + " --nameserverport=" 
			           + StarterMain.nsPort + " --nameserverhost=" + StarterMain.nsHost
			           + " --koordinator=" + StarterMain.koordinator;
			try {
				r.exec(arg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

	@Override
	public void beenden(String prozessIdAbsender) {
		running.release();
		
	}

	@Override
	public void beendeProzesse(String prozessIdAbsender) {
		//TODO prozess id abfragen?
		
	}

}
