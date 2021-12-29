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
package org.apache.rocketmq.dashboard.controller.graphql.directives;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetcherFactories;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import org.apache.rocketmq.dashboard.controller.graphql.codegen.Property;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PropertyListFindKey implements SchemaDirectiveWiring, FieldDirective<List<Property>> {
    private static final String FIND = "find";
    private static final String FIND_KEY = "key";

    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
        String key = (String) environment.getDirective().getArgument(FIND_KEY).getValue();
        GraphQLFieldDefinition field = environment.getElement();
        GraphQLFieldsContainer parentType = environment.getFieldsContainer();
        DataFetcher originalFetcher = environment.getCodeRegistry().getDataFetcher(parentType, field);
        DataFetcher dataFetcher = DataFetcherFactories.wrapDataFetcher(originalFetcher, (dataFetchingEnvironment, value) -> {
            if (value instanceof List
                    && !((List) value).isEmpty()
                    && ((List) value).get(0) instanceof Property) {
                return ((List<Property>) value)
                        .stream()
                        .filter(v -> v.getKey().equals(key))
                        .collect(Collectors.toList());
            }
            return value;
        });
        environment.getCodeRegistry().dataFetcher(parentType, field, dataFetcher);
        return field;
    }

    @Override
    public List<Property> apply(DataFetchingEnvironment dataFetchingEnvironment, List<Property> properties) {
        Set<String> keys = dataFetchingEnvironment.getQueryDirectives().getImmediateDirective(FIND)
                .stream()
                .map(GraphQLDirective::getArguments)
                .flatMap(List::stream)
                .map(graphQLArgument -> (List<String>) graphQLArgument.getValue())
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        if (!keys.isEmpty()) {
            return properties.stream()
                    .filter(property -> keys.contains(property.getKey()))
                    .collect(Collectors.toList());
        }
        return properties;
    }
}
