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
package jd.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import jd.http.requests.FormData;
import jd.http.requests.GetRequest;
import jd.http.requests.HeadRequest;
import jd.http.requests.PostFormDataRequest;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.KeyValueStringEntry;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.ConsoleLogImpl;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.net.PublicSuffixList;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils.IPVERSION;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.ProxyAuthException;
import org.appwork.utils.parser.UrlQuery;

public class Browser {
    // we need this class in here due to jdownloader stable 0.9 compatibility
    public class BrowserException extends IOException {
        private static final long serialVersionUID = 1509988898224037320L;
        private final Request     request;

        public BrowserException(final String message, final Request request) {
            this(message, request, null);
        }

        public BrowserException(IOException exception, final Request request) {
            this(null, request, exception);
        }

        public BrowserException(final String message, final Request request, final Exception e) {
            super(message, e);
            this.request = request;
        }

        public Request getRequest() {
            return this.request;
        }
    }

    private static final HashMap<String, Cookies> COOKIES            = new HashMap<String, Cookies>();
    private static ProxySelectorInterface         GLOBAL_PROXY       = null;
    private static LogInterface                   LOGGER             = new ConsoleLogImpl();
    private static HashMap<String, Integer>       REQUEST_INTERVAL_LIMIT_MAP;
    private static HashMap<String, Long>          REQUESTTIME_MAP;
    private static int                            TIMEOUT_CONNECT    = 30000;
    private static int                            TIMEOUT_READ       = 30000;
    private Boolean                               defaultSSLTrustALL = null;
    protected static IPVERSION                    GLOBAL_IPVERSION   = null;
    protected IPVERSION                           ipVersion          = null;

    public static IPVERSION getGlobalIPVersion() {
        return Browser.GLOBAL_IPVERSION;
    }

    public static void setGlobalIPVersion(IPVERSION ipVersion) {
        Browser.GLOBAL_IPVERSION = ipVersion;
    }

    public IPVERSION getIPVersion() {
        if (this.ipVersion != null) {
            return this.ipVersion;
        } else {
            return Browser.getGlobalIPVersion();
        }
    }

    public void setIPVersion(IPVERSION ipVersion) {
        this.ipVersion = ipVersion;
    }

    public static ProxySelectorInterface _getGlobalProxy() {
        return Browser.GLOBAL_PROXY;
    }

    public static int getGlobalReadTimeout() {
        return Browser.TIMEOUT_READ;
    }

    public static String getHost(final String url) {
        return Browser.getHost(url, false);
    }

    public static String getHost(final URL url) {
        return Browser.getHost(url, false);
    }

    private static final Pattern HOST_IP_PATTERN1     = Pattern.compile("^(?:[a-z0-9]{2,64}://)?[a-z0-9]{2,64}://(?:[^\\s@/]+?@)?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(?::\\d+)?(?:/|/.+|)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HOST_IP_PATTERN2     = Pattern.compile("^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(?::\\d+)?(?:/|/.+|)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HOST_DOMAIN_PATTERN  = Pattern.compile("^(?:[a-z0-9]{2,64}://)?[a-z0-9]{2,64}://(?:[^\\s@/]+?@)?([a-z0-9\\-\\.]+?)(/|$|:\\d+$|:\\d+/)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HOST_DOMAIN_PATTERN2 = Pattern.compile("^(?:[^\\s@/]+?@)?([a-z0-9\\-\\.]+?)(/|$|:\\d+$|:\\d+/)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /*
     * this method extracts domain/ip from given url. optional keeps existing subdomains
     */
    public static String getHost(final String url, final boolean includeSubDomains) {
        if (url == null) {
            return null;
        }
        final String trimURL = url.trim();
        try {
            return Browser.getHost(new URL(trimURL), includeSubDomains);
        } catch (Throwable e) {
        }
        /* direct ip with protocol */
        String ret = new Regex(trimURL, Browser.HOST_IP_PATTERN1).getMatch(0);
        if (ret == null) {
            /* direct ip without protocol */
            ret = new Regex(trimURL, Browser.HOST_IP_PATTERN2).getMatch(0);
        }
        if (ret != null) {
            return ret;
        }
        /* normal url with protocol */
        ret = new Regex(trimURL, Browser.HOST_DOMAIN_PATTERN).getMatch(0);
        if (ret == null) {
            /* without procotol */
            ret = new Regex(trimURL, Browser.HOST_DOMAIN_PATTERN2).getMatch(0);
        }
        if (ret != null && includeSubDomains == false) {
            /* cut off all subdomains */
            final PublicSuffixList psl = PublicSuffixList.getInstance();
            if (psl != null) {
                final String domain = psl.getDomain(ret.toLowerCase(Locale.ENGLISH));
                if (domain != null) {
                    return domain;
                }
            }
            int indexPoint = ret.lastIndexOf(".");
            indexPoint = ret.lastIndexOf(".", indexPoint - 1);
            if (indexPoint >= 0) {
                /* we enter this branch only if a subdomain exists */
                ret = ret.substring(indexPoint + 1);
            }
        }
        if (ret != null) {
            return ret.toLowerCase(Locale.ENGLISH);
        }
        return url;
    }

    public static String getHost(final URL uri, final boolean includeSubDomains) {
        if (uri == null) {
            return null;
        }
        String ret = uri.getHost();
        if (Browser.HOST_IP_PATTERN2.matcher(ret).matches()) {
            return ret;
        }
        if (ret != null && includeSubDomains == false) {
            /* cut off all subdomains */
            final PublicSuffixList psl = PublicSuffixList.getInstance();
            if (psl != null) {
                final String domain = psl.getDomain(ret.toLowerCase(Locale.ENGLISH));
                if (domain != null) {
                    return domain;
                }
            }
            int indexPoint = ret.lastIndexOf(".");
            indexPoint = ret.lastIndexOf(".", indexPoint - 1);
            if (indexPoint >= 0) {
                /* we enter this branch only if a subdomain exists */
                ret = ret.substring(indexPoint + 1);
            }
        }
        if (ret != null) {
            return ret.toLowerCase(Locale.ENGLISH);
        }
        return ret;
    }

    /**
     * Sets the global connect timeout
     *
     * @param valueMS
     */
    public static void setGlobalConnectTimeout(final int valueMS) {
        Browser.TIMEOUT_CONNECT = valueMS;
    }

    public static void setGlobalLogger(final LogInterface logger) {
        Browser.LOGGER = logger;
    }

    public static void setGlobalProxy(final ProxySelectorInterface p) {
        Browser.GLOBAL_PROXY = p;
    }

    /**
     * Sets the global readtimeout in ms
     *
     * @param valueMS
     */
    public static void setGlobalReadTimeout(final int valueMS) {
        Browser.TIMEOUT_READ = valueMS;
    }

    public static void setGlobalVerbose(final boolean b) {
        Browser.VERBOSE = b;
    }

    private boolean        keepResponseContentBytes = false;
    private int[]          allowedResponseCodes     = new int[0];
    private static boolean VERBOSE                  = false;

    /**
     * Lädt über eine URLConnection eine Datei herunter. Zieldatei ist file.
     *
     * @param file
     * @param con
     * @return Erfolg true/false
     * @throws IOException
     */
    public static void download(final File file, final URLConnectionAdapter con) throws IOException {
        if (con == null) {
            throw new IOException("con is null");
        }
        if (file.isFile()) {
            if (file.exists() && !file.delete()) {
                throw new IOException("Could not overwrite file: " + file);
            }
        }
        final File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        FileOutputStream fos = null;
        InputStream input = null;
        boolean okay = false;
        try {
            input = con.getInputStream();
            file.createNewFile();
            fos = new FileOutputStream(file, false);
            final long length;
            if (con.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_ENCODING) == null) {
                length = con.getCompleteContentLength();
            } else {
                length = -1;
            }
            final byte[] b = new byte[32767];
            long done = 0;
            int len;
            while ((len = input.read(b)) != -1) {
                fos.write(b, 0, len);
                done += len;
            }
            if (length > 0 && length != done) {
                throw new IOException("Incomplete:" + length + "<=>" + done);
            }
            okay = true;
        } finally {
            try {
                input.close();
            } catch (final Throwable e) {
            }
            try {
                fos.close();
            } catch (final Throwable e) {
            }
            if (okay == false) {
                file.delete();
            }
        }
    }

    public static int getGlobalConnectTimeout() {
        return Browser.TIMEOUT_CONNECT;
    }

    public static LogInterface getGlobalLogger() {
        return Browser.LOGGER;
    }

    public static synchronized void setRequestIntervalLimitGlobal(final String host, final int i) {
        final String domain = Browser.getHost(host);
        if (domain == null) {
            return;
        }
        if (Browser.REQUEST_INTERVAL_LIMIT_MAP == null) {
            Browser.REQUEST_INTERVAL_LIMIT_MAP = new HashMap<String, Integer>();
            Browser.REQUESTTIME_MAP = new HashMap<String, Long>();
        }
        Browser.REQUEST_INTERVAL_LIMIT_MAP.put(domain, i);
    }

    private static HashMap<String, ArrayList<Long>> REQUESTS_THRESHOLD_TIME_HISTORY_MAP;
    private static HashMap<String, Long>            REQUESTS_THRESHOLD_INTERVAL_MAP;
    private static HashMap<String, Integer>         REQUESTS_THRESHOLD_REQUESTS_MAP;

    /**
     * sets request thresholds based on upper burstable limit. eg. 20(x) requests over 60000(y)[=1min]. Then on after it sets limit between
     * interval.
     *
     * @author raztoki
     * @since JD2
     * @param host
     * @param i
     *            (ms)
     * @param x
     *            (requests)
     * @param y
     *            (ms)
     * @throws Exception
     */
    public static synchronized void setRequestIntervalLimitGlobal(final String host, final int i, final int x, final long y) throws Exception {
        if (x <= 0 || y <= 0 || i <= 0) {
            throw new Exception("'x' and 'y' and 'i' have to be above zero!");
            // return;
        }
        final String domain = Browser.getHost(host);
        if (domain == null) {
            throw new Exception("Browser.getHost(host) returned null");
            // return;
        }
        // Utilise existing method
        Browser.setRequestIntervalLimitGlobal(domain, i);
        // here we go.
        if (Browser.REQUESTS_THRESHOLD_INTERVAL_MAP == null) {
            Browser.REQUESTS_THRESHOLD_INTERVAL_MAP = new HashMap<String, Long>();
            Browser.REQUESTS_THRESHOLD_REQUESTS_MAP = new HashMap<String, Integer>();
            Browser.REQUESTS_THRESHOLD_TIME_HISTORY_MAP = new HashMap<String, ArrayList<Long>>();
        }
        Browser.REQUESTS_THRESHOLD_REQUESTS_MAP.put(domain, x);
        Browser.REQUESTS_THRESHOLD_INTERVAL_MAP.put(domain, y);
    }

    private static synchronized void waitForPageAccess(final Browser browser, final Request request) throws InterruptedException {
        final String host = Browser.getHost(request.getURL());
        ArrayList<Long> ts = null;
        if (Browser.REQUESTS_THRESHOLD_INTERVAL_MAP != null && Browser.REQUESTS_THRESHOLD_INTERVAL_MAP.containsKey(host)) {
            ts = Browser.REQUESTS_THRESHOLD_TIME_HISTORY_MAP.get(host);
            if (ts == null) {
                ts = new ArrayList<Long>();
            }
        }
        try {
            if (ts != null) {
                final long maxInterval = Browser.REQUESTS_THRESHOLD_INTERVAL_MAP.get(host);
                final int arrayMaxSize = Browser.REQUESTS_THRESHOLD_REQUESTS_MAP.get(host);
                final ArrayList<Long> currentStill = new ArrayList<Long>();
                for (final Long t : ts) {
                    long time = System.currentTimeMillis() - t;
                    if (time < maxInterval) {
                        currentStill.add(t);
                    }
                }
                ts = currentStill;
                if (ts.size() < arrayMaxSize) {
                    return;
                }
                // since ArrayList preserves order, oldest entry should always be 0
                final long requestsThresholdOldestTimestamp = ts.get(0);
                final int globalLimit = Browser.REQUEST_INTERVAL_LIMIT_MAP.get(host);
                final long globalLastRequest = Browser.REQUESTTIME_MAP.get(host) == null ? System.currentTimeMillis() : Browser.REQUESTTIME_MAP.get(host);
                // effectively you could have to wait x minutes before slot opens up, all depends on how wide the y interval is. Once the
                // burst map is full we then use specified average wait(i) based upon the last request
                // current time - oldest timestamp should give us the elapsed time.
                long wait1 = System.currentTimeMillis() - requestsThresholdOldestTimestamp;
                // now determine how long until it expires against maxInterval
                wait1 = maxInterval - wait1;
                long wait2 = globalLimit - (System.currentTimeMillis() - globalLastRequest);
                if (wait1 < 0) {
                    wait1 = 0;
                }
                if (wait2 < 0) {
                    wait2 = 0;
                }
                if (ts.size() < arrayMaxSize && (wait1 > maxInterval || wait2 > wait1)) {
                    // only enter if we are equal to or less than arrayMaxSize.
                    Thread.sleep(wait1);
                } else {
                    Thread.sleep(wait2);
                }
                return;
            }
            Integer localLimit = null;
            Integer globalLimit = null;
            Long localLastRequest = null;
            Long globalLastRequest = null;
            if (browser.requestIntervalLimitMap != null) {
                localLimit = browser.requestIntervalLimitMap.get(host);
                localLastRequest = browser.requestTimeMap.get(host);
            }
            if (Browser.REQUEST_INTERVAL_LIMIT_MAP != null) {
                globalLimit = Browser.REQUEST_INTERVAL_LIMIT_MAP.get(host);
                globalLastRequest = Browser.REQUESTTIME_MAP.get(host);
            }
            if (localLimit == null && globalLimit == null) {
                return;
            }
            if (localLastRequest == null && globalLastRequest == null) {
                return;
            }
            if (localLimit != null && localLastRequest == null) {
                return;
            }
            if (globalLimit != null && globalLastRequest == null) {
                return;
            }
            if (globalLimit == null) {
                globalLimit = 0;
            }
            if (localLimit == null) {
                localLimit = 0;
            }
            if (localLastRequest == null) {
                localLastRequest = System.currentTimeMillis();
            }
            if (globalLastRequest == null) {
                globalLastRequest = System.currentTimeMillis();
            }
            final long dif = Math.max(localLimit - (System.currentTimeMillis() - localLastRequest), globalLimit - (System.currentTimeMillis() - globalLastRequest));
            if (dif > 0) {
                // System.out.println("Sleep " + dif + " before connect to " +
                // request.getUrl().getHost());
                Thread.sleep(dif);
                // waitForPageAccess(request);
            }
        } finally {
            if (browser.requestTimeMap != null) {
                browser.requestTimeMap.put(host, System.currentTimeMillis());
            }
            if (Browser.REQUESTTIME_MAP != null) {
                Browser.REQUESTTIME_MAP.put(host, System.currentTimeMillis());
            }
            if (ts != null) {
                ts.add(System.currentTimeMillis());
                Browser.REQUESTS_THRESHOLD_TIME_HISTORY_MAP.put(host, ts);
            }
        }
    }

    private String                   acceptLanguage   = "de, en-gb;q=0.9, en;q=0.8";
    /*
     * -1 means use default Timeouts
     * 
     * 0 means infinite (DO NOT USE if not needed)
     */
    private int                      connectTimeout   = -1;
    private HashMap<String, Cookies> cookies          = new HashMap<String, Cookies>();
    private boolean                  cookiesExclusive = true;
    private Object                   currentURL       = null;
    private String                   customCharset    = null;
    private boolean                  debug            = false;
    private boolean                  doRedirects      = false;
    private RequestHeader            headers;
    private int                      limit            = 8 * 1024 * 1024;
    private LogInterface             logger           = null;
    private ProxySelectorInterface   proxy;
    private int                      readTimeout      = -1;
    private Request                  request;
    private HashMap<String, Integer> requestIntervalLimitMap;
    private HashMap<String, Long>    requestTimeMap;
    private boolean                  verbose          = false;

    public Browser() {
        final Thread currentThread = Thread.currentThread();
        /**
         * use BrowserSettings from current thread if available
         */
        if (currentThread != null && currentThread instanceof BrowserSettings) {
            final BrowserSettings settings = (BrowserSettings) currentThread;
            this.proxy = settings.getProxySelector();
            this.debug = settings.isDebug();
            this.verbose = settings.isVerbose();
            this.logger = settings.getLogger();
        }
    }

    /**
     * Assures that the browser does not download any binary files in textmode
     *
     * @param request
     * @throws BrowserException
     */
    private void checkContentLengthLimit(final Request request) throws BrowserException {
        if (request != null && request.getHttpConnection() != null && !(request instanceof HeadRequest)) {
            final int limit = this.getLoadLimit();
            request.setReadLimit(limit);
            final long length = request.getHttpConnection().getLongContentLength();
            if (length >= 0 && length > limit) {
                request.disconnect();
                throw new BrowserException("Content-length too big:" + length + ">" + limit, request);
            }
        }
    }

    /**
     * Clears all cookies for the given URL. URL has to be a valid. if (url == null), all cookies are cleared.
     *
     * @param url
     */
    public void clearCookies(final String url) {
        final Map<String, Cookies> map = this.getCookies();
        synchronized (map) {
            if (url == null) {
                map.clear();
            } else {
                final String host = Browser.getHost(url);
                final Iterator<Entry<String, Cookies>> it = map.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<String, Cookies> next = it.next();
                    if (next.getKey() == null) {
                        it.remove();
                    } else if (StringUtils.equalsIgnoreCase(next.getKey(), host)) {
                        next.getValue().clear();
                        break;
                    }
                }
            }
        }
    }

    public Browser cloneBrowser() {
        final Browser br = new Browser();
        return this.cloneBrowser(br);
    }

    public Browser cloneBrowser(final Browser br) {
        br.requestIntervalLimitMap = this.requestIntervalLimitMap;
        br.requestTimeMap = this.requestTimeMap;
        br.acceptLanguage = this.acceptLanguage;
        br.connectTimeout = this.connectTimeout;
        br.currentURL = this.currentURL;
        br.doRedirects = this.doRedirects;
        br.defaultSSLTrustALL = this.defaultSSLTrustALL;
        br.setCustomCharset(this.customCharset);
        br.getHeaders().putAll(this.getHeaders());
        br.limit = this.limit;
        br.readTimeout = this.readTimeout;
        br.request = this.getRequest();
        br.cookies = this.cookies;
        br.browserParentID = this.getBrowserID();
        br.getBrowserID();
        br.authentications = this.authentications;
        br.cookiesExclusive = this.cookiesExclusive;
        br.authenticationFactory = this.authenticationFactory;
        br.debug = this.debug;
        br.verbose = this.verbose;
        br.logger = this.getLogger();
        br.proxy = this.proxy;
        br.ipVersion = this.ipVersion;
        br.keepResponseContentBytes = this.keepResponseContentBytes;
        br.allowedResponseCodes = this.allowedResponseCodes;
        return br;
    }

    public boolean containsHTML(final String regex) {
        return new Regex(this, regex).matches();
    }

    /**
     * Creates a new Request object based on a form
     *
     * @param form
     * @return
     * @throws Exception
     */
    public Request createFormRequest(final Form form) throws IOException {
        URL base = null;
        final Request lRequest = this.getRequest();
        if (lRequest != null) {
            /* take current url as base url */
            base = lRequest.getURL();
        }
        try {
            // we have no method to validate html tags.. could be faked, or multiples
            final String baseTag = this.getRegex("<\\s*base\\s+[^>]*>").getMatch(-1);
            if (baseTag != null) {
                String sourceBase = new Regex(baseTag, "href\\s*=\\s*(\"|')(.+?)\\1").getMatch(1);
                if (sourceBase == null) {
                    sourceBase = new Regex(baseTag, "\\s+href\\s*=([^\\s]+)").getMatch(0);
                }
                if (sourceBase != null) {
                    /* take baseURL in case we've found one in current request */
                    final URL sourceBaseURL = URLHelper.createURL(sourceBase.trim());
                    // simple validation, we should only allow base to current domain! -raztoki20160304
                    final String domainHostBase = base != null ? Browser.getHost(Request.getLocation(base.toString(), lRequest)) : null;
                    final String domainSourceBase = Browser.getHost(Request.getLocation(sourceBase, lRequest));
                    if (domainHostBase != null && domainSourceBase != null && domainHostBase.equals(domainSourceBase)) {
                        base = sourceBaseURL;
                    }
                }
            }
        } catch (final Throwable e) {
        }
        final String formAction = lRequest != null ? form.getAction(lRequest.getURL()) : form.getAction(base);
        if (formAction == null) {
            throw new NullPointerException("no valid action url");
        }
        switch (form.getMethod()) {
        case GET:
            final String getAction;
            final String varString = form.getPropertyString();
            if (varString != null && !varString.matches("[\\s]*")) {
                getAction = URLHelper.parseLocation(URLHelper.createURL(formAction), "&" + varString);
            } else {
                getAction = formAction;
            }
            return this.createGetRequest(getAction);
        case POST:
            if (form.getEncoding() == null || !form.getEncoding().toLowerCase().endsWith("form-data")) {
                return this.createPostRequest(formAction, form.getRequestVariables(), form.getEncoding());
            } else {
                final PostFormDataRequest request = this.createPostFormDataRequest(formAction);
                if (form.getEncoding() != null) {
                    request.setEncodeType(form.getEncoding());
                }
                final int size = form.getInputFields().size();
                for (int i = 0; i < size; i++) {
                    final InputField entry = form.getInputFields().get(i);
                    if (form.getPreferredSubmit() != null && entry.getType() != null && entry.getType().equalsIgnoreCase("submit") && form.getPreferredSubmit() != entry) {
                        continue;
                    }
                    if (entry.getValue() == null) {
                        // continue;
                    } else if (entry.getType() != null && entry.getType().equalsIgnoreCase("image")) {
                        request.addFormData(new FormData(entry.getKey() + ".x", entry.getProperty("x", (int) (Math.random() * 100) + "")));
                        request.addFormData(new FormData(entry.getKey() + ".y", entry.getProperty("y", (int) (Math.random() * 100) + "")));
                    } else if (entry.getType() != null && entry.getType().equalsIgnoreCase("file")) {
                        request.addFormData(new FormData(entry.getKey(), entry.getFileToPost().getName(), entry.getFileToPost()));
                    } else if (entry.getKey() != null && entry.getValue() != null) {
                        request.addFormData(new FormData(entry.getKey(), entry.getValue()));
                    }
                }
                return request;
            }
        }
        return null;
    }

    public GetRequest createGetRequest(String url) throws IOException {
        return new GetRequest(this.getURL(url));
    }

    public HeadRequest createHeadRequest(String url) throws IOException {
        return new HeadRequest(this.getURL(url));
    }

    public PostFormDataRequest createPostFormDataRequest(String url) throws IOException {
        return new PostFormDataRequest(this.getURL(url));
    }

    /**
     * Creates a new postrequest based an an requestVariable ArrayList
     *
     * @deprecated use {@link #createPostRequest(String, UrlQuery, String)
     *
     *
     *
     *
     */
    @Deprecated
    public PostRequest createPostRequest(String url, final List<KeyValueStringEntry> post, final String encoding) throws IOException {
        return this.createPostRequest(url, UrlQuery.get(post), encoding);
    }

    public PostRequest createPostRequest(String url, UrlQuery post, final String encoding) throws IOException {
        final PostRequest request = new PostRequest(this.getURL(url));
        if (post != null) {
            request.addAll(post.list());
        }
        String requestContentType = encoding;
        final RequestHeader lHeaders = this.headers;
        if (lHeaders != null) {
            final String browserContentType = lHeaders.remove(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE);
            if (requestContentType == null) {
                requestContentType = browserContentType;
            }
        }
        if (requestContentType == null) {
            requestContentType = "application/x-www-form-urlencoded";
        }
        request.setContentType(requestContentType);
        return request;
    }

    /**
     * Creates a new PostRequest based on a variable HashMap
     */
    public PostRequest createPostRequest(final String url, final UrlQuery post) throws IOException {
        return this.createPostRequest(url, post.list(), null);
    }

    /**
     * Creates a PostRequest based on a querystring
     */
    public PostRequest createPostRequest(final String url, final String post) throws MalformedURLException, IOException {
        return this.createPostRequest(url, Request.parseQuery(post));
    }

    public String followRedirect() throws IOException {
        return this.followRedirect(false);
    }

    public String followRedirect(final boolean followAllRedirects) throws IOException {
        final Request lRequest = this.getRequest();
        if (lRequest == null) {
            throw new IllegalStateException("Request is null");
        }
        if (lRequest.getLocation() != null) {
            if (lRequest.getHtmlCode() == null) {
                this.loadConnection(lRequest.getHttpConnection());
            }
            final Request redirectRequest = this.createRedirectFollowingRequest(this.request);
            return this.loadConnection(this.openRequestConnection(redirectRequest, followAllRedirects)).getHTMLSource();
        }
        if (lRequest.getHtmlCode() != null) {
            return lRequest.getHTMLSource();
        }
        return this.loadConnection(lRequest.getHttpConnection()).getHTMLSource();
    }

    /**
     * rfc2616
     *
     * @param request
     * @return
     * @throws BrowserException
     */
    public Request createRedirectFollowingRequest(final Request request) throws IOException {
        if (request == null) {
            throw new IllegalArgumentException("Request is null");
        }
        String location = request.getLocation();
        if (StringUtils.isEmpty(location)) {
            throw new IllegalStateException("Request does not contain a redirect");
        }
        final URL newURL = this.getURL(location);
        final int responseCode = request.getHttpConnection().getResponseCode();
        Request newRequest = null;
        switch (responseCode) {
        case 200:
        case 201:
            if (request instanceof HeadRequest) {
                newRequest = request.cloneRequest();
            } else {
                newRequest = new GetRequest(request);
            }
            break;
        case 301:
            if (request instanceof HeadRequest || request instanceof GetRequest) {
                newRequest = request.cloneRequest();
            } else {
                /* it seems getRequest is expected although rfc says that post can be kept */
                newRequest = new GetRequest(request);
            }
            break;
        case 302:
        case 303:
            if (request instanceof HeadRequest) {
                newRequest = request.cloneRequest();
            } else {
                newRequest = new GetRequest(request);
            }
            break;
        case 307:
        case 308:
            newRequest = request.cloneRequest();
            break;
        default:
            LogInterface logger = this.getLogger();
            if (logger != null && this.isVerbose()) {
                logger.log(new IllegalStateException("ResponseCode " + responseCode + " is unsupported!"));
            }
            return null;
        }
        newRequest.setURL(newURL);
        return newRequest;
    }

    public Request createRequest(final Form form) throws Exception {
        return this.createFormRequest(form);
    }

    public GetRequest createRequest(final String downloadURL) throws Exception {
        return this.createGetRequest(downloadURL);
    }

    public void disconnect() {
        try {
            this.getRequest().disconnect();
        } catch (final Throwable ignore) {
        }
    }

    /**
     * Downloads the contents behind con to file. if(con ==null), the latest request is downloaded. Useful for redirects
     *
     * @param file
     * @param con
     * @throws IOException
     */
    public void downloadConnection(final File file, final URLConnectionAdapter con) throws IOException {
        if (con == null) {
            Browser.download(file, this.getHttpConnection());
        } else {
            Browser.download(file, con);
        }
    }

    public String followConnection() throws IOException {
        final LogInterface llogger = this.getLogger();
        final Request lRequest = this.getRequest();
        if (lRequest == null) {
            throw new IllegalStateException("Request is null");
        }
        if (lRequest.getHtmlCode() != null) {
            if (llogger != null) {
                llogger.warning("Request has already been read");
            }
            return lRequest.getHTMLSource();
        }
        return this.loadConnection(lRequest.getHttpConnection()).getHTMLSource();
    }

    /**
     * Zeigt debuginformationen auch im Hauptprogramm an
     *
     * @param b
     */
    public void forceDebug(final boolean b) {
        this.debug = b;
    }

    public void forwardCookies(final Request request) {
        if (request != null) {
            final String host = Browser.getHost(request.getURL());
            final Map<String, Cookies> map = this.getCookies();
            final Cookies cookies;
            synchronized (map) {
                cookies = map.get(host);
            }
            if (cookies != null) {
                final Cookies requestCookies = request.getCookies();
                for (final Cookie cookie : cookies.getCookies()) {
                    if (!cookie.isExpired()) {
                        requestCookies.add(cookie);
                    }
                }
            }
        }
    }

    public String getAcceptLanguage() {
        return this.acceptLanguage;
    }

    /**
     * @return the allowedResponseCodes
     */
    public int[] getAllowedResponseCodes() {
        return this.allowedResponseCodes;
    }

    public String getBaseURL() throws MalformedURLException {
        final Request lRequest = this.getRequest();
        if (lRequest != null) {
            return URLHelper.getBaseURL(lRequest.getURL());
        } else {
            return null;
        }
    }

    /**
     * returns current ConnectTimeout
     *
     * @return
     */
    public int getConnectTimeout() {
        return this.connectTimeout < 0 ? Browser.TIMEOUT_CONNECT : this.connectTimeout;
    }

    public String getCookie(final String url, final String key) {
        final Cookies cookies = this.getCookies(url);
        final Cookie cookie = cookies.get(key);
        return cookie != null ? cookie.getValue() : null;
    }

    public HashMap<String, Cookies> getCookies() {
        return this.cookiesExclusive ? this.cookies : Browser.COOKIES;
    }

    public Cookies getCookies(final String url) {
        final String host = Browser.getHost(url);
        final Map<String, Cookies> map = this.getCookies();
        synchronized (map) {
            Cookies cookies = map.get(host);
            if (cookies == null) {
                map.put(host, cookies = new Cookies());
            }
            return cookies;
        }
    }

    public void getDownload(final File file, final String urlString) throws IOException {
        URLConnectionAdapter con = null;
        try {
            con = this.openGetConnection(urlString);
            Browser.download(file, con);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    public Form getForm(final int i) {
        final Form[] forms = this.getForms();
        return forms.length <= i ? null : forms[i];
    }

    /**
     * Returns the first form that has an input filed with name key
     *
     * @param key
     * @return
     */
    public Form getFormbyKey(final String key) {
        for (final Form f : this.getForms()) {
            if (f.hasInputFieldByName(key)) {
                return f;
            }
        }
        return null;
    }

    /**
     * Returns the first form that has input field with 'key' that equals 'value'.
     *
     * @since JD2
     * @param key
     * @param value
     * @return
     */
    public Form getFormByInputFieldKeyValue(final String key, final String value) {
        for (final Form f : this.getForms()) {
            for (final InputField field : f.getInputFields()) {
                if (key != null && key.equals(field.getKey())) {
                    if (value == null && field.getValue() == null) {
                        return f;
                    }
                    if (value != null && value.equals(field.getValue())) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the first form that has input field with a property with 'key' that equals 'value'.
     *
     * @since JD2
     * @param key
     * @param value
     * @return
     */
    public Form getFormByInputFieldPropertyKeyValue(final String key, final String value) {
        for (final Form f : this.getForms()) {
            for (final InputField field : f.getInputFields()) {
                if (field.containsPropertyKeyValue(key, value)) {
                    return f;
                }
            }
        }
        return null;
    }

    public Form getFormbyProperty(final String property, final String name) {
        for (final Form form : this.getForms()) {
            if (form.getStringProperty(property) != null && form.getStringProperty(property).equalsIgnoreCase(name)) {
                return form;
            }
        }
        return null;
    }

    /**
     * returns first found form with given Action.
     *
     * @author raztoki
     * @since JD2
     * @param action
     * @return
     */
    public final Form getFormbyAction(final String action) {
        for (final Form form : this.getForms()) {
            if (action.equalsIgnoreCase(form.getAction())) {
                return form;
            }
        }
        return null;
    }

    /**
     * returns first found form with given Action. Searches performed by Regex,
     *
     * @author raztoki
     * @since JD2
     * @param action
     * @return
     */
    public final Form getFormbyActionRegex(final String action) {
        if (action == null) {
            return null;
        }
        for (final Form form : this.getForms()) {
            if (form.getAction() != null && new Regex(form.getAction(), action).matches()) {
                return form;
            }
        }
        return null;
    }

    /**
     * Returns the first form with an Submitvalue of name<br />
     * Note: String needs to be urlEncoded as values it's comparing against are!
     *
     *
     * @param name
     * @return
     */
    public Form getFormBySubmitvalue(final String name) {
        for (final Form form : this.getForms()) {
            try {
                form.setPreferredSubmit(name);
                return form;
            } catch (final IllegalArgumentException e) {
            }
        }
        return null;
    }

    /**
     * finds first form that matches regex
     *
     * @author raztoki
     * @param regex
     * @return
     **/
    public final Form getFormByRegex(final String regex) {
        if (regex == null) {
            return null;
        }
        final Form[] forms = this.getForms();
        for (final Form form : forms) {
            if (form.containsHTML(regex)) {
                return form;
            }
        }
        return null;
    }

    public Form[] getForms() {
        return Form.getForms(this);
    }

    /**
     *
     * same as getFormbyAction
     *
     * @author raztoki
     * @since JD2
     * @param action
     * @return
     */
    public Form[] getFormsByAction(final String action) {
        final ArrayList<Form> results = new ArrayList<Form>();
        for (final Form form : this.getForms()) {
            if (StringUtils.equalsIgnoreCase(form.getAction(), action)) {
                results.add(form);
            }
        }
        return results.toArray(new Form[results.size()]);
    }

    /**
     * same as getFormbyActionRegex
     *
     * @author raztoki
     * @since JD2
     * @param action
     * @return
     */
    public Form[] getFormsByActionRegex(final String action) {
        if (action == null) {
            return null;
        }
        final ArrayList<Form> results = new ArrayList<Form>();
        for (final Form form : this.getForms()) {
            if (form.getAction() != null && new Regex(form.getAction(), action).matches()) {
                results.add(form);
            }
        }
        return results.toArray(new Form[results.size()]);
    }

    /**
     * finds All forms that matches regex
     *
     * @author raztoki
     * @param regex
     * @return
     */
    public final Form[] getFormsByRegex(final String regex) {
        if (regex == null) {
            return null;
        }
        final ArrayList<Form> results = new ArrayList<Form>();
        final Form[] forms = this.getForms();
        for (final Form form : forms) {
            if (form.containsHTML(regex)) {
                results.add(form);
            }
        }
        return results.toArray(new Form[results.size()]);
    }

    public RequestHeader getHeaders() {
        RequestHeader lHeaders = this.headers;
        if (lHeaders == null) {
            lHeaders = new RequestHeader();
            this.headers = lHeaders;
        }
        return lHeaders;
    }

    public String getHost() {
        final Request lRequest = this.getRequest();
        return lRequest == null ? null : Browser.getHost(lRequest.getURL(), false);
    }

    public String getHost(final boolean subdomain) {
        final Request lRequest = this.getRequest();
        return lRequest == null ? null : Browser.getHost(lRequest.getURL(), subdomain);
    }

    public URLConnectionAdapter getHttpConnection() {
        final Request lRequest = this.getRequest();
        if (lRequest == null) {
            return null;
        }
        return lRequest.getHttpConnection();
    }

    /**
     * Gets Browser upper page load limit Byte value.
     *
     * @since JD2
     * @param i
     */
    public int getLoadLimit() {
        return this.limit;
    }

    public LogInterface getLogger() {
        final LogInterface llogger = this.logger;
        if (llogger != null) {
            return llogger;
        }
        return Browser.LOGGER;
    }

    public String getMatch(final String string) {
        return this.getRegex(string).getMatch(0);
    }

    public String getPage(final String string) throws IOException {
        return this.getPage(this.createGetRequest(string));
    }

    public String getPage(Request request) throws IOException {
        return this.loadConnection(this.openRequestConnection(request)).getHTMLSource();
    }

    public String getPage(final URL url) throws IOException {
        return this.getPage(url.toString());
    }

    public ProxySelectorInterface getProxy() {
        return this.proxy;
    }

    /**
     * returns current ReadTimeout
     *
     * @return
     */
    public int getReadTimeout() {
        return this.readTimeout < 0 ? Browser.TIMEOUT_READ : this.readTimeout;
    }

    /**
     * If automatic redirectfollowing is disabled, you can get the redirect URL if there is any.
     *
     * @return
     */
    public String getRedirectLocation() {
        final Request lRequest = this.getRequest();
        if (lRequest == null) {
            return null;
        }
        return lRequest.getLocation();
    }

    public Regex getRegex(final Pattern compile) {
        return new Regex(this, compile);
    }

    public Regex getRegex(final String string) {
        return new Regex(this, string);
    }

    /**
     * Gets the latest request
     *
     * @return
     */
    public Request getRequest() {
        return this.request;
    }

    public ProxySelectorInterface getThreadProxy() {
        final Thread currentThread = Thread.currentThread();
        /**
         * return BrowserSettings from current thread if available
         */
        if (currentThread != null && currentThread instanceof BrowserSettings) {
            final BrowserSettings settings = (BrowserSettings) currentThread;
            return settings.getProxySelector();
        }
        return null;
    }

    public String getURL() {
        final Request lRequest = this.getRequest();
        return lRequest == null ? null : lRequest.getUrl();
    }

    public URL _getURL() {
        final Request lRequest = this.getRequest();
        return lRequest == null ? null : lRequest.getURL();
    }

    /**
     * Tries to get a full URL out of string
     *
     * @throws BrowserException
     */
    public URL getURL(String location) throws IOException {
        if (location == null) {
            location = this.getRedirectLocation();
        }
        if (location == null) {
            throw new IllegalArgumentException("location is null");
        }
        try {
            if (location.matches("^(https?|ftp)://.+")) {
                return URLHelper.fixPathTraversal(URLHelper.createURL(location.replaceAll(" ", "%20")));
            } else {
                final Request lRequest = this.getRequest();
                if (lRequest == null) {
                    throw new IOException("No request available:" + location);
                } else {
                    return URLHelper.createURL(URLHelper.parseLocation(lRequest.getURL(), location));
                }
            }
        } catch (final MalformedURLException e) {
            final Request lRequest = this.getRequest();
            if (lRequest == null) {
                throw new IOException("No request available:" + location, e);
            } else {
                return URLHelper.createURL(URLHelper.parseLocation(lRequest.getURL(), location));
            }
        }
    }

    public boolean isCookiesExclusive() {
        return this.cookiesExclusive;
    }

    public boolean isDebug() {
        return this.debug || this.isVerbose();
    }

    public boolean isFollowingRedirects() {
        return this.doRedirects;
    }

    public boolean isKeepResponseContentBytes() {
        return this.keepResponseContentBytes;
    }

    public boolean isVerbose() {
        return Browser.VERBOSE || this.verbose;
    }

    /**
     * Reads the content behind a con and returns them. Note: if con==null, the current request is read. This is useful for redirects. Note
     * #2: if a connection is loaded, data is not stored in the browser instance.
     *
     * @param con
     * @return
     * @throws IOException
     */
    public Request loadConnection(final URLConnectionAdapter connection) throws IOException {
        final Request requ;
        if (connection == null) {
            requ = this.getRequest();
        } else if (connection.getRequest() != null) {
            requ = connection.getRequest();
        } else {
            requ = new Request(connection) {
                @Override
                public long postRequest() throws IOException {
                    return 0;
                }

                @Override
                public void preRequest() throws IOException {
                }
            };
        }
        try {
            if (requ == null) {
                throw new IOException("No request available");
            } else {
                this.checkContentLengthLimit(requ);
                requ.read(this.isKeepResponseContentBytes());
                if (this.isVerbose() && requ != null) {
                    final LogInterface llogger = this.getLogger();
                    if (llogger != null) {
                        llogger.finest("\r\nBrowserID:" + requ.getBrowserID() + "|RequestID:" + requ.getRequestID() + "|URL:" + requ.getURL() + "\r\n----------------Request Content-------------\r\n" + requ.getHTMLSource() + "\r\n");
                    }
                }
                return requ;
            }
        } catch (final BrowserException e) {
            throw e;
        } catch (final IOException e) {
            throw new BrowserException(e, requ);
        } finally {
            if (requ != null) {
                requ.disconnect();
            }
        }
    }

    private void mergeHeaders(final Request request) {
        final RequestHeader lHeaders = this.headers;
        if (request != null && lHeaders != null) {
            final RequestHeader requestHeaders = request.getHeaders();
            if (lHeaders.isDominant()) {
                requestHeaders.clear();
            }
            requestHeaders.putAll(lHeaders);
        }
    }

    /**
     * Opens a new connection based on a Form
     *
     * @param form
     * @return
     * @throws Exception
     */
    public URLConnectionAdapter openFormConnection(final Form form) throws Exception {
        return this.openRequestConnection(this.createFormRequest(form));
    }

    /**
     * Opens a new get connection
     *
     * @param string
     * @return
     * @throws IOException
     */
    public URLConnectionAdapter openGetConnection(final String string) throws IOException {
        return this.openRequestConnection(this.createGetRequest(string));
    }

    /**
     * @since JD2
     **/
    public URLConnectionAdapter openHeadConnection(final String string) throws IOException {
        return this.openRequestConnection(this.createHeadRequest(string));
    }

    /**
     * Opens a Post Connection based on a variable HashMap
     *
     * @deprecated Use {@link #openPostConnection(String, UrlQuery)} instead
     */
    @Deprecated
    public URLConnectionAdapter openPostConnection(final String url, final LinkedHashMap<String, String> post) throws IOException {
        return this.openRequestConnection(this.createPostRequest(url, UrlQuery.get(post)));
    }

    /**
     * OPens a new Post connection based on a query string
     *
     * @deprecated Use {@link #openPostConnection(String, UrlQuery)} instead
     */
    @Deprecated
    public URLConnectionAdapter openPostConnection(final String url, final String post) throws IOException {
        return this.openPostConnection(url, Request.parseQuery(post));
    }

    public URLConnectionAdapter openPostConnection(String url, UrlQuery query) throws IOException {
        return this.openRequestConnection(this.createPostRequest(url, query));
    }

    private void setRequestProperties(final Request sourceRequest, final Request nextRequest, final String refererURL) {
        if (nextRequest != null) {
            if (nextRequest.isSSLTrustALLSet() == null) {
                nextRequest.setSSLTrustALL(this.getDefaultSSLTrustALL());
            }
            this.forwardCookies(nextRequest);
            if (nextRequest.getCustomCharset() == null) {
                nextRequest.setCustomCharset(this.customCharset);
            }
            if (!nextRequest.getHeaders().contains(HTTPConstants.HEADER_REQUEST_ACCEPT_LANGUAGE)) {
                nextRequest.getHeaders().put(HTTPConstants.HEADER_REQUEST_ACCEPT_LANGUAGE, this.getAcceptLanguage());
            }
            nextRequest.setConnectTimeout(this.getConnectTimeout());
            nextRequest.setReadTimeout(this.getReadTimeout());
            final boolean allowRefererURL;
            if (sourceRequest != null && StringUtils.startsWithCaseInsensitive(sourceRequest.getURL().getProtocol(), "https")) {
                // http://allben.net/post/2009/02/25/Null-Url-Referrer-going-from-HTTPS-to-HTTP
                allowRefererURL = StringUtils.startsWithCaseInsensitive(nextRequest.getURL().getProtocol(), "https");
            } else {
                allowRefererURL = true;
            }
            if (allowRefererURL && refererURL != null && !nextRequest.getHeaders().contains(HTTPConstants.HEADER_REQUEST_REFERER)) {
                nextRequest.getHeaders().put(HTTPConstants.HEADER_REQUEST_REFERER, refererURL);
            }
            this.mergeHeaders(nextRequest);
        }
    }

    public URLConnectionAdapter openRequestConnection(final Request request) throws IOException {
        return this.openRequestConnection(request, this.isFollowingRedirects());
    }

    private final static AtomicLong          BROWSERIDS                   = new AtomicLong(0);
    private final AtomicLong                 requestID                    = new AtomicLong(0);
    protected static AuthenticationFactory   DEFAULTAUTHENTICATIONFACTORY = new URLUserInfoAuthentication();
    protected volatile AuthenticationFactory authenticationFactory        = null;

    public static AuthenticationFactory getDefaultAuthenticationFactory() {
        return Browser.DEFAULTAUTHENTICATIONFACTORY;
    }

    public static void setDefaultAuthenticationFactory(AuthenticationFactory authenticationFactory) {
        Browser.DEFAULTAUTHENTICATIONFACTORY = authenticationFactory;
    }

    public AuthenticationFactory getCustomAuthenticationFactory() {
        return this.authenticationFactory;
    }

    public AuthenticationFactory getAuthenticationFactory() {
        final AuthenticationFactory ret = this.getCustomAuthenticationFactory();
        if (ret != null) {
            return ret;
        } else {
            return Browser.getDefaultAuthenticationFactory();
        }
    }

    public void setCustomAuthenticationFactory(AuthenticationFactory authenticationFactory) {
        this.authenticationFactory = authenticationFactory;
    }

    public void clearAuthentications() {
        this.authentications.clear();
    }

    private long browserID       = -1;
    private long browserParentID = -1;

    public synchronized long getBrowserID() {
        if (this.browserID == -1) {
            this.browserID = Browser.BROWSERIDS.incrementAndGet();
        }
        return this.browserID;
    }

    public long getBrowserParentID() {
        return this.browserParentID;
    }

    protected long getNextRequestID() {
        return this.requestID.incrementAndGet();
    }

    protected CopyOnWriteArrayList<Authentication> authentications = new CopyOnWriteArrayList<Authentication>();

    public boolean addAuthentication(Authentication authentication) {
        if (authentication != null) {
            return this.authentications.addIfAbsent(authentication);
        } else {
            return false;
        }
    }

    public boolean removeAuthentication(Authentication authentication) {
        return authentication != null && this.authentications.remove(authentication);
    }

    public List<Authentication> getAuthentications() {
        return this.authentications;
    }

    protected Authentication applyAuthentication(Request request) throws IOException {
        final Authentication authentication = request.getAuthentication();
        if (authentication == null) {
            final AuthenticationFactory authenticationFactory = this.getAuthenticationFactory();
            if (authenticationFactory != null) {
                final Authentication authenticationByFactory = authenticationFactory.authorize(this, request);
                if (authenticationByFactory != null) {
                    request.setAuthentication(authenticationByFactory);
                    return authenticationByFactory;
                }
            }
            for (final Authentication browserAuthentication : this.getAuthentications()) {
                if (browserAuthentication.authorize(this, request)) {
                    request.setAuthentication(browserAuthentication);
                    return browserAuthentication;
                }
            }
        } else {
            if (authentication.authorize(this, request)) {
                return authentication;
            }
        }
        return null;
    }

    protected boolean retryAuthentication(Request request) throws IOException {
        final Authentication requestAuthentication = request.getAuthentication();
        final AuthenticationFactory authenticationFactory = this.getAuthenticationFactory();
        if (requestAuthentication != null) {
            if (authenticationFactory != null) {
                return authenticationFactory.retry(requestAuthentication, this, request);
            } else {
                return requestAuthentication.retry(this, request);
            }
        } else if (requestAuthentication == null && authenticationFactory != null) {
            final Authentication authentication = authenticationFactory.buildAuthentication(this, request);
            if (authentication != null) {
                request.setAuthentication(authentication);
            }
            return authentication != null;
        }
        return false;
    }

    public URLConnectionAdapter openRequestConnection(Request request, final boolean followRedirects) throws IOException {
        if (request == null) {
            throw new IllegalStateException("Request is null");
        }
        int redirectLoopPrevention = 0;
        final Request originalRequest = request;
        final String refererURL = this.getRefererURL();
        while (true) {
            this.setRequestProperties(originalRequest, request, refererURL);
            int connectRetryCounter = 0;
            connectLoop: while (true) {
                try {
                    // connect may throw ProxyAuthException for https or direct connection method requests
                    try {
                        Browser.waitForPageAccess(this, request);
                    } catch (final InterruptedException e) {
                        throw new BrowserException("requestIntervalTime Exception", request, e);
                    }
                    final URLConnectionAdapter connection;
                    try {
                        if (request.getProxy() == null) {
                            final List<HTTPProxy> proxies = this.selectProxies(request.getURL());
                            // choose first one
                            request.setProxy(proxies.get(0));
                        }
                        this.applyAuthentication(request);
                        connection = request.connect(this).getHttpConnection();
                    } finally {
                        this.updateCookies(request);
                        if (this.isDebug()) {
                            final LogInterface llogger = this.getLogger();
                            if (llogger != null) {
                                try {
                                    llogger.finest("\r\n" + request.printHeaders());
                                } catch (final Throwable e) {
                                    llogger.log(e);
                                }
                            }
                        }
                    }
                    if (connection != null) {
                        if (this.retryAuthentication(request)) {
                            this.loadConnection(connection);
                            request.resetConnection();
                            continue connectLoop;
                        }
                        connection.setAllowedResponseCodes(this.getAllowedResponseCodes());
                        if (connection.getResponseCode() == 407) {
                            throw new ProxyAuthException(request.getProxy());
                        }
                    } else {
                        throw new BrowserException("connection is null", request);
                    }
                    break;
                } catch (BrowserException e) {
                    request.disconnect();
                    throw e;
                } catch (IOException e) {
                    request.disconnect();
                    final LogInterface llogger = this.getLogger();
                    if (llogger != null) {
                        llogger.log(e);
                    }
                    connectRetryCounter++;
                    if (this.reportConnectException(connectRetryCounter, e, request) || e instanceof ProxyAuthException && this.updateProxy(connectRetryCounter, request)) {
                        // reset proxy
                        request.setProxy(null);
                        continue connectLoop;
                    } else {
                        throw new BrowserException(e, request);
                    }
                }
            }
            final String redirect = request.getLocation();
            if (followRedirects && redirect != null) {
                if (redirectLoopPrevention++ > 20) {
                    request.disconnect();
                    throw new BrowserException("Too many redirects!", originalRequest);
                }
                /**
                 * needs loadConnection for keep-Alive
                 */
                this.loadConnection(request.getHttpConnection());
                final Request redirectRequest = this.createRedirectFollowingRequest(request);
                this.setRequest(request);
                if (redirectRequest == null) {
                    return request.getHttpConnection();
                } else {
                    request = redirectRequest;
                }
            } else {
                this.setRequest(request);
                return request.getHttpConnection();
            }
        }
    }

    private boolean reportConnectException(final int proxyRetryCounter, final IOException e, final Request request) {
        final ProxySelectorInterface selector;
        if (this.proxy != null) {
            selector = this.proxy;
        } else {
            selector = Browser.GLOBAL_PROXY;
        }
        return selector != null && selector.reportConnectException(request, proxyRetryCounter, e);
    }

    /**
     * loads a new page (post)
     *
     * @deprecated Use {@link #postPage(String, UrlQuery)} instead
     */
    @Deprecated
    public String postPage(final String url, final LinkedHashMap<String, String> post) throws IOException {
        return this.postPage(url, UrlQuery.get(post));
    }

    /**
     * loads a new page (POST)
     *
     * @deprecated Use {@link #postPage(String, UrlQuery)} or {@link #postPageRaw(String, String) instead
     */
    @Deprecated
    public String postPage(final String url, final String post) throws IOException {
        return this.postPage(url, Request.parseQuery(post));
    }

    public String postPage(String url, UrlQuery queryInfo) throws IOException {
        return this.getPage(this.createPostRequest(url, queryInfo));
    }

    public String postPageRaw(final String url, final byte[] post) throws IOException {
        final PostRequest request = this.createPostRequest(url, new ArrayList<KeyValueStringEntry>(), null);
        request.setPostBytes(post);
        return this.getPage(request);
    }

    /**
     * loads a new page (post) the postdata is given by the poststring. It will be sent as is
     */
    public String postPageRaw(final String url, final String post) throws IOException {
        final PostRequest request = this.createPostRequest(url, new ArrayList<KeyValueStringEntry>(), null);
        request.setPostDataString(post);
        return this.getPage(request);
    }

    public List<HTTPProxy> selectProxies(final URL url) throws IOException {
        final ProxySelectorInterface selector;
        if (this.proxy != null) {
            selector = this.proxy;
        } else {
            selector = Browser.GLOBAL_PROXY;
        }
        if (selector == null) {
            final ArrayList<HTTPProxy> ret = new ArrayList<HTTPProxy>();
            ret.add(HTTPProxy.NONE);
            return ret;
        }
        final List<HTTPProxy> list;
        try {
            list = selector.getProxiesByURL(url);
        } catch (Throwable e) {
            throw new NoGateWayException(selector, e);
        }
        if (list == null || list.size() == 0) {
            throw new NoGateWayException(selector, "No Gateway or Proxy Found: " + url);
        }
        return list;
    }

    public void setAcceptLanguage(final String acceptLanguage) {
        this.acceptLanguage = acceptLanguage;
    }

    /**
     * @param allowedResponseCodes
     *            the allowedResponseCodes to set
     * @since JD2
     */
    public void setAllowedResponseCodes(final int... allowedResponseCodes) {
        this.allowedResponseCodes = allowedResponseCodes;
    }

    public boolean hasAllowedResponseCode(final int input) {
        final int[] original = this.getAllowedResponseCodes();
        for (final int a : original) {
            if (a == input) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds input to existing response codes. This solves the issue were setAllowedResponseCodes(int...) destroys old with new.
     *
     * @param input
     * @author raztoki
     * @since JD2
     */
    public void addAllowedResponseCodes(final int... input) {
        final int[] original = this.getAllowedResponseCodes();
        final HashSet<Integer> dupe = new HashSet<Integer>();
        for (final int a : original) {
            dupe.add(a);
        }
        for (final int a : input) {
            dupe.add(a);
        }
        final int[] outcome = new int[dupe.size()];
        int index = 0;
        for (final Integer i : dupe) {
            outcome[index++] = i;
        }
        this.setAllowedResponseCodes(outcome);
    }

    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setCookie(final String url, final String key, final String value) {
        final Cookies cookies = this.getCookies(url);
        cookies.add(new Cookie(Browser.getHost(url), key, value));
    }

    /**
     * Adds given Cookies to current Cookies session for given host.
     *
     * @author raztoki
     * @since JD2
     * @param url
     * @param iCookies
     * @param replace
     */
    public void setCookies(final String url, final Cookies iCookies) {
        this.setCookies(url, iCookies, false);
    }

    /**
     * Adds given Cookies to current Cookies session for given host. replace when true will dump _all_ Cookies
     *
     * @author raztoki
     * @since JD2
     * @param url
     * @param iCookies
     * @param replace
     */
    public void setCookies(final String url, final Cookies set, final boolean replace) {
        final Cookies cookies = this.getCookies(url);
        if (replace) {
            cookies.clear();
            cookies.add(set);
        } else {
            cookies.add(set);
        }
    }

    public void setCookiesExclusive(final boolean b) {
        if (this.cookiesExclusive == b) {
            return;
        }
        if (b) {
            this.cookies.clear();
            for (final Entry<String, Cookies> next : Browser.COOKIES.entrySet()) {
                Cookies tmp;
                this.cookies.put(next.getKey(), tmp = new Cookies());
                tmp.add(next.getValue());
            }
        } else {
            this.cookies.clear();
        }
        // this needs to be last so you can drop to frame, in test situations. Otherwise this reference is destroyed.
        this.cookiesExclusive = b;
    }

    /**
     *
     * sets CurrentURL (used for referer)
     *
     * null -> null
     *
     * empty -> do not set referer for next request
     *
     * other -> use given referer for next request
     *
     * @param string
     * @since JD2
     */
    public void setCurrentURL(final String url) throws MalformedURLException {
        if (url == null) {
            this.currentURL = null;
        } else if (StringUtils.isEmpty(url)) {
            this.currentURL = "";
        } else {
            this.currentURL = url;
        }
    }

    /**
     * returns referer for next request
     *
     * 1.) getAndClear referer from browser.getHeaders
     *
     * 2.) if 1==null, get currentURL (String)
     *
     * 3.) if 2==null, get url from getURL(returns url from last request)
     *
     * @return
     */
    private String getRefererURL() {
        final String refererURLHeader = this.getHeaders().remove(HTTPConstants.HEADER_REQUEST_REFERER);
        if (refererURLHeader == null) {
            final Object lCurrentURL = this.currentURL;
            if (lCurrentURL != null && lCurrentURL instanceof String) {
                return (String) lCurrentURL;
            }
            if (lCurrentURL != null && lCurrentURL instanceof Request) {
                return ((Request) lCurrentURL).getUrl();
            }
            return this.getURL();
        } else {
            return refererURLHeader;
        }
    }

    public void setCustomCharset(final String charset) {
        this.customCharset = charset;
    }

    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    public void setFollowRedirects(final boolean b) {
        this.doRedirects = b;
    }

    /**
     * do not below revision 10000
     *
     * @since JD2
     */
    public void setHeader(final String field, final String value) {
        this.getHeaders().put(field, value);
    }

    public void setHeaders(final RequestHeader h) {
        this.headers = h;
    }

    public void setKeepResponseContentBytes(final boolean keepResponseContentBytes) {
        this.keepResponseContentBytes = keepResponseContentBytes;
    }

    /**
     * Sets Browser upper page load limit Byte value.
     *
     * @since JD2
     * @param i
     */
    public void setLoadLimit(final int i) {
        this.limit = Math.max(0, i);
    }

    public void setLogger(final LogInterface logger) {
        this.logger = logger;
    }

    @Deprecated
    /**
     * @deprecated
     * @param proxy2
     */
    public void setProxy(final HTTPProxy proxy2) {
        this.setProxySelector(new StaticProxySelector(proxy2));
    }

    @Deprecated
    /**
     * for usage in plugins for stable compatibility only
     *
     * @param threadProxy
     */
    public void setProxy(final ProxySelectorInterface threadProxy) {
        this.setProxySelector(threadProxy);
    }

    public void setProxySelector(ProxySelectorInterface proxy) {
        final ProxySelectorInterface wished = proxy;
        if (proxy == null) {
            proxy = this.getThreadProxy();
        }
        if (proxy == this.proxy) {
            return;
        }
        this.proxy = proxy;
        if (this.debug) {
            final LogInterface llogger = this.getLogger();
            if (llogger != null) {
                llogger.info("Use local proxy: " + proxy + " wished: " + wished);
            }
        }
    }

    public void setReadTimeout(final int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setRequest(final Request request) {
        if (request == null) {
            this.currentURL = null;
        } else {
            this.currentURL = request.getUrl();
        }
        this.updateCookies(request);
        this.request = request;
    }

    public void setRequestIntervalLimit(final String host, final int i) {
        final String domain = Browser.getHost(host);
        if (domain == null) {
            return;
        }
        if (this.requestIntervalLimitMap == null) {
            this.requestTimeMap = new HashMap<String, Long>();
            this.requestIntervalLimitMap = new HashMap<String, Integer>();
        }
        this.requestIntervalLimitMap.put(domain, i);
    }

    public void setVerbose(final boolean b) {
        this.verbose = b;
    }

    public String submitForm(final Form form) throws IOException {
        return this.getPage(this.createFormRequest(form));
    }

    @Override
    public String toString() {
        final Request lRequest = this.getRequest();
        if (lRequest == null) {
            return "Browser. No request yet";
        }
        return lRequest.getHTMLSource();
    }

    public void updateCookies(final Request request) {
        if (request != null && request.hasCookies()) {
            final Cookies cookies = this.getCookies(Browser.getHost(request.getURL()));
            cookies.add(request.getCookies());
        }
    }

    /**
     * can update the connection information - for example ask for proxy auth.
     *
     * @param request
     * @param proxyRetryCounter
     *
     * @return true if a failed request should be done again.
     */
    protected boolean updateProxy(final int proxyRetryCounter, final Request request) {
        final ProxySelectorInterface selector;
        if (this.proxy != null) {
            selector = this.proxy;
        } else {
            selector = Browser.GLOBAL_PROXY;
        }
        return selector != null && selector.updateProxy(request, proxyRetryCounter);
    }

    /**
     * @return the defaultSSLTrustALL
     */
    public Boolean getDefaultSSLTrustALL() {
        return this.defaultSSLTrustALL;
    }

    /**
     * @param defaultSSLTrustALL
     *            the defaultSSLTrustALL to set
     */
    public void setDefaultSSLTrustALL(Boolean defaultSSLTrustALL) {
        this.defaultSSLTrustALL = defaultSSLTrustALL;
    }
}
