/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.dashboard.controller.graphql.fetcher;

import com.google.common.collect.Lists;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.admin.TopicStatsTable;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.dashboard.controller.graphql.codegen.Broker;
import org.apache.rocketmq.dashboard.controller.graphql.codegen.QueueRoute;
import org.apache.rocketmq.dashboard.controller.graphql.codegen.QueueStatus;
import org.apache.rocketmq.dashboard.controller.graphql.codegen.Topic;
import org.apache.rocketmq.dashboard.controller.graphql.codegen.TopicMessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class TopicDataFetchers implements DataFetcher<List<Topic>> {
    private static final String NAME = "name";
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private MQAdminExt mqAdminExt;

    @Resource
    private BrokerDataFetchers brokerDataFetchers;

    private Topic getTopic(String name) {
        return Topic.builder()
                .name(name)
                .messageQueues(TopicMessageQueue.builder()
                        .topic(name)
                        .build())
                .build();
    }

    private List<Topic> getTopics() throws RemotingException, MQClientException, InterruptedException {
        TopicList allTopics = mqAdminExt.fetchAllTopicList();
        return allTopics.getTopicList().stream()
                .map(this::getTopic)
                .collect(Collectors.toList());
    }

    @Override
    public List<Topic> get(DataFetchingEnvironment dataFetchingEnvironment)
            throws RemotingException, MQClientException, InterruptedException {
        String name = dataFetchingEnvironment.getArgument(NAME);
        return Objects.nonNull(name) ? Lists.newArrayList(getTopic(name)) : getTopics();
    }

    private List<QueueStatus> geQueueStatus(String topic)
            throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        TopicStatsTable topicStatsTable = mqAdminExt.examineTopicStats(topic);
        if (Objects.nonNull(topicStatsTable)) {
            return topicStatsTable.getOffsetTable()
                    .entrySet()
                    .stream()
                    .map(entry ->
                            QueueStatus.builder()
                                    .queueId(entry.getKey().getQueueId())
                                    .brokerName(entry.getKey().getBrokerName())
                                    .lastUpdateTimeStamp(entry.getValue().getLastUpdateTimestamp())
                                    .maxOffset(entry.getValue().getMaxOffset())
                                    .minOffset(entry.getValue().getMinOffset())
                                    .build())
                    .collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    private List<QueueRoute> getQueueRoute(String topic)
            throws RemotingException, MQClientException, InterruptedException {
        TopicRouteData topicRouteData = mqAdminExt.examineTopicRouteInfo(topic);
        if (Objects.nonNull(topicRouteData)) {
            return topicRouteData.getQueueDatas()
                    .stream()
                    .map(queueData ->
                            QueueRoute.builder()
                                    .brokerName(queueData.getBrokerName())
                                    .readQueueNums(queueData.getReadQueueNums())
                                    .writeQueueNums(queueData.getWriteQueueNums())
                                    .perm(queueData.getPerm())
                                    .topicSysFlag(queueData.getTopicSysFlag())
                                    .build())
                    .collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    private List<Broker> getBrokers(String topic)
            throws RemotingException, MQClientException, InterruptedException {
        TopicRouteData topicRouteData = mqAdminExt.examineTopicRouteInfo(topic);
        if (Objects.nonNull(topicRouteData)) {
            return topicRouteData.getBrokerDatas()
                    .stream()
                    .map(brokerData ->
                            brokerDataFetchers.getBroker(brokerData))
                    .collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    public DataFetcher geQueueStatus() {
        return dataFetchingEnvironment -> {
            TopicMessageQueue topicMessageQueue = dataFetchingEnvironment.getSource();
            return geQueueStatus(topicMessageQueue.getTopic());
        };
    }

    public DataFetcher getQueueRoute() {
        return dataFetchingEnvironment -> {
            TopicMessageQueue topicMessageQueue = dataFetchingEnvironment.getSource();
            return getQueueRoute(topicMessageQueue.getTopic());
        };
    }

    public DataFetcher getBrokers() {
        return dataFetchingEnvironment -> {
            TopicMessageQueue topicMessageQueue = dataFetchingEnvironment.getSource();
            return getBrokers(topicMessageQueue.getTopic());
        };
    }
}
