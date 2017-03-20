package com.fr.plugin.fractional.fun;

/**
 * Created by richie on 2017/3/20.
 */
public enum Position {

    BOTTOM(0), TOP(1);

    private int p;

    Position(int p) {
        this.p = p;
    }

    public int toInt() {
        return p;
    }

    public static Position parse(int p) {
        for (Position position : values()) {
            if (position.p == p) {
                return position;
            }
        }
        return BOTTOM;
    }
}
