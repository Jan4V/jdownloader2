//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.optional.customizer;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import jd.PluginWrapper;
import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberPackagingEvent;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.actions.ToolBarAction.Types;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", id = "packagecustomizer", hasGui = true, interfaceversion = 5)
public class JDPackageCustomizer extends PluginOptional implements LinkGrabberPackagingEvent {

    private static final String JDL_PREFIX = "jd.plugins.optional.customizer.JDPackageCustomizer.";

    private static final String PROPERTY_ENABLE = "ENABLE";
    public static final String PROPERTY_SETTINGS = "SETTINGS";

    private LinkGrabberController ctrl;
    private final ImageIcon customIconOpen;
    private final ImageIcon customIconClose;

    private CustomizerView view;

    private MenuAction enableAction;
    private MenuAction showAction;

    public JDPackageCustomizer(PluginWrapper wrapper) {
        super(wrapper);
        
        // TODO: create & use 'correct' icons
        customIconOpen = JDTheme.II("gui.images.package_open_error_tree", 16, 16);
        customIconClose = JDTheme.II("gui.images.package_closed_error_tree", 16, 16);
    }

    @Override
    public String getIconKey() {
        return "gui.images.package_opened";
    }

    @Override
    public boolean initAddon() {
        ctrl = LinkGrabberController.getInstance();
        ctrl.setCustomizedPackager(this);

        enableAction = new MenuAction(getWrapper().getID(), 1);
        enableAction.setActionListener(this);
        enableAction.setTitle(JDL.L(JDL_PREFIX + "enabled", "Enable Customizer"));
        enableAction.setSelected(getPluginConfig().getBooleanProperty(PROPERTY_ENABLE, true));

        showAction = new MenuAction(getWrapper().getID(), 0);
        showAction.setActionListener(this);
        showAction.setTitle(JDL.L(JDL_PREFIX + "settings", "Show Settings-GUI"));
        showAction.setSelected(false);

        logger.info("Customizer OK");
        return true;
    }

    @Override
    public void onExit() {
        ctrl.setCustomizedPackager(null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == enableAction) {
            getPluginConfig().setProperty(PROPERTY_ENABLE, enableAction.isSelected());
            getPluginConfig().save();
        } else if (e.getSource() == showAction) {
            if (showAction.isSelected()) {
                showGui();
            } else {
                view.close();
            }
        }
    }

    private void showGui() {
        if (view == null) {
            view = new CustomizerView();
            view.getBroadcaster().addListener(new SwitchPanelListener() {

                @Override
                public void onPanelEvent(SwitchPanelEvent event) {
                    if (event.getID() == SwitchPanelEvent.ON_REMOVE) showAction.setSelected(false);
                }

            });
            view.setContent(new CustomizerGui(getPluginConfig()));
        }
        showAction.setSelected(true);
        JDGui.getInstance().setContent(view);
    }

    @Override
    public void setGuiEnable(boolean b) {
        if (b) {
            showGui();
        } else {
            if (view != null) view.close();
        }
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();
        menu.add(enableAction);
        menu.add(new MenuAction(Types.SEPARATOR));
        menu.add(showAction);
        return menu;
    }

    public void attachToPackagesFirstStage(DownloadLink link) {
        ctrl.attachToPackagesFirstStageInternal(link);
    }

    public void attachToPackagesSecondStage(DownloadLink link) {
        if (enableAction.isSelected()) {
            ArrayList<CustomizeSetting> settings = getPluginConfig().getGenericProperty(PROPERTY_SETTINGS, new ArrayList<CustomizeSetting>());
            for (CustomizeSetting setting : settings) {
                if (setting.isEnabled() && setting.matches(link.getName())) {
                    logger.info("Customizer: Using customization of filepackage for link " + link.getName());
                    if (ctrl.isExtensionFiltered(link)) {
                        ctrl.getFilterPackage().add(link);
                        return;
                    }

                    String packageName = setting.getPackageName();
                    LinkGrabberFilePackage fp;
                    if (packageName == null || packageName.equals("")) {
                        fp = ctrl.getGeneratedPackage(link);
                    } else {
                        fp = ctrl.getFPwithName(packageName);
                        if (fp == null) fp = new LinkGrabberFilePackage(packageName, ctrl);
                    }
                    fp.setCustomIcon(customIconOpen, customIconClose);
                    fp.setExtractAfterDownload(setting.isExtract());
                    fp.setDownloadDirectory(setting.getDownloadDir());
                    fp.setUseSubDir(setting.isUseSubDirectory());
                    fp.setPassword(setting.getPassword());
                    fp.add(link);
                    link.setPriority(setting.getPriority());

                    setting.incMatchCount();
                    return;
                }
            }
        }
        LinkGrabberController.getInstance().attachToPackagesSecondStageInternal(link);
    }

}
