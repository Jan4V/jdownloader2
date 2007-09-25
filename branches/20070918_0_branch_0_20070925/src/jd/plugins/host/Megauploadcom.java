package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class Megauploadcom extends PluginForHost {
    // http://www.megaupload.com/de/?d=0XOSKVY9
    static private final Pattern    PAT_SUPPORTED                       = getSupportPattern("http://[*]megaupload.com/[*]\\?d=[+]");

    static private final String     HOST                                = "megaupload.com";

    static private final String     PLUGIN_NAME                         = HOST;

    static private final String     PLUGIN_VERSION                      = "0.1";

    static private final String     PLUGIN_ID                           = PLUGIN_NAME + "-" + VERSION;

    static private final String     CODER                               = "coalado";

    static private final String     SIMPLEPATTERN_CAPTCHA_URl           = " <img src=\"/capgen.php?°\">";

    static private final String     SIMPLEPATTERN_FILE_NAME             = "<b>Dateiname:</b>°</div>";

    static private final String     SIMPLEPATTERN_FILE_SIZE             = "<b>Dateigr°e:</b>°</div>";

    static private final String     SIMPLEPATTERN_CAPTCHA_POST_URL      = "<form method=\"POST\" action=\"°\" target";

    static private final String     COOKIE                              = "l=de; v=1; ve_view=1";

    static private final String     SIMPLEPATTERN_GEN_DOWNLOADLINK      = "var ° = String.fromCharCode(Math.abs(°));°var ° = '°' + String.fromCharCode(Math.sqrt(°));";

    static private final String     SIMPLEPATTERN_GEN_DOWNLOADLINK_LINK = "document.getElementById(\"dlbutton\").innerHTML = '<a href=\"°' ° '°\" onclick=\"loadingdownload();\">";

    static private final String     ERROR_TEMP_NOT_AVAILABLE            = "Zugriff auf die Datei ist vor";

    static private final String     ERROR_FILENOTFOUND                  = "Dieser Link ist leider nicht";

    static private final long       PENDING_WAITTIME                    = 45000;

    // /Simplepattern

    private String                  finalURL;

    private String                  captchaURL;

    private HashMap<String, String> fields;

    private String                  captchaPost;

    public Megauploadcom() {

        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));

    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public boolean isClipboardEnabled() {
        return true;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    // @Override
    // public URLConnection getURLConnection() {
    // // XXX: ???
    // return null;
    // }

    public PluginStep doStep(PluginStep step, DownloadLink parameter) {

        DownloadLink downloadLink = (DownloadLink) parameter;

        downloadLink.setUrlDownload(downloadLink.getUrlDownloadDecrypted().replaceAll("/de", ""));

        try {

            switch (step.getStep()) {

                case PluginStep.STEP_WAIT_TIME:
                    logger.info("::" + new URL(downloadLink.getUrlDownloadDecrypted()));
                    requestInfo = getRequest(new URL(downloadLink.getUrlDownloadDecrypted()), COOKIE, null, true);

                    if (requestInfo.containsHTML(ERROR_TEMP_NOT_AVAILABLE)) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                        step.setParameter(60 * 30l);
                        return step;

                    }

                    if (requestInfo.containsHTML(ERROR_FILENOTFOUND)) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);

                        return step;

                    }

                    this.captchaURL = "http://" + HOST + "/capgen.php?" + getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_CAPTCHA_URl, 0);
                    this.fields = getInputHiddenFields(requestInfo.getHtmlCode(), "checkverificationform", "passwordhtml");
                    this.captchaPost = getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_CAPTCHA_POST_URL, 0);
                    step.setParameter(captchaURL);
                    if (captchaURL.endsWith("null") || captchaPost == null) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);

                    }
                 
                    return step;

                case PluginStep.STEP_GET_CAPTCHA_FILE:

                    File file = this.getLocalCaptchaFile(this);
                    logger.info("Captcha " + captchaURL);
                    requestInfo = getRequestWithoutHtmlCode(new URL(captchaURL), COOKIE, requestInfo.getLocation(), true);
                    if (!requestInfo.isOK() || !JDUtilities.download(file, requestInfo.getConnection()) || !file.exists()) {
                        logger.severe("Captcha Download fehlgeschlagen: " + captchaURL);
                        step.setParameter(null);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                        return step;
                    }
                    else {
                        step.setParameter(file);
                        step.setStatus(PluginStep.STATUS_USER_INPUT);

                        return step;
                    }
                case PluginStep.STEP_PENDING:
                    requestInfo = postRequest(new URL(captchaPost), COOKIE, null, null, joinMap(fields, "=", "&") + "&imagestring=" + steps.get(1).getParameter(), true);
                    if (getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_CAPTCHA_URl, 0) != null) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                        return step;
                    }
                    step.setParameter(PENDING_WAITTIME);
                    return step;
                case PluginStep.STEP_DOWNLOAD:

                    Character l = (char) Math.abs(Integer.parseInt(getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK, 1).trim()));

                    String i = getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK, 4) + (char) Math.sqrt(Integer.parseInt(getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK, 5).trim()));

                    String url = (JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK_LINK, 0) + i + l + getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK_LINK, 2)));
                    logger.info(".." + url);

                    defaultDownloadStep(downloadLink, step, url, COOKIE, true);

                    return step;

            }
            return step;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void reset() {
        this.finalURL = null;
        this.captchaPost = null;
        this.captchaURL = null;
        this.fields = null;

    }

    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        // TODO Auto-generated method stub
        RequestInfo requestInfo;

        downloadLink.setUrlDownload(downloadLink.getUrlDownloadDecrypted().replaceAll("/de", ""));
        try {
            requestInfo = getRequest(new URL(downloadLink.getUrlDownloadDecrypted()), "l=de; v=1; ve_view=1", null, true);
            if (requestInfo.containsHTML(ERROR_TEMP_NOT_AVAILABLE) || requestInfo.containsHTML(ERROR_FILENOTFOUND)) {
                return false;

            }
            String fileName = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_FILE_NAME, 0));
            String fileSize = getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_FILE_SIZE, 1);
            if (fileName == null || fileSize == null) {
                return false;
            }
            downloadLink.setName(fileName.trim());

            if (fileSize.indexOf("KB") > 0) {
                downloadLink.setDownloadMax((int) (Double.parseDouble(fileSize.trim().split(" ")[0].trim()) * 1024));
            }

            if (fileSize.indexOf("MB") > 0) {
                downloadLink.setDownloadMax((int) (Double.parseDouble(fileSize.trim().split(" ")[0].trim()) * 1024 * 1024));
            }
        }
        catch (MalformedURLException e) {
            return false;
        }
        catch (IOException e) {
            return false;
        }

        return true;
    }

    @Override
    public int getMaxSimultanDownloadNum() {

        return 1;
    }
}
