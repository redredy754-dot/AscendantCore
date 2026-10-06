package dev.ascendant.core.gui;

import dev.ascendant.core.AscendantCore;
import dev.ascendant.core.util.Fx;
import dev.ascendant.core.util.Items;
import dev.ascendant.core.util.Text;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/** Per-enchantment max level editor. Left click = decrease, right click = increase (shift = 5). 0 bans it. */
public final class LimitMenu extends PagedMenu<Enchantment> {
    private final String path;

    public LimitMenu(AscendantCore plugin, Player viewer, String title, List<Enchantment> enchants,
                     String path, Supplier<Menu> back) {
        super(plugin, viewer, title, enchants, back);
        this.path = path;
    }

    private int limit(Enchantment e) {
        return plugin.getConfig().getInt(path + "." + e.getKey().getKey(), e.getMaxLevel());
    }

    @Override
    protected ItemStack icon(Enchantment e) {
        int lim = limit(e);
        return Items.glow(Items.of(Material.ENCHANTED_BOOK, "<#c084fc><bold>" + Text.pretty(e.getKey().getKey()),
                "<gray>Max level: " + (lim <= 0 ? "<red>banned" : "<white>" + lim),
                "<gray>Vanilla max: <white>" + e.getMaxLevel(), "",
                "<yellow>Left <gray>» decrease  <yellow>Right <gray>» increase",
                "<dark_gray>Hold shift for steps of 5"), lim > 0);
    }

    @Override
    protected void onEntryClick(Enchantment e, ClickType t) {
        int step = t.isShiftClick() ? 5 : 1;
        int lim = limit(e);
        int next = left(t) ? Math.max(0, lim - step) : Math.min(255, lim + step);
        plugin.getConfig().set(path + "." + e.getKey().getKey(), next);
        plugin.saveConfig();
        if (left(t)) Fx.off(viewer); else Fx.on(viewer);
    }

    @Override
    protected String footer() { return "<gray>Left: lower  Right: raise"; }
}
