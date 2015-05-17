package starter;


import java.util.Properties;

import koordinator.Koordinator;
import koordinator.KoordinatorHelper;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

public class StarterMain {
	public static NamingContextExt nc;
	public static String starterName = "";
	public static String nsPort = "20000";
	public static String nsHost = "localhost";
	public static String koordinator = "";

	public static void main(String[] args) {
		
		try {

			for (int i = 0; i < args.length; ++i) {
				if(args[i].contains("--name=")) {
					starterName = args[i].split("=")[1];
				} else if (args[i].contains("--nameserverport=")) {
					nsPort = args[i].split("=")[1];
				} else if (args[i].contains("--nameserverhost=")) {
					nsHost = args[i].split("=")[1];
				} else if (args[i].contains("--koordinator=")) {
					koordinator = args[i].split("=")[1];
				}
			}
			if(koordinator.isEmpty()) {
				return;
			}

			Properties props = new Properties();
			props.put("org.omg.CORBA.ORBInitialPort", nsPort);
			props.put("org.omg.CORBA.ORBInitialHost", nsHost);
			ORB orb = ORB.init(args, props);

			POA rootPoa = POAHelper.narrow(orb
					.resolve_initial_references("RootPOA"));
			rootPoa.the_POAManager().activate();

			nc = NamingContextExtHelper.narrow(orb
					.resolve_initial_references("NameService"));
			
			Koordinator koord = KoordinatorHelper.narrow(nc.resolve_str(koordinator));
			
			
			StarterImpl obj = new StarterImpl(starterName);
			obj.run(koord);

			System.out.println("ggT running");

			Thread.sleep(500);
			orb.shutdown(true);

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("ggT destroyed");
	}

}
