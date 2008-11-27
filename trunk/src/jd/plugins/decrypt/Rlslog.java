//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.decrypt;

import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.DistributeData;
import jd.controlling.ProgressController;
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class Rlslog extends PluginForDecrypt {

    public Rlslog(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        Vector<String> passwords;
        String parameter = param.toString();
        br.getPage(parameter);
        String directComment = new Regex(param.toString(), "http://[\\w\\.]*?rlslog\\.net/.+/.+/#comments|/.+/#comments|/.+/.*?#(comment-\\d+)").getMatch(0);

        if (directComment != null) {
            String comment = br.getRegex(Pattern.compile("<li class=.*? id=.*?" + directComment + ".*?>(.*?)</li>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            passwords = HTMLParser.findPasswords(comment);
            String[] links = new Regex(comment, "rel=\"nofollow\">(.*?)</a>", Pattern.CASE_INSENSITIVE).getColumn(0);
            for (String link : links) {
                if (!new Regex(link, this.getSupportedLinks()).matches() && DistributeData.hasPluginFor(link)) {
                    DownloadLink dLink = createDownloadlink(link);
                    dLink.addSourcePluginPasswords(passwords);
                    decryptedLinks.add(dLink);
                }
            }
        } else {
            String comment_pages_tag = br.getRegex(Pattern.compile("<!-- Comment page numbers -->(.*?)<!-- End comment page numbers -->", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            String comment_pages[] = new Regex(comment_pages_tag, "<a class=\"next-comment-page\" href=\"(.*?)\"").getColumn(0);
            Vector<String> pages = new Vector<String>();
            pages.add(param.toString());
            for (String page : comment_pages) {
                pages.add(page);
            }
            for (String page : pages) {
                br.getPage(page);
                String comments[] = br.getRegex(Pattern.compile("<li class=.*? id=.*? value=.*?>(.*?)</li>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getColumn(0);
                for (String comment : comments) {
                    passwords = HTMLParser.findPasswords(comment);
                    String[] links = new Regex(comment, "rel=\"nofollow\">(.*?)</a>", Pattern.CASE_INSENSITIVE).getColumn(0);
                    for (String link : links) {
                        if (!new Regex(link, this.getSupportedLinks()).matches() && DistributeData.hasPluginFor(link)) {
                            DownloadLink dLink = createDownloadlink(link);
                            dLink.addSourcePluginPasswords(passwords);
                            decryptedLinks.add(dLink);
                        }
                    }
                }
            }
        }
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}