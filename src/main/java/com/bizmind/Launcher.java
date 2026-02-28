package com.bizmind;

/**
 * Launcher class — avoids "JavaFX runtime components are missing" error.
 * This is a plain class (not extending Application) that delegates to BizMindApp.
 */
public class Launcher {
    public static void main(String[] args) {
        BizMindApp.main(args);
    }
}

