package com.nefalas.executiontimer;

import org.junit.Test;

public class SimpleExecutionTimerTest {

    @Test
    public void testTimer() {
        SimpleExecutionTimer millisTimer = new SimpleExecutionTimer("Millis Timer")
                .withMillisPrecision();
        SimpleExecutionTimer nanoTimer = new SimpleExecutionTimer("Nano Timer")
                .withNanoPrecision();

        this.runTimer(millisTimer.start());
        this.runTimer(nanoTimer.start());
    }

    private void runTimer(SimpleExecutionTimer timer) {
        try {
            Thread.sleep(15);
            timer.addStep("15ms");

            Thread.sleep(103);
            timer.addStep("103ms");

            Thread.sleep(200);
            timer.addStep("200ms");

            Thread.sleep(1);
            timer.addStep("1ms");

            Thread.sleep(2);
            timer.addStep("2ms");

            timer.log();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}