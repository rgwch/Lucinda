/*******************************************************************************
 * Copyright (c) 2016 by G. Weirich
 *
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *
 * Contributors:
 * G. Weirich - initial implementation
 ******************************************************************************/

package ch.rgw.lucinda;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Created by gerry on 11.05.16.
 */
public class Util {
    public static String matchIP(String mask) {
        if (mask == null) {
            return "";
        }
        String[] check = mask.split("\\.");
        if (check.length != 4) {
            return "";
        }
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();

            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    InetAddress ia = ias.nextElement();
                    String ip = ia.getHostAddress();
                    String[] iface = ip.split("\\.");
                    if (iface.length != 4) {
                        continue;
                    }
                    if (isMatch(check, iface)) {
                        return ip;
                    }
                }
            }
            return "";

        } catch (Exception ex) {
            return "";
        }

    }

    private static boolean isMatch(String[] check, String[] ip) {
        for (int i = 0; i < 3; i++) {
            if (check[i].equals("*")) {
                continue;
            }
            if (!check[i].equals(ip[i])) {
                return false;
            }
        }
        return true;
    }
}
