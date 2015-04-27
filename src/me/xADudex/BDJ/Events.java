package me.xADudex.BDJ;

import java.util.UUID;

import me.xADudex.BDJ.Main.FallMode;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Events implements Listener{
	
	@EventHandler
	public void onFly(PlayerToggleFlightEvent event){
		final Player p = event.getPlayer();
		if(Main.hasJumpPerm(p) && p.getGameMode() != GameMode.CREATIVE && !Main.isDisabled(p) && !Main.jumping.contains(p.getUniqueId())){
			if(!Main.canJumpInside(p.getLocation()) || !Main.payFood(p)){
				event.setCancelled(true);
				return;
			}
			Vector jump = p.getLocation().getDirection();
			double y = jump.getY()+Main.ySpeed*Main.directionJump; 
			jump.multiply(Main.directionJump);
			jump.setY(y);
			p.setVelocity(jump);
			playJumpEffect(p);
			Main.jumping.add(p.getUniqueId());
			if(Main.fallMode == FallMode.RESET){
				p.setFallDistance(0F);
			}
			new BukkitRunnable(){
				int count = Main.ticks;
				boolean pos = true;
				public void run(){
					if(p.getGameMode() == GameMode.CREATIVE || !p.isOnline() || !Main.jumping.contains(p.getUniqueId()) || Main.isDisabled(p) 
							|| !Main.canJumpInside(p.getLocation())){
						new BukkitRunnable(){
							public void run(){
								Main.jumping.remove(p.getUniqueId());
							}
						}.runTaskLater(Main.pl, 5);
						this.cancel();
						return;
					}
					/*
					Material down = p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
					if(down.isSolid() || down == Material.WATER || down == Material.LAVA){
						p.setFlying(false);
						p.setAllowFlight(true);
						new BukkitRunnable(){
							public void run(){
								Main.jumping.remove(p.getUniqueId());
							}
						}.runTaskLater(Main.pl, 5);
						this.cancel();
						return;
					}
					*/
					if(p.getFallDistance() > 0F){
						pos = false;
					}
					if(p.getFallDistance() <= 0F){
						if(!pos){
							new BukkitRunnable(){
								public void run(){
									Main.jumping.remove(p.getUniqueId());
									p.setFlying(false);
									p.setAllowFlight(true);
								}
							}.runTaskLater(Main.pl, 5);
							this.cancel();
							return;
						}
					}
					if(count <= 0){
						p.setFlying(false);
						p.setAllowFlight(false);
						return;
					}
					count--;
					Vector v = p.getVelocity();
					v.add(p.getLocation().getDirection().multiply(Main.directionSpeed));
					v.setY(v.getY()*Main.ySpeed);
					p.setVelocity(v);
				}
			}.runTaskTimer(Main.pl, 0, 1);
		}
	}
	
	@EventHandler
	public void onGameModeChange(final PlayerGameModeChangeEvent event){
		final Player p = event.getPlayer();
		if(event.getNewGameMode() != GameMode.CREATIVE && !Main.isDisabled(p) && event.getNewGameMode() != GameMode.SPECTATOR){
			final boolean en = Main.canJumpInside(p.getLocation()) && !Main.isWorldEnabled(event.getPlayer().getWorld());
			final boolean rem = (en && !Main.jumping.contains(p.getUniqueId()));
			if(rem){
				new BukkitRunnable(){
					public void run(){
						if(rem){
							Main.jumping.remove(p.getUniqueId());	
						}
					}
				}.runTaskLater(Main.pl, 5);
				Main.jumping.add(event.getPlayer().getUniqueId());
			}
			new BukkitRunnable(){
				public void run(){
					p.setAllowFlight(en);
				}
			}.runTaskLater(Main.pl, 1);
		}
	}
	
	@EventHandler
	public void onDamage(EntityDamageEvent event){
		if(event.getCause() == DamageCause.FALL){
			if(event.getEntity() instanceof Player){
				Player p = (Player) event.getEntity();
				boolean dis = Main.isDisabled(p); 
				if(dis && p.getAllowFlight()) event.setCancelled(true);
				if(dis) return;
				
				if(Main.fallMode == FallMode.NONE_GLOBAL){
					event.setCancelled(true);
				} else if(Main.fallMode == FallMode.NONE){
					event.setCancelled(Main.canJumpInside(p.getLocation()) && Main.isWorldEnabled(p.getWorld().getName()) && Main.hasJumpPerm(p));
				} else if(Main.fallMode == FallMode.RESET_NONE){
					if(Main.jumping.contains(((Player)event.getEntity()).getUniqueId())){
						event.setCancelled(true);
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event){
		UUID id = event.getPlayer().getUniqueId();
		Main.jumping.remove(id);
		Main.wasInside.remove(id);
		Main.disabled.remove(id);
	}
	
	@EventHandler
	public void onTeleport(PlayerTeleportEvent event){
		if(event.getTo() != null && !Main.isDisabled(event.getPlayer())){
			if(event.getPlayer().getGameMode() == GameMode.SPECTATOR || event.getPlayer().getGameMode() == GameMode.CREATIVE){
				event.getPlayer().setAllowFlight(true);
				return;
			}
			
			boolean from = !Main.isWorldEnabled(event.getFrom().getWorld());
			boolean to = !Main.isWorldEnabled(event.getTo().getWorld());
			if(!from && to){
				event.getPlayer().setAllowFlight(false);
			} else if(!to && Main.hasJumpPerm(event.getPlayer())){
				event.getPlayer().setAllowFlight(true);
			} else if((to || !Main.hasJumpPerm(event.getPlayer())) && !from){
				event.getPlayer().setAllowFlight(false);
			} else {
				event.getPlayer().setAllowFlight(true);
			}
		}
	}
	
	@EventHandler
	public void onPortal(PlayerPortalEvent event){
		onTeleport(event);
	}
	
	void playJumpEffect(Player p){
		if(p == null){
			return;
		}
		Location l = p.getLocation();
		for(CustomSound c : Main.sounds){
			c.play(l);
		}
//		for(ParticleEffect e : Main.effects){
//			e.animateAtLocation(l);
//		}
		for(StoredEffect e : Main.effects){
			e.play(l);
		}
	}

}
