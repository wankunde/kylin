/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.apache.kylin.stream.core.storage.columnar;

import java.io.File;
import java.util.List;

public class FragmentsMergeResult {
    private List<DataSegmentFragment> origFragments;
    private FragmentId mergedFragmentId;
    private File mergedFragmentMetaFile;
    private File mergedFragmentDataFile;

    public FragmentsMergeResult(List<DataSegmentFragment> origFragments, FragmentId mergedFragmentId,
            File mergedFragmentMetaFile, File mergedFragmentDataFile) {
        this.origFragments = origFragments;
        this.mergedFragmentId = mergedFragmentId;
        this.mergedFragmentMetaFile = mergedFragmentMetaFile;
        this.mergedFragmentDataFile = mergedFragmentDataFile;
    }

    public FragmentId getMergedFragmentId() {
        return mergedFragmentId;
    }

    public File getMergedFragmentMetaFile() {
        return mergedFragmentMetaFile;
    }

    public File getMergedFragmentDataFile() {
        return mergedFragmentDataFile;
    }

    public List<DataSegmentFragment> getOrigFragments() {
        return origFragments;
    }
}
