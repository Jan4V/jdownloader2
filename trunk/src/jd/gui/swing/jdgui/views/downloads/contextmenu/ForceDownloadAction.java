package jd.gui.swing.jdgui.views.downloads.contextmenu;


 import org.jdownloader.gui.translate.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.DownloadWatchDog;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.utils.locale.JDL;

public class ForceDownloadAction extends ContextMenuAction {

    private static final long             serialVersionUID = 7107840091963427544L;

    private final ArrayList<DownloadLink> links;

    public ForceDownloadAction(final ArrayList<DownloadLink> links) {
        this.links = links;
        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.next";
    }

    @Override
    protected String getName() {
        return T._.gui_table_contextmenu_tryforce() + " (" + links.size() + ")";
    }

    @Override
    public boolean isEnabled() {
        return !links.isEmpty() && DownloadWatchDog.getInstance().getStateMonitor().isState(DownloadWatchDog.IDLE_STATE, DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.STOPPED_STATE);
    }

    public void actionPerformed(ActionEvent e) {
        DownloadWatchDog.getInstance().forceDownload(links);
    }

}