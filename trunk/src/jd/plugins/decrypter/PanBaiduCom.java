//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pan.baidu.com" }, urls = { "http://(www\\.)?pan\\.baidu\\.com/(share/link(\\?(shareid|uk)=\\d+\\&(shareid|uk)=\\d+(#dir/path=%2F.+)?)|netdisk/(singlepublic\\?fid=|extractpublic\\?username=)[\\w\\%]+|s/\\w+)" }, flags = { 0 })
public class PanBaiduCom extends PluginForDecrypt {

    public PanBaiduCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static boolean      pluginloaded            = false;
    private static final String TYPE_DIRECT_FOLDER_LINK = "http://(www\\.)?pan\\.baidu\\.com/share/link\\?shareid=\\d+\\&uk=\\d+#dir/path=%2F.+";
    private static final String TYPE_FOLDER_LINK_NORMAL = "http://(www\\.)?pan\\.baidu\\.com/share/link\\?shareid=\\d+\\&uk=\\d+";
    private static final String TYPE_FOLDER_LINK_SHORT  = "http://(www\\.)?pan\\.baidu\\.com/s/[A-Za-z0-9]+";

    // TODO: TYPE_FOLDER_LINK_SHORT and normal links can also be single files
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getURL().contains("pan.baidu.com/share/error?") || br.containsHTML("啊哦，你来晚了，分享的文件已经被删除了，下次要早点哟|啊哦，你来晚了，分享的文件已经被取消了，下次要早点哟。")) {
            logger.info("Link offline: " + parameter);
            DownloadLink dl = createDownloadlink("directhttp://" + parameter);
            dl.setProperty("OFFLINE", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        JDUtilities.getPluginForHost("pan.baidu.com");
        if (br.getURL().matches(jd.plugins.hoster.PanBaiduCom.PWPROTECTEDLINK)) {
            decryptedLinks.add(createDownloadlink(br.getURL()));
            return decryptedLinks;
        }
        if (parameter.contains("netdisk")) parameter = br.getURL();// old linkformat

        String singleFolder = new Regex(parameter, "#dir/path=(.*?)$").getMatch(0);
        String correctedBR = br.toString().replaceAll("\\\\\\\\", "\\\\").replaceAll("\\\\\"", "\"");// save unicode backslash
        String dir = null;
        // Jump into folder or get content of the main link
        if (parameter.matches(TYPE_DIRECT_FOLDER_LINK)) {
            final String dirName = new Regex(parameter, "dir/path=%2F(.+)").getMatch(0);
            dir = "%2F" + dirName;
            getDownloadLinks(decryptedLinks, parameter, dirName, dir);
        } else if (parameter.matches(TYPE_FOLDER_LINK_SHORT)) {
            final String uk = br.getRegex("FileUtils\\.share_uk=\"(\\d+)\"").getMatch(0);
            final String shareid = br.getRegex("FileUtils\\.share_id=\"(\\d+)\"").getMatch(0);
            if (uk == null || shareid == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            parameter = "http://pan.baidu.com/share/link?shareid=" + shareid + "&uk=" + uk;
        }
        /* create HashMap with json key/value pair */
        String json = new Regex(correctedBR, "\"\\[(\\{.*?\\})\\]\"").getMatch(0);
        if (json == null) json = new Regex(correctedBR, "\"(\\{.*?\\})\"").getMatch(0);
        // cleanup, poor mans method to remove entries that breaks the important 'dlink'
        if (json != null) {
            String cleaned = json;
            String thumbs = new Regex(cleaned, "(,\"thumbs\":\\{[^\\}]+\\})").getMatch(0);
            if (thumbs != null) cleaned = cleaned.replace(thumbs, "");
            String resolution = new Regex(cleaned, "(,\"resolution\":\"[^\"]+\")").getMatch(0);
            if (resolution != null) cleaned = cleaned.replace(resolution, "");
            json = cleaned;
        } else {
            logger.warning("Problemo! Please report to JDownloader Development Team");
            return null;
        }

        HashMap<String, String> ret = new HashMap<String, String>();
        for (String[] values : new Regex((json == null ? "" : json), "\\{([^\\}]+)").getMatches()) {

            for (String[] value : new Regex(values[0] + ",", "\"(.*?)\":\"?(.*?)\"?,").getMatches()) {
                ret.put(value[0], value[1]);
            }

            if (!(ret.containsKey("headurl") || ret.containsKey("parent_path"))) continue;
            dir = new Regex(ret.get("headurl"), "filename=(.*?)$").getMatch(0);
            if (dir == null) dir = ret.get("server_filename");
            ret.put("path", dir);
            dir = ret.get("parent_path") + "%2F" + dir;
            if (singleFolder != null && !singleFolder.equals(dir)) continue;// only selected folder
            if (ret.containsKey("md5") && !"".equals(ret.get("md5"))) {// file in root
                final DownloadLink dl = generateDownloadLink(ret, parameter, dir);
                decryptedLinks.add(dl);
            } else {
                getDownloadLinks(decryptedLinks, parameter, ret.get("path"), dir);// folder in root
            }
        }

        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private void getDownloadLinks(final ArrayList<DownloadLink> decryptedLinks, final String parameter, String dirName, String dir) throws Exception {
        if (dirName == null || dir == null) return;
        FilePackage fp = null;
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage(getFolder(parameter, dir));
        fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(unescape(dirName)));
        HashMap<String, String> ret = new HashMap<String, String>();
        String list = br.getRegex("\"list\":\\[(\\{.*?\\})\\]").getMatch(0);

        for (String[] links : new Regex((list == null ? "" : list), "\\{(.*?)\\}").getMatches()) {

            for (String[] link : new Regex(links[0] + ",", "\"(.*?)\":\"?(.*?)\"?,").getMatches()) {
                ret.put(link[0], link[1]);
            }

            // TODO: Add subfolder support in a better way
            // /* subfolder in folder */
            // if (!ret.containsKey("dlink")) {
            // if ("1".equals(ret.get("isdir"))) {
            // String folderName = ret.get("server_filename");
            // String path = ret.get("path");
            // if (folderName == null || path == null) continue;
            // getDownloadLinks(decryptedLinks, parameter, dirName + "-" + folderName, unescape(path));
            // }
            // }
            final String shareid = new Regex(parameter, "shareid=(\\d+)").getMatch(0);
            final String uk = new Regex(parameter, "uk=(\\d+)").getMatch(0);
            final String fsid = ret.get("fs_id");
            if (shareid != null && uk != null && fsid != null) {
                final DownloadLink dl = generateDownloadLink(ret, parameter, dir);
                dl.setProperty("origurl_shareid", shareid);
                dl.setProperty("origurl_uk", uk);
                dl.setProperty("important_fsid", fsid);
                fp.add(dl);
                try {
                    distribute(dl);
                } catch (final Throwable e) {
                    /* does not exist in 09581 */
                }
                decryptedLinks.add(dl);
            }
        }
    }

    private String getFolder(final String parameter, String dir) {
        String shareid = new Regex(parameter, "shareid=(\\d+)").getMatch(0);
        if (shareid == null) shareid = new Regex(parameter, "/s/(.*)").getMatch(0);
        String uk = new Regex(parameter, "uk=(\\d+)").getMatch(0);
        if (uk == null) uk = br.getRegex("uk=(\\d+)").getMatch(0);
        return "http://pan.baidu.com/share/list?channel=chunlei&clienttype=0&web=1&num=100&t=" + System.currentTimeMillis() + "&page=1&dir=" + dir + "&t=0." + +System.currentTimeMillis() + "&uk=" + (uk != null ? uk : "") + "&shareid=" + (shareid != null ? shareid : "") + "&_=" + System.currentTimeMillis();
    }

    private static synchronized String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (pluginloaded == false) {
            final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
            if (plugin == null) throw new IllegalStateException("youtube plugin not found!");
            pluginloaded = true;
        }
        return jd.plugins.hoster.Youtube.unescape(s);
    }

    private DownloadLink generateDownloadLink(HashMap<String, String> ret, String parameter, String dir) {
        DownloadLink dl = createDownloadlink("http://pan.baidudecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
        for (Entry<String, String> next : ret.entrySet()) {
            dl.setProperty(next.getKey(), next.getValue());
        }
        if (dl.getStringProperty("server_filename") != null) dl.setFinalFileName(Encoding.htmlDecode(unescape(dl.getStringProperty("server_filename"))));
        if (dl.getStringProperty("size") != null) dl.setDownloadSize(SizeFormatter.getSize(dl.getStringProperty("size") + "b"));
        dl.setProperty("mainLink", parameter);
        dl.setProperty("dirName", dir);
        dl.setAvailable(true);
        return dl;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}