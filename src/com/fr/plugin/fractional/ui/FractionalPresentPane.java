package com.fr.plugin.fractional.ui;

import com.fr.design.beans.FurtherBasicBeanPane;
import com.fr.design.gui.icombobox.UIDictionaryComboBox;
import com.fr.design.gui.ilable.UILabel;
import com.fr.design.gui.ispinner.UISpinner;
import com.fr.design.layout.TableLayout;
import com.fr.design.layout.TableLayoutHelper;
import com.fr.plugin.fractional.fun.FractionalAttr;
import com.fr.plugin.fractional.fun.FractionalPresent;
import com.fr.plugin.fractional.fun.Position;
import com.fr.plugin.fractional.fun.Weight;

import javax.swing.*;
import java.awt.*;

/**
 * Created by richie on 2017/3/20.
 */
public class FractionalPresentPane extends FurtherBasicBeanPane<FractionalPresent> {

    private UIDictionaryComboBox<Weight> weightComboBox;
    private UIDictionaryComboBox<Position> positionComboBox;
    private UISpinner hGapSpinner;
    private UISpinner vGapSpinner;


    public FractionalPresentPane() {
        setLayout(new BorderLayout());
        weightComboBox = new UIDictionaryComboBox<Weight>(Weight.values(), new String[]{"无", "细", "中等", "粗"});
        positionComboBox = new UIDictionaryComboBox<Position>(Position.values(), new String[]{"下", "上"});
        hGapSpinner = new UISpinner(0, 100, 1);
        vGapSpinner = new UISpinner(0.5, 100, 0.5);
        double p = TableLayout.PREFERRED;
        double f = TableLayout.FILL;
        double[] rowSize = {p , p, p, p};
        double[] columnSize = {p, f};
        JPanel panel = TableLayoutHelper.createTableLayoutPane(new Component[][]{
                {new UILabel("线条粗细:"), weightComboBox},
                {new UILabel("线条位置:"), positionComboBox},
                {new UILabel("水平间隙:"), hGapSpinner},
                {new UILabel("垂直间隙:"), vGapSpinner}
        }, rowSize, columnSize);
        add(panel, BorderLayout.CENTER);
    }

    @Override
    public String title4PopupWindow() {
        return "分数线";
    }

    @Override
    public void populateBean(FractionalPresent ob) {
        if (ob != null) {
            weightComboBox.setSelectedItem(ob.getAttr().getWeight());
            positionComboBox.setSelectedItem(ob.getAttr().getPosition());
            hGapSpinner.setValue(ob.getAttr().getHgap());
            vGapSpinner.setValue(ob.getAttr().getVgap());
        }
    }

    @Override
    public FractionalPresent updateBean() {
        FractionalAttr attr = new FractionalAttr();
        attr.setWeight(weightComboBox.getSelectedItem());
        attr.setPosition(positionComboBox.getSelectedItem());
        attr.setHgap(hGapSpinner.getValue());
        attr.setVgap(vGapSpinner.getValue());

        FractionalPresent present = new FractionalPresent();
        present.setAttr(attr);
        return present;
    }

    @Override
    public boolean accept(Object ob) {
        return ob instanceof FractionalPresent;
    }


    @Override
    public void reset() {

    }
}
