/* $Id: ControlQuarter.java,v 1.11 2006/03/09 13:24:37 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.navigation;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.uif_lite.panel.SimpleInternalFrame;
import de.dal33t.powerfolder.*;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.NavigationEvent;
import de.dal33t.powerfolder.event.NavigationListener;
import de.dal33t.powerfolder.ui.DebugPanel;
import de.dal33t.powerfolder.ui.model.FolderModel;
import de.dal33t.powerfolder.ui.model.DirectoryModel;
import de.dal33t.powerfolder.ui.dialog.PreviewToJoinPanel;
import de.dal33t.powerfolder.ui.action.*;
import de.dal33t.powerfolder.ui.folder.FilesTab;
import de.dal33t.powerfolder.ui.folder.FolderPanel;
import de.dal33t.powerfolder.ui.render.NavTreeCellRenderer;
import de.dal33t.powerfolder.ui.widget.AutoScrollingJTree;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.DragDropChecker;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.TreeNodeList;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Controler Quarter.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.11 $
 */
public class ControlQuarter extends PFUIComponent {

    /* Complete panel */
    private JPanel uiPanel;

    /* Navtree */
    private JTree uiTree;
    private NavTreeModel navTreeModel;

    /* The popup menu */
    private JPopupMenu myFoldersMenu;
    private JPopupMenu myFolderMenu;
    private JPopupMenu previewFolderMenu;
    private JPopupMenu friendsListMenu;
    private JPopupMenu directoryMenu;
    /* Models */
    /** The parent of the currently selected value in our selection model */
    private Object selectionParent;
    /** The currently selected item */
    private SelectionModel selectionModel;

    private NavigationModel navigationModel;
    /**
     * The path in the tree that was last expanded, use to restore the tree
     * state if a tree structure change was fired.
     */
    private TreePath lastExpandedPath;

    /**
     * Constructs a new navigation tree for a controller
     * 
     * @param controller
     */
    public ControlQuarter(Controller controller) {
        super(controller);
        navTreeModel = getUIController().getApplicationModel()
            .getNavTreeModel();
        selectionModel = new SelectionModel();
        selectionParent = null;
    }

    /*
     * Exposing methods for UI Component **************************************
     * components get initalized lazy
     */

    /**
     * TODO move this into a <code>UIModel</code>
     * 
     * @return the uis navigation model
     */
    public NavigationModel getNavigationModel() {
        return navigationModel;
    }

    /**
     * Answers and builds if needed the complete ui component
     * 
     * @return
     */
    public JComponent getUIComponent() {
        if (uiPanel == null) {
            FormLayout layout = new FormLayout("fill:pref:grow", "fill:0:grow");
            PanelBuilder builder = new PanelBuilder(layout);

            CellConstraints cc = new CellConstraints();

            // Make preferred size smaller.
            JScrollPane pane = new JScrollPane(getUITree());
            pane.setBorder(Borders.EMPTY_BORDER);
            UIUtil.setZeroHeight(pane);
            Dimension dims = pane.getPreferredSize();
            dims.width = 10;
            pane.setPreferredSize(dims);

            builder.add(pane, cc.xy(1, 1));

            SimpleInternalFrame frame = new SimpleInternalFrame(Translation
                .getTranslation("navtree.title"));
            frame.setToolBar(new NavigationToolBar(getController(),
                navigationModel).getUIComponent());
            frame.add(builder.getPanel());
            uiPanel = frame;
        }
        return uiPanel;
    }

    /**
     * @return
     */
    public JTree getUITree() {
        if (uiTree == null) {
            uiTree = new AutoScrollingJTree(navTreeModel);

            // Selection listener to update selection model
            uiTree.getSelectionModel().addTreeSelectionListener(
                new NavTreeSelectionAdapater());
            // HACK
            getUIController().getInformationQuarter().registerNavTreeListener(
                uiTree);
            uiTree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);

            // build popup menus
            buildPopupMenus();

            // Set renderer
            uiTree.setCellRenderer(new NavTreeCellRenderer(getController()));
            uiTree.addMouseListener(new NavTreeListener());

            // remember the last expanded path
            // make it null if a parent of the last expanded path is closed
            uiTree.addTreeExpansionListener(new TreeExpansionListener() {
                public void treeCollapsed(TreeExpansionEvent treeExpansionEvent)
                {
                    TreePath closedPath = treeExpansionEvent.getPath();
                    // log().debug("closed path : " + closedPath);
                    // log().debug("lastExpandedPath path: " +
                    // lastExpandedPath);

                    // note that this method name maybe confusing
                    // it is true if lastExpandedPath is a descendant of
                    // closedPath
                    if (closedPath.isDescendant(lastExpandedPath)) {
                        // log().debug("isDescendant!");
                        lastExpandedPath = null;
                    }
                }

                public void treeExpanded(TreeExpansionEvent treeExpansionEvent)
                {
                    lastExpandedPath = treeExpansionEvent.getPath();
                }
            });
            if (FilesTab.ENABLE_DRAG_N_DROP) {
                new DropTarget(uiTree, DnDConstants.ACTION_COPY,
                    new MyDropTargetListener(), true);
            }
            navigationModel = new NavigationModel(uiTree.getSelectionModel());
            navigationModel.addNavigationListener(new NavigationListener() {
                public void navigationChanged(NavigationEvent event) {
                    TreePath newTreePath = event.getTreePath();
                    TreePath current = uiTree.getSelectionModel()
                        .getSelectionPath();
                    if (current != null && newTreePath != null
                        && !newTreePath.equals(current))
                    {
                        setSelectedTreePath(newTreePath);
                    }
                }
            });

        }
        return uiTree;
    }

    /**
     * Builds the popup menues
     */
    private void buildPopupMenus() {
        // Popupmenus
        // create popup menu for directory

        directoryMenu = new JPopupMenu();
        if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
            directoryMenu.add(new OpenLocalFolder(getController()));
        }

        // create popup menu for My Folder

        myFoldersMenu = new JPopupMenu();
        myFoldersMenu.add(getUIController().getSyncAllFoldersAction());
        myFoldersMenu.add(getUIController().getFolderCreateAction());
        myFoldersMenu.add(getUIController().getHidePreviewsAction());

        // create popup menu for (my) folder
        myFolderMenu = new JPopupMenu();
        myFolderMenu.add(new SyncFolderAction(getController()));
        if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
            myFolderMenu.add(new OpenLocalFolder(getController()));
        }
        myFolderMenu.add(new OpenChatAction(getController(), selectionModel));
        myFolderMenu.add(new SendInvitationAction(getController(),
            selectionModel));

        // Separator
        myFolderMenu.addSeparator();

        if (getUIController().getFolderCreateShortcutAction().getValue(
            CreateShortcutAction.SUPPORTED) == Boolean.TRUE)
        {
            myFolderMenu.add(getUIController().getFolderCreateShortcutAction());
        }
        myFolderMenu.add(getUIController().getFolderLeaveAction());

        // create popup menu for (preview) folder
        previewFolderMenu = new JPopupMenu();
        previewFolderMenu.add(getUIController().getPreviewJoinAction());
        previewFolderMenu.add(getUIController().getPreviewFolderRemoveAction());

        // Friends list popup menu
        friendsListMenu = new JPopupMenu();
        friendsListMenu.add(getUIController().getFindFriendAction());
    }

    // Exposing ***************************************************************

    /**
     * used in RootTable and on init, should alway be a level 1 or 2 treeNode,
     * the root or just below the root
     */
    public void setSelected(TreeNode node) {
        if (node != null) {
            if (node == navTreeModel.getRootNode()) {
                TreeNode[] path = new TreeNode[1];
                path[0] = navTreeModel.getRootNode();
                setSelectedPath(path);
            } else {
                TreeNode[] path = new TreeNode[2];
                path[0] = navTreeModel.getRootNode();
                path[1] = node;
                setSelectedPath(path);
            }
        }
    }

    private void setSelectedPath(TreeNode[] pathArray) {
        TreePath treePath = new TreePath(pathArray);
        setSelectedTreePath(treePath);
    }

    private void setSelectedTreePath(TreePath path) {
        setSelectedTreePath(path, true);
    }

    private void setSelectedTreePath(final TreePath path, final boolean scroll)
    {
        Runnable runner = new Runnable() {
            public void run() {
                uiTree.setSelectionPath(path);
                if (scroll) {
                    uiTree.scrollPathToVisible(path);
                }
            }
        };
        if (EventQueue.isDispatchThread()) {
            runner.run();
        } else {
            EventQueue.invokeLater(runner);
        }
    }

    /**
     * sets the selected Directory in a tree
     * 
     * @param directory
     *            The newly selected directory
     */
    public void setSelected(Directory directory) {
        log().verbose("setSelected:" + directory);
        if (directory != null) {
            Folder folder = directory.getRootFolder();
            FolderModel folderModel = getController().getUIController()
                    .getFolderRepositoryModel().locateFolderModel(folder);
            TreeNode[] path = new TreeNode[3];
            path[0] = navTreeModel.getRootNode();
            path[1] = getUIController().getFolderRepositoryModel()
                .getMyFoldersTreeNode();
            path[2] = folderModel.getTreeNode();
            setSelectedPath(path);
        }
    }

    /**
     * navigation uses this to reopen the expanded nodes if model has changed
     */
    public JTree getTree() {
        return uiTree;
    }

    public void setSelected(Folder folder) {
        FolderModel folderModel = getController().getUIController()
                .getFolderRepositoryModel().locateFolderModel(folder);
        MutableTreeNode node = folderModel.getTreeNode();
        TreeNode[] path = new TreeNode[3];
        path[0] = navTreeModel.getRootNode();
        path[1] = getUIController().getFolderRepositoryModel()
            .getMyFoldersTreeNode();
        path[2] = node;
        setSelectedPath(path);
    }

    public void setSelected(Member member) {
        if (member.isFriend()) { // try to select the friend node
            TreeNodeList friendsNode = getUIController().getNodeManagerModel()
                .getFriendsTreeNode();
            for (int i = 0; i < friendsNode.getChildCount(); i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) friendsNode
                    .getChildAt(i);
                if (member == node.getUserObject()) {
                    TreeNode[] path = new TreeNode[3];
                    path[0] = navTreeModel.getRootNode();
                    path[1] = friendsNode;
                    path[2] = node;
                    setSelectedPath(path);
                    return;
                }
            }
        } else { // else try to find the member in "chats"
            TreeNodeList chatsNode = getUIController().getNodeManagerModel()
                .getNotInFriendsTreeNodes();
            for (int i = 0; i < chatsNode.getChildCount(); i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) chatsNode
                    .getChildAt(i);
                if (member == node.getUserObject()) {
                    TreeNode[] path = new TreeNode[3];
                    path[0] = navTreeModel.getRootNode();
                    path[1] = chatsNode;
                    path[2] = node;
                    setSelectedPath(path);
                    return;
                }
            }
        }
        // Neither a friend nor in a chat:
        // select the connected member node
        if (getController().isVerbose()) {
            TreeNodeList otherNode = getUIController().getNodeManagerModel()
                .getConnectedTreeNode();
            for (int i = 0; i < otherNode.getChildCount(); i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) otherNode
                    .getChildAt(i);
                if (member == node.getUserObject()) {
                    TreeNode[] path = new TreeNode[3];
                    path[0] = navTreeModel.getRootNode();
                    path[1] = otherNode;
                    path[2] = node;
                    setSelectedPath(path);
                    return;
                }
            }
        }
    }

    /**
     * The path in the tree that was last expanded, use to restore the tree
     * state if a tree structure change was fired.
     */
    protected TreePath getLastExpandedPath() {
        return lastExpandedPath;
    }

    /**
     * Convience method for getSelectedItem. Returns the selected folder, or
     * null if nothing or not a folder is selected
     * 
     * @return the selected folder or null
     */
    public Folder getSelectedFolder() {
        Object item = getSelectedItem();
        if (item instanceof Folder) {
            return (Folder) item;
        }
        return null;
    }

    /**
     * Returns the selection model, contains the model for the selected item on
     * navtree. If you need information about the parent of the current
     * selection see <code>getSelectionParentModel</code>
     * 
     * @see #getSelectionParentModel()
     * @return
     */
    public SelectionModel getSelectionModel() {
        return selectionModel;
    }

    /**
     * @return The selected item in navtree
     */
    public Object getSelectedItem() {
        return selectionModel.getSelection();
    }

    /**
     * Returns the parent tree item of the selected item. Listens/Act on
     * selection changes by listening to our <code>SelectionModel</code>
     * 
     * @see #getSelectionModel()
     * @return
     */
    public Object getSelectionParent() {
        return selectionParent;
    }

    public void selectOverview() {
        setSelected(navTreeModel.getRootNode());
    }

    public void selectMyFolders() {
        TreeNode[] path = new TreeNode[2];
        path[0] = navTreeModel.getRootNode();
        path[1] = getUIController().getFolderRepositoryModel()
            .getMyFoldersTreeNode();
        setSelectedPath(path);
    }

    public void selectDownloads() {
        TreeNode[] path = new TreeNode[2];
        path[0] = navTreeModel.getRootNode();
        path[1] = getUIController().getTransferManagerModel()
            .getDownloadsTreeNode();
        setSelectedPath(path);
    }

    private void doubleClicked() {
        if (getSelectedItem() == RootNode.WEBSERVICE_NODE_LABEL) {
            try {
                BrowserLauncher.openURL(Constants.ONLINE_STORAGE_URL);
            } catch (IOException e) {
                log().error("Unable to open online storage in browser", e);
            }
        }
        Folder folder = getSelectedFolder();
        if (folder == null) {
            return;
        }
        if (folder.isPreviewOnly()) {
            PreviewToJoinPanel panel = new PreviewToJoinPanel(getController(), folder);
            panel.open();
        } else {
            File localBase = folder.getLocalBase();
            try {
                FileUtils.executeFile(localBase);
            } catch (IOException ioe) {
                log().error(ioe);
            }
        }
    }

    // Internal classes *******************************************************

    private final class NavTreeSelectionAdapater implements
        TreeSelectionListener
    {
        public void valueChanged(TreeSelectionEvent e) {
            TreePath selectionPath = e.getPath();
            if (logVerbose) {
                log().verbose(selectionPath.toString());
            }
            // First set parent of selection
            if (selectionPath.getPathCount() > 1) {
                selectionParent = UIUtil.getUserObject(selectionPath
                    .getPathComponent(selectionPath.getPathCount() - 2));
            } else {
                // Parent of selection empty
                selectionParent = null;
            }

            Object newSelection = UIUtil.getUserObject(selectionPath
                .getLastPathComponent());
            selectionModel.setSelection(newSelection);
            if (logVerbose) {
                log().verbose(
                    "Selection: " + selectionModel.getSelection()
                        + ", parent: " + selectionParent);
            }
        }
    }

    /**
     * Navtree listner, cares for selection and popup menus
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.11 $
     */
    private class NavTreeListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {

            if (e.getClickCount() == 2) {
                doubleClicked();
            }
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        private void showContextMenu(MouseEvent evt) {
            TreePath path = uiTree.getPathForLocation(evt.getX(), evt.getY());
            if (path == null) {
                return;
            }
            Object selection = UIUtil
                .getUserObject(path.getLastPathComponent());
            if (!path.getLastPathComponent().equals(getSelectedItem())) {
                // #668 - Do not scroll,
                // because the path is already visible.
                setSelectedTreePath(path, false);
            }

            if (selection instanceof Member) {
                // Have to build Member popup menu dynamically because it is
                // dependant on debugReports.
                JPopupMenu memberMenu = new JPopupMenu();
                memberMenu.add(new OpenChatAction(getController(),
                    getSelectionModel()));
                memberMenu.add(new ChangeFriendStatusAction(getController(),
                    getSelectionModel()));
                memberMenu.add(getUIController().getInviteUserAction());
                memberMenu.addSeparator();
                memberMenu.add(getUIController().getReconnectAction());

                Preferences pref = getController().getPreferences();
                boolean debugReportsEnabled = ConfigurationEntry.DEBUG_REPORTS
                    .getValueBoolean(getController());
                if (debugReportsEnabled
                    && pref.getBoolean(DebugPanel.showDebugReportsPrefKey,
                        false))
                {
                    // Show request debug only in debugReports mode set
                    memberMenu.add(getUIController().getRequestReportAction());
                }
                memberMenu.show(evt.getComponent(), evt.getX(), evt.getY());

            } else if (selection instanceof Folder) {
                // show menu
                Folder folder = (Folder) selection;
                if (folder.isPreviewOnly()) {
                    previewFolderMenu.show(evt.getComponent(), evt.getX(), evt
                        .getY());
                } else {
                    myFolderMenu.show(evt.getComponent(), evt.getX(), evt
                        .getY());
                }
            } else if (selection == getUIController().getNodeManagerModel()
                .getFriendsTreeNode())
            {
                friendsListMenu
                    .show(evt.getComponent(), evt.getX(), evt.getY());

                if (getController().isVerbose()) {
                    friendsListMenu.show(evt.getComponent(), evt.getX(), evt
                        .getY());
                } else {
                    log()
                        .warn(
                            "Not displaing friendlist/master user selection context menu");
                }
            } else if (selection instanceof Directory) {
                if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
                    directoryMenu.show(evt.getComponent(), evt.getX(), evt
                        .getY());
                }
            } else if (selection == getUIController()
                .getFolderRepositoryModel().getMyFoldersTreeNode())
            {
                myFoldersMenu.show(evt.getComponent(), evt.getX(), evt.getY());
            }
        }
    }

    /*
     * General ****************************************************************
     */

    public String toString() {
        return "Navigation tree";
    }

    /** Helper class, Opens the local folder on action * */
    private class OpenLocalFolder extends BaseAction {

        public OpenLocalFolder(Controller controller) {
            super("open_local_folder", controller);
        }

        /**
         * opens the folder currently in view in the operatings systems file
         * explorer
         */
        public void actionPerformed(ActionEvent e) {
            Object selection = getSelectedItem();
            if (selection instanceof Folder) {
                Folder folder = (Folder) selection;
                File localBase = folder.getLocalBase();
                try {
                    FileUtils.executeFile(localBase);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            } else if (selection instanceof Directory) {
                Directory directory = (Directory) selection;
                if (directory != null) {
                    Folder folder = directory.getRootFolder();
                    File localBase = folder.getLocalBase();
                    File path = new File(localBase.getAbsolutePath() + '/'
                        + directory.getPath());
                    while (!path.exists()) { // try finding the first path
                        // that
                        // exists
                        String pathStr = path.getAbsolutePath();
                        int index = pathStr.lastIndexOf(File.separatorChar);
                        if (index == -1) {
                            return;
                        }
                        path = new File(pathStr.substring(0, index));
                    }
                    try {
                        FileUtils.executeFile(path);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * class that handles the Drag and Drop TO the uiTree. will expand tree
     * items after a delay if drag above them
     */
    private class MyDropTargetListener implements DropTargetListener {
        private long timeEntered;
        private int delay = 500;
        private Object lastSelection;

        public void dragEnter(DropTargetDragEvent dtde) {
            Point location = dtde.getLocation();
            TreePath path = uiTree.getPathForLocation(location.x, location.y);
            if (path == null) {
                return;
            }
            Object selection = UIUtil
                .getUserObject(path.getLastPathComponent());

            if (selection instanceof Folder || selection instanceof Directory) {
                if (DragDropChecker.allowDropCopy(getController(), dtde)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    timeEntered = System.currentTimeMillis();
                    lastSelection = selection;
                } else {
                    dtde.rejectDrag();
                }
            } else {
                dtde.rejectDrag();
            }
        }

        public void dragExit(DropTargetEvent dte) {
            timeEntered = 0;
            lastSelection = null;
        }

        public void dragOver(DropTargetDragEvent dtde) {
            if (DragDropChecker.allowDropCopy(getController(), dtde)) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY);
            } else {
                dtde.rejectDrag();
            }

            Point location = dtde.getLocation();
            TreePath path = uiTree.getPathForLocation(location.x, location.y);
            if (path == null) {
                return;
            }
            Object selection = UIUtil
                .getUserObject(path.getLastPathComponent());

            if (!selection.equals(lastSelection)) {
                // restart the delay if different object if being draged
                // over
                timeEntered = System.currentTimeMillis();
                lastSelection = selection;
                return;
            }
            lastSelection = selection;
            if (System.currentTimeMillis() - timeEntered > delay) {
                // open current item if closed
                if (selection instanceof Folder) {
                    if (uiTree.isCollapsed(path)) {
                        uiTree.expandPath(path);
                    }
                    if (path.getLastPathComponent() != getSelectedItem()) {
                        setSelectedTreePath(path);
                    }
                    Folder f = (Folder) selection;
                    if (f.isPreviewOnly()) {
                        FolderPanel folderPanel = getUIController()
                            .getInformationQuarter().getPreviewFolderPanel();
                        folderPanel.setTab(FolderPanel.FILES_TAB);
                    } else {
                        FolderPanel folderPanel = getUIController()
                            .getInformationQuarter().getMyFolderPanel();
                        folderPanel.setTab(FolderPanel.FILES_TAB);
                    }
                } else if (selection instanceof Directory) {
                    if (uiTree.isCollapsed(path)) {
                        uiTree.expandPath(path);
                    }
                    if (path.getLastPathComponent() != getSelectedItem()) {
                        setSelectedTreePath(path);
                    }
                    Directory d = (Directory) selection;
                    Folder f = d.getRootFolder();
                    if (f.isPreviewOnly()) {
                        FolderPanel folderPanel = getUIController()
                            .getInformationQuarter().getPreviewFolderPanel();
                        folderPanel.setTab(FolderPanel.FILES_TAB);
                    } else {
                        FolderPanel folderPanel = getUIController()
                            .getInformationQuarter().getMyFolderPanel();
                        folderPanel.setTab(FolderPanel.FILES_TAB);
                    }
                }
            }
        }

        public void drop(DropTargetDropEvent dtde) {
            timeEntered = 0;
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                // test if there is a directory to drop onto
                if (lastSelection instanceof Folder) {
                    Folder f = (Folder) lastSelection;
                    FilesTab filesTab;
                    if (f.isPreviewOnly()) {
                        filesTab = getUIController().getInformationQuarter()
                            .getPreviewFolderPanel().getFilesTab();
                    } else {
                        filesTab = getUIController().getInformationQuarter()
                            .getMyFolderPanel().getFilesTab();
                    }
                    Directory targetDirectory = filesTab.getDirectoryTable()
                        .getDirectory();
                    if (targetDirectory != null) {
                        // test if not the same:
                        if (Arrays.asList(dtde.getCurrentDataFlavors())
                            .contains(Directory.getDataFlavor()))
                        {
                            try {
                                Directory sourceDir = (Directory) dtde
                                    .getTransferable().getTransferData(
                                        Directory.getDataFlavor());
                                if (sourceDir == targetDirectory) {
                                    dtde.dropComplete(false);
                                    return;
                                }
                            } catch (UnsupportedFlavorException e) {
                                log().error(e);
                            } catch (IOException ioe) {
                                log().error(ioe);
                            }
                        }
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);
                        if (filesTab.drop(dtde.getTransferable())) {
                            dtde.dropComplete(true);
                        } else {
                            dtde.dropComplete(false);
                        }
                    }
                }
            }
            dtde.dropComplete(false);
        }

        public void dropActionChanged(DropTargetDragEvent dtde) {

        }

    }
}