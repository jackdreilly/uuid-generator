package org.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class UUIDGeneratorTest {

    @Test
    @DisplayName("Listener logs all generated UUIDs")
    void listener() {
        var log = new ArrayList<UUIDGenerator.UniqueId>();
        var generator = UUIDGenerator.builder().listener(log::add).build();
        var first = generator.generate();
        var second = generator.generate();
        assertThat(log, is(List.of(first, second)));
        assertThat(first, is(not(second)));
    }

    @Test
    @DisplayName("Default generator produces sensible nanos")
    void sensibleNanos() {
        assertThat(UUIDGenerator.make().generate().hundredNanos() / 10000.0, is(closeTo(Instant.now().toEpochMilli(),
                1)));
    }

    @Test
    @DisplayName("machine address passes through")
    void machineAddress() {
        assertThat(UUIDGenerator.builder().machineAddress(42L).build().generate().machineAddress(), is(42L));
    }

    @Test
    @DisplayName("Sort unique IDs by timestamp first")
    void sortByTimestamp() {
        assertThat(new UUIDGenerator.UniqueId(10, 20, 30), lessThan(new UUIDGenerator.UniqueId(12, 1, 2)));
    }

    @Test
    @DisplayName("Tiebreak unique ID ordering on machine address first")
    void sortByTimestampThenMachineAddress() {
        assertThat(new UUIDGenerator.UniqueId(10, 20, 30), lessThan(new UUIDGenerator.UniqueId(10, 21, 2)));
    }

    @Test
    @DisplayName("Tiebreak unique ID ordering on machine address, then sequence")
    void sortByTimestampThenMachineAddressThenSequenceNumber() {
        assertThat(new UUIDGenerator.UniqueId(10, 20, 30), lessThan(new UUIDGenerator.UniqueId(10, 20, 31)));
    }

    @Test
    @DisplayName("Unique ID uses field-by-field equality")
    void uniqueIdEquality() {
        assertThat(new UUIDGenerator.UniqueId(10, 20, 30), is(new UUIDGenerator.UniqueId(10, 20, 30)));
        assertThat(new UUIDGenerator.UniqueId(10, 20, 30), is(not(new UUIDGenerator.UniqueId(10, 20, 31))));
    }

    @Test
    @DisplayName("Sequence number increases for identical hundred nanos")
    void incrementSequenceNumber() {
        var clock = new FakeClock();
        var generator = UUIDGenerator.builder().clock(clock).build();
        var first = generator.generate();
        var second = generator.generate();
        assertThat(first.hundredNanos(), is(second.hundredNanos()));
        assertThat(first.sequenceNumber(), is(0));
        assertThat(second.sequenceNumber(), is(1));
    }

    @Test
    @DisplayName("Sequence number resets for new hundred nano")
    void resetSequenceNumber() {
        var clock = new FakeClock();
        var generator = UUIDGenerator.builder().clock(clock).build();
        var first = generator.generate();
        clock.hundredMillis++;
        var second = generator.generate();
        assertThat(first.hundredNanos(), is(second.hundredNanos() - 1));
        assertThat(first.sequenceNumber(), is(0));
        assertThat(second.sequenceNumber(), is(0));
    }

    @Test
    @DisplayName("String representation of unique ID is dashes separating integers")
    void testToString() {
        assertThat(new UUIDGenerator.UniqueId(1, 2, 3).toString(), is("1-2-3"));
    }

    static class FakeClock extends Clock {
        int hundredMillis = 1;

        @Override
        public ZoneId getZone() {
            return null;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return null;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochSecond(0, hundredMillis * 100L);
        }
    }
}