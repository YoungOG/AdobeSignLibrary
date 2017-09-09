package com.adobe.sign.adobelibrary.ui.cells;

import com.adobe.sign.adobelibrary.AdobeLibrary;
import com.adobe.sign.api.AgreementsApi;
import com.adobe.sign.model.agreements.*;
import com.adobe.sign.utils.ApiException;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Getter;
import lombok.Setter;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by Calvin on 4/20/2017
 * for the AdobeLibrary project.
 */
@Getter
@Setter
public class AgreementTableCell {

    private AgreementInfo agreementInfo;
    private SimpleStringProperty docUrl = new SimpleStringProperty();
    private SimpleStringProperty name = new SimpleStringProperty();
    private SimpleStringProperty message = new SimpleStringProperty();
    private SimpleStringProperty status = new SimpleStringProperty();
    private SimpleBooleanProperty visible = new SimpleBooleanProperty(true);

    public AgreementTableCell(AgreementInfo agreementInfo) {
        this.agreementInfo = agreementInfo;

        new Thread(() -> {
            AgreementsApi api = new AgreementsApi();

            MultivaluedMap headers = new MultivaluedMapImpl();
            headers.put(AdobeLibrary.ACCESS_TOKEN_HEADER, AdobeLibrary.getInstance().getAuthenticationToken().getAccess_token());

            try {
                AgreementDocuments documents = api.getAllDocuments(headers, agreementInfo.getAgreementId(), agreementInfo.getLatestVersionId(), null, null);

                if (documents.getDocuments() != null && documents.getDocuments().size() > 0) {
                    if (documents.getDocuments().get(0) != null) {
                        Document document = documents.getDocuments().get(0);

                        DocumentImageUrl documentImageUrl = api.getDocumentImageUrls(headers, agreementInfo.getAgreementId(), document.getDocumentId(), agreementInfo.getLatestVersionId(), null, "FIXED_WIDTH_250px", false, 1, 1);

                        if (documentImageUrl.getImageUrls() != null && documentImageUrl.getImageUrls().size() > 0) {
                            if (documentImageUrl.getImageUrls().get(0) != null && documentImageUrl.getImageUrls().get(0).getImagesAvailable()) {
                                ImageUrl imageUrl = documentImageUrl.getImageUrls().get(0);

                                if (imageUrl.getUrls() != null && imageUrl.getUrls().size() > 0) {
                                    if (imageUrl.getUrls().get(0) != null) {

                                        docUrl.setValue(imageUrl.getUrls().get(0));
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (ApiException e) {
                e.printStackTrace();
            }

            name.setValue(agreementInfo.getName());
            message.setValue(agreementInfo.getMessage());
            status.setValue(agreementInfo.getStatus().toString());
        }).start();
    }
}
