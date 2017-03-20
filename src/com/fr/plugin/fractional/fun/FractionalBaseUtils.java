package com.fr.plugin.fractional.fun;

import com.fr.base.*;
import com.fr.data.core.DataCoreUtils;
import com.fr.general.*;
import com.fr.general.data.DataModel;
import com.fr.general.data.TableDataException;
import com.fr.general.xml.GeneralXMLTools;
import com.fr.js.JavaScript;
import com.fr.js.NameJavaScriptGroup;
import com.fr.json.JSONArray;
import com.fr.json.JSONException;
import com.fr.json.JSONObject;
import com.fr.json.JSONUtils;
import com.fr.plugin.ExtraClassManager;
import com.fr.report.fun.VerticalTextProcessor;
import com.fr.report.fun.impl.DefaultVerticalTextProcessor;
import com.fr.script.Calculator;
import com.fr.stable.Constants;
import com.fr.stable.ListSet;
import com.fr.stable.StableUtils;
import com.fr.stable.StringUtils;
import com.fr.stable.fun.FontProcessor;
import com.fr.stable.unit.PT;
import com.fr.stable.web.Repository;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.text.NumberFormat;
import java.util.*;

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
     * 获取样式中关于水平方向上的对齐类型
     *
     * @param style 样式
     * @return 水平方向上的对齐类型
     */
    public static int getAlignment4Horizontal(Style style) {
        return getAlignment4Horizontal(style, null);
    }

    /**
     * 获取DataMoodel中第columnIndex列的数据.
     *
     * @param model       取数的数据来源
     * @param columnIndex 取数的数据列序号
     * @return 返回数据数组Object[]
     */
    public static Object[] getDistinctValues(DataModel model, int columnIndex) throws TableDataException {
        ListSet list = new ListSet();
        if (columnIndex != DataModel.COLUMN_NAME_NOT_FOUND) {
            for (int i = 0, len = model.getRowCount(); i < len; i++) {
                list.add(model.getValueAt(i, columnIndex));
            }
        }

        return list.toArray();
    }

    /**
     * 获取水平方向上的对齐样式
     *
     * @param style 样式
     * @param value 单元格的值，默认情况下，当不设置对齐类型时，如果单元格的值是数字则靠右对齐，字符串则靠左对齐
     * @return 水平方向上的对齐样式
     */
    public static int getAlignment4Horizontal(Style style, Object value) {
        int horizontalAlignment = style.getHorizontalAlignment();
        //若是默认值：判断 bug5188 数字居右
        if (value != null && horizontalAlignment == Constants.NULL) {
            if (value instanceof String) {
                if (style.getFormat() instanceof NumberFormat || Utils.string2Number((String) value) != null) {
                    horizontalAlignment = Constants.RIGHT;
                } else {
                    //字符串：靠左
                    horizontalAlignment = Constants.LEFT;
                }
            } else if (value instanceof Number) {
                horizontalAlignment = Constants.RIGHT;
            }
        }
        return horizontalAlignment;
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
        if (StringUtils.isBlank(text)) {
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
     * 将输入字符串转换为实际字符，\代表转义字符。
     *
     * @param text 字符串
     * @return the real value 实际字符
     */
    public static String textToString(String text) {
        //跟这里的drawText算法是一致的，这样"所见即所得",
        //如果把drawText的参数text先用此方法转换也行的，但是会遍历俩次，所以先留着
        if (text == null) {
            return "";
        }

        int len = text.length();
        StringBuffer sb = new StringBuffer(len);

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < len) {
                char next = text.charAt(i + 1);
                if (next == 'n') {
                    i++;
                    c = '\n';
                } else if (next == '\\') {
                    i++;
                }

            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * _denny: 自动换行算法, 这个算法要求跟DHTML中Table的自动换行表现结果一样
     *
     * @param text
     * @param style
     * @param font
     * @param paintWidth
     * @return
     */
    public static java.util.List getLineTextList(String text, Style style, Font font, double paintWidth) {
        java.util.List lineTextList = new ArrayList();
        if (text == null || text.length() <= 0) {
            return lineTextList;
        }

        style = style == null ? Style.DEFAULT_STYLE : style;

        if (style.getRotation() != 0) {
            lineTextList.add(text);
            return lineTextList;
        }

        if (font == null) {
            font = style.getFRFont();
        }
        FontMetrics fontMetrics = GraphHelper.getFontMetrics(font);

        if (style.getTextStyle() != Style.TEXTSTYLE_WRAPTEXT) {
            return lineTextListNotChangeLine(lineTextList, text);
        }
        // 自动换行
        else {
            lineTextListAutoChangeLine(lineTextList, text, fontMetrics, style, paintWidth);
        }

        return lineTextList;
    }

    private static java.util.List lineTextListNotChangeLine(java.util.List lineTextList, String text) {
        char tmpChar;
        StringBuffer tmpTextBuf = new StringBuffer();
        for (int t = 0; t < text.length(); t++) {
            tmpChar = text.charAt(t);
            if (tmpChar == '\\') {// 判断是否是 "\n"
                if (t + 1 < text.length() && text.charAt(t + 1) == 'n') {
                    // 是"\n"字符串，但不是换行符.
                    t++;
                    lineTextList.add(tmpTextBuf.toString());
                    tmpTextBuf.delete(0, tmpTextBuf.length());
                } else {
                    tmpTextBuf.append(tmpChar);
                }
            } else {
                tmpTextBuf.append(tmpChar);
            }
        }

        // 最后一个
        if (tmpTextBuf.length() > 0) {
            lineTextList.add(tmpTextBuf.toString());
            tmpTextBuf.delete(0, tmpTextBuf.length());
        }

        return lineTextList;
    }

    private static void lineTextListAutoChangeLine(java.util.List lineTextList, String text, FontMetrics fontMetrics, Style style, double paintWidth) {
        double width = paintWidth - style.getPaddingLeft() - (style.getPaddingRight() == Style.DEFAULT_PADDING ? 0 : style.getPaddingRight()) - style.getBorderLeftWidth();

        StringBuffer lineTextBuf = new StringBuffer();
        int lineTextWidth = 0;

        StringBuffer wordBuf = new StringBuffer();
        int wordWidth = 0;
        int[] tmpWidth = new int[2];
        tmpWidth[0] = wordWidth;
        tmpWidth[1] = lineTextWidth;

        lineTextListDealWithText(text, lineTextList, width, tmpWidth, wordBuf, lineTextBuf, fontMetrics);

        // 最后处理
        if (tmpWidth[1] + tmpWidth[0] > width && lineTextBuf.length() > 0) {
            lineTextList.add(lineTextBuf.toString());
            lineTextList.add(wordBuf.toString());
        } else {
            lineTextBuf.append(wordBuf);
            lineTextList.add(lineTextBuf.toString());
        }
    }

    private static void lineTextListDealWithText(String text, java.util.List lineTextList, double width, int[] tmpWidth,
                                                 StringBuffer wordBuf, StringBuffer lineTextBuf, FontMetrics fontMetrics) {
        for (int t = 0; t < text.length(); t++) {
            if (t != 0 && isNumOrLetter(text.charAt(t)) && isNumOrLetter(text.charAt(t - 1))) {
                dealWithTextNumOrLetter(text, t, lineTextList, width, tmpWidth, wordBuf, lineTextBuf, fontMetrics);
            } else if (text.charAt(t) == '\n' || (text.charAt(t) == '\r' && t + 1 < text.length() - 1 && text.charAt(t + 1) != '\n')) {
                dealWithTextChangeLineSymbol(text, t, lineTextList, width, tmpWidth, wordBuf, lineTextBuf, fontMetrics);
            } else if (text.charAt(t) == '\\' && t + 1 < text.length() && text.charAt(t + 1) == 'n') {// 判断是否是 "\n"
                // 是"\n"字符串，但不是换行符,依然需要换行.
                dealWidthTextManualChangeLine(text, t, lineTextList, width, tmpWidth, wordBuf, lineTextBuf, fontMetrics);
                // 需要跳过后面的n
                t++;
            } else {
                if (text.charAt(t) == '\\' && t + 1 < text.length() && text.charAt(t + 1) == '\\') {// 判断是否是转义字符'\'
                    // _denny: 增加了转义字符'\\'用来表示\，使"\n"可以输入
                    t++;
                }
                if (tmpWidth[1] + tmpWidth[0] > width && lineTextBuf.length() > 0) {
                    if (isPunctuationAtLineHead(t, text)) {
                        for (int index = lineTextBuf.length(); index > 0; index--) {
                            char prec = lineTextBuf.charAt(index - 1);
                            lineTextBuf.deleteCharAt(index - 1);
                            if (!isPunctuation(prec)) {
                                break;
                            }
                        }
                    }
                    lineTextList.add(lineTextBuf.toString());
                    lineTextBuf.delete(0, lineTextBuf.length());
                    tmpWidth[1] = isPunctuationAtLineHead(t, text) ? dealWithPunctuationAtLinehead(t, fontMetrics, text, lineTextBuf) : 0;
                }
                lineTextBuf.append(wordBuf);
                tmpWidth[1] += tmpWidth[0];

                wordBuf.delete(0, wordBuf.length());
                tmpWidth[0] = 0;
                wordBuf.append(text.charAt(t));
                tmpWidth[0] = fontMetrics.charWidth(text.charAt(t));
            }
        }
    }

    /**
     * 标点符号是否在换行后的行首
     */
    private static boolean isPunctuationAtLineHead(int t, String text) {
        return t > 1 && com.fr.base.BaseUtils.isPunctuation(text.charAt(t - 1));
    }

    private static int dealWithPunctuationAtLinehead(int t, FontMetrics fontMetrics, String text, StringBuffer lineTextBuf) {
        if (t < 2) {
            return 0;
        }
        int lineWidth = 0;
        for (int index = t - 2; index >= 0; index--) {
            lineWidth += fontMetrics.charWidth(text.charAt(index));
            lineTextBuf.insert(0, text.charAt(index));
            if (!isPunctuation(text.charAt(index))) {
                break;
            }
        }
        return lineWidth;
    }

    private static void dealWithTextNumOrLetter(String text, int t, java.util.List lineTextList, double width, int[] tmpWidth,
                                                StringBuffer wordBuf, StringBuffer lineTextBuf, FontMetrics fontMetrics) {
        if (tmpWidth[0] + fontMetrics.charWidth(text.charAt(t)) > width) {
            if (tmpWidth[1] > 0) {
                lineTextList.add(lineTextBuf.toString());
                lineTextBuf.delete(0, lineTextBuf.length());
                tmpWidth[1] = 0;
            }

            lineTextList.add(wordBuf.toString());
            wordBuf.delete(0, wordBuf.length());
            tmpWidth[0] = 0;
        }

        wordBuf.append(text.charAt(t));
        tmpWidth[0] += fontMetrics.charWidth(text.charAt(t));
    }

    private static void dealWithTextChangeLineSymbol(String text, int t, java.util.List lineTextList, double width, int[] tmpWidth,
                                                     StringBuffer wordBuf, StringBuffer lineTextBuf, FontMetrics fontMetrics) {
        wordBuf.append('\n');
        if (tmpWidth[1] + tmpWidth[0] > width && lineTextBuf.length() > 0) {
            lineTextList.add(lineTextBuf.toString());
            lineTextList.add(wordBuf.toString());
        } else {
            lineTextBuf.append(wordBuf);
            lineTextList.add(lineTextBuf.toString());
        }
        lineTextBuf.delete(0, lineTextBuf.length());
        tmpWidth[1] = 0;
        wordBuf.delete(0, wordBuf.length());
        tmpWidth[0] = 0;
    }

    private static void dealWidthTextManualChangeLine(String text, int t, java.util.List lineTextList, double width, int[] tmpWidth,
                                                      StringBuffer wordBuf, StringBuffer lineTextBuf, FontMetrics fontMetrics) {
        t++;// 忽略'n'字符.
        wordBuf.append('\n');
        if (tmpWidth[1] + tmpWidth[0] > width && lineTextBuf.length() > 0) {
            lineTextList.add(lineTextBuf.toString());
            lineTextList.add(wordBuf.toString());
        } else {
            lineTextBuf.append(wordBuf);
            lineTextList.add(lineTextBuf.toString());
        }
        lineTextBuf.delete(0, lineTextBuf.length());
        tmpWidth[1] = 0;
        wordBuf.delete(0, wordBuf.length());
        tmpWidth[0] = 0;
    }


    /**
     * @param cuChar
     * @return
     * @see StableUtils
     * @deprecated
     */
    public static boolean isNum(char cuChar) {
        return StableUtils.isNum(cuChar);
    }


    /**
     * 判断字符是否为数字或字母
     *
     * @param curChar 被检查的字符
     * @return 是否为数字或字母
     */
    public static boolean isNumOrLetter(char curChar) {
        return GeneralUtils.isLetter(curChar) || StableUtils.isNum(curChar);
    }

    public static boolean isPunctuation(char c) {
        int type = Character.getType(c);
        return type == Character.OTHER_PUNCTUATION
                || type == Character.DASH_PUNCTUATION
                || type == Character.START_PUNCTUATION
                || type == Character.END_PUNCTUATION
                || type == Character.CONNECTOR_PUNCTUATION;
    }


    public static java.util.List getLineTextList(String text, Style style, Font font, double paintHeight, double paintWidth) {
        return getLineTextList(text, style, font, paintHeight, paintWidth, Constants.DEFAULT_PRINT_AND_EXPORT_RESOLUTION);
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

    /**
     * 返回边框宽
     *
     * @param borderType 边框类型
     * @return 宽度
     */
    public static int getBorderWidth(int borderType) {
        switch (borderType) {
            case Constants.LINE_NONE:
                return 0;
            case Constants.LINE_THIN:
                return 1;
            case Constants.LINE_DASH:
                return 1;
            case Constants.LINE_HAIR:
                return 1;
            case Constants.LINE_HAIR2:
                return 1;
            case Constants.LINE_THICK:
                return 3;
            case Constants.LINE_DOT:
                return 1;
            default:
                return 2;
        }
    }

    /**
     * 将边框转为对应的字符串描述
     *
     * @param borderStyle 边框线型
     * @return web端对应的边框
     */
    public static String border2Style(int borderStyle) {
        switch (borderStyle) {
            case Constants.LINE_NONE:
                return "none";
            case Constants.LINE_THIN:
                return "solid";
            case Constants.LINE_MEDIUM:
                return "solid";
            case Constants.LINE_THICK:
                return "solid";
            case Constants.LINE_DOUBLE:
                return "double";
            case Constants.LINE_DOT:
                return "double";
            case Constants.LINE_DASH_DOT:
                return "double";
            case Constants.LINE_DASH_DOT_DOT:
                return "dotted";
            default:
                return "dashed";
        }
    }

    /**
     * 克隆对象
     *
     * @param object 对象
     * @return 克隆出的对象
     * @throws CloneNotSupportedException 不被支持的克隆异常
     * @see StableUtils#cloneObject(Object)
     * @deprecated
     */
    public static Object cloneObject(Object object) throws CloneNotSupportedException {
        return StableUtils.cloneObject(object);
    }


    /**
     * 把java.util.Map转成一段JSONObject的String与上面方法对应
     *
     * @param map map对象
     * @return json对象
     * @throws JSONException json异常
     */
    public static JSONObject map2JSON(Map map) throws JSONException {
        JSONObject jo = new JSONObject();
        Iterator iter = map.keySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Object value = entry.getValue();
            if (value instanceof FArray) {
                JSONArray ja = new JSONArray();
                FArray array = (FArray) value;
                for (int i = 0; i < array.length(); i++) {
                    ja.put(array.elementAt(i));
                }
                value = ja;
            }
            jo.put((String) entry.getKey(), value);
        }
        return jo;
    }

    /**
     * 将一个对象转化成json样式的字符串
     *
     * @param o 待转化的对象
     * @return json样式的字符串
     * @throws JSONException json异常
     * @see JSONObject#valueToString(Object)
     * @deprecated
     */
    public static String jsonEncode(Object o) throws JSONException {
        return JSONObject.valueToString(o);
    }


    /**
     * 将一个字符串转化成JSON格式的对象
     *
     * @param str 要转化的字符串
     * @return JSON格式的对象
     * @throws JSONException json异常
     * @see JSONUtils#jsonDecode(String)
     * @deprecated
     */
    public static Object jsonDecode(String str) throws JSONException {
        return JSONUtils.jsonDecode(str);
    }

    /**
     * 把jsonArray转化成FArray，元素是JSONArray的也对应的转化为FArray
     *
     * @param ja json数组
     * @return 链表类
     * @see JSONUtils#JSONArrayToList(JSONArray)
     * @deprecated
     */
    public static FArray JSONArrayToFArray(JSONArray ja) {
        return new FArray(JSONUtils.JSONArrayToList(ja));
    }


    /**
     * 检查图片1和2是否相同.
     *
     * @param img1 图片1
     * @param img2 图片2
     * @return 图片1和2是否相同
     */
    public static boolean imageEquals(Image img1, Image img2) {
        if (img1 == img2) {
            return true;
        }

        if (img1 == null || img2 == null) {//null < not null
            return img1 == null && img2 == null;
        }

        //开始比较图片.
        byte[] buf1 = GeneralXMLTools.imageEncode(img1);
        byte[] buf2 = GeneralXMLTools.imageEncode(img2);
        if (buf1.length != buf2.length) {
            return false;
        }

        for (int i = 0; i < buf1.length; i++) {
            if (buf1[i] != buf2[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * 根据数据集名获取数据模型
     *
     * @param cal    算子
     * @param tdName 列表名
     * @return 数据模型
     */
    public static DataModel getDataModelFromTableDataName(Calculator cal, String tdName) {
        String tableDataName = tdName;
        //先看当前的报表中的私有数据源
        DataModel resultSet = SynchronizedLiveDataModelUtils.getSELiveDataModel4Share(cal, tableDataName);
        if (resultSet == null) {
            TableData td = DataCoreUtils.getTableData(cal, tableDataName);
            resultSet = td == null ? null : td.createDataModel(cal);
        }
        return resultSet;
    }


    /**
     * 写超链的内容  TODO 底层?
     *
     * @param g    超级链接的集合
     * @param repo 库
     * @return JSONArray转化的字符串
     * @throws JSONException JSON异常   J
     */
    public static String writeJSLinkContent(NameJavaScriptGroup g, Repository repo) throws JSONException {
        JSONArray ja = createJSLink(g, repo);

        return ja.toString();
    }

    /**
     * 把超链转化成json
     *
     * @param g    超链
     * @param repo 库
     * @return json对象
     * @throws JSONException JSON异常
     */
    public static JSONArray createJSLink(NameJavaScriptGroup g, Repository repo) throws JSONException {
        JSONArray ja = new JSONArray();

        for (int i = 0; i < g.size(); i++) {
            JSONObject jo = new JSONObject();
            // richer:这里不能再使用CodeUtils.javascriptEncode()了，否则会把"\"继续转义，形成多个"\\\"
            if (g.getNameHyperlink(i) != null && g.getNameHyperlink(i).getJavaScript() != null) {
                String name = g.getNameHyperlink(i).getName();
                JavaScript javaScript = g.getNameHyperlink(i).getJavaScript();
                javaScript.setLinkTitle(name);
                String dataString = g.getNameHyperlink(i).getJavaScript().createJS(repo);
                jo.put("data", dataString == null ? "" : dataString.replaceAll("\n", ""));
                jo.put("name", name);
                ja.put(jo);
            }
        }
        return ja;
    }
}