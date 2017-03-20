package com.fr.plugin.fractional;

import com.fr.base.present.Present;
import com.fr.design.beans.FurtherBasicBeanPane;
import com.fr.design.fun.impl.AbstractPresentKindProvider;
import com.fr.plugin.fractional.fun.FractionalPresent;
import com.fr.plugin.fractional.ui.FractionalPresentPane;

/**
 * Created by richie on 2017/3/20.
 */
public class FractionalPresentKind extends AbstractPresentKindProvider {

    @Override
    public FurtherBasicBeanPane<? extends Present> appearanceForPresent() {
        return new FractionalPresentPane();
    }

    @Override
    public String title() {
        return "分数线";
    }

    @Override
    public Class<? extends Present> kindOfPresent() {
        return FractionalPresent.class;
    }

    @Override
    public char mnemonic() {
        return 'F';
    }
}
