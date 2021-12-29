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
package org.apache.rocketmq.dashboard.controller.graphql.common;

import org.apache.rocketmq.dashboard.controller.graphql.codegen.Property;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CommonUtils {
    private CommonUtils() {}

    static public <K, V extends Object> List<Property> mapToProperties(Map<K, V> map) {
        return map.entrySet().stream()
                .map(entry -> Property.builder()
                        .key(String.valueOf(entry.getKey()))
                        .value(String.valueOf(entry.getValue()))
                        .build())
                .collect(Collectors.toList());
    }
}
