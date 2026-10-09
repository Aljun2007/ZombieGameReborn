"""Generate the awareness stress-test datapack

Writes into <repo>/run/world/datapacks/zgrstress (override the run dir with ZGR_RUN_DIR).
See README.md for what it measures and how to run the A/B comparison.

300 zombies on a platform in open air (y=249/250), then continuous TNT noise for 70 s.
Driven entirely by a datapack tick function (console stdin does not reach the game under Gradle).
"""
import os
import shutil

WORLD = os.environ.get("ZGR_RUN_DIR") or os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "run", "world")
DP = os.path.join(WORLD, "datapacks", "zgrstress")

ZOMBIE_COUNT_X = 20
ZOMBIE_COUNT_Z = 15
X0, Z0, STEP_X, STEP_Z = -30, -30, 3, 4
PLATFORM_Y = 249
STAND_Y = 250

NOISE_SPOTS = [
    (-20, -20), (20, -20), (-20, 20), (20, 20),
    (0, 0), (-10, 10), (10, -10), (0, 20),
]

MEASURE_START = 400
NOISE_EVERY = 20
MEASURE_END = 2000


def main():
    if os.path.isdir(DP):
        shutil.rmtree(DP)
    func = os.path.join(DP, "data", "zgrstress", "function")
    tags = os.path.join(DP, "data", "minecraft", "tags", "function")
    os.makedirs(func)
    os.makedirs(tags)

    def write(rel, text):
        path = os.path.join(DP, rel) if rel.startswith("data") else os.path.join(DP, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as fh:
            fh.write(text.encode("utf-8"))

    write("pack.mcmeta",
          '{\n  "pack": {\n    "pack_format": 48,\n'
          '    "description": "ZGR awareness stress test (throwaway)"\n  }\n}\n')
    write("data/minecraft/tags/function/load.json",
          '{\n  "values": [\n    "zgrstress:load"\n  ]\n}\n')
    write("data/minecraft/tags/function/tick.json",
          '{\n  "values": [\n    "zgrstress:tick"\n  ]\n}\n')

    write("data/zgrstress/function/load.mcfunction",
          "scoreboard objectives add zgr_t dummy\n"
          "scoreboard players set #t zgr_t 0\n"
          "say ===STRESS_LOADED===\n")

    setup = [
        "forceload add -48 -48 48 48",
        "gamerule doMobSpawning false",
        "gamerule doDaylightCycle false",
        "gamerule doWeatherCycle false",
        "gamerule mobGriefing false",
        "gamerule randomTickSpeed 0",
        "weather clear",
        "time set midnight",
        "difficulty normal",
        "kill @e[type=!minecraft:player]",
        f"fill -40 {PLATFORM_Y} -40 40 {PLATFORM_Y + 1} 40 minecraft:stone",
        "say ===STRESS_SETUP_DONE===",
    ]
    write("data/zgrstress/function/setup.mcfunction", "\n".join(setup) + "\n")

    # one function per zombie row
    rows = []
    spawned = 0
    for j in range(ZOMBIE_COUNT_Z):
        z = Z0 + STEP_Z * j
        lines = []
        for i in range(ZOMBIE_COUNT_X):
            x = X0 + STEP_X * i
            lines.append(f"summon minecraft:zombie {x}.5 {STAND_Y} {z}.5 {{PersistenceRequired:1b}}")
            spawned += 1
        lines.append(f"say ===ZOMBIE_ROW_{j}===")
        write(f"data/zgrstress/function/row{j}.mcfunction", "\n".join(lines) + "\n")
        rows.append(f"function zgrstress:row{j}")

    write("data/zgrstress/function/spawnzombies.mcfunction", "\n".join(rows) + "\n")

    # noise bursts (TNT; explodes ~4 s later)
    for idx, (x, z) in enumerate(NOISE_SPOTS):
        write(f"data/zgrstress/function/noise{idx}.mcfunction",
              f"summon minecraft:tnt {x}.5 {STAND_Y} {z}.5\n")

    tick = ["execute if score #t zgr_t matches 40 run function zgrstress:setup",
            "execute if score #t zgr_t matches 80 run function zgrstress:spawnzombies",
            f"execute if score #t zgr_t matches {MEASURE_START} run function zgrstress:measure_start"]
    noise_index = 0
    for t in range(MEASURE_START + NOISE_EVERY, MEASURE_END, NOISE_EVERY):
        tick.append(f"execute if score #t zgr_t matches {t} run function "
                    f"zgrstress:noise{noise_index % len(NOISE_SPOTS)}")
        noise_index += 1
    tick.append(f"execute if score #t zgr_t matches {MEASURE_END} run function zgrstress:measure_end")
    tick.append("scoreboard players add #t zgr_t 1")
    write("data/zgrstress/function/tick.mcfunction", "\n".join(tick) + "\n")

    write("data/zgrstress/function/measure_start.mcfunction", "say ===MEASURE_START===\n")
    write("data/zgrstress/function/measure_end.mcfunction", "say ===MEASURE_END===\n")

    print(f"zombies: {spawned} ({ZOMBIE_COUNT_X}x{ZOMBIE_COUNT_Z})")
    print(f"noise events: {noise_index} (every {NOISE_EVERY} ticks, "
          f"{MEASURE_START}..{MEASURE_END})")
    print(f"tick.mcfunction lines: {len(tick)}")
    print("datapack: " + DP)


if __name__ == "__main__":
    main()
