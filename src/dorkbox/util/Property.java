/*
 * Copyright 2010 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to track system properties across the entire code-base. System properties are values that can be changed programmatically,
 * via a "System.getProperty()", or via arguments.
 * <br>
 *  Loading arguments is left for the end-user application. Using an annotation detector to load/save properties is recommended.
 *  <br>
 *  For example (if implemented): -Ddorkbox.Args.Debug=true
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD})
public
@interface Property {

    /**
     * This is used to mark a unique value for the property, in case the object name is used elsewhere, is generic, or is repeated.
     */
    String alias() default "";
}
