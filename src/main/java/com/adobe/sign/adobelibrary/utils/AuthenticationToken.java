package com.adobe.sign.adobelibrary.utils;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AuthenticationToken implements Serializable {

    private static final long serialVersionUID = 1L;

    private String expires_in;
    private String token_type;
    private String refresh_token;
    private String access_token;

    @Override
    public String toString() {
        return "ClassPojo [expires_in = " + expires_in + ", token_type = " + token_type + ", refresh_token = " + refresh_token + ", access_token = " + access_token + "]";
    }
}