package dev.ascendant.core;

import dev.ascendant.core.gui.MainMenu;
import dev.ascendant.core.util.Items;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Commands implements TabExecutor {
    private final AscendantCore plugin;

    public Commands(AscendantCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return switch (cmd.getName().toLowerCase(Locale.ROOT)) {
            case "string" -> string(sender);
            case "withdrawheart" -> withdraw(sender, args);
            default -> main(sender, args);
        };
    }

    private boolean main(CommandSender s, String[] a) {
        if (!s.hasPermission("ascendantcore.admin")) {
            plugin.msg(s, "<red>You don't have permission.");
            return true;
        }
        String sub = a.length == 0 ? "gui" : a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "gui" -> {
                if (s instanceof Player p) new MainMenu(plugin, p).open();
                else plugin.msg(s, "<red>Only players can open the GUI.");
            }
            case "reload" -> {
                plugin.reloadConfig();
                plugin.changed();
                plugin.msg(s, "<green>Config reloaded.");
            }
            case "revive" -> {
                Player t = a.length > 1 ? plugin.getServer().getPlayerExact(a[1]) : null;
                if (t == null) { plugin.msg(s, "<red>Usage: /ascendantcore revive <online player>"); break; }
                plugin.lifesteal().revive(t);
                plugin.msg(s, "<green>Revived " + t.getName() + ".");
            }
            case "stacker" -> {
                if (a.length < 3) { plugin.msg(s, "<red>Usage: /ascendantcore stacker <add|remove> <item>"); break; }
                Material m = Material.matchMaterial(a[2]);
                if (m == null) { plugin.msg(s, "<red>Unknown item."); break; }
                List<String> list = new ArrayList<>(plugin.getConfig().getStringList("modules.item-stacker.list"));
                if (a[1].equalsIgnoreCase("add")) { if (!list.contains(m.name())) list.add(m.name()); }
                else list.remove(m.name());
                plugin.getConfig().set("modules.item-stacker.list", list);
                plugin.saveConfig();
                plugin.msg(s, "<green>Item stacker list updated: <white>" + (list.isEmpty() ? "empty" : String.join(", ", list)));
            }
            case "restart" -> {
                if (a.length > 1 && a[1].equalsIgnoreCase("cancel")) plugin.restarter().cancel();
                else plugin.restarter().start("manual (" + s.getName() + ")");
            }
            default -> plugin.msg(s, "<gray>/ascendantcore <gui|reload|revive|stacker|restart>");
        }
        return true;
    }

    /** Reverse of the vanilla 4 string -> 1 cobweb recipe, so it can never create items out of nothing. */
    private boolean string(CommandSender s) {
        if (!(s instanceof Player p)) return true;
        if (!p.hasPermission("ascendantcore.string")) { plugin.msg(p, "<red>You don't have permission."); return true; }
        if (!plugin.enabled("string-command")) { plugin.msg(p, "<red>This command is disabled."); return true; }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType() != Material.COBWEB) { plugin.msg(p, "<red>Hold cobwebs in your main hand."); return true; }
        int total = hand.getAmount() * 4;
        p.getInventory().setItemInMainHand(null);               // remove first, give second
        while (total > 0) {
            int n = Math.min(64, total);
            Items.give(p, new ItemStack(Material.STRING, n));
            total -= n;
        }
        plugin.msg(p, "<green>Converted your cobwebs into string.");
        return true;
    }

    private boolean withdraw(CommandSender s, String[] a) {
        if (!(s instanceof Player p)) return true;
        if (!p.hasPermission("ascendantcore.withdrawheart")) { plugin.msg(p, "<red>You don't have permission."); return true; }
        int n = 1;
        if (a.length > 0) {
            try { n = Integer.parseInt(a[0]); } catch (NumberFormatException e) { plugin.msg(p, "<red>Usage: /withdrawheart [amount]"); return true; }
        }
        plugin.lifesteal().withdraw(p, n);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (!cmd.getName().equalsIgnoreCase("ascendantcore") || !s.hasPermission("ascendantcore.admin")) return out;
        if (a.length == 1) for (String o : List.of("gui", "reload", "revive", "stacker", "restart"))
            if (o.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(o);
        if (a.length == 2 && a[0].equalsIgnoreCase("stacker")) { out.add("add"); out.add("remove"); }
        if (a.length == 2 && a[0].equalsIgnoreCase("restart")) out.add("cancel");
        if (a.length == 2 && a[0].equalsIgnoreCase("revive")) plugin.getServer().getOnlinePlayers().forEach(pl -> out.add(pl.getName()));
        return out;
    }
}
