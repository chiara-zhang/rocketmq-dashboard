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
package org.apache.rocketmq.dashboard.controller.graphql;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.rocketmq.dashboard.controller.graphql.directives.ListLengthLimit;
import org.apache.rocketmq.dashboard.controller.graphql.fetcher.BrokerDataFetchers;
import org.apache.rocketmq.dashboard.controller.graphql.fetcher.ClusterDataFetchers;
import org.apache.rocketmq.dashboard.controller.graphql.fetcher.TopicDataFetchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.net.URL;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class GraphQLProvider {
    private Logger logger = LoggerFactory.getLogger(GraphQLProvider.class);
    private GraphQL graphQL;

    @Resource
    ClusterDataFetchers clusterDataFetchers;

    @Resource
    BrokerDataFetchers brokerDataFetchers;

    @Resource
    TopicDataFetchers topicDataFetchers;

    @Bean
    public GraphQL graphQL() {
        return graphQL;
    }

    @PostConstruct
    public void init() throws IOException {
        URL url = Resources.getResource("graphql/schema.graphqls");
        String sdl = Resources.toString(url, Charsets.UTF_8);
        GraphQLSchema graphQLSchema = buildSchema(sdl);
        this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    private GraphQLSchema buildSchema(String sdl) {
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
        RuntimeWiring runtimeWiring = buildWiring();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    }

    private RuntimeWiring buildWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .scalar(Scalars.GraphQLLong)
                .directive("length", new ListLengthLimit())
                .type(newTypeWiring("Query")
                        .dataFetcher("clusters", clusterDataFetchers)
                        .dataFetcher("broker", brokerDataFetchers)
                        .dataFetcher("topic", topicDataFetchers))
                .type(newTypeWiring("BrokerNode")
                        .dataFetcher("config", brokerDataFetchers.getBrokerNodeConfig())
                        .dataFetcher("status", brokerDataFetchers.getBrokerNodeStatus()))
                .type(newTypeWiring("TopicMessageQueue")
                        .dataFetcher("brokers", topicDataFetchers.getBrokers())
                        .dataFetcher("queueStatus", topicDataFetchers.geQueueStatus())
                        .dataFetcher("queueRoute", topicDataFetchers.getQueueRoute()))
                .build();
    }
}
