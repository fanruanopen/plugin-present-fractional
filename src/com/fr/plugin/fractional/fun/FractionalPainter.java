package com.fr.plugin.fractional.fun;

import com.fr.base.AbstractPainter;
import com.fr.base.Style;
import com.fr.general.FRFont;

import java.awt.*;

/**
 * Created by richie on 2017/3/20.
 */
public class FractionalPainter extends AbstractPainter {
    private String text;
    private FractionalAttr attr;

    public FractionalPainter(FractionalAttr attr, String text) {
        this.text = text;
        this.attr = attr;
    }

    @Override
    public void paint(Graphics g, int width, int height, int resolution, Style style) {
        Graphics2D g2d = (Graphics2D) g;
        FRFont font = FRFont.getInstance();

        font = font.applyUnderline(attr.getWeight().toStyleLine());
        g2d.setFont(font);
        style = style.deriveFRFont(font);
        FractionalBaseUtils.drawStringStyleInRotation(g2d, attr.getPosition(), width, height, text, style, 96);
    }
}
