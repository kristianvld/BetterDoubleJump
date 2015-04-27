package me.xADudex.BDJ;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class Main extends JavaPlugin{
	
	private final static String adminPerm = "BetterDoubleJump.Admin";
	private final static String jumpPerm = "BetterDoubleJump.Jump";
	private final static String disablePerm = "BetterDoubleJump.Disable";
	private final static String disableOtherPerm = "BetterDoubleJump.DisableOther";
	private final static String foodBypassPerm = "BetterDoubleJump.FoodBypass";
	
	static ArrayList<UUID> disabled = new ArrayList<UUID>();
	
	static ArrayList<UUID> wasInside = new ArrayList<UUID>();
	
	static Main pl;
	
	Events events = new Events();
	
	static double ySpeed = 0.1;
	static double directionSpeed = 0.03;
	static double directionJump = 0.1;
	static int ticks = 5;
	
	static ArrayList<CustomSound> sounds = new ArrayList<CustomSound>();
//	static ArrayList<ParticleEffect> effects = new ArrayList<ParticleEffect>();
	static ArrayList<StoredEffect> effects = new ArrayList<StoredEffect>();
	
	static ArrayList<String> disabledWorlds = new ArrayList<String>();
	
	static ArrayList<UUID> jumping = new ArrayList<UUID>();
	
	static ArrayList<String> wgRegions = new ArrayList<String>();
	static boolean disableWGRegions = false;
	static WorldGuardPlugin wgPlugin = null; 
	
	static HashMap<Player, Float> fallDistance = new HashMap<Player, Float>();
	
	static FallMode fallMode = FallMode.NONE;
	
	static String bukkitPackage;
	static String nmsPackage;
	static String version;
	
	static enum FallMode {
		NONE_GLOBAL,NONE,VANILLA,RESET,RESET_NONE;
	}
	
	public void onEnable() {
		pl = this;
		
		bukkitPackage = this.getServer().getClass().getPackage().getName();
		version = bukkitPackage.substring(bukkitPackage.lastIndexOf('.') + 1);
		
		nmsPackage = "net.minecraft.server." + version;
		this.getLogger().info("Doing version check...");
		if(isVersionSupported()){
			this.getLogger().info("Version Supported");
		} else {
			this.getLogger().warning("######################################################################################");
			this.getLogger().warning("# This version is not supported, please inform the author xGamingDudex at bukkit dev #");
			this.getLogger().warning("# by making a comment at http://dev.bukkit.org/bukkit-plugins/betterdoublejump/      #");
			this.getLogger().warning("#------------------------------------------------------------------------------------#");
			this.getLogger().warning("# This plugin will now disable...                                                    #");
			this.getLogger().warning("######################################################################################");
			this.setEnabled(false);
			return;
		}
		
		Bukkit.getPluginManager().registerEvents(events, this);
		
		saveDefaultConfig();
		
		reloadConfigFile();
		this.getCommand("bdj").setExecutor(new CmdHandler());
		
		new BukkitRunnable(){
			public void run(){
				for(Player p : Bukkit.getOnlinePlayers()){
					
					if(getConfig().getBoolean("RunActiveCheck")){
						
						if(isDisabled(p)) continue;
						if(jumping.contains(p.getUniqueId())) continue;
						
						if(p.getGameMode() == GameMode.ADVENTURE || p.getGameMode() == GameMode.SURVIVAL){
							p.setAllowFlight(canJumpInside(p.getLocation()) && p.getFoodLevel() >= getConfig().getInt("Food.Min") && hasJumpPerm(p));
						}
						
						
					} else {
					
						if(!isWorldEnabled(p.getWorld()) || !hasJumpPerm(p)){
							continue;
						}
						if(jumping.contains(p.getUniqueId())) continue;
						if(disabled.contains(p.getUniqueId())) continue;
						
						boolean wasInside = Main.wasInside.contains(p.getUniqueId());
						boolean isInside = isInside(p.getLocation()); 
						if(wgPlugin == null){
							p.setAllowFlight(true);
							continue;
						}
						if(wasInside){
							if(!isInside){
								p.setAllowFlight(disableWGRegions);
								Main.wasInside.remove(p.getUniqueId());
							}
						} else {
							if(isInside){
								p.setAllowFlight(!disableWGRegions);
								Main.wasInside.add(p.getUniqueId());
							}
						}
					}
				}
			}
		}.runTaskTimer(this, 0, 5);
		
		new BukkitRunnable(){
			public void run(){
				for(Player p : Bukkit.getOnlinePlayers()){
					if(!p.getAllowFlight()) continue;
					float d = p.getFallDistance();
					if(d == 0F){
						Float prev = fallDistance.get(p);
						if(prev != null && prev > 0){
							fallDMG(p, prev);
						}
					}
					fallDistance.put(p, d);
				}
			}
		}.runTaskTimer(pl, 0, 1);
		
		try {
		    MetricsLite metrics = new MetricsLite(this);
		    metrics.start();
		} catch (IOException e) {
		    // Failed to submit the stats :-(
		}
		new BukkitRunnable(){
			public void run(){
				if(!pl.getConfig().getBoolean("CheckForUpdates")) return;
				int id = 84484;
				Updater updater = new Updater(pl, id, pl.getFile(), Updater.UpdateType.DEFAULT, false);
				if(updater.getResult() == Updater.UpdateResult.SUCCESS){
					pl.getLogger().info("Successfully updated to version '" + updater.getLatestName() + "'");
					pl.getLogger().info("Please reload or restart the server of the update to take affect");
					this.cancel();
				}
			}
		}.runTaskTimerAsynchronously(pl, 0, 20*60*30);
	}
	
	
	
	boolean isVersionSupported(){
		try {
			Class<?> cp = Class.forName(bukkitPackage + ".entity.CraftPlayer");
			Class<?> returnHandle = cp.getMethod("getHandle", null).getReturnType();
			Class<?> nmsPlayer = Class.forName(nmsPackage + ".EntityPlayer");
			if(!returnHandle.equals(nmsPlayer)) return false;
			Class<?> ds = Class.forName(nmsPackage + ".DamageSource");
			ds.getDeclaredField("FALL");
			nmsPlayer.getMethod("damageEntity", ds, float.class);
//			Class<?> pc = Class.forName(nmsPackage + ".PlayerConnection");
//			if(!pc.equals(nmsPlayer.getDeclaredField("playerConnection").getType()));
//			Class<?> parPack = Class.forName(nmsPackage + ".PacketPlayOutWorldParticles");
//			if(version.startsWith("v1.7")){
////				1.7.X CONSTRUCTOR
//				parPack.getConstructor(String.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class, int.class);
//				pc.getMethod("sendPacket", parPack.getSuperclass());
//			} else {
////				1.8.X CONSTRUCTOR
//				Class<?> type = Class.forName(nmsPackage + ".EnumParticle");
//				parPack.getConstructor(type, boolean.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class, int.class, int[].class);
//				pc.getMethod("sendPacket", parPack.getInterfaces()[0]);
//			}
			
			return true;
		} catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	
	void fallDMG(Player p, float d){
		if(jumping.contains(p.getUniqueId())) return;
		if(p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.WATER ||
				p.getLocation().getBlock().getType() == Material.WATER) return;
		if(p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.STATIONARY_WATER ||
				p.getLocation().getBlock().getType() == Material.STATIONARY_WATER) return;
		if(p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
		
		float f = 0f;
		for(PotionEffect ef : p.getActivePotionEffects()){
			if(ef.getType() == PotionEffectType.JUMP){
				f = ef.getAmplifier()+1;
				break;
			}
		}
		
		d = d-3F-f;
		
		int dmg = (int)d;
		if(d > dmg) dmg++;
		
		if(dmg <= 0) return;
		try {
			Object ep = p.getClass().getMethod("getHandle", (Class<?>[])null).invoke(p, (Object[])null);
			Class<?> ds = Class.forName(nmsPackage + ".DamageSource");
			Object dmgfall = ds.getDeclaredField("FALL").get(null);
			ep.getClass().getMethod("damageEntity", ds, float.class).invoke(ep, dmgfall, dmg);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
//		((CraftPlayer)p).getHandle().damageEntity(DamageSource.FALL, dmg) ;
	}
	
	void reloadConfigFile(){
		FileConfiguration c = getConfig();
		ySpeed = c.getDouble("Ymultiplier");
		directionSpeed = c.getDouble("DirectionMultiplier");
		directionJump = c.getDouble("JumpMultiplier");
		ticks = c.getInt("Ticks");
		
		String sounds = c.getString("Sounds");
		String effects = c.getString("Effects");
		Main.sounds.clear();
		Main.effects.clear();
		
		disabledWorlds.clear();
		disabledWorlds.addAll(Arrays.asList(c.getString("DisabledWorlds").split(",")));
		
		wgRegions.clear();
		wgRegions.addAll(Arrays.asList(c.getString("WorldGuardRegions").split(",")));
		
		disableWGRegions = c.getBoolean("DisableWGRegions");
		
		try {
			fallMode = FallMode.valueOf(c.getString("FallMode").toUpperCase());
		} catch (Exception e){
			getLogger().warning("Unknown fall mode: " + c.getString("FallMode"));
			getLogger().warning("Using default mode instead (NONE)");
			fallMode = FallMode.NONE;
		}
		
		for(String sound : sounds.split(",")){
			try {
				String[] args = sound.split("-");
				Main.sounds.add(new CustomSound(Sound.valueOf(args[0].toUpperCase()), Float.parseFloat(args[1]), Float.parseFloat(args[2])));
			}catch(Exception e){
				getLogger().warning("Unable to parse sound: " + sound);
			}
		}
		
		for(String effect : effects.split(",")){
			try {
				String[] args = effect.split("-");
				float spread = Float.parseFloat(args[1]);
				int id = 0;
				int data = 0;
				if(args.length == 6){
					id = Integer.parseInt(args[4]);
					data = Integer.parseInt(args[5]);
				}
				StoredEffect e = new StoredEffect(args[0].toUpperCase(), spread, spread, spread, Float.parseFloat(args[3]), Integer.parseInt(args[2]), id, data);
//				ParticleEffect pe = ParticleEffect.valueOfStringRaw(args[0]);
//				float spread = Float.parseFloat(args[1]);
//				pe.setStack(spread, spread, spread);
//				pe.setSpeedAndCount(Integer.parseInt(args[2]), Float.parseFloat(args[3]));
//				Main.effects.add(pe);
				Main.effects.add(e);
			}catch(Exception e){
				getLogger().warning("Unable to parse effect: " + effect + "; " + e.getMessage());
			}
		}
		
		Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
		if(wg != null && wg.isEnabled() && wg instanceof WorldGuardPlugin){
			wgPlugin = (WorldGuardPlugin)wg;
		} else {
			wgPlugin = null;
		}
	}
	
	static boolean canJumpInside(Location loc){
		if(wgPlugin == null) return true;
		
		String w = loc.getWorld().getName();
		
		if(!isWorldEnabled(w)) return false;
		
		return isInside(loc) != disableWGRegions;
	}
	
	static boolean isInside(Location loc){
		if(loc == null) return false;
		if(wgPlugin == null) return false;
		boolean inside = false;
		try {
			for(String r : wgPlugin.getRegionManager(loc.getWorld()).getApplicableRegionsIDs(BukkitUtil.toVector(loc))){
				if(wgRegions.contains(r)){
					inside = true;
					break;
				}
			}
			return inside;
		} catch(Exception e){
			return false;
		}
	}
	
	static boolean isWorldEnabled(World world){
		return isWorldEnabled(world.getName());
	}
	
	static boolean isWorldEnabled(String world){
		return disabledWorlds.contains(world) == pl.getConfig().getBoolean("OnlyDisabledWorlds");
	}
	
	static boolean hasAdminPerm(CommandSender p){
		if(p == null) return false;
		return p.hasPermission(adminPerm);
	}
	
	static boolean hasDisablePerm(CommandSender p){
		if(p == null) return false;
		return (hasAdminPerm(p) || hasDisableOtherPerm(p) || p.hasPermission(disablePerm));
	}
	
	static boolean hasDisableOtherPerm(CommandSender p){
		if(p == null) return false;
		return (hasAdminPerm(p) || p.hasPermission(disableOtherPerm));
	}
	
	static boolean hasJumpPerm(CommandSender p){
		if(p == null) return false;
		return (hasAdminPerm(p) || p.hasPermission(jumpPerm));
	}
	
	static boolean isDisabled(Player p){
		if(p == null) return false;
		return disabled.contains(p.getUniqueId());
	}
	
	static boolean payFood(Player p){
		if(hasAdminPerm(p) || p.hasPermission(foodBypassPerm)){
			return true;
		}
		if(p.getFoodLevel() < pl.getConfig().getInt("Food.Min")){
			return false;
		}
		int food = p.getFoodLevel();
		food -= pl.getConfig().getInt("Food.Cost");
		if(food < 0) food = 0;
		if(food > 20) food = 20;
		p.setFoodLevel(food);
		return true;
	}

}
