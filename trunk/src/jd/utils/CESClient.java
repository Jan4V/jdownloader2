package jd.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.logging.Logger;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import jd.captcha.CES;
import jd.config.SubConfiguration;
import jd.plugins.HTTPPost;
import jd.plugins.Plugin;
import jd.plugins.RequestInfo;

public class CESClient implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1448063149305109311L;
    private static final long MAX_TIME = 5 * 60 * 1000;
    private static final String PATTERN_TITLE = "<title>CaptchaExchangeServer - v° (°) by DVK</title>";
    private static final String PATTERN_STATS_RECEIVE = "Your balance is <b>°</b> points. You have received <b>°</b> captchas, recognized <b>°</b>, errors <b>° (°%)</b>";
    private static final String PATTERN_STATS_SENT = "You have sent on a server of <b>°</b> captchas, from them has been recognized <b>°</b>, errors <b>° (°%)</b>.";
    private static final String PATTERN_STATS_QUEUE = "There are <b>°</b> registered users now in queue on this server.";
    private static final String PATTERN_QUEUE = "<p>Your Username is <b>\"°\"</b>, your balance is <b>°</b> points. You have received <b>°</b> captchas, recognized <b>°</b>, errors <b>° (°%)</b>.<br>You have sent on a server of <b>°</b> captchas, from them has been recognized <b>°</b>, errors <b>° (°%)</b>.<br></p>°<p><br><div id=\"ta\">You are staying in queue to captcha recognition for <b>°</b> seconds.</div><br>There are <b>°</b> users in queue and <b>°</b> before you.°<br>Please, wait a little more... (estimated waiting time: °)<a href=°>[ reload ]";

    private static final String PATTERN_MESSAGE = "official C.E.S forum</a>!<br><p>°[ reload ]";
    private static final long RECEIVE_INTERVAL = 20 * 1000;
    private static final long CHECK_INTERVAL = 20 * 1000;
    private static final int FLAG_ABORT_RECEIVING = 0;
    public static final String PARAM_USER = "CES_USER";
    public static final String PARAM_PASS = "CES_PASS";

    public static final String UPDATE = "CES_QUEUE_LENGTH";
    private static final String MESSAGE = "CES_MESSAGE";
    private static final String PATTERN_IMAGE = "<p>Captcha image here:<br><img src=\"°\" border=\"2\" />";
    private static final String PATTERN_CAPTCHA_ID = "<input type=\"hidden\" name=\"Captcha\" value=\"°\"/>";
    private static final String PATTERN_CAPTCHA_STATE = "<input type=\"hidden\" name=\"State\" value=\"°\"/>";
    public static final String LASTEST_INSTANCE = "CES_LASTEST_INSTANCE";

    public static void main(String[] args) {
     
        // File file = new
        // File("C:/Users/coalado/.jd_home/captchas/rapidshare.com/21.04.2008_17.19.51.jpg");
        //
        // CESClient ces = new CESClient();
       
        // ces.enterQueueAndWait();
        // if (ces.sendCaptcha()) {
        // JDUtilities.getLogger().info("code: "+ces.waitForAnswer());
        // }
        //
        // // ces.register("coalado");
        //
        // // ces.login();

    }

    private String cesVersion;
    private String cesDate;
    private String captchaID;

    private boolean cesStatus;
    private String cesCode;
    private String statusText;
    private String captchaCode;
    private String price;
    private String recUser;
    private String recTime;
    private long startTime;
    private File file;
    private String md5;
    private String server = "http://dvk.com.ua/rapid/";// "http://dvk.org.ua/sokol/rapid/";//
    private String boundary = "---------------------------19777693011381";
    private String user;
    private String pass;
    private int save = 0;
    private int type = 1;
    private String k = JDUtilities.Base64Decode("akRvV25Mb2FEZXIxMDY=");
    private String regKey = "RapidByeByeBye";
    transient private Logger logger;
    private String balance;
    private String receivedCaptchas;
    private String recognizedCaptchas;
    private String receiveErrors;
    private String receiveErrorsPercent;
    private String sentCaptchasNum;
    private String sendRecognized;
    private String sentErrors;
    private Object sendErrorsPercent;
    private String queueLength;
    private String queuePosition;
    private String eta;
    private int abortFlag = -1;
    private SubConfiguration config;
    private String specs = "";
    private Plugin plugin;

    public CESClient(File captcha) {
        this.file = captcha;
        this.md5 = JDUtilities.getLocalHash(file);
        logger = JDUtilities.getLogger();
        config = JDUtilities.getSubConfig("JAC");
        config.setProperty(LASTEST_INSTANCE, this);

    }

    public CESClient() {
        config = JDUtilities.getSubConfig("JAC");
        config.setProperty(LASTEST_INSTANCE, this);
        logger = JDUtilities.getLogger();

    }

    /**
     * Informiert den Server darüber dass der letzte Captcha falsch erkannt
     * wurde
     */
    public void sendCaptchaWrong() {
        try {
            
            RequestInfo ri = Plugin.postRequest(new URL(server + "test.php"), null, null, null, "Nick=" + user + "&Pass=" + pass + "&Wrong=" + captchaID, true);
            config.setProperty(LASTEST_INSTANCE, this);
            printMessage(ri.getHtmlCode());
        } catch (Exception e) {

        }
    }

    /**
     * Betritt die Warteschleife TODO!!!
     */
    public void enterQueueAndWait() {
        while (true) {
            try {

                // http://dvk.com.ua/rapid/index.php?Nick=coalado&Pass=aCvtSmZwNCqm1&State=1
                if (this.abortFlag == FLAG_ABORT_RECEIVING) {
                    Plugin.postRequest(new URL(server + "index.php?Nick=" + user + "&Pass=" + pass + "&State=1"), "logout=Stop+captcha+recognition&State=4&Nick=" + user + "&Pass=" + pass);
                    return;
                }
                RequestInfo ri = Plugin.getRequest(new URL(server + "index.php?Nick=" + user + "&Pass=" + pass + "&State=1"));
               
                printMessage(ri.getHtmlCode());
                config.setProperty(LASTEST_INSTANCE, this);
                // PATTERN_IMAGE
                String img = Plugin.getSimpleMatch(ri.getHtmlCode(), PATTERN_IMAGE, 0);
                if (img != null) {
                    String id = Plugin.getSimpleMatch(ri.getHtmlCode(), PATTERN_CAPTCHA_ID, 0);
                    String state = Plugin.getSimpleMatch(ri.getHtmlCode(), PATTERN_CAPTCHA_STATE, 0);
                    this.askUserForCode(server + img, id, state);

                }

                String[] answer;

                answer = Plugin.getSimpleMatches(ri.getHtmlCode(), PATTERN_QUEUE);
               
                if (answer != null) {
                    this.balance = answer[1];
                    this.receivedCaptchas = answer[2];
                    this.recognizedCaptchas = answer[3];
                    this.receiveErrors = answer[4];
                    this.receiveErrorsPercent = answer[5];
                    this.sentCaptchasNum = answer[6];
                    this.sendRecognized = answer[7];
                    this.sentErrors = answer[8];
                    this.sendErrorsPercent = answer[9];
                    this.queueLength = answer[12];
                    this.queuePosition = answer[13];
                    this.eta = answer[15];
                    config.setProperty(UPDATE, (int) System.currentTimeMillis());
                    
                    
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            long i = RECEIVE_INTERVAL;

            while (i > 0) {

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                i -= 500;

                if (this.abortFlag == FLAG_ABORT_RECEIVING) {
                    try {
                        Plugin.postRequest(new URL(server + "index.php?Nick=" + user + "&Pass=" + pass + "&State=1"), "logout=Stop+captcha+recognition&State=4&Nick=" + user + "&Pass=" + pass);
                    } catch (MalformedURLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return;
                }
            }
        }

    }

    private void printMessage(String htmlCode) {
        String msg=Plugin.getSimpleMatch(htmlCode, PATTERN_MESSAGE, 0);
        if(msg==null)msg=htmlCode;
       logger.info("CES: "+msg);
        
    }

    public String getAccountInfoString() {
        String l = JDLocale.L("captcha.ces.infostring", "C.E.S |Punkte: %s |Benutzer: %s |Position: %s |Wartezeit bis Captcha: %s");
        return String.format(l, getBalance(), getQueueLength(), getQueuePosition(), this.getEta());
    }

    private void askUserForCode(String adr, String id, String state) {

        int index = Math.max(adr.lastIndexOf("/"), adr.lastIndexOf("\\"));
        String ext = adr.substring(index + 1);
        Calendar calendar = Calendar.getInstance();
        String date = String.format("%1$td.%1$tm.%1$tY_%1$tH.%1$tM.%1$tS", calendar);
        File dest = JDUtilities.getResourceFile("captchas/CES/" + date + "." + ext);
        JDUtilities.downloadBinary(dest.getAbsolutePath(), adr);
        String code = JDUtilities.getGUI().getCaptchaCodeFromUser(plugin, dest, "");
        if (code != null && code.length() > 0) {
            try {
                RequestInfo ri = Plugin.postRequest(new URL(server + "index.php?Nick=" + user + "&Pass=" + pass + "&State=1"), "Code=" + code + "&enter=OK&Captcha=" + id + "&State=" + state + "&Nick=" + user + "&Pass=" + pass);
                config.setProperty(LASTEST_INSTANCE, this);
                printMessage(ri.getHtmlCode());
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            CES.setEnabled(false);
            config.setProperty(LASTEST_INSTANCE, this);
            config.setProperty(UPDATE, (int)System.currentTimeMillis());

        }
    }

    /**
     * Brihct das Warten auf einen Captcha ab
     */
    public void abortReceiving() {
        config.setProperty(LASTEST_INSTANCE, this);
        this.abortFlag = FLAG_ABORT_RECEIVING;
    }

    public String getStatusPage() {
        RequestInfo ri;
        try {

            ri = Plugin.postRequest(new URL(server), null, null, null, "Nick=" + user + "&Pass=" + pass, true);
            return ri.getHtmlCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * login. Statistische Informationen über den account werden abgerfen. Ist
     * zur funktionweise nicht zwingend nötig
     * 
     * @return
     */
    public boolean login() {
        RequestInfo ri;
        try {

            ri = Plugin.postRequest(new URL(server), null, null, null, "Nick=" + user + "&Pass=" + pass, true);
            String error = Plugin.getSimpleMatch(ri.getHtmlCode(), "<font color=\"red\"><h2>°</h2></font>", 0);
            printMessage(ri.getHtmlCode());
            this.statusText = error;
            if (error != null) {

            return false; }

            String[] answer = Plugin.getSimpleMatches(ri.getHtmlCode(), PATTERN_TITLE);
            this.cesVersion = answer[0];
            this.cesDate = answer[1];
            answer = Plugin.getSimpleMatches(ri.getHtmlCode(), PATTERN_STATS_RECEIVE);
            this.balance = answer[0];
            this.receivedCaptchas = answer[1];
            this.recognizedCaptchas = answer[2];
            this.receiveErrors = answer[3];
            this.receiveErrorsPercent = answer[4];
            answer = Plugin.getSimpleMatches(ri.getHtmlCode(), PATTERN_STATS_SENT);
            this.sentCaptchasNum = answer[0];
            this.sendRecognized = answer[1];
            this.sentErrors = answer[2];
            this.sendErrorsPercent = answer[3];
            answer = Plugin.getSimpleMatches(ri.getHtmlCode(), PATTERN_STATS_QUEUE);

            this.queueLength = answer[0];

        } catch (Exception e) {

        }
        return true;
    }

    /**
     * Registriert einen user und gibt sein Passwort zurück null wird im
     * fehlerfall zurückgegeben. mit getStatusText() kann in diesem Fall der
     * fehler abgerufen werden
     * 
     * @param user
     * @return
     */
    public String register(String user) {

        RequestInfo ri;
        try {
            ri = Plugin.postRequest(new URL(server), null, null, null, "Nick=" + user + "&Pass=RapidByeByeBye", true);
            String pass = Plugin.getSimpleMatch(ri.getHtmlCode(), "You password is <b>°<b></h2>", 0);
            if (pass == null) {

                String error = Plugin.getSimpleMatch(ri.getHtmlCode(), "<font color=\"red\"><h2>°</h2></font>", 0);
                this.statusText = error;
            }
            return pass;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Nach dem mit sendCaptcha ein captcha verschickt wurde, muss diese
     * Funktion aufgerufen werden. Sie wartet solange bis der captcha
     * zurückgegeben wird und gibt den code zurück
     * 
     * @return
     */
    public String waitForAnswer() {
        // <html><head></head><body>CaptchaExchangeServer - v2.06 (29-04-08
        // 02:00) by DVK<br>dvk_ok001; Captcha is loaded with ID(510), your
        // balance (0), please wait recognition...<br></body></html>
        this.startTime = System.currentTimeMillis();
        try {
            logger.info("WAIT FOR REC");
            while (true) {
                config.setProperty(LASTEST_INSTANCE, this);
                Thread.sleep(CHECK_INTERVAL);
                if (System.currentTimeMillis() - startTime > MAX_TIME) {
                    cesStatus = false;
                    statusText = "Timeout of " + MAX_TIME;
                    return null;
                }
                RequestInfo ri = Plugin.postRequest(new URL(server + "test.php"), null, null, null, "Nick=" + user + "&Pass=" + pass + "&Test=" + captchaID, true);
                printMessage(ri.getHtmlCode());
                if (ri.containsHTML("dvk_ok002")) {

                    String[] answer = Plugin.getSimpleMatches(ri.getHtmlCode(), "<html><head></head><body>CaptchaExchangeServer - v° (°) by DVK<br>dvk_ok002; AccessCode(°), price ° point, \"°\" recognized in ° seconds early, balance (°).<br></body></html>");
                    this.cesVersion = answer[0];
                    this.cesDate = answer[1];
                    this.cesStatus = true;
                    this.captchaCode = answer[2];
                    this.price = answer[3];
                    this.recUser = answer[4];
                    this.recTime = answer[5];

                    this.balance = answer[5];
                    return captchaCode;

                }

            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Setzt die Account Logins
     * 
     * @param user
     * @param pass
     */
    public void setLogins(String user, String pass) {
        this.user = user;
        this.pass = pass;

    }

    /**
     * Schickt den übergebenen Captcha an den Server
     * 
     * @return
     */
    public boolean sendCaptcha() {
        try {
            HTTPPost up = new HTTPPost(server + "post.php", true);
            up.setBoundary(boundary);
            up.doUpload();
            up.connect();
            up.setForm("Captcha");
            up.sendFile(file.getAbsolutePath(), file.getName());
            up.sendVariable("Nick", user);
            up.sendVariable("Pass", pass);
            up.sendVariable("Save", save + "");
            up.sendVariable("Link", "");
            up.sendVariable("Type", type + "");
            up.sendVariable("Key", getKey());
            up.sendVariable("Specs", specs);

            RequestInfo ri = up.getRequestInfo();
            // <html><head></head><body>CaptchaExchangeServer - v2.06 (29-04-08
            // 02:00) by DVK<br>dvk_ok001; Captcha is loaded with ID(580281),
            // your
            // balance (96), please wait recognition...<br></body></html>
            up.close();
            printMessage(ri.getHtmlCode());
            if (ri.containsHTML("dvk_ok001")) {
                String[] answer = Plugin.getSimpleMatches(ri.getHtmlCode(), "<html><head></head><body>CaptchaExchangeServer - v° (°) by DVK<br>dvk_ok°; Captcha is loaded with ID(°), your balance (°), please wait recognition...<br></body></html>");
                this.cesVersion = answer[0];
                this.cesDate = answer[1];
                this.cesStatus = true;
                this.cesCode = answer[2];
                this.captchaID = answer[3];
                this.balance = answer[4];
                return true;
            } else {
                this.cesStatus = false;
                String[] answer = Plugin.getSimpleMatches(ri.getHtmlCode(), "<html><head></head><body>CaptchaExchangeServer - v2.06 (29-04-08 02:00) by DVK<br>dvk_°;°<br></body></html>");
                this.cesVersion = answer[0];
                this.cesDate = answer[1];

                this.cesCode = answer[2];
                this.statusText = answer[3];

                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Erstellt den Auth key für den Server
     * 
     * @return
     */
    private String getKey() {
        return JDUtilities.getMD5(k + md5);

    }

    /**
     * Getter für alles mögliche
     * 
     * @return
     */
    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getCesVersion() {
        return cesVersion;
    }

    public String getCesDate() {
        return cesDate;
    }

    public String getCaptchaID() {
        return captchaID;
    }

    public boolean isCesStatus() {
        return cesStatus;
    }

    public String getCesCode() {
        return cesCode;
    }

    public String getStatusText() {
        return statusText;
    }

    public String getCaptchaCode() {
        return captchaCode;
    }

    public String getPrice() {
        return price;
    }

    public String getRecUser() {
        return recUser;
    }

    public String getRecTime() {
        return recTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getSave() {
        return save;
    }

    public String getBalance() {
        return balance;
    }

    public String getReceivedCaptchas() {
        return receivedCaptchas;
    }

    public String getRecognizedCaptchas() {
        return recognizedCaptchas;
    }

    public String getReceiveErrors() {
        return receiveErrors;
    }

    public String getReceiveErrorsPercent() {
        return receiveErrorsPercent;
    }

    public String getSentCaptchasNum() {
        return sentCaptchasNum;
    }

    public String getSendRecognized() {
        return sendRecognized;
    }

    public String getSentErrors() {
        return sentErrors;
    }

    public Object getSendErrorsPercent() {
        return sendErrorsPercent;
    }

    public String getQueueLength() {
        return queueLength;
    }

    public String getQueuePosition() {
        return queuePosition;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        this.md5 = JDUtilities.getLocalHash(file);
    }

    public void setSpecs(String specs) {
        this.specs = specs;

    }

    public void setPlugin(Plugin plg) {
        this.plugin = plg;

    }

    public String getEta() {
        return eta;
    }

}