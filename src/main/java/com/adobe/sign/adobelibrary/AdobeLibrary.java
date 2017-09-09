package com.adobe.sign.adobelibrary;

import com.adobe.sign.adobelibrary.utils.AuthenticationToken;
import com.adobe.sign.adobelibrary.utils.AuthorizationUtils;
import com.adobe.sign.adobelibrary.utils.FileUtils;
import com.adobe.sign.api.OAuthApi;
import com.adobe.sign.model.oAuth.AccessTokenRefreshRequest;
import com.adobe.sign.model.oAuth.AccessTokenRefreshResponse;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Calvin on 4/18/2017
 * for the AdobeLibrary project.
 */
public class AdobeLibrary extends Application {

    @Getter
    public static final String ACCESS_TOKEN_HEADER = "Access-Token";
    @Getter
    public static final String X_API_USER_KEY = "x-api-user";
    @Getter
    private static AdobeLibrary instance;
    @Getter
    private static OAuthApi authApi;
    @Getter
    @Setter
    private AuthenticationToken authenticationToken;
    @Getter
    @Setter
    private boolean authorized = false;
    @Getter
    @Setter
    private Stage window;
    @Getter
    @Setter
    private Scene mainScene;

    public static void main(String[] args) {
        System.out.println("Application started");

        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;

        authApi = new OAuthApi();

        window = primaryStage;

        //Check Authentication
        try {
            Object obj = FileUtils.readFile("AuthenticationToken.yml");
            authenticationToken = (AuthenticationToken) obj;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }

        if (authenticationToken != null) {
            if (!AuthorizationUtils.isExpired(Long.parseLong(authenticationToken.getExpires_in()))) {
                System.out.println("AuthenticationToken found and is valid:\n" + authenticationToken.toString());
                authorized = true;

                setupMainWindow();
            } else {
                AccessTokenRefreshResponse response = getAuthApi().refreshAccessToken(new AccessTokenRefreshRequest(AuthorizationUtils.getClientID(), AuthorizationUtils.getClientSecret(), authenticationToken.getRefresh_token(), "refresh_token"));

                System.out.println("Response: " + response.toString());
                System.out.println("AuthenticationToken found,");

                if (response.getAccessToken() != null && !response.getAccessToken().isEmpty()) {
                    authorized = true;
                    authenticationToken = new AuthenticationToken();
                    authenticationToken.setAccess_token(response.getAccessToken());
                    authenticationToken.setExpires_in(response.getExpiresIn() + "");
                    authenticationToken.setRefresh_token(authenticationToken.getRefresh_token());
                    authenticationToken.setToken_type(response.getTokenType());

                    setupMainWindow();
                } else {
                    System.out.println("AuthenticationToken AccessToken expired, requesting a new token.");
                    authorized = AuthorizationUtils.requestAuth(window);
                }
            }
        } else {
            System.out.println("AuthenticationToken expired, requesting a new token.");
            authorized = AuthorizationUtils.requestAuth(window);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setupMainWindow() throws Exception {
        Parent parent = FXMLLoader.load(getClass().getClassLoader().getResource("main.fxml"));
        mainScene = new Scene(parent);

        window.setScene(mainScene);
        window.setTitle("Adobe Library");
        window.setMinHeight(700);
        window.setMinWidth(1250);

        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application_icon.png");
            window.getIcons().add(new Image(inputStream));
        } catch (Exception ignored) {
            System.out.println("Failed to load application icon: " + ignored.getMessage());
        }

        window.show();
    }
}
