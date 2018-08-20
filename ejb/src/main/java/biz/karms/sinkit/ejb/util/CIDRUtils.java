package biz.karms.sinkit.ejb.util;

/*
* The MIT License
*
* Copyright (c) 2013 Edin Dazdarevic (edin.dazdarevic@gmail.com)
*
* Modified by Michal Karm Babacek, redistributed under the MIT License.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:

* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.

* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
*
* */

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * A class that enables to get an IP range from CIDR specification. It supports
 * both IPv4 and IPv6.
 */
public class CIDRUtils {
    private CIDRUtils() {
    }

    public static ImmutablePair<String, String> getStartEndAddresses(final String cidr) throws UnknownHostException {
        final ImmutablePair<BigInteger, BigInteger> startEnd = getStartEndAddressesBigInt(cidr);
        return new ImmutablePair<>(String.format("%040d", startEnd.getLeft()), String.format("%040d", startEnd.getRight()));
    }

    public static ImmutablePair<BigInteger, BigInteger> getStartEndAddressesBigInt(final String cidr) throws UnknownHostException {
        //TODO: This is silly. Refactor CIDRUtils so as to accept actual IPs as well as subnets.
        //TODO: Validate the thing before processing. Guava?
        final String fixedCIDR;
        if (!cidr.contains("/")) {
            //IPv6? Hmmm...
            if (cidr.contains(":")) {
                fixedCIDR = cidr + "/128";
            } else {
                fixedCIDR = cidr + "/32";
            }
        } else {
            fixedCIDR = cidr;
        }
        final int index = fixedCIDR.indexOf("/");
        final InetAddress inetAddress = InetAddress.getByName(fixedCIDR.substring(0, index));
        final int prefixLength = Integer.parseInt(fixedCIDR.substring(index + 1));

        final ByteBuffer maskBuffer;
        if (inetAddress.getAddress().length == 4) {
            maskBuffer = ByteBuffer.allocate(4).putInt(-1);
        } else {
            maskBuffer = ByteBuffer.allocate(16).putLong(-1L).putLong(-1L);
        }

        final BigInteger mask = (new BigInteger(1, maskBuffer.array())).not().shiftRight(prefixLength);
        final ByteBuffer buffer = ByteBuffer.wrap(inetAddress.getAddress());
        final BigInteger ipVal = new BigInteger(1, buffer.array());
        final BigInteger startIp = ipVal.and(mask);
        final BigInteger endIp = startIp.add(mask.not());

        return new ImmutablePair<>(startIp, endIp);
    }
}
