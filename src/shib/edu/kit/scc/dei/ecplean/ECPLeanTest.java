package edu.kit.scc.dei.ecplean;

import java.net.URI;

public class ECPLeanTest {

    public static void main(String[] args) throws Exception {
        String idp = "";
        String sp = "";
        String un = "";
        String pw = ""; 

        if (args.length < 4) {
            System.err.println("Please specify command line options");
            return;
        }
        if (args.length >= 1) {
            sp = args[0];
        }
        if (args.length >= 2) {
            idp = args[1];
        }
        if (args.length >= 3) {
            un = args[2];
        }
        if (args.length >= 4) {
            pw = args[3];
        }

        System.out.println();
        System.out.println();
        System.out.println("USING:");
        System.out.println("Service Provider URL : " + sp);
        System.out.println("Identity Provider URL: " + idp);
        System.out.println("Username: " + un);
        System.out.println("Password: " + pw);
        System.out.println();
        System.out.println();

        ECPAuthenticator auth = new ECPAuthenticator(un, pw, new URI(idp),
            new URI(sp));
        String email = auth.authenticate();
        System.out.println("Email: " + email);
    }
}
