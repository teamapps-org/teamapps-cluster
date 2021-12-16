package org.teamapps.cluster.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Utils {

    public static <T> T randomListEntry(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        int id = randomValue(0, list.size());
        return list.get(id);
    }

    public static int randomValue(int minInclusive, int maxExclusive) {
        int value = -1;
        while (value < minInclusive || value >= maxExclusive) {
            value = (int) (maxExclusive * Math.random()) + minInclusive;
        }
        return value;
    }
}
