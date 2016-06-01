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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "unionmangas.com" }, urls = { "https?://(?:www\\.)?unionmangas\\.(com|net)/leitor/[^/]+/\\d+(\\.|,\\d+)?" }, flags = { 0 })
public class UnionmangasCom extends PluginForDecrypt {

    public UnionmangasCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !this.br.getURL().contains("/leitor/")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final Regex urlinfo = new Regex(parameter, "unionmangas\\.(?:com|net)/leitor/([^/]+)/(\\d+(\\.|,\\d+)?)");
        final String chapter_str = urlinfo.getMatch(1);
        final String url_name = urlinfo.getMatch(0);
        final String url_fpname = url_name + "_chapter_" + chapter_str;

        final String[] links = this.br.getRegex("data\\-lazy=\"(http[^<>\"]+)\"").getColumn(0);

        if (links == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        for (final String url : links) {
            final DownloadLink dl = this.createDownloadlink("directhttp://" + Encoding.urlEncode_light(url), false);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(url_fpname);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
