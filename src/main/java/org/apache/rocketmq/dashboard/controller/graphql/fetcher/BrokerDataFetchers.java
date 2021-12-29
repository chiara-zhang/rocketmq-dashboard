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

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.common.protocol.body.KVTable;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.dashboard.controller.graphql.codegen.Broker;
import org.apache.rocketmq.dashboard.controller.graphql.codegen.BrokerNode;
import org.apache.rocketmq.dashboard.controller.graphql.codegen.BrokerNodeSummary;
import org.apache.rocketmq.dashboard.controller.graphql.codegen.BrokerType;
import org.apache.rocketmq.dashboard.controller.graphql.codegen.Property;
import org.apache.rocketmq.dashboard.controller.graphql.common.CommonUtils;
import org.apache.rocketmq.dashboard.controller.graphql.directives.ListLengthLimit;
import org.apache.rocketmq.dashboard.controller.graphql.directives.PropertyListFindKey;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class BrokerDataFetchers implements DataFetcher<Broker> {
    private static final String VERSION = "brokerVersionDesc";
    private static final String PRO_MSG_TPS = "putTps";
    private static final String CUS_MSG_TPS = "getTransferedTps";
    private static final String MSG_PUT_TOTAL_TODAY_MORNING = "msgPutTotalTodayMorning";
    private static final String MSG_PUT_TOTAL_YESTERDAY_MORNING = "msgPutTotalYesterdayMorning";
    private static final String MSG_PUT_TOTAL_TODAY_NOW = "msgPutTotalTodayNow";
    private static final String MSG_GET_TOTAL_TODAY_MORNING = "msgGetTotalTodayMorning";
    private static final String MSG_GET_TOTAL_YESTERDAY_MORNING = "msgGetTotalYesterdayMorning";
    private static final String MSG_GET_TOTAL_TODAY_NOW = "msgGetTotalTodayNow";

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private MQAdminExt mqAdminExt;

    @Resource
    private ListLengthLimit<Property> listLengthLimit;

    @Resource
    private PropertyListFindKey propertyListFindKey;

    @Override
    public Broker get(DataFetchingEnvironment dataFetchingEnvironment) {
        return getBroker((String) dataFetchingEnvironment.getArgument("name"));
    }

    public Broker getBroker(String name) {
        BrokerData brokerData = null;
        try {
            brokerData = mqAdminExt.examineBrokerClusterInfo().getBrokerAddrTable().get(name);
            return getBroker(brokerData);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MQBrokerException e) {
            e.printStackTrace();
        } catch (RemotingTimeoutException e) {
            e.printStackTrace();
        } catch (RemotingSendRequestException e) {
            e.printStackTrace();
        } catch (RemotingConnectException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Broker getBroker(BrokerData brokerData) {
        List<BrokerNode> brokerNodes = new ArrayList<>();
        for (Map.Entry<Long, String> node : brokerData.getBrokerAddrs().entrySet()) {
            String address = node.getValue();
            KVTable brokerRuntimeStats = null;
            try {
                brokerRuntimeStats = mqAdminExt.fetchBrokerRuntimeStats(address);
                brokerNodes.add(BrokerNode.builder()
                        .address(address)
                        .type(node.getKey() == 0L ? BrokerType.MASTER : BrokerType.SLAVE)
                        .summary(generateBrokerNodeSummary(brokerRuntimeStats.getTable()))
                        .build());
            } catch (RemotingConnectException e) {
                e.printStackTrace();
            } catch (RemotingSendRequestException e) {
                e.printStackTrace();
            } catch (RemotingTimeoutException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (MQBrokerException e) {
                e.printStackTrace();
            }
        }
        return Broker.builder()
                .name(brokerData.getBrokerName())
                .clusterName(brokerData.getCluster())
                .nodes(brokerNodes)
                .build();
    }

    private BrokerNodeSummary generateBrokerNodeSummary(Map<String, String> brokerRuntimeStats) {
        return BrokerNodeSummary.builder()
                .version(brokerRuntimeStats.get(VERSION))
                .produceMessageTPS(Double.valueOf(brokerRuntimeStats.get(PRO_MSG_TPS).split(" ")[0]))
                .consumerMessageTPS(Double.valueOf(brokerRuntimeStats.get(CUS_MSG_TPS).split(" ")[0]))
                .yesterdayProduceCnt(Integer.valueOf(brokerRuntimeStats.get(MSG_PUT_TOTAL_TODAY_MORNING))
                        - Integer.valueOf(brokerRuntimeStats.get(MSG_PUT_TOTAL_YESTERDAY_MORNING)))
                .yesterdayConsumerCnt(Integer.valueOf(brokerRuntimeStats.get(MSG_GET_TOTAL_TODAY_MORNING))
                        - Integer.valueOf(brokerRuntimeStats.get(MSG_GET_TOTAL_YESTERDAY_MORNING)))
                .todayProduceCnt(Integer.valueOf(brokerRuntimeStats.get(MSG_PUT_TOTAL_TODAY_NOW))
                        - Integer.valueOf(brokerRuntimeStats.get(MSG_PUT_TOTAL_TODAY_MORNING)))
                .todayConsumerCnt(Integer.valueOf(brokerRuntimeStats.get(MSG_GET_TOTAL_TODAY_NOW))
                        - Integer.valueOf(brokerRuntimeStats.get(MSG_GET_TOTAL_TODAY_MORNING)))
                .build();
    }

    public DataFetcher getBrokerNodeConfig() {
        return dataFetchingEnvironment -> {
            BrokerNode node = dataFetchingEnvironment.getSource();
            return listLengthLimit.apply(dataFetchingEnvironment,
                    propertyListFindKey.apply(dataFetchingEnvironment,
                            CommonUtils.mapToProperties(mqAdminExt.getBrokerConfig(node.getAddress()))));
        };
    }

    public DataFetcher getBrokerNodeStatus() {
        return dataFetchingEnvironment -> {
            BrokerNode node = dataFetchingEnvironment.getSource();
            return listLengthLimit.apply(dataFetchingEnvironment,
                    propertyListFindKey.apply(dataFetchingEnvironment,
                            CommonUtils.mapToProperties(mqAdminExt.getBrokerConfig(node.getAddress()))));
        };
    }
}
