package org.ch4rlesexe.cList;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminCommand implements CommandExecutor {

    private final CList plugin;

    public AdminCommand(CList plugin) {
        this.plugin = plugin;
    }

    private String translateColors(String message) {
        if (message == null) return "";
        Pattern hexPattern = Pattern.compile("#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder legacy = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                legacy.append("§").append(c);
            }
            message = message.replace("#" + hex, legacy.toString());
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = plugin.getConfig();

        String noPerm = translateColors(config.getString("messages.no_permission", "&cYou don't have permission."));
        String usage = translateColors(config.getString("messages.clist_usage", "&eUsage: /clist <reload>"));
        String reloadSuccess = translateColors(config.getString("messages.reload_success", "&aCList configuration reloaded!"));
        String unknown = translateColors(config.getString("messages.unknown_subcommand", "&cUnknown subcommand."));

        if (!sender.hasPermission("clist.admin")) {
            sender.sendMessage(noPerm);
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(usage);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(reloadSuccess);
            return true;
        }

        sender.sendMessage(unknown);
        return true;
    }
}
