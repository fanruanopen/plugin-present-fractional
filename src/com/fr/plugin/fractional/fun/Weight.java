package com.fr.plugin.fractional.fun;

import com.fr.stable.Constants;

/**
 * Created by richie on 2017/3/20.
 */
public enum Weight {

    NONE(0), THIN(1),MEDIUM(2),THICK(5);

    private int line;

    Weight(int line) {
        this.line = line;
    }

    public int toInt() {
        return line;
    }

    public int toStyleLine() {
        if (line == 1) {
            return Constants.LINE_THIN;
        } else if (line == 2) {
            return Constants.LINE_MEDIUM;
        } else if (line == 5){
            return Constants.LINE_THICK;
        } else {
            return Constants.LINE_NONE;
        }
    }

    public static Weight parse(int line) {
        for (Weight weight: values()) {
            if (weight.line == line) {
                return weight;
            }
        }
        return THIN;
    }
}
