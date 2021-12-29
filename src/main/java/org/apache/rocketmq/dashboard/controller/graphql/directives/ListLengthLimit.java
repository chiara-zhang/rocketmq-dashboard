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
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class ListLengthLimit<T> implements SchemaDirectiveWiring, FieldDirective<List<T>> {
    private static final String LENGTH = "length";
    private static final String LENGTH_LIMIT = "limit";

    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
        Integer limitLength = (Integer) environment.getDirective().getArgument(LENGTH_LIMIT).getValue();
        GraphQLFieldDefinition field = environment.getElement();
        GraphQLFieldsContainer parentType = environment.getFieldsContainer();
        DataFetcher originalFetcher = environment.getCodeRegistry().getDataFetcher(parentType, field);
        DataFetcher dataFetcher = DataFetcherFactories.wrapDataFetcher(originalFetcher, (dataFetchingEnvironment, value) -> {
            if (value instanceof List) {
                return ((List) value).subList(0, Math.min(limitLength, ((List) value).size()));
            }
            return value;
        });
        environment.getCodeRegistry().dataFetcher(parentType, field, dataFetcher);
        return field;
    }

    @Override
    public List<T> apply(DataFetchingEnvironment dataFetchingEnvironment, List<T> list) {
        Optional<Integer> limitLength = dataFetchingEnvironment.getQueryDirectives().getImmediateDirective(LENGTH)
                .stream()
                .map(GraphQLDirective::getArguments)
                .flatMap(List::stream)
                .filter(graphQLArgument -> graphQLArgument.getName().equals(LENGTH_LIMIT))
                .map(graphQLArgument -> (Integer) graphQLArgument.getValue())
                .min(Comparator.comparing(Integer::intValue));
        if (limitLength.isPresent()) {
            return list.subList(0, Math.min(limitLength.get(), list.size()));
        }
        return list;
    }
}
