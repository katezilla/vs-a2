package koordinator;

import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

public class KoordinatorMain {
    public static NamingContextExt nc;
    public static String name = "";
    public static String nsPort = "20000";
    public static String nsHost = "localhost";

    public static void main(String[] args) {

        try {

            for (int i = 0; i < args.length; ++i) {
                if (args[i].contains("--name=")) {
                    name = args[i].split("=")[1];
                } else if (args[i].contains("--nameserverport=")) {
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

            nc = NamingContextExtHelper.narrow(orb
                    .resolve_initial_references("NameService"));

            KoordinatorImpl obj = new KoordinatorImpl(name,nc);

            // register at the naming context
            org.omg.CORBA.Object ref = rootPoa.servant_to_reference(obj);
            NameComponent path[] = nc.to_name(name);
            nc.rebind(path, ref);

            System.out.println("Koordinator running");
            obj.run();

            // unregister at the naming context and shut down orb
            nc.unbind(path);
            Thread.sleep(1000);
            orb.shutdown(true);

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Koordinator destroyed");
    }
}
