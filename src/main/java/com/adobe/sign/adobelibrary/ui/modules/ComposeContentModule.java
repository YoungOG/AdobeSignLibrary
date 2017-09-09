package com.adobe.sign.adobelibrary.ui.modules;

import com.adobe.sign.adobelibrary.AdobeLibrary;
import com.adobe.sign.adobelibrary.ui.cells.FileCell;
import com.adobe.sign.adobelibrary.ui.cells.RecipientTableCell;
import com.adobe.sign.api.AgreementsApi;
import com.adobe.sign.api.TransientDocumentsApi;
import com.adobe.sign.model.agreements.*;
import com.adobe.sign.model.agreements.FileInfo;
import com.adobe.sign.model.agreements.RecipientInfo;
import com.adobe.sign.model.agreements.RecipientSetInfo;
import com.adobe.sign.model.transientDocuments.TransientDocumentResponse;
import com.adobe.sign.utils.ApiException;
import com.google.gson.Gson;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;

import javax.ws.rs.core.MultivaluedMap;
import java.io.*;
import java.net.URL;
import java.util.*;

import static com.adobe.sign.adobelibrary.utils.SafeThread.runOnFXApplicationThread;

/**
 * Created by Calvin on 4/21/2017
 * for the AdobeLibrary project.
 */

@Getter
@Setter
public class ComposeContentModule implements Initializable {

    private AdobeLibrary main = AdobeLibrary.getInstance();

    @FXML
    private Pane contentPane;

    //Order Section
    @FXML
    private Label inOrderLabel;
    @FXML
    private Label anyOrderLabel;
    @FXML
    private HBox orderHBox;

    private ToggleSwitch orderToggleSwitch;

    //Recipient Section
    @FXML
    private ComboBox<String> recipientComboBox;
    @FXML
    private TextField recipientEmailTextField;
    @FXML
    private Button recipientAddButton;
    @FXML
    private TableView<RecipientTableCell> recipientsTableView;
    @FXML
    private TableColumn<RecipientTableCell, String> roleTableColumn;
    @FXML
    private TableColumn<RecipientTableCell, String> emailTableColumn;
    @FXML
    private ObservableList<RecipientTableCell> recipientTableCells = FXCollections.observableArrayList();

    //Message Section
    @FXML
    private TextField agreementNameTextField;
    @FXML
    private TextArea messageTextArea;

    //File Section
    @FXML
    private Button addFilesButton;
    @FXML private Button clearFilesButton;
    @FXML
    private ListView<String> filesListView;
    private ObservableList<String> fileNames = FXCollections.observableArrayList();
    private List<File> selectedFiles = new ArrayList<>();
    private static HashMap<String, String> selectedLibraryDocuments = new HashMap<>();

    //Loading
    @FXML
    private Pane loadingPane;
    @FXML
    private ProgressBar progressIndicator;

    @FXML
    private Button sendButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        contentPane.getChildren().remove(loadingPane);
        sendButton.setStyle("-fx-base: #2175c8; -fx-font-family: adobe clean n4;-fx-font-size: 18px;-fx-font-weight: bold;-fx-text-fill: white; -fx-border-fill: none;");

        setupRecipientsSection();

        setupFilesSection();

        if (selectedLibraryDocuments.size() > 0) {
            fileNames.clear();
            fileNames.addAll(selectedLibraryDocuments.keySet());
            filesListView.getItems().clear();
            filesListView.setItems(fileNames);
        }
    }

    public void setupRecipientsSection() {
        orderToggleSwitch = new ToggleSwitch();

        sendButton.setOnAction(event -> {
            if (recipientTableCells.size() <= 0) {
                int depth = 30;

                DropShadow borderGlow = new DropShadow();
                borderGlow.setColor(Color.RED);
                borderGlow.setOffsetX(0F);
                borderGlow.setOffsetY(0F);
                borderGlow.setWidth(depth);
                borderGlow.setHeight(depth);

                recipientsTableView.toFront();
                recipientsTableView.setEffect(borderGlow);

                KeyFrame kf1 = new KeyFrame(Duration.seconds(1), e -> recipientsTableView.setEffect(null));
                Platform.runLater(new Timeline(kf1)::play);
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText(null);
            alert.setContentText("Are you sure you want to create this agreement?");
            alert.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.PAPER_PLANE_ALT, "30px"));
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().addAll(main.getWindow().getIcons());

            if (alert.showAndWait().get() == ButtonType.OK) {
                sendAgreement();
            }
        });

        recipientComboBox.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<String>() {
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);

                        if (item != null && !empty) {
                            setText(item);
                            setGraphic(getLabelIconFromType(item));
                        } else {
                            setText(null);
                            setGraphic(null);
                        }
                    }
                };
            }
        });
        ListView<String> values = new ListView<>();
        values.getItems().addAll("Signer", "Approver");
        recipientComboBox.setButtonCell(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<String>() {
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);

                        if (item != null && !empty) {
                            setText(item);
                            setGraphic(getLabelIconFromType(item));
                        } else {
                            setText(null);
                            setGraphic(null);
                        }
                    }
                };
            }
        }.call(values));

        recipientComboBox.getItems().addAll("Signer", "Approver");
        recipientComboBox.getSelectionModel().selectFirst();

        recipientAddButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.USER_PLUS, "17px"));
        recipientAddButton.setOnAction(event -> {
            String role;
            String email;

            if (recipientComboBox.getSelectionModel() != null && recipientComboBox.getSelectionModel().getSelectedItem() != null) {
                if (recipientComboBox.getSelectionModel().getSelectedIndex() == 0) {
                    role = "Signer";
                } else {
                    role = "Approver";
                }
            } else {
                int depth = 30;

                DropShadow borderGlow = new DropShadow();
                borderGlow.setColor(Color.RED);
                borderGlow.setOffsetX(0F);
                borderGlow.setOffsetY(0F);
                borderGlow.setWidth(depth);
                borderGlow.setHeight(depth);

                recipientComboBox.toFront();
                recipientComboBox.setEffect(borderGlow);

                KeyFrame kf1 = new KeyFrame(Duration.seconds(1), e -> recipientComboBox.setEffect(null));
                Platform.runLater(new Timeline(kf1)::play);
                return;
            }

            if (recipientEmailTextField.getText() != null && !recipientEmailTextField.getText().isEmpty()) {
                email = recipientEmailTextField.getText();
            } else {
                int depth = 30;

                DropShadow borderGlow = new DropShadow();
                borderGlow.setColor(Color.RED);
                borderGlow.setOffsetX(0F);
                borderGlow.setOffsetY(0F);
                borderGlow.setWidth(depth);
                borderGlow.setHeight(depth);

                recipientEmailTextField.toFront();
                recipientEmailTextField.setEffect(borderGlow);

                KeyFrame kf1 = new KeyFrame(Duration.seconds(1), e -> recipientEmailTextField.setEffect(null));
                Platform.runLater(new Timeline(kf1)::play);
                return;
            }

            recipientTableCells.add(new RecipientTableCell(UUID.randomUUID().toString(), role, email));
            recipientsTableView.setItems(recipientTableCells);
        });

        recipientEmailTextField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                recipientAddButton.fire();
            }
        });

        recipientsTableView.setRowFactory(param -> {
            TableRow<RecipientTableCell> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();

            MenuItem removeMenuItem = new MenuItem("Remove");
            removeMenuItem.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.REMOVE, "18px"));
            removeMenuItem.setOnAction(event -> recipientsTableView.getItems().remove(row.getItem()));

            contextMenu.getItems().addAll(removeMenuItem);

            row.contextMenuProperty().bind(Bindings.when(Bindings.isNotNull(row.itemProperty()))
                    .then(contextMenu)
                    .otherwise((ContextMenu) null));

            row.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            row.setAlignment(Pos.CENTER);

            row.setOnDragDetected(event -> {
                try {
                    if (!orderToggleSwitch.getOrder().getValue()) {
                        return;
                    }

                    if (row.getItem() == null) {
                        return;
                    }

                    //Serialize the object
                    String cellStateSerialized = new Gson().toJson(row.getItem(), RecipientTableCell.class);

                    Dragboard dragboard = row.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(cellStateSerialized);
                    dragboard.setContent(content);

                    event.consume();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            row.setOnDragOver(event -> {
                if (!orderToggleSwitch.getOrder().getValue()) {
                    return;
                }

                if (event.getGestureSource() != row && event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }

                event.consume();
            });

            row.setOnDragEntered(event -> {
                if (!orderToggleSwitch.getOrder().getValue()) {
                    return;
                }

                if (event.getGestureSource() != row && event.getDragboard().hasString()) {
                    row.setOpacity(0.3);
                }
            });

            row.setOnDragExited(event -> {
                if (!orderToggleSwitch.getOrder().getValue()) {
                    return;
                }

                if (event.getGestureSource() != row && event.getDragboard().hasString()) {
                    row.setOpacity(1);
                }
            });

            row.setOnDragDropped(event -> {
                if (!orderToggleSwitch.getOrder().getValue()) {
                    return;
                }

                if (row.getItem() == null) {
                    return;
                }

                Dragboard db = event.getDragboard();
                boolean success = false;

                if (db.hasString()) {
                    try {
                        // Deserialize the object
                        RecipientTableCell cellState = new Gson().fromJson(db.getString(), RecipientTableCell.class);

                        int draggedIdx = recipientTableCells.indexOf(cellState);
                        int thisIdx = recipientTableCells.indexOf(row.getItem());

                        recipientTableCells.set(draggedIdx, row.getItem());
                        recipientTableCells.set(thisIdx, cellState);

                        recipientsTableView.setItems(recipientTableCells);

                        success = true;
                    } catch (Exception e) {
                        success = false;
                        e.printStackTrace();
                    }
                }

                event.setDropCompleted(success);

                event.consume();
            });

            row.setOnDragDone(event -> {
                if (!orderToggleSwitch.getOrder().getValue()) {
                    return;
                }

                event.consume();
            });

            return row;
        });

        roleTableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRole()));
        roleTableColumn.setCellFactory(cell -> new TableCell<RecipientTableCell, String>() {
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
                        setGraphic(null);

                        if ((((RecipientTableCell) getTableRow().getItem()).getRole() != null)) {
                            setText((((RecipientTableCell) getTableRow().getItem()).getRole()));

                            setGraphic(getLabelIconFromType(((RecipientTableCell) getTableRow().getItem()).getRole()));
                        }
                    }
                }
            }
        });

        emailTableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
        emailTableColumn.setCellFactory(cell -> new TableCell<RecipientTableCell, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                setAlignment(Pos.CENTER_LEFT);

                if (item == null || empty) {
                    setText(null);
                } else {
                    if (getTableRow().getItem() != null) {
                        setText(null);

                        if ((((RecipientTableCell) getTableRow().getItem()).getEmail() != null)) {
                            setText((((RecipientTableCell) getTableRow().getItem()).getEmail()));
                        }
                    }
                }
            }
        });
    }

    public void setupFilesSection() {
        addFilesButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.PLUS, "17px"));
        addFilesButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select A File");
            List<File> files = fileChooser.showOpenMultipleDialog(main.getWindow());

            if (files != null && files.size() > 0) {
                for (File file : files) {
                    selectedFiles.add(file);
                    fileNames.add(file.getName());
                    filesListView.setItems(fileNames);
                }
            }
        });

        clearFilesButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TRASH_ALT, "17px"));
        clearFilesButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText(null);
            alert.setContentText("Are you sure you want to clear files?");
            alert.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.QUESTION_CIRCLE, "30px"));
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().addAll(main.getWindow().getIcons());

            if (alert.showAndWait().get() == ButtonType.OK) {
                selectedLibraryDocuments.clear();
                selectedFiles.clear();
                fileNames.clear();
                filesListView.getItems().clear();
            }
        });

        filesListView.setCellFactory(param -> {
            ListCell<String> row = new FileCell();

            ContextMenu contextMenu = new ContextMenu();

            MenuItem removeMenuItem = new MenuItem("Remove");
            removeMenuItem.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.REMOVE, "18px"));
            removeMenuItem.setOnAction(event -> {
                selectedFiles.remove(getFile(row.getItem()));
                selectedLibraryDocuments.remove(row.getItem());
                fileNames.remove(row.getItem());
                filesListView.setItems(fileNames);
            });

            contextMenu.getItems().addAll(removeMenuItem);

            row.contextMenuProperty().bind(Bindings.when(Bindings.isNotNull(row.itemProperty()))
                    .then(contextMenu)
                    .otherwise((ContextMenu) null));

            row.setOnDragDetected(event2 -> {
                if (row.getItem() == null) {
                    return;
                }

                Dragboard dragboard = row.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(row.getItem());
                dragboard.setContent(content);

                event2.consume();
            });

            row.setOnDragOver(event3 -> {
                if (event3.getGestureSource() != row && event3.getDragboard().hasString()) {
                    event3.acceptTransferModes(TransferMode.MOVE);
                }

                event3.consume();
            });

            row.setOnDragEntered(event4 -> {
                if (event4.getGestureSource() != row && event4.getDragboard().hasString()) {
                    row.setOpacity(0.3);
                }
            });

            row.setOnDragExited(event4 -> {
                if (event4.getGestureSource() != row && event4.getDragboard().hasString()) {
                    row.setOpacity(1);
                }
            });

            row.setOnDragDropped(event5 -> {
                if (row.getItem() == null) {
                    return;
                }

                Dragboard db = event5.getDragboard();
                boolean success = false;

                if (db.hasString()) {
                    int draggedIdx = fileNames.indexOf(db.getString());
                    int thisIdx = fileNames.indexOf(row.getItem());

                    File file = selectedFiles.get(draggedIdx);
                    selectedFiles.set(draggedIdx, selectedFiles.get(thisIdx));
                    selectedFiles.set(thisIdx, file);

                    fileNames.set(draggedIdx, row.getItem());
                    fileNames.set(thisIdx, db.getString());

                    success = true;
                }

                event5.setDropCompleted(success);

                event5.consume();
            });

            row.setOnDragDone(DragEvent::consume);

            return row;
        });
    }


    public void sendAgreement() {
    }

    public class ToggleSwitch {

        private Label label = new Label();

        @Getter
        private SimpleBooleanProperty order = new SimpleBooleanProperty(true);

        public ToggleSwitch() {
            setup();
        }

        public void setup() {
            orderHBox.getChildren().add(label);

            label.setOnMouseClicked(event -> {
                if (order.getValue()) {
                    inOrderLabel.setTextFill(Paint.valueOf("grey"));
                    anyOrderLabel.setTextFill(Paint.valueOf("black"));

                    label.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TOGGLE_ON, "32px"));
                } else {
                    inOrderLabel.setTextFill(Paint.valueOf("black"));
                    anyOrderLabel.setTextFill(Paint.valueOf("grey"));

                    label.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TOGGLE_OFF, "32px"));
                }

                order.setValue(!order.getValue());
            });
            label.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TOGGLE_OFF, "32px"));

            setStyle();
            bindProperties();
        }

        public void setStyle() {
            orderHBox.setPrefWidth(40);
            label.setAlignment(Pos.CENTER);
            orderHBox.setAlignment(Pos.CENTER_LEFT);
        }

        public void bindProperties() {
            label.prefWidthProperty().bind(orderHBox.widthProperty().divide(2));
            label.prefHeightProperty().bind(orderHBox.heightProperty());
        }
    }

    public Text getLabelIconFromType(String type) {
        switch (type) {
            case "Signer":
                return GlyphsDude.createIcon(FontAwesomeIcon.PENCIL_SQUARE_ALT, "17px");
            case "Approver":
                return GlyphsDude.createIcon(FontAwesomeIcon.CHECK_SQUARE_ALT, "17px");
        }

        return null;
    }

    private File getFile(String fileName) {
        for (File file : selectedFiles) {
            if (file.getName().equalsIgnoreCase(fileName)) {
                return file;
            }
        }

        return null;
    }

    public static HashMap<String, String> getSelectedLibraryDocuments() {
        return selectedLibraryDocuments;
    }
}
