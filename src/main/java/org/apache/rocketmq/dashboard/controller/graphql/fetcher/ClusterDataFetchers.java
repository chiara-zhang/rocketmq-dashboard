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
import org.apache.rocketmq.dashboard.controller.graphql.codegen.Cluster;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ClusterDataFetchers implements DataFetcher<List<Cluster>> {
    private static final String NAME = "name";

    @Resource
    private MQAdminExt mqAdminExt;

    /**
     *
     * @param name
     * @return clusterAddrTable -- clusterName to set of brokerNames
     * @throws InterruptedException
     * @throws RemotingConnectException
     * @throws RemotingTimeoutException
     * @throws RemotingSendRequestException
     * @throws MQBrokerException
     */
    private Cluster getCluster(String name)
            throws InterruptedException, RemotingConnectException, RemotingTimeoutException,
            RemotingSendRequestException, MQBrokerException {
        Map<String, Set<String>> clusterAddrTable = mqAdminExt.examineBrokerClusterInfo().getClusterAddrTable();
        return Cluster.builder()
                .name(name)
                .brokers(Lists.newArrayList(clusterAddrTable.get(name)))
                .build();
    }

    private List<Cluster> getClusters()
            throws InterruptedException, RemotingConnectException, RemotingTimeoutException,
            RemotingSendRequestException, MQBrokerException {
        Map<String, Set<String>> clusterAddrTable = mqAdminExt.examineBrokerClusterInfo().getClusterAddrTable();
        return clusterAddrTable
                .keySet()
                .stream()
                .map(name -> Cluster.builder()
                        .name(name)
                        .brokers(Lists.newArrayList(clusterAddrTable.get(name)))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<Cluster> get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
        String name = dataFetchingEnvironment.getArgument(NAME);
        return Objects.nonNull(name) ? Lists.newArrayList(getCluster(name)) : getClusters();
    }
}
