package com.adobe.sign.adobelibrary.ui.cells;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by Calvin on 4/24/2017
 * for the AdobeLibrary project.
 */

@Getter
@Setter
public class RecipientTableCell implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uuid;
    private String role = "";
    private String email = "";

    public RecipientTableCell(String uuid, String role, String email) {
        this.uuid = uuid;
        this.role = role;
        this.email = email;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RecipientTableCell)) {
            return false;
        }

        RecipientTableCell that = (RecipientTableCell) other;

        return this.uuid.equals(that.uuid) && this.role.equals(that.role) && this.email.equals(that.email);
    }
}
