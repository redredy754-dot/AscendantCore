package dev.ascendant.core.modules;

import dev.ascendant.core.AscendantCore;
import dev.ascendant.core.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class BanItemsModule implements Listener {
    public enum Category {
        TOOLS("Tools & Gear"), FOOD("Food"), UTILITIES("Utilities"), BLOCKS("Blocks"), MISC("Misc");
        public final String label;
        Category(String label) { this.label = label; }
    }

    public static final List<Material> NETHERITE_ARMOR = List.of(Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS);
    public static final List<Material> NETHERITE_TOOLS = List.of(Material.NETHERITE_SWORD, Material.NETHERITE_PICKAXE,
            Material.NETHERITE_AXE, Material.NETHERITE_SHOVEL, Material.NETHERITE_HOE);

    private static final Set<Material> UTILITY = EnumSet.of(Material.ENDER_PEARL, Material.ENDER_EYE, Material.FLINT_AND_STEEL,
            Material.FIRE_CHARGE, Material.ELYTRA, Material.FIREWORK_ROCKET, Material.END_CRYSTAL, Material.TNT,
            Material.TNT_MINECART, Material.TOTEM_OF_UNDYING, Material.WIND_CHARGE, Material.SNOWBALL, Material.EGG,
            Material.COMPASS, Material.CLOCK, Material.SPYGLASS, Material.LEAD, Material.NAME_TAG, Material.SADDLE,
            Material.FILLED_MAP, Material.MAP, Material.ARROW, Material.SPECTRAL_ARROW, Material.TIPPED_ARROW,
            Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION, Material.EXPERIENCE_BOTTLE);

    private static Map<Category, List<Material>> cache;

    public static synchronized List<Material> items(Category c) {
        if (cache == null) {
            cache = new EnumMap<>(Category.class);
            for (Category cat : Category.values()) cache.put(cat, new ArrayList<>());
            for (Material m : Material.values()) {
                if (m.isLegacy() || !m.isItem() || m.isAir()) continue;
                cache.get(classify(m)).add(m);
            }
            cache.values().forEach(l -> l.sort(Comparator.comparing(Enum::name)));
        }
        return cache.get(c);
    }

    private static Category classify(Material m) {
        String n = m.name();
        if (n.endsWith("_SWORD") || n.endsWith("_PICKAXE") || n.endsWith("_AXE") || n.endsWith("_SHOVEL") || n.endsWith("_HOE")
                || n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS")
                || m == Material.TRIDENT || m == Material.MACE || m == Material.BOW || m == Material.CROSSBOW
                || m == Material.SHIELD || m == Material.FISHING_ROD || m == Material.SHEARS) return Category.TOOLS;
        if (UTILITY.contains(m) || n.endsWith("_BUCKET")) return Category.UTILITIES;
        if (m.isEdible()) return Category.FOOD;
        if (m.isBlock()) return Category.BLOCKS;
        return Category.MISC;
    }

    private final AscendantCore plugin;
    private final Set<Material> banned = EnumSet.noneOf(Material.class);

    public BanItemsModule(AscendantCore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        banned.clear();
        for (String s : plugin.getConfig().getStringList("modules.ban-items.banned")) {
            Material m = Material.matchMaterial(s);
            if (m != null) banned.add(m);
        }
    }

    public int count() { return banned.size(); }
    public boolean isBanned(Material m) { return banned.contains(m); }
    public boolean allBanned(Collection<Material> mats) { return banned.containsAll(mats); }

    public void set(Material m, boolean ban) {
        if (ban) banned.add(m); else banned.remove(m);
        save();
    }

    public void setAll(Collection<Material> mats, boolean ban) {
        if (ban) banned.addAll(mats); else banned.removeAll(mats);
        save();
    }

    private void save() {
        List<String> names = new ArrayList<>();
        banned.forEach(m -> names.add(m.name()));
        Collections.sort(names);
        plugin.getConfig().set("modules.ban-items.banned", names);
        plugin.saveConfig();
    }

    private boolean blocks(ItemStack it) {
        return it != null && plugin.enabled("ban-items") && banned.contains(it.getType());
    }

    private void warn(Player p) {
        p.sendActionBar(Text.mm("<red>That item is banned on this server."));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() == org.bukkit.event.block.Action.PHYSICAL) return;
        if (blocks(e.getItem())) { e.setCancelled(true); warn(e.getPlayer()); }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (blocks(e.getPlayer().getInventory().getItem(e.getHand()))) { e.setCancelled(true); warn(e.getPlayer()); }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (blocks(e.getItemInHand())) { e.setCancelled(true); warn(e.getPlayer()); }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player && blocks(e.getItem().getItemStack())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && blocks(p.getInventory().getItemInMainHand())) {
            e.setCancelled(true);
            warn(p);
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent e) {
        if (!plugin.enabled("ban-items")) return;
        ItemStack r = e.getInventory().getResult();
        if (r != null && banned.contains(r.getType())) e.getInventory().setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!plugin.enabled("ban-items") || !(e.getWhoClicked() instanceof Player p)) return;
        if (e.getSlotType() == SlotType.ARMOR && blocks(e.getCursor())) { e.setCancelled(true); warn(p); return; }
        if (e.isShiftClick() && blocks(e.getCurrentItem())) { e.setCancelled(true); warn(p); return; }
        if (e.getClick() == ClickType.NUMBER_KEY && e.getSlotType() == SlotType.ARMOR
                && blocks(p.getInventory().getItem(e.getHotbarButton()))) { e.setCancelled(true); warn(p); }
    }
}
