package starter;

import ggTProcess.GgTProcessHelper;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import org.omg.CORBA.Object;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import koordinator.Koordinator;

public class StarterImpl extends StarterPOA {

    private static int STARTER_ID = 0;
    public String m_name;
    private Semaphore running;
    //private final String command = "java ./../ggTProzess/bin/GgTProcessMain";
    private final String cp_absolut = "D:/gitrepos/vs-a2/code/ggTProzess/bin";
    private final String command = "java -cp " + cp_absolut + " ggTProcess.GgTProcessMain";
    private String orb = " -ORBInitialHost localhost -ORBInitialPort 2000";
    private ArrayList<String> prozesse;

    public StarterImpl(final String name) {
        m_name = name;
        running = new Semaphore(0);
        prozesse = new ArrayList<String>();
    }

    public void run(Koordinator koord) {
        koord.anmelden(STARTER_ID, m_name);
        running = new Semaphore(0);
        try {
            running.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setAnzahlProzesse(int anzahl) {
    	System.out.println("setAnzahlProzesse(" + anzahl + ")");
        Runtime r = Runtime.getRuntime();
        String name;
        while (anzahl-- > 0) {
            name = ManagementFactory.getRuntimeMXBean().getName().split("@")[0]
                    + "-" + m_name + "-" + anzahl;
            prozesse.add(name);
            String arg = command + " --name=" + name + " --nameserverport="
                    + StarterMain.nsPort + " --nameserverhost="
                    + StarterMain.nsHost + " --koordinator="
                    + StarterMain.koordinator + " " + orb;
            try {
				r.exec(arg);
				System.out.println(arg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void beenden(String prozessIdAbsender) {
    	System.out.println("beenden(" + prozessIdAbsender + ")");
        if (prozessIdAbsender.startsWith("CLIENT")
                || prozessIdAbsender.startsWith("KOORD")) {
            System.out
                    .println("Received regular shutdown command, shutting down processes.");
        } else {
            System.out.println("Received irregular shutdown command by "
                    + prozessIdAbsender + ", shutting down processes.");
        }
        beendeProzesse("STARTER" + m_name);
        System.out.println("Processes shut down, shutting down.");
        running.release();
    }

    @Override
    public void beendeProzesse(String prozessIdAbsender) {
    	System.out.println("beendeProzesse(" + prozessIdAbsender + ")");
        if (prozessIdAbsender.startsWith("CLIENT")
                || prozessIdAbsender.startsWith("KOORD")) {
            System.out
                    .println("Received regular shutdown processes command, shutting down processes.");
        } else if (!prozessIdAbsender.startsWith("STARTER")) {
            System.out
                    .println("Received irregular shutdown processes command by "
                            + prozessIdAbsender + ", shutting down processes.");
        }
        for (int i = 0; i < prozesse.size(); i++) {
            try {
                GgTProcessHelper
                        .narrow(getCorbaObjectByString(prozesse.get(i)))
                        .beenden("STARTER" + m_name);
            } catch (NotFound | CannotProceed | InvalidName e) {
                e.printStackTrace();
            }
        }
    }

    private Object getCorbaObjectByString(String string) throws NotFound,
            CannotProceed, InvalidName {
        return StarterMain.nc.resolve_str(string);
    }
}
