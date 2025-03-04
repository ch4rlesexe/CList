package org.ch4rlesexe.cList;

import org.bukkit.plugin.java.JavaPlugin;

public class CList extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("list").setExecutor(new ListCommand(this));
        getCommand("clist").setExecutor(new AdminCommand(this));
    }
}
