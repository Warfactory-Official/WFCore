package wfcore.api.radar;

import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wfcore.WFCore;

import java.util.Objects;

public class RadarTargetIdentifier {
    @NotNull
    public final String id;
    @Nullable
    public final String blockState;

    public int intensity = 0;

    public RadarTargetIdentifier(@NotNull String id, @Nullable String blockState) {
        this.id = id;
        this.blockState = blockState;
    }

    public RadarTargetIdentifier intensity(int intensity) {
        this.intensity = intensity;
        return this;
    }

    @Override
    public String toString() {
        return id;
        /*
        StringBuilder propertiesString = new StringBuilder("[");
        var propertyEntries = properties.object2ObjectEntrySet();
        var propIt = propertyEntries.iterator();

        // iterate over all properties and stringify
        while (propIt.hasNext()) {
            var property = propIt.next();
            propertiesString.append(property.getKey());
            propertiesString.append(": ");
            propertiesString.append(property.getValue());
            if (propIt.hasNext()) { propertiesString.append(", "); }
        }

        propertiesString.append("]");
        return propertiesString.toString();
         */
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) { return true; }

        if (other instanceof RadarTargetIdentifier identifier) {
            return this.blockState == null ? Objects.equals(id, identifier.id) : Objects.equals(id, identifier.id) && Objects.equals(blockState, identifier.blockState);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    // will try, in increasing preference, the blockstate string, then the resource location, then display name string if possible
    /*
    Data ranked based on priority
    1. GT metaID
    2. TE ID
    3. TE ID + blockstate (if blockstate is specified)


     */
    public static RadarTargetIdentifier getBestIdentifier(TileEntity targetTE) {
        String identifier;
        String serializedBlockState = null;
        ResourceLocation teResource = TileEntity.getKey(targetTE.getClass());
        IBlockState state = targetTE.getWorld().getBlockState(targetTE.getPos());
        //GT metadata is our best bet
        if(targetTE instanceof IGregTechTileEntity mte){
            identifier = mte.getMetaTileEntity().metaTileEntityId.toString();
        }
        else {
            //NonGT stuff is a lot less fancy
            identifier = teResource.toString();
            serializedBlockState = Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(state.getBlock())).toString();
        }

        return new RadarTargetIdentifier(identifier,  serializedBlockState);
    }



    public static RadarTargetIdentifier getBestIdentifier(Entity target) {
        // get the entity key
        ResourceLocation entityKey = EntityList.getKey(target);

        // get the nbt data associated with the entity - may be useful later
        //NBTTagCompound entityNBT = target.getEntityData();

        if (entityKey == null) {
            WFCore.LOGGER.atError().log("Got an entity with no resource location: " + target);
            entityKey = new ResourceLocation(target.getClass().getName());
        }

        return new RadarTargetIdentifier(entityKey.toString(), null );
    }
}
