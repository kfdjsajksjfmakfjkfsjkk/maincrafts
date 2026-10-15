package com.example.tornadomod.client;

import com.example.tornadomod.Storm;
import java.util.ArrayList;
import java.util.List;

public class ClientStorms {
    private static volatile List<Storm> storms = new ArrayList<>();

    public static void set(List<Storm> list) { storms = list; }
    public static List<Storm> get() { return storms; }
}
