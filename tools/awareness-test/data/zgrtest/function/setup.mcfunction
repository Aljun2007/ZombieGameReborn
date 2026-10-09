forceload add -16 -16 64 32
gamerule doMobSpawning false
gamerule doDaylightCycle false
gamerule doWeatherCycle false
gamerule mobGriefing false
weather clear
time set midnight
difficulty normal
kill @e[type=minecraft:zombie]
kill @e[type=minecraft:tnt]
kill @e[type=minecraft:villager]
fill -10 249 -10 50 249 20 minecraft:stone
summon minecraft:zombie 8.5 250.0 8.5
say ===SETUP_DONE_platform_y249_zombie_y250===