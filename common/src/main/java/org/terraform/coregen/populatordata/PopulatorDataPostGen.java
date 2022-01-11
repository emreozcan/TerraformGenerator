package org.terraform.coregen.populatordata;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.terraform.biome.BiomeBank;
import org.terraform.coregen.NaturalSpawnType;
import org.terraform.coregen.TerraLootTable;
import org.terraform.coregen.bukkit.TerraformGenerator;
import org.terraform.data.TerraformWorld;
import org.terraform.main.TerraformGeneratorPlugin;

public class PopulatorDataPostGen extends PopulatorDataICABiomeWriterAbstract {
    private final World w;
    private final Chunk c;

    public PopulatorDataPostGen(Chunk c) {
        this.w = c.getWorld();
        this.c = c;
    }

    /**
     * @return the w
     */
    public World getWorld() {
        return w;
    }


    /**
     * @return the c
     */
    public Chunk getChunk() {
        return c;
    }


    @Override
    public Material getType(int x, int y, int z) {
        return w.getBlockAt(x, y, z).getType();
    }

    @Override
    public BlockData getBlockData(int x, int y, int z) {
        return w.getBlockAt(x, y, z).getBlockData();
    }

    @Override
    public void setType(int x, int y, int z, Material type) {
        boolean isFragile = type.toString().contains("DOOR") ||
                type.toString().contains("CARPET") ||
                type == Material.FARMLAND ||
                type == Material.WATER;
        //TerraformGeneratorPlugin.injector.getICAData(w.getBlockAt(x,y,z).getChunk())
        //.setType(x, y, z, type);
        Block b = w.getBlockAt(x, y, z);
        b.setType(type, !isFragile);
    }

    @Override
    public void setBlockData(int x, int y, int z, BlockData data) {
        boolean isFragile = data.getMaterial().toString().contains("DOOR") ||
        		data.getMaterial().toString().contains("CARPET") ||
                data.getMaterial() == Material.FARMLAND ||
                data.getMaterial() == Material.WATER;
        //TerraformGeneratorPlugin.injector.getICAData(w.getBlockAt(x,y,z).getChunk())
        //.setBlockData(x, y, z, data);
        Block b = w.getBlockAt(x, y, z);
        b.setBlockData(data.clone(), !isFragile);
    }

    /**
     * Blockstates are mutable, so just edit them. There is no method to directly set them.
     * @param x
     * @param y
     * @param z
     * @param state
     * @return
     */
    public BlockState getBlockState(int x, int y, int z) {
        Block b = w.getBlockAt(x, y, z);
        return b.getState();
    }

    public void noPhysicsUpdateForce(int x, int y, int z, BlockData data) {
        Block b = w.getBlockAt(x, y, z);
        b.setBlockData(data.clone(), false);
    }

    @Override
    public Biome getBiome(int rawX, int rawZ) {
        return w.getBlockAt(rawX, TerraformGenerator.seaLevel, rawZ).getBiome();
    }

    @Override
    public void setBiome(int rawX, int rawY, int rawZ, Biome biome) {
        w.setBiome(rawX, rawY, rawZ, biome);
    }

    @Override
    public int getChunkX() {
        return c.getX();
    }

    @Override
    public int getChunkZ() {
        return c.getZ();
    }

    @Override
    public void addEntity(int x, int y, int z, EntityType type) {
    	//Always offset by 0.5 to prevent them spawning in corners.
    	//Y is offset by a small bit to prevent falling through weird spawning areas
        Entity e = c.getWorld().spawnEntity(new Location(c.getWorld(),x+0.5,y+0.3,z+0.5), type);
        e.setPersistent(true);
    }
    
    private static int spawnerRetries = 0;
    @Override
    public void setSpawner(int rawX, int rawY, int rawZ, EntityType type) {
        Block b = w.getBlockAt(rawX, rawY, rawZ);
        b.setType(Material.SPAWNER, false);
        try {
            CreatureSpawner spawner = (CreatureSpawner) b.getState();
            spawner.setSpawnedType(type);
            spawner.update();
        }
        catch(IllegalStateException | ClassCastException e)
        {
        	spawnerRetries++;
        	if(spawnerRetries > 10){ 
            	Bukkit.getLogger().info("Giving up on spawner at " + rawX + "," + rawY + "," + rawZ);
            	spawnerRetries = 0;
        		return;
        	}
        	Bukkit.getLogger().info("Failed to get state for spawner at " + rawX + "," + rawY + "," + rawZ + ", try " + spawnerRetries);
        	setSpawner(rawX, rawY, rawZ, type);
        }
    }

    @Override
    public void lootTableChest(int x, int y, int z, TerraLootTable table) {
        TerraformGeneratorPlugin.injector.getICAData(w.getBlockAt(x, y, z).getChunk()).lootTableChest(x, y, z, table);
    }

	@Override
	public TerraformWorld getTerraformWorld() {
		return TerraformWorld.get(w);
	}

	@Override
	public void setBiome(int rawX, int rawY, int rawZ, BiomeBank biomebank) {
		PopulatorDataICAAbstract icad = TerraformGeneratorPlugin.injector.getICAData(w.getBlockAt(rawX, rawY, rawZ).getChunk());
		if(icad instanceof PopulatorDataICABiomeWriterAbstract)
		((PopulatorDataICABiomeWriterAbstract) icad).setBiome(rawX, rawY, rawZ, biomebank);
	}

	@Override
	public void registerNaturalSpawns(NaturalSpawnType type, int x0, int y0, int z0, int x1, int y1, int z1) {
		PopulatorDataICAAbstract icad = TerraformGeneratorPlugin.injector.getICAData(w.getBlockAt(x0,y0,z0).getChunk());
		if(icad instanceof PopulatorDataICABiomeWriterAbstract)
		((PopulatorDataICABiomeWriterAbstract) icad).registerNaturalSpawns(type, x0, y0, z0, x1, y1, z1);
	}

	@Override
	public void spawnMinecartWithChest(int x, int y, int z, TerraLootTable table, Random random) {
		PopulatorDataICAAbstract icad = TerraformGeneratorPlugin.injector.getICAData(w.getBlockAt(x,y,z).getChunk());
		if(icad instanceof PopulatorDataICABiomeWriterAbstract)
		((PopulatorDataICABiomeWriterAbstract) icad).spawnMinecartWithChest(x,y,z,table,random);
	}
}