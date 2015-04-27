package me.xADudex.BDJ;

import java.util.ArrayList;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class StoredEffect {
	
	float x,y,z,speed_data;
	int count;
	Effect type;
	int id;
	int data;
	
	public StoredEffect(String name, float x, float y, float z, float speed_data, int count, int id, int data){
		type = Effect.valueOf(name);
		this.x = x;
		this.y = y;
		this.z = z;
		this.count = count;
		this.speed_data = speed_data;
		this.id = id;
		this.data = data;
	}
	
	public void play(Location loc){
		for(Player p : loc.getWorld().getPlayers()){
			p.spigot().playEffect(loc, type, id, data, x, y, z, speed_data, count, 256);
		}
	}
	
	public void play(Location loc, Player... players){
		for(Player p : players){
			p.spigot().playEffect(loc, type, id, data, x, y, z, speed_data, count, 256);
		}
	}
	
	public void play(Location loc, ArrayList<Player> players){
		for(Player p : players){
			p.spigot().playEffect(loc, type, id, data, x, y, z, speed_data, count, 256);
		}
	}

}
