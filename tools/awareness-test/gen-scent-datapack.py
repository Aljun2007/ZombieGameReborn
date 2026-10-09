"""Generate the scent-trail test datapack.

Goal: isolate SCENT gradient-following from every other awareness channel.

How the isolation works:
  * awareness_sound_radius is dialled down to 6 in the config, so the hurt-sound stimulus
    (radius = soundRadius * 0.5 = 3) never reaches the zombie -> no SOUND stimulus, no target.
  * awareness_scent_decay_ticks is raised to 2400 so the whole trail survives the wait.
  * A villager is walked along +X in 4-block steps and damaged at each step, depositing one
    scent cell per step over ~500 ticks -> a real age gradient (newest end strongest).
  * The villager is then killed (removing any possible target), the zombie is teleported back
    to the trail head, and we wait for the last hurt sound (lifespan 100) to expire.
  * From then on the ONLY thing the zombie can sense is the scent grid. If it walks +X along
    the trail, gradient-following works.
"""
import os
import shutil

WORLD = (os.environ.get("ZGR_RUN_DIR")
         or os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
                         "run", "world"))
DP = os.path.join(WORLD, "datapacks", "zgrscent")

PLATFORM_Y = 249
STAND_Y = 250
ZOMBIE = (8.5, 8.5)
TRAIL_X0, TRAIL_STEP, TRAIL_STEPS = 12, 4, 13      # 12 .. 60
TRAIL_EVERY = 40                                   # ticks between deposits
TRAIL_START_TICK = 100

SETUP_TICK = 40
KILL_VILLAGER_TICK = TRAIL_START_TICK + TRAIL_EVERY * TRAIL_STEPS + 20   # 640
RESET_ZOMBIE_TICK = KILL_VILLAGER_TICK + 20                              # 660
# last possible hurt sound is at tick 620, lifespan 100 -> silent from 720
OBSERVE_FROM = 760
END_TICK = 1800


def main():
    if os.path.isdir(DP):
        shutil.rmtree(DP)
    func = os.path.join(DP, "data", "zgrscent", "function")
    tags = os.path.join(DP, "data", "minecraft", "tags", "function")
    os.makedirs(func)
    os.makedirs(tags)

    def write(rel, text):
        path = os.path.join(DP, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as fh:
            fh.write(text.replace("\r\n", "\n").encode("utf-8"))

    write("pack.mcmeta",
          '{\n  "pack": {\n    "pack_format": 48,\n'
          '    "description": "ZGR scent trail test"\n  }\n}\n')
    write("data/minecraft/tags/function/load.json", '{\n  "values": ["zgrscent:load"]\n}\n')
    write("data/minecraft/tags/function/tick.json", '{\n  "values": ["zgrscent:tick"]\n}\n')

    write("data/zgrscent/function/load.mcfunction",
          "scoreboard objectives add zgr_t dummy\nscoreboard players set #t zgr_t 0\n"
          "say ===SCENT_TEST_LOADED===\n")

    write("data/zgrscent/function/setup.mcfunction", "\n".join([
        "forceload add -16 -16 112 32",
        "gamerule doMobSpawning false",
        "gamerule doDaylightCycle false",
        "gamerule doWeatherCycle false",
        "gamerule mobGriefing false",
        "weather clear",
        "time set midnight",
        "difficulty normal",
        "kill @e[type=!minecraft:player]",
        f"fill -10 {PLATFORM_Y} -10 110 {PLATFORM_Y} 20 minecraft:stone",
        f"summon minecraft:zombie {ZOMBIE[0]} {STAND_Y}.0 {ZOMBIE[1]}",
        f"summon minecraft:villager {TRAIL_X0}.5 {STAND_Y}.0 8.5",
        "say ===SETUP_ZOMBIE_AT_8_VILLAGER_AT_12===",
    ]) + "\n")

    # one step function per deposit: teleport the villager, then damage it -> deposit + hurt sound
    step_calls = []
    for k in range(TRAIL_STEPS):
        x = TRAIL_X0 + TRAIL_STEP * k
        write(f"data/zgrscent/function/step{k}.mcfunction", "\n".join([
            f"tp @e[type=minecraft:villager,limit=1] {x}.5 {STAND_Y}.0 8.5",
            "damage @e[type=minecraft:villager,limit=1] 1 minecraft:generic",
        ]) + "\n")
        step_calls.append((TRAIL_START_TICK + TRAIL_EVERY * k, f"zgrscent:step{k}"))

    write("data/zgrscent/function/killvillager.mcfunction",
          "kill @e[type=minecraft:villager]\nsay ===VILLAGER_REMOVED===\n")
    write("data/zgrscent/function/resetzombie.mcfunction",
          f"tp @e[type=minecraft:zombie,limit=1] {ZOMBIE[0]} {STAND_Y}.0 {ZOMBIE[1]}\n"
          "say ===ZOMBIE_RESET_TO_TRAIL_HEAD===\n")
    write("data/zgrscent/function/observe.mcfunction",
          "say ===OBSERVE_START_ONLY_SCENT_REMAINS===\n")
    write("data/zgrscent/function/finish.mcfunction", "say ===TEST_END===\n")

    tick = [f"execute if score #t zgr_t matches {SETUP_TICK} run function zgrscent:setup"]
    for t, fn in step_calls:
        tick.append(f"execute if score #t zgr_t matches {t} run function {fn}")
    tick += [
        f"execute if score #t zgr_t matches {KILL_VILLAGER_TICK} run function zgrscent:killvillager",
        f"execute if score #t zgr_t matches {RESET_ZOMBIE_TICK} run function zgrscent:resetzombie",
        f"execute if score #t zgr_t matches {OBSERVE_FROM} run function zgrscent:observe",
        f"execute if score #t zgr_t matches {END_TICK} run function zgrscent:finish",
        "scoreboard players add #t zgr_t 1",
    ]
    write("data/zgrscent/function/tick.mcfunction", "\n".join(tick) + "\n")

    print(f"trail: x={TRAIL_X0}..{TRAIL_X0 + TRAIL_STEP * (TRAIL_STEPS - 1)} in "
          f"{TRAIL_STEPS} deposits every {TRAIL_EVERY} ticks ({TRAIL_START_TICK}"
          f"..{TRAIL_START_TICK + TRAIL_EVERY * (TRAIL_STEPS - 1)})")
    print(f"villager removed at {KILL_VILLAGER_TICK}, zombie reset at {RESET_ZOMBIE_TICK}, "
          f"last hurt sound expires ~{KILL_VILLAGER_TICK - 20 + 100}, observe from {OBSERVE_FROM}")
    print("datapack: " + DP)


if __name__ == "__main__":
    main()
