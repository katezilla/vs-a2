package ggTProcess;

import java.util.Properties;

import koordinator.Koordinator;
import koordinator.KoordinatorHelper;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

public class GgTProcessMain {
	public static NamingContextExt nc;

	public static void main(String[] args) {
		
		try {
			String name = "";
			String nsPort = "20000";
			String nsHost = "localhost";
			String koordinator = "";
			for (int i = 0; i < args.length; ++i) {
				if(args[i].contains("--name=")) {
					name = args[i].split("=")[1];
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
			
			//String our_name = "ggT:" + java.net.InetAddress.getLocalHost().getCanonicalHostName() 
			//         + ":" + ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
			
			
			GgTProcessImpl obj = new GgTProcessImpl(name);
			
			// register at the naming context
            org.omg.CORBA.Object ref = rootPoa.servant_to_reference(obj);
            NameComponent path[] = nc.to_name(name);
            nc.rebind(path, ref);

            System.out.println("ggT running");
            obj.run(nc, koord);

            // unregister at the naming context and shut down orb
            nc.unbind(path);
            Thread.sleep(500);
			orb.shutdown(true);

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("ggT destroyed");
	}

}
