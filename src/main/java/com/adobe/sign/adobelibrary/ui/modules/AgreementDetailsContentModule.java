package com.adobe.sign.adobelibrary.ui.modules;

import com.adobe.sign.adobelibrary.AdobeLibrary;
import com.adobe.sign.adobelibrary.utils.DateUtil;
import com.adobe.sign.api.AgreementsApi;
import com.adobe.sign.model.agreements.*;
import com.adobe.sign.utils.ApiException;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Pagination;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;
import lombok.Setter;

import javax.ws.rs.core.MultivaluedMap;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static com.adobe.sign.adobelibrary.utils.SafeThread.runOnFXApplicationThread;

/**
 * Created by Calvin on 4/21/2017
 * for the AdobeLibrary project.
 */

@Getter
@Setter
public class AgreementDetailsContentModule implements Initializable {

    private AdobeLibrary main = AdobeLibrary.getInstance();

    private static AgreementInfo agreementInfo;

    //Labels
    @FXML
    private Label agreementNameLabel;
    @FXML
    private Label fromLabel;
    @FXML
    private Label toLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label messageLabel;

    //View Tab
    @FXML
    private AnchorPane viewAnchorPane;
    @FXML
    private Pagination viewContentPagination;

    //History Tab
    @FXML
    private ListView<String> historyListView;

    private ArrayList<ImageView> imageViews = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        agreementNameLabel.setText(agreementInfo.getName());
        statusLabel.setText(agreementInfo.getStatus().toString());
        messageLabel.setText(agreementInfo.getMessage());

        List<String> toRecipients = new ArrayList<>();

        for (ParticipantSetInfo participantSetInfo : agreementInfo.getParticipantSetInfos()) {
            for (ParticipantSetInfo.RolesEnum rolesEnum : participantSetInfo.getRoles()) {
                if (rolesEnum == ParticipantSetInfo.RolesEnum.SENDER) {
                    for (ParticipantInfo participantInfo : participantSetInfo.getParticipantSetMemberInfos()) {
                        fromLabel.setText(participantInfo.getName());
                    }
                } else {
                    for (ParticipantInfo participantInfo : participantSetInfo.getParticipantSetMemberInfos()) {
                        toRecipients.add(participantInfo.getName());
                    }
                }
            }
        }

        toLabel.setText(toRecipients.toString().replace("[", "").replace("]", ""));

        for (DocumentHistoryEvent historyEvent : agreementInfo.getEvents()) {
            if (historyEvent.getType() == DocumentHistoryEvent.TypeEnum.CREATED) {
                dateLabel.setText(DateUtil.getProperDate(historyEvent.getDate()));
            }

            String historyString = historyEvent.getDescription() + " " + historyEvent.getActingUserEmail() + "";

            switch (historyEvent.getType()) {
                case ESIGNED:
                    historyString += " Signature Date: " + DateUtil.getProperDate(historyEvent.getDate()) + (historyEvent.getComment() != null ? " Source: " + historyEvent.getDeviceLocation() : "");
                    break;
                case APPROVED:
                    historyString += " Approval Date: " + DateUtil.getProperDate(historyEvent.getDate()) + (historyEvent.getComment() != null ? " Source: " + historyEvent.getDeviceLocation() : "");
                    break;
                default:
                    historyString += " " + DateUtil.getProperDate(historyEvent.getDate());
            }

            historyListView.getItems().add(historyString);
        }

        viewAnchorPane.getChildren().remove(viewContentPagination);

        new Thread(() -> {
            AgreementsApi api = new AgreementsApi();

            MultivaluedMap headers = new MultivaluedMapImpl();
            headers.put(AdobeLibrary.ACCESS_TOKEN_HEADER, main.getAuthenticationToken().getAccess_token());

            try {
                AgreementDocuments agreementDocuments = api.getAllDocuments(headers, agreementInfo.getAgreementId(), agreementInfo.getLatestVersionId(), null, null);

                if (agreementDocuments.getDocuments() != null && agreementDocuments.getDocuments().size() > 0) {
                    DocumentImageUrls documentImageUrls = api.getCombinedDocumentImageUrls(headers, agreementInfo.getAgreementId(), agreementInfo.getLatestVersionId(), null, "ZOOM_100_PERCENT", true, false);

                    try {
                        if (documentImageUrls.getDocumentsImageUrls().size() > 0) {
                            for (DocumentImageUrl documentImageUrl : documentImageUrls.getDocumentsImageUrls()) {
                                if (documentImageUrl.getImageUrls().size() > 0) {
                                    for (ImageUrl imageUrl : documentImageUrl.getImageUrls()) {
                                        if (imageUrl.getImagesAvailable()) {
                                            if (imageUrl.getUrls().size() > 0) {
                                                for (String url : imageUrl.getUrls()) {
                                                    runOnFXApplicationThread(() -> {
                                                        ImageView imageView = new ImageView(url);
                                                        imageViews.add(imageView);
                                                    });
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (NullPointerException e) {
                        System.out.println("Failed to load image views.");
                    }
                }

                runOnFXApplicationThread(() -> {
                    if (imageViews.size() > 0) {
                        viewAnchorPane.getChildren().clear();
                        viewContentPagination.setMaxPageIndicatorCount(imageViews.size());
                        viewContentPagination.setPageCount(imageViews.size());
                        viewContentPagination.setPageFactory((pageNumber) -> {
                            ScrollPane scrollPane = new ScrollPane(imageViews.get(pageNumber));
                            scrollPane.setPannable(true);
                            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                            return scrollPane;
                        });

                        viewAnchorPane.getChildren().add(viewContentPagination);
                    }
                });
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void setAgreementInfo(AgreementInfo agreementInfo) {
        AgreementDetailsContentModule.agreementInfo = agreementInfo;
    }
}
