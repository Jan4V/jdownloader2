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

package jd.plugins.optional.JDAntiStandby;

import java.util.logging.Logger;

import jd.controlling.DownloadWatchDog;
import jd.controlling.JDLogger;

import com.sun.jna.Native;

public class JDAntiStandbyThread implements Runnable {

    public boolean running = true;
    private boolean run = false;
    private int sleep = 5000;
    private Logger logger;
    private JDAntiStandby jdAntiStandby = null;

    private Kernel32 kernel32 = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);

    public JDAntiStandbyThread(JDAntiStandby jdAntiStandby) {
        super();
        this.logger = JDLogger.getLogger();
        this.jdAntiStandby = jdAntiStandby;
    }

    public void run() {
        while (running) {
            try {
                if (jdAntiStandby.isStatus()) {
                    switch (jdAntiStandby.getPluginConfig().getIntegerProperty("CONFIG_MODE")) {
                    case 0:
                        if (run) {
                            run = false;
                            logger.fine("JDAntiStandby: Stop");
                            kernel32.SetThreadExecutionState(Kernel32.ES_CONTINUOUS);
                        }
                        break;
                    case 1:
                        if (DownloadWatchDog.getInstance().getDownloadStatus() == DownloadWatchDog.STATE.RUNNING) {
                            if (!run) {
                                run = true;
                                logger.fine("JDAntiStandby: Start");
                            }
                            kernel32.SetThreadExecutionState(Kernel32.ES_CONTINUOUS | Kernel32.ES_SYSTEM_REQUIRED | Kernel32.ES_DISPLAY_REQUIRED);

                        }
                        if ((DownloadWatchDog.getInstance().getDownloadStatus() == DownloadWatchDog.STATE.NOT_RUNNING) || (DownloadWatchDog.getInstance().getDownloadStatus() == DownloadWatchDog.STATE.STOPPING)) {
                            if (run) {
                                run = false;
                                logger.fine("JDAntiStandby: Stop");
                                kernel32.SetThreadExecutionState(Kernel32.ES_CONTINUOUS);
                            }
                        }
                        break;
                    case 2:
                        if (!run) {
                            run = true;
                            logger.fine("JDAntiStandby: Start");
                        }
                        kernel32.SetThreadExecutionState(Kernel32.ES_CONTINUOUS | Kernel32.ES_SYSTEM_REQUIRED | Kernel32.ES_DISPLAY_REQUIRED);
                        break;
                    default:
                        logger.finest("JDAntiStandby: Config error");

                    }
                } else {
                    if (run) {
                        run = false;
                        logger.fine("JDAntiStandby: Stop");
                        kernel32.SetThreadExecutionState(Kernel32.ES_CONTINUOUS);
                    }
                }
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
            }
        }
        kernel32.SetThreadExecutionState(Kernel32.ES_CONTINUOUS);
        logger.fine("JDAntiStandby: Terminated");
    }

}
