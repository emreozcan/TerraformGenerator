package org.terraform.data;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;

import java.util.Objects;

/**
 * class represent chunk location in simple form
 *
 * @author wysohn
 */
public class SimpleChunkLocation implements Cloneable {
    private final String world;
    private final int x;
    private final int z;

    public SimpleChunkLocation(String world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }

    /**
     * BLOCK COORD XYZ.
     * @param world
     * @param x
     * @param y
     * @param z
     */
    public SimpleChunkLocation(String world, int x, int y, int z) {
        this.world = world;
        this.x = x>>4;
        this.z = z>>4;
    }

    public SimpleChunkLocation(Chunk chunk) {
        this.world = chunk.getWorld().getName();
        this.x = chunk.getX();
        this.z = chunk.getZ();
    }

    public static SimpleChunkLocation of(Block block) {
        return new SimpleChunkLocation(block.getWorld().getName(), block.getX() >> 4, block.getZ() >> 4);
    }

    public static Chunk toChunk(SimpleChunkLocation loc) {
        return Bukkit.getWorld(loc.world).getChunkAt(loc.x, loc.z);
    }

    //world , x, z
    public static SimpleChunkLocation chunkStrToLoc(String chunk) {
        if (chunk == null) return null;
        String[] split = StringUtils.split(StringUtils.deleteWhitespace(chunk), ',');

        String world = split[0];
        int x = Integer.parseInt(split[1]);
        int z = Integer.parseInt(split[2]);

        return new SimpleChunkLocation(world, x, z);
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public SimpleChunkLocation getRelative(int nx, int nz) {
        return new SimpleChunkLocation(world, nx + x, nz + z);
    }

    @Override
    public SimpleChunkLocation clone() {
        return new SimpleChunkLocation(world, x, z);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * world.hashCode();
        result = prime * result + x;
        result = prime * result + z;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SimpleChunkLocation)) return false;
        SimpleChunkLocation other = (SimpleChunkLocation) obj;
        return x == other.x && z == other.z && Objects.equals(world, other.world);
    }

    @Override
    public String toString() {
        return world + ", " + x + ", " + z;
    }

    public Chunk toChunk() {
        return Bukkit.getWorld(world).getChunkAt(x, z);
    }

}
