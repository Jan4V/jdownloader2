package jd.captcha.easy;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.ImageIcon;

import jd.captcha.JAntiCaptcha;
import jd.captcha.easy.load.LoadCaptchas;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.utils.Utilities;
import jd.config.container.JDLabelContainer;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.userio.UserIOGui;
import jd.nutils.JDFlags;
import jd.nutils.JDImage;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class EasyMethodFile implements JDLabelContainer, Serializable {
    public File file = null;

    public EasyMethodFile(String methodename) {
        this.file = new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/" + JDUtilities.getJACMethodsDirectory(), methodename);
    }

    public EasyMethodFile(File file) {
        this.file = file;
    }

    public boolean copyExampleImage() {
        File exf = getExampleImage();
        if (exf == null || !exf.exists()) {
            File[] listF = getCaptchaFolder().listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.matches("(?is).*\\.(jpg|png|gif)");
                }
            });
            if (listF != null && listF.length > 1) {
                JDIO.copyFile(listF[0], new File(file, "example." + getCaptchaType(false)));
                return true;
            }
        }
        return false;
    }

    public EasyMethodFile() {
    }

    public boolean isReadyToTrain() {
        if (!isEasyCaptchaMethode()) return true;
        Vector<CPoint> list = ColorTrainer.load(new File(file, "CPoints.xml"));
        boolean hasForderGround = false;
        boolean hasBackGround = false;

        for (CPoint point : list) {
            if (point.isForeground())
                hasForderGround = true;
            else
                hasBackGround = true;
        }
        return hasForderGround && hasBackGround;
    }

    public boolean isEasyCaptchaMethode() {
        File js = getScriptJas();
        return js.exists() && JDIO.readFileToString(js).contains("param.useSpecialGetLetters=EasyCaptcha");
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    private static final long serialVersionUID = 1L;

    public File getScriptJas() {
        return new File(file, "script.jas");

    }

    public File getJacinfoXml() {
        return new File(file, "jacinfo.xml");
    }

    /**
     * 
     * @return JAntiCaptcha Instanz für die Methode
     */
    public JAntiCaptcha getJac() {
        return new JAntiCaptcha(getName());
    }

    /**
     * erstellt eine Liste aller vorhandener Methoden
     */
    public static EasyMethodFile[] getMethodeList() {
        File[] files = new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/" + JDUtilities.getJACMethodsDirectory()).listFiles();
        ArrayList<EasyMethodFile> ret = new ArrayList<EasyMethodFile>();
        for (int i = 0; i < files.length; i++) {
            EasyMethodFile ef = new EasyMethodFile(files[i]);
            if (ef.getScriptJas().exists()) ret.add(ef);

        }
        return ret.toArray(new EasyMethodFile[] {});
    }

    public File getCaptchaFolder() {
        return new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/captchas/" + file.getName());
    }

    /**
     * gibt falls vorhanden das File zum example.jpg/png/gif im Methodenordner
     * aus
     * 
     * @return
     */
    public File getExampleImage() {
        File[] files = file.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                String ln = pathname.getName().toLowerCase();
                if (ln.contains("example") && ln.matches(".*\\.(jpg|jpeg|gif|png|bmp)")) return true;
                return false;
            }
        });
        if (files != null && files.length > 0) return files[0];
        return null;
    }

    /**
     * Ermittelt den Dateityp der Captchas im Captchaordner ist der Ordner leer
     * wird jpg ausgegeben bzw Bilder geladen wenn showLoadDialog true ist
     * 
     * @param showLoadDialog
     * @return captchatyp jpg | png | gif
     */
    public String getCaptchaType(boolean showLoadDialog) {
        final File folder2 = getCaptchaFolder();
        if (showLoadDialog) {
            if (!folder2.exists() || folder2.list().length < 1) {
                int res = UserIOGui.getInstance().requestConfirmDialog(0, JDL.L("easycaptcha.loadcaptchas.title", "Load Captchas"), JDL.L("easycaptcha.needCaptchas", "You need Captchas first!"), null, JDL.L("easycaptcha.openCaptchaFolder", "Open Captcha Folder"), JDL.L("easycaptcha.loadcaptchas", "Load Captchas"));
                if (JDFlags.hasSomeFlags(res, UserIO.RETURN_OK)) {
                    folder2.mkdir();
                    openCaptchaFolder();
                    return "jpg";
                } else {
                    return new GuiRunnable<String>() {
                        public String runSave() {
                            if (!new LoadCaptchas(EasyCaptchaTool.ownerFrame, file.getName()).start()) {
                                openCaptchaFolder();
                                return "jpg";
                            }
                            String filetype = "jpg";
                            File[] fl = folder2.listFiles();
                            if (fl[fl.length - 1].getName().toLowerCase().contains("png"))
                                filetype = "png";
                            else if (fl[fl.length - 1].getName().toLowerCase().contains("gif")) filetype = "gif";
                            return filetype;
                        }
                    }.getReturnValue();
                }
            }
        }
        String filetype = "jpg";
        File[] fl = folder2.listFiles();
        if (fl != null && fl.length > 0) {
            if (fl[fl.length - 1].getName().toLowerCase().contains("png"))
                filetype = "png";
            else if (fl[fl.length - 1].getName().toLowerCase().contains("gif")) filetype = "gif";
        }
        return filetype;
    }

    /**
     * öffnet den Captchaordner
     */
    public void openCaptchaFolder() {
        JDUtilities.openExplorer(getCaptchaFolder());
    }

    /**
     * läd das ExamleImage fals vorhanden und gibt ein ImageIcon mit
     * maxWidht=44px und maxHeight=24 aus
     */
    public ImageIcon getIcon() {
        try {
            File image = getExampleImage();
            if (image != null) {
                ImageIcon img = JDImage.getScaledImageIcon(JDImage.getImageIcon(image), 44, 24);
                return img;

            }
        } catch (Exception e) {
        }
        return null;
    }

    public File getRandomCaptchaFile() {
        File[] list = getCaptchaFolder().listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                String ln = pathname.getName().toLowerCase();
                if (pathname.isFile() && ln.matches(".*\\.(jpg|jpeg|gif|png|bmp)") && pathname.length() > 1) return true;
                return false;
            }
        });
        if (list.length == 0) return null;
        int id = (int) (Math.random() * (list.length - 1));
        return list[id];
    }

    /**
     * Erzeugt ein zufallsCaptcha aus dem Captchaordner
     * 
     * @return
     */
    public Captcha getRandomCaptcha() {
        Captcha captchaImage = getJac().createCaptcha(Utilities.loadImage(getRandomCaptchaFile()));
        if (captchaImage != null && captchaImage.getWidth() > 0 && captchaImage.getWidth() > 0) return captchaImage;
        captchaImage = getJac().createCaptcha(Utilities.loadImage(getRandomCaptchaFile()));
        if (captchaImage != null && captchaImage.getWidth() > 0 && captchaImage.getWidth() > 0) return captchaImage;
        return null;
    }

    @Override
    public String toString() {
        File infoxml = getJacinfoXml();
        String ret= null;
        try {
            if(infoxml.exists())
                ret= new Regex(JDIO.readFileToString(infoxml),"name=\"([^\"]*)").getMatch(0);
        } catch (Exception e) {
//e.printStackTrace();
        }
        if(ret==null)
            ret = file.getName();
        return ret;
    }

    public String getLabel() {
        return toString();
    }

    public String getName() {
        return toString();
    }
}