package org.maowtm.mc.warden_warn;

import de.tr7zw.nbtapi.NBTEntity;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.stream.Collectors;

public class WardenWarn extends JavaPlugin {
    private BukkitTask update_task;

    @Override
    public void onEnable() {
        update_task = getServer().getScheduler().runTaskTimer(this, this::updateAllPlayers, 20, 20);
    }

    @Override
    public void onDisable() {
        update_task.cancel();
    }

    private static class WardenSpawnTracker {
        public int warning_level;
        public int ticks_since_last;

        public WardenSpawnTracker(int warning_level, int ticks_since_last) {
            this.warning_level = warning_level;
            this.ticks_since_last = ticks_since_last;
        }
    }

    static final String ST_NBT_KEY = "warden_spawn_tracker";
    static final int WARNING_LEVEL_RESET_TICKS = 12000;

    private WardenSpawnTracker getPlayerWardenSpawn(Player p) {
        var nbtent = new NBTEntity(p);
        if (!nbtent.hasKey(ST_NBT_KEY)) {
            return new WardenSpawnTracker(0, 0);
        }
        var c = nbtent.getCompound(ST_NBT_KEY);
        return new WardenSpawnTracker(c.getInteger("warning_level"), c.getInteger("ticks_since_last_warning"));
    }

    private void clearPlayerWardenSpawn(Player p) {
        var nbtent = new NBTEntity(p);
        if (nbtent.hasKey(ST_NBT_KEY)) {
//            nbtent.removeKey(ST_NBT_KEY);
            nbtent.getCompound(ST_NBT_KEY).setInteger("warning_level", 0);
            nbtent.getCompound(ST_NBT_KEY).setInteger("ticks_since_last_warning", 0);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final var cmd = command.getName();
        switch (cmd) {
            case "getwardenlevel", "clearwardenlevel" -> {
                if (args.length != 1) {
                    return false;
                }
                var p = getServer().getPlayer(args[0]);
                if (p == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                if (cmd.equals("clearwardenlevel")) {
                    clearPlayerWardenSpawn(p);
                    sender.sendMessage("Cleared warden level for " + args[0]);
                } else {
                    var res = getPlayerWardenSpawn(p);
                    if (res.warning_level == 0) {
                        sender.sendMessage("Player warning level is 0.");
                    } else {
                        sender.sendMessage(String.format("Player warning level is %d (last update %s ago)", res.warning_level, Utils.ticksToHumanTime(res.ticks_since_last)));
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        final var cmd = command.getName();
        switch (cmd) {
            case "getwardenlevel", "clearwardenlevel" -> {
                if (args.length != 1) {
                    return List.of();
                }
                return getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            }
        }
        return List.of();
    }

    private void updateAllPlayers() {
        for (var p : getServer().getOnlinePlayers()) {
            var wl = getPlayerWardenSpawn(p);
            if (wl.warning_level != 0) {
                if (!p.getLocation().getBlock().getBiome().equals(Biome.DEEP_DARK)) {
                    return;
                }
                String text;
                net.md_5.bungee.api.ChatColor color;
                int reset_ticks = WARNING_LEVEL_RESET_TICKS - wl.ticks_since_last;
                reset_ticks += (wl.warning_level - 1) * WARNING_LEVEL_RESET_TICKS;
                var reset_rem_time = Utils.ticksToHumanTime(reset_ticks);
                if (wl.warning_level < 3) {
                    text = String.format("Warden warning: %d left before spawn. (Resets in %s)", 3 - wl.warning_level, reset_rem_time);
                    color = net.md_5.bungee.api.ChatColor.YELLOW;
                } else {
                    text = String.format("Warden will spawn on next shriek. (Resets in %s)", reset_rem_time);
                    color = net.md_5.bungee.api.ChatColor.RED;
                }
                if (wl.warning_level == 4 && p.getNearbyEntities(32, 32, 32).stream().anyMatch(e -> e.getType().equals(EntityType.WARDEN))) {
                    // Player already facing a warden so don't bother.
                    continue;
                }
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder()
                        .color(color).append(text).create());
            }
        }
    }
}
