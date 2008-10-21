/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id$
*/
package de.dal33t.powerfolder.net;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.message.KnownNodes;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.net.NetworkUtil;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Listens on a local port for incoming connections
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.41 $
 */
public class ConnectionListener extends PFComponent implements Runnable {

    //
    // constants
    //
    public static final int DEFAULT_PORT = 1337;

    // return constants from dyndns validation
    public static final int OK = 0; // validation succeeded
    public static final int CANNOT_RESOLVE = 1; // dyndns could not be resolved
    public static final int VALIDATION_FAILED = 2; // dyndns does not match the
    // local host

    private Thread myThread;
    private ServerSocket serverSocket;
    private InetSocketAddress myDyndns;
    private int port;
    private String interfaceAddress;
    private boolean hasIncomingConnection;

    public ConnectionListener(Controller controller, int port,
        String bindToInterface) throws ConnectionException
    {
        super(controller);
        if (port < 0) {
            port = DEFAULT_PORT;
        }
        this.port = port;
        this.hasIncomingConnection = false;
        this.interfaceAddress = bindToInterface;

        // check our own dyndns address
        String dns = ConfigurationEntry.DYNDNS_HOSTNAME
            .getValue(getController());
        String clidns = controller.getCommandLine() != null ? controller
            .getCommandLine().getOptionValue("d") : null;

        if (!StringUtils.isEmpty(clidns)) {
            // Overwrite dyndns entry by commandline server address
            dns = clidns;
        }
        
        // set the dyndns without any validations
        // assuming it has been validated on the pevious time
        // round when it was set.
        setMyDynDns(dns, false);

        // Open server socket
        openServerSocket(); // Port is first valid after this call
    }

    /**
     * Opens the serversocket
     * 
     * @throws ConnectionException
     *             if port is blocked
     */
    private void openServerSocket() throws ConnectionException {
        try {
            logFiner("Opening listener on port " + port);
            String bind = interfaceAddress;
            InetAddress bAddress = null;
            if (bind != null && !StringUtils.isBlank(bind)) {
                try {
                    bAddress = InetAddress.getByName(bind);
                } catch (UnknownHostException e) {
                    logInfo("Bad BIND address: " + bind);
                    bind = null;
                }
            }
            serverSocket = new ServerSocket(port,
                Constants.MAX_INCOMING_CONNECTIONS, bAddress);
        } catch (IOException e) {
            throw new ConnectionException(Translation.getTranslation(
                "dialog.unable_to_open_port", port + ""), e);
        }

        logInfo(
                "Listening for incoming connections on port "
                    + serverSocket.getLocalPort()
                    + (myDyndns != null
                        ? ", own dyndns address: " + myDyndns
                        : ""));
        // Force correct port setting
        port = serverSocket.getLocalPort();
    }

    /**
     * Answers if the server socket is opened
     * 
     * @return
     */
    private boolean isServerSocketOpen() {
        return serverSocket != null && !serverSocket.isClosed();
    }

    /**
     * parse entered dyndns and gets rid of any 'http://' found at the beginning
     * of it
     * 
     * @param newDns
     * @return string
     */
    private String parseString(String newDns) {

        if (newDns.startsWith("http://")) {
            int index = newDns.indexOf("//");
            newDns = newDns.substring(index + 2);
        }
        return newDns;
    }

    /**
     * get local networ interfaces.
     * 
     * @return an array of local inet addresses
     */
    private List<InetAddress> getNetworkAddresses() {
        Enumeration<NetworkInterface> networkInterfaces;
        ArrayList<NetworkInterface> getLocalNI = new ArrayList<NetworkInterface>();
        ArrayList<InetAddress> localNIAddrs = new ArrayList<InetAddress>();

        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            logSevere("Unable to get local network interfaces");
            logFiner("SocketException", e);
            return null;
        }

        while (networkInterfaces.hasMoreElements()) {
            getLocalNI.add(networkInterfaces.nextElement());
        }

        for (int i = 0; i < getLocalNI.size(); i++) {
            NetworkInterface ni = getLocalNI.get(i);

            Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                localNIAddrs.add(inetAddresses.nextElement());
            }
        }

        return localNIAddrs;
    }

    /**
     * Tries to set a new dyndns address.
     * 
     * @newDns new dyndns to set
     * @validate flag indicating whether to perform dyndns validtion or just to
     *           set it
     * @return OK if succeded, CANNOT_RESOLVE if dyndns could not be resolved
     *         and VALIDATION_FAILED if dyndns does not match the local host
     */
    public int setMyDynDns(String newDns, boolean validate) {
        logFine(
            "Setting own dns to " + newDns + ". was: "
                + (myDyndns != null ? myDyndns.getHostName() : ""));

        // FIXME Don't reset!!! If nothing has changed! CLEAN UP THIS MESS!
        if (validate) {
            // show wait message box to the user
            getController().getDynDnsManager().show(newDns);
        } else {
            if (myDyndns != null && myDyndns.getHostName().equals(newDns)) {
                // Not restetting supernode state
                logWarning("Not resetting supernode state");
                return OK;
            }
            logInfo("Resetting supernode state");
        }

        // Reset my setting
        myDyndns = null;
        getController().getMySelf().getInfo().isSupernode = false;

        if (!StringUtils.isBlank(newDns)) {
            logFiner("Resolving " + newDns);

            // parses the string in case it contains http://
            newDns = parseString(newDns).trim();

            myDyndns = new InetSocketAddress(newDns, port);

            if (myDyndns.isUnresolved()) {

                if (validate) {
                    getController().getDynDnsManager().close();
                    getController().getDynDnsManager().showWarningMsg(
                        CANNOT_RESOLVE, myDyndns.getHostName());
                }

                logWarning(
                    "Unable to resolve own dyndns address '" + newDns + "'");
                myDyndns = null;
                return CANNOT_RESOLVE;
            }

            if (validate) {
                logFiner("Validating " + newDns);

                InetAddress myDyndnsIP = myDyndns.getAddress(); // the entered
                // dyndns
                // address
                List<InetAddress> localIPs = getNetworkAddresses(); // list of
                // all
                // local host
                // IPs
                String strDyndnsIP = myDyndnsIP.getHostAddress(); // dyndns IP
                // address
                String externalIP = getController().getDynDnsManager()
                    .getIPviaHTTPCheckIP(); // internet IP of the local host

                boolean checkOK = false;

                // check if dyndns really matches the own host
                for (int i = 0; i < localIPs.size(); i++) {
                    InetAddress niAddrs = localIPs.get(i);

                    if (Util.compareIpAddresses(myDyndnsIP.getAddress(),
                        niAddrs.getAddress()))
                    {
                        checkOK = true;
                        break;
                    }
                }

                if (!checkOK) {
                    if (externalIP.equals(strDyndnsIP)) {
                        logFiner(
                            "DynDns matches with external IP " + newDns);
                        checkOK = true;
                    }
                }

                if (!checkOK) {
                    getController().getDynDnsManager().close();

                    logWarning(
                            "Own dyndns address "
                                + newDns
                                + " does not match any of the local network intergaces");
                    return VALIDATION_FAILED;
                }

                // close message box
                getController().getDynDnsManager().close();

                if (externalIP == "") {
                    logWarning("cannot determine the external IP of this host");
                    return VALIDATION_FAILED;
                }

                // check if dyndns really matches the external IP of this host
                boolean dyndnsMatchesExternalIP = externalIP
                    .equals(strDyndnsIP);
                boolean dyndnsMatchesLastUpdatedIP = externalIP
                    .equals(ConfigurationEntry.DYNDNS_LAST_UPDATED_IP
                        .getValue(getController()));
                if (!dyndnsMatchesExternalIP && !dyndnsMatchesLastUpdatedIP) {
                    // getController().getDynDnsManager().showWarningMsg(
                    // VALIDATION_FAILED, myDyndns.getHostName());
                    logWarning(
                        "Own dyndns address " + newDns
                            + " does not match the external IP of this host");
                    return VALIDATION_FAILED;
                }
            }
        }

        if (myDyndns != null) {
            logFiner(
                "Successfully set dyndns to " + myDyndns.getHostName());
        } else {
            logFine("Dyndns not set");
        }
        return OK;
    }

    /**
     * Starts the connection listener
     * 
     * @throws ConnectionException
     *             if port is blocked
     */
    public void start() throws ConnectionException {
        if (!isServerSocketOpen()) {
            // Open the server socket if required
            openServerSocket();
        }

        myThread = new Thread(this, "Listener on port "
            + serverSocket.getLocalPort());
        myThread.setPriority(Thread.MIN_PRIORITY);
        myThread.start();
        logFine("Started");
    }

    /**
     * Shuts the listener down
     */
    public void shutdown() {
        if (myThread != null) {
            myThread.interrupt();
        }

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logFiner(e.toString());
            }
        }
        logFine("Stopped");
    }

    /**
     * @return my dyndns entry if available otherwise <code>null</code>
     */
    public InetSocketAddress getMyDynDns() {
        return myDyndns;
    }

    /**
     * @return Address where incoming connects are possible. returns the own
     *         dyndns address if available
     */
    public InetSocketAddress getAddress() {
        return (myDyndns != null) ? myDyndns : (serverSocket == null)
            ? null
            : (InetSocketAddress) serverSocket.getLocalSocketAddress();
    }

    /**
     * @return Address where incoming connects are possible. returns the bound
     *         to address
     */
    public InetSocketAddress getLocalAddress() {
        return (serverSocket == null) ? null : (InetSocketAddress) serverSocket
            .getLocalSocketAddress();
    }

    /**
     * @return true if we have incoming connections
     */
    public boolean hasIncomingConnections() {
        return hasIncomingConnection;
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // accept a clients socket and add it to the connection pool
                if (isFiner()) {
                    logFiner(
                        "Listening for new connections on " + serverSocket);
                }
                Socket nodeSocket = serverSocket.accept();
                boolean inAllowedNetworkScope = true;
                if (getController().isLanOnly()) {
                    inAllowedNetworkScope = getController().getNodeManager()
                        .isOnLANorConfiguredOnLAN(nodeSocket.getInetAddress());
                }
                if (!inAllowedNetworkScope) {
                    nodeSocket.close();
                    continue;
                }
                NetworkUtil.setupSocket(nodeSocket);

                hasIncomingConnection = true;
                if (isFiner()) {
                    logFiner(
                        "Incoming connection from: "
                            + nodeSocket.getInetAddress() + ":"
                            + nodeSocket.getPort());
                }

                if (myDyndns != null
                    && !getController().getMySelf().getInfo().isSupernode)
                {
                    // ok, act as supernode
                    logFine("Acting as supernode on address " + myDyndns);
                    getController().getMySelf().getInfo().isSupernode = true;
                    getController().getMySelf().getInfo().setConnectAddress(
                        getAddress());
                    // Broadcast our new status, we want stats ;)
                    getController().getNodeManager().broadcastMessage(
                        new KnownNodes(getController().getMySelf().getInfo()));
                }

                // new member, accept it
                getController().getNodeManager().acceptConnectionAsynchron(
                    nodeSocket);
                //
                // if (!getController().isConsoleMode()) {
                // // Thottle connections a bit in non-console mode to save
                // // CPU.
                // Thread.sleep(getController().getWaitTime() / 10);
                // }
                // Thread.sleep(getController().getWaitTime() / 4);
                // Thread.sleep(50);
            } catch (SocketException e) {
                logFine(
                    "Listening socket on port " + serverSocket.getLocalPort()
                        + " closed");
                break;
            } catch (IOException e) {
                logSevere("Exception while accepting socket: " + e, e);
            } catch (RuntimeException e) {
                logSevere("Exception while accepting socket: " + e, e);
            }

            // catch (InterruptedException e) {
            // logFiner("Interrupted", e);
            // break;
            // }
        }
    }

    /**
     * Returns the port this listener is bound to.
     * 
     * @return
     */
    public int getPort() {
        return port;
    }
}