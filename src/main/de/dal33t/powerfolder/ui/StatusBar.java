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
package de.dal33t.powerfolder.ui;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.net.ConnectionListener;
import de.dal33t.powerfolder.ui.action.SyncAllFoldersAction;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.ComplexComponentFactory;
import de.dal33t.powerfolder.util.ui.LimitedConnectivityChecker;
import de.dal33t.powerfolder.util.ui.UIPanel;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The status bar on the lower side of the main window.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class StatusBar extends PFUIComponent implements UIPanel {

    private static final int UNKNOWN = -1;
    private static final int DISABLED = 0;
    private static final int CONNECTED = 1;
    private static final int DISCONNECTED = 2;

    private Component comp;

    /** Online state info field */
    private JLabel onlineStateInfo;
    private JLabel limitedConnectivityLabel;
    private JLabel syncLabel;
    private JLabel upStats;
    private JLabel downStats;
    private JLabel portLabel;
    private JButton aboutButton;
    private JButton preferencesButton;
    private JLabel spacerLabel;
    private JLabel syncAllLabel;

    /** Connection state */
    private final AtomicInteger state = new AtomicInteger(UNKNOWN);

    protected StatusBar(Controller controller) {
        super(controller);
    }

    public Component getUIComponent() {

        if (comp == null) {

            boolean showPort = ConfigurationEntry.NET_BIND_RANDOM_PORT
                .getValueBoolean(getController())
                && getController().getConnectionListener().getPort() !=
                    ConnectionListener.DEFAULT_PORT;
            initComponents();

            CellConstraints cc = new CellConstraints();

            // Upper section

            FormLayout upperLayout = new FormLayout(
                    "pref, 3dlu, center:pref:grow, 3dlu, pref", "pref, 3dlu, pref");
            DefaultFormBuilder upperBuilder = new DefaultFormBuilder(upperLayout);

            upperBuilder.add(spacerLabel, cc.xy(1, 1));
            upperBuilder.add(syncAllLabel, cc.xywh(3, 1, 1, 3));
            upperBuilder.add(preferencesButton, cc.xy(5, 1));
            upperBuilder.add(aboutButton, cc.xy(5, 3));

            // Lower section

            FormLayout lowerLayout;
            if (showPort) {
                lowerLayout = new FormLayout(
                    "pref, 3dlu, pref, 3dlu, pref, fill:pref:grow, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref",
                    "pref");
            } else {
                lowerLayout = new FormLayout(
                    "pref, 3dlu, pref, 3dlu, pref, fill:pref:grow, pref, 3dlu, pref, 3dlu, pref",
                    "pref");
            }
            DefaultFormBuilder lowerBuilder = new DefaultFormBuilder(lowerLayout);

            int col = 1;
            lowerBuilder.add(onlineStateInfo, cc.xy(col, 1));
            col += 2;

            lowerBuilder.add(syncLabel, cc.xy(col, 1));
            col += 2;

            lowerBuilder.add(limitedConnectivityLabel, cc.xy(col, 1));
            col += 2;

            if (showPort) {
                lowerBuilder.add(portLabel, cc.xy(col, 1));
                col += 2;

                JSeparator sep0 = new JSeparator(SwingConstants.VERTICAL);
                sep0.setPreferredSize(new Dimension(2, 12));

                lowerBuilder.add(sep0, cc.xy(col, 1));
                col += 2;

            }

            JSeparator sep1 = new JSeparator(SwingConstants.VERTICAL);
            sep1.setPreferredSize(new Dimension(2, 12));

            lowerBuilder.add(downStats, cc.xy(col, 1));
            col += 2;
            lowerBuilder.add(sep1, cc.xy(col, 1));
            col += 2;
            lowerBuilder.add(upStats, cc.xy(col, 1));

            // Main section

            FormLayout mainLayout = new FormLayout(
                    "fill:pref:grow", "pref, 3dlu, pref");
            DefaultFormBuilder mainBuilder = new DefaultFormBuilder(mainLayout);
            mainBuilder.add(upperBuilder.getPanel(), cc.xy(1, 1));
            mainBuilder.add(lowerBuilder.getPanel(), cc.xy(1, 3));
            comp = mainBuilder.getPanel();
            return mainBuilder.getPanel();
        }
        return comp;
    }

    private void initComponents() {
        // Create online state info
        onlineStateInfo = createOnlineStateLabel(getController());
        // Add behavior
        onlineStateInfo.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // open connect dialog
                if (getController().getNodeManager().isStarted()) {
                    getUIController().getConnectAction().actionPerformed(null);
                } else if (!Util.isRunningProVersion()) {
                    // Smells like hack(tm).
                    new FreeLimitationDialog(getController()).open();
                }
            }
        });

        upStats = ComplexComponentFactory.createTransferCounterLabel(
            getController(), Icons.UPLOAD, Translation.getTranslation("status.upload"),
            getController().getTransferManager().getUploadCounter(),
                Translation.getTranslation("status.upload.text"));

        downStats = ComplexComponentFactory.createTransferCounterLabel(
            getController(), Icons.DOWNLOAD, Translation.getTranslation("status.download"),
            getController().getTransferManager().getDownloadCounter(),
                Translation.getTranslation("status.download.text"));

        limitedConnectivityLabel = new JLabel();
        limitedConnectivityLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (getController().isLimitedConnectivity()) {
                    getController().getIOProvider().startIO(
                        new LimitedConnectivityChecker.CheckTask(
                            getController(), false));
                    // Directly show dialog, not after check! may take up to 30
                    // seconds.poü
                    LimitedConnectivityChecker
                        .showConnectivityWarning(getController());
                }
            }
        });

        // Behaviour when the limited connecvitiy gets checked
        getController().addPropertyChangeListener(
            Controller.PROPERTY_LIMITED_CONNECTIVITY,
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    // EventQueue because property change not fired in EDT.
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            updateLimitedConnectivityLabel();
                        }
                    });
                }
            });

        syncLabel = new JLabel();
        getController().getTransferManager().addListener(
            new MyTransferManagerListener());
        updateSyncLabel();

        portLabel = new JLabel(String.valueOf(
                getController().getConnectionListener().getPort()));
        portLabel.setIcon(Icons.MAC);
        portLabel.setToolTipText(Translation.getTranslation("status.port.text"));

        preferencesButton = new JButton(getController().getUIController()
                .getOpenPreferencesAction());
        preferencesButton.setIcon(Icons.SETTINGS);
        aboutButton = new JButton(getController().getUIController()
                .getOpenAboutAction());
        aboutButton.setIcon(Icons.INFORMATION);
        spacerLabel = new JLabel() {

            /**
             * This keeps the sync button in the center of the panel.
             * @return
             */
            public Dimension getPreferredSize() {
                int w = Math.max((int) preferencesButton.getPreferredSize().getWidth(), 
                        (int) aboutButton.getPreferredSize().getWidth());
                return new Dimension(w, super.getHeight());
            }
        };

        syncAllLabel = new JLabel(Icons.SYNC_40_NORMAL);
        syncAllLabel.setToolTipText(Translation.getTranslation("scan_all_folders.description"));
        final JLabel finalLabel = syncAllLabel;
        final AtomicBoolean over = new AtomicBoolean();
        syncAllLabel.addMouseListener(new MouseAdapter(){

            public void mousePressed(MouseEvent e) {
                finalLabel.setIcon(Icons.SYNC_40_PUSH);
            }

            public void mouseReleased(MouseEvent e) {
                boolean bool = over.get();
                if (bool) {
                    finalLabel.setIcon(Icons.SYNC_40_HOVER);
                    SyncAllFoldersAction.perfomSync(getController());
                } else {
                    finalLabel.setIcon(Icons.SYNC_40_NORMAL);
                }
            }

            public void mouseEntered(MouseEvent e) {
                finalLabel.setIcon(Icons.SYNC_40_HOVER);
                over.set(true);
            }

            public void mouseExited(MouseEvent e) {
                finalLabel.setIcon(Icons.SYNC_40_NORMAL);
                over.set(false);
            }
        });




    }

    private void updateSyncLabel() {
        if (getController().getFolderRepository().isAnyFolderTransferring()) {
            syncLabel.setToolTipText(Translation
                .getTranslation("status_bar.synchronizing"));
            syncLabel.setIcon(Icons.DOWNLOAD_ACTIVE);
        } else {
            syncLabel.setText(null);
            syncLabel.setIcon(null);
        }
    }

    private void updateLimitedConnectivityLabel() {
        if (getController().isLimitedConnectivity()) {
            limitedConnectivityLabel.setToolTipText(Translation
                .getTranslation("limited_connection.title"));
            limitedConnectivityLabel.setIcon(Icons.WARNING);
        } else {
            limitedConnectivityLabel.setText("");
            limitedConnectivityLabel.setIcon(null);
        }
    }

    /**
     * Creates a label which shows the online state of a controller
     * 
     * @param controller
     *            the controller.
     * @return the label.
     */
    private JLabel createOnlineStateLabel(final Controller controller) {

        final JLabel label = new JLabel();

        NodeManagerListener nodeListener = new NodeManagerListener() {
            public void friendAdded(NodeManagerEvent e) {
            }

            public void friendRemoved(NodeManagerEvent e) {
            }

            public void nodeAdded(NodeManagerEvent e) {
                updateOnlineStateLabel(label, controller);
            }

            public void nodeConnected(NodeManagerEvent e) {
                updateOnlineStateLabel(label, controller);
            }

            public void nodeDisconnected(NodeManagerEvent e) {
                updateOnlineStateLabel(label, controller);
            }

            public void nodeRemoved(NodeManagerEvent e) {
                updateOnlineStateLabel(label, controller);
            }

            public void settingsChanged(NodeManagerEvent e) {
            }

            public void startStop(NodeManagerEvent e) {
                updateOnlineStateLabel(label, controller);
            }

            public boolean fireInEventDispathThread() {
                return true;
            }
        };
        // set initial values
        updateOnlineStateLabel(label, controller);

        // Add behavior
        controller.getNodeManager().addNodeManagerListener(nodeListener);

        return label;
    }

    private void updateOnlineStateLabel(JLabel label, Controller controller) {
        // Get connectes node count
        int nOnlineUser = controller.getNodeManager().countConnectedNodes();

        int newState;

        // System.err.println("Got " + nOnlineUser + " online users");
        if (!controller.getNodeManager().isStarted()) {
            label.setToolTipText(Translation.getTranslation("online_label.disabled"));
            label.setIcon(Icons.WARNING);
            newState = DISABLED;
        } else if (nOnlineUser > 0) {
            String text = Translation.getTranslation("online_label.online");
            if (controller.isLanOnly()) {
                text += " (" + Translation.getTranslation("general.lan_only")
                    + ')';
            }
            label.setToolTipText(text);
            label.setIcon(Icons.CONNECTED);
            newState = CONNECTED;
        } else {
            String text = Translation.getTranslation("online_label.connecting");
            if (controller.isLanOnly()) {
                text += " (" + Translation.getTranslation("general.lan_only")
                    + ')';
            }
            label.setToolTipText(text);
            label.setIcon(Icons.DISCONNECTED);
            newState = DISCONNECTED;
        }

        if (!ConfigurationEntry.SHOW_SYSTEM_NOTIFICATIONS
            .getValueBoolean(getController()))
        {
            return;
        }

        synchronized (state) {

            int oldState = state.getAndSet(newState);
            if (oldState != newState) {
                // State changed, notify ui.
                String notificationText;
                String title = Translation
                    .getTranslation("status_bar.status_change.title");
                if (newState == DISABLED) {
                    notificationText = Translation
                        .getTranslation("status_bar.status_change.disabled");
                    getUIController().notifyMessage(title, notificationText);
                } else if (newState == CONNECTED) {
                    notificationText = Translation
                        .getTranslation("status_bar.status_change.connected");
                    getUIController().notifyMessage(title, notificationText);
                } else {
                    // Disconnected
                }
            }
        }
    }

    private class MyTransferManagerListener implements TransferManagerListener {

        public void completedDownloadRemoved(TransferManagerEvent event) {
        }

        public void downloadAborted(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void downloadBroken(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void downloadCompleted(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void downloadQueued(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void downloadRequested(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void downloadStarted(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void uploadAborted(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void uploadBroken(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void uploadCompleted(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void uploadRequested(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void uploadStarted(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public void completedUploadRemoved(TransferManagerEvent event) {
            updateSyncLabel();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }

    }
}
