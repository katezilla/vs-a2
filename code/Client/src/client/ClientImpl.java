package client;

import java.util.Properties;

import javax.swing.JOptionPane;

import koordinator.Koordinator;
import koordinator.KoordinatorHelper;
import monitor.Monitor;
import monitor.MonitorHelper;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

public class ClientImpl extends ClientPOA {

	public static void main(String[] args) {
		try {

			String nsPort = "20000";
			String nsHost = "localhost";
			for (int i = 0; i < args.length; ++i) {
				if (args[i].contains("--nameserverport=")) {
					nsPort = args[i].split("=")[1];
				} else if (args[i].contains("--nameserverhost=")) {
					nsHost = args[i].split("=")[1];
				}
			}

			Properties props = new Properties();
			props.put("org.omg.CORBA.ORBInitialPort", nsPort);
			props.put("org.omg.CORBA.ORBInitialHost", nsHost);
			ORB orb = ORB.init(args, props);

			POA rootPoa = POAHelper.narrow(orb
					.resolve_initial_references("RootPOA"));
			rootPoa.the_POAManager().activate();

			NamingContextExt nc = NamingContextExtHelper.narrow(orb
					.resolve_initial_references("NameService"));

			run(nc);

			System.out.println("Client running");

			orb.shutdown(true);
			Thread.sleep(500);

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Client destroyed");
	}

	private static void run(NamingContextExt nc) throws NotFound,
			CannotProceed, InvalidName {
		String nameKoordinator = "Koordinator";
		String nameMonitor = "Monitor";
		int minProzesse = 0;
		int maxProzesse = 1;
		int minDelay = 0;
		int maxDelay = 1;
		int termAbfragePeriode = 100;
		int gewuenschteGGT = 1;
		Monitor monitor;
		Koordinator koordinator;
		boolean running = true;
		String command;

		while (running) {
			// input request
			command = JOptionPane.showInputDialog(null,
					"Bitte geben Sie ein Kommando ein:");
			switch (command) {
			case "berechnen":
				String tempInput = JOptionPane.showInputDialog(null,
						"Bitte geben Sie Koordinator-namen ein:");
				nameKoordinator = (tempInput == null) ? nameKoordinator
						: tempInput;
				tempInput = null;
				tempInput = JOptionPane.showInputDialog(null,
						"Bitte geben Sie Monitor-namen ein:");
				nameMonitor = (tempInput == null) ? nameMonitor : tempInput;
				tempInput = null;
				int tempIntInput = Integer
						.parseInt(JOptionPane
								.showInputDialog(null,
										"Bitte geben sie die minimale Anzahl der ggT-Prozesse ein:"));
				minProzesse = (tempIntInput == 0) ? minProzesse : tempIntInput;
				tempIntInput = Integer
						.parseInt(JOptionPane
								.showInputDialog(null,
										"Bitte geben sie die maximale Anzahl der ggT-Prozesse ein:"));
				maxProzesse = (tempIntInput == 0) ? maxProzesse : tempIntInput;
				tempIntInput = Integer.parseInt(JOptionPane.showInputDialog(
						null, "Bitte geben sie die minimale Verzögerung ein:"));
				minDelay = (tempIntInput == 0) ? minDelay : tempIntInput;
				tempIntInput = Integer.parseInt(JOptionPane.showInputDialog(
						null, "Bitte geben sie die maximale Verzögerung ein:"));
				maxDelay = (tempIntInput == 0) ? maxDelay : tempIntInput;
				tempIntInput = Integer
						.parseInt(JOptionPane
								.showInputDialog(null,
										"Bitte geben sie die Terminierungsabfrageperiode (ms) ein:"));
				termAbfragePeriode = (tempIntInput == 0) ? termAbfragePeriode
						: tempIntInput;
				tempIntInput = Integer.parseInt(JOptionPane.showInputDialog(
						null, "Bitte geben sie den gewuenschten ggT ein:"));
				gewuenschteGGT = (tempIntInput == 0) ? gewuenschteGGT
						: tempIntInput;

				// execute input
				monitor = MonitorHelper.narrow(nc.resolve_str(nameMonitor));
				koordinator = KoordinatorHelper.narrow(nc
						.resolve_str(nameKoordinator));
				koordinator.berechnen(nameMonitor, minProzesse, maxProzesse,
						minDelay, maxDelay, termAbfragePeriode, gewuenschteGGT);
				break;
			case "beenden":
				running = false;
				System.out.println("System wird beendet");
				break;
			default:
				System.err
						.println("Sorry, ich habe Sie nicht verstanden, bitte geben Sie eines der Kommandos ein: berechnen, beenden");
			}
		}
	}

}
