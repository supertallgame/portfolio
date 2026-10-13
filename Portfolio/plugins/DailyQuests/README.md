# DailyQuests

Randomised daily quests for Paper 1.21+.

## Features
- Each player gets N random quests per day (resets at server midnight)
- Quest types: `BREAK_BLOCK` and `KILL_MOB`, with a specific target or `ANY`
- Progress GUI with progress bars (`/quests`)
- Rewards run as console commands, so they work with any plugin (the defaults use CoreEconomy's `/eco give`)
- Per-player data persisted to `data.yml`, loaded lazily and saved on quit / autosave
- Creative-mode players don't earn progress
- Admin: `/quests reroll [player]`, `/quests reload`

## Build
```
mvn clean package
```

## Adding quests
Add an entry under `quests:` in `config.yml` - no code changes needed.
