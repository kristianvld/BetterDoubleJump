package me.xADudex.BDJ;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdHandler implements CommandExecutor {

	static String a = ChatColor.AQUA + "";
	static String g = ChatColor.GOLD + "";
	static String pref = g + "[" + a + "BetterDoubleJump" + g + "] " + a; 
	static String noPermMSG = pref + ChatColor.DARK_RED + "You don't have permission to use that command!";
	static String notPlayer = pref + "You must be a player to use that command";
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(args.length == 0){
			boolean hasPermForCommand = false;
			sender.sendMessage(g + ChatColor.STRIKETHROUGH + "                          " + 
					a + " BetterDoubleJump " + g + ChatColor.STRIKETHROUGH + "                          ");
			sender.sendMessage(g + "Author: " + a + "xADudex aka xGamingDudex");
			sender.sendMessage(g + "Version: " + a + Main.pl.getDescription().getVersion());
			sender.sendMessage(g + "Avalible Commands:");
			if(Main.hasAdminPerm(sender)){
				sender.sendMessage(g + " - " + a + "reload " + g + "- " + a + "Reload the config file");
				hasPermForCommand = true;
			}
			if(Main.hasDisableOtherPerm(sender)){
				sender.sendMessage(g + " - " + a + "disable [player] " + g + "- " + a + "Disable double jump");
				hasPermForCommand = true;
			} else if(Main.hasDisablePerm(sender)){
				sender.sendMessage(g + " - " + a + "disable " + g + "- " + a + "Disable double jump");
				hasPermForCommand = true;
			}
			if(!hasPermForCommand){
				sender.sendMessage(g + " -" + a + " none");
			}
		} else if(args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")){
			if(Main.hasAdminPerm(sender)){
				Main.pl.reloadConfig();
				Main.pl.reloadConfigFile();
				sender.sendMessage(pref + "Config successfully reloaded");
			} else {
				sender.sendMessage(noPermMSG);
			}
		} else if(args[0].equalsIgnoreCase("disable")){
			if(args.length >= 2 && Main.hasDisableOtherPerm(sender)){
				Player p = getPlayer(args[1]);
				if(p != null){
					String state = toggleDisable(p)? "Disabled" : "Enabled";
					sender.sendMessage(pref + "Double jump has been " + g + state + a + " for " + g + p.getName());
				} else {
					sender.sendMessage(pref + "Could not find any players by that name!");
				}
			} else if(Main.hasDisablePerm(sender)){
				if(sender instanceof Player){
					Player p = (Player) sender;
					String state = toggleDisable(p)? "Disabled" : "Enabled";
					sender.sendMessage(pref + "You have " + g + state + a + " double jump");
				} else {
					sender.sendMessage(notPlayer);
				}
			} else {
				sender.sendMessage(noPermMSG);
			}
		} else {
			sender.sendMessage(pref + "Unknown command");
		}
		return false;
	}
	
	static boolean toggleDisable(Player p){
		if(!Main.disabled.remove(p.getUniqueId())){
			Main.disabled.add(p.getUniqueId());
			p.setAllowFlight(false);
			return true;
		}
		p.setAllowFlight(true);
		return false;
	}
	
	static Player getPlayer(String name) {
		name = name.toLowerCase();
		int off = -1;
		Player found = null;
		for(Player p : Bukkit.getOnlinePlayers()){
			String pn = p.getName().toLowerCase();
			if(pn.equals(name)){
				return p;
			} else if(pn.startsWith(name)){
				int poff = name.length()-pn.length();
				if(poff < off || off == -1){
					found = p;
					off = poff;
				}
			}
		}
		return found;
	}
}
