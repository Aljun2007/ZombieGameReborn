execute if score #t zgr_t matches 40 run function zgrtest:setup
execute if score #t zgr_t matches 400 run function zgrtest:report1
execute if score #t zgr_t matches 420 run function zgrtest:impact
execute if score #t zgr_t matches 800 run function zgrtest:report2
execute if score #t zgr_t matches 1400 run function zgrtest:finish
scoreboard players add #t zgr_t 1