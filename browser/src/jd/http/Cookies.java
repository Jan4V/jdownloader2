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
package jd.http;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;

import org.appwork.utils.StringUtils;

public class Cookies {
    public static Cookies parseSetCookies(final Request request) throws IOException {
        final URLConnectionAdapter httpConnection = request.getHttpConnection();
        final String date = httpConnection.getHeaderField("Date");
        final List<String> setCookies = httpConnection.getHeaderFields("Set-Cookie");
        final Cookies ret = new Cookies();
        if (setCookies != null && setCookies.size() > 0) {
            final String host = Browser.getHost(request.getURL());
            final String currentPath;
            if (StringUtils.isEmpty(request.getURL().getPath())) {
                currentPath = "/";
            } else {
                final String tmp = new Regex(request.getURL().getPath(), "(/.+)/").getMatch(0);
                if (tmp == null) {
                    currentPath = "/";
                } else {
                    currentPath = tmp;
                }
            }
            for (int i = 0; i < setCookies.size(); i++) {
                final String setCookie = setCookies.get(i);
                final Cookie cookie = new Cookie();
                cookie.setHost(host);
                cookie.setPath(currentPath);
                cookie.setHostTime(date);
                final StringTokenizer st = new StringTokenizer(setCookie, ";");
                while (st.hasMoreTokens()) {
                    final String cookieelement = st.nextToken().trim();
                    /* Key and Value */
                    final String st2[] = new Regex(cookieelement, "(.*?)=(.*)").getRow(0);
                    final String key;
                    final String value;
                    if (st2 == null || st2.length == 0) {
                        break;
                    } else if (st2.length == 1) {
                        key = st2[0].trim();
                        value = null;
                    } else if (st2.length == 2) {
                        key = st2[0].trim();
                        value = st2[1].trim();
                    } else {
                        continue;
                    }
                    if (key != null) {
                        if ("path".equalsIgnoreCase(key)) {
                            cookie.setPath(value);
                        } else if ("expires".equalsIgnoreCase(key)) {
                            cookie.setExpires(value);
                        } else if ("domain".equalsIgnoreCase(key)) {
                            cookie.setDomain(value);
                        } else if ("secure".equalsIgnoreCase(key)) {
                            cookie.setSecure(true);
                        } else if ("HttpOnly".equalsIgnoreCase(key)) {
                            // HttpOnly
                        } else if ("Max-Age".equalsIgnoreCase(key)) {
                            // Max-Age
                            try {
                                // RFC 6265, section 5.2.2
                                final long maxAge = Long.parseLong(value);
                                if (maxAge <= 0) {
                                    cookie.setExpireDate(1l);// 01/01/1970
                                } else if (cookie.getHostTime() > 0) {
                                    cookie.setExpireDate(cookie.getHostTime() + maxAge * 1000l);
                                }
                            } catch (final Throwable e) {
                            }
                        } else {
                            if (cookie.getKey() == null) {
                                cookie.setKey(key);
                                cookie.setValue(value);
                            }
                        }
                    }
                }
                if (cookie.getKey() != null) {
                    ret.add(cookie);
                }
            }
        }
        return ret;
    }

    public static Cookies parseCookies(final String cookieString, final String host, final String serverTime, boolean isSetCookie) {
        final String header = cookieString;
        String path = null;
        String expires = null;
        String domain = null;
        boolean secure = false;
        final LinkedHashMap<String, String> tmp = new LinkedHashMap<String, String>();
        /* Cookie individual elements */
        final StringTokenizer st = new StringTokenizer(header, ";");
        while (st.hasMoreTokens()) {
            final String cookieelement = st.nextToken().trim();
            /* Key and Value */
            final String st2[] = new Regex(cookieelement, "(.*?)=(.*)").getRow(0);
            final String key;
            final String value;
            if (st2 == null || st2.length == 0) {
                break;
            } else if (st2.length == 1) {
                key = st2[0].trim();
                value = null;
            } else if (st2.length == 2) {
                key = st2[0].trim();
                value = st2[1].trim();
            } else {
                continue;
            }
            if (key != null) {
                if ("path".equalsIgnoreCase(key)) {
                    path = value;
                } else if ("expires".equalsIgnoreCase(key)) {
                    expires = value;
                } else if ("domain".equalsIgnoreCase(key)) {
                    domain = value;
                } else if ("secure".equalsIgnoreCase(key)) {
                    secure = true;
                } else if ("HttpOnly".equalsIgnoreCase(key)) {
                    // HttpOnly
                } else if ("Max-Age".equalsIgnoreCase(key)) {
                    // Max-Age
                } else {
                    if (!isSetCookie || tmp.size() == 0) {
                        /**
                         * SetCookie only contains a single cookie
                         */
                        tmp.put(key, value);
                    }
                }
            }
        }
        final Cookies cookies = new Cookies();
        for (final Entry<String, String> next : tmp.entrySet()) {
            /*
             * no cookies are cookies without a value
             */
            if (next.getValue() != null) {
                final Cookie cookie = new Cookie();
                cookies.add(cookie);
                cookie.setHost(host);
                cookie.setPath(path);
                cookie.setDomain(domain);
                cookie.setExpires(expires);
                cookie.setValue(next.getValue());
                cookie.setKey(next.getKey());
                cookie.setHostTime(serverTime);
                cookie.setSecure(secure);
            }
        }
        return cookies;
    }

    public static Cookies parseCookies(final String cookieString, final String host, final String serverTime) {
        return Cookies.parseCookies(cookieString, host, serverTime, false);
    }

    private final CopyOnWriteArrayList<Cookie> cookies = new CopyOnWriteArrayList<Cookie>();

    public Cookies() {
    }

    public Cookies(final Cookies cookies) {
        this.add(cookies);
    }

    public synchronized void add(final Cookie cookie) {
        for (final Cookie cookie2 : this.cookies) {
            if (cookie2.equals(cookie)) {
                cookie2.update(cookie);
                return;
            }
        }
        this.cookies.add(cookie);
    }

    public void add(final Cookies newcookies) {
        if (newcookies != null) {
            for (final Cookie cookie : newcookies.getCookies()) {
                this.add(cookie);
            }
        }
    }

    public void clear() {
        this.cookies.clear();
    }

    public static String NOTDELETEDPATTERN = "^(?i)((?!^deleted$).)*$";

    public Cookie get(final String key, final String valuePattern) {
        final Cookie cookie = this.get(key);
        if (cookie != null) {
            if (valuePattern == null || Pattern.compile(valuePattern).matcher(cookie.getValue()).matches()) {
                return cookie;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public Cookie get(final String key) {
        if (key == null) {
            return null;
        }
        for (final Cookie cookie : this.cookies) {
            if (cookie.getKey().equals(key)) {
                return cookie;
            }
        }
        for (final Cookie cookie : this.cookies) {
            if (cookie.getKey().equalsIgnoreCase(key)) {
                return cookie;
            }
        }
        return null;
    }

    public List<Cookie> getCookies() {
        return this.cookies;
    }

    public boolean isEmpty() {
        return this.cookies.isEmpty();
    }

    public void remove(final Cookie cookie) {
        this.cookies.remove(cookie);
    }

    /**
     * Removes Cookie from current session, based on keyName
     *
     * @author raztoki
     * @since JD2
     * */
    public void remove(final String keyName) {
        if (keyName == null) {
            return;
        }
        final Cookie ckie = this.get(keyName);
        if (ckie != null) {
            this.cookies.remove(ckie);
        }
    }

    @Override
    public String toString() {
        final StringBuilder ret = new StringBuilder();
        for (final Cookie el : this.cookies) {
            if (ret.length() > 0) {
                ret.append("\r\n");
            }
            ret.append(el.toString());
        }
        return ret.toString();
    }
}
