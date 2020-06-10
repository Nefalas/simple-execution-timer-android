package com.nefalas.executiontimer;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

public class SimpleExecutionTimer {

    private Logger logger;

    private final String name;
    private boolean isDisabled;

    private Precision precision = Precision.MILLIS;
    private Map<String, Long> steps;

    private boolean didCompute;

    private long start;
    private long lastRecordedTime;
    private long total;

    public SimpleExecutionTimer(String name) {
        this.name = name;
        this.logger = new Logger();
    }

    public SimpleExecutionTimer disableIf(boolean disableIf) {
        this.isDisabled = disableIf;
        
        return this;
    }

    public SimpleExecutionTimer disableIfNot(boolean disableIfNot) {
        this.isDisabled = !disableIfNot;

        return this;
    }

    public SimpleExecutionTimer withMillisPrecision() {
        this.precision = Precision.MILLIS;

        return this;
    }

    public SimpleExecutionTimer withNanoPrecision() {
        this.precision = Precision.NANO;

        return this;
    }

    public SimpleExecutionTimer start() {
        if (this.isDisabled) {
            return this;
        }

        this.steps = new HashMap<>();
        this.start = this.getTime();
        this.lastRecordedTime = 0;

        return this;
    }

    public void addStep(String name) {
        if (this.isDisabled) {
            return;
        }

        this.lastRecordedTime = this.getTime();
        this.steps.put(name, this.lastRecordedTime);
    }

    public void log() {
        if (this.isDisabled) {
            return;
        }

        this.computeResults();
        this.logResults();
    }

    private void computeResults() {
        this.total = this.lastRecordedTime - this.start;

        this.didCompute = true;
    }

    private void logResults() {
        _log("--- Execution time for {name} ---");
        _log("TOTAL TIME: {total} {precision}");
        _log();

        long previousTime = this.start;
        for (Map.Entry<String, Long> entry : this.steps.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }

            String name = entry.getKey();
            long time = entry.getValue();
            long elapsed = time - previousTime;
            previousTime = entry.getValue();

            _log("STEP [%s] took %d {precision} - %d", name, elapsed, time);
        }
    }

    private long getTime() {
        switch (this.precision) {
            case NANO:
                return System.nanoTime();
            case MILLIS:
            default:
                return System.currentTimeMillis();
        }
    }

    private void _log() {
        if (!this.isDisabled) this.logger.emptyLine();
    }
    
    private void _log(String text, Object... args) {
        if (!this.isDisabled) this.logger.log(text, args);
    }

    private enum Precision {
        MILLIS("ms"),
        NANO("ns");

        private String name;

        Precision(String name) {
            this.name = name;
        }

        @NonNull
        @Override
        public String toString() {
            return this.name;
        }
    }

    private class Logger {

        private static final String TAG = "SimpleExecutionTimer";

        private Map<String, Callable<String>> variables;

        Logger() {
            this.variables = new HashMap<>();
            variables.put("{name}", () -> name);
            variables.put("{total}", () -> String.valueOf(total));
            variables.put("{precision}", precision::toString);
        }

        void emptyLine() {
            this.log("");
        }
        
        void log(String text, Object... args) {
            Log.i(TAG, format(text, args));
        }

        private String format(String text, Object... args) {
            text = replaceVariables(text);
            if (args.length > 0) text = String.format(Locale.ENGLISH, text, args);
            
            return text;
        }

        private String replaceVariables(String text) {
            for (Map.Entry<String, Callable<String>> entry : this.variables.entrySet()) {
                try {
                    String id = entry.getKey();
                    String value = entry.getValue().call();
                    text = text.replaceAll(id, value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return text;
        }
    }
}
