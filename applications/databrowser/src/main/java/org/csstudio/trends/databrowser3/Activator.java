/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.csstudio.javafx.rtplot.util.NamedThreadFactory;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Global Data Browser helper
 *  @author Kay Kasemir
 */
// TODO Rename
@SuppressWarnings("nls")
public class Activator
{
    /** Logger for all Data Browser code */
    public static final Logger logger = Logger.getLogger(Activator.class.getPackageName());

    /** Thread pool, mostly for fetching archived data */
    public static final ExecutorService thread_pool = Executors.newCachedThreadPool(new NamedThreadFactory("DataBrowserJobs"));

    /** @param base_name Icon base name (no path, no extension)
     *  @return {@link Image}
     *  @throws Exception on error
     */
    public static Image getImage(final String base_name)
    {
        return ImageCache.getImage(Activator.class, "/icons/" + base_name + ".png");
    }

    /** @param base_name Icon base name (no path, no extension)
     *  @return {@link ImageView}
     *  @throws Exception on error
     */
    public static ImageView getIcon(final String base_name)
    {
        return new ImageView(getImage(base_name));
    }

}
