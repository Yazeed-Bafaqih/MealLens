package com.example.meallens.data;

import java.io.Serializable;

/** A health note shown on the summary screen. */
public class Flag implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Level { DANGER, WARN, OK, INFO }

    public final Level level;
    public final String title;
    public final String detail;

    public Flag(Level level, String title, String detail) {
        this.level = level;
        this.title = title;
        this.detail = detail;
    }
}
