package org.jdownloader.captcha.v2.solver.dbc;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseValidation;
import org.jdownloader.captcha.v2.SolverStatus;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.dbc.api.Captcha;
import org.jdownloader.captcha.v2.solver.dbc.api.Client;
import org.jdownloader.captcha.v2.solver.dbc.api.SocketClient;
import org.jdownloader.captcha.v2.solver.dbc.api.User;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_DBC;

public class DeathByCaptchaSolver extends CESChallengeSolver<String> implements ChallengeResponseValidation {

    private DeathByCaptchaSettings            config;
    private static final DeathByCaptchaSolver INSTANCE   = new DeathByCaptchaSolver();
    private ThreadPoolExecutor                threadPool = new ThreadPoolExecutor(0, 1, 30000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory());
    private LogSource                         logger;
    private SocketClient                      client;

    public static DeathByCaptchaSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    @Override
    public DeathByCaptchaSolverService getService() {
        return (DeathByCaptchaSolverService) super.getService();
    }

    private DeathByCaptchaSolver() {
        super(new DeathByCaptchaSolverService(), Math.max(1, Math.min(25, JsonConfig.create(DeathByCaptchaSettings.class).getThreadpoolSize())));
        getService().setSolver(this);
        config = JsonConfig.create(DeathByCaptchaSettings.class);
        logger = LogController.getInstance().getLogger(DeathByCaptchaSolver.class.getName());

        threadPool.allowCoreThreadTimeOut(true);

    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        return c instanceof BasicCaptchaChallenge && super.canHandle(c);
    }

    protected void solveCES(CESSolverJob<String> job) throws InterruptedException, SolverException {

        solveBasicCaptchaChallenge(job, (BasicCaptchaChallenge) job.getChallenge());

    }

    private void solveBasicCaptchaChallenge(CESSolverJob<String> job, BasicCaptchaChallenge challenge) throws InterruptedException {

        if (config.getWhiteList() != null) {
            if (config.getWhiteList().length() > 5) {
                if (config.getWhiteList().contains(challenge.getTypeID())) {
                    job.getLogger().info("Hoster on WhiteList for deathbycaptcha.eu. - " + challenge.getTypeID());
                } else {
                    job.getLogger().info("Hoster not on WhiteList for deathbycaptcha.eu. - " + challenge.getTypeID());
                    return;
                }
            }
        }
        if (config.getBlackList() != null) {
            if (config.getBlackList().length() > 5) {
                if (config.getBlackList().contains(challenge.getTypeID())) {
                    job.getLogger().info("Hoster on BlackList for deathbycaptcha.eu. - " + challenge.getTypeID());
                    return;
                } else {
                    job.getLogger().info("Hoster not on BlackList for deathbycaptcha.eu. - " + challenge.getTypeID());
                }
            }
        }
        job.showBubble(this);
        checkInterruption();
        try {

            Client client = getClient();
            Captcha captcha = null;

            // Put your CAPTCHA image file, file object, input stream,
            // or vector of bytes here:
            job.setStatus(SolverStatus.UPLOADING);
            captcha = client.upload(challenge.getImageFile());
            if (null != captcha) {
                job.setStatus(new SolverStatus(_GUI._.DeathByCaptchaSolver_solveBasicCaptchaChallenge_solving(), NewTheme.I().getIcon(IconKey.ICON_WAIT, 20)));
                job.getLogger().info("CAPTCHA " + challenge.getImageFile() + " uploaded: " + captcha.id);
                long startTime = System.currentTimeMillis();
                // Poll for the uploaded CAPTCHA status.
                while (captcha.isUploaded() && !captcha.isSolved()) {

                    job.getLogger().info("deathbycaptcha.eu NO answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s ");

                    Thread.sleep(Client.POLLS_INTERVAL * 1000);
                    captcha = client.getCaptcha(captcha);
                }

                if (captcha.isSolved()) {
                    job.getLogger().info("CAPTCHA " + challenge.getImageFile() + " solved: " + captcha.text);
                    job.setAnswer(new DeathByCaptchaResponse(challenge, this, captcha));

                } else {
                    job.getLogger().info("Failed solving CAPTCHA");
                    throw new SolverException("Failed:" + captcha.toString());
                }
            }

        } catch (Exception e) {
            job.getLogger().log(e);
        }

    }

    private synchronized Client getClient() {
        if (client != null) {
            return client;
        }
        client = new SocketClient(config.getUserName(), config.getPassword());
        client.isVerbose = true;

        return client;

    }

    protected boolean validateLogins() {
        if (!CFG_DBC.ENABLED.isEnabled()) {
            return false;
        }
        if (StringUtils.isEmpty(CFG_DBC.USER_NAME.getValue())) {
            return false;
        }
        if (StringUtils.isEmpty(CFG_DBC.PASSWORD.getValue())) {
            return false;
        }

        return true;
    }

    @Override
    public void setUnused(AbstractResponse<?> response, SolverJob<?> job) {
    }

    @Override
    public void setInvalid(final AbstractResponse<?> response, SolverJob<?> job) {
        if (config.isFeedBackSendingEnabled() && response instanceof DeathByCaptchaResponse) {
            threadPool.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        Captcha captcha = ((DeathByCaptchaResponse) response).getCaptcha();
                        // Report incorrectly solved CAPTCHA if neccessary.
                        // Make sure you've checked if the CAPTCHA was in fact
                        // incorrectly solved, or else you might get banned as
                        // abuser.
                        Client client = getClient();
                        Challenge<?> challenge = response.getChallenge();
                        if (challenge instanceof BasicCaptchaChallenge) {
                            if (client.report(captcha)) {
                                logger.info("CAPTCHA " + challenge + " reported as incorrectly solved");
                            } else {
                                logger.info("Failed reporting incorrectly solved CAPTCHA. Disabled Feedback");
                                config.setFeedBackSendingEnabled(false);
                            }
                        }

                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
            });
        }
    }

    public DBCAccount loadAccount() {

        DBCAccount ret = new DBCAccount();
        try {
            Client c = getClient();
            User user = c.getUser();
            ret.setBalance(user.getBalance());
            ret.setBanned(user.isBanned());
            ret.setId(user.getId());
            ret.setRate(user.getRate());
        } catch (Exception e) {
            logger.log(e);
            ret.setError(e.getMessage());
        }
        return ret;

    }

    @Override
    public void setValid(AbstractResponse<?> response, SolverJob<?> job) {
    }

}
