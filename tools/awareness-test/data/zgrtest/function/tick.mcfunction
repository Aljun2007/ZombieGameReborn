execute if score #t zgr_t matches 40 run function zgrtest:setup
execute if score #t zgr_t matches 240 run function zgrtest:report1
execute if score #t zgr_t matches 260 run function zgrtest:impact
execute if score #t zgr_t matches 760 run function zgrtest:report2
execute if score #t zgr_t matches 1160 run function zgrtest:report3
scoreboard players add #t zgr_t 1
