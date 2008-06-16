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
package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.io.FileWriter;

import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.ControllerTestCase;

public class RecycleTest extends ControllerTestCase {

    public void setUp() throws Exception {
        // Remove directries

        super.setUp();

        setupTestFolder(SyncProfile.HOST_FILES);
        File localbase = getFolder().getLocalBase();
        File testFile = new File(localbase, "test.txt");
        if (testFile.exists()) {
            testFile.delete();
        }

        assertTrue(testFile.createNewFile());

        FileWriter writer = new FileWriter(testFile);
        writer
            .write("This is the test text.\n\nl;fjk sdl;fkjs dfljkdsf ljds flsfjd lsjdf lsfjdoi;ureffd dshf\nhjfkluhgfidgh kdfghdsi8yt ribnv.,jbnfd kljhfdlkghes98o jkkfdgh klh8iesyt");
        writer.close();
        scanFolder(getFolder());
    }

    public void testRecycleBin() {
        System.out.println("testRecycleBin");
        FileInfo[] files = getFolder().getKnowFilesAsArray();
        FileInfo testfile = files[0];
        File file = getFolder().getDiskFile(testfile);
        RecycleBin bin = getController().getRecycleBin();

        getFolder().removeFilesLocal(files);
        assertFalse(file.exists());
        assertTrue(bin.restoreFromRecycleBin(testfile));
        assertTrue(file.exists());
        assertFileMatch(file, getFolder().getKnownFiles().iterator().next());
        assertEquals(testfile.getModifiedDate(), getFolder().getKnownFiles()
            .iterator().next().getModifiedDate());
        getFolder().removeFilesLocal(files);
        assertFalse(file.exists());
        bin.delete(testfile);
        assertFalse(file.exists());
    }

}
