package com.smartstream.morph.rcp;

import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.widgets.Display;

public class TestForm extends ApplicationWindow {

    public TestForm() {
        super(null);
        init();
    }

    private void init() {

    }

    public static void main(String[] args) {
        TestForm wwin = new TestForm();
        wwin.setBlockOnOpen(true);
        wwin.open();
        Display.getCurrent().dispose();
    }

}
