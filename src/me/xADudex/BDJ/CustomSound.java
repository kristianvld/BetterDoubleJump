package me.xADudex.BDJ;

import org.bukkit.Location;
import org.bukkit.Sound;

public class CustomSound {
	
	
	Sound sound;
	float volume, speed;
	
	CustomSound(Sound sound, float volume, float speed){
		this.sound = sound;
		this.volume = volume;
		this.speed = speed;
	}
	
	void play(Location loc){
		if(loc != null){
			loc.getWorld().playSound(loc, sound, volume, speed);
		}
	}

}
