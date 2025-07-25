/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.common;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
/**
 * Parse a VectorDrawble's XML file, and generate an internal tree representation,
 * which can be used for drawing / previewing.
 */
class VdParser {
    private static Logger logger = Logger.getLogger(VdParser.class.getSimpleName());

    // Note that the incoming file is the VectorDrawable's XML file, not the SVG.
    @javax.annotation.Nullable
    public VdTree parse(@javax.annotation.Nonnull InputStream is, @javax.annotation.Nullable StringBuilder vdErrorLog) {
        final VdTree tree = new VdTree();
        try {
            Document doc = PositionXmlParser.parse(is, false);
            tree.parse(doc);
        }
        catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        catch (SAXException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return tree;
    }

    public VdTree parse(URL r, StringBuilder vdErrorLog) throws IOException {
        return parse(r.openStream(), vdErrorLog);
    }


}