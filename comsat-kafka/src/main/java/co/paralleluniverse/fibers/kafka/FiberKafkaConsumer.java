/*
 * COMSAT
 * Copyright (C) 2013-2015, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.fibers.kafka;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class FiberKafkaConsumer<K, V> implements Consumer<K, V> {

    private final Consumer<K, V> consumer;

    public FiberKafkaConsumer(Consumer<K, V> consumer) {
        this.consumer = consumer;
    }

    @Override
    public Set<TopicPartition> assignment() {
        return null;
    }

    @Override
    public Set<String> subscription() {
        return null;
    }

    @Override
    public void subscribe(List<String> topics) {

    }

    @Override
    public void subscribe(List<String> topics, ConsumerRebalanceListener callback) {

    }

    @Override
    public void assign(List<TopicPartition> partitions) {

    }

    @Override
    public void subscribe(Pattern pattern, ConsumerRebalanceListener callback) {

    }

    @Override
    public void unsubscribe() {

    }

    @Override
    public ConsumerRecords<K, V> poll(long timeout) {
        return null;
    }

    @Override
    public void commitSync() {

    }

    @Override
    public void commitSync(Map<TopicPartition, OffsetAndMetadata> offsets) {

    }

    @Override
    public void commitAsync() {

    }

    @Override
    public void commitAsync(OffsetCommitCallback callback) {

    }

    @Override
    public void commitAsync(Map<TopicPartition, OffsetAndMetadata> offsets, OffsetCommitCallback callback) {

    }

    @Override
    public void seek(TopicPartition partition, long offset) {

    }

    @Override
    public void seekToBeginning(TopicPartition... partitions) {

    }

    @Override
    public void seekToEnd(TopicPartition... partitions) {

    }

    @Override
    public long position(TopicPartition partition) {
        return 0;
    }

    @Override
    public OffsetAndMetadata committed(TopicPartition partition) {
        return null;
    }

    @Override
    public Map<MetricName, ? extends Metric> metrics() {
        return null;
    }

    @Override
    public List<PartitionInfo> partitionsFor(String topic) {
        return null;
    }

    @Override
    public Map<String, List<PartitionInfo>> listTopics() {
        return null;
    }

    @Override
    public void pause(TopicPartition... partitions) {

    }

    @Override
    public void resume(TopicPartition... partitions) {

    }

    @Override
    public void close() {

    }

    @Override
    public void wakeup() {

    }
}
