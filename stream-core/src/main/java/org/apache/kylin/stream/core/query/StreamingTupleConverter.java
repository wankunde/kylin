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

package org.apache.kylin.stream.core.query;

import java.util.List;

import org.apache.kylin.measure.MeasureType;
import org.apache.kylin.measure.MeasureType.IAdvMeasureFiller;
import org.apache.kylin.metadata.model.FunctionDesc;
import org.apache.kylin.metadata.model.TblColRef;
import org.apache.kylin.metadata.tuple.Tuple;
import org.apache.kylin.metadata.tuple.TupleInfo;
import org.apache.kylin.stream.core.storage.Record;

import com.google.common.collect.Lists;

/**
 * Convert Streaming Record to Tuple
 * 
 */
public class StreamingTupleConverter {
    final TupleInfo tupleInfo;

    final int[] dimTupleIdx;
    final int[] metricsTupleIdx;
    final int dimCnt;
    final int metricsCnt;
    final MeasureType<?>[] measureTypes;

    final List<MeasureType.IAdvMeasureFiller> advMeasureFillers;
    final List<Integer> advMeasureIndexInGTValues;


    public StreamingTupleConverter(ResponseResultSchema schema, TupleInfo returnTupleInfo) {
        this.tupleInfo = returnTupleInfo;
        dimCnt = schema.getDimensionCount();
        metricsCnt = schema.getMetricsCount();
        dimTupleIdx = new int[dimCnt];
        metricsTupleIdx = new int[metricsCnt];

        // measure types don't have this many, but aligned length make programming easier
        measureTypes = new MeasureType[metricsCnt];

        advMeasureFillers = Lists.newArrayListWithCapacity(1);
        advMeasureIndexInGTValues = Lists.newArrayListWithCapacity(1);

        int idx = 0;
        // pre-calculate dimension index mapping to tuple
        for (TblColRef dim : schema.getDimensions()) {
            dimTupleIdx[idx] = tupleInfo.hasColumn(dim) ? tupleInfo.getColumnIndex(dim) : -1;
            idx++;
        }

        idx = 0;
        for (FunctionDesc metric : schema.getMetrics()) {
            if (metric.needRewrite()) {
                String rewriteFieldName = metric.getRewriteFieldName();
                metricsTupleIdx[idx] = tupleInfo.hasField(rewriteFieldName) ? tupleInfo.getFieldIndex(rewriteFieldName) : -1;
            } else { // a non-rewrite metrics (like sum, or dimension playing as metrics) is like a dimension column
                TblColRef col = metric.getParameter().getColRefs().get(0);
                metricsTupleIdx[idx] = tupleInfo.hasColumn(col) ? tupleInfo.getColumnIndex(col) : -1;
            }

            MeasureType<?> measureType = metric.getMeasureType();
            if (measureType.needAdvancedTupleFilling()) {
                advMeasureFillers.add(measureType.getAdvancedTupleFiller(metric, returnTupleInfo, null));
                advMeasureIndexInGTValues.add(idx);
            } else {
                measureTypes[idx] = measureType;
            }
            idx++;
        }
    }

    public List<IAdvMeasureFiller> translateResult(Record record, Tuple tuple) {
        // dimensions
        String[] dimValues = record.getDimensions();
        Object[] metricsValues = record.getMetrics();
        for (int i = 0; i < dimCnt; i++) {
            int ti = dimTupleIdx[i];
            if (ti >= 0) {
                tuple.setDimensionValue(ti, dimValues[i]);
            }
        }

        // measures
        for (int i = 0; i < metricsCnt; i++) {
            int ti = metricsTupleIdx[i];
            if (ti >= 0 && measureTypes[i] != null) {
                measureTypes[i].fillTupleSimply(tuple, ti, metricsValues[i]);
            }
        }

        // advanced measure filling, due to possible row split, will complete at caller side
        if (advMeasureFillers.isEmpty()) {
            return null;
        } else {
            for (int i = 0; i < advMeasureFillers.size(); i++) {
                Object measureValue = metricsValues[advMeasureIndexInGTValues.get(i)];
                advMeasureFillers.get(i).reload(measureValue);
            }
            return advMeasureFillers;
        }
    }
}
