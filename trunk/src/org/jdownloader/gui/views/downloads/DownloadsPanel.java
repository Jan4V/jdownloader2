package org.jdownloader.gui.views.downloads;

import java.awt.Color;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadControllerEvent;
import jd.controlling.downloadcontroller.DownloadControllerListener;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.laf.LookAndFeelController;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class DownloadsPanel extends SwitchPanel implements DownloadControllerListener, GenericConfigEventListener<Boolean> {

    /**
     * 
     */
    private static final long   serialVersionUID = -2610465878903778445L;
    private DownloadsTable      table;
    private JScrollPane         tableScrollPane;
    private DownloadsTableModel tableModel;
    private ScheduledFuture<?>  timer            = null;
    private BottomBar           bottomBar;
    private HeaderScrollPane    sidebarScrollPane;
    private DownloadViewSidebar sidebar;

    public DownloadsPanel() {
        super(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]", "[grow, fill]2[]"));
        tableModel = new DownloadsTableModel();
        table = new DownloadsTable(tableModel);
        tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(null);
        bottomBar = new BottomBar(table);

        layoutComponents();

        GraphicalUserInterfaceSettings.DOWNLOAD_VIEW_SIDEBAR_ENABLED.getEventSender().addListener(this);

        GraphicalUserInterfaceSettings.DOWNLOAD_VIEW_SIDEBAR_TOGGLE_BUTTON_ENABLED.getEventSender().addListener(this);
        GraphicalUserInterfaceSettings.DOWNLOAD_VIEW_SIDEBAR_VISIBLE.getEventSender().addListener(this);

    }

    private void layoutComponents() {
        if (GraphicalUserInterfaceSettings.CFG.isDownloadViewSidebarEnabled() && GraphicalUserInterfaceSettings.CFG.isDownloadViewSidebarVisible()) {

            if (sidebarScrollPane == null) {
                createSidebar();
            }
            this.add(tableScrollPane, "pushx,growx");
            add(sidebarScrollPane, "width 240!");
        } else {
            this.add(tableScrollPane, "pushx,growx,spanx");

        }

        add(bottomBar, "spanx,height 24!");
    }

    private void createSidebar() {
        sidebar = new DownloadViewSidebar(table);

        sidebarScrollPane = new HeaderScrollPane(sidebar) {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;
            // protected int getHeaderHeight() {
            // return (int)
            // table.getTableHeader().getPreferredSize().getHeight();
            // }
        };

        // ScrollPaneUI udi = sp.getUI();
        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();
        // LayoutManager lm = sp.getLayout();

        if (c >= 0) {
            sidebarScrollPane.setBackground(new Color(c));
            sidebarScrollPane.setOpaque(true);

        }
        sidebarScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        sidebarScrollPane.setColumnHeaderView(new DownloadViewSideBarHeader());
        // ExtButton bt = new ExtButton(new AppAction() {
        // {
        // setSmallIcon(NewTheme.I().getIcon("close", -1));
        // setToolTipText(_GUI._.LinkGrabberSideBarHeader_LinkGrabberSideBarHeader_object_());
        // }
        //
        // public void actionPerformed(ActionEvent e) {
        // GraphicalUserInterfaceSettings.LINKGRABBER_SIDEBAR_ENABLED.setValue(false);
        // }
        // });
        //
        // sidebarScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER,
        // bt);
        // LinkFilterSettings.LINKGRABBER_QUICK_SETTINGS_VISIBLE.getEventSender().addListener(new
        // GenericConfigEventListener<Boolean>() {
        //
        // public void onConfigValidatorError(KeyHandler<Boolean> keyHandler,
        // Boolean invalidValue, ValidationException validateException) {
        // }
        //
        // public void onConfigValueModified(KeyHandler<Boolean> keyHandler,
        // Boolean newValue) {
        //
        // if (Boolean.TRUE.equals(newValue)) {
        // SwingUtilities.invokeLater(new Runnable() {
        //
        // public void run() {
        // sidebarScrollPane.getVerticalScrollBar().setValue(sidebarScrollPane.getVerticalScrollBar().getMaximum());
        // }
        // });
        //
        // }
        // }
        // });

    }

    @Override
    protected void onShow() {
        tableModel.recreateModel(false);
        synchronized (this) {
            if (timer != null) timer.cancel(false);
            timer = tableModel.getThreadPool().scheduleWithFixedDelay(new Runnable() {

                public void run() {
                    tableModel.refreshModel();
                }

            }, 250, 1000, TimeUnit.MILLISECONDS);
        }
        DownloadController.getInstance().addListener(this);
        table.requestFocusInWindow();
    }

    @Override
    protected void onHide() {
        synchronized (this) {
            if (timer != null) {
                timer.cancel(false);
                timer = null;
            }
        }
        DownloadController.getInstance().removeListener(this);
    }

    public void onDownloadControllerEvent(DownloadControllerEvent event) {
        switch (event.getType()) {
        case REFRESH_STRUCTURE:
        case REMOVE_CONTENT:
            tableModel.recreateModel();
            break;
        case REFRESH_DATA:
            tableModel.refreshModel();
            break;
        }
    }

    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (!newValue && keyHandler == GraphicalUserInterfaceSettings.DOWNLOAD_VIEW_SIDEBAR_VISIBLE) {
            JDGui.help(_GUI._.LinkGrabberPanel_onConfigValueModified_title_(), _GUI._.LinkGrabberPanel_onConfigValueModified_msg_(), NewTheme.I().getIcon("warning_green", 32));

        }
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                removeAll();
                layoutComponents();

                revalidate();
            }
        };
    }
}
