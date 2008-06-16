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
package de.dal33t.powerfolder.ui.notification;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Creates an animated view that slides out of the bottom-right corner of the
 * screen. The user of this class supplies the contents for sliding. Also, the
 * view automatically closes itself after predetermined amount of time, which is
 * currently set to 15 seconds.<br>
 * This class is based on code and ideas from <i>Swing Hacks</i> book by Joshua
 * Marinacci and Chris Adamson.<br>
 * 
 * @author <a href="mailto:magapov@gmail.com">Maxim Agapov</a>
 * @version $Revision: 1.2 $
 */
public class Slider {

    /**
     * Default animation time
     */
    public static final int ANIMATION_TIME = 700;

    /**
     * Default delay between animation frames
     */
    public static final int ANIMATION_DELAY = 10;

    /**
     * Default delay before autodismissing the view is 15 seconds
     */
    public static final int DISMISS_DELAY = 15000;

    private JWindow window;

    private JComponent contents;

    private Timer animateUpTimer;

    private Timer dismissTimer;

    private Timer animateDownTimer;

    private int showX;

    private int startY;

    private Dimension contentsSize;

    private AnimatingSheet animatingSheet;

    /**
     * Constructor
     * 
     * @param contents
     */
    public Slider(JComponent contents) {
        this.contents = contents;
    }

    public JComponent getContents() {
        return contents;
    }

    /*
     * Query graphics environment for maximum window bounds.
     */
    private static Rectangle initDesktopBounds() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getMaximumWindowBounds();
    }

    /**
     * Create a window with an animating sheet copy over its contents from the
     * temp window animate it when done, remove animating sheet and add real
     * contents.
     */
    public void show() {
        if (window != null) {
            return;
        }
        window = new JWindow();
        animatingSheet = new AnimatingSheet();
        animatingSheet.setSource(contents);
        window.setAlwaysOnTop(true);
        window.setVisible(true);

        // Initial boundaries.
        contentsSize = contents.getSize();
        Rectangle desktopBounds = initDesktopBounds();
        showX = desktopBounds.width - contentsSize.width;
        startY = desktopBounds.y + desktopBounds.height;

        // Timer to animate the sheet down.
        animateDownTimer = new Timer(ANIMATION_DELAY, new ActionListener() {
            private int percentage = 99;
            public void actionPerformed(ActionEvent e) {
                animate(percentage);
                if (percentage-- <= 0) {
                    animateDownTimer.stop();
                }
            }
        });

        // Timer to pause for the user to read.
        dismissTimer = new Timer(DISMISS_DELAY, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                animateDownTimer.start();
            }
        });
        dismissTimer.setRepeats(false);

        // Timer to animate the sheet up.
        animateUpTimer = new Timer(ANIMATION_DELAY, new ActionListener() {
            private int percentage = 1;
            public void actionPerformed(ActionEvent e) {
                animate(percentage);
                if (percentage++ >= 100) {
                    animateUpTimer.stop();
                    dismissTimer.start();
                }
            }
        });

        animateUpTimer.start();
    }


    /**
     * Close and dispose the animated window.
     */
    public void close() {

        // stop the timers
        if (animateUpTimer != null && animateUpTimer.isRunning()) {
            animateUpTimer.stop();
        }
        if (animateDownTimer != null && animateDownTimer.isRunning()) {
            animateDownTimer.stop();
        }
        if (dismissTimer != null && dismissTimer.isRunning()) {
            dismissTimer.stop();
        }

        // close the window
        if (window != null) {
            window.dispose();
            window = null;
        }
    }

    /**
     * Show the correct percentage of the size.
     *
     * @param percentage
     */
    public void animate(long percentage) {

        if (window == null) {
            // Huh?
            return;
        }

        if (percentage >= 100) {
            // Put real contents in window and show
            window.getContentPane().removeAll();
            window.getContentPane().add(contents);
            window.pack();
            window.setLocation(showX, startY - window.getHeight());
        } else if (percentage <= 0) {
            // Nothing to show
            window.dispose();
        } else {
            int animatingHeight = (int) (percentage * contentsSize.height / 100.0);
            animatingHeight = Math.max(animatingHeight, 1);
            animatingSheet.setAnimatingHeight(animatingHeight);
            window.getContentPane().removeAll();
            window.getContentPane().add(animatingSheet);
            window.pack();
            window.setLocation(showX, startY - window.getHeight());
        }
    }
}
