package dev.ascendant.core.modules;

import dev.ascendant.core.AscendantCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/** Merges nearby dropped items into one entity (fewer entities = less lag). Item totals are always preserved. */
public final class StackerModule implements Listener {
    private final AscendantCore plugin;
    private final NamespacedKey labelKey;

    public StackerModule(AscendantCore plugin) {
        this.plugin = plugin;
        this.labelKey = new NamespacedKey(plugin, "stack_label");
    }

    private boolean allowed(Material m) {
        boolean listed = plugin.getConfig().getStringList("modules.item-stacker.list").stream()
                .anyMatch(s -> s.equalsIgnoreCase(m.name()));
        boolean white = "WHITELIST".equalsIgnoreCase(plugin.getConfig().getString("modules.item-stacker.mode", "BLACKLIST"));
        return white == listed;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(ItemSpawnEvent e) {
        if (!plugin.enabled("item-stacker")) return;
        Item item = e.getEntity();
        ItemStack stack = item.getItemStack();
        if (!allowed(stack.getType())) return;
        int max = Math.max(2, Math.min(99, plugin.getConfig().getInt("modules.item-stacker.max-stack-size", 99)));
        double radius = plugin.getConfig().getDouble("modules.item-stacker.radius", 3.0);
        for (Item other : item.getLocation().getNearbyEntitiesByType(Item.class, radius)) {
            if (other == item || !other.isValid() || other.isDead()) continue;
            ItemStack o = other.getItemStack();
            if (!o.isSimilar(stack)) continue;
            int total = o.getAmount() + stack.getAmount();
            if (total > max) continue;
            o.setAmount(total);
            other.setItemStack(o);
            label(other);
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        Item item = e.getItem();
        Bukkit.getScheduler().runTask(plugin, () -> { if (item.isValid()) label(item); });
    }

    private void label(Item item) {
        if (!plugin.getConfig().getBoolean("modules.item-stacker.show-name", true)) return;
        ItemStack s = item.getItemStack();
        boolean ours = item.getPersistentDataContainer().has(labelKey, PersistentDataType.BYTE);
        if (s.getAmount() > 1) {
            item.customName(Component.text(s.getAmount() + "x ", NamedTextColor.YELLOW)
                    .append(Component.translatable(s.getType().translationKey(), NamedTextColor.WHITE)));
            item.setCustomNameVisible(true);
            item.getPersistentDataContainer().set(labelKey, PersistentDataType.BYTE, (byte) 1);
        } else if (ours) {
            item.customName(null);
            item.setCustomNameVisible(false);
            item.getPersistentDataContainer().remove(labelKey);
        }
    }

    public List<String> list() { return plugin.getConfig().getStringList("modules.item-stacker.list"); }
}
