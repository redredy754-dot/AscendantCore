package dev.ascendant.core.gui;

import dev.ascendant.core.AscendantCore;
import dev.ascendant.core.gui.ConfigMenu.Entry;
import dev.ascendant.core.util.Fx;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Entry lists for every module that has a settings screen. */
public final class ConfigMenus {
    private ConfigMenus() {}

    private static ConfigMenu make(AscendantCore plugin, Player p, String title, List<Entry> entries) {
        return new ConfigMenu(plugin, p, title, entries, () -> new MainMenu(plugin, p));
    }

    public static ConfigMenu lifesteal(AscendantCore plugin, Player p) {
        String m = "modules.lifesteal.";
        return make(plugin, p, "<gradient:#ef4444:#f43f5e><bold>Lifesteal</bold></gradient>", List.of(
                Entry.integer(m + "starting-hearts", "Starting Hearts", Material.APPLE, "Hearts new players begin with", 1, 100),
                Entry.integer(m + "max-hearts", "Max Hearts", Material.GOLDEN_APPLE, "Upper heart limit", 1, 100),
                Entry.integer(m + "hearts-per-kill", "Hearts Per Kill", Material.IRON_SWORD, "Hearts a killer gains", 1, 20),
                Entry.integer(m + "hearts-lost-on-death", "Hearts Lost On Death", Material.SKELETON_SKULL, "Hearts a victim loses", 1, 20),
                Entry.bool(m + "lose-heart-on-natural-death", "Lose Heart On Natural Death", Material.COBWEB, "Also lose hearts to mobs/environment"),
                Entry.choice(m + "elimination", "Elimination", Material.BARRIER, "What happens at 0 hearts", "SPECTATOR", "BAN"),
                Entry.bool(m + "heart-item", "Heart Item (/withdrawheart)", Material.NETHER_STAR, "Allow turning hearts into items"),
                Entry.bool(m + "drop-heart-at-max", "Heart At Max", Material.CHEST, "Killer at max hearts gets a heart item")));
    }

    public static ConfigMenu clumps(AscendantCore plugin, Player p) {
        String m = "modules.clumps.";
        return make(plugin, p, "<gradient:#84cc16:#22c55e><bold>Clumps</bold></gradient>", List.of(
                Entry.decimal(m + "radius", "Merge Radius", Material.COMPASS, "XP orbs within this range merge (blocks)", 0.5, 32),
                Entry.integer(m + "max-orb-value", "Max Orb Value", Material.EXPERIENCE_BOTTLE, "Largest merged orb (xp)", 1, 1_000_000)));
    }

    public static ConfigMenu stacker(AscendantCore plugin, Player p) {
        String m = "modules.item-stacker.";
        return make(plugin, p, "<gradient:#f59e0b:#f97316><bold>Item Stacker</bold></gradient>", List.of(
                Entry.decimal(m + "radius", "Merge Radius", Material.COMPASS, "Dropped items within range merge (blocks)", 0.5, 16),
                Entry.integer(m + "max-stack-size", "Max Stack Size", Material.CHEST, "Largest ground stack (max 99)", 2, 99),
                Entry.choice(m + "mode", "List Mode", Material.WRITABLE_BOOK, "BLACKLIST: stack all but list. WHITELIST: only list", "BLACKLIST", "WHITELIST"),
                Entry.bool(m + "show-name", "Show Amount", Material.NAME_TAG, "Show amount above stacked items"),
                Entry.action("Edit List", Material.BOOK, "Use /ascendantcore stacker add|remove <item>", pl -> {
                    List<String> l = plugin.getConfig().getStringList(m + "list");
                    plugin.msg(pl, "<gray>List (" + plugin.getConfig().getString(m + "mode") + "): <white>" + (l.isEmpty() ? "empty" : String.join(", ", l)));
                    plugin.msg(pl, "<gray>Edit with <white>/ascendantcore stacker add|remove <item>");
                })));
    }

    public static ConfigMenu restarter(AscendantCore plugin, Player p) {
        String m = "modules.server-restarter.";
        Runnable reopen = () -> restarter(plugin, p).open();
        return make(plugin, p, "<gradient:#38bdf8:#6366f1><bold>Server Restarter</bold></gradient>", List.of(
                Entry.integer(m + "countdown-seconds", "Countdown", Material.CLOCK, "Seconds of warning before restart", 5, 600),
                Entry.integer(m + "protect-seconds", "Combat Protection", Material.SHIELD, "Players take no damage for the last N seconds", 0, 60),
                Entry.choice(m + "method", "Restart Method", Material.COMMAND_BLOCK, "SPIGOT_RESTART needs a restart script", "SPIGOT_RESTART", "SHUTDOWN"),
                Entry.integer(m + "min-uptime-minutes", "Min Uptime", Material.REPEATER, "Ignore resource triggers this long after start", 0, 1440),
                Entry.bool(m + "ram-percent.enabled", "RAM % Restart", Material.REDSTONE, "Restart when heap use stays high"),
                Entry.integer(m + "ram-percent.value", "RAM % Limit", Material.REDSTONE_TORCH, "Percent of max heap", 50, 99),
                Entry.bool(m + "ram-mb.enabled", "RAM MB Restart", Material.REDSTONE_BLOCK, "Restart at a specific heap size"),
                Entry.integer(m + "ram-mb.value", "RAM MB Limit", Material.COMPARATOR, "Used heap in MB", 512, 1_048_576),
                Entry.bool(m + "tps.enabled", "TPS Restart", Material.LIGHTNING_ROD, "Restart when TPS stays low"),
                Entry.decimal(m + "tps.value", "TPS Limit", Material.DAYLIGHT_DETECTOR, "5-minute average TPS", 1, 19.5),
                Entry.bool(m + "scheduled.enabled", "Scheduled Restart", Material.RECOVERY_COMPASS, "Restart at fixed times of day"),
                Entry.action("Add Restart Time", Material.WRITABLE_BOOK, "Type a time like 06:00 in the anvil", pl ->
                        plugin.prompts().ask(pl, "Restart time HH:mm", "06:00", text -> {
                            try {
                                LocalTime t = LocalTime.parse(text.length() == 4 ? "0" + text : text);
                                List<String> times = new ArrayList<>(plugin.getConfig().getStringList(m + "scheduled.times"));
                                String s = String.format("%02d:%02d", t.getHour(), t.getMinute());
                                if (!times.contains(s)) times.add(s);
                                plugin.getConfig().set(m + "scheduled.times", times);
                                plugin.saveConfig();
                                Fx.success(pl);
                            } catch (Exception ex) {
                                plugin.msg(pl, "<red>Use the format HH:mm, for example 06:00");
                                Fx.error(pl);
                            }
                            reopen.run();
                        }, reopen)),
                Entry.action("Clear Restart Times", Material.LAVA_BUCKET, "Remove every scheduled time", pl -> {
                    plugin.getConfig().set(m + "scheduled.times", new ArrayList<String>());
                    plugin.saveConfig();
                    Fx.off(pl);
                    plugin.msg(pl, "<gray>Scheduled restart times cleared.");
                }),
                Entry.action("Restart Now", Material.TNT, "Starts the countdown immediately", pl -> {
                    plugin.restarter().start("manual (" + pl.getName() + ")");
                    pl.closeInventory();
                }),
                Entry.action("Cancel Restart", Material.MILK_BUCKET, "Stops a running countdown", pl -> {
                    plugin.restarter().cancel();
                    Fx.on(pl);
                })));
    }

    public static ConfigMenu explosions(AscendantCore plugin, Player p) {
        String m = "modules.explosion-control.";
        List<Entry> l = new ArrayList<>();
        String[][] src = {{"global", "All Explosions", "NETHER_STAR"}, {"creeper", "Creeper", "CREEPER_HEAD"},
                {"tnt", "TNT", "TNT"}, {"tnt-minecart", "TNT Minecart", "TNT_MINECART"},
                {"end-crystal", "End Crystal", "END_CRYSTAL"}, {"other", "Other (beds, anchors, fireballs)", "FIRE_CHARGE"}};
        for (String[] s : src) {
            Material icon = Material.valueOf(s[2]);
            l.add(Entry.bool(m + s[0] + ".destruction", s[1] + " Block Damage", icon, "Explosions break blocks"));
            l.add(Entry.decimal(m + s[0] + ".power", s[1] + " Power", icon, "Blast radius multiplier", 0, 20));
        }
        return make(plugin, p, "<gradient:#ef4444:#f59e0b><bold>Explosion Control</bold></gradient>", l);
    }

    public static ConfigMenu optimizations(AscendantCore plugin, Player p) {
        String m = "modules.optimizations.";
        return make(plugin, p, "<gradient:#22d3ee:#a855f7><bold>Optimizations</bold></gradient>", List.of(
                Entry.integer(m + "spawn-limits.monsters", "Monster Spawn Limit", Material.ZOMBIE_HEAD, "Per-player mob cap", 0, 500),
                Entry.integer(m + "spawn-limits.animals", "Animal Spawn Limit", Material.WHEAT, "Per-player mob cap", 0, 500),
                Entry.integer(m + "spawn-limits.water-animals", "Water Animal Limit", Material.COD, "Per-player mob cap", 0, 500),
                Entry.integer(m + "spawn-limits.water-ambient", "Water Ambient Limit", Material.TROPICAL_FISH, "Per-player mob cap", 0, 500),
                Entry.integer(m + "spawn-limits.ambient", "Ambient Limit", Material.BAT_SPAWN_EGG, "Per-player mob cap", 0, 500),
                Entry.integer(m + "view-distance", "Render Distance", Material.SPYGLASS, "Chunks sent to players", 2, 32),
                Entry.integer(m + "simulation-distance", "Simulation Distance", Material.CLOCK, "Chunks that tick", 2, 32),
                Entry.integer(m + "entity-tracking-range.players", "Tracking: Players", Material.PLAYER_HEAD, "Blocks (needs spigot.yml write)", 1, 512),
                Entry.integer(m + "entity-tracking-range.animals", "Tracking: Animals", Material.BEEF, "Blocks (needs spigot.yml write)", 1, 512),
                Entry.integer(m + "entity-tracking-range.monsters", "Tracking: Monsters", Material.BONE, "Blocks (needs spigot.yml write)", 1, 512),
                Entry.integer(m + "entity-tracking-range.misc", "Tracking: Misc", Material.ITEM_FRAME, "Blocks (needs spigot.yml write)", 1, 512),
                Entry.integer(m + "entity-tracking-range.other", "Tracking: Other", Material.ARMOR_STAND, "Blocks (needs spigot.yml write)", 1, 512),
                Entry.action("Write Tracking Ranges", Material.WRITTEN_BOOK, "Saves ranges to spigot.yml (backup made, restart needed)", pl -> {
                    String result = plugin.optimization().writeTrackingRanges();
                    plugin.msg(pl, result);
                    Fx.success(pl);
                })));
    }
}
