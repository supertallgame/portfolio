package dev.portfolio.quests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One player's quests for a single day. */
public class PlayerQuests {
    public String date = "";
    public final List<String> active = new ArrayList<>();
    public final Map<String, Integer> progress = new HashMap<>();
    public final Set<String> completed = new HashSet<>();
}
