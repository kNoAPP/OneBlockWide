package com.knoban.atlas.utils;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import java.util.concurrent.TimeUnit;

/**
 * @author Alden Bansemer (kNoAPP)
 */
public class Tools {
    
	public static void clearFullInv(Player p) {
		p.getInventory().clear();
		p.getInventory().setBoots(new ItemStack(Material.AIR, 1));
		p.getInventory().setLeggings(new ItemStack(Material.AIR, 1));
		p.getInventory().setChestplate(new ItemStack(Material.AIR, 1));
		p.getInventory().setHelmet(new ItemStack(Material.AIR, 1));
		
		for(PotionEffect pe : p.getActivePotionEffects()) p.removePotionEffect(pe.getType());
	}
	
	public static Firework launchFirework(Location l, Color c, int power) {
		Firework fw = (Firework) l.getWorld().spawn(l, Firework.class);
		FireworkMeta data = fw.getFireworkMeta();
		data.setPower(power);
		data.addEffects(new FireworkEffect[]{FireworkEffect.builder().withColor(c).withColor(c).withColor(c).with(FireworkEffect.Type.BALL_LARGE).build()});
		fw.setFireworkMeta(data);
		return fw;
	}

	public static String millisToDHMSWithSpacing(long millis) {
		StringBuilder sb = new StringBuilder();
		long days = TimeUnit.MILLISECONDS.toDays(millis);
		boolean show = false;
		if(show || (show = days > 0)) {
			sb.append(String.format("%02d", days));
			sb.append("d ");
		}

		long hours = TimeUnit.MILLISECONDS.toHours(millis) % TimeUnit.DAYS.toHours(1);
		if(show || (show = hours > 0)) {
			sb.append(String.format("%02d", hours));
			sb.append("h ");
		}

		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1);
		if(show || (show = minutes > 0)) {
			sb.append(String.format("%02d", minutes));
			sb.append("m ");
		}

		long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1);
		if(show || (show = seconds > 0)) {
			sb.append(String.format("%02d", seconds));
			sb.append("s");
		}

		if(!show)
			return "0s";

		return sb.toString();
	}
}
