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
package de.dal33t.powerfolder;

import com.jgoodies.binding.adapter.PreferencesAdapter;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.util.Reject;

import java.util.logging.Level;

/**
 * Refelects an entry setting in the preferences. Provides basic method for
 * accessing and setting the prefs. Preferences are stored (on windows) in the
 * registry.
 */
public enum PreferencesEntry {
    /**
     * Hide offline friends
     */
    NODE_MANAGER_MODEL_HIDE_OFFLINE_FRIENDS("NodeManagerModel_HideOfflineFriends",
        false),
    /**
     * Include all LAN users
     */
    NODE_MANAGER_MODEL_INCLUDE_ONLINE_LAN_USERS("NodeManagerModel_IncludeLanUsers",
        true),

    /** find offline users */
    FRIEND_SEARCH_HIDE_OFFLINE("FriendsSearch_HideOfflineUsers", false),

    QUIT_ON_X("quitonx", false),
    
    ASK_FOR_QUIT_ON_X("AskForQuitOnX", true),

    WARN_ON_CLOSE("WarnOnClose", true),

    ASK_FOR_FRIENDSHIP_ON_PRIVATE_FOLDER_JOIN(
        "AskForFriendshipOnPrivateFolderJoin", true),

    ASK_FOR_FRIENDSHIP_MESSAGE("AskForFriendshipMessage", true),

    /**
     * Days until auto cleanup of uploads.
     * Zero = cleanup on completion.
     */
    UPLOAD_AUTO_CLEANUP_FREQUENCY("UploadAutoCleanupFrequency", 5),

    /**
     * Days until auto cleanup of downloads.
     * Zero = cleanup on completion.
     */
    DOWNLOAD_AUTO_CLEANUP_FREQUENCY("DownloadAutoCleanupFrequency", 5),

    SHOW_PREVIEW_PANEL("ShowPreviewPanel", false),

    UI_LOOK_AND_FEEL("UILookAndFeel", null),

    SHOW_ADVANCED_SETTINGS("ShowAdvancedSettings", false),

    UNDERLINE_LINKS("UnderlineLinks", true),

    FILE_NAME_CHECK("folder.check_filenames", true),

    CHECK_UPDATE("updatechecker.askfornewreleaseversion", true),

    MASS_DELETE_PROTECTION("mass.delete.protection", true),

    MASS_DELETE_THRESHOLD("mass.delete.threshold", 75),

    /**
     * the pref that holds a boolean value if the connection should be tested
     * and a warning displayed if limited connectivty is given.
     */
    TEST_CONNECTIVITY("test_for_connectivity", true),

    DUPLICATE_FOLDER_USE("duplicaet_folder_use", true),

    SETUP_DEFAULT_FOLDER("setup_default_folder", true),

    /**
     * If the last password of login should be reminded.
     */
    SERVER_REMEMBER_PASSWORD("server_remind_password", true),

    COMPUTER_TYPE_SELECTION("computer_type_selection", 0),

    FOLDER_TYPE_SELECTION("folder_type_selection", 0),

    /**
     * Whether to use magnetic frames by default (i.e. not with ctrl key)
     */
    USE_MAGNETIC_FRAMES("use.magnetic.frames", true),

    DOCUMENT_LOGGING("document.logging", Level.WARNING.getName()),

    /** Whether the backup os check box is checked. */
    BACKUP_OS("backup.os", true),

    /** Whether the user uses OS. If not, don't show OS stuff. */
    USE_ONLINE_STORAGE("use.os", true),

    /** Percentage of translucency of main frame when lost focus. */
    TRANSLUCENT_PERCENTAGE("translucent.percentage", 50),

    /** Whether to show the main frame translucent when lost focus. */
    TRANSLUCENT_MAIN_FRAME("translucent.main.frame", false),

    /** How many seconds the notification should display. */
    NOTIFICATION_DISPLAY("notification.display", 10),

    /** How translucent the notification should display, as percentage. */
    NOTIFICATION_TRANSLUCENT("notification.translucent", 0),

    /** Main frame always on top. */
    MAIN_ALWAYS_ON_TOP("main.stay.on.top", false),

    /** Warning about unsyned folders. */
    FOLDER_SYNC_USE("folder.sync.use", true),

    /** Days before warning about unsynced folders. */
    FOLDER_SYNC_WARN("folder.sync.warn", 10),

    /** Skin index. */
    SKIN_INDEX("skin.index", 0);

    /** String, Boolean, Integer */
    private Class type;

    private String preferencesKey;
    private Object defaultValue;

    // Methods/Constructors ***************************************************

    private PreferencesEntry(String aPreferencesKey, boolean theDefaultValue) {
        Reject.ifBlank(aPreferencesKey, "Preferences key is blank");
        this.type = Boolean.class;
        this.preferencesKey = aPreferencesKey;
        this.defaultValue = theDefaultValue;
    }

    private PreferencesEntry(String aPreferencesKey, int theDefaultValue) {
        Reject.ifBlank(aPreferencesKey, "Preferences key is blank");
        this.type = Integer.class;
        this.preferencesKey = aPreferencesKey;
        this.defaultValue = theDefaultValue;
    }

    private PreferencesEntry(String aPreferencesKey, String theDefaultValue) {
        Reject.ifBlank(aPreferencesKey, "Preferences key is blank");
        this.type = String.class;
        this.preferencesKey = aPreferencesKey;
        this.defaultValue = theDefaultValue;
    }

    /**
     * @param controller
     *            the controller to read the config from
     * @return The current value from the configuration for this entry. or
     */
    public String getValueString(Controller controller) {
        if (!type.isAssignableFrom(String.class)) {
            throw new IllegalStateException("This preferences entry has type "
                + type.getName() + " cannot acces as String");
        }
        return controller.getPreferences().get(preferencesKey,
            (String) defaultValue);
    }

    /**
     * the preferences entry if its a Integer.
     * 
     * @param controller
     *            the controller to read the config from
     * @return The current value from the preferences for this entry. or the
     *         default value if value not set.
     */
    public Integer getValueInt(Controller controller) {
        if (!type.isAssignableFrom(Integer.class)) {
            throw new IllegalStateException("This preferences entry has type "
                + type.getName() + " cannot access as Integer");
        }
        return controller.getPreferences().getInt(preferencesKey,
            (Integer) defaultValue);
    }

    /**
     * Parses the configuration entry into a Boolen.
     * 
     * @param controller
     *            the controller to read the config from
     * @return The current value from the configuration for this entry. or the
     *         default value if value not set/unparseable.
     */
    public Boolean getValueBoolean(Controller controller) {
        if (!type.isAssignableFrom(Boolean.class)) {
            throw new IllegalStateException("This preferences entry has type "
                + type.getName() + " cannot access as Boolean");
        }
        return controller.getPreferences().getBoolean(preferencesKey,
            (Boolean) defaultValue);
    }

    /**
     * Constructs a preferences adapter which is directly bound to the
     * preferences entry.
     * 
     * @param controller
     *            the controller
     * @return the model bound to the pref entry.
     */
    public ValueModel getModel(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        return new PreferencesAdapter(controller.getPreferences(),
            preferencesKey, defaultValue);
    }

    /**
     * Sets the value of this preferences entry.
     * 
     * @param controller
     *            the controller of the prefs
     * @param value
     *            the value to set
     */
    public void setValue(Controller controller, String value) {
        Reject.ifNull(controller, "Controller is null");
        if (!type.isAssignableFrom(String.class)) {
            throw new IllegalStateException("This preferences entry has type "
                + type.getName() + " cannot set as String");
        }
        controller.getPreferences().put(preferencesKey, value);
    }

    /**
     * Sets the value of this preferences entry.
     * 
     * @param controller
     *            the controller of the prefs
     * @param value
     *            the value to set
     */
    public void setValue(Controller controller, boolean value) {
        Reject.ifNull(controller, "Controller is null");
        if (!type.isAssignableFrom(Boolean.class)) {
            throw new IllegalStateException("This preferences entry has type "
                + type.getName() + " cannot set as Boolean");
        }
        controller.getPreferences().putBoolean(preferencesKey, value);
    }

    /**
     * Sets the value of this preferences entry.
     * 
     * @param controller
     *            the controller of the prefs
     * @param value
     *            the value to set
     */
    public void setValue(Controller controller, int value) {
        Reject.ifNull(controller, "Controller is null");
        if (!type.isAssignableFrom(Integer.class)) {
            throw new IllegalStateException("This preferences entry has type "
                + type.getName() + " cannot set as Integer");
        }
        controller.getPreferences().putInt(preferencesKey, value);
    }

    /**
     * Removes the entry from the preferences.
     * 
     * @param controller
     *            the controller to use
     */
    public void removeValue(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        controller.getPreferences().remove(preferencesKey);
    }
}
