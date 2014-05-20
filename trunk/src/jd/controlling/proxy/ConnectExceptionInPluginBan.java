package jd.controlling.proxy;

import jd.plugins.Plugin;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.translate._JDT;

public class ConnectExceptionInPluginBan extends PluginRelatedConnectionBan {

    public ConnectExceptionInPluginBan(Plugin plg, AbstractProxySelectorImpl proxySelector, HTTPProxy proxy) {
        super(plg, proxySelector, proxy);
        created = System.currentTimeMillis();

    }

    @Override
    public String toString() {
        return _JDT._.ConnectExceptionInPluginBan(proxy.toString());
    }

    private long created;

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() - created > 15 * 60 * 1000l;
    }

}
