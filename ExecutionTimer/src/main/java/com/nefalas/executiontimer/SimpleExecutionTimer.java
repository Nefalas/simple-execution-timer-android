package com.nefalas.executiontimer;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class SimpleExecutionTimer {

    private Logger logger;

    private final String name;
    private boolean isDisabled = true;
    private boolean didRequestDisable;

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
        this.didRequestDisable = disableIf;
        
        return this;
    }

    public SimpleExecutionTimer disableIfNot(boolean disableIfNot) {
        this.didRequestDisable = !disableIfNot;

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
        if (this.didRequestDisable) {
            return this;
        }

        this.isDisabled = false;
        this.didCompute = false;
        this.steps = new LinkedHashMap<>();
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

        if (!this.didCompute) this.computeResults();
        this.logResults();
    }

    private void computeResults() {
        this.total = this.lastRecordedTime - this.start;

        this.didCompute = true;
    }

    private void logResults() {
        _log();
        _log("--- Execution time for {name} ---");
        _log("TOTAL TIME: {total} {precision}");
        _log();
        _log("STEP|TIME|TIME SINCE START");
        _log();

        long previousTime = this.start;
        for (Map.Entry<String, Long> entry : this.steps.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }

            String name = entry.getKey();
            long time = entry.getValue();
            long elapsed = time - previousTime;
            long elapsedTotal = time - this.start;
            previousTime = entry.getValue();

            _log("%s|%d{precision}|%d", name, elapsed, elapsedTotal);
        }

        _log();
        _print();
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

    private void _print() {
        if (!this.isDisabled) this.logger.print();
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

    public interface LogFunction {

        void log(String text);
    }

    @SuppressWarnings("unused")
    private class Logger {

        private static final String TAG = "SimpleExecutionTimer";

        private LogFunction logFunc = text -> Log.i(TAG, text);
        private Map<String, Callable<String>> variables;

        private List<String> logs;

        Logger() {
            this.logs = new ArrayList<>();
            this.variables = new HashMap<>();
            variables.put("name", () -> name);
            variables.put("total", () -> String.valueOf(total));
            variables.put("precision", () -> precision.toString());
        }

        void setLogFunction(LogFunction logFunction) {
            this.logFunc = logFunction;
        }

        void emptyLine() {
            this.logs.add("");
        }
        
        void log(String text, Object... args) {
            this.logs.add(format(text, args));
        }

        void print() {
            this.buildTables().forEach(text -> this.logFunc.log(text));
            this.logs.clear();
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
                    text = text.replaceAll("\\{" + id + "}", value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return text;
        }

        private List<String> buildTables() {
            ArrayList<String[]> columns = logs.stream()
                    .map(log -> log.split("\\|"))
                    .collect(Collectors.toCollection(ArrayList::new));

            int[][] lengths = this.computeMaxLengths(columns);
            List<String> text = new ArrayList<>();
            for (String[] strings : columns) {
                if (strings.length < 2) {
                    text.add(strings[0]);
                    continue;
                }

                int[] stringLengths = lengths[strings.length];
                StringBuilder builder = new StringBuilder();
                builder.append("  ");
                for (int i = 0; i < strings.length; i++) {
                    if (i == 0) builder.append(strings[i]);

                    int spaces = stringLengths[i] - strings[i].length();
                    for (int j = 0; j < spaces; j++) {
                        builder.append(" ");
                    }

                    if (i > 0) builder.append(strings[i]);
                    builder.append("  ");
                }

                text.add(builder.toString());
            }

            return text;
        }

        private int[][] computeMaxLengths(ArrayList<String[]> columns) {
            int[][] lengthArray = new int[11][];
            for (String[] cols : columns) {
                int nbOfCols = cols.length;
                if (nbOfCols > 10) {
                    continue;
                }

                int[] lengths = Arrays.stream(cols).mapToInt(String::length).toArray();
                if (lengthArray[nbOfCols] == null) {
                    lengthArray[nbOfCols] = lengths;
                    continue;
                }

                int[] currentLengths = lengthArray[nbOfCols];
                for (int i = 0; i < nbOfCols; i++) {
                    int currentLength = currentLengths[i];
                    int newLength = cols[i].length();

                    if (newLength > currentLength) {
                        currentLengths[i] = newLength;
                    }
                }
            }

            return lengthArray;
        }
    }
}
