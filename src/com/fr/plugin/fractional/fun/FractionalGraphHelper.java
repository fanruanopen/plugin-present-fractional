package com.fr.plugin.fractional.fun;

/*
 * Copyright(c) 2001-2010, FineReport Inc, All Rights Reserved.
 */

import com.fr.base.DoubleDimension2D;
import com.fr.base.FRContext;
import com.fr.general.DefaultValues;
import com.fr.general.FRFont;
import com.fr.general.Inter;
import com.fr.plugin.ExtraClassManager;
import com.fr.stable.Constants;
import com.fr.stable.StringUtils;
import com.fr.stable.fun.FontProcessor;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.TextLayout;
import java.awt.geom.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Graphics draw, fill.
 */
public class FractionalGraphHelper {

    private FractionalGraphHelper() {
    }


    //在画线的时候，不用每次都new新的对象.
    private static Line2D tmpLine2D = new Line2D.Double(0, 0, 0, 0);


    private static final char A = 0xAC00;
    private static final char B = 0xD7A3;
    private static final char C = 0x3130;
    private static final char D = 0x318F;
    private static final char E = 0x1100;
    private static final char F = 0x11FF;
    private static final char G = 0x20A9;


    /**
     * Draw a shape with line style.
     *
     * @param g         the graphics.
     * @param shape     the shape to be rendered.
     * @param lineStyle the line style of rectangle.
     */
    public static void draw(Graphics g, Shape shape, int lineStyle) {
        Graphics2D g2d = (Graphics2D) g;
        Stroke oldStroke = g2d.getStroke();

        BasicStroke stroke = (BasicStroke) com.fr.base.GraphHelper.getStroke(lineStyle);
        // kunsnat: BasicStroke中将1.0f改为0.2f(0.5f以下都可以). 因为1.0f在RoundRect的时候有明显粗糙
        if (shape instanceof RoundRectangle2D) {
            if (getLineStyleSize(lineStyle) == 1) {
                stroke = new BasicStroke(0.2f, stroke.getEndCap(), stroke.getLineJoin(),
                        stroke.getMiterLimit(), stroke.getDashArray(), stroke.getDashPhase());
            }
        }
        com.fr.base.GraphHelper.setStroke(g2d, stroke);

        g2d.draw(shape);

        g2d.setStroke(oldStroke);
    }


    /**
     * Draw a line according to type.
     * The lineStyle was defined in ReportConstants.
     */
    public static void drawLine(Graphics g,
                                double x1, double y1, double x2, double y2, int lineStyle) {
        if (lineStyle == Constants.LINE_NONE) {
            return;
        } else if (lineStyle == Constants.LINE_DOUBLE) {
            double xd = x2 - x1;
            double yd = y2 - y1;
            int l = (int) Math.sqrt(xd * xd + yd * yd);

            if (l != 0) {
                int xadj = (int) (-yd / l);
                int yadj = (int) (xd / l);

                drawLine(g, x1 - xadj, y1 - yadj, x2 - xadj, y2 - yadj,
                        Constants.LINE_THIN);
                drawLine(g, x1 + xadj, y1 + yadj, x2 + xadj, y2 + yadj,
                        Constants.LINE_THIN);
                return;
            }
        } else if (lineStyle == Constants.LINE_DOUBLE_DOT) {
            double xd = x2 - x1;
            double yd = y2 - y1;
            int l = (int) Math.sqrt(xd * xd + yd * yd);

            if (l != 0) {
                int width = 1;
                int xadj = (int) (-yd * width / l);
                int yadj = (int) (xd * width / l);

                drawLine(g, x1, y1, x2, y2, Constants.LINE_DOT);
                drawLine(g, x1 + xadj, y1 + yadj, x2 + xadj, y2 + yadj,
                        Constants.LINE_DOT);
                return;
            }
        }
        synchronized (tmpLine2D) {
            tmpLine2D.setLine(x1, y1, x2, y2);
            FractionalGraphHelper.draw(g, tmpLine2D, lineStyle); //drawline now.
        }
    }
    /**
     * Draw string.
     *
     * @param str the string to be rendered.
     * @param x   the X coordinates.
     * @param y   the X coordinates.
     *            should be rendered.
     */
    public static void drawString(Graphics g, FractionalAttr attr, String str, int w, int h, double x, double y) {
        if (StringUtils.isBlank(str) && !attr.isAlwaysShow()) {
            return;
        }
        if (g.getFont() instanceof FRFont) {
            FRFont frFont = (FRFont) g.getFont();
            FontMetrics fm = g.getFontMetrics(frFont);
            int style = frFont.getStyle();

            if (frFont.isSuperscript()) {
                y = y - fm.getAscent() / 3;
                frFont = FRFont.getInstance(frFont.getName(), style, frFont.getSize() * 2 / 3,
                        frFont.getForeground(), frFont.getUnderline(),
                        frFont.isStrikethrough(), frFont.isShadow(), frFont.isSuperscript(), frFont.isSubscript());
                fm = g.getFontMetrics(frFont);
                g.setFont(frFont);
            } else if (frFont.isSubscript()) {
                y = y + fm.getDescent() / 3;
                frFont = FRFont.getInstance(frFont.getName(), style, frFont.getSize() * 2 / 3,
                        frFont.getForeground(), frFont.getUnderline(),
                        frFont.isStrikethrough(), frFont.isShadow(), frFont.isSuperscript(), frFont.isSubscript());
                fm = g.getFontMetrics(frFont);
                g.setFont(frFont);
            }
            if (frFont.isShadow()) {
                Color color = g.getColor();
                g.setColor(color.brighter());
                FractionalGraphHelper.drawNormalString(g, attr, str, x + 1, y + 1);
                g.setColor(color);
            }
            FractionalGraphHelper.drawNormalString(g, attr, str, x, y);//draw string.
            if (frFont.getUnderline() != Constants.LINE_NONE) {
                double hgap = attr.getHgap();
                double vgap = attr.getVgap();
                if (attr.getPosition() == Position.BOTTOM) {
                    double bot = h - vgap;
                    FractionalGraphHelper.drawLine(g, hgap - 3, bot, w - hgap, bot, frFont.getUnderline());
                } else {
                    FractionalGraphHelper.drawLine(g, hgap - 3, vgap, w - hgap, vgap, frFont.getUnderline());
                }
                //double bot = y + fm.getDescent() + FractionalGraphHelper.getLineStyleSize(frFont.getUnderline()) + 6;

            }
            if (frFont.isStrikethrough()) {
                int height = fm.getHeight() + 1;

                double mid = y - fm.getAscent() + (height / 2);
                FractionalGraphHelper.drawLine(g, x, mid, x + fm.stringWidth(str), mid, Constants.LINE_THIN);
            }
        } else {
            FractionalGraphHelper.drawNormalString(g, attr, str, x, y);
        }
    }

    /**
     * Draw string, donot check the font.
     *
     * @param str the string to be rendered.
     * @param x   the X coordinates.
     * @param y   the X coordinates.
     *            should be rendered.
     */
    private static void drawNormalString(Graphics g, FractionalAttr attr, String str, double x, double y) {
        if (StringUtils.isEmpty(str)) {
            return;
        }
        Font font = initFont(g);
        // kunsnat:  默认在系统不支持 无法显示时  确认为宋体. 图表保持和报表单元格一致.
        Font defaultShowFont = FRFont.getInstance(Inter.getLocText("FR-Base-Song_TypeFace"), font.getStyle(), font.getSize());
        char[] chars = new char[1];
        int charX = (int) x;
        int charY = (int) y;
        for (int ci = 0, clen = str.length(); ci < clen; ci++) {
            char c = str.charAt(ci);
            if (font.canDisplay(c)) {
                chars[0] = c;
                g.drawChars(chars, 0, 1, charX, charY);
                charX += getFontMetrics(font).charWidth(c);
            } else {
                if (isKoreanCharacter(str, defaultShowFont)) {//neil:如果是韩语，就不要给他设置字体了，不然显示的全是？，这边比较蛋疼
                    Font korea = new Font("", font.getSize(), font.getSize());
                    g.setFont(korea);
                } else {
                    g.setFont(defaultShowFont);
                }
                chars[0] = c;
                g.drawChars(chars, 0, 1, charX, charY);
                charX += getFontMetrics(defaultShowFont).charWidth(c);
                g.setFont(font);
            }
        }
    }

    private static Font initFont(Graphics g) {
        Font font = g.getFont();
        FontProcessor processor = ExtraClassManager.getInstance().getSingle(FontProcessor.MARK_STRING);
        if (processor != null) {
            font = processor.readExtraFont(font);
            g.setFont(font);
        }
        return font;
    }

    private static boolean isKoreanCharacter(String gridString, Font defaultShowFont) {
        char[] value = gridString.toCharArray();
        for (int i = 0; i < value.length; i++) {
            //韩文拼音：AC00-D7AF	  韩文字母：1100-11FF	韩文兼容字母：3130-318F
            if (inKoreanCharacter(value[i]) || !defaultShowFont.canDisplay(value[i])) {
                //0x20A9这个符号没找到
                return true;
            }
        }
        return false;
    }

    private static boolean inKoreanCharacter(char value) {
        return value >= A && value <= B || value > C && value < D
                || value > E && value < F || value == G;
    }

    /**
     * 画字符串.
     */
    public static void drawString2(Graphics g, FractionalAttr attr, String str, double x, double y, int width) {
        if (StringUtils.isBlank(str)) {
            return;
        }
        if (g.getFont() instanceof FRFont) {
            FRFont frFont = (FRFont) g.getFont();
            FontMetrics fm = g.getFontMetrics(frFont);
            int style = frFont.getStyle();
            if (frFont.isSuperscript()) {
                y = y - fm.getAscent() / 3;
                frFont = FRFont.getInstance(frFont.getName(), style, frFont.getSize() * 2 / 3,
                        frFont.getForeground(), frFont.getUnderline(),
                        frFont.isStrikethrough(), frFont.isShadow(), frFont.isSuperscript(), frFont.isSubscript());
                fm = g.getFontMetrics(frFont);
                g.setFont(frFont);
            } else if (frFont.isSubscript()) {
                y = y + fm.getDescent() / 3;
                frFont = FRFont.getInstance(frFont.getName(), style, frFont.getSize() * 2 / 3,
                        frFont.getForeground(), frFont.getUnderline(),
                        frFont.isStrikethrough(), frFont.isShadow(), frFont.isSuperscript(), frFont.isSubscript());
                fm = g.getFontMetrics(frFont);
                g.setFont(frFont);
            }

            if (frFont.isShadow()) {
                Color color = g.getColor();
                g.setColor(color.brighter());
                FractionalGraphHelper.drawNormalString2(g, str, x + 1, y + 1, width);
                g.setColor(color);
            }
            FractionalGraphHelper.drawNormalString2(g, str, x, y, width);//draw string.
            if (frFont.getUnderline() != Constants.LINE_NONE) {
                double bot = y + fm.getDescent() + FractionalGraphHelper.getLineStyleSize(frFont.getUnderline());
                FractionalGraphHelper.drawLine(g, x, bot, x + fm.stringWidth(str), bot, frFont.getUnderline());
            }
            if (frFont.isStrikethrough()) {
                int height = fm.getHeight() + 1;
                double mid = y - fm.getAscent() + (height / 2);
                FractionalGraphHelper.drawLine(g, x, mid, x + fm.stringWidth(str), mid, Constants.LINE_THIN);
            }
        } else {
            FractionalGraphHelper.drawNormalString2(g, str, x, y, width);
        }
    }

    //10506 from 655
    private static void drawNormalString2(Graphics g, String str, double x, double y, int width) {
        if (StringUtils.isEmpty(str)) {
            return;
        }
        Font f = g.getFont();
        Font simsunFont = null;
        char[] chars = new char[1];
        int charX = (int) x;
        int charY = (int) y;
        int clen = str.length();
        FractionalGraphHelper.PP[] pps = new FractionalGraphHelper.PP[clen];
        charX = initPPsAndChartX(str, clen, charX, simsunFont, f, g, pps);

        int dw = charX - (int) x;
        if (dw < (width - 1) && clen > 1) {
            int offset = width - dw;
            int b = offset / (clen - 1);
            int c = offset % (clen - 1);
            double o = 0;
            if (c != 0) {
                o = (double) c / (clen - 1);
            }
            double ox = 0;
            int oi = 0;
            for (int i = 1; i < pps.length; i++) {
                oi += b;
                ox += o;
                if (ox > 1) {
                    oi += 1;
                    ox -= 1;
                }
                pps[i].textX += oi;
            }
        }

        for (int i = 0; i < pps.length; i++) {
            if (pps[i].font != f) {
                g.setFont(pps[i].font);
            }
            chars[0] = pps[i].c;
            g.drawChars(chars, 0, 1, pps[i].textX, charY);
            if (pps[i].font != f) {
                g.setFont(f);
            }
        }
    }

    private static int initPPsAndChartX(String str, int clen, int charX, Font simsunFont, Font f, Graphics g, FractionalGraphHelper.PP[] pps) {
        for (int ci = 0; ci < clen; ci++) {
            char c = str.charAt(ci);
            if (f.canDisplay(c)) {
                pps[ci] = new FractionalGraphHelper.PP(c, f, charX);
                charX += getFontMetrics(f).charWidth(c);
            } else {
                if (simsunFont == null) {
                    simsunFont = new Font(Inter.getLocText("FR-Base-Song_TypeFace"), f.getStyle(), f.getSize());
                }
                g.setFont(simsunFont);
                pps[ci] = new FractionalGraphHelper.PP(c, simsunFont, charX);
                charX += getFontMetrics(simsunFont).charWidth(c);
                g.setFont(f);
            }
        }

        return charX;
    }

    private static class PP {
        private char c;
        private Font font;
        private int textX;

        public PP(char c, Font font, int textX) {
            this.c = c;
            this.font = font;
            this.textX = textX;
        }
    }

    /**
     * 画旋转字体.
     */
    public static void drawRotatedString(Graphics g, String str, double x, double y, int rotation) {
        drawRotatedString(g, str, x, y, rotation, 1);
    }

    /**
     * Draw rotated string.
     *
     * @param str      the string to be rendered.
     * @param x        the X coordinates.
     * @param y        the X coordinates.
     * @param rotation the rotation of the string.
     */
    public static void drawRotatedString(Graphics g, String str, double x, double y, int rotation, double scale) {
        if (str == null || str.length() == 0) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;

        Font font = g2d.getFont();
        if (font instanceof FRFont) {
            createCellFont(g2d, (FRFont) font, y);
        }

        FontMetrics fontmetrics = g2d.getFontMetrics();
        FontRenderContext fontrendercontext = g2d.getFontRenderContext();
        Rectangle2D rectangle2d = fontmetrics.getStringBounds(str, g2d);
        LineMetrics linemetrics = font.getLineMetrics(str, fontrendercontext);
        double lineDescent = linemetrics.getDescent();
        double lineLeading = linemetrics.getLeading();

        double af[] = new double[2];
        af[0] = (-rectangle2d.getWidth()) / 2.0F;
        af[1] = (-lineDescent - lineLeading) + (rectangle2d.getHeight() / 2D);

        //rotation anchor offsets.
        double af1[] = new double[2];
        af1[0] = rectangle2d.getWidth() / 2.0F;
        af1[1] = (lineDescent + lineLeading) - (rectangle2d.getHeight() / 2D);

        AffineTransform oldTransform = g2d.getTransform();
        g2d.transform(AffineTransform.getRotateInstance(Math.toRadians(rotation), x + af[0] + af1[0], y + af[1] + af1[1]));

        if (g2d.getFont() instanceof FRFont) {
            drawTextWithFRFont((FRFont) g2d.getFont(), g2d, str, x, y, scale, fontmetrics, af);
        } else {
            TextLayout textlayout = new TextLayout(str, g2d.getFont(), g2d.getFontRenderContext());
            g2d.scale(scale, scale);
            textlayout.draw(g2d, (float) ((x + af[0]) / scale), (float) ((y + af[1]) / scale));
            g2d.scale(1 / scale, 1 / scale);
        }

        g2d.setTransform(oldTransform);
    }

    private static void drawTextWithFRFont(FRFont cellFont, Graphics2D g2d, String str,
                                           double x, double y, double scale, FontMetrics fontmetrics, double[] af) {
        TextLayout textlayout = new TextLayout(str, g2d.getFont(), g2d.getFontRenderContext());
        float textX = (float) (x + af[0]);
        float textY = (float) (y + af[1]);

        if (cellFont.isShadow()) {
            Color color = g2d.getColor();
            g2d.setColor(color.brighter());
            g2d.scale(scale, scale);
            textlayout.draw(g2d, (float) ((textX + 1) / scale), (float) ((textY + 1) / scale));
            g2d.scale(1 / scale, 1 / scale);
            g2d.setColor(color);
        }
        //获取字符串宽度
        int width = fontmetrics.stringWidth(str);
        //画下划线
        com.fr.base.GraphHelper.drawLine(g2d, textX, textY + fontmetrics.getDescent(), textX + width,
                textY + fontmetrics.getDescent(), cellFont.getUnderline());
        if (cellFont.isStrikethrough()) { //画删除线
            com.fr.base.GraphHelper.drawLine(g2d, textX,
                    textY + fontmetrics.getDescent() - fontmetrics.getAscent() / 2 -
                            fontmetrics.getDescent() / 2, textX + width, textY + fontmetrics.getDescent() -
                            fontmetrics.getAscent() / 2 - fontmetrics.getDescent() / 2);

        }
        g2d.scale(scale, scale);
        textlayout.draw(g2d, (float) (textX / scale), (float) (textY / scale));
        g2d.scale(1 / scale, 1 / scale);
    }

    private static void createCellFont(Graphics2D g2d, FRFont cellFont, double y) {
        FontMetrics fm = g2d.getFontMetrics(cellFont);
        int style = cellFont.getStyle();

        if (cellFont.isSuperscript()) {
            y = y - fm.getAscent() / 3;
            cellFont = FRFont.getInstance(cellFont.getName(), style, cellFont.getSize() * 2 / 3,
                    cellFont.getForeground(), cellFont.getUnderline(),
                    cellFont.isStrikethrough(), cellFont.isShadow(),
                    cellFont.isSuperscript(), cellFont.isSubscript());
            g2d.setFont(cellFont);
        } else if (cellFont.isSubscript()) {
            y = y + fm.getDescent() / 3;
            cellFont = FRFont.getInstance(cellFont.getName(), style, cellFont.getSize() * 2 / 3,
                    cellFont.getForeground(), cellFont.getUnderline(),
                    cellFont.isStrikethrough(), cellFont.isShadow(),
                    cellFont.isSuperscript(), cellFont.isSubscript());
            g2d.setFont(cellFont);
        }
    }

    /**
     * Return the size of line style.
     */
    public static int getLineStyleSize(int lineStyle) {
        if (lineStyle == Constants.LINE_NONE) {
            return 0;
        } else if (lineStyle == Constants.LINE_MEDIUM ||
                lineStyle == Constants.LINE_DOUBLE ||
                lineStyle == Constants.LINE_MEDIUM_DASH ||
                lineStyle == Constants.LINE_MEDIUM_DASH_DOT ||
                lineStyle == Constants.LINE_MEDIUM_DASH_DOT_DOT ||
                lineStyle == Constants.LINE_SLANTED_DASH_DOT) {
            return 2;
        } else if (lineStyle == Constants.LINE_THICK) {
            return 3;
        }

        return 1;
    }



    /**
     * 将旋转的字符串转为矩形区域
     *
     * @param string            字符串
     * @param font              字体
     * @param rotation          旋转角度
     * @param fontRenderContext 字体信息
     * @return 矩形区域
     * @date 2014-12-21-下午6:48:34
     */
    public static Dimension2D stringDimensionWithRotation(
            String string, Font font, int rotation, FontRenderContext fontRenderContext) {
        Rectangle2D strBounds = font.getStringBounds(string, fontRenderContext);
        //2014-12-16 string.length() * font.getSize() -> strBounds.getWidth()
        //取String.length并不准确, 因为英文字符是旋转的, 如果算水平的还可以, 垂直的就不对了
        double norWidth = strBounds.getWidth();

        double nRotation = Math.toRadians(rotation);
        return new DoubleDimension2D(norWidth * Math.abs(Math.cos(nRotation)) +
                strBounds.getHeight() * Math.abs(Math.sin(nRotation)),
                norWidth * Math.abs(Math.sin(nRotation)) + strBounds.getHeight() * Math.abs(Math.cos(nRotation)));
    }


    /**
     * Do not create new FontMetrics object every time.
     */
    public static FontMetrics getFontMetrics(Font font) {
        //Compare hashcode to the same Font object.
        if (font == oldFont) {
            return oldFontMetrics;
        }

        if (font == null) {
            //这里主动从Context, 获得默认的FRFont的值.
            DefaultValues defaultValues = FRContext.getDefaultValues();
            font = defaultValues.getFRFont();
        }

        FontMetrics returnFontMetrics = (FontMetrics) fontMetricsHash.get(font);
        if (returnFontMetrics == null) {
            returnFontMetrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
            fontMetricsHash.put(font, returnFontMetrics);
        }

        oldFont = font;
        oldFontMetrics = returnFontMetrics;

        return returnFontMetrics;
    }

    //保留住FontMetrics
    private static java.awt.Font oldFont = null;
    private static FontMetrics oldFontMetrics = null;
    private static Map fontMetricsHash = new ConcurrentHashMap();


}