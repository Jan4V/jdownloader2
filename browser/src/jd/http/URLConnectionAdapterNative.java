package jd.http;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;

import jd.http.requests.PostFormDataRequest;
import jd.http.requests.PostRequest;

import org.appwork.utils.net.httpconnection.HTTPConnectionUtils.IPVERSION;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.NativeHTTPConnectionImpl;
import org.appwork.utils.net.socketconnection.SocketConnection;

public class URLConnectionAdapterNative extends NativeHTTPConnectionImpl implements URLConnectionAdapter {
    private Request request;

    public URLConnectionAdapterNative(final URL url) {
        super(url);
    }

    public URLConnectionAdapterNative(final URL url, final HTTPProxy p) {
        super(url, p);
        // TODO Auto-generated constructor stub
    }

    @Override
    public InputStream getErrorStream() {
        try {
            return super.getInputStream();
        } catch (final IOException e) {
            return null;
        }
    }

    @Override
    public long getLongContentLength() {
        return this.getContentLength();
    }

    @Override
    public long[] getRange() {
        final long[] ret = super.getRange();
        if (ret == null && this.getResponseCode() == 206) {
            this.ranges = URLConnectionAdapterDirectImpl.buildFakeContentRange(this);
            return this.ranges;
        }
        return ret;
    }

    @Override
    public Request getRequest() {
        return this.request;
    }

    @Override
    public void setRequest(final Request request) {
        this.request = request;
    }

    @Override
    protected boolean isRequiresOutputStream() {
        return super.isRequiresOutputStream() || this.request != null && this.request.requireOutputStream();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (this.convertedInputStream == null && !RequestMethod.HEAD.equals(this.getRequestMethod())) {
            super.getInputStream();
            if (!this.isContentDecoded()) {
                final String encoding = this.getHeaderField("Content-Encoding");
                if ("br".equalsIgnoreCase(encoding)) {
                    this.convertedInputStream = new WrappedBrotliInputStream(this.convertedInputStream);
                    this.contentDecoded = true;
                }
            }
        }
        this.getEndPointSocketAddress();
        return super.getInputStream();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(300);
        final Request req = this.getRequest();
        if (req != null) {
            sb.append("Caller: " + req.getCaller());
            sb.append(URLConnectionAdapter.CRLF);
            sb.append("BrowserID:" + req.getBrowserID() + "|BrowserParentID:" + req.getBrowserParentID() + "|RequestID:" + req.getRequestID() + "|URL:" + req.getURL());
            sb.append(URLConnectionAdapter.CRLF);
        }
        sb.append(this.getRequestInfo());
        if (req != null) {
            if (req instanceof PostRequest) {
                final String log = ((PostRequest) req).log();
                if (log != null) {
                    sb.append(URLConnectionAdapter.CRLF);
                    sb.append(log);
                }
            } else if (req instanceof PostFormDataRequest) {
                final String postDataString = ((PostFormDataRequest) req).getPostDataString();
                if (postDataString != null) {
                    sb.append(URLConnectionAdapter.CRLF);
                    sb.append(postDataString);
                }
            }
            sb.append(URLConnectionAdapter.CRLF);
        }
        sb.append(this.getResponseInfo());
        return sb.toString();
    }

    protected SocketAddress endPointSocketAddress;

    @Override
    public void disconnect() {
        try {
            this.getEndPointSocketAddress();
        } finally {
            super.disconnect();
        }
    }

    @Override
    public SocketAddress getEndPointSocketAddress() {
        if (this.endPointSocketAddress == null) {
            try {
                final Field instProxy = this.con.getClass().getDeclaredField("instProxy");
                instProxy.setAccessible(true);
                final Proxy proxy = (Proxy) instProxy.get(this.con);
                if (proxy != null) {
                    this.endPointSocketAddress = proxy.address();
                } else {
                    final Field http = this.con.getClass().getDeclaredField("http");
                    http.setAccessible(true);
                    final Object httpValue = http.get(this.con);
                    if (httpValue != null) {
                        final Field serverSocket = httpValue.getClass().getSuperclass().getDeclaredField("serverSocket");
                        serverSocket.setAccessible(true);
                        this.endPointSocketAddress = SocketConnection.getRootEndPointSocketAddress((Socket) serverSocket.get(httpValue));
                    }
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
        return this.endPointSocketAddress;
    }

    @Override
    public IPVERSION getIPVersion() {
        return IPVERSION.SYSTEM;
    }

    @Override
    public void setIPVersion(IPVERSION tcpVersion) {
    }
}
