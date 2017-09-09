package com.adobe.sign.adobelibrary.ui.modules;

import com.adobe.sign.adobelibrary.AdobeLibrary;
import com.adobe.sign.adobelibrary.ui.data.BasicObject;
import com.adobe.sign.adobelibrary.ui.data.ObjectType;
import com.adobe.sign.api.LibraryDocumentsApi;
import com.adobe.sign.api.TransientDocumentsApi;
import com.adobe.sign.model.libraryDocuments.*;
import com.adobe.sign.model.transientDocuments.TransientDocumentResponse;
import com.adobe.sign.utils.ApiException;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

import javax.ws.rs.core.MultivaluedMap;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static com.adobe.sign.adobelibrary.utils.SafeThread.runOnFXApplicationThread;
import static javafx.scene.control.TreeItem.expandedItemCountChangeEvent;
import static javafx.scene.control.TreeItem.valueChangedEvent;

/**
 * Created by Calvin on 4/20/2017
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
public class DocumentsContentModule implements Initializable {

    private AdobeLibrary main = AdobeLibrary.getInstance();

    @FXML
    private TreeView<BasicObject> treeView;

    @FXML
    private Button uploadButton;

    @FXML
    private Pane loadingPane;
    @FXML
    private Pane contentPane;
    private boolean toggled = true;

    private ArrayList<File> selectedFiles = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        toggleLoading();
        createUploadModule();
        createDocumentTreeViewModule();
    }

    public void toggleLoading() {
        if (toggled) {
            toggled = false;
            contentPane.getChildren().remove(loadingPane);
        } else {
            toggled = true;
            contentPane.getChildren().add(loadingPane);
        }
    }

    public void createUploadModule() {
        uploadButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.UPLOAD, "24px"));
        uploadButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select A File");
            List<File> files = fileChooser.showOpenMultipleDialog(main.getWindow());

            if (files != null && files.size() > 0) {
                LibraryDocumentsApi libraryDocumentsApi = new LibraryDocumentsApi();
                TransientDocumentsApi transientDocumentsApi = new TransientDocumentsApi();

                System.out.println("Files: " + files.size());

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirmation");
                alert.setHeaderText(null);
                alert.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.QUESTION_CIRCLE, "30px"));
                alert.setContentText("Are you sure you want to upload these files?");
                Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                stage.getIcons().addAll(main.getWindow().getIcons());

                if (alert.showAndWait().get() == ButtonType.OK) {
                    if (files.size() > 0) {
                        new Thread(() -> {
                            runOnFXApplicationThread(this::toggleLoading);

                            try {
                                MultivaluedMap headers = new MultivaluedMapImpl();
                                headers.put(AdobeLibrary.ACCESS_TOKEN_HEADER, main.getAuthenticationToken().getAccess_token());

                                for (File file : files) {
                                    List<FileInfo> fileInfoList = new ArrayList<>();
                                    TransientDocumentResponse documentResponse = transientDocumentsApi.createTransientDocument(headers, file.getName(), file, null);

                                    FileInfo fileInfo = new FileInfo();
                                    fileInfo.setTransientDocumentId(documentResponse.getTransientDocumentId());

                                    fileInfoList.add(fileInfo);

                                    LibraryDocumentCreationInfo libraryDocumentCreationInfo = new LibraryDocumentCreationInfo();
                                    libraryDocumentCreationInfo.setName(file.getName().split("\\.")[0]);
                                    libraryDocumentCreationInfo.setFileInfos(fileInfoList);
                                    libraryDocumentCreationInfo.setLibrarySharingMode(LibraryDocumentCreationInfo.LibrarySharingModeEnum.USER);
                                    List<LibraryDocumentCreationInfo.LibraryTemplateTypesEnum> templates = new ArrayList<>();
                                    templates.add(LibraryDocumentCreationInfo.LibraryTemplateTypesEnum.DOCUMENT);
                                    libraryDocumentCreationInfo.setLibraryTemplateTypes(templates);

                                    LibraryCreationInfo libraryCreationInfo = new LibraryCreationInfo();
                                    libraryCreationInfo.setLibraryDocumentCreationInfo(libraryDocumentCreationInfo);
                                    InteractiveOptions options = new InteractiveOptions();
                                    options.setAuthoringRequested(false);
                                    options.setAutoLoginUser(false);
                                    options.setNoChrome(false);
                                    libraryCreationInfo.setOptions(options);

                                    LibraryDocumentCreationResponse creationResponse = libraryDocumentsApi.createLibraryDocument(headers, libraryCreationInfo);

                                    runOnFXApplicationThread(() -> {
                                        if (!treeView.getSelectionModel().isEmpty()) {
                                            TreeItem<BasicObject> treeItem = treeView.getSelectionModel().getSelectedItem();

                                            if (treeItem != null) {
                                                treeItem.getValue().createAndAddChild(file.getName().split("\\.")[0], creationResponse.getLibraryDocumentId(), ObjectType.DOCUMENT);

                                                System.out.println("Uploaded document to: " + treeItem.getValue().getName());
                                            }  else {
                                                treeView.getRoot().getValue().createAndAddChild(file.getName().split("\\.")[0], creationResponse.getLibraryDocumentId(), ObjectType.DOCUMENT);

                                                System.out.println("Uploaded document to: " + treeView.getRoot().getValue().getName());
                                            }
                                        } else {
                                            treeView.getRoot().getValue().createAndAddChild(file.getName().split("\\.")[0], creationResponse.getLibraryDocumentId(), ObjectType.DOCUMENT);

                                            System.out.println("Uploaded document to: " + treeView.getRoot().getValue().getName());
                                        }

                                        toggleLoading();
                                        Alert alert1 = new Alert(Alert.AlertType.CONFIRMATION);
                                        alert1.setTitle("Success");
                                        alert1.setHeaderText("Document successfully created.");
                                        alert1.setContentText("Library Document ID: " + creationResponse.getLibraryDocumentId());
                                        alert1.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.EXCLAMATION_CIRCLE, "30px"));
                                        Stage stage1 = (Stage) alert1.getDialogPane().getScene().getWindow();
                                        stage1.getIcons().addAll(main.getWindow().getIcons());
                                        alert1.show();
                                    });
                                }
                            } catch (ApiException e) {
                                runOnFXApplicationThread(() -> {
                                    toggleLoading();
                                    Alert alert1 = new Alert(Alert.AlertType.ERROR);
                                    alert1.setTitle("Failed");
                                    alert1.setHeaderText("Document could not be uploaded");
                                    alert1.setContentText("Error: " + e.getMessage());
                                    alert1.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.EXCLAMATION_TRIANGLE, "30px"));
                                    Stage stage1 = (Stage) alert1.getDialogPane().getScene().getWindow();
                                    stage1.getIcons().addAll(main.getWindow().getIcons());
                                    alert1.show();

                                    e.printStackTrace();
                                });
                            }
                        }).start();

                        contentPane.getChildren().remove(loadingPane);
                    }
                }
            }
        });
    }

    public void createDocumentTreeViewModule() {
        //TreeView CellFactory
        //Cell ContextMenu
        ContextMenu treeContextMenu = new ContextMenu();

        MenuItem newMenuItem = new MenuItem("New Folder");
        newMenuItem.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.FOLDER_ALT, "18px"));
        newMenuItem.setOnAction(event -> treeView.getRoot().getValue().createAndAddChild("New Folder", "", ObjectType.FOLDER));

        treeContextMenu.getItems().addAll(newMenuItem);
        treeView.setContextMenu(treeContextMenu);

        treeView.setCellFactory(param -> {
            TreeCell<BasicObject> cell = new TreeCell<BasicObject>() {
                @Override
                protected void updateItem(BasicObject item, boolean empty) {
                    super.updateItem(item, empty);
                    textProperty().unbind();

                    if (empty) {
                        setText(null);
                        setGraphic(null);
                        setContextMenu(null);
                    } else {
                        setText(item.getName().getValue());

                        if (item.getObjectType() == ObjectType.DOCUMENT) {
                            setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.FILE_TEXT_ALT, "24px"));

                            //Cell ContextMenu
                            ContextMenu contextMenu = new ContextMenu();

                            MenuItem sendMenuItem = new MenuItem("Send");
                            sendMenuItem.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.MAIL_REPLY, "18px"));
                            sendMenuItem.setOnAction(event -> sendDocument(item));

                            MenuItem removeMenuItem = new MenuItem("Remove");
                            removeMenuItem.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.REMOVE, "18px"));
                            removeMenuItem.setOnAction(event -> {
                                TreeItem<BasicObject> parent = search(treeView.getRoot(), item.getUniqueId()).getParent();
                                parent.getValue().getItems().remove(search(treeView.getRoot(), item.getUniqueId()).getValue());
                                parent.getChildren().remove(search(treeView.getRoot(), item.getUniqueId()));
                            });

                            contextMenu.getItems().addAll(sendMenuItem, removeMenuItem);
                            setContextMenu(contextMenu);
                        } else if (item.getObjectType() == ObjectType.FOLDER) {
                            setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.FOLDER_ALT, "24px"));

                            //Cell ContextMenu
                            if (item.getName().getValue().equalsIgnoreCase("All Documents")) {
                                ContextMenu contextMenu = new ContextMenu();

                                MenuItem sendMenuItem = new MenuItem("Send All");
                                sendMenuItem.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.MAIL_REPLY_ALL, "18px"));
                                sendMenuItem.setOnAction(event -> sendDocuments(item));

                                MenuItem newMenuItem = new MenuItem("New Folder");
                                newMenuItem.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.FOLDER_ALT, "18px"));
                                newMenuItem.setOnAction(event -> item.createAndAddChild("New Folder", "", ObjectType.FOLDER));

                                contextMenu.getItems().addAll(sendMenuItem, newMenuItem);
                                setContextMenu(contextMenu);
                            } else {
                                ContextMenu contextMenu = new ContextMenu();

                                MenuItem sendMenuItem = new MenuItem("Send All");
                                sendMenuItem.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.MAIL_REPLY_ALL, "18px"));
                                sendMenuItem.setOnAction(event -> sendDocuments(item));

                                MenuItem newMenuItem = new MenuItem("New Folder");
                                newMenuItem.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.FOLDER_ALT, "18px"));
                                newMenuItem.setOnAction(event -> item.createAndAddChild("New Folder", "", ObjectType.FOLDER));

                                MenuItem renameMenuItem = new MenuItem("Rename");
                                renameMenuItem.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.EDIT, "18px"));
                                renameMenuItem.setOnAction(event -> {
                                    TextInputDialog textInputDialog = new TextInputDialog(item.getName().getValue());
                                    textInputDialog.setHeaderText("");
                                    textInputDialog.setContentText("");
                                    textInputDialog.setTitle("Rename");
                                    textInputDialog.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.EDIT, "24px"));
                                    Stage stage1 = (Stage) textInputDialog.getDialogPane().getScene().getWindow();
                                    stage1.getIcons().addAll(main.getWindow().getIcons());

                                    Optional<String> result = textInputDialog.showAndWait();

                                    result.ifPresent(s -> item.getName().setValue(s));
                                    TreeItem.TreeModificationEvent<BasicObject> ev = new TreeItem.TreeModificationEvent<>(valueChangedEvent(), search(treeView.getRoot(), item.getUniqueId()));
                                    Event.fireEvent(search(treeView.getRoot(), item.getUniqueId()), ev);
                                });

                                MenuItem removeMenuItem = new MenuItem("Remove");
                                removeMenuItem.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.REMOVE, "18px"));
                                removeMenuItem.setOnAction(event -> {
                                    TreeItem<BasicObject> parent = search(treeView.getRoot(), item.getUniqueId()).getParent();
                                    parent.getValue().getItems().remove(search(treeView.getRoot(), item.getUniqueId()).getValue());
                                    parent.getChildren().remove(search(treeView.getRoot(), item.getUniqueId()));
                                });

                                contextMenu.getItems().addAll(sendMenuItem, newMenuItem, renameMenuItem, removeMenuItem);
                                setContextMenu(contextMenu);
                            }
                        }
                    }
                }
            };

            //Cell Hovering
            cell.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                if (isNowHovered && (!cell.isEmpty())) {
                    //Possibly preview image on hover
                }
            });

            //Cell Drag & Drop
            cell.setOnDragDetected(event -> {
                if (cell.getItem() == null) {
                    return;
                }

                if (cell.getItem().getName().getValue().equalsIgnoreCase("") || cell.getItem().getName().getValue().equalsIgnoreCase("All Documents")) {
                    return;
                }

                try {
                    Dragboard dragboard = cell.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(cell.getItem().getUniqueId().toString());
                    dragboard.setContent(content);

                    event.consume();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            cell.setOnDragOver(event -> {
                if (event.getDragboard().hasString()) {
                    if ((cell.getItem().getObjectType() == ObjectType.DOCUMENT)) {
                        return;
                    }

                    TreeItem<BasicObject> itemBeingMoved = search(treeView.getRoot(), UUID.fromString(event.getDragboard().getString()));

                    if (itemBeingMoved == cell.getTreeItem().getParent() && !cell.getTreeItem().getParent().isLeaf()) {
                        return;
                    }

                    if (!cell.getItem().getUniqueId().equals(UUID.fromString(event.getDragboard().getString()))) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                }

                event.consume();
            });

            cell.setOnDragEntered(event -> {
                if ((cell.getItem().getObjectType() == ObjectType.DOCUMENT)) {
                    return;
                }

                if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                    cell.setOpacity(0.3);
                }
            });

            cell.setOnDragExited(event -> {
                if ((cell.getItem().getObjectType() == ObjectType.DOCUMENT)) {
                    return;
                }

                if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                    cell.setOpacity(1);
                }
            });

            cell.setOnDragDropped(event -> {
                if ((cell.getItem().getObjectType() == ObjectType.DOCUMENT)) {
                    return;
                }

                if (cell.getItem() == null) {
                    return;
                }

                boolean success = false;

                if (event.getDragboard().hasString()) {
                    try {
                        TreeItem<BasicObject> itemToMove = search(treeView.getRoot(), UUID.fromString(event.getDragboard().getString()));
                        TreeItem<BasicObject> newParent = search(treeView.getRoot(), cell.getItem().getUniqueId());

                        itemToMove.getParent().getChildren().remove(itemToMove);

                        newParent.getChildren().add(itemToMove);
                        newParent.setExpanded(true);

                        success = true;
                    } catch (Exception e) {
                        success = false;
                        e.printStackTrace();
                    }
                }

                event.setDropCompleted(success);

                event.consume();
            });

            cell.setOnDragDone(Event::consume);

            return cell;
        });

        //Load Folders & Documents
        createSamplesOnRoot();
    }

    private TreeItem<BasicObject> search(TreeItem<BasicObject> currentNode, UUID valueToSearch) {
        TreeItem<BasicObject> result = null;

        if (currentNode.getValue().getUniqueId().equals(valueToSearch)) {
            result = currentNode;
        } else if (!currentNode.isLeaf()) {
            for (TreeItem<BasicObject> child : currentNode.getChildren()) {
                result = search(child, valueToSearch);

                if (result != null) {
                    break;
                }
            }
        }

        return result;
    }

    public void createSamplesOnRoot() {
        BasicObject root = new BasicObject("", "", ObjectType.FOLDER);

        TreeItem<BasicObject> treeRoot = createItem(root);
        BasicObject allDocsFolder = new BasicObject("All Documents", "", ObjectType.FOLDER);
        BasicObject folder1 = new BasicObject("Folder 1", "", ObjectType.FOLDER);
        BasicObject folder2 = new BasicObject("Folder 2", "", ObjectType.FOLDER);
        BasicObject folder22 = new BasicObject("Folder 2 2", "", ObjectType.FOLDER);
        BasicObject folder3 = new BasicObject("Folder 3", "", ObjectType.FOLDER);
        BasicObject folder4 = new BasicObject("Folder 4", "", ObjectType.FOLDER);

        new Thread(() -> {
            try {
                MultivaluedMap headers = new MultivaluedMapImpl();
                headers.put(AdobeLibrary.ACCESS_TOKEN_HEADER, main.getAuthenticationToken().getAccess_token());

                LibraryDocumentsApi libraryDocumentsApi = new LibraryDocumentsApi();
                DocumentLibraryItems items = libraryDocumentsApi.getLibraryDocuments(headers);

                if (items != null && items.getLibraryDocumentList() != null) {
                    for (DocumentLibraryItem item : items.getLibraryDocumentList()) {
                        allDocsFolder.createAndAddChild(item.getName(), item.getLibraryDocumentId(), ObjectType.DOCUMENT);
                    }
                }
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }).start();

        folder1.createAndAddChild("Document 1 Folder 1", "", ObjectType.DOCUMENT);
        folder1.createAndAddChild("Document 2 Folder 1", "", ObjectType.DOCUMENT);
        folder1.createAndAddChild("Document 3 Folder 1", "", ObjectType.DOCUMENT);
        folder22.createAndAddChild("Document 1 Folder 22", "", ObjectType.DOCUMENT);
        folder22.createAndAddChild("Document 2 Folder 22", "", ObjectType.DOCUMENT);
        folder2.createAndAddChild("Document 1 Folder 2", "", ObjectType.DOCUMENT);
        folder2.getItems().add(folder22);
        folder3.createAndAddChild("Document 2 Folder 3", "", ObjectType.DOCUMENT);
        folder4.createAndAddChild("Document 1 Folder 4", "", ObjectType.DOCUMENT);
        folder4.createAndAddChild("Document 2 Folder 45", "", ObjectType.DOCUMENT);
        root.getItems().addAll(allDocsFolder, folder1, folder2, folder3, folder4);
        treeView.setRoot(treeRoot);
        treeView.setShowRoot(false);
    }

    public TreeItem<BasicObject> createItem(BasicObject object) {
        TreeItem<BasicObject> item = new TreeItem<>(object);
        item.setExpanded(false);
        item.getChildren().addAll(object.getItems().stream().map(this::createItem).collect(Collectors.toList()));

        object.getItems().addListener((ListChangeListener<BasicObject>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    item.getChildren().addAll(c.getAddedSubList().stream().map(this::createItem).collect(Collectors.toList()));
                }
                if (c.wasRemoved()) {
                    item.getChildren().removeIf(treeItem -> c.getRemoved().contains(treeItem.getValue()));
                }
            }
        });

        return item;
    }

    public void sendDocument(BasicObject document) {
        ComposeContentModule.getSelectedLibraryDocuments().put(document.getName().getValue(), document.getDocumentId().getValue());
    }

    public void sendDocuments(BasicObject folder) {
        for (BasicObject children : folder.getItems()) {
            if (children.getObjectType() == ObjectType.DOCUMENT) {
                ComposeContentModule.getSelectedLibraryDocuments().put(children.getName().getValue(), children.getDocumentId().getValue());
            }
        }
    }
}
