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

package org.apache.kylin.rest.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.kylin.common.util.Pair;
import org.apache.kylin.rest.service.StreamingCoordinatorService;
import org.apache.kylin.stream.core.model.CubeAssignment;
import org.apache.kylin.stream.core.model.ReplicaSet;
import org.apache.kylin.stream.coordinator.client.CoordinatorResponse;
import org.apache.kylin.stream.coordinator.exception.NotLeadCoordinatorException;
import org.apache.kylin.stream.core.model.Node;
import org.apache.kylin.stream.core.model.RemoteStoreCompleteRequest;
import org.apache.kylin.stream.core.model.ReplicaSetLeaderChangeRequest;
import org.apache.kylin.stream.core.source.Partition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * StreamingCoordinatorController is defined as Restful API entrance for stream coordinator.
 *
 */
@Controller
@RequestMapping(value = "/streaming_coordinator")
public class StreamingCoordinatorController extends BasicController {
    private static final Logger logger = LoggerFactory.getLogger(StreamingCoordinatorController.class);

    @Autowired
    private StreamingCoordinatorService streamingCoordinartorService;

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(NotLeadCoordinatorException.class)
    @ResponseBody
    CoordinatorResponse handleNotLeadCoordinator(HttpServletRequest req, Exception ex) {
        CoordinatorResponse response = new CoordinatorResponse();
        response.setCode(CoordinatorResponse.NOT_LEAD_COORDINATOR);
        response.setMsg(ex.getMessage());
        return response;
    }

    @RequestMapping(value = "/balance/recommend", method = { RequestMethod.GET })
    @ResponseBody
    public CoordinatorResponse reBalanceRecommend() {
        Map<Integer, Map<String, List<Partition>>> result = streamingCoordinartorService.reBalanceRecommend();
        CoordinatorResponse response = new CoordinatorResponse();
        response.setData(result);
        return response;
    }

    @RequestMapping(value = "/balance", method = { RequestMethod.POST })
    @ResponseBody
    public CoordinatorResponse reBalance(@RequestBody String reBalancePlanStr) {
        Map<Integer, Map<String, List<Partition>>> reBalancePlan = deserializeRebalancePlan(reBalancePlanStr);
        streamingCoordinartorService.reBalance(reBalancePlan);
        return new CoordinatorResponse();
    }

    private Map<Integer, Map<String, List<Partition>>> deserializeRebalancePlan(String reBalancePlanStr) {
        TypeReference<Map<Integer, Map<String, List<Partition>>>> typeRef = new TypeReference<Map<Integer, Map<String, List<Partition>>>>() {
        };
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(reBalancePlanStr, typeRef);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @RequestMapping(value = "/cubes/{cubeName}/assign", method = { RequestMethod.PUT }, produces = {
            "application/json" })
    @ResponseBody
    public CoordinatorResponse assignStreamingCube(@PathVariable String cubeName) {
        streamingCoordinartorService.assignCube(cubeName);
        return new CoordinatorResponse();
    }

    @RequestMapping(value = "/cubes/{cubeName}/unAssign", method = { RequestMethod.PUT }, produces = {
            "application/json" })
    @ResponseBody
    public CoordinatorResponse unAssignStreamingCube(@PathVariable String cubeName) {
        streamingCoordinartorService.unAssignCube(cubeName);
        return new CoordinatorResponse();
    }

    @RequestMapping(value = "/cubes/{cubeName}/reAssign", method = { RequestMethod.POST }, produces = {
            "application/json" })
    @ResponseBody
    public CoordinatorResponse reAssignStreamingCube(@PathVariable String cubeName,
            @RequestBody CubeAssignment newAssignments) {
        streamingCoordinartorService.reAssignCube(cubeName, newAssignments);
        return new CoordinatorResponse();
    }

    @RequestMapping(value = "/replicaSet", method = { RequestMethod.POST }, produces = { "application/json" })
    @ResponseBody
    public CoordinatorResponse createReplicaSet(@RequestBody ReplicaSet rs) {
        streamingCoordinartorService.createReplicaSet(rs);
        return new CoordinatorResponse();
    }

    @RequestMapping(value = "/replicaSet/{replicaSetID}", method = { RequestMethod.DELETE }, produces = {
            "application/json" })
    @ResponseBody
    public CoordinatorResponse deleteReplicaSet(@PathVariable Integer replicaSetID) {
        streamingCoordinartorService.removeReplicaSet(replicaSetID);
        return new CoordinatorResponse();
    }

    @RequestMapping(value = "/replicaSet/{replicaSetID}/{nodeID:.+}", method = { RequestMethod.PUT }, produces = {
            "application/json" })
    @ResponseBody
    public CoordinatorResponse addNodeToReplicaSet(@PathVariable Integer replicaSetID, @PathVariable String nodeID) {
        streamingCoordinartorService.addNodeToReplicaSet(replicaSetID, nodeID);
        return new CoordinatorResponse();
    }

    @RequestMapping(value = "/replicaSet/{replicaSetID}/{nodeID:.+}", method = { RequestMethod.DELETE }, produces = {
            "application/json" })
    @ResponseBody
    public CoordinatorResponse removeNodeFromReplicaSet(@PathVariable Integer replicaSetID,
            @PathVariable String nodeID) {
        streamingCoordinartorService.removeNodeFromReplicaSet(replicaSetID, nodeID);
        return new CoordinatorResponse();
    }

    @RequestMapping(value = "/cubes/{cubeName}/pauseConsume", method = { RequestMethod.PUT }, produces = {
            "application/json" })
    @ResponseBody
    public CoordinatorResponse pauseCubeConsume(@PathVariable String cubeName) {
        streamingCoordinartorService.pauseConsumers(cubeName);
        return new CoordinatorResponse();
    }

    @RequestMapping(value = "/cubes/{cubeName}/resumeConsume", method = { RequestMethod.PUT }, produces = {
            "application/json" })
    @ResponseBody
    public CoordinatorResponse resumeCubeConsume(@PathVariable String cubeName) {
        streamingCoordinartorService.resumeConsumers(cubeName);
        return new CoordinatorResponse();
    }

    @RequestMapping(value = "/remoteStoreComplete", method = { RequestMethod.POST }, produces = { "application/json" })
    @ResponseBody
    public CoordinatorResponse segmentRemoteStoreComplete(@RequestBody RemoteStoreCompleteRequest request) {
        Pair<Long, Long> segmentRange = new Pair<>(request.getSegmentStart(), request.getSegmentEnd());
        Node receiver = request.getReceiverNode();
        logger.info(
                "receive segment remote store complete request for cube:{}, segment:{}, try to find proper segment to build",
                request.getCubeName(), segmentRange);
        streamingCoordinartorService.onSegmentRemoteStoreComplete(request.getCubeName(), segmentRange, receiver);
        return new CoordinatorResponse();
    }

    @RequestMapping(value = "/replicaSetLeaderChange", method = { RequestMethod.POST }, produces = {
            "application/json" })
    @ResponseBody
    public CoordinatorResponse replicaSetLeaderChange(@RequestBody ReplicaSetLeaderChangeRequest request) {
        logger.info("receive replicaSet leader change:" + request);
        streamingCoordinartorService.replicaSetLeaderChange(request.getReplicaSetID(), request.getNewLeader());
        return new CoordinatorResponse();
    }
}
