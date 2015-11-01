/*
 * Copyright 2015 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.util;

import javafx.application.Platform;

import java.awt.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 *
 */
public
class JavaFxUtil {

    public static final javafx.scene.text.Font DEFAULT_FONT = new javafx.scene.text.Font(13);


    public static
    void showOnSameScreenAsMouseCenter(javafx.stage.Window stage) {
        Point mouseLocation = MouseInfo.getPointerInfo()
                                       .getLocation();

        GraphicsDevice deviceAtMouse = ScreenUtil.getGraphicsDeviceAt(mouseLocation);
        Rectangle bounds = deviceAtMouse.getDefaultConfiguration()
                                        .getBounds();

        stage.setX(bounds.x + bounds.width / 2 - stage.getWidth() / 2);
        stage.setY(bounds.y + bounds.height / 2 - stage.getHeight() / 2);
    }

    public static
    void showOnSameScreenAsMouse(javafx.stage.Window stage) {
        Point mouseLocation = MouseInfo.getPointerInfo()
                                       .getLocation();

        GraphicsDevice deviceAtMouse = ScreenUtil.getGraphicsDeviceAt(mouseLocation);

        stage.setX(deviceAtMouse.getDefaultConfiguration()
                                .getBounds().x);
    }

    public static void invokeAndWait(Runnable runnable) {
        // run synchronously on JavaFX thread
        if (Platform.isFxApplicationThread()) {
            runnable.run();
            return;
        }

        FutureTask future = new FutureTask(runnable, null);
        Platform.runLater(future);
        try {
            future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static <T> T invokeAndWait(Callable callable) {
        // run synchronously on JavaFX thread
        if (Platform.isFxApplicationThread()) {
            try {
                return (T) callable.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        FutureTask future = new FutureTask(callable);
        Platform.runLater(future);
        try {
            return (T) future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void invokeLater(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}
