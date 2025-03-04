package org.ch4rlesexe.cList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ListCommand implements CommandExecutor {

    private final CList plugin;

    public ListCommand(CList plugin) {
        this.plugin = plugin;
    }

    // Converts hex color codes (#RRGGBB) to §x§R§R§G§G§B§B and then applies &-codes
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

        String listMessageRaw = config.getString("list message");
        String header = "";

        List<ParsedGroup> parsedGroups = new ArrayList<>();

        if (listMessageRaw != null && !listMessageRaw.isEmpty()) {
            String[] lines = listMessageRaw.split("\\r?\\n");
            if (lines.length > 0) {
                header = translateColors(lines[0]);
                for (int i = 1; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.isEmpty()) continue;
                    String delimiter = null;
                    if (line.contains(" - ")) {
                        delimiter = " - ";
                    } else if (line.contains(":")) {
                        delimiter = ":";
                    }
                    if (delimiter == null) continue;

                    String[] parts = line.split(Pattern.quote(delimiter), 2);
                    if (parts.length < 2) continue;

                    String disp = parts[0].trim();
                    String permKeyRaw = parts[1].trim();

                    int max = config.getInt("max-per-group", 100);
                    if (permKeyRaw.contains("|")) {
                        String[] split = permKeyRaw.split("\\|", 2);
                        permKeyRaw = split[0].trim();
                        try {
                            max = Integer.parseInt(split[1].trim());
                        } catch (NumberFormatException ignored) {
                            max = config.getInt("max-per-group", 100);
                        }
                    }

                    parsedGroups.add(new ParsedGroup(permKeyRaw, disp, max, i));
                }
            }
        } else {
            header = translateColors(config.getString("message", "&aPlayers online:"));

            ConfigurationSection groupsSection = config.getConfigurationSection("groups");
            if (groupsSection != null) {
                List<Group> groupsList = new ArrayList<>();
                for (String key : groupsSection.getKeys(false)) {
                    String disp = groupsSection.getString(key + ".display", key);
                    int priority = groupsSection.getInt(key + ".priority", 0);
                    int max = groupsSection.getInt(key + ".max", config.getInt("max-per-group", 100));
                    groupsList.add(new Group(key, disp, priority, max));
                }
                groupsList.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
                int index = 1;
                for (Group g : groupsList) {
                    parsedGroups.add(new ParsedGroup(g.getName(), g.getDisplay(), g.getMax(), index));
                    index++;
                }
            }
        }

        ConfigurationSection groupsSection = config.getConfigurationSection("groups");
        Map<String, Group> groupsFromSection = new HashMap<>();
        if (groupsSection != null) {
            for (String key : groupsSection.getKeys(false)) {
                String disp = groupsSection.getString(key + ".display", key);
                int priority = groupsSection.getInt(key + ".priority", 0);
                int max = groupsSection.getInt(key + ".max", config.getInt("max-per-group", 100));
                groupsFromSection.put(key.toLowerCase(), new Group(key, disp, priority, max));
            }
        }

        List<FinalGroup> finalGroups = new ArrayList<>();
        for (ParsedGroup pg : parsedGroups) {
            if (groupsFromSection.containsKey(pg.permission.toLowerCase())) {
                Group g = groupsFromSection.get(pg.permission.toLowerCase());
                finalGroups.add(new FinalGroup(g.getName(), g.getDisplay(), pg.order, g.getMax()));
            } else {
                finalGroups.add(new FinalGroup(pg.permission, pg.display, pg.order, pg.max));
            }
        }

        for (String key : groupsFromSection.keySet()) {
            boolean found = false;
            for (FinalGroup fg : finalGroups) {
                if (fg.permission.equalsIgnoreCase(key)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Group g = groupsFromSection.get(key);
                finalGroups.add(new FinalGroup(g.getName(), g.getDisplay(), finalGroups.size() + 1, g.getMax()));
            }
        }

        finalGroups.sort(Comparator.comparingInt(fg -> fg.order));

        Map<String, List<String>> groupPlayers = new LinkedHashMap<>();
        for (FinalGroup fg : finalGroups) {
            groupPlayers.put(fg.permission.toLowerCase(), new ArrayList<>());
        }

        int totalPlayers = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            FinalGroup assigned = null;
            for (FinalGroup fg : finalGroups) {
                if (p.hasPermission("clist.group." + fg.permission)) {
                    assigned = fg;
                    break;
                }
            }
            if (assigned == null) {
                for (FinalGroup fg : finalGroups) {
                    if (fg.permission.equalsIgnoreCase("default")) {
                        assigned = fg;
                        break;
                    }
                }
            }
            if (assigned != null) {
                groupPlayers.get(assigned.permission.toLowerCase()).add(p.getName());
                totalPlayers++;
            }
        }

        header = header.replace("{online}", String.valueOf(totalPlayers));

        List<String> outputLines = new ArrayList<>();
        outputLines.add(header);

        for (FinalGroup fg : finalGroups) {
            List<String> players = groupPlayers.get(fg.permission.toLowerCase());
            if (players == null || players.isEmpty()) continue;

            players.sort(String.CASE_INSENSITIVE_ORDER);
            int count = players.size();

            String placeholder = "(" + fg.permission.toLowerCase() + "count)";
            String displayWithCount = translateColors(fg.display).replace(placeholder, "(" + count + ")");

            StringBuilder sb = new StringBuilder();
            sb.append(displayWithCount).append(": ");

            int maxToShow = (fg.max == -1 ? count : Math.min(count, fg.max));
            List<String> subList = players.subList(0, maxToShow);
            sb.append(String.join(", ", subList));

            if (count > maxToShow) {
                sb.append(" ").append(translateColors("&7(and " + (count - maxToShow) + " more)"));
            }

            outputLines.add(sb.toString());
        }

        for (String line : outputLines) {
            sender.sendMessage(line);
        }

        return true;
    }

    private static class ParsedGroup {
        String permission;
        String display;
        int max;
        int order;

        ParsedGroup(String permission, String display, int max, int order) {
            this.permission = permission;
            this.display = display;
            this.max = max;
            this.order = order;
        }
    }

    private static class FinalGroup {
        String permission;
        String display;
        int order;
        int max;

        FinalGroup(String permission, String display, int order, int max) {
            this.permission = permission;
            this.display = display;
            this.order = order;
            this.max = max;
        }
    }
}
