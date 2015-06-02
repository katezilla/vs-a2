package client;

import java.util.Properties;

import javax.swing.JOptionPane;

import koordinator.Koordinator;
import koordinator.KoordinatorHelper;
import monitor.Monitor;
import monitor.MonitorHelper;

import org.omg.CORBA.ORB;
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

			Thread.sleep(1000);
			orb.shutdown(true);

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Client destroyed");
	}

	private static void run(NamingContextExt nc) throws NotFound,
			CannotProceed, InvalidName {
		String nameKoordinator = "KOORD_jk";
		String nameMonitor = "MONITOR";
		int minProzesse = 3;
		int maxProzesse = 5;
		int minDelay = 10;
		int maxDelay = 15;
		int termAbfragePeriode = 2000;
		int gewuenschteGGT = 1;
		Monitor monitor; // TODO: decide reason..
		Koordinator koordinator;
		boolean running = true;
		String command;

		JOptionPane.getRootFrame().setAlwaysOnTop(true);

		while (running) {
			// input request
			command = JOptionPane.showInputDialog(null,
					"Bitte geben Sie ein Kommando ein:");
			if (command != null) {
				switch (command) {
				case "berechnen":
					String tempInput = JOptionPane.showInputDialog(null,
							"Bitte geben Sie Koordinator-namen ein:");
					nameKoordinator = (tempInput.startsWith("d")) ? nameKoordinator
							: tempInput;
					tempInput = null;
					tempInput = JOptionPane.showInputDialog(null,
							"Bitte geben Sie Monitor-namen ein:");
					nameMonitor = (tempInput.startsWith("d")) ? nameMonitor
							: tempInput;
					tempInput = null;
					int tempIntInput = 0;
					try {
						tempIntInput = Integer
								.parseInt(JOptionPane
										.showInputDialog(null,
												"Bitte geben sie die minimale Anzahl der ggT-Prozesse ein:"));
					} catch (NumberFormatException e) {
					}
					minProzesse = (tempIntInput == 0) ? minProzesse
							: tempIntInput;
					try {
						tempIntInput = Integer
								.parseInt(JOptionPane
										.showInputDialog(null,
												"Bitte geben sie die maximale Anzahl der ggT-Prozesse ein:"));
					} catch (NumberFormatException e) {
					}
					maxProzesse = (tempIntInput == 0) ? maxProzesse
							: tempIntInput;
					try {
						tempIntInput = Integer
								.parseInt(JOptionPane
										.showInputDialog(null,
												"Bitte geben sie die minimale Verzoegerung ein:"));
					} catch (NumberFormatException e) {
					}
					minDelay = (tempIntInput == 0) ? minDelay : tempIntInput;
					try {
						tempIntInput = Integer
								.parseInt(JOptionPane
										.showInputDialog(null,
												"Bitte geben sie die maximale Verzoegerung ein:"));
					} catch (NumberFormatException e) {
					}
					maxDelay = (tempIntInput == 0) ? maxDelay : tempIntInput;
					try {
						tempIntInput = Integer
								.parseInt(JOptionPane
										.showInputDialog(null,
												"Bitte geben sie die Terminierungsabfrageperiode (ms) ein:"));
					} catch (NumberFormatException e) {
					}
					termAbfragePeriode = (tempIntInput == 0) ? termAbfragePeriode
							: tempIntInput;
					try {
						tempIntInput = Integer
								.parseInt(JOptionPane
										.showInputDialog(null,
												"Bitte geben sie den gewuenschten ggT ein:"));
					} catch (NumberFormatException e) {
					}
					gewuenschteGGT = (tempIntInput == 0) ? gewuenschteGGT
							: tempIntInput;

					// execute input
					try {
						System.out.println("Verbinde mit Monitor: "
								+ nameMonitor);
						monitor = MonitorHelper.narrow(nc
								.resolve_str(nameMonitor));
						System.out.println("Verbinde mit Koordinator: "
								+ nameKoordinator);
						koordinator = KoordinatorHelper.narrow(nc
								.resolve_str(nameKoordinator));

						if (koordinator.getStarterIds().length > 0) {
							koordinator.berechnen(nameMonitor, minProzesse,
									maxProzesse, minDelay, maxDelay,
									termAbfragePeriode, gewuenschteGGT);
						} else { // no starter registered at koordinator,
									// therefore
									// can't execute
							System.err
									.println("Kann nicht berechnet werden, der Koordinator kennt keinen Starter!");
						}

					} catch (NotFound | CannotProceed | InvalidName e) {
						e.printStackTrace();
					}
					break;
				case "r":
					// execute input
					System.out.println("Verbinde mit Monitor: " + nameMonitor);
					monitor = MonitorHelper.narrow(nc.resolve_str(nameMonitor));
					System.out.println("Verbinde mit Koordinator: "
							+ nameKoordinator);
					koordinator = KoordinatorHelper.narrow(nc
							.resolve_str(nameKoordinator));

					if (koordinator.getStarterIds().length > 0) {
						koordinator.berechnen(nameMonitor, minProzesse,
								maxProzesse, minDelay, maxDelay,
								termAbfragePeriode, gewuenschteGGT);
					} else { // no starter registered at koordinator, therefore
								// can't execute
						System.err
								.println("Kann nicht berechnet werden, der Koordinator kennt keinen Starter!");
					}
					break;
				case "beenden":
					tempInput = JOptionPane.showInputDialog(null,
							"Bitte geben Sie Koordinator-namen ein:");
					nameKoordinator = (tempInput == null) ? nameKoordinator
							: tempInput;
					koordinator = KoordinatorHelper.narrow(nc
							.resolve_str(nameKoordinator));
					koordinator.beenden("CLIENT_");
					running = false;
					System.out.println("System wird beendet");
					break;
				default:
					System.err
							.println("Sorry, ich habe Sie nicht verstanden, bitte geben Sie eines der Kommandos ein: berechnen, beenden, r(recalc)");
				}
			}// if command != null
		}// while running
	}

}
