package com.adobe.sign.adobelibrary.ui.cells;

import javafx.scene.control.ListCell;

/**
 * Created by Calvin on 4/25/2017
 * for the AdobeLibrary project.
 */
public class FileCell extends ListCell<String> {

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (item == null || empty) {
            setText(null);
        } else {
            setText(item);
        }
    }
}
