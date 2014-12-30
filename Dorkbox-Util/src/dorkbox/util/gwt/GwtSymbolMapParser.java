/*
 * Copyright 2011 dorkbox, llc
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
package dorkbox.util.gwt;

import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class GwtSymbolMapParser {

    private final Map<String, String> symbolMap;

    public GwtSymbolMapParser() {
        this.symbolMap = new HashMap<String, String>();
    }

    /**
     * Efficiently parses the inputstream for symbolmap information.
     * <p>
     * Automatically closes the input stream when finished.
     */
    public void parse(InputStream inputStream) {
        if (inputStream == null) {
            return;
        }

        InputStreamReader in = new InputStreamReader(inputStream, CharsetUtil.UTF_8);

        // 1024 is the longest the line will get. We start there, but StringBuilder will let us grow.
        StringBuilder builder = new StringBuilder(1024);

        int charRead = '\r';
        char CHAR = (char) charRead;
        try {
            while ((charRead = in.read()) != -1) {
                CHAR = (char) charRead;

                if (CHAR != '\r' && CHAR != '\n') {
                    builder.append(CHAR);
                } else {
                    processLine(builder.toString());
                    // new line!
                    builder.delete(0, builder.capacity());
                }
            }
        } catch (IOException e) {

        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
    }

    public Map<String, String> getSymbolMap() {
        return this.symbolMap;
    }

    public void processLine(String line) {
        if (line.charAt(0) == '#') {
            return;
        }

        String[] symbolInfo = line.split(",");

        // There are TWO versions of this file!
        // 1) the ORIGINAL version (as created by the GWT compiler)
        // 2) the SHRUNK version (as created by the build scripts)

        // version 1:
        // # jsName, jsniIdent, className, memberName, sourceUri, sourceLine, fragmentNumber

        // version 2:
        // jsName, className

        if (symbolInfo.length > 2) {
            // version 1
            String jsName = symbolInfo[0];
//            String jsniIdent = symbolInfo[1];
            String className = symbolInfo[2];
            String memberName = symbolInfo[3];
//        String sourceUri = symbolInfo[4];
//        String sourceLine = symbolInfo[5];
//        String fragmentNumber = symbolInfo[6];
//
//        // The path relative to the source server. We assume it is just the
//        // class path base.
//        String sourcePath = className.replace('.', '/');
//        int lastSlashIndex = sourcePath.lastIndexOf("/") + 1;
//        String sourcePathBase = sourcePath.substring(0, lastSlashIndex);
//
//        // The sourceUri contains the actual file name.
//        String sourceFileName = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.length());
//
//        String sourceSymbolName = className + "::" + memberName;
//
//        // simple symbol "holder" class
//        GwtSymbol sourceSymbol = new GwtSymbol(sourcePathBase + sourceFileName,
//                                               Integer.parseInt(sourceLine),
//                                               sourceSymbolName,
//                                               fileName);

            // only register class definitions.
            // also, ignore if the source/dest name are the same, since that doesn't do any good for obfuscated names anyways.
            if (memberName.isEmpty() && !jsName.equals(className)) {
//            System.err.println(jsName + "  :  " + memberName + "  :  " + className);
                this.symbolMap.put(jsName, className);
            }
        } else {
            // version 2
            // The list has already been pruned, so always put everything into the symbol map
            this.symbolMap.put(symbolInfo[0], symbolInfo[1]);
        }
    }
}
