/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.app.viewer3d;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

/**
 * VBox that wraps a Viewer3d instance and adds support for loading
 * <i>.shp</i> files from the file system or URLs.
 * 
 * @author Evan Smith
 *
 */
public class Viewer3dPane extends VBox
{
    public final static Logger logger = Logger.getLogger(Viewer3dPane.class.getName());
    
    private final static Border errorBorder = new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(2)));
    
    private final Consumer<URI> setInput;
    
    private String current_resource;
    
    public Viewer3dPane(final URI resource, final Consumer<URI> setInput) throws Exception
    {
        super();
        
        this.setInput = setInput;
        
        TextField textField = new TextField();
        Button fileButton = new Button(null, ImageCache.getImageView(ImageCache.class, "/icons/fldr_obj.png"));
        Button refreshButton = new Button(null, ImageCache.getImageView(ImageCache.class, "/icons/refresh.png"));
        Button resetViewButton = new Button(null, ImageCache.getImageView(ImageCache.class, "/icons/reset.png"));
        Button clearViewerButton = new Button(null, ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));
        
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Shape files (.shp)", "*.shp");
        
        HBox toolbar = new HBox();
        
        Viewer3d viewer = new Viewer3d();

        fileChooser.getExtensionFilters().add(extFilter);
        toolbar.getChildren().addAll(fileButton, refreshButton, resetViewButton, textField, clearViewerButton);

        VBox.setVgrow(viewer, Priority.ALWAYS);
        VBox.setMargin(viewer, new Insets(0, 10, 10, 10));        

        fileButton.setOnAction(event -> 
        {   
            
            File file = fileChooser.showOpenDialog(getScene().getWindow());
            
            if (null != file)
            {
                try
                {
                    String input = file.toURI().toURL().toString();
                    loadResource(input, viewer, textField);
                } 
                catch (Exception ex)
                {
                    ex.printStackTrace();
                    logger.log(Level.WARNING, "Loading Resource failed", ex);
                }
            }
        });
        fileButton.setTooltip(new Tooltip("Select resource from file system."));
        
        refreshButton.setOnAction(event -> loadResource(current_resource, viewer, textField));
        refreshButton.setTooltip(new Tooltip("Refresh structure from resource."));
        
        resetViewButton.setOnAction(event -> viewer.reset());
        resetViewButton.setTooltip(new Tooltip("Reset view rotation and zoom."));
        
        HBox.setHgrow(textField, Priority.ALWAYS);
        toolbar.setSpacing(10);
        toolbar.setPadding(new Insets(10));
        
        textField.setOnKeyPressed(event ->
        {
            if (event.getCode() == KeyCode.ENTER)
            {
                String input = textField.getText();
                loadResource(input, viewer, textField);
                
            }
        });
        
        textField.setTooltip(new Tooltip("Enter in the URL of a resource to load."));
        
        clearViewerButton.setOnAction(event -> 
        {
            viewer.clear();
            textField.clear();
            current_resource = null;
            setInput.accept(null);
        });
        clearViewerButton.setTooltip(new Tooltip("Clear the viewer of any loaded resources."));
        
        getChildren().addAll(toolbar, viewer);
        
        if (null != resource)
            loadResource(resource.toString(), viewer, textField);        
    }
    
    /**
     * Load a resource file and update the viewer, upon error set the textField's border to signal error.
     * @param resource
     * @param viewer
     */
    private void loadResource(final String resource, final Viewer3d viewer, final TextField textField)
    {
        if (null != resource && ! resource.isEmpty())
        {
            JobManager.schedule("Read 3d viewer resource", monitor -> 
            {
                InputStream inputStream = null;
                
                try
                {
                    inputStream = ResourceUtil.openResource(resource);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Opening resource '" + resource + "' failed", ex);
                    Platform.runLater(() -> textField.setBorder(errorBorder));
                    return;
                }
                
                if (null != inputStream)
                {
                    try
                    {
                        final Xform struct = Viewer3d.buildStructure(inputStream);
                        if (null != struct)
                            Platform.runLater(() -> viewer.setStructure(struct));
                        current_resource = resource;
                    }
                    catch (Exception ex)
                    {
                        logger.log(Level.WARNING, "Building structure failed", ex);
                        Platform.runLater(() -> textField.setBorder(errorBorder));
                        return;
                    }
                }
                
                Platform.runLater(() -> textField.setBorder(null));
                
                textField.setText(resource);
                
                if (null != setInput)
                    setInput.accept(new URI(resource));
            });
        }
    }
}
