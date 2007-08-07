﻿package jd.plugins;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.JProgressBar;


/**
 * Hier werden alle notwendigen Informationen zu einem einzelnen Download festgehalten.
 * Die Informationen werden dann in einer Tabelle dargestellt
 * 
 * @author astaldo
 */
public class DownloadLink{
    /**
     * Beschreibung des Downloads
     */
    private String name;
    /**
     * Von hier soll de Download stattfinden
     */
    private URL urlDownload;
    /**
     * Hoster des Downloads
     */
    private String host;
    /**
     * Zeigt an, ob dieser Downloadlink aktiviert ist
     */
    private boolean isEnabled;
    /**
     * Zeigt, ob dieser DownloadLink grad heruntergeladen wird
     */
    private boolean inProgress;
    /**
     * Das Plugin, das für diesen Download zuständig ist
     */
    private Plugin plugin;
    /**
     * Die Fortschrittsanzeige
     */
    private JProgressBar progressBar = new JProgressBar();
    /**
     * Hierhin soll die Datei gespeichert werden.
     */
    private File fileOutput;
    /**
     * Logger für Meldungen
     */
    private Logger logger = Plugin.getLogger();
    /**
     * Erzeugt einen neuen DownloadLink
     * 
     * @param plugin Das Plugins, das für diesen Download zuständig ist
     * @param name Bezeichnung des Downloads
     * @param host Anbieter, von dem dieser Download gestartet wird
     * @param urlDownload Die Download URL
     * @param isEnabled Markiert diesen DownloadLink als aktiviert oder deaktiviert
     */
    public DownloadLink(PluginForHost plugin, String name, String host, String urlDownload, boolean isEnabled){
        this.plugin      = plugin;
        this.name        = name;
        this.host        = host;
        this.isEnabled   = isEnabled;
        try {
            this.urlDownload = new URL(urlDownload);
        }
        catch (MalformedURLException e) {logger.severe("url malformed. "+e.toString());}
    }
    /**
     * Liefert den Namen dieses Downloads zurück
     * @return Name des Downloads
     */
    public String getName()                    { return name; }
    /**
     * Gibt den Hoster dieses Links azurück.
     * 
     * @return Der Hoster, auf dem dieser Link verweist
     */
    public String getHost()                    { return host; }
    /**
     * Zeigt, ob dieser Download aktiviert ist
     * 
     * @return wahr, falls dieser DownloadLink aktiviert ist
     */
    public boolean isEnabled()                 { return isEnabled;               }
    /**
     * Verändert den Aktiviert-Status
     * 
     * @param isEnabled Soll dieser DownloadLink aktiviert sein oder nicht
     */
    public void setEnabled(boolean isEnabled)  { this.isEnabled = isEnabled;     }
    public Plugin getPlugin()                  { return plugin;                  }
    public JProgressBar getProgressBar()       { return progressBar;             }
    public File getFileOutput()                { return fileOutput;              }
    public void setFileOutput(File fileOutput) { this.fileOutput = fileOutput;   }
    public URL getUrlDownload()                { return urlDownload;             }
    public void setUrlDownload(URL urlDownload){ this.urlDownload = urlDownload; }
    public boolean isInProgress()                 {        return inProgress;    }
    public void setInProgress(boolean inProgress) {        this.inProgress = inProgress;    }
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof DownloadLink)
            return this.urlDownload.equals(((DownloadLink)obj).urlDownload);
        else
            return super.equals(obj);
    }
}
