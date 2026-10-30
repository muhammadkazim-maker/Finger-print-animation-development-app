package com.example;

import com.example.ITouchListener;

interface ITouchService {
    void destroy() = 16777114;
    void start(ITouchListener l) = 1;
    void setZone(float cx, float cy, float r) = 2;
    void stop() = 3;
    boolean grantSecureSettings() = 4;
    void setScreenInteractive(boolean interactive) = 5;
    String runDiagnosticCommand(String cmd) = 6;
}
