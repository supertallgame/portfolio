package dev.portfolio.quests;

import java.util.List;

/** A quest template loaded from config.yml. */
public record QuestDef(
        String id,
        String name,
        QuestType type,
        String target,
        int amount,
        String rewardText,
        List<String> rewardCommands) {

    public boolean matches(QuestType eventType, String eventTarget) {
        return type == eventType && (target.equalsIgnoreCase("ANY") || target.equalsIgnoreCase(eventTarget));
    }
}
