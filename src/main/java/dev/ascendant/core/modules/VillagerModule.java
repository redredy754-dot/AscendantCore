package dev.ascendant.core.modules;

import dev.ascendant.core.AscendantCore;
import dev.ascendant.core.util.Fx;
import dev.ascendant.core.util.Items;
import dev.ascendant.core.util.Text;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Click villagers (pick up as egg, trades kept) + infinite restock. */
public final class VillagerModule implements Listener {
    private static final int HUGE = 1_000_000;
    private final AscendantCore plugin;
    private final NamespacedKey dataKey;

    public VillagerModule(AscendantCore plugin) {
        this.plugin = plugin;
        this.dataKey = new NamespacedKey(plugin, "villager_data");
    }

    // ---------- click villagers ----------

    private boolean isOurEgg(ItemStack it) {
        if (it == null || it.getType() != Material.VILLAGER_SPAWN_EGG || !it.hasItemMeta()) return false;
        return it.getItemMeta().getPersistentDataContainer().has(dataKey, PersistentDataType.STRING);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClickVillager(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        ItemStack held = p.getInventory().getItemInMainHand();
        if (isOurEgg(held)) { e.setCancelled(true); return; }
        if (!(e.getRightClicked() instanceof Villager v)) return;
        if (!plugin.enabled("click-villagers") || !p.isSneaking() || !held.getType().isAir()
                || !p.hasPermission("ascendantcore.clickvillagers")) return;
        e.setCancelled(true);
        if (!v.isValid() || v.isDead()) return;
        ItemStack egg = toEgg(v);
        v.remove();                // removed first, item given second: no way to end up with both
        Items.give(p, egg);
        Fx.success(p);
        p.sendActionBar(Text.mm("<green>Villager picked up."));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlaceEgg(PlayerInteractEvent e) {
        if (e.getHand() == null || !isOurEgg(e.getItem())) return;
        Player p = e.getPlayer();
        if (e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR) { e.setCancelled(true); return; }
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = e.getClickedBlock();
        if (clicked == null) return;
        if (clicked.getType().isInteractable() && !p.isSneaking()) {
            e.setUseItemInHand(Event.Result.DENY);   // let the block (chest, door...) work, never the vanilla egg
            return;
        }
        e.setCancelled(true);
        ItemStack hand = p.getInventory().getItem(e.getHand());
        if (!isOurEgg(hand)) return;
        String data = hand.getItemMeta().getPersistentDataContainer().get(dataKey, PersistentDataType.STRING);
        BlockFace face = e.getBlockFace();
        Location loc = clicked.getRelative(face).getLocation().add(0.5, 0, 0.5);
        if (!loc.getBlock().isPassable()) return;
        Villager spawned = loc.getWorld().spawn(loc, Villager.class, v -> apply(v, data));
        if (!spawned.isValid()) return;
        if (p.getGameMode() != GameMode.CREATIVE) {
            if (hand.getAmount() > 1) { hand.setAmount(hand.getAmount() - 1); p.getInventory().setItem(e.getHand(), hand); }
            else p.getInventory().setItem(e.getHand(), null);
        }
        Fx.success(p);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent e) {
        if (isOurEgg(e.getItem())) e.setCancelled(true);
    }

    private ItemStack toEgg(Villager v) {
        String profession = v.getProfession().getKey().getKey();
        ItemStack egg = Items.of(Material.VILLAGER_SPAWN_EGG, "<#c084fc><bold>Villager",
                "<gray>Profession: <white>" + Text.pretty(profession), "<gray>Level: <white>" + v.getVillagerLevel(), "",
                "<yellow>Right-click a block <gray>» place it back");
        ItemMeta meta = egg.getItemMeta();
        meta.getPersistentDataContainer().set(dataKey, PersistentDataType.STRING, serialize(v));
        egg.setItemMeta(meta);
        return egg;
    }

    private String serialize(Villager v) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("profession", v.getProfession().getKey().toString());
        y.set("type", v.getVillagerType().getKey().toString());
        y.set("level", v.getVillagerLevel());
        y.set("xp", v.getVillagerExperience());
        y.set("baby", !v.isAdult());
        if (v.customName() != null) y.set("name", LegacyComponentSerializer.legacySection().serialize(v.customName()));
        List<MerchantRecipe> recipes = v.getRecipes();
        for (int i = 0; i < recipes.size(); i++) {
            MerchantRecipe r = recipes.get(i);
            String p = "recipes." + i + ".";
            y.set(p + "result", r.getResult());
            y.set(p + "ingredients", r.getIngredients());
            y.set(p + "uses", r.getUses());
            y.set(p + "max-uses", r.getMaxUses());
            y.set(p + "xp-reward", r.hasExperienceReward());
            y.set(p + "villager-xp", r.getVillagerExperience());
            y.set(p + "multiplier", (double) r.getPriceMultiplier());
            y.set(p + "demand", r.getDemand());
            y.set(p + "special", r.getSpecialPrice());
        }
        return y.saveToString();
    }

    private void apply(Villager v, String data) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(data);
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not read stored villager data: " + ex.getMessage());
            return;
        }
        NamespacedKey prof = NamespacedKey.fromString(y.getString("profession", "minecraft:none"));
        NamespacedKey type = NamespacedKey.fromString(y.getString("type", "minecraft:plains"));
        if (type != null && Registry.VILLAGER_TYPE.get(type) != null) v.setVillagerType(Registry.VILLAGER_TYPE.get(type));
        if (prof != null && Registry.VILLAGER_PROFESSION.get(prof) != null) v.setProfession(Registry.VILLAGER_PROFESSION.get(prof));
        v.setVillagerLevel(Math.max(1, Math.min(5, y.getInt("level", 1))));
        v.setVillagerExperience(y.getInt("xp", 0));
        if (y.getBoolean("baby")) v.setBaby(); else v.setAdult();
        String name = y.getString("name");
        if (name != null) v.customName(LegacyComponentSerializer.legacySection().deserialize(name));
        ConfigurationSection rs = y.getConfigurationSection("recipes");
        if (rs == null) return;
        List<MerchantRecipe> list = new ArrayList<>();
        for (String k : rs.getKeys(false)) {
            String p = "recipes." + k + ".";
            ItemStack result = y.getItemStack(p + "result");
            if (result == null) continue;
            MerchantRecipe r = new MerchantRecipe(result, y.getInt(p + "uses"), Math.max(1, y.getInt(p + "max-uses", 12)),
                    y.getBoolean(p + "xp-reward", true), y.getInt(p + "villager-xp"), (float) y.getDouble(p + "multiplier"),
                    y.getInt(p + "demand"), y.getInt(p + "special"));
            List<ItemStack> ing = new ArrayList<>();
            List<?> raw = y.getList(p + "ingredients");
            if (raw != null) for (Object o : raw) if (o instanceof ItemStack is) ing.add(is);
            r.setIngredients(ing);
            list.add(r);
        }
        v.setRecipes(list);
    }

    // ---------- infinite restock ----------

    private void refill(Villager v) {
        List<MerchantRecipe> list = new ArrayList<>(v.getRecipes());
        for (MerchantRecipe r : list) {
            r.setMaxUses(HUGE);
            r.setUses(0);
            r.setDemand(0);
        }
        v.setRecipes(list);
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        if (!plugin.enabled("infinite-restock")) return;
        if (e.getInventory() instanceof MerchantInventory mi && mi.getMerchant() instanceof Villager v) refill(v);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrade(InventoryClickEvent e) {
        if (!plugin.enabled("infinite-restock")) return;
        if (e.getInventory() instanceof MerchantInventory mi && e.getRawSlot() == 2 && mi.getMerchant() instanceof Villager v) {
            Bukkit.getScheduler().runTask(plugin, () -> { if (v.isValid()) refill(v); });
        }
    }
}
