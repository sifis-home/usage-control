package ucs.pipreader;

import java.util.Timer;
import java.util.TimerTask;

public class PIPSensorReaderSubscriberTimer extends TimerTask {
    private Timer timer;
    PIPSensorReader pip;

    private static final long DEFAULT_RATE = 1L * 1000;
    private long rate = DEFAULT_RATE;

    PIPSensorReaderSubscriberTimer(PIPSensorReader pip ) {
        this.timer = new Timer();
        this.pip = pip;
    }

    @Override
    public void run() {
        pip.checkSubscriptions();
    }

    public void start() {
        timer.scheduleAtFixedRate( this, 0, rate );
    }

    public long getRate() {
        return rate;
    }

    public void setRate( long rate ) {
        if( rate <= 0 ) {
            this.rate = DEFAULT_RATE;
        }
        this.rate = rate;
    }
}
