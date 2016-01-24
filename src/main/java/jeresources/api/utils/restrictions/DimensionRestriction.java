package jeresources.api.utils.restrictions;

import jeresources.api.messages.utils.MessageKeys;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import java.util.*;

public class DimensionRestriction
{
    public static final DimensionRestriction OVERWORLD = new DimensionRestriction(0);
    public static final DimensionRestriction NETHER = new DimensionRestriction(-1);
    public static final DimensionRestriction END = new DimensionRestriction(1);
    public static final DimensionRestriction NONE = new DimensionRestriction();

    private int min;
    private int max;
    private Type type;

    public DimensionRestriction()
    {
        this.type = Type.NONE;
    }

    public DimensionRestriction(int dim)
    {
        this(dim, dim);
    }

    public DimensionRestriction(Type type, int dim)
    {
        this(type, dim, dim);
    }

    public DimensionRestriction(int minDim, int maxDim)
    {
        this(Type.WHITELIST, minDim, maxDim);
    }

    public DimensionRestriction(Type type, int minDim, int maxDim)
    {
        this.type = type;
        this.min = Math.min(minDim, maxDim);
        this.max = Math.max(maxDim, minDim);
    }

    public DimensionRestriction(NBTTagCompound tagCompound)
    {
        this.type = Type.values()[tagCompound.getByte(MessageKeys.type)];
        int minDim = tagCompound.getInteger(MessageKeys.min);
        int maxDim = tagCompound.getInteger(MessageKeys.max);
        this.min = Math.min(minDim, maxDim);
        this.max = Math.max(maxDim, minDim);
    }

    public List<String> getValidDimensions(BlockRestriction blockRestriction)
    {
        Set<Integer> dimensions = DimensionRegistry.getDimensions(blockRestriction);
        if (dimensions != null) return getDimensionString(dimensions);
        return getAltDimensionString(DimensionRegistry.getAltDimensions());
    }

    private Set<Integer> getValidDimensions(Set<Integer> dimensions)
    {
        if (type == Type.NONE) return dimensions;
        Set<Integer> result = new TreeSet<Integer>();
        for (int dimension : dimensions)
        {
            if (dimension >= min == (type == Type.WHITELIST) == dimension <= max) result.add(dimension);
        }
        return result;
    }

    private List<String> getDimensionString(Set<Integer> dimensions)
    {
        return getStringList(getValidDimensions(dimensions));
    }

    private List<String> getStringList(Set<Integer> set)
    {
        List<String> result = new ArrayList<>();
        for (Integer i : set)
        {
            String dimName = DimensionRegistry.getDimensionName(i);
            if (dimName != null) result.add("  " + dimName);
        }
        return result;
    }

    private List<String> getAltDimensionString(Set<Integer> dimensions)
    {
        Set<Integer> validDimensions = new TreeSet<Integer>();
        int dimMin = Integer.MAX_VALUE;
        int dimMax = Integer.MIN_VALUE;
        for (Integer dim : dimensions)
        {
            if (dim < dimMin) dimMin = dim;
            if (dim > dimMax) dimMax = dim;
        }
        for (int i = Math.min(min, dimMin) - 1; i <= Math.max(max, dimMax) + 1; i++)
            if (!dimensions.contains(i)) validDimensions.add(i);
        List<String> result = getStringList(getValidDimensions(type != Type.NONE ? validDimensions : dimensions));
        if (result.isEmpty()) result.add(StatCollector.translateToLocal("ner.dim.no"));
        switch (type)
        {
            default:
                break;
            case NONE:
                result.add(0, StatCollector.translateToLocal("ner.not"));
                break;
            case BLACKLIST:
                result.add(0, "<=");
                result.add(result.size(), "=<");
        }
        return result;
    }

    public NBTTagCompound writeToNBT()
    {
        return writeToNBT(new NBTTagCompound());
    }

    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound)
    {
        tagCompound.setInteger(MessageKeys.max, max);
        tagCompound.setInteger(MessageKeys.min, min);
        tagCompound.setByte(MessageKeys.type, (byte) type.ordinal());
        return tagCompound;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof DimensionRestriction)
        {
            DimensionRestriction other = (DimensionRestriction) obj;
            return other.min == min && other.max == max && other.type == type;
        }
        return false;
    }

    public boolean isMergeable(DimensionRestriction other)
    {
        if (other.type == Type.NONE) return true;
        int dimMin = Math.min(min, other.min) - 1;
        int dimMax = Math.max(max, other.max) + 1;
        Set<Integer> testDimensions = new TreeSet<Integer>();
        for (int dim = dimMin; dim <= dimMax; dim++)
            testDimensions.add(dim);
        Set<Integer> thisValidDimensions = getValidDimensions(testDimensions);
        Set<Integer> otherValidDimensions = other.getValidDimensions(testDimensions);
        return otherValidDimensions.containsAll(thisValidDimensions);
    }

    @Override
    public String toString()
    {
        return "Dimension: " + type + (type != Type.NONE ? " " + min + "-" + max : "");
    }

    @Override
    public int hashCode()
    {
        return type.hashCode() ^ min ^ max;
    }
}
