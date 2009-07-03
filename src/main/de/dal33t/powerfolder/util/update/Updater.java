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
 * $Id: Updater.java 6236 2008-12-31 15:44:10Z tot $
 */
package de.dal33t.powerfolder.util.update;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Base64;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StreamCallback;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Util;

/**
 * A Thread that checks for updates on powerfolder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.27 $
 */
public class Updater extends Thread {
    private static Logger LOG = Logger.getLogger(Updater.class.getName());
    protected Controller controller;
    protected UpdateSetting settings;
    private UpdaterHandler handler;

    public Updater(Controller controller, UpdateSetting settings,
        UpdaterHandler handler)
    {
        super("Update checker");
        Reject.ifNull(controller, "Controller is null");
        Reject.ifNull(settings, "Settings are null");
        Reject.ifNull(handler, "Handler is null");

        this.controller = controller;
        this.settings = settings;
        this.handler = handler;
    }

    public void run() {
        checkForNewRelease();
    }

    /**
     * Installs a periodical (usually once per hour) update check with the given
     * handler.
     * 
     * @param controller
     * @param updateHandler
     */
    public static void installPeriodicalUpdateCheck(
        final Controller controller, final UpdaterHandler updateHandler)
    {
        Reject.ifNull(controller, "Controller is null");
        TimerTask updateCheckTask = new TimerTask() {
            @Override
            public void run() {
                // Check for an update
                if (controller.getUpdateSettings() != null) {
                    new Updater(controller, controller.getUpdateSettings(),
                        updateHandler).start();
                }
            }
        };
        // Check for shortly after start.
        controller.scheduleAndRepeat(updateCheckTask,
            Controller.getWaitTime() * 3,
            1000L * 60 * Constants.UPDATE_CHECK_PERIOD_MINUTES);

    }

    /**
     * Checks for new application release at the remote location
     */
    private void checkForNewRelease() {
        LOG.info("Checking for newer version");
        if (!handler.shouldCheckForNewVersion()) {
            return;
        }
        final String newerVersion = newerReleaseVersionAvailable();
        if (newerVersion != null) {
            handler.newReleaseAvailable(new UpdaterEvent(this, newerVersion,
                getReleaseExeURL()));
        } else {
            handler.noNewReleaseAvailable(new UpdaterEvent(this));
        }
    }

    /**
     * Method that downloads and installs the version of PowerFolder from the
     * given URL.
     * 
     * @param url
     * @param progressCallback
     * @param silentUpdate
     * @return the updater Process or null if failed.
     */
    public Process downloadAndUpdate(URL url, StreamCallback progressCallback,
        boolean silentUpdate)
    {
        File releaseExe = download(url, progressCallback);
        if (releaseExe == null) {
            return null;
        }
        return openReleaseExe(releaseExe, silentUpdate);
    }

    /**
     * Downloads a new powerfolder release file from a URL.
     * 
     * @param url
     *            the url
     * @param progressCallback
     *            the callback to monitor the download.
     * @return the downloaded file if succeeded or null if failed
     */
    public File download(URL url, StreamCallback progressCallback) {
        URLConnection con;
        String username = settings.httpUser;
        String pw = settings.httpPassword;
        String filename = url.getFile();
        if (StringUtils.isBlank(filename)) {
            filename = "PowerFolder_Latest_Win32_Installer.exe";
        }
        File targetFile = new File(Controller.getTempFilesLocation(), filename);
        try {
            con = url.openConnection();
            if (!StringUtils.isEmpty(username)) {
                String s = username + ":" + pw;
                String base64 = "Basic " + Base64.encodeBytes(s.getBytes());
                con.setDoInput(true);
                con.setRequestProperty("Authorization", base64);
                con.connect();
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Unable to download from " + url, e);
            return null;
        }

        LOG.log(Level.WARNING, "Downloading latest version from "
            + con.getURL());
        File tempFile = new File(targetFile.getParentFile(), "(downloading) "
            + targetFile.getName());
        try {
            // Copy/Download from URL
            con.connect();
            FileUtils.copyFromStreamToFile(con.getInputStream(), tempFile,
                progressCallback != null ? progressCallback : null, con
                    .getContentLength());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Unable to download from " + url, e);
            return null;
        }

        // Rename file and set modified/build time
        targetFile.delete();
        tempFile.renameTo(targetFile);
        targetFile.setLastModified(con.getLastModified());

        if (targetFile.getName().toLowerCase().endsWith("jar")) {
            // Additional jar check
            if (!FileUtils.isValidZipFile(targetFile)) {
                // Invalid file downloaded
                targetFile.delete();
                return null;
            }
        }

        return targetFile;
    }

    /**
     * Returns the newer program version available on the net. Otherwise returns
     * null
     * 
     * @return
     */
    private String newerReleaseVersionAvailable() {
        URL url;
        try {
            url = new URL(settings.versionCheckURL);
        } catch (MalformedURLException e) {
            LOG.log(Level.FINER, e.toString(), e);
            return null;
        }
        try {
            InputStream in = (InputStream) url.getContent();
            String latestVersion = "";
            while (in.available() > 0) {
                latestVersion += (char) in.read();
            }

            if (latestVersion != null) {
                if (latestVersion.length() > 50) {
                    LOG.log(Level.SEVERE,
                        "Received illegal response while checking latest available version from "
                            + settings.versionCheckURL);
                    return null;
                }
                LOG.info("Latest available version: " + latestVersion);

                if (Util.compareVersions(latestVersion,
                    Controller.PROGRAM_VERSION))
                {
                    LOG.info("Latest version is newer than this one");
                    return latestVersion;
                }
                LOG.info("This version is up-to-date");
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING,
                "Unable to retrieve latest available version for: "
                    + settings.versionCheckURL);
            LOG.log(Level.FINER, e.toString(), e);
        }
        return null;
    }

    /**
     * Returns the download URL for the latest program version
     * 
     * @return
     */
    private URL getReleaseExeURL() {
        URL releaseExeURL = null;
        try {
            if (settings.downloadLinkInfoURL != null) {
                URL url = new URL(settings.downloadLinkInfoURL);
                InputStream in = (InputStream) url.getContent();
                StringBuilder b = new StringBuilder();
                while (in.available() > 0) {
                    b.append((char) in.read());
                }
                in.close();

                releaseExeURL = new URL(b.toString());
                LOG.info("Latest available version download: "
                    + releaseExeURL.toExternalForm());
            }
        } catch (MalformedURLException e) {
            LOG.log(Level.FINER, e.toString(), e);
        } catch (IOException e) {
            LOG.log(Level.FINER, e.toString(), e);
        }
        if (releaseExeURL == null) {
            // Fallback to standart settings
            try {
                releaseExeURL = new URL(settings.releaseExeURL);
            } catch (MalformedURLException e) {
                LOG.log(Level.SEVERE, "Invalid release exec download location",
                    e);
            }
        }
        return releaseExeURL;
    }

    private Process openReleaseExe(File file, boolean updateSilently) {
        try {
            String c = "cmd.exe";
            c += " /c ";
            c += '"';
            c += file.getAbsolutePath();
            if (updateSilently) {
                c += " /S";
            }
            c += '"';
            LOG.info("Executing: " + c);
            return Runtime.getRuntime().exec(c);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Unable to start update exe at "
                + file.getAbsolutePath() + ". " + e, e);
            return null;
        }
    }

    /**
     * Contains settings for the updatecheck.
     */
    public static class UpdateSetting {
        public String versionCheckURL = "http://checkversion.powerfolder.com/PowerFolder_LatestVersion.txt";
        /**
         * A info file containing the link that may override
         * <code>releaseExeURL</code> if existing.
         */
        public String downloadLinkInfoURL = "http://checkversion.powerfolder.com/PowerFolder_DownloadLocation.txt";
        public String releaseExeURL = "http://download.powerfolder.com/free/PowerFolder_Latest_Win32_Installer.exe";

        public String httpUser;
        public String httpPassword;
    }
}