package com.fr.plugin.fractional.fun;

import com.fr.base.FRContext;
import com.fr.base.FRCoreContext;
import com.fr.base.Style;
import com.fr.general.ComparatorUtils;
import com.fr.general.FRFont;
import com.fr.general.Inter;
import com.fr.plugin.ExtraClassManager;
import com.fr.report.fun.VerticalTextProcessor;
import com.fr.report.fun.impl.DefaultVerticalTextProcessor;
import com.fr.stable.Constants;
import com.fr.stable.StringUtils;
import com.fr.stable.fun.FontProcessor;
import com.fr.stable.unit.PT;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.util.Arrays;
import java.util.Locale;

/**
 * 基本的工具类
 */
public class FractionalBaseUtils {

    private FractionalBaseUtils() {
    }

    /**
     * 获取样式中字体的类型
     *
     * @param g2d        图形上下文
     * @param style      样式
     * @param resolution 屏幕分辨率
     * @return 字体
     */
    public static Font getStyleFont(Graphics2D g2d, Style style, int resolution) {
        FRFont font = style.getFRFont();
        Font rfont = initFontWithLocaleAndG2d(g2d, font, resolution);
        if (!ComparatorUtils.equals(rfont, g2d.getFont())) {
            g2d.setFont(rfont);
        }
        Color foreground = font.getForeground();
        if (!ComparatorUtils.equals(foreground, g2d.getPaint())) {
            g2d.setPaint(foreground);
        }

        return rfont;
    }

    private static Font initFontWithLocaleAndG2d(Graphics2D g2d, FRFont font, int resolution) {
        Locale locale = FRContext.getLocale();
        Font rfont;
        if (ComparatorUtils.equals(Locale.ENGLISH, locale)) {
            rfont = FRFont.getInstance("Dialog", font.getStyle(), font.getSize());
        }

        rfont = font.applyResolutionNP(resolution);
        //itext的bug 用SimSun导出无法实现粗体斜体, 其作者解释是使用basefont是不支持这些style的, 要转成itext的亚洲字体
        //这边不用instance of的原因就是不想把itext包引到applet包里面去.
        if (g2d.toString().indexOf("PdfGraphics2D") != -1 && ComparatorUtils.equals(FRFont.DEFAULT_FONTNAME, font.getName())) {
            //不要把这边的'宋体'改成'Simsun', 就这么设定的
            rfont = FRFont.getInstance(Inter.getLocText("FR-Base-Song_TypeFace"), rfont.getStyle(), rfont.getSize(),
                    font.getForeground(), font.getUnderline(), font.isStrikethrough(), font.isShadow(),
                    font.isSuperscript(), font.isSubscript());
        }

        return rfont;
    }


    /**
     * 基本的画文本的方法，只考虑样式中的字体和对齐方式
     *
     * @param g2d        图形上下文
     * @param width      画文本的区域的宽度
     * @param height     画文本的区域的高度
     * @param text       要画的文本
     * @param style      样式
     * @param resolution 屏幕分辨率
     */
    public static void drawStringStyleInRotation(Graphics2D g2d, FractionalAttr attr, int width, int height, String text, Style style, int resolution) {
        if (!attr.isAlwaysShow() && StringUtils.isBlank(text)) {
            return;
        }

        Paint oldPaint = g2d.getPaint();
        Font oldFont = g2d.getFont();

        if (style == null) {
            style = Style.DEFAULT_STYLE;
        }

        Font font = getStyleFont(g2d, style, resolution);
        font = readExtraFont(g2d, font);
        int horizontalAlignment = com.fr.base.BaseUtils.getAlignment4Horizontal(style, text);

        if (style.getRotation() != 0 && style.getVerticalText() == Style.HORIZONTALTEXT) {
            drawHorizontalText(g2d, attr, text, font, style, width, height, horizontalAlignment);
        } else {
            drawRotationText(g2d, attr, text, style, font, width, height, horizontalAlignment, resolution);
        }

        g2d.setFont(oldFont);
        g2d.setPaint(oldPaint);
    }

    private static Font readExtraFont(Graphics2D g2d, Font font) {
        FontProcessor processor = ExtraClassManager.getInstance().getSingle(FontProcessor.MARK_STRING);
        if (processor != null) {
            font = processor.readExtraFont(font);
            g2d.setFont(font);
        }
        return font;
    }

    private static void drawHorizontalText(Graphics2D g2d, FractionalAttr attr, String text, Font rfont, Style style, double width, double height, int horizontalAlignment) {
        AffineTransform trans = new AffineTransform();
        trans.rotate(-Math.toRadians(style.getRotation()));

        double textX = width / 2.0;
        double textY = height / 2.0;

        Dimension2D textDimension = FractionalGraphHelper.stringDimensionWithRotation(text, rfont, -style.getRotation(), g2d.getFontRenderContext());

        if (textDimension.getWidth() < width) {
            if (horizontalAlignment == Constants.LEFT) {
                textX = textDimension.getWidth() / 2.0;
            } else if (horizontalAlignment == Constants.RIGHT) {
                textX = width - textDimension.getWidth() / 2.0 - style.getPaddingLeft() *
                        Math.cos(Math.toRadians(style.getRotation()));
            } else {
            }
        }

        // adjust y, height.
        if (textDimension.getHeight() < height) {
            if (style.getVerticalAlignment() == Constants.TOP) {
                textY = textDimension.getHeight() / 2.0;
            } else if (style.getVerticalAlignment() == Constants.BOTTOM) {
                textY = height - textDimension.getHeight() / 2.0 - style.getPaddingLeft() * Math.sin(Math.toRadians(style.getRotation()));
            } else {
            }
        }

        FractionalGraphHelper.drawRotatedString(g2d, text, textX, textY, -style.getRotation());
    }

    private static void drawRotationText(Graphics2D g2d, FractionalAttr attr, String text, Style style, Font rfont, int width, int height, int horizontalAlignment, int resolution) {
//		FontMetrics cellFM = g2d.getFontMetrics();
        FontMetrics cellFM = FractionalGraphHelper.getFontMetrics(rfont);
        java.util.List lineTextList = FractionalBaseUtils.getLineTextList(text, style, rfont, height, width, resolution);

        if (width <= 0 || lineTextList.isEmpty()) {
            FractionalGraphHelper.drawString(g2d, attr, StringUtils.EMPTY, width, height, 0, 0);
            return;
        }

        int textAscent = cellFM.getAscent();
        int textHeight = cellFM.getHeight();

        int textY = calculateTextY(style, height, textHeight, textAscent, lineTextList, resolution);

        int maxWidth = 0;
        for (int i = 0; i < lineTextList.size(); i++) {
            String paint_str = (String) lineTextList.get(i);
            int textWidth = cellFM.stringWidth(paint_str);
            if (textWidth > maxWidth) {
                maxWidth = textWidth;
            }
        }
        for (int i = 0; i < lineTextList.size(); i++) {
            String paint_str = (String) lineTextList.get(i);
            //把自定义角度为0的横排和竖排区分开来
            if (style.getRotation() == 0 && style.getVerticalText() == style.HORIZONTALTEXT) {
                maxWidth = cellFM.stringWidth(paint_str);
            }
            boolean textLonger = false;//KevinWang: 标志一下：是否串长大于列宽
            if (maxWidth > width - style.getPaddingLeft() - style.getPaddingRight()) {
                textLonger = true;//added by KevinWang  <处理分散对齐时使用>
            } //待会串长大于列宽时需要特别处理：只从中抽取合适长度的串值显示

            int textX = calculateTextX(style, rfont, paint_str, width, maxWidth, horizontalAlignment);

            if (isAutomaticLine(style, horizontalAlignment, textLonger)) {//自动换行时
                FractionalGraphHelper.drawString2(g2d, attr, paint_str, textX, textY, width);
            } else if (isDistributeAlign(style, horizontalAlignment, textLonger)) {
                drawTextWihenDistributeAlign(g2d, attr, paint_str, cellFM, width, height, textX, textY);
            } else {  //这里不能删除
                FractionalGraphHelper.drawString(g2d, attr, paint_str, width, height, textX, textY);
            }
            textY += textHeight;// TODO 只增加了Y.
            textY += PT.pt2pix(style.getLineSpacing(), resolution);
        }
    }

    /**
     * 计算Y的高度
     *
     * @param style        样式
     * @param height       总高度
     * @param textHeight   文本高度
     * @param textAscent   字体的基线到大多数字母数字字符顶部的距离
     * @param lineTextList 文本列
     * @param resolution   分辨率
     * @return Y高度
     */
    public static int calculateTextY(Style style, int height, int textHeight, int textAscent, java.util.List lineTextList, int resolution) {
        // 计算Y的高度.
        int textY = 0;
        int textAllHeight = textHeight * lineTextList.size();
        double spacingBefore = PT.pt2pix(style.getSpacingBefore(), resolution);
        double spacingAfter = PT.pt2pix(style.getSpacingAfter(), resolution);
        double lineSpacing = PT.pt2pix(style.getLineSpacing(), resolution);
        textAllHeight += spacingBefore + spacingAfter + lineSpacing * lineTextList.size();
        if (style.getVerticalAlignment() == Constants.TOP) {
        } else if (style.getVerticalAlignment() == Constants.CENTER) {
            if (height > textAllHeight) {// 如果所有文本的高度小于当前可以绘区域的高度,就从0开始画字符.
                textY = (height - textAllHeight) / 2;
            }
        } else if (style.getVerticalAlignment() == Constants.BOTTOM) {
            if (height > textAllHeight) {
                textY = height - textAllHeight;
            }
        }
        textY += textAscent;// 在绘画的时候,必须添加Ascent的高度.
        textY += spacingBefore + lineSpacing;//james：加上"段前间距"+“行间距”
        return textY;
    }

    /**
     * 计算X宽度
     *
     * @param style               样式
     * @param rfont               字体
     * @param paint_str           字符串
     * @param width               宽度
     * @param textWidth           文本宽度
     * @param horizontalAlignment 垂向对齐
     * @return X宽度
     */
    public static int calculateTextX(Style style, Font rfont, String paint_str, int width, int textWidth, int horizontalAlignment) {
        int textX = style.getPaddingLeft();
        if (horizontalAlignment == Constants.CENTER) {
            textX = (width - textWidth) / 2;
        } else if (horizontalAlignment == Constants.RIGHT) {
            textX = width - style.getPaddingRight() - textWidth;
            // TODO alex:just for flash print && for font.Tahoma
            if (Boolean.TRUE == FRCoreContext.TMAP.get()
                    && ComparatorUtils.equals(rfont.getFontName(), "Tahoma")) {
                textX -= paint_str.length() / 3;
            }
        }
        return textX;
    }

    private static boolean isAutomaticLine(Style style, int horizontalAlignment, boolean textLonger) {
        return horizontalAlignment == Constants.DISTRIBUTED
                && style.getTextStyle() == Style.TEXTSTYLE_WRAPTEXT
                || horizontalAlignment == Constants.DISTRIBUTED
                && style.getTextStyle() == Style.TEXTSTYLE_SINGLELINE
                && !textLonger;
    }

    private static boolean isDistributeAlign(Style style, int horizontalAlignment, boolean textLonger) {
        return horizontalAlignment == Constants.DISTRIBUTED
                && style.getTextStyle() == Style.TEXTSTYLE_SINGLELINE
                && textLonger;
    }

    private static void drawTextWihenDistributeAlign(Graphics2D g2d, FractionalAttr attr, String paint_str, FontMetrics cellFM, int width, int height, int textX, int textY) {
        // 单行显示时的分散对齐实现
        //串长大于列宽时需要特别处理：只从中抽取合适长度的串值显示
        String lineText = paint_str;// 分行显示时第i行的内容，但是也可能单行
        StringBuffer strBuff = new StringBuffer();
        for (int charIndex = 0; charIndex < lineText.length(); charIndex++) {
            strBuff.append(lineText.charAt(charIndex));
            int buffWidth = cellFM.stringWidth(new String(strBuff));
            if (buffWidth > width) { //长度足够了，开始显示
                char[] tmpChars = new char[128];//可能因为不足而危险/出错
                strBuff.getChars(0, charIndex - 1, tmpChars, 0);//获得append之前构造的那个串值
                tmpChars[charIndex] = '\0';//串结束标志，用于消除串尾乱码，但是不起作用，不知怎的。
                FractionalGraphHelper.drawString(g2d, attr, new String(tmpChars), width, height, textX, textY);
                break;//只画一行，画完就退出
            }
        }
    }

    /**
     * james daniel 放一起
     * 同时含有height和width参数表示会根据style自动判断字体为竖排还是横排
     * TODO
     */
    public static java.util.List getLineTextList(String text, Style style, Font font, double paintHeight, double paintWidth, int resolution) {
        //正常文字
        if (style == null
                || style.getVerticalText() != Style.VERTICALTEXT) {//james:正常的文字时
            return com.fr.base.BaseUtils.getLineTextList(text, style, font, paintWidth);
        }

        VerticalTextProcessor processor = ExtraClassManager.getInstance().getSingle(VerticalTextProcessor.XML_TAG, DefaultVerticalTextProcessor.class);
        String[] res = processor.process(text, style, font, paintHeight, paintWidth, resolution);
        return Arrays.asList(res);
    }
}