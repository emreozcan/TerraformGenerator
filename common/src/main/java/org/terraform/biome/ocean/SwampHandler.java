package org.terraform.biome.ocean;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.terraform.biome.BiomeBank;
import org.terraform.biome.BiomeHandler;
import org.terraform.coregen.HeightMap;
import org.terraform.coregen.PopulatorDataAbstract;
import org.terraform.coregen.bukkit.TerraformGenerator;
import org.terraform.data.SimpleLocation;
import org.terraform.data.TerraformWorld;
import org.terraform.main.config.TConfigOption;
import org.terraform.structure.small.WitchHutPopulator;
import org.terraform.tree.FractalTreeBuilder;
import org.terraform.tree.FractalTypes;
import org.terraform.utils.BlockUtils;
import org.terraform.utils.CoralGenerator;
import org.terraform.utils.GenUtils;
import org.terraform.utils.noise.FastNoise;
import org.terraform.utils.noise.NoiseCacheHandler;
import org.terraform.utils.noise.FastNoise.NoiseType;
import org.terraform.utils.noise.NoiseCacheHandler.NoiseCacheEntry;

import java.util.Random;

public class SwampHandler extends BiomeHandler {


    @Override
    public boolean isOcean() {
        return true;
    }

    @Override
    public Biome getBiome() {
        return Biome.SWAMP;
    }
    @Override
    public Material[] getSurfaceCrust(Random rand) {
        return new Material[]{GenUtils.randMaterial(rand, Material.GRASS_BLOCK, Material.PODZOL, Material.PODZOL),
                GenUtils.randMaterial(rand, Material.DIRT),
                GenUtils.randMaterial(rand, Material.DIRT, Material.DIRT, Material.STONE),
                GenUtils.randMaterial(rand, Material.DIRT, Material.STONE),
                GenUtils.randMaterial(rand, Material.DIRT, Material.STONE)};
    }


    @Override
    public void populateSmallItems(TerraformWorld tw, Random random, PopulatorDataAbstract data) {
        int seaLevel = TerraformGenerator.seaLevel;

        for (int x = data.getChunkX() * 16; x < data.getChunkX() * 16 + 16; x++) {
            for (int z = data.getChunkZ() * 16; z < data.getChunkZ() * 16 + 16; z++) {
                int y = GenUtils.getHighestGround(data, x, z);
                if (data.getBiome(x, z) != getBiome()) continue;
                if (!BlockUtils.isStoneLike(data.getType(x, y, z))) continue;
                if (y < seaLevel) {
                    
                    FastNoise mudNoise = NoiseCacheHandler.getNoise(
                    		tw, 
                    		NoiseCacheEntry.BIOME_SWAMP_MUDNOISE, 
                    		world -> {
                                FastNoise n = new FastNoise((int) (world.getSeed() * 4));
                                n.SetNoiseType(NoiseType.SimplexFractal);
                                n.SetFrequency(0.05f);
                                n.SetFractalOctaves(4);
                            
                    	        return n;
                    		});
                    
                    double noise = mudNoise.GetNoise(x,z);
                	
                	if (noise < 0) noise = 0;
                    int att = (int) Math.round(noise * 10);
                    if (att + y > seaLevel)
                        att = seaLevel - y;
                    for (int i = 1; i <= att; i++) {
                        if (i < att)
                            data.setType(x, y + i, z, getSurfaceCrust(random)[1]);
                        else
                            data.setType(x, y + i, z, getSurfaceCrust(random)[0]);
                    }
                    y += att;

                    if (y < TerraformGenerator.seaLevel) {
                        if (GenUtils.chance(random, 1, 30))
                            data.setType(x, TerraformGenerator.seaLevel + 1, z, Material.LILY_PAD);
                    }
                }

                if (GenUtils.chance(random, 10, 100) && y < TerraformGenerator.seaLevel - 3) { //SEA GRASS/KELP
                    CoralGenerator.generateKelpGrowth(data, x, y + 1, z);

                }
                if (GenUtils.chance(random, TConfigOption.BIOME_CLAY_DEPOSIT_CHANCE_OUT_OF_THOUSAND.getInt(), 1000)) {
                    BlockUtils.generateClayDeposit(x, y, z, data, random);
                }
            }
        }

    }

	@Override
	public void populateLargeItems(TerraformWorld tw, Random random, PopulatorDataAbstract data) {

        int treeX = 0, treeY, treeZ = 0;
        if (GenUtils.chance(random, 3, 10)) {
            treeX = GenUtils.randInt(random, 2, 12) + data.getChunkX() * 16;
            treeZ = GenUtils.randInt(random, 2, 12) + data.getChunkZ() * 16;

            if (data.getBiome(treeX, treeZ) == getBiome()) {
                treeY = GenUtils.getHighestGround(data, treeX, treeZ);
                
                if(treeY < TerraformGenerator.seaLevel) {
                	 //Don't do gradient checks for swamp trees, the mud is uneven.
                	//just make sure it's submerged
                    new FractalTreeBuilder(FractalTypes.Tree.SWAMP_BOTTOM)
                            .skipGradientCheck().build(tw, data, treeX, treeY - 3, treeZ);
                    new FractalTreeBuilder(FractalTypes.Tree.SWAMP_TOP)
                    		.skipGradientCheck().build(tw, data, treeX, treeY - 2, treeZ);
                }
            }
        }
        
        SimpleLocation[] roots = GenUtils.randomObjectPositions(tw, data.getChunkX(), data.getChunkZ(), 7, 0.6f);
        
        for (SimpleLocation sLoc : roots) {
            if (data.getBiome(sLoc.getX(),sLoc.getZ()) == getBiome()) {
                int rootY = GenUtils.getHighestGround(data, sLoc.getX(),sLoc.getZ());
                sLoc.setY(rootY);
                if(!BlockUtils.isDirtLike(data.getType(sLoc.getX(),sLoc.getY(),sLoc.getZ())))
                		continue;
                
                int minHeight = 3;
                if (sLoc.getY() < TerraformGenerator.seaLevel) {
                    minHeight = TerraformGenerator.seaLevel - sLoc.getY();
                }

                BlockUtils.spawnPillar(random, data, sLoc.getX(), sLoc.getY() + 1, sLoc.getZ(), Material.OAK_LOG, minHeight, minHeight + 3);
                
            }
        }
        
        WitchHutPopulator whp = new WitchHutPopulator();
        if (GenUtils.chance(tw.getHashedRand(data.getChunkX(), data.getChunkZ(), 66666), TConfigOption.STRUCTURES_SWAMPHUT_CHANCE_OUT_OF_TEN_THOUSAND.getInt(), 10000)) {
            whp.populate(tw, random, data);
        }
	}

	@Override
	public BiomeBank getBeachType() {
		return BiomeBank.MUDFLATS;
	}
	
	@Override
    public double calculateHeight(TerraformWorld tw, int x, int z) {
    	
        double height = HeightMap.CORE.getHeight(tw, x, z) - 10;

        //If the height is too low, force it back to 3.
        if (height <= 0) height = 3;
        
        return height;
    }

}
