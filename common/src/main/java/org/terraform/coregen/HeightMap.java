package org.terraform.coregen;

import org.terraform.biome.BiomeSection;
import org.terraform.coregen.bukkit.TerraformGenerator;
import org.terraform.data.SimpleLocation;
import org.terraform.data.TerraformWorld;
import org.terraform.main.TConfigOption;
import org.terraform.main.TerraformGeneratorPlugin;
import org.terraform.utils.FastNoise;
import org.terraform.utils.GenUtils;
import org.terraform.utils.FastNoise.NoiseType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

public enum HeightMap {
    /**
     * Current river depth, also returns negative values if on dry ground.
     */
    RIVER {
        @Override
        public double getHeight(TerraformWorld tw, int x, int z) {
            FastNoise noise = computeNoise(tw, world -> {
                FastNoise n = new FastNoise();
                n.SetSeed((int) tw.getSeed());
                n.SetNoiseType(NoiseType.PerlinFractal);
                n.SetFrequency(TConfigOption.HEIGHT_MAP_RIVER_FREQUENCY.getFloat());
                n.SetFractalOctaves(5);
                return n;
            });
            return 15 - 100 * Math.abs(noise.GetNoise(x, z));
        }
    },
    CORE {
        @Override
        public double getHeight(TerraformWorld tw, int x, int z) {
            FastNoise cubic = computeNoise(tw, world -> {
                FastNoise n = new FastNoise((int) tw.getSeed());
                n.SetNoiseType(NoiseType.CubicFractal);
                n.SetFractalOctaves(6);
                n.SetFrequency(TConfigOption.HEIGHT_MAP_CORE_FREQUENCY.getFloat());
                return n;
            });

            double height = cubic.GetNoise(x, z) * 2 * 15 + 13 + defaultSeaLevel;

            //Ensure that height doesn't automatically go upwards sharply
            if (height > defaultSeaLevel + 10) {
                height = (height - defaultSeaLevel - 10) * 0.1 + defaultSeaLevel + 10;
            }

            //Ensure that height doesn't automatically go too deep
            if (height < defaultSeaLevel - 30) {
                height = -(defaultSeaLevel - 30 - height) * 0.1 + defaultSeaLevel - 30;
            }

            return height;
        }
    }, ATTRITION {
        @Override
        public double getHeight(TerraformWorld tw, int x, int z) {
            FastNoise perlin = computeNoise(tw, world -> {
                FastNoise n = new FastNoise((int) tw.getSeed());
                n.SetNoiseType(NoiseType.PerlinFractal);
                n.SetFractalOctaves(4);
                n.SetFrequency(0.02f);
                return n;
            });

            double height = perlin.GetNoise(x, z) * 2 * 7;
            return height < 0 ? 0 : height;
        }
    };

    public static final int defaultSeaLevel = 62;
    public static final float heightAmplifier = TConfigOption.HEIGHT_MAP_LAND_HEIGHT_AMPLIFIER.getFloat();
    protected final Map<TerraformWorld, FastNoise> noiseCache = Collections.synchronizedMap(new IdentityHashMap<>(TerraformWorld.WORLDS.size()));

    protected FastNoise computeNoise(TerraformWorld world, Function<TerraformWorld, FastNoise> noiseFunction) {
        synchronized(noiseCache) {
            return noiseCache.computeIfAbsent(world, noiseFunction);
        }
    }

    /**
     * Returns the average increase or decrease in height for surrounding blocks compared to the provided height at those coords.
     * 1.5 for a radius of 3 is considered steep.
     */
    public static double getNoiseGradient(TerraformWorld tw, int x, int z, int radius) {
        double totalChangeInGradient = 0;
        int count = 0;
        double centerNoise = getBlockHeight(tw, x, z);
        for (int nx = -radius; nx <= radius; nx++)
            for (int nz = -radius; nz <= radius; nz++) {
                if (nx == 0 && nz == 0) continue;
                //Bukkit.getLogger().info(nx + "," + nz + ":"+(getHeight(tw,x+nx,z+nz)-centerNoise));
                totalChangeInGradient += Math.abs(getBlockHeight(tw, x + nx, z + nz) - centerNoise);
                count++;
            }
        //Bukkit.getLogger().info("Count: " + count);
        //Bukkit.getLogger().info("Total: " + totalChangeInGradient);

        return totalChangeInGradient / count;
    }

    public static double getRawRiverDepth(TerraformWorld tw, int x, int z) {
    	double depth = HeightMap.RIVER.getHeight(tw, x, z);
        depth = depth < 0 ? 0 : depth;
        return depth;
    }

    public static double getPreciseHeight(TerraformWorld tw, int x, int z) {
        ChunkCache cache = TerraformGenerator.getCache(tw, x, z);

        double cachedValue = cache.getHeight(x, z);
        if (cachedValue != 0) return cachedValue;

        double height = getRiverlessHeight(tw,x,z);
    	
    	//River Depth
        double depth = getRawRiverDepth(tw,x,z);

        //Normal scenario: Shallow area
        if (height - depth >= TerraformGenerator.seaLevel - 15) {
            height -= depth;

            //Fix for underwater river carving: Don't carve deeply
        } else if (height > TerraformGenerator.seaLevel - 15 
        		&& height - depth < TerraformGenerator.seaLevel - 15) {
            height = TerraformGenerator.seaLevel - 15;
        }

        if (heightAmplifier != 1f && height > TerraformGenerator.seaLevel) 
        	height += heightAmplifier * (height - TerraformGenerator.seaLevel);

    	
        cache.cacheHeight(x, z, height);
        return height;
    }

    static boolean debugged = false;
    /**
     * Biome calculations are done here as well.
     * @param tw
     * @param x
     * @param z
     * @return
     */
    public static double getRiverlessHeight(TerraformWorld tw, int x, int z) {
        double dither = TConfigOption.BIOME_DITHER.getDouble();
    	Random locationBasedRandom  = new Random(Objects.hash(tw.getSeed(),x,z));
    	SimpleLocation target  = new SimpleLocation(x,0,z);
    	
    	Collection<BiomeSection> sections = BiomeSection.getSurroundingSections(tw, x, z);
    	HashMap<BiomeSection, Double> dominanceMap = new HashMap<>();
    	
    	double height = 0;
    	double totalDom = 0;
    	double lowestDom = 0;
    	for(BiomeSection sect:sections) {
    		double dom = (sect.getDominance(target)+GenUtils.randDouble(locationBasedRandom,-dither,dither));
    		if(dom < lowestDom) lowestDom = dom;
    		dominanceMap.put(sect, dom);
    	}
    	
    	//Make all dominance values positive
    	if(lowestDom < 0) {
        	for(BiomeSection sect:sections) {
        		dominanceMap.put(sect, dominanceMap.get(sect)+Math.abs(lowestDom));
        		totalDom += dominanceMap.get(sect);
        	}
    	}
        
    	//Calculate the height based on the percentage dominance
    	for(BiomeSection sect:sections) {
    		double multiplier = 0.25;
    		if(totalDom > 0)
    			multiplier = dominanceMap.get(sect)/totalDom;
    		
    		height += sect.getBiomeBank().getHandler()
    				.calculateHeight(tw, x, z) 
    				* multiplier;
//    		if((dominanceMap.get(sect)/totalDom) > 1) {
//    			TerraformGeneratorPlugin.logger.info("Weird percentage detected => " + (dominanceMap.get(sect)/totalDom));
//				TerraformGeneratorPlugin.logger.info("=> Total Dom : " + totalDom);
//    			for(BiomeSection s:sections)
//    				TerraformGeneratorPlugin.logger.info("=> " + s + " : " + dominanceMap.get(sect) );
//    		}
    	}
    	
    	if(!debugged && height != sections.iterator().next().getBiomeBank().getHandler()
				.calculateHeight(tw, x, z)) {
    		debugged =  true;
    		TerraformGeneratorPlugin.logger.info("-=[DEBUG HEIGHTMAP]=-");
    		TerraformGeneratorPlugin.logger.info("Total dom: " + totalDom);
    		TerraformGeneratorPlugin.logger.info("Result: " + height);
    		TerraformGeneratorPlugin.logger.info("Lowest Dom: " + lowestDom);
    		for(BiomeSection sect:sections) {
        		double multiplier = 0.25;
        		if(totalDom > 0)
        			multiplier = dominanceMap.get(sect)/totalDom;
    			TerraformGeneratorPlugin.logger.info("    " + sect);
    			TerraformGeneratorPlugin.logger.info("    Dominance Map:  " + dominanceMap.get(sect));
    			TerraformGeneratorPlugin.logger.info("    Multiplier: " + multiplier);
    			TerraformGeneratorPlugin.logger.info("    Section Height: " + sect.getBiomeBank().getHandler()
    					.calculateHeight(tw, x, z));
    			TerraformGeneratorPlugin.logger.info("----------------------------------------");
    		}
    	}
		return height;
    }

	public static int getBlockHeight(TerraformWorld tw, int x, int z) {
        return (int) getPreciseHeight(tw, x, z);
    }

    public abstract double getHeight(TerraformWorld tw, int x, int z);
}
