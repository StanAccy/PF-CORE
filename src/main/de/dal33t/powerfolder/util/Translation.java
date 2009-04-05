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
package de.dal33t.powerfolder.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Basic class which provides accessor to tranlation files
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.13 $
 */
public class Translation {

    private static final Logger log = Logger.getLogger(Translation.class.getName());

    // Useful locales, which are not already included in Locale
    public static final Locale DUTCH = new Locale("nl");
    public static final Locale SPANISH = new Locale("es");
    public static final Locale RUSSIAN = new Locale("ru");
    public static final Locale SWEDISH = new Locale("sv");
    public static final Locale ARABIC = new Locale("ar");
    public static final Locale POLISH = new Locale("pl");
    public static final Locale PORTUGUESE = new Locale("pt");

    /** List of all supported locales */
    private static Locale[] supportedLocales;

    // The resource bundle, initalized lazy
    private static ResourceBundle resourceBundle;

    /**
     * 
     */
    private Translation() {
        super();
    }

    /**
     * @return the supported locales by PowerFolder
     */
    public static synchronized Locale[] getSupportedLocales() {
        if (supportedLocales == null) {
            supportedLocales = new Locale[14];
            supportedLocales[0] = Locale.ENGLISH;
            supportedLocales[1] = Locale.UK;
            supportedLocales[2] = Locale.GERMAN;
            supportedLocales[3] = DUTCH;
            supportedLocales[4] = Locale.JAPANESE;
            supportedLocales[5] = Locale.ITALIAN;
            supportedLocales[6] = SPANISH;
            supportedLocales[7] = RUSSIAN;
            supportedLocales[8] = Locale.FRENCH;
            supportedLocales[9] = Locale.CHINESE;
            supportedLocales[10] = SWEDISH;
            supportedLocales[11] = ARABIC;
            supportedLocales[12] = POLISH;
            supportedLocales[13] = PORTUGUESE;
        }
        Arrays.sort(supportedLocales, new Comparator<Locale>() {
            public int compare(Locale o1, Locale o2) {
                return o1.getDisplayName(o1).compareTo(o2.getDisplayName(o2));
            }});
        return supportedLocales;
    }

    /**
     * @return the currently active locale of the used resource bundle
     */
    public static Locale getActiveLocale() {
        Locale locale = getResourceBundle().getLocale();
        if (locale == null || StringUtils.isEmpty(locale.getLanguage())) {
            // Workaround for english
            return Locale.ENGLISH;
        }
        return getResourceBundle().getLocale();
    }

    /**
     * @return true if a custom (non-standard) locale is currently active.
     */
    public static boolean isCustomLocale() {
        return !Arrays.asList(getSupportedLocales()).contains(getActiveLocale());
    }

    /**
     * Saves/Overrides the locale setting. Next time the resource bundle is
     * initalized, it tries to gain bundle with that locale. Otherwise fallback
     * to default locale
     * 
     * @param locale
     *            the locale, or null to reset
     */
    public static void saveLocalSetting(Locale locale) {
        if (locale != null) {
            if (locale.getCountry().equals("")) {
                Preferences.userNodeForPackage(Translation.class).put("locale",
                    locale.getLanguage());
            } else {
                Preferences.userNodeForPackage(Translation.class).put("locale",
                    locale.getLanguage() + '_' + locale.getCountry());
            }
        } else {
            Preferences.userNodeForPackage(Translation.class).remove("locale");
        }
    }

    /**
     * Reset the resource bundle. Next call will return a freshly initalized RB
     */
    public static void resetResourceBundle() {
        resourceBundle = null;
    }

    public static void setResourceBundle(ResourceBundle newResourceBundle) {
        resourceBundle = newResourceBundle;
    }

    /**
     * @return the currently active resource bundle
     */
    public static synchronized ResourceBundle getResourceBundle() {
        if (resourceBundle == null) {
            // Intalize bundle
            try {
                // Get language out of preferences
                String confLangStr = Preferences.userNodeForPackage(
                    Translation.class).get("locale", null);
                Locale confLang = confLangStr != null
                    ? new Locale(confLangStr)
                    : null;
                // Take default locale if config is empty
                if (confLang == null) {
                    confLang = Locale.getDefault();
                }
                // Workaround for EN
                if (confLangStr != null) {
                    if (confLangStr.equals("en_GB")) {
                        confLang = Locale.UK;
                    } else if (confLangStr.equals("en")) {
                        // Normal (USA) English
                        confLang = new Locale("");
                    }
                }
                resourceBundle = ResourceBundle.getBundle("Translation",
                    confLang);

                log.warning("Default Locale '" + Locale.getDefault()
                    + "', using '" + resourceBundle.getLocale()
                    + "', in config '" + confLang + '\'') ;
            } catch (MissingResourceException e) {
                log.log(Level.SEVERE, "Unable to load translation file", e);
            }
        }
        return resourceBundle;
    }

    /**
     * Returns translation for this id
     * 
     * @param id
     *            the id for the translation entry
     * @return the localized string
     */
    public static String getTranslation(String id) {
        ResourceBundle rb = getResourceBundle();
        if (rb == null) {
            return "- " + id + " -";
        }
        try {
            String translation = rb.getString(id);
            // log.warning("Translation for '" + id + "': " + translation);
            return translation;
        } catch (MissingResourceException e) {
            if (id != null && !id.startsWith("date_format.")) {
                // Only log non-date format errors.
                // Date format error may occur during logging, prevent
                // stackoverflow error.
                log.warning("Unable to find translation for ID '" + id + '\'');
                log.log(Level.FINER, "MissingResourceException", e);
            }
            return "- " + id + " -";
        }
    }

    /**
     * Returns a paramterized translation for this id.
     * <p>
     * Use <code>{0}</code> <code>{1}</code> etc as placeholders in property files
     * 
     * @param id
     * @param params
     *            the parameters to be included.
     * @return a paramterized translation for this id.
     */
    public static String getTranslation(String id, Object... params) {
        String translation = getTranslation(id);
        int paramCount = 0;
        for (Object param : params) {
            int i;
            String formattedParam = formatParam(param);
            String paramSymbol = "{" + paramCount++ + '}';
            while ((i = translation.indexOf(paramSymbol)) >= 0) {
                translation = translation.substring(0, i) + formattedParam
                    + translation.substring(i + 3, translation.length());
            }
        }
        return translation;
    }

    /**
     * Convert some primitive types into nice formatted strings.
     *
     * @param param
     * @return
     */
    private static String formatParam(Object param) {
        if (param instanceof Integer) {
            Integer i = (Integer) param;
            return Format.formatLong(i.longValue());
        } else if (param instanceof Long) {
            Long l = (Long) param;
            return Format.formatLong(l);
        } else if (param instanceof Double) {
            Double d = (Double) param;
            return Format.formatNumber(d);
        } else if (param instanceof Float) {
            Float f = (Float) param;
            return Format.formatNumber(f.doubleValue());
        }
        return param.toString();
    }
}