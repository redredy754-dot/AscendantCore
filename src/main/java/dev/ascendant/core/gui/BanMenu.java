package dev.ascendant.core.gui;

import dev.ascendant.core.AscendantCore;
import dev.ascendant.core.modules.BanItemsModule;
import dev.ascendant.core.modules.BanItemsModule.Category;
import dev.ascendant.core.util.Fx;
import dev.ascendant.core.util.Items;
import dev.ascendant.core.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Ban items hub: two presets on top, the five item categories in the middle. */
public final class BanMenu extends Menu {

    public BanMenu(AscendantCore plugin, Player viewer) {
        super(plugin, viewer, 5, "<gradient:#ef4444:#f97316><bold>Ban Items</bold></gradient>");
    }

    @Override
    protected void build() {
        background();
        BanItemsModule bans = plugin.banItems();
        set(11, preset("Ban Netherite Armor", Material.NETHERITE_CHESTPLATE, BanItemsModule.NETHERITE_ARMOR),
                t -> toggle(BanItemsModule.NETHERITE_ARMOR));
        set(15, preset("Ban Netherite Tools", Material.NETHERITE_PICKAXE, BanItemsModule.NETHERITE_TOOLS),
                t -> toggle(BanItemsModule.NETHERITE_TOOLS));
        set(13, Items.of(Material.BARRIER, "<#f87171><bold>Ban Items", "<gray>Status: " + Text.state(plugin.enabled("ban-items")),
                "<gray>Banned items: <white>" + bans.count(), "", "<yellow>Click <gray>» toggle"), t -> {
            boolean v = !plugin.enabled("ban-items");
            plugin.setEnabled("ban-items", v);
            if (v) Fx.on(viewer); else Fx.off(viewer);
        });
        Material[] icons = {Material.NETHERITE_SWORD, Material.COOKED_BEEF, Material.ENDER_PEARL, Material.GRASS_BLOCK, Material.NAME_TAG};
        int[] slots = {20, 21, 22, 23, 24};
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            Category c = cats[i];
            set(slots[i], Items.of(icons[i], "<#c084fc><bold>" + c.label,
                    "<gray>" + BanItemsModule.items(c).size() + " items", "", "<yellow>Click <gray>» open"),
                    t -> new CategoryMenu(plugin, viewer, c).open());
        }
        backButton(40, () -> new MainMenu(plugin, viewer));
    }

    private ItemStack preset(String name, Material icon, List<Material> mats) {
        boolean all = plugin.banItems().allBanned(mats);
        return Items.glow(Items.of(icon, (all ? "<red><bold>" : "<green><bold>") + name,
                "<gray>Preset: " + (all ? "<red>banned" : "<green>not banned"), "", "<yellow>Click <gray>» " + (all ? "unban all" : "ban all")), all);
    }

    private void toggle(List<Material> mats) {
        boolean ban = !plugin.banItems().allBanned(mats);
        plugin.banItems().setAll(mats, ban);
        if (ban) Fx.off(viewer); else Fx.on(viewer);
    }

    /** One category, every item clickable: click toggles banned. */
    public static final class CategoryMenu extends PagedMenu<Material> {
        public CategoryMenu(AscendantCore plugin, Player viewer, Category c) {
            super(plugin, viewer, "<gradient:#ef4444:#f97316><bold>Ban: " + c.label + "</bold></gradient>",
                    BanItemsModule.items(c), () -> new BanMenu(plugin, viewer));
        }

        @Override
        protected ItemStack icon(Material m) {
            boolean banned = plugin.banItems().isBanned(m);
            return Items.glow(Items.of(m, (banned ? "<red><bold>" : "<green>") + Text.pretty(m.name()),
                    "<gray>Status: " + (banned ? "<red>BANNED" : "<green>allowed"), "", "<yellow>Click <gray>» toggle"), banned);
        }

        @Override
        protected void onEntryClick(Material m, ClickType t) {
            boolean ban = !plugin.banItems().isBanned(m);
            plugin.banItems().set(m, ban);
            if (ban) Fx.off(viewer); else Fx.on(viewer);
        }

        @Override
        protected String footer() { return "<gray>Click an item to ban/unban"; }
    }
}
