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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mirrorcreator.com" }, urls = { "http://[\\w\\.]*?mirrorcreator\\.com/files/[0-9A-Z]{8}/" }, flags = { 0 })
public class MrrrCrtrCm extends PluginForDecrypt {

    public MrrrCrtrCm(PluginWrapper wrapper) {
      super(wrapper);
    }
    
    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
      ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
      String parameter = param.toString();
      String id = new Regex(parameter, "files/([0-9A-Z]{8})").getMatch(0);
      parameter = "http://mirrorcreator.com/status.php?uid=" + id;
      br.getPage(parameter);
      /* Error handling */
      if (br.containsHTML("Unfortunately, the link you have clicked is not available")|| br.containsHTML("Invalid link. Please check the URL")) {
          logger.warning("The requested document was not found on this server.");
          logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
          return new ArrayList<DownloadLink>();
      }
      
      String[] redirectLinks = br.getRegex("href=\"(.*?)\"").getColumn(0);
      
      if (redirectLinks.length == 0) return null;
      for (String link : redirectLinks) {
          decryptedLinks.add(createDownloadlink(link));
      }
          
      return decryptedLinks;
    }
    
}