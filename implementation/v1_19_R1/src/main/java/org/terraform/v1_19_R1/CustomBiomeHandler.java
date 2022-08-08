package org.terraform.v1_19_R1;

import java.lang.reflect.Field;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.terraform.biome.custombiomes.CustomBiomeType;
import org.terraform.main.TerraformGeneratorPlugin;

import com.mojang.serialization.Lifecycle;

import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryWritable;
import net.minecraft.core.RegistryMaterials;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeFog;
import net.minecraft.world.level.biome.BiomeFog.GrassColor;
import net.minecraft.world.level.biome.BiomeSettingsGeneration;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.biome.Biomes;

public class CustomBiomeHandler {
	
	public static final HashMap<CustomBiomeType, ResourceKey<BiomeBase>> terraformGenBiomeRegistry = new HashMap<>();

	@SuppressWarnings("deprecation")
	public static void init() {
		CraftServer craftserver = (CraftServer)Bukkit.getServer();
		DedicatedServer dedicatedserver = craftserver.getServer();
		MinecraftServer minecraftServer = MinecraftServer.getServer();
		//aP is BIOME_REGISTRY
		//aX is registryAccess
		//b is ownedRegistryOrThrow
		IRegistryWritable<BiomeBase> registrywritable = (IRegistryWritable<BiomeBase>) minecraftServer.aX().b(IRegistry.aR);
		
		//This thing isn't actually writable, so we have to forcefully UNFREEZE IT
		//ca is frozen
		try {
			Field frozen = RegistryMaterials.class.getDeclaredField("ca");
			frozen.setAccessible(true);
			frozen.set(registrywritable, false);
			TerraformGeneratorPlugin.logger.info("Unfreezing biome registry...");
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e1) {
			e1.printStackTrace();
		}
		
		BiomeBase forestbiome = registrywritable.a(Biomes.i); //forest
	
		for(CustomBiomeType type:CustomBiomeType.values()) {
			if(type == CustomBiomeType.NONE)
				continue;
			
			try {
				registerCustomBiomeBase(
						type,
						dedicatedserver,
						registrywritable,
						forestbiome
						);
				TerraformGeneratorPlugin.logger.info("Registered custom biome: " + type.toString().toLowerCase());
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				TerraformGeneratorPlugin.logger.error("Failed to register custom biome: " + type.getKey());
				e.printStackTrace();
			}
		}
		
		try {
			Field frozen = RegistryMaterials.class.getDeclaredField("ca");
			frozen.setAccessible(true);
			frozen.set(registrywritable, true);
			TerraformGeneratorPlugin.logger.info("Freezing biome registry");
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e1) {
			e1.printStackTrace();
		}

//		MinecraftServer ms = DedicatedServer.getServer();
//		ms.getCustomRegistry().d(IRegistry.aR).forEach(biomeBase -> {
//			TerraformGeneratorPlugin.logger.info("biome id " + ms.getCustomRegistry().d(IRegistry.aR).getKey(biomeBase) + " " +  ms.getCustomRegistry().d(IRegistry.aR).getId(biomeBase) );
//        });
		
	}
	
	@SuppressWarnings("deprecation")
	private static void registerCustomBiomeBase(CustomBiomeType biomeType, DedicatedServer dedicatedserver, IRegistryWritable<BiomeBase> registrywritable, BiomeBase forestbiome) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

		ResourceKey<BiomeBase> newKey = ResourceKey.a(IRegistry.aR, new MinecraftKey("terraformgenerator", biomeType.toString().toLowerCase()));

		//BiomeBase.a is BiomeBuilder
		BiomeBase.a newBiomeBuilder = new BiomeBase.a();
		
		//BiomeBase.b is ClimateSettings
		//d is temperatureModifier
		//This temperature modifier stuff is more cleanly handled below.
//		Class<?> climateSettingsClass = Class.forName("net.minecraft.world.level.biome.BiomeBase.b");
//		Field temperatureModififierField = climateSettingsClass.getDeclaredField("d");
//		temperatureModififierField.setAccessible(true);

		//i is climateSettings
		//Field f = BiomeBase.class.getDeclaredField("i");
		//f.setAccessible(true);
		//newBiomeBuilder.a((BiomeBase.TemperatureModifier) temperatureModififierField.get(f.get(forestbiome)));
		newBiomeBuilder.a(forestbiome.c()); //c is getPrecipitation

		//k is mobSettings
		Field biomeSettingMobsField = BiomeBase.class.getDeclaredField("k");
		biomeSettingMobsField.setAccessible(true);
		BiomeSettingsMobs biomeSettingMobs = (BiomeSettingsMobs) biomeSettingMobsField.get(forestbiome);
		newBiomeBuilder.a(biomeSettingMobs);

		//j is generationSettings
		Field biomeSettingGenField = BiomeBase.class.getDeclaredField("j");
		biomeSettingGenField.setAccessible(true);
		BiomeSettingsGeneration biomeSettingGen = (BiomeSettingsGeneration) biomeSettingGenField.get(forestbiome);
		newBiomeBuilder.a(biomeSettingGen);
		
		//newBiome.a(0.2F); //Depth of biome (Obsolete?)
		//newBiome.b(0.05F); //Scale of biome (Obsolete?)
		newBiomeBuilder.a(0.7F); //Temperature of biome
		newBiomeBuilder.b(biomeType.getRainFall()); //Downfall of biome

		//BiomeBase.TemperatureModifier.a will make your biome normal
		//BiomeBase.TemperatureModifier.b will make your biome frozen
		if(biomeType.isCold())
			newBiomeBuilder.a(BiomeBase.TemperatureModifier.b); 
		else
			newBiomeBuilder.a(BiomeBase.TemperatureModifier.a); 
		
		BiomeFog.a newFog = new BiomeFog.a();
		newFog.a(GrassColor.a); //This doesn't affect the actual final grass color, just leave this line as it is or you will get errors
		
		//Set biome colours. If field is empty, default to forest color
		
		//fogcolor
		newFog.a(biomeType.getFogColor().equals("") ? forestbiome.f():Integer.parseInt(biomeType.getFogColor(),16));
		
		//water color
		newFog.b(biomeType.getWaterColor().equals("") ? forestbiome.k():Integer.parseInt(biomeType.getWaterColor(),16)); 
		
		//water fog color
		newFog.c(biomeType.getWaterFogColor().equals("") ? forestbiome.l():Integer.parseInt(biomeType.getWaterFogColor(),16)); 
		
		//sky color
		newFog.d(biomeType.getSkyColor().equals("") ? forestbiome.a():Integer.parseInt(biomeType.getSkyColor(),16)); 

		//Unnecessary values; can be removed safely if you don't want to change them
		
		//foliage color (leaves, fines and more)
		newFog.e(biomeType.getFoliageColor().equals("") ? forestbiome.g():Integer.parseInt(biomeType.getFoliageColor(),16)); 
		
		//grass blocks color
		newFog.f(biomeType.getGrassColor().equals("") ? Integer.parseInt("79C05A",16):Integer.parseInt(biomeType.getGrassColor(),16)); 
		
		newBiomeBuilder.a(newFog.a());
		
		BiomeBase biome = newBiomeBuilder.a(); //biomebuilder.build();

		//Inject into the data registry for biomes
		//RegistryGeneration.a(RegistryGeneration.i, newKey, biome);
		
		//Inject into the biome registry
		//aP is BIOME_REGISTRY
		//aU is registryAccess
		//b is ownedRegistryOrThrow
		RegistryMaterials<BiomeBase> registry = ((RegistryMaterials<BiomeBase>)MinecraftServer.getServer().aX().b(IRegistry.aR));
		
		//a is ownedRegistryOrThrow
		registry.a(newKey, biome, Lifecycle.stable());
		
		terraformGenBiomeRegistry.put(biomeType, newKey);
	
	}
}