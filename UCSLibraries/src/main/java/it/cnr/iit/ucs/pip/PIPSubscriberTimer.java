package it.cnr.iit.ucs.pip;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Subscriber timer for the PIPs.
 * It's in charge of performing the task of refreshing periodically the value
 * of a certain attribute.
 *
 * @author Antonio La Marra, Alessandro Rosetti
 */
public final class PIPSubscriberTimer extends TimerTask {
    private final Timer timer;
    PIPBase pip;

    private static final long DEFAULT_RATE = 1L * 1000;
    private long rate = DEFAULT_RATE;

    public PIPSubscriberTimer(PIPBase pip) {
        this.timer = new Timer();
        this.pip = pip;
    }

    @Override
    public void run() {
        pip.checkSubscriptions();
    }

    public void start() {
        timer.scheduleAtFixedRate(this, 0, rate);
    }

    public long getRate() {
        return rate;
    }

    public void setRate(long rate) {
        if (rate <= 0) {
            this.rate = DEFAULT_RATE;
        } else {
            this.rate = rate;
        }
    }

}
