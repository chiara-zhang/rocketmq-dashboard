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
package ${packageUtilName};

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.AllArgsConstructor;
import lombok.Builder;

#foreach($import in ${object.imports})
    #if ($import == "java.util.List")
import $import;
    #end
#end

#if ($list.contains(${object.imports}, "java.util.List"))
import java.util.List
#end

/**
 #foreach ($comment in $object.comments)
 * $comment
 #end
 #if ($object.comments.size() > 0)
 * <BR/>
 *
 #end
 * generated by graphql-java-generator
 * @see <a href="https://github.com/graphql-java-generator/graphql-java-generator">https://github.com/graphql-java-generator/graphql-java-generator</a>
 */
@Value
@NonFinal
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder(toBuilder = true)
public class ${object.javaName} {
#foreach ($field in $object.fields)
    private ${field.javaType} ${field.javaName};
#end
}