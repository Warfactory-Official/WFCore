// WFCore research tree configuration example.
// Place scripts like this in: config/groovyscript/postInit/  (reloadable with /gs reload)
//
// The research tree drives the Research Unit multiblock. Each research completes Factorio-style:
// it repeats `runs` runs, each consuming `itemPerRun` items + `cwuPerRun` compute at `eut` power
// over at least `ticksPerRun` ticks, advancing completion by one run.

// Add a new research that branches off the built-in "fire_control_systems".
mods.wfcore.research.add(
    mods.wfcore.research.builder('rocketry')
        .name('wfcore.research.rocketry.name')   // optional; defaults to wfcore.research.<id>.name
        .icon(item('minecraft:firework_charge'))
        .pos(3, 1)                                // grid position in the tree GUI
        .requires('fire_control_systems')         // prerequisite research id(s)
        .runs(80)                                  // 80 runs to finish
        .itemPerRun(item('minecraft:gunpowder') * 4)
        .itemPerRun(item('minecraft:iron_ingot') * 2)
        .cwuPerRun(1024)                           // compute per run (drawn from the Mainframe)
        .eut(64)                                   // constant power draw while running
        .ticksPerRun(40)
        .blueprint()                               // completing it yields a research data stick
)

// A pure precursor research (no blueprint output), unlocked from the start.
mods.wfcore.research.add(
    mods.wfcore.research.builder('metallurgy')
        .icon(item('minecraft:iron_block'))
        .pos(0, 3)
        .runs(20)
        .itemPerRun(item('minecraft:iron_ingot') * 8)
        .cwuPerRun(128).eut(32).ticksPerRun(30)
)

// Remove a built-in research (tracked for clean /gs reload).
// mods.wfcore.research.remove('sensor_arrays')
