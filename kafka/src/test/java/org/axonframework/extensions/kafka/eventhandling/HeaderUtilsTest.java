/*
 * Copyright (c) 2010-2018. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.extensions.kafka.eventhandling;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.messaging.MetaData;
import org.axonframework.serialization.SerializedObject;
import org.axonframework.serialization.SimpleSerializedType;
import org.junit.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.axonframework.eventhandling.GenericEventMessage.asEventMessage;
import static org.axonframework.extensions.kafka.eventhandling.HeaderUtils.*;
import static org.axonframework.extensions.kafka.eventhandling.util.HeaderAssertUtil.assertDomainHeaders;
import static org.axonframework.extensions.kafka.eventhandling.util.HeaderAssertUtil.assertEventHeaders;
import static org.axonframework.messaging.Headers.MESSAGE_METADATA;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link HeaderUtils}.
 *
 * @author Nakul Mishra
 */
public class HeaderUtilsTest {

    @Test
    public void testReadingValueAsBytesExistingKeyShouldReturnBytes() {
        RecordHeaders headers = new RecordHeaders();
        String value = "a1b2";
        addHeader(headers, "bar", value);

        assertThat(value(headers, "bar")).isEqualTo(value.getBytes());
    }

    @Test
    public void testReadingValuesAsBytesNonExistingKeyShouldReturnNull() {
        assertThat(value(new RecordHeaders(), "123")).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadingValueFromNullHeaderShouldThrowException() {
        value(null, "bar");
    }

    @Test
    public void testReadingValuesAsTextExistingKeyShouldReturnText() {
        RecordHeaders headers = new RecordHeaders();
        String expectedValue = "Şơм℮ śẩмρŀę ÅŚÇÍỈ-ťęҳť FFlETYeKU3H5QRqw";
        addHeader(headers, "foo", expectedValue);

        assertThat(valueAsString(headers, "foo")).isEqualTo(expectedValue);
        assertThat(valueAsString(headers, "foo", "default-value")).isEqualTo(expectedValue);
    }

    @Test
    public void testReadingValueAsTextNonExistingKeyShouldReturnNull() {
        assertThat(valueAsString(new RecordHeaders(), "some-invalid-key")).isNull();
    }

    @Test
    public void testReadingValueAsTextNonExistingKeyShouldReturnDefaultValue() {
        assertThat(valueAsString(new RecordHeaders(), "some-invalid-key", "default-value")).isEqualTo("default-value");
    }

    @Test
    public void testReadingValuesAsLongExistingKeyShouldReturnLong() {
        RecordHeaders headers = new RecordHeaders();
        addHeader(headers, "positive", 4_891_00_921_388_62621L);
        addHeader(headers, "zero", 0L);
        addHeader(headers, "negative", -4_8912_00_921_388_62621L);

        assertThat(valueAsLong(headers, "positive")).isEqualTo(4_891_00_921_388_62621L);
        assertThat(valueAsLong(headers, "zero")).isZero();
        assertThat(valueAsLong(headers, "negative")).isEqualTo(-4_8912_00_921_388_62621L);
    }

    @Test
    public void testReadingValueAsLongNonExistingKeyShouldReturnNull() {
        assertThat(valueAsLong(new RecordHeaders(), "some-invalid-key")).isNull();
    }

    @Test
    public void testWritingTimestampShouldBeWrittenAsLong() {
        RecordHeaders target = new RecordHeaders();
        Instant value = Instant.now();
        addHeader(target, "baz", value);

        assertThat(valueAsLong(target, "baz")).isEqualTo(value.toEpochMilli());
    }

    @Test
    public void testWritingNonNegativeValuesShouldBeWrittenAsNonNegativeValues() {
        RecordHeaders target = new RecordHeaders();
        short expectedShort = 1;
        int expectedInt = 200;
        long expectedLong = 300L;
        float expectedFloat = 300.f;
        double expectedDouble = 0.000;

        addHeader(target, "short", expectedShort);
        assertThat(shortValue(target)).isEqualTo(expectedShort);

        addHeader(target, "int", expectedInt);
        assertThat(intValue(target)).isEqualTo(expectedInt);

        addHeader(target, "long", expectedLong);
        assertThat(longValue(target)).isEqualTo(expectedLong);

        addHeader(target, "float", expectedFloat);
        assertThat(floatValue(target)).isEqualTo(expectedFloat);

        addHeader(target, "double", expectedDouble);
        assertThat(doubleValue(target)).isEqualTo(expectedDouble);
    }

    @Test
    public void testWritingNegativeValuesShouldBeWrittenAsNegativeValues() {
        RecordHeaders target = new RecordHeaders();
        short expectedShort = -123;
        int expectedInt = -1_234_567_8;
        long expectedLong = -1_234_567_89_0L;
        float expectedFloat = -1_234_567_89_0.0f;
        double expectedDouble = -1_234_567_89_0.987654321;

        addHeader(target, "short", expectedShort);
        assertThat(shortValue(target)).isEqualTo(expectedShort);

        addHeader(target, "int", expectedInt);
        assertThat(intValue(target)).isEqualTo(expectedInt);

        addHeader(target, "long", expectedLong);
        assertThat(longValue(target)).isEqualTo(expectedLong);

        addHeader(target, "float", expectedFloat);
        assertThat(floatValue(target)).isEqualTo(expectedFloat);

        addHeader(target, "double", expectedDouble);
        assertThat(doubleValue(target)).isEqualTo(expectedDouble);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWritingNonPrimitiveJavaValueShouldThrowAnException() {
        addHeader(new RecordHeaders(), "short", BigInteger.ZERO);
    }

    @Test
    public void testWritingTextValueShouldBeWrittenAsString() {
        RecordHeaders target = new RecordHeaders();
        String expectedKey = "foo";
        String expectedValue = "a";
        addHeader(target, expectedKey, expectedValue);

        assertThat(target.toArray().length).isEqualTo(1);
        assertThat(target.lastHeader(expectedKey).key()).isEqualTo(expectedKey);
        assertThat(valueAsString(target, expectedKey)).isEqualTo(expectedValue);
    }

    @Test
    public void testWritingNullValueShouldBeWrittenAsNull() {
        RecordHeaders target = new RecordHeaders();
        addHeader(target, "baz", null);

        assertThat(value(target, "baz")).isNull();
    }

    @Test
    public void testWritingCustomValueShouldBeWrittenAsRepresentedByToString() {
        RecordHeaders target = new RecordHeaders();
        Foo expectedValue = new Foo("someName", new Bar(100));
        addHeader(target, "object", expectedValue);

        assertThat(valueAsString(target, "object")).isEqualTo(expectedValue.toString());
    }

    @Test
    public void testExtractingExistingKeysShouldReturnAllKeys() {
        RecordHeaders target = new RecordHeaders();
        addHeader(target, "a", "someValue");
        addHeader(target, "b", "someValue");
        addHeader(target, "c", "someValue");
        Set<String> expectedKeys = new HashSet<>();
        target.forEach(header -> expectedKeys.add(header.key()));

        assertThat(keys(target)).isEqualTo(expectedKeys);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractingKeysFromNullHeaderShouldThrowAnException() {
        keys(null);
    }

    @Test
    public void testGeneratingKeyForSendingAxonMetadataToKafkaShouldGenerateCorrectKeys() {
        assertThat(generateMetadataKey("foo")).isEqualTo(MESSAGE_METADATA + "-foo");
        assertThat(generateMetadataKey(null)).isEqualTo(MESSAGE_METADATA + "-null");
    }

    @Test
    public void testExtractingKeyForSendingAxonMetadataToKafkaShouldReturnActualKey() {
        assertThat(extractKey(generateMetadataKey("foo"))).isEqualTo("foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractingKeyNullMetadataKeyShouldThrowAnException() {
        extractKey(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractingKeyNonExistingMetadataKeyShouldThrowAnException() {
        extractKey("foo-bar-axon-metadata");
    }

    @Test
    public void testExtractingAxonMetadataShouldReturnMetadata() {
        RecordHeaders target = new RecordHeaders();
        String key = generateMetadataKey("headerKey");
        String value = "abc";
        Map<String, Object> expectedValue = new HashMap<>();
        expectedValue.put("headerKey", value);
        addHeader(target, key, value);

        assertThat(extractAxonMetadata(target)).isEqualTo(expectedValue);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractingAxonMetadataFromNullHeaderShouldThrowAnException() {
        extractAxonMetadata(null);
    }

    @Test
    public void testGeneratingHeadersForEventMessageShouldGenerateEventHeaders() {
        String metaKey = "someHeaderKey";
        EventMessage<Object> evt = asEventMessage("SomePayload").withMetaData(
                MetaData.with(metaKey, "someValue")
        );
        SerializedObject<byte[]> so = serializedObject();
        Headers headers = toHeaders(evt, so, byteMapper());

        assertEventHeaders(metaKey, evt, so, headers);
    }

    @Test
    public void testGeneratingHeadersForDomainMessageShouldGenerateBothEventAndDomainHeaders() {
        String metaKey = "someHeaderKey";
        DomainEventMessage<Object> evt =
                new GenericDomainEventMessage<>("Stub", "axc123-v", 1L, "Payload", MetaData.with("key", "value"));
        SerializedObject<byte[]> so = serializedObject();
        Headers headers = toHeaders(evt, so, byteMapper());

        assertEventHeaders(metaKey, evt, so, headers);
        assertDomainHeaders(evt, headers);
    }

    @Test
    public void testByteMapperNullValueShouldBeAbleToHandle() {
        BiFunction<String, Object, RecordHeader> fxn = byteMapper();
        RecordHeader header = fxn.apply("abc", null);

        assertThat(header.value()).isNull();
    }

    @Test
    public void testGeneratingHeadersWithByteMapperShouldGenerateCorrectHeaders() {
        BiFunction<String, Object, RecordHeader> fxn = byteMapper();
        String expectedKey = "abc";
        String expectedValue = "xyz";
        RecordHeader header = fxn.apply(expectedKey, expectedValue);

        assertThat(header.key()).isEqualTo(expectedKey);
        assertThat(new String(header.value())).isEqualTo(expectedValue);
    }

    @Test
    public void testGeneratingHeadersWithCustomMapperShouldGeneratedCorrectHeaders() {
        String metaKey = "someHeaderKey";
        String expectedMetaDataValue = "evt:someValue";
        Headers header = toHeaders(
                asEventMessage("SomePayload").withMetaData(MetaData.with(metaKey, "someValue")),
                serializedObject(),
                (key, value) -> new RecordHeader(key, ("evt:" + value.toString()).getBytes())
        );

        assertThat(valueAsString(header, generateMetadataKey(metaKey))).isEqualTo(expectedMetaDataValue);
    }

    private static double doubleValue(RecordHeaders target) {
        return ByteBuffer.wrap(Objects.requireNonNull(value(target, "double"))).getDouble();
    }

    private static float floatValue(RecordHeaders target) {
        return ByteBuffer.wrap(Objects.requireNonNull(value(target, "float"))).getFloat();
    }

    private static long longValue(RecordHeaders target) {
        return ByteBuffer.wrap(Objects.requireNonNull(value(target, "long"))).getLong();
    }

    private static int intValue(RecordHeaders target) {
        return ByteBuffer.wrap(Objects.requireNonNull(value(target, "int"))).getInt();
    }

    private static short shortValue(RecordHeaders target) {
        return ByteBuffer.wrap(Objects.requireNonNull(value(target, "short"))).getShort();
    }

    @SuppressWarnings("unchecked")
    private static SerializedObject<byte[]> serializedObject() {
        SerializedObject serializedObject = mock(SerializedObject.class);
        when(serializedObject.getType()).thenReturn(new SimpleSerializedType("someObjectType", "10"));
        return serializedObject;
    }

    private static class Foo {

        private final String name;
        private final Bar bar;

        Foo(String name, Bar bar) {
            this.name = name;
            this.bar = bar;
        }

        @Override
        public String toString() {
            return "Foo{" +
                    "name='" + name + '\'' +
                    ", bar=" + bar +
                    '}';
        }
    }

    private static class Bar {

        private int count;

        Bar(int count) {
            this.count = count;
        }

        @Override
        public String toString() {
            return "Bar{" +
                    "count=" + count +
                    '}';
        }
    }
}