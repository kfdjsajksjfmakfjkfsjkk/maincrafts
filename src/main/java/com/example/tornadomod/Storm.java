package com.example.tornadomod;

public class Storm {
    public enum Kind { TORNADO, CYCLONE }

    // Tornado wind speeds (km/h) EF0-EF5
    public static final float[] TORNADO_KMH  = { 120, 160, 200, 250, 310, 400 };
    // Cyclone wind speeds (km/h) Cat1-Cat5
    public static final float[] CYCLONE_KMH  = { 120, 150, 180, 210, 260 };
    // Cyclone central pressure (hPa)
    public static final float[] CYCLONE_HPA  = { 980, 970, 955, 935, 910 };

    public final int   id;
    public final Kind  kind;
    public final int   maxLevel;
    public int         level;
    public double      x, y, z;
    public double      vx, vz;
    public int         age;
    public int         life;
    public int         landTicks;
    public float       f;   // funnel progress 0-1

    public Storm(int id, Kind kind, int maxLevel, double x, double y, double z) {
        this.id = id;
        this.kind = kind;
        this.maxLevel = maxLevel;
        this.level = 0;
        this.x = x; this.y = y; this.z = z;
        this.vx = 0; this.vz = 0;
        this.age = 0;
        this.life = 24000 + (int)(Math.random() * 24000);
        this.landTicks = 0;
        this.f = 0f;
    }

    public float radius() {
        if (kind == Kind.TORNADO) {
            return 8 + level * 6;
        } else {
            return 200 + level * 80;
        }
    }

    public float eye() {
        return kind == Kind.CYCLONE ? (30 + level * 10) : 0;
    }

    public float kmh() {
        if (kind == Kind.TORNADO) {
            return level < TORNADO_KMH.length ? TORNADO_KMH[level] : TORNADO_KMH[TORNADO_KMH.length - 1];
        } else {
            return level < CYCLONE_KMH.length ? CYCLONE_KMH[level] : CYCLONE_KMH[CYCLONE_KMH.length - 1];
        }
    }

    public float pressureHpa() {
        if (kind == Kind.CYCLONE) {
            return level < CYCLONE_HPA.length ? CYCLONE_HPA[level] : CYCLONE_HPA[CYCLONE_HPA.length - 1];
        }
        return 1013f - level * 15f;
    }

    public String name() {
        if (kind == Kind.TORNADO) {
            return "EF" + Math.min(level, 5);
        } else {
            return "CAT" + Math.min(level + 1, 5);
        }
    }

    public String stage() {
        if (kind == Kind.TORNADO) {
            String[] stages = {"Rope","Cone","Wedge","Stovepipe","Multi-vortex","Violent"};
            return stages[Math.min(level, stages.length - 1)];
        } else {
            String[] stages = {"Tropical Storm","Cat 1","Cat 2","Cat 3","Cat 4","Cat 5"};
            return stages[Math.min(level, stages.length - 1)];
        }
    }

    public float progress() {
        return (float) age / Math.max(1, life);
    }
}
