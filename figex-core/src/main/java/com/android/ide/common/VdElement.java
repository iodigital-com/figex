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

import org.w3c.dom.NamedNodeMap;

import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Used to represent one VectorDrawble's element, can be a group or path.
 */
abstract class VdElement {
    String mName;

    public String getName() {
        return mName;
    }

    public abstract void draw(Graphics2D g, AffineTransform currentMatrix, float scaleX, float scaleY);

    public abstract void parseAttributes(NamedNodeMap attributes);

    public abstract boolean isGroup();
}