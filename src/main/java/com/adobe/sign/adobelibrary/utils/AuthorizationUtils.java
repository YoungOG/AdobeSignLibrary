package com.adobe.sign.adobelibrary.utils;

import java.io.IOException;
import java.util.ArrayList;

import com.adobe.sign.adobelibrary.AdobeLibrary;
import com.google.gson.Gson;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import com.adobe.sign.model.oAuth.AuthorizationRequest;
import com.adobe.sign.model.oAuth.Scope;
import com.adobe.sign.utils.ApiException;
import lombok.Getter;

public class AuthorizationUtils {

	//TODO: Replace <> with required data.
    @Getter public static AuthenticationToken token;
    private static Scene scene;
	private static String authorizationUrl = "NO URL";
	private static String apiUserEmail = "<email>";
    private static String appID = "<appId>";
	private static String state = "myState";
    @Getter private static String clientID = "<clientId>";
	@Getter private static String clientSecret = "<clientSecret>";
    private static String redirectURL = "https://www.adobe.com";
    @Getter private static String returnType = "code";

	public static boolean setAuthorizationData(String urlContainingAuthCode) {
		String[] format = urlContainingAuthCode.split("\\?");
		String[] data = format[1].split("\\&");
		String json;
		String apiAddress = "http://api.echosign.com/oauth/token";
		String authTokenRequest = "" + 
			"code=" + data[0].substring(5) + "&" +
			"client_id=" + clientID + "&" +
			"client_secret=" + clientSecret + "&" +
			"redirect_uri=" + redirectURL + "&" +
			"grant_type=authorization_code";

		try {
			json = RESTfulUtils.getAuthResponse(apiAddress, authTokenRequest);
			System.out.println("API token address: \n" + apiAddress);
			System.out.println("API request: \n" + authTokenRequest);
			Gson gson = new Gson();
			token = gson.fromJson(json, AuthenticationToken.class);
			token.setExpires_in("" + (Long.parseLong(token.getExpires_in()) + getTime()));
			AdobeLibrary.getInstance().setAuthenticationToken(token);
			AdobeLibrary.getInstance().setAuthorized(true);
			System.out.println(token.getExpires_in() + getTime() + "");
			System.out.println("Authorization Token Saved: \n" + token.toString());

            try {
                FileUtils.writeToFile(token, "AuthenticationToken.yml");
            } catch (IOException e) {
                System.out.println(e.getMessage());
                return false;
            }
		} catch (IOException e) {
		    System.out.println(e.getMessage());
		    e.printStackTrace();
		}

		return true;
	}
	@SuppressWarnings("unchecked")
	public static boolean requestAuth(Stage stage) {
		ArrayList<Scope> myScopes = new ArrayList<>();
		
		//Provide the scope type and modifier
    	myScopes.add(new Scope("user_read", "self"));
    	myScopes.add(new Scope("user_write", "self"));
    	myScopes.add(new Scope("user_login", "self"));
    	myScopes.add(new Scope("agreement_read", "self"));
    	myScopes.add(new Scope("agreement_write", "self"));
    	myScopes.add(new Scope("agreement_send", "self"));
    	myScopes.add(new Scope("library_read", "self"));
		myScopes.add(new Scope("library_write", "self"));

    	//Create authorization request for AuthoricationUrl;
    	AuthorizationRequest authorizationInfo = new AuthorizationRequest(appID, redirectURL, myScopes, state, returnType);    
    	try {
    		authorizationUrl = AdobeLibrary.getAuthApi().getAuthorizationUrl(authorizationInfo);
    		System.out.println("Authorization URL created: \n" + authorizationUrl);
    	} catch (ApiException e) {
			System.out.println(e.getMessage());
    		System.out.println("Failed to create authorization URL!");
    		return false;
    	}

        // create browser for user to supply credentials.
        if (authorizationUrl != null) {
            StackPane stackPane = new StackPane();
            ProgressIndicator progressIndicator = new ProgressIndicator();
			WebView webView = new WebView();
			WebEngine webEngine = webView.getEngine();

            stackPane.getChildren().add(progressIndicator);

			webEngine.getLoadWorker().stateProperty().addListener(
					(ov, oldState, newState) -> {
						if (newState == Worker.State.SUCCEEDED) {
                            stackPane.getChildren().remove(progressIndicator);
                            webView.setVisible(true);

							if (webEngine.getLocation().contains("web_access_point=")) {
                                AuthorizationUtils.setAuthorizationData(webEngine.getLocation());

                                if (AdobeLibrary.getInstance().isAuthorized()) {
                                    try {
                                        AdobeLibrary.getInstance().setupMainWindow();
                                    } catch (Exception e) {
                                        System.out.println("Failed to create main window.");
                                        e.printStackTrace();
                                    }
                                } else {
                                    System.out.println("Failed to create main window. (User not authorized)");
                                }
							}
						}

						if (oldState == Worker.State.SUCCEEDED) {
						    stackPane.getChildren().add(progressIndicator);
						    webView.setVisible(false);
                        }
					});

			progressIndicator.setMaxSize(100.0, 100.0);

            webView.setVisible(false);
            stackPane.getChildren().add(webView);

			webEngine.load(authorizationUrl);

            scene = new Scene(stackPane, webView.getWidth(), webView.getHeight(), Color.web("#666970"));
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } else {
        	System.out.println("authorizationUrl is null");
    		return false;
        }

        return true;
	}

	private static long getTime(){
		return System.currentTimeMillis() / 1000L;
	}

	public static boolean isExpired(long expireTime) {
		System.out.println("ExpireTime" + expireTime);
		System.out.println("Time" + (getTime() - 500L));

		return expireTime <= (getTime() - 500L);
	}
}