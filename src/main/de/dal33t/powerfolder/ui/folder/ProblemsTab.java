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
package de.dal33t.powerfolder.ui.folder;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractCellEditor;
import javax.swing.AbstractListModel;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.FilenameProblem;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.TextLinesPanelBuilder;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Displays the filenameproblems of a Folder
 *      *** DISABLED ***
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
public class ProblemsTab extends PFUIComponent implements FolderTab {

    private String[] columns = new String[]{
        Translation.getTranslation("filelist.name"),
        Translation.getTranslation("general.description"),
        Translation.getTranslation("filenameproblemhandler.solution")};

    private static final int FILENAME_COLUMN = 0;
    private static final int PROBLEM_COLUMN = 1;
    private static final int SOLUTION_COLUMN = 2;

    private Map<FileInfo, List<FilenameProblem>> problems;
    private List<FileInfo> problemList;

    private JTable table;
    private ProblemTableModel problemTableModel;
    private JScrollPane tablePane;
    private JPanel panel;

    private Folder folder;

    public ProblemsTab(Controller controller) {
        super(controller);
    }

    public String getTitle() {
        return Translation.getTranslation("folderpanel.problemstab.title");
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
            FormLayout layout = new FormLayout("fill:pref:grow", "fill:pref:grow");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
            builder.add(tablePane, cc.xy(1, 1));
            setColumnSizes(table);
            panel = builder.getPanel();
        }
        return panel;
    }

    private void initComponents() {
        problemTableModel = new ProblemTableModel();
        table = new JTable(problemTableModel);
        table.setDefaultRenderer(Object.class, new ProblemTableCellRenderer());
        table.setDefaultEditor(Object.class, new ProblemTableCellRenderer());
        tablePane = new JScrollPane(table);
        UIUtil.whiteStripTable(table);
        UIUtil.setZeroHeight(tablePane);
        UIUtil.removeBorder(tablePane);
    }

    private void setColumnSizes(JTable table) {
        table.setRowHeight(100);
        // otherwise the table header may not be visible:
        table.getTableHeader().setPreferredSize(new Dimension(600, 20));
        TableColumn column = table.getColumn(table.getColumnName(0));

        column.setPreferredWidth(150);
        column = table.getColumn(table.getColumnName(1));
        column.setPreferredWidth(500);
        column = table.getColumn(table.getColumnName(2));
        column.setPreferredWidth(150);

    }

    void update() {
        if (problemTableModel != null) {
            problemTableModel.fireTableDataChanged();
        }
    }
    
    public void setFolder(Folder folder) {
        this.folder = folder;
        problems = folder.getProblemFiles();
        if (problems != null) {
            problemList = new ArrayList<FileInfo>(problems.keySet());
        }
        update();
    }

    private class ProblemTableModel extends AbstractTableModel {

        public int getColumnCount() {
            return columns.length;
        }

        public int getRowCount() {
            if (problems == null) {
                return 0;
            }
            return problems.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return problems;
        }

        @Override
        public String getColumnName(int columnIndex)
        {
            return columns[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            // use an editor because else the events are not passed to the
            // scrollpane or button
            return (columnIndex == PROBLEM_COLUMN || columnIndex == SOLUTION_COLUMN);
        }

    }

    private class ProblemTableCellRenderer extends AbstractCellEditor implements
        TableCellRenderer, TableCellEditor
    {

        private Map<FileInfo, JPanel> solutionsPanelCache;

        public ProblemTableCellRenderer() {
            solutionsPanelCache = new HashMap<FileInfo, JPanel>();
        }

        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column)
        {
            FileInfo fileInfo = problemList.get(row);
            switch (column) {
                case FILENAME_COLUMN : {
                    JLabel label = new JLabel(fileInfo.getName());
                    label.setToolTipText(fileInfo.getName());
                    return label;
                }
                case PROBLEM_COLUMN : {
                    return getProblemComponent(fileInfo);

                }
                case SOLUTION_COLUMN : {

                    return getSolutionComponent(fileInfo);
                }
            }
            return null;
        }

        private class ProblemJList extends JList {
            public ProblemJList(List<FilenameProblem> list) {
                super(new MyListModel(list));
                setCellRenderer(new MyCellRenderer());
            }
        }

        private class MyListModel extends AbstractListModel {
            List<FilenameProblem> list;

            public MyListModel(List<FilenameProblem> list) {
                this.list = list;
            }

            public Object getElementAt(int index) {
                return list.get(index);
            }

            public int getSize() {
                return list.size();
            }
        }

        private class MyCellRenderer implements ListCellRenderer {

            public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus)
            {
                JPanel panel = TextLinesPanelBuilder.createTextPanel(
                    (String) value, 10);

                return panel;
            }

        }

        private Component getSolutionComponent(FileInfo fileInfo) {
            if (solutionsPanelCache.containsKey(fileInfo)) {
                return solutionsPanelCache.get(fileInfo);
            }

            FormLayout layout = new FormLayout("pref", "pref, pref, pref");

            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            JRadioButton nothingRadioButton = new JRadioButton("nothing");
            JRadioButton renameRadioButton = new JRadioButton("rename to ...");
            JRadioButton addToIgnoreRadioButton = new JRadioButton(
                "add to ignore");
            nothingRadioButton.setSelected(true);

            nothingRadioButton.setBackground(Color.WHITE);
            renameRadioButton.setBackground(Color.WHITE);
            addToIgnoreRadioButton.setBackground(Color.WHITE);

            ButtonGroup group = new ButtonGroup();
            group.add(nothingRadioButton);
            group.add(renameRadioButton);
            group.add(addToIgnoreRadioButton);

            builder.add(nothingRadioButton, cc.xy(1, 1));
            builder.add(renameRadioButton, cc.xy(1, 2));
            builder.add(addToIgnoreRadioButton, cc.xy(1, 3));
            JPanel panel = builder.getPanel();
            panel.setBackground(Color.WHITE);
            solutionsPanelCache.put(fileInfo, panel);
            return panel;
        }

        private Component getProblemComponent(FileInfo fileInfo) {
            JList jList = new ProblemJList(problems.get(fileInfo));
            List<FilenameProblem> problemDesctiptions = problems.get(fileInfo);
            String tooltip = "";
            String line = "";
            for (FilenameProblem problem : problemDesctiptions) {
                tooltip += line + problem.describeProblem();
                line = "<hr>";
            }
            int index = tooltip.indexOf("\n");
            while (index != -1) {
                String before = tooltip.substring(0, index);
                String after = tooltip.substring(index + 1, tooltip.length());
                tooltip = before + "<br>" + after;
                index = tooltip.indexOf("\n");
            }
            tooltip = "<html>" + tooltip + "</html>";
            jList.setToolTipText(tooltip);
            jList.setSize(jList.getPreferredSize());
            JScrollPane pane = new JScrollPane(jList);
            pane.setToolTipText(tooltip);
            pane
                .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            pane
                .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            return pane;
        }

        public Object getCellEditorValue() {
            return null;
        }

        public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected, int row, int column)
        {
            if (!(column == PROBLEM_COLUMN || column == SOLUTION_COLUMN)) {
                throw new IllegalStateException(
                    "only problem and solution column use an editor");
            }
            switch (column) {
                case PROBLEM_COLUMN : {
                    FileInfo fileInfo = problemList.get(row);
                    return getProblemComponent(fileInfo);
                }
                case SOLUTION_COLUMN : {
                    FileInfo fileInfo = problemList.get(row);
                    return getSolutionComponent(fileInfo);
                }
            }
            return null;
        }
    }
}
