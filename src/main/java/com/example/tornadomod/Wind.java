package com.example.tornadomod;

import java.util.List;

public class Wind {
    public static float getWindX(double px, double pz, List<Storm> storms) {
        float wx = 0;
        for (Storm s : storms) {
            double dx = px - s.x;
            double dz = pz - s.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            double r = s.radius() * 2;
            if (dist > r || dist < 1) continue;
            double factor = (1.0 - dist / r) * s.kmh() / 200.0;
            if (s.kind == Storm.Kind.TORNADO) {
                wx += -dz / dist * factor;
            } else {
                wx += (-dz / dist * 0.7 - dx / dist * 0.3) * factor;
            }
        }
        return (float)wx;
    }

    public static float getWindZ(double px, double pz, List<Storm> storms) {
        float wz = 0;
        for (Storm s : storms) {
            double dx = px - s.x;
            double dz = pz - s.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            double r = s.radius() * 2;
            if (dist > r || dist < 1) continue;
            double factor = (1.0 - dist / r) * s.kmh() / 200.0;
            if (s.kind == Storm.Kind.TORNADO) {
                wz += dx / dist * factor;
            } else {
                wz += (dx / dist * 0.7 - dz / dist * 0.3) * factor;
            }
        }
        return (float)wz;
    }
}
