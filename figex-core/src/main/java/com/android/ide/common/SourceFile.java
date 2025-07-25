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

import com.google.common.base.Objects;

import java.io.File;

/**
 * Represents a source file.
 */
@javax.annotation.concurrent.Immutable
public final class SourceFile {

    @javax.annotation.Nonnull
    public static final SourceFile UNKNOWN = new SourceFile();

    @javax.annotation.Nullable
    private final File mSourceFile;

    /**
     * A human readable description
     *
     * Usually the file name is OK for the short output, but for the manifest merger,
     * where all of the files will be named AndroidManifest.xml the variant name is more useful.
     */
    @javax.annotation.Nullable
    private final String mDescription;

    @SuppressWarnings("NullableProblems")
    public SourceFile(
            @javax.annotation.Nonnull File sourceFile,
            @javax.annotation.Nonnull String description) {
        mSourceFile = sourceFile;
        mDescription = description;
    }

    public SourceFile(
            @SuppressWarnings("NullableProblems") @javax.annotation.Nonnull File sourceFile) {
        mSourceFile = sourceFile;
        mDescription = null;
    }

    public SourceFile(
            @SuppressWarnings("NullableProblems") @javax.annotation.Nonnull String description) {
        mSourceFile = null;
        mDescription = description;
    }

    private SourceFile() {
        mSourceFile = null;
        mDescription = null;
    }

    @javax.annotation.Nullable
    public File getSourceFile() {
        return mSourceFile;
    }

    @javax.annotation.Nullable
    public String getDescription() {
        return mDescription;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SourceFile)) {
            return false;
        }
        SourceFile other = (SourceFile) obj;

        return Objects.equal(mDescription, other.mDescription) &&
                Objects.equal(mSourceFile, other.mSourceFile);
    }

    @Override
    public int hashCode() {
        String filePath = mSourceFile != null ? mSourceFile.getPath() : null;
        return Objects.hashCode(filePath, mDescription);
    }

    @Override
    @javax.annotation.Nonnull
    public String toString() {
        return print(false /* shortFormat */);
    }

    @javax.annotation.Nonnull
    public String print(boolean shortFormat) {
        if (mSourceFile == null) {
            if (mDescription == null) {
                return "Unknown source file";
            }
            return mDescription;
        }
        String fileName = mSourceFile.getName();
        String fileDisplayName = shortFormat ? fileName : mSourceFile.getAbsolutePath();
        if (mDescription == null || mDescription.equals(fileName)) {
            return fileDisplayName;
        } else {
            return String.format("[%1$s] %2$s", mDescription, fileDisplayName);
        }
    }

}