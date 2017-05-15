package com.fr.plugin.fractional.fun;

import com.fr.base.present.AbstractPresent;
import com.fr.general.GeneralUtils;
import com.fr.general.xml.GeneralXMLTools;
import com.fr.script.Calculator;
import com.fr.stable.ColumnRow;
import com.fr.stable.StringUtils;
import com.fr.stable.xml.XMLPrintWriter;
import com.fr.stable.xml.XMLableReader;

/**
 * Created by richie on 2017/3/20.
 */
public class FractionalPresent extends AbstractPresent {

    private FractionalAttr attr;

    public FractionalPresent() {
        attr = new FractionalAttr();
    }

    public FractionalAttr getAttr() {
        return attr;
    }

    public void setAttr(FractionalAttr attr) {
        this.attr = attr;
    }

    @Override
    public void readXML(XMLableReader reader) {
        if (reader.isChildNode()) {
            String tagName = reader.getTagName();
            if (FractionalAttr.XML_TAG.equals(tagName)) {
                attr = (FractionalAttr) GeneralXMLTools.readXMLable(reader);
            }
        }
     }

    @Override
    public void writeXML(XMLPrintWriter writer) {
        if (attr != null) {
            GeneralXMLTools.writeXMLable(writer, attr, FractionalAttr.XML_TAG);
        }
    }

    @Override
    public Object present(Object value, Calculator calculator, ColumnRow cr) {
        String text = GeneralUtils.objectToString(value);
        if (StringUtils.isBlank(text)) {
            text = FractionalAttr.EMPTY_STRING_HOLDER;
        }
        return new FractionalPainter(attr, text);
    }
}
