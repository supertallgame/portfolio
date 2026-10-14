# HomeHub

Homes and warps for Paper 1.21+.

## Features
- `/sethome [name]`, `/home [name]`, `/delhome`, `/homes`
- Server warps: `/setwarp`, `/warp`, `/delwarp`, `/warps`
- Rank-based home limits through permissions (`homes.limit.5`, `homes.limit.unlimited`, ...)
- Teleport warmup that cancels if you move or take damage, plus per-player cooldowns
- `homehub.bypass` skips warmup and cooldown
- Async teleports (`teleportAsync`), tab completion, validated names, YAML persistence

## Build
```
mvn clean package
```

## Config
See `config.yml` for warmup, cooldown and default home limit.
