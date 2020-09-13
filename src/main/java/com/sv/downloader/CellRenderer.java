package com.sv.downloader;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public abstract class CellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
        String name = table.getValueAt(row, ResourceDownLoader.COLS.NAME.getIdx()).toString();
        boolean isFailedOrCancelled = name.startsWith(ResourceDownLoader.FAILED_MSG) ||
                name.startsWith(ResourceDownLoader.CANCELLED_MSG);
        c.setForeground(isFailedOrCancelled ? Color.RED : Color.BLACK);
        setToolTipText(table.getValueAt(row, ResourceDownLoader.COLS.PATH.getIdx()).toString());
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

}
