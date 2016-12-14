package org.jruby.ext.socket;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/*
 * Copyright 2013 Jan Van Besien
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
 * 
 * @author Jan Van Besien
 */
public class SocketUtilsIPV6 {

    public static String getIPV6Address(String ip) {
        return new IPv6Address().fromString(ip).toString();
    }

    public static String getIPV6NetMask(String ip) {
        IPv6Network strangeNetwork = new IPv6Network().fromString(ip);
        return strangeNetwork.getNetmask().asAddress().toString();
    }

    public static class IPv6Address {

        private static final int N_SHORTS = 8;
        private long highBits;
        private long lowBits;

        IPv6Address(long highBits, long lowBits) {
            this.highBits = highBits;
            this.lowBits = lowBits;
        }

        private IPv6Address() { }

        /**
         * Create an IPv6 address from its String representation. For example
         * "1234:5678:abcd:0000:9876:3210:ffff:ffff" or "2001::ff" or even "::".
         * IPv4-Mapped IPv6 addresses such as "::ffff:123.456.123.456" are also
         * supported.
         *
         * @param string string representation
         * @return IPv6 address
         */
        public IPv6Address fromString(String string) {
            if (string == null) {
                throw new IllegalArgumentException("can not parse [null]");
            }

            final String withoutScope = removeScope(string);
            final String withoutIPv4MappedNotation = rewriteIPv4MappedNotation(withoutScope);
            final String longNotation = expandShortNotation(withoutIPv4MappedNotation);

            final long[] longs = tryParseStringArrayIntoLongArray(string, longNotation);

            return mergeLongArrayIntoIPv6Address(longs);
        }

        public InetAddress toInetAddress() throws UnknownHostException {
            return Inet6Address.getByName(toString());
        }

        private IPv6Address mergeLongArrayIntoIPv6Address(long[] longs) {
            long high = 0L;
            long low = 0L;

            for (int i = 0; i < longs.length; i++) {
                if (inHighRange(i)) {
                    high |= (longs[i] << ((longs.length - i - 1) * 16));
                } else {
                    low |= (longs[i] << ((longs.length - i - 1) * 16));
                }
            }

            return new IPv6Address(high, low);
        }

        private String rewriteIPv4MappedNotation(String string) {
            if (!string.contains(".")) {
                return string;
            } else {
                int lastColon = string.lastIndexOf(":");
                String firstPart = string.substring(0, lastColon + 1);
                String mappedIPv4Part = string.substring(lastColon + 1);

                if (mappedIPv4Part.contains(".")) {
                    String[] dotSplits = Pattern.compile("\\.").split(mappedIPv4Part);
                    if (dotSplits.length != 4) {
                        throw new IllegalArgumentException(String.format("can not parse [%s]", string));
                    }

                    StringBuilder rewrittenString = new StringBuilder();
                    rewrittenString.append(firstPart);
                    int byteZero = Integer.parseInt(dotSplits[0]);
                    int byteOne = Integer.parseInt(dotSplits[1]);
                    int byteTwo = Integer.parseInt(dotSplits[2]);
                    int byteThree = Integer.parseInt(dotSplits[3]);

                    rewrittenString.append(String.format("%02x", byteZero));
                    rewrittenString.append(String.format("%02x", byteOne));
                    rewrittenString.append(":");
                    rewrittenString.append(String.format("%02x", byteTwo));
                    rewrittenString.append(String.format("%02x", byteThree));

                    return rewrittenString.toString();
                } else {
                    throw new IllegalArgumentException(String.format("can not parse [%s]", string));
                }
            }
        }

        private long[] tryParseStringArrayIntoLongArray(String string, String longNotation) {
            try {
                return parseStringArrayIntoLongArray(longNotation.split(":"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("can not parse [" + string + "]");
            }
        }

        private long[] parseStringArrayIntoLongArray(String[] strings) {
            final long[] longs = new long[strings.length];
            for (int i = 0; i < strings.length; i++) {
                longs[i] = Long.parseLong(strings[i], 16);
            }
            return longs;
        }

        private String expandShortNotation(String string) {
            if (!string.contains("::")) {
                return string;
            } else if (string.equals("::")) {
                return generateZeroes(8);
            } else {
                final int numberOfColons = countOccurrences(string, ':');
                if (string.startsWith("::")) {
                    return string.replace("::", generateZeroes((7 + 2) - numberOfColons));
                } else if (string.endsWith("::")) {
                    return string.replace("::", ":" + generateZeroes((7 + 2) - numberOfColons));
                } else {
                    return string.replace("::", ":" + generateZeroes((7 + 2 - 1) - numberOfColons));
                }
            }
        }

        private int countOccurrences(String haystack, char needle) {
            int count = 0;
            for (int i = 0; i < haystack.length(); i++) {
                if (haystack.charAt(i) == needle) {
                    count++;
                }
            }
            return count;
        }

        private String generateZeroes(int number) {
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < number; i++) {
                builder.append("0:");
            }

            return builder.toString();
        }

        private String removeScope(String string) {
            int hasScope = string.indexOf('%');

            if (hasScope != -1) {
                return string.substring(0, hasScope);
            }

            return string;
        }

        /**
         * Addition. Will never overflow, but wraps around when the highest ip
         * address has been reached.
         *
         * @param value value to add
         * @return new IPv6 address
         */
        public IPv6Address add(int value) {
            final long newLowBits = lowBits + value;

            if (value >= 0) {
                if (isLessThanUnsigned(newLowBits, lowBits)) {
                    // oops, we added something positive and the result is smaller -> overflow detected (carry over one bit from low to high)
                    return new IPv6Address(highBits + 1, newLowBits);
                } else {
                    // no overflow
                    return new IPv6Address(highBits, newLowBits);
                }
            } else {
                if (isLessThanUnsigned(lowBits, newLowBits)) {
                    // oops, we added something negative and the result is bigger -> overflow detected (carry over one bit from high to low)
                    return new IPv6Address(highBits - 1, newLowBits);
                } else {
                    // no overflow
                    return new IPv6Address(highBits, newLowBits);
                }
            }
        }

        /**
         * Subtraction. Will never underflow, but wraps around when the lowest
         * ip address has been reached.
         *
         * @param value value to substract
         * @return new IPv6 address
         */
        public IPv6Address subtract(int value) {
            final long newLowBits = lowBits - value;

            if (value >= 0) {
                if (isLessThanUnsigned(lowBits, newLowBits)) {
                    // oops, we subtracted something postive and the result is bigger -> overflow detected (carry over one bit from high to low)
                    return new IPv6Address(highBits - 1, newLowBits);
                } else {
                    // no overflow
                    return new IPv6Address(highBits, newLowBits);
                }
            } else {
                if (isLessThanUnsigned(newLowBits, lowBits)) {
                    // oops, we subtracted something negative and the result is smaller -> overflow detected (carry over one bit from low to high)
                    return new IPv6Address(highBits + 1, newLowBits);
                } else {
                    // no overflow
                    return new IPv6Address(highBits, newLowBits);
                }
            }
        }

        private boolean isLessThanUnsigned(long a, long b) {
            return (a < b) ^ ((a < 0) != (b < 0));
        }

        /**
         * Mask the address with the given network mask.
         *
         * @param networkMask network mask
         * @return an address of which the last 128 -
         * networkMask.asPrefixLength() bits are zero
         */
        public IPv6Address maskWithNetworkMask(final IPv6NetworkMask networkMask) {
            if (networkMask.asPrefixLength() == 128) {
                return this;
            } else if (networkMask.asPrefixLength() == 64) {
                return new IPv6Address(this.highBits, 0);
            } else if (networkMask.asPrefixLength() == 0) {
                return new IPv6Address(0, 0);
            } else if (networkMask.asPrefixLength() > 64) {
                // apply mask on low bits only
                final int remainingPrefixLength = networkMask.asPrefixLength() - 64;
                return new IPv6Address(this.highBits, this.lowBits & (0xFFFFFFFFFFFFFFFFL << (64 - remainingPrefixLength)));
            } else {
                // apply mask on high bits, low bits completely 0
                return new IPv6Address(this.highBits & (0xFFFFFFFFFFFFFFFFL << (64 - networkMask.asPrefixLength())), 0);
            }
        }

        /**
         * Calculate the maximum address with the given network mask.
         *
         * @param networkMask network mask
         * @return an address of which the last 128 -
         * networkMask.asPrefixLength() bits are one
         */
        public IPv6Address maximumAddressWithNetworkMask(final IPv6NetworkMask networkMask) {
            if (networkMask.asPrefixLength() == 128) {
                return this;
            } else if (networkMask.asPrefixLength() == 64) {
                return new IPv6Address(this.highBits, 0xFFFFFFFFFFFFFFFFL);
            } else if (networkMask.asPrefixLength() > 64) {
                // apply mask on low bits only
                final int remainingPrefixLength = networkMask.asPrefixLength() - 64;
                return new IPv6Address(this.highBits, this.lowBits | (0xFFFFFFFFFFFFFFFFL >>> remainingPrefixLength));
            } else {
                // apply mask on high bits, low bits completely 1
                return new IPv6Address(this.highBits | (0xFFFFFFFFFFFFFFFFL >>> networkMask.asPrefixLength()), 0xFFFFFFFFFFFFFFFFL);
            }
        }

        /**
         * Returns true if the address is an IPv4-mapped IPv6 address. In these
         * addresses, the first 80 bits are zero, the next 16 bits are one, and
         * the remaining 32 bits are the IPv4 address.
         *
         * @return true if the address is an IPv4-mapped IPv6 addresses.
         */
        public boolean isIPv4Mapped() {
            return this.highBits == 0 // 64 zero bits
                    && (this.lowBits & 0xFFFF000000000000L) == 0 // 16 more zero bits
                    && (this.lowBits & 0x0000FFFF00000000L) == 0x0000FFFF00000000L; // 16 one bits and the remainder is the IPv4 address
        }

        /**
         * Returns a string representation of the IPv6 address. It will use
         * shorthand notation and special notation for IPv4-mapped IPv6
         * addresses whenever possible.
         *
         * @return String representation of the IPv6 address
         */
        @Override
        public String toString() {
            if (isIPv4Mapped()) {
                return toIPv4MappedAddressString();
            } else {
                return toShortHandNotationString();
            }
        }

        private String toIPv4MappedAddressString() {
            int byteZero = (int) ((this.lowBits & 0x00000000FF000000L) >> 24);
            int byteOne = (int) ((this.lowBits & 0x0000000000FF0000L) >> 16);
            int byteTwo = (int) ((this.lowBits & 0x000000000000FF00L) >> 8);
            int byteThree = (int) ((this.lowBits & 0x00000000000000FFL));

            final StringBuilder result = new StringBuilder("::ffff:");
            result.append(byteZero).append(".").append(byteOne).append(".").append(byteTwo).append(".").append(byteThree);

            return result.toString();
        }

        private String toShortHandNotationString() {
            final String[] strings = toArrayOfShortStrings();

            final StringBuilder result = new StringBuilder();

            int[] shortHandNotationPositionAndLength = startAndLengthOfLongestRunOfZeroes();
            int shortHandNotationPosition = shortHandNotationPositionAndLength[0];
            int shortHandNotationLength = shortHandNotationPositionAndLength[1];

            boolean useShortHandNotation = shortHandNotationLength > 1; // RFC5952 recommends not to use shorthand notation for a single zero

            for (int i = 0; i < strings.length; i++) {
                if (useShortHandNotation && i == shortHandNotationPosition) {
                    if (i == 0) {
                        result.append("::");
                    } else {
                        result.append(":");
                    }
                } else if (!(i > shortHandNotationPosition && i < shortHandNotationPosition + shortHandNotationLength)) {
                    result.append(strings[i]);
                    if (i < N_SHORTS - 1) {
                        result.append(":");
                    }
                }
            }

            return result.toString().toLowerCase();
        }

        private String[] toArrayOfShortStrings() {
            final short[] shorts = toShortArray();
            final String[] strings = new String[shorts.length];
            for (int i = 0; i < shorts.length; i++) {
                strings[i] = String.format("%x", shorts[i]);
            }
            return strings;
        }

        private short[] toShortArray() {
            final short[] shorts = new short[N_SHORTS];

            for (int i = 0; i < N_SHORTS; i++) {
                if (inHighRange(i)) {
                    shorts[i] = (short) (((highBits << i * 16) >>> 16 * (N_SHORTS - 1)) & 0xFFFF);
                } else {
                    shorts[i] = (short) (((lowBits << i * 16) >>> 16 * (N_SHORTS - 1)) & 0xFFFF);
                }
            }

            return shorts;
        }

        private boolean inHighRange(int shortNumber) {
            return shortNumber >= 0 && shortNumber < 4;
        }

        int[] startAndLengthOfLongestRunOfZeroes() {
            int longestConsecutiveZeroes = 0;
            int longestConsecutiveZeroesPos = -1;
            short[] shorts = toShortArray();
            for (int pos = 0; pos < shorts.length; pos++) {
                int consecutiveZeroesAtCurrentPos = countConsecutiveZeroes(shorts, pos);
                if (consecutiveZeroesAtCurrentPos > longestConsecutiveZeroes) {
                    longestConsecutiveZeroes = consecutiveZeroesAtCurrentPos;
                    longestConsecutiveZeroesPos = pos;
                }
            }

            return new int[]{longestConsecutiveZeroesPos, longestConsecutiveZeroes};
        }

        private int countConsecutiveZeroes(short[] shorts, int offset) {
            int count = 0;
            for (int i = offset; i < shorts.length && shorts[i] == 0; i++) {
                count++;
            }

            return count;
        }

        public long getHighBits() {
            return highBits;
        }

        public long getLowBits() {
            return lowBits;
        }

        public int numberOfTrailingZeroes() {
            return lowBits == 0
                    ? Long.numberOfTrailingZeros(highBits) + 64
                    : Long.numberOfTrailingZeros(lowBits);
        }

        public int numberOfTrailingOnes() {
            // count trailing ones in "value" by counting the trailing zeroes in "value + 1"
            final IPv6Address plusOne = this.add(1);
            return plusOne.getLowBits() == 0
                    ? Long.numberOfTrailingZeros(plusOne.getHighBits()) + 64
                    : Long.numberOfTrailingZeros(plusOne.getLowBits());
        }

        public int numberOfLeadingZeroes() {
            return highBits == 0
                    ? Long.numberOfLeadingZeros(lowBits) + 64
                    : Long.numberOfLeadingZeros(highBits);
        }

        public int numberOfLeadingOnes() {
            // count leading ones in "value" by counting leading zeroes in "~ value"
            final IPv6Address flipped = new IPv6Address(~this.highBits, ~this.lowBits);
            return flipped.numberOfLeadingZeroes();
        }

    }

    public static class IPv6NetworkMask {

        private final int prefixLength;

        IPv6NetworkMask(int prefixLength) {
            if (prefixLength < 0 || prefixLength > 128) {
                throw new IllegalArgumentException("prefix length should be in interval [0, 128]");
            }

            this.prefixLength = prefixLength;
        }

        public int asPrefixLength() {
            return prefixLength;
        }

        public IPv6Address asAddress() {
            if (prefixLength == 128) {
                return new IPv6Address(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
            } else if (prefixLength == 64) {
                return new IPv6Address(0xFFFFFFFFFFFFFFFFL, 0L);
            } else if (prefixLength > 64) {
                final int remainingPrefixLength = prefixLength - 64;
                return new IPv6Address(0xFFFFFFFFFFFFFFFFL, (0xFFFFFFFFFFFFFFFFL << (64 - remainingPrefixLength)));
            } else {
                return new IPv6Address(0xFFFFFFFFFFFFFFFFL << (64 - prefixLength), 0);
            }
        }
    }

    public static class IPv6Network {

        private IPv6NetworkMask networkMask;
        private IPv6Address first;
        private IPv6Address last;
        private IPv6Address address;

        /**
         * Construct from address and network mask.
         *
         * @param address address
         * @param networkMask network mask
         */
        private IPv6Network(IPv6Address address, IPv6NetworkMask networkMask) {

            this.first = address.maskWithNetworkMask(networkMask);
            this.last = address.maximumAddressWithNetworkMask(networkMask);
            this.address = address.maskWithNetworkMask(networkMask);
            this.networkMask = networkMask;
        }

        private IPv6Network() {
        }

        /**
         * Create an IPv6 network from its String representation. For example
         * "1234:5678:abcd:0:0:0:0:0/64" or "2001::ff/128".
         *
         * @param string string representation
         * @return IPv6 network
         */
        public IPv6Network fromString(String string) {
            if (string.indexOf('/') == -1) {
                throw new IllegalArgumentException("Expected format is network-address/prefix-length");
            }

            final String networkAddressString = parseNetworkAddress(string);
            int prefixLength = parsePrefixLength(string);

            IPv6Address networkAddress = new IPv6Address().fromString(networkAddressString);

            return new IPv6Network(networkAddress, new IPv6NetworkMask(prefixLength));
        }

        private String parseNetworkAddress(String string) {
            return string.substring(0, string.indexOf('/'));
        }

        private int parsePrefixLength(String string) {
            try {
                return Integer.parseInt(string.substring(string.indexOf('/') + 1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Prefix length should be a positive integer");
            }
        }

        public IPv6NetworkMask getNetmask() {
            return networkMask;
        }
    }
}
