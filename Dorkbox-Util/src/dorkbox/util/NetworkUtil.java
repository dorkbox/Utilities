/*
 * Copyright 2015 dorkbox, llc
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final
class NetworkUtil {

    /**
     * Returns an <code>InetAddress</code> object encapsulating what is most likely the machine's LAN IP address.
     * <p/>
     * This method is intended for use as a replacement of JDK method <code>InetAddress.getLocalHost</code>, because
     * that method is ambiguous on Linux systems. Linux systems enumerate the loopback network interface the same
     * way as regular LAN network interfaces, but the JDK <code>InetAddress.getLocalHost</code> method does not
     * specify the algorithm used to select the address returned under such circumstances, and will often return the
     * loopback address, which is not valid for network communication. Details
     * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4665037">here</a>.
     * <p/>
     * This method will scan all IP addresses on all network interfaces on the host machine to determine the IP address
     * most likely to be the machine's LAN address. If the machine has multiple IP addresses, this method will prefer
     * a site-local IP address (e.g. 192.168.x.x or 10.10.x.x, usually IPv4) if the machine has one (and will return the
     * first site-local address if the machine has more than one), but if the machine does not hold a site-local
     * address, this method will return simply the first non-loopback address found (IPv4 or IPv6).
     * <p/>
     * If this method cannot find a non-loopback address using this selection algorithm, it will fall back to
     * calling and returning the result of JDK method <code>InetAddress.getLocalHost</code>.
     * <p/>
     *
     * @throws UnknownHostException If the LAN address of the machine cannot be found.
     *                              <p/>
     *                              From: https://issues.apache.org/jira/browse/JCS-40
     */
    public static
    InetAddress getLocalHost() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // Iterate all NICs (network interface cards)...
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                NetworkInterface iface = ifaces.nextElement();
                // Iterate all IP addresses assigned to each card...
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {

                        if (inetAddr.isSiteLocalAddress()) {
                            // Found non-loopback site-local address. Return it immediately...
                            return inetAddr;
                        }
                        else if (candidateAddress == null) {
                            // Found non-loopback address, but not necessarily site-local.
                            // Store it as a candidate to be returned if site-local address is not subsequently found...
                            candidateAddress = inetAddr;
                            // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
                            // only the first. For subsequent iterations, candidate will be non-null.
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                // We did not find a site-local address, but we found some other non-loopback address.
                // Server might have a non-site-local address assigned to its NIC (or it might be running
                // IPv6 which deprecates the "site-local" concept).
                // Return this non-loopback candidate address...
                return candidateAddress;
            }
            // At this point, we did not find a non-loopback address.
            // Fall back to returning whatever InetAddress.getLocalHost() returns...
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }


    /**
     * This will retrieve your IP address via an HTTP server.
     * <p/>
     * <b>NOTE: Use DnsClient.getPublicIp() instead. It's much faster and more reliable as it uses DNS.</b>
     *
     * @return the public IP address if found, or null if it didn't find it
     */
    @Deprecated
    public static
    String getPublicIpViaHttp() {
        // method 1: use DNS servers
        // dig +short myip.opendns.com @resolver1.opendns.com

        // method 2: use public http servers
        // @formatter:off
        final String websites[] = {"http://ip.dorkbox.com/",
                                   "http://ip.javalauncher.com/",
                                   "http://checkip.dyndns.com/",
                                   "http://checkip.dyn.com/",
                                   "http://curlmyip.com/",
                                   "http://tnx.nl/ip",
                                   "http://ipecho.net/plain",
                                   "http://icanhazip.com/",
                                   "http://ip.appspot.com/",};
        // @formatter:on

        // loop, since they won't always work.
        for (int i = 0; i < websites.length; i++) {
            try {
                URL autoIP = new URL(websites[i]);
                BufferedReader in = new BufferedReader(new InputStreamReader(autoIP.openStream()));
                String response = in.readLine()
                                    .trim();
                in.close();

                Pattern pattern = Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");
                Matcher matcher = pattern.matcher(response);
                if (matcher.find()) {
                    return matcher.group()
                                  .trim();
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    /**
     * Tries to retrieve the IP address from the NIC. Order is ETHx -> EMx -> WLANx
     *
     * @return null if not found
     */
    public static
    InetAddress getIpAddressesFromNic() {
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements()) {
                NetworkInterface nextElement = nets.nextElement();
                String name = nextElement.getName();

                // we only want to use ethX addresses!
                if (name.startsWith("eth")) {
                    Enumeration<InetAddress> inetAddresses = nextElement.getInetAddresses();

                    while (inetAddresses.hasMoreElements()) {
                        InetAddress address = inetAddresses.nextElement();
                        // we only support IPV4 addresses.
                        if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                            return address;
                        }
                    }
                }
            }

            // it didn't work with ethX, so try emX!
            while (nets.hasMoreElements()) {
                nets = NetworkInterface.getNetworkInterfaces();
                NetworkInterface nextElement = nets.nextElement();
                String name = nextElement.getName();

                // we only want to use emX addresses!
                if (name.startsWith("em")) {
                    Enumeration<InetAddress> inetAddresses = nextElement.getInetAddresses();

                    while (inetAddresses.hasMoreElements()) {
                        InetAddress address = inetAddresses.nextElement();
                        // we only support IPV4 addresses.
                        if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                            return address;
                        }
                    }
                }
            }

            // it didn't work with emX, so try enX!
            while (nets.hasMoreElements()) {
                nets = NetworkInterface.getNetworkInterfaces();
                NetworkInterface nextElement = nets.nextElement();
                String name = nextElement.getName();

                // we only want to use enX addresses!
                if (name.startsWith("en")) {
                    Enumeration<InetAddress> inetAddresses = nextElement.getInetAddresses();

                    while (inetAddresses.hasMoreElements()) {
                        InetAddress address = inetAddresses.nextElement();
                        // we only support IPV4 addresses.
                        if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                            return address;
                        }
                    }
                }
            }

            // it didn't work with ethX, so try wifi!
            while (nets.hasMoreElements()) {
                nets = NetworkInterface.getNetworkInterfaces();
                NetworkInterface nextElement = nets.nextElement();
                String name = nextElement.getName();

                // we only want to use wlanX addresses!
                if (name.startsWith("wlan")) {
                    Enumeration<InetAddress> inetAddresses = nextElement.getInetAddresses();

                    while (inetAddresses.hasMoreElements()) {
                        InetAddress address = inetAddresses.nextElement();
                        // we only support IPV4 addresses.
                        if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                            return address;
                        }
                    }
                }
            }
        } catch (SocketException ignored) {
        }

        return null;
    }

    private
    NetworkUtil() {
    }
}
