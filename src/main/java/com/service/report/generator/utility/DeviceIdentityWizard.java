package com.service.report.generator.utility;

import com.service.report.generator.tag.DeviceAddressType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.net.*;
import java.util.Enumeration;


@Component
public class DeviceIdentityWizard {

    @Cacheable("deviceAddress")
    public String getDeviceAddress(DeviceAddressType type) {
        if (type == null)
            return null;

        InetAddress lanIP = null;
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface element = networkInterfaces.nextElement();
                Enumeration<InetAddress> addresses = element.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();
                    byte[] hardwareAddress = element.getHardwareAddress();
                    if (hardwareAddress != null && hardwareAddress.length > 0 && !isVMMac(hardwareAddress)) {
                        if (ip instanceof Inet4Address) {
                            if (ip.isSiteLocalAddress())
                                lanIP = InetAddress.getByName(ip.getHostAddress());
                        }
                    }
                } // IN
            } // OUT

            if (lanIP == null)
                return null;

            switch (type) {
                case IP -> {
                    return lanIP.toString().replaceAll("^/+", "");
                }
                case MAC -> {
                    return getMacAddress(lanIP);
                }
            }

        } catch (UnknownHostException | SocketException ignored) {}
        return null;
    }

    private static String getMacAddress(InetAddress ip) {
        try {
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            byte[] mac = network.getHardwareAddress();

            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < mac.length; i++)
                stringBuilder.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));

            return stringBuilder.toString();
        } catch (SocketException ignored) {}
        return null;
    }

    private static boolean isVMMac(byte[] mac) {
        if(null == mac)
            return false;

        byte[][] invalidMacs = {
                {0x00, 0x05, 0x69},             // VMWare
                {0x00, 0x1C, 0x14},             // VMWare
                {0x00, 0x0C, 0x29},             // VMWare
                {0x00, 0x50, 0x56},             // VMWare
                {0x08, 0x00, 0x27},             // Virtualbox
                {0x0A, 0x00, 0x27},             // Virtualbox
                {0x00, 0x03, (byte)0xFF},       // Virtual-PC
                {0x00, 0x15, 0x5D}              // Hyper-V
        };

        for (byte[] invalid: invalidMacs) {
            if (invalid[0] == mac[0] && invalid[1] == mac[1] && invalid[2] == mac[2])
                return true;
        }

        return false;
    }

}

