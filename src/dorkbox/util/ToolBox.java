/*
 * Copyright 2010 dorkbox, llc
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

import java.util.concurrent.ConcurrentHashMap;

/**
 * Allows for different objects to be reused in the system directly
 */
public
class ToolBox {

    private final ConcurrentHashMap<Class<?>, dorkbox.util.Tool> toolMap = new ConcurrentHashMap<Class<?>, dorkbox.util.Tool>();

    /**
     * Registers a tool with the server, to be used by other services.
     */
    public
    <Tool extends dorkbox.util.Tool> void register(Tool toolClass) {
        if (toolClass == null) {
            throw new IllegalArgumentException("Tool must not be null! Unable to add tool");
        }

        dorkbox.util.Tool put = this.toolMap.put(toolClass.getClass(), toolClass);
        if (put != null) {
            throw new IllegalArgumentException("Tool must be unique! Unable to add tool '" + toolClass + "'");
        }
    }



    /**
     * Only get the tools in the ModuleStart (ie: load) methods. If used in the constructor, the tool might not be available yet
     */
    public
    <Tool extends dorkbox.util.Tool> Tool get(Class<Tool> toolClass) {
        if (toolClass == null) {
            throw new IllegalArgumentException("Tool must not be null! Unable to add tool");
        }

        @SuppressWarnings("unchecked")
        Tool tool = (Tool) this.toolMap.get(toolClass);
        return tool;
    }

    /**
     * Only get the tools in the ModuleStart (ie: load) methods. If done in the constructor, the tool might not be available yet
     */
    public
    <Tool extends dorkbox.util.Tool> void remove(Class<Tool> toolClass) {
        if (toolClass == null) {
            throw new IllegalArgumentException("Tool must not be null! Unable to remove tool");
        }

        this.toolMap.remove(toolClass);
    }
}
