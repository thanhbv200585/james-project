/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.mailet;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

public class FutureReleaseParameters {
    public static final String HOLDFOR_PARAMETER = "HOLDFOR";
    public static final String HOLDUNITL_PARAMETER = "HOLDUNITL";
    public static final long holdforValue = 604800;
    /**
     *  The HOLDFOR parameter value is a future-release-interval, which is
     *       a positive integer indicating the amount of time the message is to
     *       be held by the MSA before release.
     *
     *
     * https://www.rfc-editor.org/rfc/rfc4865.html
     **/
    public static class Holdfor {
        public static Optional<Holdfor> fromSMTPArgLine(Map<String, Long> mailFromArgLine) {
            return Optional.ofNullable(mailFromArgLine.get(HOLDFOR_PARAMETER))
                .map(Holdfor::of);
        }

        public static Holdfor fromAttributeValue(AttributeValue<Long> attributeValue) {
            return of(attributeValue.value());
        }

        private final Long value;

        public static Holdfor of (Long value) {
            Preconditions.checkNotNull(value);
//            Preconditions.checkArgument(XText.isValid(value), "According to RFC-4865 Holdfor should be a valid xtext" +
//                ", thus composed of CHARs between \"!\" (33) and \"~\" (126) inclusive, except for \"+\" and \"=\" or follow the hexadecimal escape sequence.");
            return new Holdfor(value);
        }

        private Holdfor(Long value) {
            this.value = value;
        }

        public Long asString() {
            return value;
        }

        public AttributeValue<Long> toAttributeValue() {
            return AttributeValue.of(value);
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof Holdfor) {
                Holdfor that = (Holdfor) o;

                return Objects.equals(this.value, that.value);
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("value", value)
                .toString();
        }
    }

        /**
     *   The HOLDUNTIL parameter value is a future-release-date-time, which
     *       is a timestamp, normalized to UTC, indicating the future date and
     *       time until which the message is to be held by the MSA before
     *       release.
     *
     * https://www.rfc-editor.org/rfc/rfc4865.html
     */
    public static class Holduntil {
        public static Optional<Holduntil> fromSMTPArgLine(Map<String, String> mailFromArgLine) {
            return Optional.ofNullable(mailFromArgLine.get(HOLDUNITL_PARAMETER))
                .map(Holduntil::of);
        }

        public static Holduntil fromAttributeValue(AttributeValue<String> attributeValue) {
            return of(attributeValue.value());
        }

        private final String value;

        private Holduntil(String value) {
            this.value = value;
        }

        public static Holduntil of (String value) {
            Preconditions.checkNotNull(value);
//            Preconditions.checkArgument(XText.isValid(value), "According to RFC-4865 Holdfor should be a valid xtext" +
//                ", thus composed of CHARs between \"!\" (33) and \"~\" (126) inclusive, except for \"+\" and \"=\" or follow the hexadecimal escape sequence.");
            return new Holduntil(value);
        }

        public String asString() {
            return value;
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof Holduntil) {
                Holduntil that = (Holduntil) o;
                return Objects.equals(this.value, that.value);
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
               .add("value", value)
               .toString();
        }

        public AttributeValue<String> toAttributeValue() {
            return AttributeValue.of(value);
        }
    }
}
