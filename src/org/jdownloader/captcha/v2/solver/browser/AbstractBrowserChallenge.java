package org.jdownloader.captcha.v2.solver.browser;

import java.awt.Rectangle;
import java.io.IOException;

import jd.controlling.accountchecker.AccountChecker.AccountCheckJob;
import jd.controlling.accountchecker.AccountCheckerThread;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkchecker.LinkCheckerThread;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.plugins.Account;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;

import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.solverjob.ResponseList;

public abstract class AbstractBrowserChallenge extends Challenge<String> {

    private Plugin plugin;

    public Plugin getPlugin() {
        return plugin;
    }

    public boolean isSolved() {
        final ResponseList<String> results = getResult();
        return results != null && results.getValue() != null;
    }

    public AbstractBrowserChallenge(String method, Plugin pluginForHost) {

        super(method, null);
        this.plugin = pluginForHost;
        if (pluginForHost == null) {
            plugin = getPluginFromThread();
        }
    }

    abstract public String getHTML();

    abstract public BrowserViewport getBrowserViewport(BrowserWindow screenResource, Rectangle elementBounds);

    public boolean onGetRequest(BrowserReference browserReference, GetRequest request, HttpResponse response) throws IOException {
        return false;
    }

    public boolean onPostRequest(BrowserReference browserReference, PostRequest request, HttpResponse response) throws IOException {

        return false;

    }

    private Plugin getPluginFromThread() {
        final Thread thread = Thread.currentThread();
        if (thread instanceof AccountCheckerThread) {
            final AccountCheckJob job = ((AccountCheckerThread) thread).getJob();
            if (job != null) {
                final Account account = job.getAccount();
                return account.getPlugin();
            }
        } else if (thread instanceof LinkCheckerThread) {
            final PluginForHost plg = ((LinkCheckerThread) thread).getPlugin();
            if (plg != null) {
                return plg;
            }
        } else if (thread instanceof SingleDownloadController) {
            return ((SingleDownloadController) thread).getDownloadLinkCandidate().getCachedAccount().getPlugin();
        } else if (thread instanceof LinkCrawlerThread) {
            final Object owner = ((LinkCrawlerThread) thread).getCurrentOwner();
            if (owner instanceof Plugin) {
                return (Plugin) owner;
            }
        }
        return null;
    }

    public String getHttpPath() {
        if (plugin != null) {
            return plugin.getHost();
        }
        Thread th = Thread.currentThread();
        return "jd";
    }

}
