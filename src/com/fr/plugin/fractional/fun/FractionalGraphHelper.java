package com.fr.plugin.fractional.fun;

/*
 * Copyright(c) 2001-2010, FineReport Inc, All Rights Reserved.
 */

import com.fr.base.DoubleDimension2D;
import com.fr.base.FRContext;
import com.fr.base.Style;
import com.fr.general.DefaultValues;
import com.fr.general.FRFont;
import com.fr.general.Inter;
import com.fr.plugin.ExtraClassManager;
import com.fr.stable.Constants;
import com.fr.stable.CoreGraphHelper;
import com.fr.stable.StringUtils;
import com.fr.stable.core.FontProvider;
import com.fr.stable.fun.FontProcessor;
import com.fr.stable.fun.GraphDrawProcessor;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.TextLayout;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
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

    private static final float MITERLIMIT = 10.0f;
    private static final float DASHINCR = 8.0f;
    private static final float DASHMIN = 4;

    private static final char A = 0xAC00;
    private static final char B = 0xD7A3;
    private static final char C = 0x3130;
    private static final char D = 0x318F;
    private static final char E = 0x1100;
    private static final char F = 0x11FF;
    private static final char G = 0x20A9;

    /**
     * Draw a shape
     *
     * @param g     the graphics.
     * @param shape the shape to be rendered.
     */
    public static void draw(Graphics g, Shape shape) {
        com.fr.base.GraphHelper.draw(g, shape, Constants.LINE_THIN);
    }

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
     * Draw a thin line.
     *
     * @param g  the graphics.
     * @param x1 line start x
     * @param y1 line start y
     * @param x2 line end x
     * @param y2 line end y
     */
    public static void drawLine(Graphics g, double x1, double y1, double x2, double y2) {
        drawLine(g, x1, y1, x2, y2, Constants.LINE_THIN);
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
    public static void drawString(Graphics g, Position position, String str, int w, int h, double x, double y) {
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
                FractionalGraphHelper.drawNormalString(g, str, x + 1, y + 1);
                g.setColor(color);
            }
            FractionalGraphHelper.drawNormalString(g, str, x, y);//draw string.
            if (frFont.getUnderline() != Constants.LINE_NONE) {
                if (position == Position.BOTTOM) {
                    double bot = h - 0.5;
                    FractionalGraphHelper.drawLine(g, 10, bot, w - 10, bot, frFont.getUnderline());
                } else {
                    FractionalGraphHelper.drawLine(g, 10, 0.5, w - 10, 0.5, frFont.getUnderline());
                }
                //double bot = y + fm.getDescent() + FractionalGraphHelper.getLineStyleSize(frFont.getUnderline()) + 6;

            }
            if (frFont.isStrikethrough()) {
                int height = fm.getHeight() + 1;

                double mid = y - fm.getAscent() + (height / 2);
                FractionalGraphHelper.drawLine(g, x, mid, x + fm.stringWidth(str), mid, Constants.LINE_THIN);
            }
        } else {
            FractionalGraphHelper.drawNormalString(g, str, x, y);
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
    private static void drawNormalString(Graphics g, String str, double x, double y) {
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
    public static void drawString2(Graphics g, Position position, String str, double x, double y, int width) {
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
     * 将Image对象转为BufferedImage
     *
     * @param image 图片
     * @return BufferedImage图片
     * @date 2014-12-21-下午6:51:51
     */
    public static BufferedImage createBufferedImageFromImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        CoreGraphHelper.waitForImage(image);

        int width = image.getWidth(null);
        int height = image.getHeight(null);

        BufferedImage bufferedImage = CoreGraphHelper.createBufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D big = bufferedImage.createGraphics();
        big.drawImage(image, 0, 0, null);

        return bufferedImage;
    }

    /**
     * 从图片中创建一张不带布局的图片
     *
     * @param image  指定的图片
     * @param width  宽度
     * @param height 高度
     * @param style  样式
     * @return 图片
     * @date 2014-12-21-下午6:50:48
     */
    public static BufferedImage createBufferedImageFromImageWithLayout(
            Image image, int width, int height, Style style) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        CoreGraphHelper.waitForImage(image);

        BufferedImage bufferedImage = CoreGraphHelper.createBufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bufferedImage.createGraphics();

        int imageLayout = CoreGraphHelper.changeImageLayout4Draw(image, style.getImageLayout(), width, height);
        com.fr.base.GraphHelper.paintImage(g2d, width, height, image, imageLayout,
                style.getHorizontalAlignment(), style.getVerticalAlignment(), -1, -1);

        return bufferedImage;
    }

    /**
     * Set stroke.
     */
    public static void setStroke(Graphics2D g2d, Stroke stroke) {
        Stroke oldStroke = g2d.getStroke();

        //p: 由于setStroke()运行会占据0.01的时候，属于比较慢的，这里需要判断，不每次都调用..
        if (stroke.hashCode() != oldStroke.hashCode()) {
            g2d.setStroke(stroke);
        }
    }

    /**
     * Create a stroke object from a line style.
     */
    public static Stroke getStroke(int lineStyle) {
        Stroke stroke = null;
        if (lineStyle < LineStrokeArray.length) {//已经在数组中了.
            stroke = LineStrokeArray[lineStyle];

            if (stroke != null) {
                return stroke;
            }
        } else {
            //双倍增大数组.
            Stroke[] oldLineStrokeArray = LineStrokeArray;
            LineStrokeArray = new Stroke[Math.max(10, oldLineStrokeArray.length * 2)];
            System.arraycopy(oldLineStrokeArray, 0, LineStrokeArray, 0, oldLineStrokeArray.length);
        }

        int cap = BasicStroke.CAP_BUTT;
        int join = BasicStroke.JOIN_MITER;

        stroke = createStrokeWithLineStyle(stroke, lineStyle);

        if (stroke == null) {
            stroke = new BasicStroke(1.0f, cap, join);
        }

        LineStrokeArray[lineStyle] = stroke;//保存Stroke.

        return stroke;
    }

    private static Stroke createStrokeWithLineStyle(Stroke stroke, int lineStyle) {
        int cap = BasicStroke.CAP_BUTT;
        int join = BasicStroke.JOIN_MITER;

        if (lineStyle == Constants.LINE_THIN || lineStyle == Constants.LINE_CHART_THIN_ARROW) {
            stroke = new BasicStroke(1.0f, cap, join);
        } else if (lineStyle == Constants.LINE_MEDIUM || lineStyle == Constants.LINE_CHART_MED_ARROW) {
            stroke = new BasicStroke(2.0f, cap, join);
        } else if (lineStyle == Constants.LINE_LARGE) {
            stroke = new BasicStroke(3.0f, cap, join);
        } else if (lineStyle == Constants.LINE_DASH) {
            stroke = new BasicStroke(1.0f, cap, join, MITERLIMIT, new float[]{DASHMIN, 2}, 0.0f);
        } else if (lineStyle == Constants.LINE_HAIR) {
            stroke = new BasicStroke(1.0f, cap, join, MITERLIMIT, new float[]{2}, 0.0f);
        } else if (lineStyle == Constants.LINE_THICK || lineStyle == Constants.LINE_CHART_THICK_ARROW) {
            stroke = new BasicStroke(3.0f, cap, join);
        } else if (lineStyle == Constants.LINE_DOT) {
            stroke = new BasicStroke(1.0f, cap, join, MITERLIMIT, new float[]{1}, 0.0f);
        } else if (lineStyle == Constants.LINE_MEDIUM_DASH) {
            stroke = new BasicStroke(2.0f, cap, join, MITERLIMIT, new float[]{DASHINCR, 2}, 0.0f);
        } else if (lineStyle == Constants.LINE_DASH_DOT) {
            stroke = new BasicStroke(1.0f, cap, join, MITERLIMIT, new float[]{DASHINCR, DASHMIN, 2, DASHMIN}, 0.0f);
        } else if (lineStyle == Constants.LINE_MEDIUM_DASH_DOT) {
            stroke = new BasicStroke(2.0f, cap, join, MITERLIMIT, new float[]{DASHINCR, DASHMIN, 2, DASHMIN}, 0.0f);
        } else if (lineStyle == Constants.LINE_DASH_DOT_DOT) {
            stroke = new BasicStroke(1.0f, cap, join, MITERLIMIT, new float[]{DASHINCR, DASHMIN, 2, DASHMIN, 2, DASHMIN}, 0.0f);
        } else if (lineStyle == Constants.LINE_MEDIUM_DASH_DOT_DOT) {
            stroke = new BasicStroke(2.0f, cap, join, MITERLIMIT, new float[]{DASHINCR, DASHMIN, 2, DASHMIN, 2, DASHMIN}, 0.0f);
        } else if (lineStyle == Constants.LINE_SLANTED_DASH_DOT) {
            stroke = new BasicStroke(2.0f, cap, join, MITERLIMIT, new float[]{DASHINCR, DASHMIN, 2, DASHMIN}, 0.0f);
        } else if (lineStyle == Constants.LINE_HAIR2) {
            stroke = new BasicStroke(1.0f, cap, join, MITERLIMIT, new float[]{DASHMIN}, 0.0f);
        }
        return stroke;
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
     * 获取字符宽度
     *
     * @param string            字符
     * @param font              字体
     * @param fontRenderContext 字体信息
     * @return 字符串宽度
     */
    public static double stringWidth(String string, java.awt.Font font, FontRenderContext fontRenderContext) {
        if (string == null || string.length() <= 0) {
            return 0;
        }

        double strWidth = font.getStringBounds(string, fontRenderContext).getWidth();
        if (font instanceof FontProvider) {
            FontProvider frFont = (FontProvider) font;

            if (frFont.isSuperscript() || frFont.isSubscript()) {
                strWidth = strWidth * 2 / 3;
            }
        }

        return strWidth;
    }

    /**
     * 获取国际化后的字符串长度
     *
     * @param font 当前字体
     * @param key  国际化key
     * @return 字符串长度
     */
    public static int getLocTextWidth(String key, Font font) {
        String content = Inter.getLocText(key);

        FontMetrics fontMetrics = getFontMetrics(font);
        return fontMetrics.stringWidth(content);
    }

    /**
     * 获取国际化后的字符串长度, 使用默认字体
     *
     * @param key 国际化key
     * @return 字符串长度
     */
    public static int getLocTextWidth(String key) {
        Font font = FRFont.getInstance().applyResolutionNP(Constants.FR_PAINT_RESOLUTION);
        return getLocTextWidth(key, font);
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
     * 获得字体规格属性.
     */
    public static FontMetrics getFontMetrics(Font font, Graphics2D g2d) {
        if (g2d == null) {
            return getFontMetrics(font);
        }

        return g2d.getFontMetrics(font);
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

    //这个地方为了加快速度，所以提前将所有的Line将要用到的Stroke放到这里。
    private static Stroke[] LineStrokeArray = new Stroke[15];


    /**
     * Paint Image according to its layout and style
     *
     * @param g
     * @param width  the width of the draw pane
     * @param height the height of the draw pane
     * @param image  image to be painted
     * @param layout the layout of the image
     */
    public static void paintImage(Graphics g, int width, int height, Image image,
                                  int layout, int hAlign, int vAlign, int specifiedImageWidth, int specifiedImageHeight) {
        GraphDrawProcessor drawProcessor = ExtraClassManager.getInstance().getSingle(GraphDrawProcessor.XML_TAG);
        if (drawProcessor != null) {
            drawProcessor.paintImage(g, width, height, image, layout, hAlign, vAlign, specifiedImageWidth, specifiedImageHeight);
        } else {
            paintImageMoved(g, width, height, image, layout, hAlign, vAlign, specifiedImageWidth, specifiedImageHeight, 0, 0, false);
        }
    }

    /**
     * 默认非移动初始位置 初始高度偏移0, 初始宽度偏移0, 图片paint
     */
    public static void paintImage(Graphics g, int width, int height, Image image,
                                  int layout, int hAlign, int vAlign, int specifiedImageWidth, int specifiedImageHeight, boolean isNotSupportARGB) {
        paintImageMoved(g, width, height, image, layout, hAlign, vAlign, specifiedImageWidth, specifiedImageHeight, 0, 0, isNotSupportARGB);
    }

    /**
     * 默认背景白色的 图片paint
     */
    public static void paintImageMoved(Graphics g, int width, int height, Image image, int layout, int hAlign, int vAlign, int specifiedImageWidth, int specifiedImageHeight,
                                       int moveWidth, int moveHeight, boolean isNotSupportARGB) {
        paintImageMoved(g, width, height, image, layout, hAlign, vAlign, specifiedImageWidth, specifiedImageHeight, moveWidth, moveHeight, isNotSupportARGB, Color.white);
    }

    /**
     * 将图片绘制到画板上
     *
     * @param g      当前画板
     * @param width  画板宽度
     * @param height 画板高度
     * @param image  绘制的图片
     * @param layout 绘制图片的布局
     */
    public static void paintImageMoved(Graphics g, int width, int height, Image image, int layout) {
        paintImageMoved(g, width, height, image, layout, 0, 0, -1, -1, 0, 0, true, Color.WHITE);
    }

    /**
     * Paint Image according to its layout and style
     *
     * @param g
     * @param width      the width of the draw pane
     * @param height     the height of the draw pane
     * @param image      image to be painted
     * @param layout     the layout of the image
     * @param moveWidth  the moveWidth that the background hide
     * @param moveHeight the moveHeight that the background hide
     *                   see paintImage(), this method has two more parameters.
     *                   by Denny
     *                   note by Denny: 这个函数应该是用来在冻结行列时候画背景图片的，所以多了两个参数.
     */
    public static void paintImageMoved(Graphics g, int width, int height, Image image,
                                       int layout, int hAlign, int vAlign, int specifiedImageWidth, int specifiedImageHeight,
                                       int moveWidth, int moveHeight,
                                       boolean isNotSupportARGB, Color backgroundcolor) {
        if (image == null) {
            return;
        }
        CoreGraphHelper.waitForImage(image);    // load image
        // denny: 得到图片的宽，如有指定的宽，按指定的宽
        int imgWidth;
        if (specifiedImageWidth == -1) {
            imgWidth = image.getWidth(null);
        } else {
            imgWidth = specifiedImageWidth;
        }
        int imgHeight; // denny: 得到图片的高，如有指定的高，按指定的高
        if (specifiedImageHeight == -1) {
            imgHeight = image.getHeight(null);
        } else {
            imgHeight = specifiedImageHeight;
        }
        if (imgWidth < 0 || imgHeight < 0) {
            return; // denny: 看其它代码是这样写的，不太清楚会不会遇到这种可能
        }

        layout = CoreGraphHelper.changeImageLayout4Draw(image, layout, width, height);

        //denny: 按照layout绘图
        if (layout == Constants.IMAGE_TILED) {
            drawLayoutTile(g, width, height, image, imgWidth, imgHeight, moveWidth, moveHeight, backgroundcolor);
        } else if (layout == Constants.IMAGE_CENTER) {
            drawLayoutCenter(g, width, height, image, imgWidth, imgHeight, moveWidth, moveHeight, isNotSupportARGB, backgroundcolor);
        } else if (layout == Constants.IMAGE_ADJUST) {
            drawLayoutAdjust(g, width, height, image, moveWidth, moveHeight, backgroundcolor, isNotSupportARGB);
        } else if (layout == Constants.IMAGE_EXTEND) {
            // denny: 拉伸
            for (int x = -moveWidth % width; x < width; x += width) {
                for (int y = -moveHeight % height; y < height; y += height) {
                    g.drawImage(image, x, y, width, height, null);
                }
            }
        } else if (layout == Constants.IMAGE_DEFAULT) {
            drawLayoutDefault(g, width, height, image, imgWidth, imgHeight,
                    hAlign, vAlign, moveWidth, moveHeight, isNotSupportARGB, backgroundcolor);
        }
    }

    private static void drawLayoutTile(Graphics g, int width, int height,
                                       Image image, int imgWidth, int imgHeight,
                                       int moveWidth, int moveHeight, Color backgroundcolor) {
        // denny: 平铺
        int tmpx = -moveWidth;
        int tmpy = -moveWidth;
        for (int x = -moveWidth; x < width; x += imgWidth) {    // denny: 增加的moveWidth参量用来隐去图片的一部分
            if ((x + moveWidth) / width > (tmpx + moveWidth) / width) {
                x = ((tmpx + moveWidth) / width + 1) * width - moveWidth;
            }
            for (int y = -moveHeight; y < height; y += imgHeight) {        // denny: 增加的moveHeight参量用来隐去图片的上面
                if ((y + moveHeight) / height > (tmpy + moveHeight) / height) {
                    y = ((tmpy + moveHeight) / height + 1) * height - moveHeight;
                }
                try {
                    g.drawImage(image, x, y, imgWidth, imgHeight, null);
                } catch (Exception e) {
                    //neil: swfgraphics2d画一些格式图片时会出运行时异常IIOException, catch住重画一下, 见bug11429
                    BufferedImage bufferedImage = CoreGraphHelper.createBufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D buffered_g2d = bufferedImage.createGraphics();
                    buffered_g2d.setPaint(backgroundcolor);
                    com.fr.base.GraphHelper.paintImage(buffered_g2d, imgWidth, imgHeight, image, Constants.IMAGE_EXTEND,
                            Constants.NULL, Constants.CENTER, -1, -1);
                    g.drawImage(bufferedImage, x, y, imgWidth, imgHeight, null);
                }

                tmpx = x;
                tmpy = y;
            }
        }
    }

    private static void drawLayoutCenter(Graphics g, int width, int height,
                                         Image image, int imgWidth, int imgHeight,
                                         int moveWidth, int moveHeight, boolean isNotSupportARGB, Color backgroundcolor) {
        for (int x = ((width - imgWidth) / 2 - moveWidth) % width; x < width; x += width) {// denny: 左右居中
            for (int y = ((height - imgHeight) / 2 - moveHeight) % height; y < height; y += height) {    // denny: 上下居中
                g.drawImage(image, Math.max(x, x - (width - imgWidth) / 2), Math.max(y, y - (height - imgHeight) / 2),
                        (x > (x - (width - imgWidth) / 2) ? imgWidth : width),
                        (y > (y - (height - imgHeight) / 2) ? imgHeight : height), null);
                //daniel
                //如果图片比容器小的话 ，非ARGB色需要将其他地方填充为白色内容，否则会显示为黑色
                //所以这里单独处理下
                //居中填充分为4个部分上，下 ，左 ，右
                if (isNotSupportARGB) {
                    Color oldColor = g.getColor();
                    g.setColor(backgroundcolor);
                    //上
                    //下 有上就有下   小就不画
                    if (height > imgHeight) {
                        //上
                        g.fillRect(moveWidth % width, moveHeight % height, width, (height - imgHeight) / 2);
                        //下
                        g.fillRect(moveWidth % width, Math.max(y, y - (height - imgHeight) / 2) + imgHeight, width, (height - imgHeight) / 2);
                    }

                    //左
                    //右 同上
                    if (width > imgWidth) {
                        //左
                        g.fillRect(moveWidth % width, moveHeight % height, (width - imgWidth) / 2, height);
                        //右
                        g.fillRect(Math.max(x, x - (width - imgWidth) / 2) + imgWidth, moveHeight % height, (width - imgWidth) / 2, height);
                    }

                    g.setColor(oldColor);
                }
            }
        }
    }

    private static void drawLayoutAdjust(Graphics g, int width, int height,
                                         Image image, int moveWidth, int moveHeight, Color backgroundcolor, boolean isNotSupportARGB) {
        if (width == 0 || height == 0) {
            return;
        }

        int imageWidth = image.getWidth(null); // neil:适应, 所谓的适应就是保持长宽比的拉伸, 保证图片在拉伸过程中不扭曲.
        int imageHeight = image.getHeight(null);

        for (int x = -moveWidth % width; x < width; x += width) {
            for (int y = -moveHeight % height; y < height; y += height) {
                double scaleWidth = (double) width / imageWidth;
                double scaleHeight = (double) height / imageHeight;

                int oldX = x;
                int oldY = y;

                //实际绘制的宽高
                int paintWidth = width;
                int paintHeight = height;

                if (scaleWidth > scaleHeight) {
                    paintWidth = (int) (width * scaleHeight / scaleWidth);
                    x = (width - paintWidth) / 2;
                } else {
                    paintHeight = (int) (height * scaleWidth / scaleHeight);
                    y = (height - paintHeight) / 2 + y;
                }

                Color oldColor = g.getColor();
                if (isNotSupportARGB) {
                    g.setColor(backgroundcolor);
                    g.fillRect(x, y, width, height);
                    g.setColor(oldColor);
                }

                g.drawImage(image, x, y, paintWidth, paintHeight, null);


                if (isNotSupportARGB && oldX == 0 && oldY == 0) {
                    drawWhiteRec(g, backgroundcolor, paintHeight, paintWidth, width, height, oldColor);
                }
            }
        }
    }

    private static void drawWhiteRec(Graphics g, Color backgroundcolor, int paintHeight, int paintWidth,
                                     int width, int height, Color oldColor) {
        //这边为什么要这么麻烦花白色矩形呢, 因为不画的话, 填报那边上传的图片有黑边.
        g.setColor(backgroundcolor);
        //左侧白色矩形
        if (width > paintWidth) {
            g.fillRect(0, 0, (width - paintWidth) / 2, height);
        }

        //上边白色矩形
        if (height > paintHeight) {
            g.fillRect(0, 0, width, (height - paintHeight) / 2);
        }
        g.setColor(oldColor);

    }

    private static void drawLayoutDefault(Graphics g, int width, int height, Image image, int imgWidth, int imgHeight,
                                          int hAlign, int vAlign, int moveWidth, int moveHeight, boolean isNotSupportARGB, Color backgroundcolor) {
        // denny: 默认  具体位置由单元格属性决定
        int left, top;


        if (hAlign == Constants.RIGHT) {      // denny: 水平方向的起始位置
            left = width - imgWidth - moveWidth;
        } else if (hAlign == Constants.CENTER) {
            left = (width - imgWidth) / 2 - moveWidth;
        } else {
            left = -moveWidth;
        }

        if (vAlign == Constants.BOTTOM) {   // denny: 垂直方向的其实位置
            top = height - imgHeight - moveHeight;
        } else if (vAlign == Constants.CENTER) {
            top = (height - imgHeight) / 2 - moveHeight;
        } else {
            top = -moveHeight;
        }

        //p:需要图片的宽度和高度大于0,不然PdfGraphics2D会报错.
        if (imgWidth > 0 && imgHeight > 0) {
            for (int x = left % width; x < width; x += width) {
                for (int y = top % height; y < height; y += height) {
                    g.drawImage(image, x, y, imgWidth, imgHeight, null);
                    if (isNotSupportARGB) {
                        //daniel 剩下的没有画的部分需要画白色，否则会显示黑色内容
                        //分为个两个区域来画 右边 与下边  ，右下角会画两次 但如果单独画右下角就要分三个区域
                        //首先计算量会增大,而且不能确定哪个更好一点
                        Color oldColor = g.getColor();
                        g.setColor(backgroundcolor);
                        if (width > imgWidth) {//右边：小就不用画了
                            g.fillRect(imgWidth, y, width - imgWidth, height);
                        }
                        if (height > imgHeight) {    //下边：同上
                            g.fillRect(x, imgHeight, width, height - imgHeight);
                        }
                        g.setColor(oldColor);
                    }
                }
            }
        }
    }
}