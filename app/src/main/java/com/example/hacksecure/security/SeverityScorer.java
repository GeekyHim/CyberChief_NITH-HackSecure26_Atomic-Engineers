package com.example.hacksecure.security;

import com.example.hacksecure.data.entity.EventLog;

import java.util.HashSet;
import java.util.Set;

/**
 * Weighted heuristic risk scoring for EventLog.
 * Returns severity 0-4: 0=benign, 1=low, 2=medium, 3=high, 4=critical.
 */
public final class SeverityScorer {

    private static final Set<String> TRUSTED_PACKAGES = new HashSet<>();
    static {
        TRUSTED_PACKAGES.add("com.whatsapp");
        TRUSTED_PACKAGES.add("com.google.android.gm");
        TRUSTED_PACKAGES.add("com.google.android.apps.photos");
        TRUSTED_PACKAGES.add("com.microsoft.outlook");
    }

    private SeverityScorer() {}

    public static int compute(EventLog event) {
        try {
            if (event == null) return 0;

            int baseEventScore = getBaseEventScore(safe(event.getEventType()));
            int portRisk = getPortRisk(event.getPort());
            int protocolRisk = getProtocolRisk(safe(event.getProtocol()));
            int ipRisk = getIpRisk(safe(event.getDestinationIp()));
            int trafficVolumeRisk = getTrafficVolumeRisk(safe(event.getMetadata()));
            int behaviorRisk = getBehaviorRisk(safe(event.getMetadata()));
            int appReputationAdjustment = getAppReputationAdjustment(safe(event.getPackageNameForScoring()));

            int score = baseEventScore + portRisk + protocolRisk + ipRisk
                    + trafficVolumeRisk + behaviorRisk + appReputationAdjustment;
            score = Math.max(0, score);
            return Math.min(score, 4);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static int getBaseEventScore(String eventType) {
        if (eventType.contains("NETWORK_ACCESS")) return 1;
        if (eventType.contains("PERMISSION_ACCESS")) return 2;
        if (eventType.contains("SUSPICIOUS_TRAFFIC")) return 3;
        if (eventType.contains("VPN_BYPASS")) return 4;
        return 1;
    }

    private static int getPortRisk(int port) {
        switch (port) {
            case 80:
            case 443:
            case 53:
                return 0;
            case 22:
                return 1;
            case 23:
            case 3389:
                return 2;
            case 4444:
                return 3;
            default:
                return 1;
        }
    }

    private static int getProtocolRisk(String protocol) {
        if (protocol == null) return 2;
        String p = protocol.toUpperCase();
        if ("TCP".equals(p) || "UDP".equals(p)) return 0;
        if ("ICMP".equals(p)) return 1;
        return 2;
    }

    private static int getIpRisk(String ip) {
        if (ip == null || ip.isEmpty()) return 0;
        if (ip.startsWith("10.")) return 0;
        if (ip.startsWith("192.168.")) return 0;
        if (ip.startsWith("172.")) {
            try {
                int d1 = ip.indexOf('.', 4);
                if (d1 > 0 && d1 < ip.length() - 1) {
                    int second = Integer.parseInt(ip.substring(4, d1));
                    if (second >= 16 && second <= 31) return 0;
                }
            } catch (Exception ignored) {
                // fall through to public IP risk
            }
        }
        return 1;
    }

    private static int getTrafficVolumeRisk(String metadata) {
        if (metadata == null || !metadata.toUpperCase().contains("UPLOAD")) return 0;
        long mb = parseUploadMb(metadata);
        if (mb < 0) return 0;
        if (mb < 10) return 0;
        if (mb <= 100) return 1;
        if (mb <= 200) return 2;
        return 3;
    }

    private static long parseUploadMb(String metadata) {
        try {
            int idx = metadata.toUpperCase().indexOf("UPLOAD");
            if (idx < 0) return -1;
            String rest = metadata.substring(idx);
            StringBuilder num = new StringBuilder();
            for (int i = 0; i < rest.length(); i++) {
                char c = rest.charAt(i);
                if (Character.isDigit(c)) num.append(c);
                else if (c == '.' && num.length() > 0) num.append(c);
                else if (num.length() > 0) break;
            }
            if (num.length() == 0) return -1;
            double val = Double.parseDouble(num.toString());
            if (rest.toUpperCase().contains("GB")) val *= 1024;
            return (long) val;
        } catch (Exception e) {
            return -1;
        }
    }

    private static int getBehaviorRisk(String metadata) {
        if (metadata == null) return 0;
        return metadata.toUpperCase().contains("BACKGROUND") ? 1 : 0;
    }

    private static int getAppReputationAdjustment(String packageName) {
        if (packageName == null || packageName.isEmpty()) return 0;
        return TRUSTED_PACKAGES.contains(packageName) ? -1 : 0;
    }
}
