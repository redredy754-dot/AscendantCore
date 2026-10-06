package dev.ascendant.core.gui;

import dev.ascendant.core.AscendantCore;
import dev.ascendant.core.util.Fx;
import dev.ascendant.core.util.Items;
import dev.ascendant.core.util.Text;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public final class MainMenu extends Menu {
    private record Def(String key, String name, Material icon, String[] desc, Consumer<Player> right, boolean leftOpens) {}

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

    public MainMenu(AscendantCore plugin, Player viewer) {
        super(plugin, viewer, 6, "<gradient:#a855f7:#ec4899><bold>AscendantCore</bold></gradient> <dark_gray>» <gray>Control Panel");
    }

    private List<Def> defs() {
        Player p = viewer;
        Runnable home = () -> new MainMenu(plugin, p).open();
        List<Def> d = new ArrayList<>();
        d.add(new Def("shield-stunning", "Shield Stunning", Material.SHIELD, new String[]{"ON: axes disable shields (vanilla)", "OFF: shields can't be stunned"}, null, false));
        d.add(new Def("attribute-swapping", "Attribute Swapping", Material.DIAMOND_SWORD, new String[]{"ON: allowed (vanilla)", "OFF: hits right after a hotbar swap fail"}, null, false));
        d.add(new Def("one-player-sleep", "One Player Sleep", Material.RED_BED, new String[]{"One sleeping player skips the night"}, null, false));
        d.add(new Def("string-command", "/string", Material.STRING, new String[]{"Lets players turn cobwebs into string"}, null, false));
        d.add(new Def("click-villagers", "Click Villagers", Material.VILLAGER_SPAWN_EGG, new String[]{"Sneak + right-click a villager to pick it up", "as a spawn egg, trades included"}, null, false));
        d.add(new Def("cobweb-cleaner", "Cobweb Cleaner", Material.COBWEB, new String[]{"Placed cobwebs vanish after a while"},
                pl -> ConfigMenu.editNumber(plugin, pl, "Cobweb clean time (seconds)", "modules.cobweb-cleaner.seconds", 1, 3600, true, home), false));
        d.add(new Def("ban-items", "Ban Items", Material.BARRIER, new String[]{"Ban any item, with netherite presets"},
                pl -> new BanMenu(plugin, pl).open(), false));
        d.add(new Def("mace", "Mace", Material.MACE, new String[]{"Mace enchant limiter and mace limiter"},
                pl -> new MaceMenu(plugin, pl).open(), true));
        d.add(new Def("potion-limiter", "Potion Limiter", Material.POTION, new String[]{"Ban individual potions"},
                pl -> {
                    List<PotionType> types = new ArrayList<>();
                    Registry.POTION.forEach(types::add);
                    types.sort(Comparator.comparing(t -> t.getKey().getKey()));
                    new PotionMenu(plugin, pl, types, () -> new MainMenu(plugin, pl)).open();
                }, false));
        d.add(new Def("enchant-limiter", "Enchant Limiter", Material.ENCHANTED_BOOK, new String[]{"Cap the level of every enchantment"},
                pl -> {
                    List<Enchantment> list = new ArrayList<>();
                    Registry.ENCHANTMENT.forEach(list::add);
                    list.sort(Comparator.comparing(e -> e.getKey().getKey()));
                    new LimitMenu(plugin, pl, "<gradient:#a855f7:#ec4899><bold>Enchant Limiter</bold></gradient>", list,
                            "modules.enchant-limiter.limits", () -> new MainMenu(plugin, pl)).open();
                }, false));
        d.add(new Def("server-cleaner", "Server Cleaner", Material.HOPPER, new String[]{"Clears dropped items on a timer", "Messages are editable in config.yml"},
                pl -> ConfigMenu.editNumber(plugin, pl, "Cleaner cooldown (seconds)", "modules.server-cleaner.interval-seconds", 10, 86400, true, home), false));
        d.add(new Def("lifesteal", "Lifesteal", Material.TOTEM_OF_UNDYING, new String[]{"Gain hearts by killing, lose them by dying"},
                pl -> ConfigMenus.lifesteal(plugin, pl).open(), false));
        d.add(new Def("infinite-restock", "Infinite Villager Restock", Material.EMERALD, new String[]{"Villager trades never run out"}, null, false));
        d.add(new Def("clumps", "Clumps", Material.EXPERIENCE_BOTTLE, new String[]{"Merges nearby XP orbs"},
                pl -> ConfigMenus.clumps(plugin, pl).open(), false));
        d.add(new Def("item-stacker", "Item Stacker", Material.CHEST, new String[]{"Merges dropped items into stacks"},
                pl -> ConfigMenus.stacker(plugin, pl).open(), false));
        d.add(new Def("server-restarter", "Server Restarter", Material.CLOCK, new String[]{"Auto restart by RAM, TPS or time"},
                pl -> ConfigMenus.restarter(plugin, pl).open(), false));
        d.add(new Def("explosion-control", "Explosion Control", Material.TNT, new String[]{"Power and block damage per explosion type"},
                pl -> ConfigMenus.explosions(plugin, pl).open(), false));
        d.add(new Def("optimizations", "Optimizations", Material.COMPARATOR, new String[]{"Spawn limits, render distance,", "simulation distance, tracking range"},
                pl -> ConfigMenus.optimizations(plugin, pl).open(), false));
        return d;
    }

    @Override
    protected void build() {
        background();
        List<Def> defs = defs();
        for (int i = 0; i < defs.size() && i < SLOTS.length; i++) {
            Def def = defs.get(i);
            List<String> lore = new ArrayList<>();
            for (String line : def.desc()) lore.add("<gray>" + line);
            lore.add("");
            if (def.key().equals("mace")) {
                lore.add("<gray>Enchant limiter: " + Text.state(plugin.enabled("mace.enchant-limiter")));
                lore.add("<gray>Mace limiter: " + Text.state(plugin.enabled("mace.limiter")));
                lore.add("");
                lore.add("<green>Left Click <gray>» open");
            } else {
                lore.add("<gray>Status: " + Text.state(plugin.enabled(def.key())));
                lore.add("");
                lore.add("<green>Left Click <gray>» toggle on/off");
                if (def.right() != null) lore.add("<aqua>Right Click <gray>» configure");
            }
            set(SLOTS[i], Items.glow(Items.of(def.icon(), "<#c084fc><bold>" + def.name(), lore.toArray(new String[0])),
                    !def.key().equals("mace") && plugin.enabled(def.key())), t -> {
                if (left(t)) {
                    if (def.leftOpens()) { def.right().accept(viewer); return; }
                    boolean v = !plugin.enabled(def.key());
                    plugin.setEnabled(def.key(), v);
                    if (v) Fx.on(viewer); else Fx.off(viewer);
                } else if (right(t)) {
                    if (def.right() != null) def.right().accept(viewer);
                    else Fx.error(viewer);
                }
            });
        }
        set(49, Items.of(Material.NETHER_STAR, "<gradient:#a855f7:#ec4899><bold>AscendantCore",
                "<gray>Optimization & rules core", "", "<dark_gray>Left click toggles, right click configures"));
    }
}
