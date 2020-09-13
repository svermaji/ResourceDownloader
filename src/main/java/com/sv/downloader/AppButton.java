package com.sv.downloader;

import javax.swing.*;

public class AppButton extends JButton {

    AppButton(String text, char mnemonic) {
        setText(text);
        setMnemonic(mnemonic);
    }
}
