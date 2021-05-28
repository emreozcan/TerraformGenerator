package org.terraform.utils.noise;

import java.util.function.Function;

import org.terraform.data.TerraformWorld;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * To help handle noise object caching throughout the entire plugin.
 * No more stupid hashmap caches all over the place.
 *
 */
public class NoiseCacheHandler{
	
	public static enum NoiseCacheEntry{
		TW_TEMPERATURE,
		TW_MOISTURE,
		TW_OCEAN,
		
		HEIGHTMAP_CORE,
		HEIGHTMAP_RIVER,
		//HEIGHTMAP_MOUNTAINOUS,
		HEIGHTMAP_ATTRITION,
		
		BIOME_BEACH_HEIGHT,
		
		BIOME_BADLANDS_PLATEAU_DISTORTEDCIRCLE,
		BIOME_BADLANDS_PLATEAUNOISE,
		BIOME_BADLANDS_WALLNOISE,
		BIOME_BADLANDS_PLATEAUDETAILS,
		
		BIOME_BAMBOOFOREST_PATHNOISE,
		
		BIOME_ERODEDPLAINS_CLIFFNOISE,
		BIOME_ERODEDPLAINS_DETAILS,
		
		BIOME_FOREST_PATHNOISE,

        BIOME_TAIGA_BERRY_BUSHNOISE,

		BIOME_JUNGLE_GROUNDWOOD,
		BIOME_JUNGLE_GROUNDLEAVES,
		BIOME_JUNGLE_LILYPADS,

		BIOME_DESERT_DUNENOISE,
		
		BIOME_SWAMP_MUDNOISE,
		
		STRUCTURE_LARGECAVE_CARVER,
		
		STRUCTURE_MUSHROOMCAVE_MYCELNOISE,
		
		STRUCTURE_PYRAMID_BASEELEVATOR,
		STRUCTURE_PYRAMID_BASEFUZZER,
		
		STRUCTURE_ANIMALFARM_FIELDNOISE,
		STRUCTURE_ANIMALFARM_RADIUSNOISE,
		
		GENUTILS_RANDOMOBJ_NOISE,
		
		FRACTALTREES_LEAVES_NOISE,
		FRACTALTREES_BASE_NOISE,
		;
	}
	
    private static final LoadingCache<NoiseCacheHandler.NoiseCacheKey, FastNoise> NOISE_CACHE = 
    		CacheBuilder.newBuilder()
    		.maximumSize(300).build(new NoiseCacheLoader());
	
    public static FastNoise getNoise(TerraformWorld world, NoiseCacheEntry entry, Function<TerraformWorld, FastNoise> noiseFunction) {
        NoiseCacheKey key = new NoiseCacheKey(world,entry);
        FastNoise noise = NOISE_CACHE.getIfPresent(key);
        if(noise == null) {
        	noise = noiseFunction.apply(world);
        	NOISE_CACHE.put(key, noise);
        }
        return noise;
        
    }
    
	public static class NoiseCacheLoader extends CacheLoader<NoiseCacheHandler.NoiseCacheKey, FastNoise> {
		/**
		 * Does not do loading. 
		 * If this is null, the caller is responsible for inserting it.
		 */
		@Override
		public FastNoise load(NoiseCacheKey key) throws Exception {
			return null;
		}
	}
	
	public static class NoiseCacheKey {
		private TerraformWorld tw;
		private NoiseCacheEntry entry;
		
		public NoiseCacheKey(TerraformWorld world, NoiseCacheEntry entry) {
			this.tw = world;
			this.entry = entry;
		}

		@Override
		public int hashCode() {
	        return tw.hashCode() ^ (entry.hashCode() * 31);
	    }
		
		@Override
		public boolean equals(Object other) {
			if(other instanceof NoiseCacheKey) {
				NoiseCacheKey o = (NoiseCacheKey) other;
				if(!o.tw.getName().equals(tw.getName()))
					return false;
				return entry == o.entry;
			}
			return false;
		}
	}
	

}
