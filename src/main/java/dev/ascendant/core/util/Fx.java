package dev.ascendant.core.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** All GUI sound effects live here. */
public final class Fx {
    private Fx() {}

    public static void click(Player p) { p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.3f); }
    public static void on(Player p) { p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.7f); }
    public static void off(Player p) { p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.7f); }
    public static void error(Player p) { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f); }
    public static void success(Player p) { p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.6f); }
    public static void open(Player p) { p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.0f); }
    public static void page(Player p) { p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.6f, 1.4f); }
}
