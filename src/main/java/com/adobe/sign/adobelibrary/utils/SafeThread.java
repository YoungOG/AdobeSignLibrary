package com.adobe.sign.adobelibrary.utils;

import java.util.Objects;

import javafx.application.Platform;

public final class SafeThread {

    private SafeThread() {
        throw new UnsupportedOperationException();
    }

    public static synchronized void runOnFXApplicationThread(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");

        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}