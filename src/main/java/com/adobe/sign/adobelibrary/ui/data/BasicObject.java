package com.adobe.sign.adobelibrary.ui.data;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Calvin on 4/26/2017
 * for the AdobeLibrary project.

 Copyright (C) 2017 Calvin Lacy-Hill

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
@Getter
@Setter
public class BasicObject {

    private UUID uniqueId;

    private StringProperty name = new SimpleStringProperty();
    private StringProperty documentId = new SimpleStringProperty();
    private ObjectType objectType;
    private ObservableList<BasicObject> items = FXCollections.observableArrayList(new ArrayList<BasicObject>());

    public BasicObject(String name, String documentId, ObjectType objectType) {
        this.uniqueId = UUID.randomUUID();
        this.name.setValue(name);
        this.documentId.setValue(documentId);
        this.objectType = objectType;
    }

    public void createAndAddChild(String name, String documentId, ObjectType type) {
        if ((objectType == ObjectType.FOLDER && type == ObjectType.FOLDER) || (objectType == ObjectType.FOLDER && type == ObjectType.DOCUMENT)) {
            items.add(new BasicObject(name, documentId, type));
        }
    }
}
