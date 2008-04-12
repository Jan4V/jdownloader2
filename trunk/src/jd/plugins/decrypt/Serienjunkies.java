//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypt;

import jd.plugins.DownloadLink;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.utils.UTILITIES;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.Form;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Serienjunkies extends PluginForDecrypt {
    private static final String host               = "Serienjunkies.org";

    private String              version            = "4.5.0.0";

    private Pattern             patternCaptcha     = null;

    private boolean             next               = false;

    private static final int    saveScat           = 1;

    private static final int    sCatNoThing        = 0;

    private static final int    sCatNewestDownload = 1;

    private static final int    sCatGrabb          = 2;

    private static final String PATTERN_FOR_CAPTCHA_BOT = "Du hast zu oft das Captcha falsch eingegeben";

    private static int[]        useScat            = new int[] { 0, 0 };

    private boolean             scatChecked        = false;

    private JComboBox           methods;

    private JCheckBox           checkScat;

    private String              dynamicCaptcha     = "<FORM ACTION=\".*?\" METHOD=\"post\"(?s).*?(?-s)<INPUT TYPE=\"HIDDEN\" NAME=\"s\" VALUE=\"([\\w]*)\">(?s).*?(?-s)<IMG SRC=\"([^\"]*)\"";

    public Serienjunkies() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigElements();

        default_password.add("serienjunkies.dl.am");
        default_password.add("serienjunkies.org");

    }

    /*
     * Diese wichtigen Infos sollte man sich unbedingt durchlesen
     */
    @Override
    public String getCoder() {
        // von coa gefixed
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return host + "-" + version;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    private void sCatDialog() {
        if (scatChecked || useScat[1] == saveScat) return;
        new Dialog(((SimpleGUI) JDUtilities.getGUI()).getFrame()) {

            /**
             * 
             */
            private static final long serialVersionUID = -5144850223169000644L;

            void init() {
                setLayout(new BorderLayout());
                setModal(true);
                setTitle(JDLocale.L("Plugins.SerienJunkies.CatDialog.title", "SerienJunkies ::CAT::"));
                setAlwaysOnTop(true);
                setLocation(20, 20);
                JPanel panel = new JPanel(new GridBagLayout());
                final class meth {
                    public int    var;

                    public String name;

                    public meth(String name, int var) {
                        this.name = name;
                        this.var = var;
                    }

                    @Override
                    public String toString() {
                        // TODO Auto-generated method stub
                        return name;
                    }
                }
                ;
                addWindowListener(new WindowListener() {

                    public void windowActivated(WindowEvent e) {
                    // TODO Auto-generated method stub

                    }

                    public void windowClosed(WindowEvent e) {
                    // TODO Auto-generated method stub

                    }

                    public void windowClosing(WindowEvent e) {
                        useScat = new int[] { ((meth) methods.getSelectedItem()).var, 0 };
                        dispose();

                    }

                    public void windowDeactivated(WindowEvent e) {
                    // TODO Auto-generated method stub

                    }

                    public void windowDeiconified(WindowEvent e) {
                    // TODO Auto-generated method stub

                    }

                    public void windowIconified(WindowEvent e) {
                    // TODO Auto-generated method stub

                    }

                    public void windowOpened(WindowEvent e) {
                    // TODO Auto-generated method stub

                    }
                });
                meth[] meths = new meth[3];
                meths[0] = new meth("Kategorie nicht hinzufügen", sCatNoThing);
                meths[1] = new meth("Alle Serien in dieser Kategorie hinzufügen", sCatGrabb);
                meths[2] = new meth("Den neusten Download dieser Kategorie hinzufügen", sCatNewestDownload);
                methods = new JComboBox(meths);
                checkScat = new JCheckBox("Einstellungen für diese Sitzung beibehalten?", true);
                Insets insets = new Insets(0, 0, 0, 0);
                JDUtilities.addToGridBag(panel, new JLabel(JDLocale.L("Plugins.SerienJunkies.CatDialog.action", "Wählen sie eine Aktion aus:")), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                JDUtilities.addToGridBag(panel, methods, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                JDUtilities.addToGridBag(panel, checkScat, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                JButton btnOK = new JButton(JDLocale.L("gui.btn_continue", "OK"));
                btnOK.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        useScat = new int[] { ((meth) methods.getSelectedItem()).var, checkScat.isSelected() ? saveScat : 0 };
                        dispose();
                    }

                });
                JDUtilities.addToGridBag(panel, btnOK, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                add(panel, BorderLayout.CENTER);
                pack();
                setVisible(true);
            }

        }.init();
    }

    private int getSerienJunkiesCat() {

        sCatDialog();
        return useScat[0];

    }

    private String isNext() {
        if (next)
            return "|";
        else
            next = true;
        return "";

    }

    @Override
    public boolean collectCaptchas() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean useUserinputIfCaptchaUnknown() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Pattern getSupportedLinks() {
        return null;
    }

    @Override
    public synchronized boolean canHandle(String data) {
        boolean cat = false;
        // http://serienjunkies.org/?cat=3217
        if (data.contains("serienjunkies.org") && data.contains("cat=")) {
            cat = (getSerienJunkiesCat() != sCatNoThing);
        }
        boolean rscom = (Boolean) this.getProperties().getProperty("USE_RAPIDSHARE", true);
        boolean rsde = (Boolean) this.getProperties().getProperty("USE_RAPIDSHAREDE", false);
        boolean net = (Boolean) this.getProperties().getProperty("USE_NETLOAD", false);
        boolean uploaded = (Boolean) this.getProperties().getProperty("USE_UPLOADED", false);
        next = false;
        String hosterStr = "";
        if (rscom || rsde || net || uploaded) {
            hosterStr += "(";
            if (rscom) hosterStr += isNext() + "rc[\\_\\-]";
            if (rsde) hosterStr += isNext() + "rs[\\_\\-]";
            if (net) hosterStr += isNext() + "nl[\\_\\-]";
            if (uploaded) hosterStr += isNext() + "ut[\\_\\-]";
            if (cat) hosterStr += isNext() + "cat\\=[\\d]+";
            hosterStr += ")";
        }
        else {
            hosterStr += "not";
        }
        // http://download.serienjunkies.org/f-3bd58945ab43eae0/Episode%2006.html
        Matcher matcher = Pattern.compile("http://(download\\.serienjunkies\\.org|" + (cat ? "serienjunkies.org|" : "") + "serienjunkies\\.org/s|85\\.17\\.177\\.195/s|serienjunki\\.es/s).*" + hosterStr + ".*", Pattern.CASE_INSENSITIVE).matcher(data);
        if (matcher.find()) {
            return true;
        }
        else {
            String[] links = new Regexp(data, "http://download.serienjunkies.org/.*", Pattern.CASE_INSENSITIVE).getMatches(0);
            for (int i = 0; i < links.length; i++) {
                if (!links[i].matches("(?i).*http://download.serienjunkies.org/.*(rc[\\_\\-]|rs[\\_\\-]|nl[\\_\\-]|ut[\\_\\-]|cat\\=[\\d]+).*")) return true;
            }
        }
        return false;
    }

    @Override
    public Vector<String> getDecryptableLinks(String data) {
        String[] links = new Regexp(data, "http://.*?(serienjunkies\\.org|85\\.17\\.177\\.195|serienjunki\\.es)[^\"]*", Pattern.CASE_INSENSITIVE).getMatches(0);
        Vector<String> ret = new Vector<String>();
        scatChecked = true;
        for (int i = 0; i < links.length; i++) {
            if (canHandle(links[i])) ret.add(links[i]);
        }
        return ret;
    }

    @Override
    public String cutMatches(String data) {
        return data.replaceAll("(?i)http://.*?(serienjunkies\\.org|85\\.17\\.177\\.195|serienjunki\\.es).*", "--CUT--");
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        switch (step.getStep()) {
            case PluginStep.STEP_DECRYPT:

                Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
                try {
                    URL url = new URL(parameter);
                    if (parameter.matches(".*\\?cat\\=[\\d]+.*")) {
                        int catst = getSerienJunkiesCat();
                        scatChecked = false;
                        if (sCatNewestDownload == catst) {
                            request.withHtmlCode = false;
                            request.redirect = false;
                            request.getRequest("http://serienjunkies.org/");
                            request.withHtmlCode = true;
                            request.getRequest("http://serienjunkies.org/");

                            int cat = Integer.parseInt(parameter.replaceFirst(".*cat\\=", "").replaceFirst("[^\\d].*", ""));
                            Pattern pattern = Pattern.compile("<a href=\"http://serienjunkies.org/\\?cat\\=([\\d]+)\">(.*?)</a><br", Pattern.CASE_INSENSITIVE);
                            Matcher matcher = pattern.matcher(request.getHtmlCode());
                            String name = null;
                            while (matcher.find()) {
                                if (Integer.parseInt(matcher.group(1)) == cat) {
                                    name = matcher.group(2).toLowerCase();
                                    break;
                                }
                            }
                            if (name == null) return null;
                            request.getRequest(parameter);
                            name += " ";
                            String bet = null;
                            while (bet == null) {
                                name = name.substring(0, name.length() - 1);
                                if (name.length() == 0) return null;
                                bet = request.getRegexp("<p><strong>" + name + ".*?</strong>(.*?)</p>").getFirstMatch();
                            }

                            String[] links = getHttpLinks(bet, request.urlToString());
                            for (int i = 0; i < links.length; i++) {
                                decryptedLinks.add(this.createDownloadlink(links[i]));
                            }

                            step.setParameter(decryptedLinks);
                            return null;
                        }
                        else if (catst == sCatGrabb) {
                            request.getRequest(parameter);
                            String[] links = request.getRegexp("<p><strong>.*?</strong>(.*?)</p>").getMatches(1);

                            for (int i = 0; i < links.length; i++) {
                                String[] links2 = getHttpLinks(links[i], parameter);
                                for (int j = 0; j < links2.length; j++) {
                                    decryptedLinks.add(this.createDownloadlink(links2[j]));
                                }
                            }
                            step.setParameter(decryptedLinks);
                            return null;
                        }
                        else {
                            return null;
                        }
                    }
                    String modifiedURL = JDUtilities.htmlDecode(url.toString());
                    modifiedURL = modifiedURL.replaceAll("safe/rc", "safe/frc");
                    modifiedURL = modifiedURL.replaceAll("save/rc", "save/frc");
                    modifiedURL = modifiedURL.substring(modifiedURL.lastIndexOf("/"));

                    patternCaptcha = Pattern.compile(dynamicCaptcha);
                    logger.fine("using patternCaptcha:" + patternCaptcha);
                    RequestInfo reqinfo = getRequest(url, null, null, true);
                    if (reqinfo.getLocation() != null) reqinfo = getRequest(url, null, null, true);
                    String furl = getSimpleMatch(reqinfo.getHtmlCode(), "<FRAME SRC=\"°" + modifiedURL.replaceAll("[^0-1a-zA-Z]", ".") + "\"", 0);
                    if (furl != null) {
                        url = new URL(furl + modifiedURL);
                        logger.info("Frame found. frame url: " + furl + modifiedURL);
                        reqinfo = getRequest(url, null, null, true);
                        parameter = furl + modifiedURL;

                    }

                    // logger.info(reqinfo.getHtmlCode());

                    ArrayList<ArrayList<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), " <a href=\"http://°\"");
                    Vector<String> helpvector = new Vector<String>();
                    String helpstring = "";
                    // Einzellink

                    if (parameter.indexOf("/safe/") >= 0 || parameter.indexOf("/save/") >= 0) {
                        logger.info("safe link");
                        progress.setRange(1);
                        helpstring = EinzelLinks(parameter);
                        progress.increase(1);
                        decryptedLinks.add(this.createDownloadlink(helpstring));
                    }
                    else if (parameter.indexOf("download.serienjunkies.org") >= 0) {
                        logger.info("sjsafe link");
                        progress.setRange(1);
                        helpvector = ContainerLinks(parameter);
                        progress.increase(1);
                        for (int j = 0; j < helpvector.size(); j++) {
                            decryptedLinks.add(this.createDownloadlink(helpvector.get(j)));
                        }
                    }
                    else if (parameter.indexOf("/sjsafe/") >= 0) {
                        logger.info("sjsafe link");
                        progress.setRange(1);
                        helpvector = ContainerLinks(parameter);

                        progress.increase(1);
                        for (int j = 0; j < helpvector.size(); j++) {
                            decryptedLinks.add(this.createDownloadlink(helpvector.get(j)));
                        }
                    }
                    else {
                        logger.info("else link");
                        progress.setRange(links.size());
                        // Kategorien
                        for (int i = 0; i < links.size(); i++) {
                            progress.increase(1);
                            if (links.get(i).get(0).indexOf("/safe/") >= 0) {
                                helpstring = EinzelLinks(links.get(i).get(0));
                                decryptedLinks.add(this.createDownloadlink(helpstring));
                            }
                            else if (links.get(i).get(0).indexOf("/sjsafe/") >= 0) {
                                helpvector = ContainerLinks(links.get(i).get(0));
                                for (int j = 0; j < helpvector.size(); j++) {
                                    decryptedLinks.add(this.createDownloadlink(helpvector.get(j)));
                                }
                            }
                            else {
                                decryptedLinks.add(this.createDownloadlink(links.get(i).get(0)));
                            }
                        }
                    }
                }
                catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                // veraltet: firePluginEvent(new PluginEvent(this,
                // PluginEvent.PLUGIN_PROGRESS_FINISH, null));
                step.setParameter(decryptedLinks);
        }
        return null;
    }

    private void setConfigElements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Hoster Auswahl"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_RAPIDSHARE", "Rapidshare.com"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_RAPIDSHAREDE", "Rapidshare.de"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_NETLOAD", "Netload.in"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_UPLOADED", "Uploaded.to"));
        cfg.setDefaultValue(false);

    }

    // Für Links die bei denen die Parts angezeigt werden
    private Vector<String> ContainerLinks(String url) {

        Vector<String> links = new Vector<String>();
        boolean fileDownloaded = false;
        if (!url.startsWith("http://")) url = "http://" + url;
        try {
            RequestInfo reqinfo = getRequest(new URL(url));

            String cookie = reqinfo.getCookie();
            File captchaFile = null;
            String capTxt = null;
            while (true) { // for() läuft bis kein Captcha mehr abgefragt
                reqinfo.setHtmlCode(reqinfo.getHtmlCode().replaceAll("(?s)<!--.*?-->", "").replaceAll("(?i)(?s)<div style=\"display: none;\">.*?</div>", ""));
                Matcher matcher = patternCaptcha.matcher(reqinfo.getHtmlCode());
                if (matcher.find()) {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, false);
                    }
                    ArrayList<ArrayList<String>> gifs = getAllSimpleMatches(reqinfo.getHtmlCode(), patternCaptcha);
                    String captchaAdress = "http://download.serienjunkies.org" + gifs.get(0).get(1);
                    // for (int i = 0; i < gifs.size(); i++) {
                    // if (gifs.get(i).get(0).indexOf("secure") >= 0 &&
                    // JDUtilities.filterInt(gifs.get(i).get(2)) > 0 &&
                    // JDUtilities.filterInt(gifs.get(i).get(3)) > 0) {
                    // captchaAdress = "http://85.17.177.195" +
                    // gifs.get(i).get(0);
                    // logger.info(gifs.get(i).get(0));
                    // }
                    // }
                    HTTPConnection con = getRequestWithoutHtmlCode(new URL(captchaAdress), cookie, null, true).getConnection();
                    
                    if (con.getContentLength()<1000) {
                        
                        while (!JDUtilities.getController().requestReconnect()) {

                            try {
                                Thread.sleep(5000);

                            }
                            catch (InterruptedException e) {
                            }

                        }
                        reqinfo = getRequest(new URL(url));
                        cookie = reqinfo.getCookie();

                        continue;
                    }
                    
                    captchaFile = getLocalCaptchaFile(this, ".gif");
                   
                    fileDownloaded = JDUtilities.download(captchaFile, con);
                    if (!fileDownloaded || !captchaFile.exists() || captchaFile.length() == 0) {
                        logger.severe("Captcha nicht heruntergeladen, warte und versuche es erneut");
                        try {
                            Thread.sleep(1000);
                            reqinfo = getRequest(new URL(url));
                            cookie = reqinfo.getCookie();
                        }
                        catch (InterruptedException e) {
                        }
                        continue;
                    }
                    logger.info("captchafile: " + captchaFile);
                    capTxt = Plugin.getCaptchaCode(captchaFile, this);
                    
                    reqinfo = postRequest(new URL(url), "s=" + matcher.group(1) + "&c=" + capTxt + "&action=Download");

                }
                else {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, true);

                        if (useUserinputIfCaptchaUnknown() && this.getCaptchaDetectionID() == Plugin.CAPTCHA_USER_INPUT && this.getLastCaptcha() != null && this.getLastCaptcha().getLetterComperators() != null) {
                            LetterComperator[] lcs = this.getLastCaptcha().getLetterComperators();
                            this.getLastCaptcha().setCorrectcaptchaCode(capTxt.trim());

                            if (lcs.length == capTxt.trim().length()) {
                                for (int i = 0; i < capTxt.length(); i++) {

                                    if (lcs[i] != null && lcs[i].getDecodedValue() != null && capTxt.substring(i, i + 1).equalsIgnoreCase(lcs[i].getDecodedValue()) && lcs[i].getValityPercent() < 30.0) { //
                                        logger.severe("OK letter: " + i + ": JAC:" + lcs[i].getDecodedValue() + "(" + lcs[i].getValityPercent() + ") USER: " + capTxt.substring(i, i + 1));
                                    }
                                    else {

                                        logger.severe("Unknown letter: // " + i + ":  JAC:" + lcs[i].getDecodedValue() + "(" + lcs[i].getValityPercent() + ") USER:  " + capTxt.substring(i, i + 1)); // Pixelstring.
                                        // getB()
                                        // ist
                                        // immer
                                        // der
                                        // neue
                                        // letter
                                        final String character = capTxt.substring(i, i + 1);
                                        logger.info("SEND");
                                        Letter letter = lcs[i].getA();
                                        String captchaHash = UTILITIES.getLocalHash(captchaFile);
                                        letter.setSourcehash(captchaHash);
                                        letter.setOwner(this.getLastCaptcha().owner);
                                        letter.setDecodedValue(character);
                                        this.getLastCaptcha().owner.letterDB.add(letter);
                                        this.getLastCaptcha().owner.saveMTHFile();
                                    }
                                }

                            }
                            else {
                                logger.info("LCS not length comp");
                            }
                        }
                    }
                    break;
                }
            }
            if (reqinfo.getLocation() != null) {
                links.add(reqinfo.getLocation());
            }
            Form[] forms = reqinfo.getForms();
            for (int i = 0; i < forms.length; i++) {
				if(!forms[i].action.contains("firstload"))
				{
					try {
						reqinfo = getRequest(new URL(forms[i].action));
		                reqinfo = getRequest(new URL(getBetween(reqinfo.getHtmlCode(), "SRC=\"", "\"")), null,null, false);
		                String loc = reqinfo.getLocation();
		                if (loc != null) links.add(loc);
					} catch (Exception e) {
						// TODO: handle exception
					}

				}
			}
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return links;
    }

    // Für Links die gleich auf den Hoster relocaten
    private String EinzelLinks(String url) {
        String links = "";
        boolean fileDownloaded = false;
        if (!url.startsWith("http://")) url = "http://" + url;
        try {
            url = url.replaceAll("safe/rc", "safe/frc");
            url = url.replaceAll("save/rc", "save/frc");
            url = url.replaceAll("save/rs", "save/frs");
            url = url.replaceAll("safe/rs", "safe/frs");
            RequestInfo reqinfo = getRequest(new URL(url));
            File captchaFile = null;
            String capTxt = null;
            while (true) { // for() läuft bis kein Captcha mehr abgefragt
                reqinfo.setHtmlCode(reqinfo.getHtmlCode().replaceAll("(?s)<!--.*?-->", "").replaceAll("(?i)(?s)<div style=\"display: none;\">.*?</div>", ""));
                Matcher matcher = patternCaptcha.matcher(reqinfo.getHtmlCode());
                if (matcher.find()) {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, false);
                    }
                    String captchaAdress = "http://serienjunki.es" + matcher.group(2);
                    captchaFile = getLocalCaptchaFile(this, ".gif");
                    fileDownloaded = JDUtilities.download(captchaFile, captchaAdress);
                    logger.info("captchafile: " + fileDownloaded);
                    if (!fileDownloaded || !captchaFile.exists() || captchaFile.length() == 0) {
                        logger.severe("Captcha nicht heruntergeladen, warte und versuche es erneut");
                        try {
                            Thread.sleep(1000);
                            reqinfo = getRequest(new URL(url));
                        }
                        catch (InterruptedException e) {
                        }
                        continue;
                    }
                    capTxt = JDUtilities.getCaptcha(this, "einzellinks.Serienjunkies.org", captchaFile, false);
                    reqinfo = postRequest(new URL(url), "s=" + matcher.group(1) + "&c=" + capTxt + "&dl.start=Download");
                }
                else {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, true);
                    }
                    break;
                }
            }

            links = reqinfo.getLocation();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return links;
    }

}
