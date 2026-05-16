effect give @e[sort=nearest,distance=..64] darkness 30 0
effect give @e[sort=nearest,distance=..64] weakness 15 0
effect give @e[sort=nearest,distance=..64] slowness 15 0

effect clear @s darkness
effect clear @s weakness
effect clear @s slowness

playsound minecraft:block.sculk_shrieker.shriek player @a[sort=nearest,distance=..64] ~ ~ ~ 2 0.8