//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.skins.simple.components.DownloadView;

import java.awt.Component;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import jd.controlling.DownloadController;
import jd.gui.skins.simple.SimpleGUI;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class TreeTableTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 2560352681437669412L;
    private static final int DRAG_LINKS = 1;
    private static final int DRAG_PACKAGES = 2;

    private Object draggingObjects = null;
    private int draggingType = 0;

    public boolean isDragging = false;
    private DownloadTreeTable treeTable;

    public TreeTableTransferHandler(DownloadTreeTable downloadTreeTable) {
        treeTable = downloadTreeTable;
    }

    @SuppressWarnings("unchecked")
    // @Override
    public boolean canImport(TreeTableTransferHandler.TransferSupport info) {
        if (isDragging) {
            // ACHTUNG 1.6!!!
            // ON_OR_INSERT_ROW
            // ((javax.swing.JTable.DropLocation)
            // info.getDropLocation()).isInsertColumn();
            // ((javax.swing.JTable.DropLocation)
            // info.getDropLocation()).isInsertRow();

            if (draggingObjects == null) return false;
            int row = ((JTable.DropLocation) info.getDropLocation()).getRow();
            TreePath current = treeTable.getPathForRow(row);
            if (current == null) return false;
            switch (draggingType) {
            case DRAG_LINKS:
                ArrayList<DownloadLink> downloadLinks = (ArrayList<DownloadLink>) draggingObjects;
                if (current.getLastPathComponent() instanceof DownloadLink && downloadLinks.contains(current.getLastPathComponent())) return false;
                break;
            case DRAG_PACKAGES:
                ArrayList<FilePackage> packages = (ArrayList<FilePackage>) draggingObjects;
                if (current.getLastPathComponent() instanceof FilePackage && packages.contains(current.getLastPathComponent())) return false;
                if (current.getLastPathComponent() instanceof DownloadLink && packages.contains(((DownloadLink) current.getLastPathComponent()).getFilePackage())) return false;
                break;
            default:
                return false;
            }
            return true;
        } else {
            if (info.isDataFlavorSupported(DataFlavor.stringFlavor)) return true;
            if (info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return true;
            return false;
        }
    }

    // @Override
    protected Transferable createTransferable(JComponent c) {
        isDragging = true;
        ArrayList<FilePackage> packages = treeTable.getSelectedFilePackages();
        ArrayList<DownloadLink> downloadLinks = treeTable.getSelectedDownloadLinks();
       
       Point point = MouseInfo.getPointerInfo().getLocation();   
       point.x -= (treeTable.getLocationOnScreen().x);
       point.y -= (treeTable.getLocationOnScreen().y);
        int row = treeTable.rowAtPoint( point);
        TreePath current = treeTable.getPathForRow(row);
        if (current.getLastPathComponent() instanceof FilePackage) {
            this.draggingObjects = packages;
            this.draggingType = DRAG_PACKAGES;
        } else {
            this.draggingObjects = downloadLinks;
            this.draggingType = DRAG_LINKS;
        }
        return new StringSelection("JDAFFE");
    }

    // @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        isDragging = false;
    }

    @SuppressWarnings("unchecked")
    private boolean drop(int row, Point point) {
        if (!isDragging) return false;
        final TreePath current = treeTable.getPathForRow(row);
        if (current == null) return false;
        JPopupMenu popup = new JPopupMenu();
        JMenuItem m;
        synchronized (DownloadController.getInstance().getPackages()) {
            switch (draggingType) {
            case DRAG_LINKS:
                final ArrayList<DownloadLink> downloadLinks = (ArrayList<DownloadLink>) draggingObjects;

                if (current.getLastPathComponent() instanceof FilePackage) {
                    /* Links in Package */
                    String name = ((FilePackage) current.getLastPathComponent()).getName();
                    popup.add(m = new JMenuItem(JDLocale.LF("gui.table.draganddrop.insertinpackagestart", "In Paket '%s' am Anfang einfügen", name)));
                    m.setIcon(JDTheme.II("gui.images.go_top", 16, 16));
                    m.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            synchronized (DownloadController.getInstance().getPackages()) {
                                FilePackage fp = ((FilePackage) current.getLastPathComponent());
                                fp.addLinksAt(downloadLinks, 0);
                            }
                        }
                    });

                    popup.add(m = new JMenuItem(JDLocale.LF("gui.table.draganddrop.insertinpackageend", "In Paket '%s' am Ende einfügen", name)));
                    m.setIcon(JDTheme.II("gui.images.go_bottom", 16, 16));
                    m.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            synchronized (DownloadController.getInstance().getPackages()) {
                                FilePackage fp = ((FilePackage) current.getLastPathComponent());
                                fp.addLinksAt(downloadLinks, fp.size());
                            }
                        }
                    });

                } else if (current.getLastPathComponent() instanceof DownloadLink) {
                    /* Links in Links */
                    String name = ((DownloadLink) current.getLastPathComponent()).getName();
                    popup.add(m = new JMenuItem(JDLocale.LF("gui.table.draganddrop.before", "Vor '%s' ablegen", name)));
                    m.setIcon(JDTheme.II("gui.images.go_top", 16, 16));
                    m.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            synchronized (DownloadController.getInstance().getPackages()) {
                                FilePackage fp = ((DownloadLink) current.getLastPathComponent()).getFilePackage();
                                fp.addLinksAt(downloadLinks, fp.indexOf((DownloadLink) current.getLastPathComponent()) - 1);
                            }
                        }
                    });

                    popup.add(m = new JMenuItem(JDLocale.LF("gui.table.draganddrop.after", "Nach '%s' ablegen", name)));
                    m.setIcon(JDTheme.II("gui.images.go_bottom", 16, 16));
                    m.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            synchronized (DownloadController.getInstance().getPackages()) {
                                FilePackage fp = ((DownloadLink) current.getLastPathComponent()).getFilePackage();
                                fp.addLinksAt(downloadLinks, fp.indexOf((DownloadLink) current.getLastPathComponent()) + 1);
                            }
                        }
                    });
                }
                break;
            case DRAG_PACKAGES:
                final ArrayList<FilePackage> packages = (ArrayList<FilePackage>) draggingObjects;
                final FilePackage fp;
                final String name;
                if (current.getLastPathComponent() instanceof FilePackage) {
                    name = ((FilePackage) current.getLastPathComponent()).getName();
                    fp = ((FilePackage) current.getLastPathComponent());
                } else if (current.getLastPathComponent() instanceof DownloadLink) {
                    name = ((DownloadLink) current.getLastPathComponent()).getFilePackage().getName();
                    fp = ((DownloadLink) current.getLastPathComponent()).getFilePackage();
                } else
                    return false;

                popup.add(m = new JMenuItem(JDLocale.LF("gui.table.draganddrop.movepackagebefore", "Vor Paket '%s' einfügen", name)));
                m.setIcon(JDTheme.II("gui.images.go_top", 16, 16));
                m.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        DownloadController.getInstance().addAllAt(packages, DownloadController.getInstance().indexOf(fp));
                    }

                });

                popup.add(m = new JMenuItem(JDLocale.LF("gui.table.draganddrop.movepackageend", "Nach Paket '%s' einfügen", name)));
                m.setIcon(JDTheme.II("gui.images.go_bottom", 16, 16));
                m.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        DownloadController.getInstance().addAllAt(packages, DownloadController.getInstance().indexOf(fp) + 1);
                    }
                });

                break;
            default:
                return false;
            }
        }
        popup.add(m = new JMenuItem(JDLocale.L("gui.btn_cancel", "Abbrechen")));
        m.setIcon(JDTheme.II("gui.images.unselected", 16, 16));
        popup.show(treeTable, point.x, point.y);
        return true;
    }

    // @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @SuppressWarnings("unchecked")
    // @Override
    public boolean importData(TreeTableTransferHandler.TransferSupport info) {
        try {
            Transferable tr = info.getTransferable();
            if (isDragging) {
                Point p = ((JTable.DropLocation) info.getDropLocation()).getDropPoint();
                int row = ((JTable.DropLocation) info.getDropLocation()).getRow();
                return drop(row, p);
            } else if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List list = (List) tr.getTransferData(DataFlavor.javaFileListFlavor);
                for (int t = 0; t < list.size(); t++) {
                    JDUtilities.getController().loadContainerFile((File) list.get(t));
                }
            } else if (tr.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String files = (String) tr.getTransferData(DataFlavor.stringFlavor);
                String linuxfiles[] = new Regex(files, "file://(.*?)\n").getColumn(0);
                if (linuxfiles != null && linuxfiles.length > 0) {
                    for (String file : linuxfiles) {
                        JDUtilities.getController().loadContainerFile(new File(file));
                    }
                } else
                    JDUtilities.getController().distributeLinks(files);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}