package com.fr.plugin.fractional.fun;

import com.fr.stable.xml.XMLPrintWriter;
import com.fr.stable.xml.XMLable;
import com.fr.stable.xml.XMLableReader;

/**
 * Created by richie on 2017/3/20.
 */
public class FractionalAttr implements XMLable {

    public static final String XML_TAG = "FractionalAttr";

    private Weight weight = Weight.MEDIUM;
    private Position position = Position.BOTTOM;


    public FractionalAttr() {

    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public void setWeight(Weight weight) {
        this.weight = weight;
    }

    public Weight getWeight() {
        return weight;
    }

    public Position getPosition() {
        return position;
    }

    @Override
    public void readXML(XMLableReader reader) {
        if (reader.isChildNode()) {
            String tagName = reader.getTagName();
            if ("Attr".equals(tagName)) {
                weight = Weight.parse(reader.getAttrAsInt("weight", 1));
                position = Position.parse(reader.getAttrAsInt("position", 0));
            }
        }
    }

    @Override
    public void writeXML(XMLPrintWriter writer) {
        writer.startTAG("Attr");
        writer.attr("weight", weight.toInt());
        writer.attr("position", position.toInt());
        writer.end();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        FractionalAttr cloned = (FractionalAttr) super.clone();
        cloned.weight = weight;
        cloned.position = position;
        return cloned;
    }
}
