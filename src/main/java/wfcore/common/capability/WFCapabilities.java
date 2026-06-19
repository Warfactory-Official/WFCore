package wfcore.common.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import wfcore.api.capability.IACEnergyContainer;

/** WFCore-registered Forge capabilities. */
public final class WFCapabilities {

    @CapabilityInject(IACEnergyContainer.class)
    public static Capability<IACEnergyContainer> CAPABILITY_AC_ENERGY = null;

    private WFCapabilities() {}

    public static void register() {
        CapabilityManager.INSTANCE.register(IACEnergyContainer.class,
                new Capability.IStorage<IACEnergyContainer>() {
                    @Override
                    public NBTBase writeNBT(Capability<IACEnergyContainer> capability, IACEnergyContainer instance,
                                            EnumFacing side) {
                        return null;
                    }

                    @Override
                    public void readNBT(Capability<IACEnergyContainer> capability, IACEnergyContainer instance,
                                        EnumFacing side, NBTBase nbt) {}
                },
                () -> IACEnergyContainer.DEFAULT);
    }
}
