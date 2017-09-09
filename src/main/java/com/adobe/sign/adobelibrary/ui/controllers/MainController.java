package com.adobe.sign.adobelibrary.ui.controllers;

import com.adobe.sign.adobelibrary.AdobeLibrary;
import com.adobe.sign.adobelibrary.ui.ViewState;
import com.adobe.sign.adobelibrary.ui.cells.AgreementTableCell;
import com.adobe.sign.adobelibrary.ui.data.BasicObject;
import com.adobe.sign.adobelibrary.ui.modules.AgreementDetailsContentModule;
import com.adobe.sign.api.AgreementsApi;
import com.adobe.sign.api.LibraryDocumentsApi;
import com.adobe.sign.model.agreements.FileInfo;
import com.adobe.sign.model.agreements.UserAgreement;
import com.adobe.sign.model.agreements.UserAgreements;
import com.adobe.sign.model.libraryDocuments.LibraryCreationInfo;
import com.adobe.sign.model.libraryDocuments.LibraryDocumentCreationInfo;
import com.adobe.sign.model.transientDocuments.TransientDocumentResponse;
import com.adobe.sign.utils.ApiException;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.*;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.paint.Paint;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

import javax.ws.rs.core.MultivaluedMap;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.List;

import static com.adobe.sign.adobelibrary.utils.SafeThread.runOnFXApplicationThread;

public class MainController implements Initializable {

    private static MainController instance;

    private AdobeLibrary main = AdobeLibrary.getInstance();

    private ViewState viewState = ViewState.AGREEMENTS_LIST;

    //MenuBar, Menus, and MenuItems
    @FXML
    private MenuBar menuBar;
    @FXML
    private Menu fileMenu;
    @FXML
    private MenuItem closeMenuItem;
    @FXML
    private Menu editMenu;
    @FXML
    private MenuItem deleteMenuItem;
    @FXML
    private Menu viewMenu;
    @FXML
    private MenuItem aboutMenuItem;

    //SearchTextField, SearchButton, TopRegion
    @FXML//
    private TextField searchTextField;
    @FXML
    private HBox topHBox;
    @FXML
    private Label searchLabel;

    //ComposeButton, DocumentsButton
    @FXML
    private Button composeButton;
    @FXML
    private Button agreementsButton;
    @FXML
    private Button documentsButton;

    //ContentVBox, TableView
    @FXML
    private VBox contentVBox;
    @FXML
    private TableView<AgreementTableCell> agreementsTableView;
    @FXML
    private TableColumn<AgreementTableCell, String> docTableColumn;
    @FXML
    private TableColumn<AgreementTableCell, String> nameTableColumn;
    @FXML
    private TableColumn<AgreementTableCell, String> messageTableColumn;
    @FXML
    private TableColumn<AgreementTableCell, String> statusTableColumn;

    private ObservableList<AgreementTableCell> agreementTableCells = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;

        //Setup MenuBar
        setupMenuBar();

        //Setup Agreements TableView
        setupAgreementsTableView();

        //Setup SidePanel Buttons
        setupSidePanelButtons();
    }

    private void setupMenuBar() {
        //Close application
        closeMenuItem.setOnAction(event -> Platform.exit());
        aboutMenuItem.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("About");
            alert.setHeaderText(null);
            alert.setContentText(null);

            VBox vBox1 = new VBox();
            HBox hBox1 = new HBox();
            Label lbl1 = new Label("AdobeLibrary version: 0.0.32-a\nCreated by: Calvin Lacy\n");
            Label lbl2 = new Label("Contact Email: ");
            Hyperlink link1 = new Hyperlink("superiorpvpmc@gmail.com");
            link1.setTranslateY(-3.5);

            link1.setOnAction(event1 -> {
                try {
                    Desktop.getDesktop().browse(new URI("mailto:" + link1.getText()));
                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace();
                }
            });

            hBox1.getChildren().addAll(lbl2, link1);

            HBox hBox2 = new HBox();
            Label lbl3 = new Label("GitHub: ");

            Hyperlink link2 = new Hyperlink("www.github.com/YoungOG");
            link2.setTranslateY(-3.5);

            link2.setOnAction(event1 -> {
                try {
                    Desktop.getDesktop().browse(new URI(link2.getText()));
                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace();
                }
            });

            hBox2.setTranslateY(-6.5);

            hBox2.getChildren().addAll(lbl3, link2);

            vBox1.getChildren().addAll(lbl1, hBox1, hBox2);

            alert.getDialogPane().contentProperty().set(vBox1);

            alert.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.INFO_CIRCLE, "30px"));
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().addAll(main.getWindow().getIcons());
            alert.show();
        });

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spacer.setMinSize(150, 1);
        topHBox.getChildren().add(spacer);

        searchLabel.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.SEARCH, "18px"));
    }

    private void setupAgreementsTableView() {
        //Setup TableRow factor for the TableView.
        agreementsTableView.setRowFactory(param -> {
            TableRow<AgreementTableCell> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();

            MenuItem detailsMenuItem = new MenuItem("View Details");
            detailsMenuItem.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.SERVER, "18px"));
            detailsMenuItem.setOnAction(event -> showAgreementDetails(row.getItem()));

            MenuItem removeMenuItem = new MenuItem("Remove");
            removeMenuItem.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.REMOVE, "18px"));
            removeMenuItem.setOnAction(event -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirmation");
                alert.setHeaderText(null);
                alert.setContentText("Are you sure you want to delete this agreement?");
                alert.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.QUESTION_CIRCLE, "30px"));
                Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                stage.getIcons().addAll(main.getWindow().getIcons());

                if (alert.showAndWait().get() == ButtonType.OK) {
                    agreementsTableView.getItems().remove(row.getItem());

                    try {
                        AgreementsApi agreementsApi = new AgreementsApi();

                        MultivaluedMap headers = new MultivaluedMapImpl();
                        headers.put(AdobeLibrary.ACCESS_TOKEN_HEADER, main.getAuthenticationToken().getAccess_token());

                        agreementsApi.deleteAgreement(headers, row.getItem().getAgreementInfo().getAgreementId());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            contextMenu.getItems().addAll(detailsMenuItem, removeMenuItem);

            row.contextMenuProperty().bind(Bindings.when(Bindings.isNotNull(row.itemProperty()))
                .then(contextMenu)
                .otherwise((ContextMenu) null));

            //Setup double-click listener
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    showAgreementDetails(row.getItem());
                }
            });

            return row;
        });

        docTableColumn.setSortable(false);
        docTableColumn.setCellValueFactory(cellData -> cellData.getValue().getDocUrl());
        docTableColumn.setCellFactory(cell -> new TableCell<AgreementTableCell, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                setAlignment(Pos.CENTER_LEFT);

                if (item == null || empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (getTableRow().getItem() != null) {
                        setText(null);

                        try {
                            new Thread(() -> {
                                if (getTableRow().getItem() != null) {
                                    ImageView imageView = new ImageView((((AgreementTableCell) getTableRow().getItem()).getDocUrl().getValue()));
                                    imageView.setFitHeight(66);
                                    imageView.setFitWidth(85);

                                    runOnFXApplicationThread(() -> setGraphic(imageView));
                                } else {
                                    setGraphic(null);
                                }
                            }).start();
                        } catch (Exception e) {
                            System.out.println("Failed to load ImageView for document: " + (((AgreementTableCell) getTableRow().getItem()).getName()));
                        }
                    }
                }
            }
        });

        nameTableColumn.setCellValueFactory(cellData -> cellData.getValue().getName());
        nameTableColumn.setCellFactory(cell -> new TableCell<AgreementTableCell, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                setAlignment(Pos.CENTER_LEFT);

                if (item == null || empty) {
                    setText(null);
                } else {
                    if (getTableRow().getItem() != null) {
                        setText((((AgreementTableCell) getTableRow().getItem())).getName().getValue());
                    }
                }
            }
        });

        messageTableColumn.setCellValueFactory(cellData -> cellData.getValue().getMessage());
        messageTableColumn.setCellFactory(cell -> new TableCell<AgreementTableCell, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                setAlignment(Pos.CENTER_LEFT);

                if (item == null || empty) {
                    setText(null);
                } else {
                    if (getTableRow().getItem() != null) {
                        setText((((AgreementTableCell) getTableRow().getItem())).getMessage().getValue());
                    }
                }
            }
        });

        statusTableColumn.setCellValueFactory(cellData -> cellData.getValue().getStatus());
        statusTableColumn.setCellFactory(cell -> new TableCell<AgreementTableCell, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                setAlignment(Pos.CENTER_LEFT);

                if (item == null || empty) {
                    setText(null);
                } else {
                    if (getTableRow().getItem() != null) {
                        setText((((AgreementTableCell) getTableRow().getItem())).getStatus().getValue());
                    }
                }
            }
        });

        clearTopHBox();

        Button refreshButton = new Button("Refresh");
        refreshButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.REFRESH, "18px"));
        refreshButton.setOnAction(event1 -> refreshAgreements());
        topHBox.getChildren().add(refreshButton);

        refreshAgreements();
    }

    public void showAgreementDetails(AgreementTableCell agreementTableCell) {
        //Update ViewState
        viewState = ViewState.AGREEMENT_DETAILS;

        //Clear ChildrenNodes to add content.
        contentVBox.getChildren().clear();
        clearTopHBox();

        //MenuOptions Button
        MenuItem printMenuItem = new MenuItem("Print");
        printMenuItem.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.PRINT, "18px"));
        printMenuItem.setDisable(true);

        MenuItem pdfMenuItem = new MenuItem("PDF");
        pdfMenuItem.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.FILE_PDF_ALT, "18px"));
        pdfMenuItem.setDisable(true);

        MenuItem deleteMenuItem = new MenuItem("Delete");
        deleteMenuItem.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.REMOVE, "18px"));
        deleteMenuItem.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText(null);
            alert.setContentText("Are you sure you want to delete this agreement?");
            alert.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.EXCLAMATION_CIRCLE, "30px"));
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().addAll(main.getWindow().getIcons());

            if (alert.showAndWait().get() == ButtonType.OK) {
                returnToAgreementsList();
                refreshAgreements();
                try {
                    AgreementsApi agreementsApi = new AgreementsApi();

                    MultivaluedMap headers = new MultivaluedMapImpl();
                    headers.put(AdobeLibrary.ACCESS_TOKEN_HEADER, main.getAuthenticationToken().getAccess_token());

                    agreementsApi.deleteAgreement(headers, agreementTableCell.getAgreementInfo().getAgreementId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        MenuButton optionsMenuButton = new MenuButton("Options");
        optionsMenuButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.COG, "18px"));
        optionsMenuButton.getItems().addAll(printMenuItem, pdfMenuItem, deleteMenuItem);

        //Back Button
        Button backButton = new Button("Back");
        backButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.ARROW_CIRCLE_LEFT, "18px"));
        backButton.setOnAction(event -> returnToAgreementsList());
        backButton.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);

        topHBox.getChildren().addAll(optionsMenuButton, backButton);

        //Add content
        try {
            AgreementDetailsContentModule.setAgreementInfo(agreementTableCell.getAgreementInfo());

            Parent parent = FXMLLoader.load(AdobeLibrary.getInstance().getClass().getClassLoader().getResource("details_content.fxml"));

            contentVBox.getChildren().add(parent);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to load FXML for the ComposeContent module.");
        }
    }

    public void showDocumentsLibrary() {
        //Update ViewState
        viewState = ViewState.DOCUMENTS_LIBRARY;

        //Clear ChildrenNodes to add content.
        contentVBox.getChildren().clear();
        clearTopHBox();

        //Back Button
        Button backButton = new Button("Back");
        backButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.ARROW_CIRCLE_LEFT, "18px"));
        backButton.setOnAction(event -> returnToAgreementsList());
        backButton.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);

        topHBox.getChildren().addAll(backButton);

        //Add content
        try {
            Parent parent = FXMLLoader.load(AdobeLibrary.getInstance().getClass().getClassLoader().getResource("documents_content.fxml"));

            contentVBox.getChildren().add(parent);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to load FXML for the DocumentsContent module.");
        }
    }

    public void setupSidePanelButtons() {
        agreementsButton.setText("   Agreements  ");
        agreementsButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.CLIPBOARD, "18px"));
        agreementsButton.setOnAction(event -> {
            if (viewState != ViewState.AGREEMENTS_LIST) {
                returnToAgreementsList();
            }
        });

        composeButton.setText("    Compose     ");
        composeButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.PENCIL_SQUARE, "18px"));
        composeButton.setOnAction(event -> {
            if (viewState != ViewState.COMPOSING_AGREEMENT) {
                viewState = ViewState.COMPOSING_AGREEMENT;

                contentVBox.getChildren().clear();
                clearTopHBox();

                //Add content
                try {
                    Parent parent = FXMLLoader.load(AdobeLibrary.getInstance().getClass().getClassLoader().getResource("compose_content.fxml"));

                    contentVBox.getChildren().add(parent);

                    Button backButton = new Button("Back");
                    backButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.ARROW_CIRCLE_LEFT, "18px"));
                    backButton.setOnAction(event1 -> returnToAgreementsList());
                    backButton.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);

                    topHBox.getChildren().add(backButton);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Failed to load FXML for the ComposeContent module.");
                }
            }
        });

        documentsButton.setText("  Documents   ");
        documentsButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.FOLDER_OPEN , "18px"));
        documentsButton.setOnAction(event -> {
            if (viewState != ViewState.DOCUMENTS_LIBRARY) {
                showDocumentsLibrary();
            }
        });
    }

    public void clearTopHBox() {
        topHBox.getChildren().clear();

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spacer.setMinSize(150, 1);

        topHBox.getChildren().addAll(searchLabel, searchTextField, spacer);
    }

    public void refreshAgreements() {
        contentVBox.getChildren().clear();
        contentVBox.setAlignment(Pos.CENTER);

        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setPrefSize(150, 150);

        contentVBox.getChildren().add(indicator);

        agreementTableCells.clear();

        new Thread(() -> {
            AgreementsApi api = new AgreementsApi();

            MultivaluedMap headers = new MultivaluedMapImpl();
            headers.put(AdobeLibrary.ACCESS_TOKEN_HEADER, main.getAuthenticationToken().getAccess_token());

            try {
                UserAgreements agreements = api.getAgreements(headers, "", "", "", "");

                if ((agreements != null && agreements.getUserAgreementList() != null) && agreements.getUserAgreementList().size() > 0) {
                    runOnFXApplicationThread(()-> {
                        contentVBox.getChildren().clear();
                        contentVBox.setAlignment(Pos.TOP_LEFT);
                        contentVBox.getChildren().add(agreementsTableView);
                    });

                    //TODO: Loading indicators for table cells.

                    for (UserAgreement agreement : agreements.getUserAgreementList()) {
                        new Thread(() -> {
                            try {
                                AgreementTableCell agreementTableCell = new AgreementTableCell(api.getAgreementInfo(headers, agreement.getAgreementId()));

                                runOnFXApplicationThread(() -> agreementTableCells.add(agreementTableCell));

                                Thread.currentThread().interrupt();
                            } catch (ApiException e) {
                                e.printStackTrace();
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    }
                } else {
                    runOnFXApplicationThread(()-> {
                        contentVBox.getChildren().clear();
                        contentVBox.setAlignment(Pos.CENTER);

                        Label label = new Label("Could not find any documents.");

                        contentVBox.getChildren().add(label);
                    });
                }
            } catch (ApiException e) {
                runOnFXApplicationThread(()-> {
                    contentVBox.getChildren().clear();
                    contentVBox.setAlignment(Pos.CENTER);

                    Label label = new Label("Failed to grab documents.");
                    label.setTextFill(Paint.valueOf("red"));

                    contentVBox.getChildren().add(label);
                });

                e.printStackTrace();
            }

            FilteredList<AgreementTableCell> filteredList = new FilteredList<>(agreementTableCells, c -> true);

            searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredList.setPredicate(cell -> {
                    if (viewState == ViewState.AGREEMENTS_LIST) {
                        if (newValue == null || newValue.isEmpty()) {
                            return true;
                        }

                        String lowerCaseFilter = newValue.toLowerCase();

                        if (cell.getName().getValue().toLowerCase().contains(lowerCaseFilter)) {
                            return true;
                        }
                    }

                    return false;
                });
            });

            SortedList<AgreementTableCell> sortedList = new SortedList<>(filteredList);
            sortedList.comparatorProperty().bind(agreementsTableView.comparatorProperty());
            agreementsTableView.setSortPolicy(param -> true);

            runOnFXApplicationThread(()-> {
                agreementsTableView.requestFocus();
                agreementsTableView.setItems(sortedList);
            });
        }).start();
    }

    public void returnToAgreementsList() {
        viewState = ViewState.AGREEMENTS_LIST;

        contentVBox.getChildren().clear();
        contentVBox.getChildren().add(agreementsTableView);
        clearTopHBox();

        Button refreshButton = new Button("Refresh");
        refreshButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.REFRESH, "18px"));
        refreshButton.setOnAction(event1 -> refreshAgreements());
        topHBox.getChildren().add(refreshButton);
    }

    public static MainController getInstance() {
        return instance;
    }
}
