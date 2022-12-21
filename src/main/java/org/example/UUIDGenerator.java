package org.example;

import lombok.Builder;
import lombok.NonNull;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Clock;
import java.util.Comparator;
import java.util.Random;

/**
 * Generates "universally" unique identifiers.
 * <p>
 * The unique IDs generated roughly follow the UUID V1 template, and are a tuple of:
 * <ol>
 * <li>hundreds of nanos since epoch (henceforth timestamp)</li>
 * <li>machine address</li>
 * <li>incrementing sequence number for given timestamp</li>
 * </ol>
 * <p>
 * Each UUIDGenerator is pinned to a fixed machine address, specified as a long. This class tries to fill a default
 * machine address using the hardware address of localhost.
 * <p>
 * The generated IDs are unique in the sense that there is only one instance of this class per machine address. If
 * there are duplicate instances of this class with the same machine address, then there is no guarantee of universal
 * uniqueness, only a guarantee that a single instance of this class will not generate duplicates.
 * <p>
 * This class is *NOT* threadsafe.
 * <p>
 * Unique IDs are comparable, where the fields (timestamp, machine, sequence) are compared in succession. Thus,
 * sorting a set of ids will first sort on time, then use machine+sequence to tiebreak.
 */
public final class UUIDGenerator {

    /**
     * A listener can optionally be added. The listener will be invoked exactly once for every id generated. Defaults
     * to null/no-action.
     */
    final Listener listener;
    /**
     * The address or identifier used to disambiguate this instance from all other instances. Defaults to MAC address
     * or random long on failure to find MAC address.
     */
    final long machineAddress;
    /**
     * The clock used to determine the instant that an ID was generated. Defaults to system UTC.
     */
    final @NonNull Clock clock;
    /**
     * Tracks the current sequence number for the current timestamp.
     */
    private int sequenceNumber = 0;
    /**
     * Tracks the last hundred nanos generated from this instance. The sequenceNumber is reset when a new timestamp
     * is encountered.
     */
    private long lastHundredNanos = 0;

    @Builder
    private UUIDGenerator(Listener listener, Clock clock, Long machineAddress) {
        this.listener = listener;
        this.clock = clock != null ? clock : Clock.systemUTC();
        if (machineAddress == null) {
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                NetworkInterface inetAddress = NetworkInterface.getByInetAddress(localHost);
                byte[] hardwareAddress = inetAddress.getHardwareAddress();
                machineAddress = new BigInteger(hardwareAddress).longValue();
            } catch (Exception e) {
                machineAddress = new Random().nextLong();
            }
        }
        this.machineAddress = machineAddress;
    }

    /**
     * @return A new instance with default settings for all parameters
     */
    public static UUIDGenerator make() {
        return builder().build();
    }

    /**
     * @return A new unique ID, guaranteed to be unique within this instance, and unique against all other instances
     * with different machine addresses.
     */
    public UniqueId generate() {
        var instant = clock.instant();
        var oldHundredNanos = lastHundredNanos;
        lastHundredNanos = instant.getEpochSecond() * 10000000 + instant.getNano() / 100;
        if (oldHundredNanos == lastHundredNanos) {
            sequenceNumber++;
        } else {
            sequenceNumber = 0;
        }

        var uniqueId = new UniqueId(lastHundredNanos, machineAddress, sequenceNumber);
        if (listener != null) {
            listener.uniqueIdGenerated(uniqueId);
        }
        return uniqueId;
    }

    /**
     * A callback that is invoked for every id generated for a given UUIDGenerator.
     */
    interface Listener {
        /**
         * @param uniqueId An id that was recently generated from a UUIDGenerator instance.
         */
        void uniqueIdGenerated(UniqueId uniqueId);
    }

    /**
     * An ID keyed on a sequence of fields.
     * <p>
     * The ID has a string representation which joins the fields with "-", e.g., UniqueId(1,2,3).toString() == "1-2-3".
     *
     * @param hundredNanos   - number of hundreds of nanos elapsed between the epoch and the generation of this ID.
     * @param machineAddress - an identifier for the instance that generated this ID.
     * @param sequenceNumber - an incrementing number used to tie-break hundredNanos+machineAddress.
     */
    record UniqueId(long hundredNanos, long machineAddress, int sequenceNumber) implements Comparable<UniqueId> {

        @Override
        public String toString() {
            return String.format("%d-%d-%d", hundredNanos, machineAddress, sequenceNumber);
        }

        @Override
        public int compareTo(UniqueId other) {
            return Comparator.comparing(UniqueId::hundredNanos).thenComparing(UniqueId::machineAddress).thenComparing(UniqueId::sequenceNumber).compare(this, other);
        }
    }
}
