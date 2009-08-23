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

package jd.gui.swing.jdgui.views.linkgrabberview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import jd.config.Configuration;
import jd.controlling.DownloadController;
import jd.controlling.PasswordListController;
import jd.gui.swing.components.ComboBrowseFile;
import jd.gui.swing.components.JDCollapser;
import jd.gui.swing.components.JDFileChooser;
import jd.gui.swing.components.JDTextField;
import jd.gui.swing.jdgui.views.LinkgrabberView;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class LinkGrabberFilePackageInfo extends JDCollapser implements ActionListener, FocusListener {

    private static final long serialVersionUID = 5410296068527460629L;

    private ComboBrowseFile brwSaveTo;

    private JDTextField txtComment;

    private JDTextField txtName;

    private JDTextField txtPassword;

    private JCheckBox chbExtract;

    private JCheckBox chbUseSubdirectory;

    private LinkGrabberFilePackage fp = null;



    public LinkGrabberFilePackageInfo() {
        buildGui();
        fp = null;
        this.menutitle.setText(JDL.L("gui.linkgrabber.packagetab.title", "File package"));
    }

    public void setPackage(LinkGrabberFilePackage fp) {
        if (this.fp != null && this.fp == fp) {
            update();
            return;
        }
        this.fp = fp;
        if (this.fp != null) {
            update();
        }
    }

    public void update() {
        if (fp == null) return;
        /*
         * wichtig: die set funktionen lösen eine action aus , welche ansonsten
         * wiederum ein updatevent aufrufen würden
         */
  
        txtName.setText(fp.getName());
        txtComment.setText(fp.getComment());
         txtPassword.setText(fp.getPassword());
        brwSaveTo.setText(fp.getDownloadDirectory());
        chbExtract.setSelected(fp.isExtractAfterDownload());
       chbUseSubdirectory.setSelected(fp.useSubDir());
        /* neuzeichnen */
        revalidate();
       
    }

    public LinkGrabberFilePackage getPackage() {
        return fp;
    }

    private void buildGui() {
        txtName = new JDTextField(true);
        txtName.addActionListener(this);
        txtName.addFocusListener(this);
        brwSaveTo = new ComboBrowseFile("DownloadSaveTo");
        brwSaveTo.setEditable(true);
        brwSaveTo.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
        brwSaveTo.setText(JDUtilities.getConfiguration().getDefaultDownloadDirectory());
        brwSaveTo.addActionListener(this);

        txtPassword = new JDTextField(true);
        txtPassword.addActionListener(this);

        txtComment = new JDTextField(true);
        txtComment.addActionListener(this);

        chbExtract = new JCheckBox(JDL.L("gui.linkgrabber.packagetab.chb.extractAfterdownload", "Extract"));
        chbExtract.setSelected(true);
        chbExtract.setHorizontalTextPosition(SwingConstants.LEFT);
        chbExtract.addActionListener(this);

        chbUseSubdirectory = new JCheckBox(JDL.L("gui.linkgrabber.packagetab.chb.useSubdirectory", "Use Subdirectory"));
        chbUseSubdirectory.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false));
        chbUseSubdirectory.setHorizontalTextPosition(SwingConstants.LEFT);
        chbUseSubdirectory.addActionListener(this);

        content.setLayout(new MigLayout("ins 10, wrap 3", "[]10[grow,fill][]", "[]5[]5[]5[]"));

        content.add(new JLabel(JDL.L("gui.linkgrabber.packagetab.lbl.name", "Paketname")));
        content.add(txtName, "span 2");
        content.add(new JLabel(JDL.L("gui.linkgrabber.packagetab.lbl.saveto", "Speichern unter")));
        content.add(brwSaveTo.getInput(), "gapright 10, growx");
        content.add(brwSaveTo.getButton(), "pushx,growx");

        content.add(new JLabel(JDL.L("gui.linkgrabber.packagetab.lbl.password", "Archivpasswort")), "newline");

        content.add(txtPassword, " gapright 10, growx");
        content.add(chbExtract, "alignx right");
        content.add(new JLabel(JDL.L("gui.linkgrabber.packagetab.lbl.comment", "Kommentar")));
        content.add(txtComment, "gapright 10, growx");
        content.add(chbUseSubdirectory, "alignx right");
    }

    public void actionPerformed(ActionEvent e) {
        if (fp == null ) return;
        if(e.getSource()==txtName)fp.setName(txtName.getText());
        if(e.getSource()==brwSaveTo) fp.setDownloadDirectory(brwSaveTo.getText());
        if(e.getSource()==txtComment) fp.setComment(txtComment.getText());
        if(e.getSource()==txtPassword)fp.setPassword(txtPassword.getText());
        if(e.getSource()==chbExtract) fp.setExtractAfterDownload(chbExtract.isSelected());
        if(e.getSource()==chbUseSubdirectory)fp.setUseSubDir(chbUseSubdirectory.isSelected());
        DownloadController.getInstance().fireDownloadLinkUpdate(fp.get(0));
    }

    // @Override
    public void onShow() {
        update();
    }

    public void onHideSave() {
       
        PasswordListController.getInstance().addPassword(txtPassword.getText());
        fp.setName(txtName.getText());
        fp.setComment(txtComment.getText());
        fp.setPassword(txtPassword.getText());
        fp.setDownloadDirectory(brwSaveTo.getText());
     
    }

    // @Override
    public void onHide() {
        if (this.fp == null) return;
        onHideSave();
        fp = null;
    }

    public void focusGained(FocusEvent e) {
    }

    public void focusLost(FocusEvent e) {
        if(e.getSource()==txtName)fp.setName(txtName.getText());
        if(e.getSource()==brwSaveTo) fp.setDownloadDirectory(brwSaveTo.getText());
        if(e.getSource()==txtComment) fp.setComment(txtComment.getText());
        if(e.getSource()==txtPassword)fp.setPassword(txtPassword.getText());
        if(e.getSource()==chbExtract) fp.setExtractAfterDownload(chbExtract.isSelected());
        if(e.getSource()==chbUseSubdirectory)fp.setUseSubDir(chbUseSubdirectory.isSelected());
        DownloadController.getInstance().fireDownloadLinkUpdate(fp.get(0));
    }

    @Override
    public void onClosed() {
        LinkgrabberView.getInstance().setInfoPanel(null);
    }

}
