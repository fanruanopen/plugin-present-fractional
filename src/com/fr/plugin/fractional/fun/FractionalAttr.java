package com.fr.plugin.fractional.fun;

import com.fr.stable.xml.XMLPrintWriter;
import com.fr.stable.xml.XMLable;
import com.fr.stable.xml.XMLableReader;

/**
 * Created by richie on 2017/3/20.
 */
public class FractionalAttr implements XMLable {

    public static final String XML_TAG = "FractionalAttr";

    public static final String EMPTY_STRING_HOLDER = "__EMPTY__";

    private Weight weight = Weight.MEDIUM;
    private Position position = Position.BOTTOM;
    private double hgap = 5;
    private double vgap = 0.5;
    private boolean alwaysShow = true;


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

    public double getHgap() {
        return hgap;
    }

    public void setHgap(double hgap) {
        this.hgap = hgap;
    }

    public double getVgap() {
        return vgap;
    }

    public void setVgap(double vgap) {
        this.vgap = vgap;
    }

    public boolean isAlwaysShow() {
        return alwaysShow;
    }

    public void setAlwaysShow(boolean alwaysShow) {
        this.alwaysShow = alwaysShow;
    }

    @Override
    public void readXML(XMLableReader reader) {
        if (reader.isChildNode()) {
            String tagName = reader.getTagName();
            if ("Attr".equals(tagName)) {
                weight = Weight.parse(reader.getAttrAsInt("weight", 1));
                position = Position.parse(reader.getAttrAsInt("position", 0));
                hgap = reader.getAttrAsDouble("hgap", 5);
                vgap = reader.getAttrAsDouble("vgap", 0.5);
                alwaysShow = reader.getAttrAsBoolean("alwaysShow", true);
            }
        }
    }

    @Override
    public void writeXML(XMLPrintWriter writer) {
        writer.startTAG("Attr");
        writer.attr("weight", weight.toInt());
        writer.attr("position", position.toInt());
        writer.attr("hgap", hgap);
        writer.attr("vgap", vgap);
        writer.attr("alwaysShow", alwaysShow);
        writer.end();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        FractionalAttr cloned = (FractionalAttr) super.clone();
        cloned.weight = weight;
        cloned.position = position;
        cloned.hgap = hgap;
        cloned.vgap = vgap;
        cloned.alwaysShow = alwaysShow;
        return cloned;
    }
}
