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

package jd.plugins.decrypter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;


//create this patterns with UCMS.main
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ddl-kingz.in", "oxygen-warez.com", "filefox.in", "alphawarez.us", "pirate-loads.com", "fettrap.com", "omega-music.com", "hardcoremetal.biz", "flashload.org", "twin-warez.com", "oneload.org", "steelwarez.com", "fullstreams.info", "lionwarez.com", "1dl.in", "chrome-database.com", "oneload.org", "youwarez.biz", "saugking.net", "leetpornz.com", "freefiles4u.com", "dark-load.net", "crimeland.de", "get-warez.in", "meinsound.com", "projekt-tempel-news.de.vu", "datensau.org", "musik.am", "spreaded.net", "relfreaks.com", "babevidz.com", "serien24.com", "porn-freaks.net", "xxx-4-free.net", "porn-traffic.net", "chili-warez.net", "game-freaks.net", "isos.at", "your-load.com", "mov-world.net", "xtreme-warez.net", "sceneload.to", "epicspeedload.in", "serienfreaks.to", "serienfreaks.in", "warez-load.com",
        "ddl-scene.com", "mp3king.cinipac-hosting.biz", "sauggirls.com", "pornfox.in", "xflat24.com", "alben.ws", "worldofxxx.org", "gamegalaxy.ws", "ddl.byte.to", "interload.biz", "xwebb.extra.hu/1dl", "jokermovie.org", "xtreme-warez.biz", "your-load.com", "top-hitz.com", "wii-reloaded.ath.cx/sites/epic", "wankingking.com", "projekt-tempel-news.org", "porn-ox.in", "music-dome.cc", "sound-load.com", "hoerspiele.to", "jim2008.extra.hu", "ex-yu.extra.hu", "firefiles.in", "gez-load.net", "wrzunlimited.1gb.in", "streamload.in", "toxic.to", "mp3z.to", "sexload.to", "sound-load.com", "sfulc.exofire.net/cms", "fickdiehure.com", "dream-team.bz/cms", "omega-warez.com", "ddl-scene.cc", "xxxstreams.org", "scene-warez.com", "dokuh.tv", "titanload.to", "ddlshock.com", "xtreme-warez.us", "crunkwarez.com", "serienking.in", "stream.szenepic.us", "gate-warez.com", "gateload.info", "hot-porn-ddl.com" }, urls = {
        "http://[\\w\\.]*?ddl-kingz.in/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?oxygen-warez.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?filefox.in/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?alphawarez.us/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?pirate-loads.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?fettrap.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?omega-music.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)",
        "http://[\\w\\.]*?hardcoremetal.biz/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?flashload.org/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?twin-warez.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?oneload.org/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?steelwarez.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?fullstreams.info/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)",
        "http://[\\w\\.]*?lionwarez.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?1dl.in/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?chrome-database.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?oneload.org/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?youwarez.biz/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?saugking.net/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?leetpornz.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)",
        "http://[\\w\\.]*?freefiles4u.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?dark-load.net/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?crimeland.de/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?get-warez.in/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?meinsound.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?projekt-tempel-news.de.vu/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)",
        "http://[\\w\\.]*?datensau.org/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?musik.am/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?spreaded.net/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?relfreaks.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?babevidz.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?serien24.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?porn-freaks.net/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)",
        "http://[\\w\\.]*?xxx-4-free.net/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?porn-traffic.net/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?chili-warez.net/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?game-freaks.net/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?isos.at/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?your-load.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?mov-world.net/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)",
        "http://[\\w\\.]*?xtreme-warez.net/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?sceneload.to/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?epicspeedload.in/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?serienfreaks.to/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?serienfreaks.in/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?warez-load.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)",
        "http://[\\w\\.]*?ddl-scene.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?mp3king.cinipac-hosting.biz/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?sauggirls.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?pornfox.in/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?xflat24.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?alben.ws/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)",
        "http://[\\w\\.]*?worldofxxx.org/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?gamegalaxy.ws/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?ddl.byte.to/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?interload.biz/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?xwebb.extra.hu/1dl/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?jokermovie.org/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)",
        "http://[\\w\\.]*?xtreme-warez.biz/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?your-load.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?top-hitz.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?wii-reloaded.ath.cx/sites/epic/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?wankingking.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?projekt-tempel-news.org/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)",
        "http://[\\w\\.]*?porn-ox.in/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?music-dome.cc/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?sound-load.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?hoerspiele.to/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?jim2008.extra.hu/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?ex-yu.extra.hu/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?firefiles.in/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)",
        "http://[\\w\\.]*?gez-load.net/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?wrzunlimited.1gb.in/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?streamload.in/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?toxic.to/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?mp3z.to/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?sexload.to/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?sound-load.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)",
        "http://[\\w\\.]*?sfulc.exofire.net/cms/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?fickdiehure.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?dream-team.bz/cms/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?omega-warez.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?ddl-scene.cc/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?xxxstreams.org/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)",
        "http://[\\w\\.]*?scene-warez.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?dokuh.tv/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?titanload.to/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?ddlshock.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?xtreme-warez.us/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?crunkwarez.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?serienking.in/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)",
        "http://[\\w\\.]*?stream.szenepic.us/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?gate-warez.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?gateload.info/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)", "http://[\\w\\.]*?hot-porn-ddl.com/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)" }, flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 })
public class UCMS extends PluginForDecrypt {

    private Pattern PAT_CAPTCHA = Pattern.compile("<IMG SRC=\".*?/gfx/secure/", Pattern.CASE_INSENSITIVE);

    private Pattern PAT_NO_CAPTCHA = Pattern.compile("(<INPUT TYPE=\"SUBMIT\" CLASS=\"BUTTON\" VALUE=\".*?Download.*?\".*?Click)", Pattern.CASE_INSENSITIVE);

    public UCMS(PluginWrapper wrapper) {
        super(wrapper);
    }
    public static void main(String s[]) throws Exception {

        String[] list = { "ddl-kingz.in", "oxygen-warez.com", "filefox.in", "alphawarez.us", "pirate-loads.com", "fettrap.com", "omega-music.com", "hardcoremetal.biz", "flashload.org", "twin-warez.com", "oneload.org", "steelwarez.com", "fullstreams.info", "lionwarez.com", "1dl.in", "chrome-database.com", "oneload.org", "youwarez.biz", "saugking.net", "leetpornz.com", "freefiles4u.com", "dark-load.net", "crimeland.de", "get-warez.in", "meinsound.com", "projekt-tempel-news.de.vu", "datensau.org", "musik.am", "spreaded.net", "relfreaks.com", "babevidz.com", "serien24.com", "porn-freaks.net", "xxx-4-free.net", "porn-traffic.net", "chili-warez.net", "game-freaks.net", "isos.at", "your-load.com", "mov-world.net", "xtreme-warez.net", "sceneload.to", "epicspeedload.in", "serienfreaks.to", "serienfreaks.in", "warez-load.com", "ddl-scene.com", "mp3king.cinipac-hosting.biz", "sauggirls.com",
                "pornfox.in", "xflat24.com", "alben.ws", "worldofxxx.org", "gamegalaxy.ws", "ddl.byte.to", "interload.biz", "xwebb.extra.hu/1dl", "jokermovie.org", "xtreme-warez.biz", "your-load.com", "top-hitz.com", "wii-reloaded.ath.cx/sites/epic", "wankingking.com", "projekt-tempel-news.org", "porn-ox.in", "music-dome.cc", "sound-load.com", "hoerspiele.to", "jim2008.extra.hu", "ex-yu.extra.hu", "firefiles.in", "gez-load.net", "wrzunlimited.1gb.in", "streamload.in", "toxic.to", "mp3z.to", "sexload.to", "sound-load.com", "sfulc.exofire.net/cms", "fickdiehure.com", "dream-team.bz/cms", "omega-warez.com", "ddl-scene.cc", "xxxstreams.org", "scene-warez.com", "dokuh.tv", "titanload.to", "ddlshock.com", "xtreme-warez.us", "crunkwarez.com", "serienking.in", "stream.szenepic.us", "gate-warez.com", "gateload.info", "hot-porn-ddl.com" };
        for (String pattern : list) {

            System.out.print(", ");

            System.out.println("\"http://[\\\\w\\\\.]*?" + pattern.replaceAll("\\\\.", "\\\\\\\\.") + "/(\\\\?id=.+|[\\\\?]*?/.*?\\\\.html|category/.*?/.*?\\\\.html|download/.*?/.*?\\\\.html|.*?/.*?\\\\.html)\"");

        }
        for (String pattern : list) {

            System.out.print(", ");

            System.out.println("\"" + pattern + "\"");

        }
        for (String pattern : list) {

            System.out.println(", 0");


        }
    }
    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        try {
            br.getPage(parameter);
            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
            File captchaFile = null;
            String capTxt = "";
            String host = br.getHost();

            if (!host.startsWith("http")) {
                host = "http://" + host;
            }

            String pass = br.getRegex(Pattern.compile("CopyToClipboard\\(this\\)\\; return\\(false\\)\\;\">(.*?)<\\/a>", Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (pass == null) pass = br.getRegex("<B>Passwort:</B> <input value=\"(.*?)\".*?<").getMatch(0);
            if (pass == null) pass = br.getRegex("<dt class=\"\">Passwort:</dt>.*?<dd class=\"\">(.*?)</dd>").getMatch(0);
            if (pass != null) {
                if (pass.equals("keins ben&ouml;tigt") || pass.equals("kein pw") || pass.equals("N/A") || pass.equals("n/a") || pass.equals("-") || pass.equals("-kein Passwort-") || pass.equals("-No Pass-") || pass.equals("ohne PW")) {
                    pass = null;
                }
            }

            String forms[][] = br.getRegex(Pattern.compile("<FORM ACTION=\"([^\"]*)\" ENCTYPE=\"multipart/form-data\" METHOD=\"POST\" NAME=\"(mirror|download)[^\"]*\"(.*?)</FORM>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatches();
            if (forms.length != 0) {
                for (String[] element : forms) {
                    for (int retry = 0; retry < 5; retry++) {
                        Matcher matcher = PAT_CAPTCHA.matcher(element[2]);
                        if (matcher.find()) {
                            logger.finest("Captcha Protected");
                            String captchaAdress = host + new Regex(element[2], Pattern.compile("<IMG SRC=\"(/.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                            capTxt = getCaptchaCode(captchaAdress, param);

                            String posthelp = HTMLParser.getFormInputHidden(element[2]);
                            if (element[0].startsWith("http")) {
                                br.postPage(element[0], posthelp + "&code=" + capTxt);
                            } else {
                                br.postPage(host + element[0], posthelp + "&code=" + capTxt);
                            }
                        } else {
                            Matcher matcher_no = PAT_NO_CAPTCHA.matcher(element[2]);
                            if (matcher_no.find()) {
                                logger.finest("Not Captcha protected");
                                String posthelp = HTMLParser.getFormInputHidden(element[2]);
                                if (element[0].startsWith("http")) {
                                    br.postPage(element[0], posthelp);
                                } else {
                                    br.postPage(host + element[0], posthelp);
                                }
                                break;
                            }
                        }
                        if (br.containsHTML("Der Sichheitscode wurde falsch eingeben")) {
                            logger.warning("Captcha Detection failed");
                            br.getPage(parameter);
                        } else {
                            break;
                        }
                        if (br.getHttpConnection().getURL().toString().equals(host + element[0])) {
                            break;
                        }
                    }
                    /*
                     * Bei hardcoremetal.biz wird mittlerweile der Download als
                     * DLC-Container angeboten! Workaround für diese Seite
                     */
                    if (br.containsHTML("ACTION=\"/download\\.php\"")) {
                        Form forms2[] = br.getForms();
                        for (Form form : forms2) {
                            if (form.containsHTML("dlc")) {
                                File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
                                Browser.download(container, br.openFormConnection(form));
                                decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
                                break;
                            }
                        }
                    } else {
                        String links[] = null;
                        if (br.containsHTML("unescape\\(unescape\\(unescape")) {
                            String temp = br.getRegex(Pattern.compile("unescape\\(unescape\\(unescape\\(\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                            String temp2 = Encoding.htmlDecode(Encoding.htmlDecode(Encoding.htmlDecode(temp)));
                            links = new Regex(temp2, Pattern.compile("ACTION=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
                        } else if (br.containsHTML("unescape\\(unescape")) {
                            String temp = br.getRegex(Pattern.compile("unescape\\(unescape\\(\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                            String temp2 = Encoding.htmlDecode(Encoding.htmlDecode(temp));
                            links = new Regex(temp2, Pattern.compile("ACTION=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
                        } else {
                            links = br.getRegex(Pattern.compile("ACTION=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
                        }
                        for (String element2 : links) {
                            DownloadLink link = createDownloadlink(Encoding.htmlDecode(element2));
                            link.addSourcePluginPassword(pass);
                            decryptedLinks.add(link);
                        }
                    }
                }
            } else {
                /* workaround for java ucms */
                String[] forms2 = br.getRegex("document.writeln\\('(<form.*?'</form>)").getColumn(0);
                ArrayList<Form> forms3 = new ArrayList<Form>();
                for (String form : forms2) {
                    String temp = form.replaceAll("(document\\.writeln\\('|'\\);)", "");
                    Form tform = new Form(temp);
                    tform.setAction(param.getCryptedUrl());
                    tform.remove(null);
                    tform.remove(null);
                    forms3.add(tform);
                }
                boolean cont = false;
                Browser brc = null;
                for (Form tform : forms3) {
                    for (int retry = 0; retry < 5; retry++) {
                        brc = br.cloneBrowser();
                        cont = false;
                        if (tform.containsHTML("<img src=")) {
                            logger.finest("Captcha Protected");
                            String captchaAdress = host + tform.getRegex(Pattern.compile("<img src=\"(/captcha/.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                            captchaFile = getLocalCaptchaFile();
                            Browser.download(captchaFile, captchaAdress);
                            capTxt = getCaptchaCode("hardcoremetal.biz", captchaFile, UserIO.NO_JAC, param, null, null);
                            tform.put("code", capTxt);
                            brc.submitForm(tform);
                        } else {
                            logger.finest("Not Captcha protected");
                            brc.submitForm(tform);
                        }
                        if (brc.containsHTML("UCMS.RELEASE")) {
                            cont = true;
                            break;
                        }
                    }
                    if (cont) {
                        String[] links2 = brc.getRegex("href=\\\\\"(.*?)\\\\\"").getColumn(0);
                        for (String dl : links2) {
                            dl = dl.replaceAll("\\\\/", "/");
                            Browser br2 = br.cloneBrowser();
                            br2.getPage(dl);
                            String flink = br2.getRegex("<iframe src=\"(.*?)\"").getMatch(0);
                            if (flink == null && br2.getRedirectLocation() != null) flink = br2.getRedirectLocation();
                            DownloadLink link = createDownloadlink(flink);
                            link.addSourcePluginPassword(pass);
                            decryptedLinks.add(link);
                        }
                    }
                }

            }
        } catch (PluginException e2) {
            throw e2;
        } catch (IOException e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
            return null;
        }
        return decryptedLinks;
    }

    // @Override

}
