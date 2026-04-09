package wfcore.common.drones;

import com.hbm.items.ModItems;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import wfcore.WFCore;
import wfcore.common.items.ItemRegistry;

public class EntitySuicideDrone extends Entity {

    // Energy Constants
    public static final long MAX_ENERGY = 100_000L;
    public static final long ENERGY_PER_TICK = 1L;
    public static final long ENERGY_PER_TICK_WHILE_ASCEND = 5L;
    public static final long MIN_ENERGY_TO_LAUNCH = 50_000L;

    // Fly Constants
    public static final double FLY_ALTITUDE = 50.0;
    public static final double ASCEND_SPEED = 0.6;
    public static final double FLY_SPEED = 0.75;
    public static final double DESCEND_SPEED = 1;
    public static final double START_KAMIKAZE_DIVE_DISTANCE = 5.0;
    public static final float EXPLOSION_RADIUS = 20.0f;

    // Misc Constants
    public static final int CHUNK_LOAD_RADIUS = 2;

    // NBT Data
    public long energy;
    public double targetX;
    public double targetY;
    public double targetZ;
    public boolean hasTarget = false;

    // Attributes
    private double directionX = 0;
    private double directionZ = 0;
    @Getter private DroneState state = DroneState.IDLE;

    // Constructors
    public EntitySuicideDrone(World world) {
        super(world);
        if (WFCore.DEBUG) energy = MAX_ENERGY;
        this.setSize(0.5F, 0.5F);
        this.noClip = false;
        this.isImmuneToFire = false;
    }

    // Update
    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) return;
        switch (state) {
            case IDLE -> tickIdle();
            case ASCENDING -> tickAscending();
            case FLYING -> tickFlying();
            case DIVING -> tickDiving();
            case OUT_OF_ENERGY -> tickOutOfEnergy();
        }
    }

    // State Tick Methods
    public void tickOutOfEnergy() {
        this.noClip = false;
        if (!this.onGround) { // gravity
            this.motionY -= 0.04;
        } else this.motionY = 0;
        this.motionX = 0;
        this.motionZ = 0;
        this.move(MoverType.SELF, motionX, motionY, motionZ);
        if (this.collided) explode();
    }
    public void tickIdle() {
        this.noClip = false;
        if (!this.onGround) { // gravity
            this.motionY -= 0.04;
        } else this.motionY = 0;
        this.motionX = 0;
        this.motionZ = 0;
        this.move(MoverType.SELF, motionX, motionY, motionZ);
    }
    public void tickAscending() {
        this.noClip = false;
        drainEnergy();
        forceNearbyChunkToLoad();

        if (this.posY >= FLY_ALTITUDE) {
            this.posY = FLY_ALTITUDE;
            this.motionY = 0;
            computeDirection();
            state = DroneState.FLYING;
        } else {
            this.motionX = 0;
            this.motionY = ASCEND_SPEED;
            this.motionZ = 0;
            move(MoverType.SELF, motionX, motionY, motionZ);
        }

        if (this.collided) explode();
    }
    public void tickFlying() {
        this.noClip = false;
        drainEnergy();
        forceNearbyChunkToLoad();

        double dx = targetX - this.posX;
        double dz = targetZ - this.posZ;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        if (horizontalDistance <= START_KAMIKAZE_DIVE_DISTANCE) {
            state = DroneState.DIVING;
            return;
        }

        double invDist = FLY_SPEED / horizontalDistance;
        this.motionX = dx * invDist;
        this.motionY = 0;
        this.motionZ = dz * invDist;
        move(MoverType.SELF, motionX, motionY, motionZ);
        this.rotationYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        if (this.collided) explode();
    }
    public void tickDiving() {
        this.noClip = false;
        drainEnergy();
        forceNearbyChunkToLoad();

        double dx = targetX - this.posX;
        double dy = targetY - this.posY;
        double dz = targetZ - this.posZ;
        double totalDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        double invDist = DESCEND_SPEED / totalDistance;
        this.motionX = dx * invDist;
        this.motionY = dy * invDist;
        this.motionZ = dz * invDist;
        move(MoverType.SELF, motionX, motionY, motionZ);
        this.rotationYaw   = (float) Math.toDegrees(Math.atan2(-dx, dz));
        this.rotationPitch = (float) Math.toDegrees(Math.asin(-this.motionY / DESCEND_SPEED));

        if (totalDistance < 2.0 || this.onGround || this.collided) {
            explode();
        }
    }

    // Utility Methods
    private boolean isTargetDesignator(Item item) {
        return item == ModItems.designator
                || item == ModItems.designator_range
                || item == ModItems.designator_manual;
    }
    private boolean isDetonator(Item item) {
        return item == ModItems.detonator
                || item == ModItems.detonator_multi
                || item == ModItems.detonator_laser;
    }
    public boolean tryLaunch() {
        if (!hasTarget) return false;
        if (energy <  MIN_ENERGY_TO_LAUNCH) return false;
        if (state != DroneState.IDLE) return false;
        state = DroneState.ASCENDING;
        this.motionY = ASCEND_SPEED;
        return true;
    }
    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.hasTarget = true;
    }
    public long addEnergy(long amount) {
        if (state != DroneState.IDLE) return 0;
        long added = Math.min(amount, MAX_ENERGY - energy);
        energy += added;
        return added;
    }
    public void drainEnergy() {
        if (state == DroneState.IDLE) return;
        long energyToDrain = state == DroneState.ASCENDING ? ENERGY_PER_TICK_WHILE_ASCEND : ENERGY_PER_TICK;
        energy = Math.max(0, energy - energyToDrain);
        if (energy == 0) state = DroneState.OUT_OF_ENERGY;
    }
    public void forceNearbyChunkToLoad() {
        if (this.ticksExisted % 10 != 0) return;
        int chunkX = (int) this.posX >> 4;
        int chunkZ = (int) this.posZ >> 4;

        for (int cx = chunkX - CHUNK_LOAD_RADIUS; cx <= chunkX + CHUNK_LOAD_RADIUS; cx++) {
            for (int cz = chunkZ - CHUNK_LOAD_RADIUS; cz <= chunkZ + CHUNK_LOAD_RADIUS; cz++) {
                world.getChunk(cx, cz);
            }
        }
    }
    public void computeDirection() {
        double dx = targetX - this.posX;
        double dz = targetZ - this.posZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.001) distance = 0.001;
        directionX = dx / distance;
        directionZ = dz / distance;
    }
    public void explode() {
        state = DroneState.DEAD;
        if (world.isRemote) return;
        world.createExplosion(this, posX, posY, posZ, EXPLOSION_RADIUS, true);
        this.setDead();
    }

    // Right Click Interaction
    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (world.isRemote) return true;
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty()) return false;
        Item item = held.getItem();

        // Target Designators
        if (isTargetDesignator(item)) {
            NBTTagCompound nbt = held.getTagCompound();
            if (nbt != null && nbt.hasKey("xCoord") && nbt.hasKey("zCoord")) {
                targetX = nbt.getInteger("xCoord");
                targetZ = nbt.getInteger("zCoord");
                hasTarget = true;
                player.sendMessage(new TextComponentString("\u00a7aTarget acquired: " + (int) targetX + " / " + (int) targetZ));
            } else {
                player.sendMessage(new TextComponentString("\u00a7cTarget designator has no coordinates!"));
            }
            return true;
        }

        // Detonator
        if (isDetonator(item) || (WFCore.DEBUG && item == Items.FLINT_AND_STEEL)) {
            if (state != DroneState.IDLE) {
                player.sendMessage(new TextComponentString("\u00a7eDrone already airborne."));
                return true;
            }
            if (energy < MAX_ENERGY) {
                player.sendMessage(new TextComponentString("\u00a7eThe drone battery is not full!"));
                return true;
            }
            if (!hasTarget) {
                player.sendMessage(new TextComponentString("\u00a7cNo target set! Right-click with a target designator first."));
                return true;
            }
            targetY = world.getHeight((int) targetX, (int) targetZ);
            state = DroneState.ASCENDING;
            this.motionY = ASCEND_SPEED;
            player.sendMessage(new TextComponentString("\u00a76Drone launched!"));
            return true;
        }
        return false;
    }

    // React to Attacks And Interactions
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
    @Override
    public boolean canBeAttackedWithItem() {
        return true;
    }
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (state == DroneState.IDLE) {
            this.entityDropItem(new ItemStack(ItemRegistry.ITEM_SUICIDE_DRONE, 1), 0);
            this.setDead();
            return true;
        }
        explode();
        return true;
    }

    // Override Methods
    @Override
    protected void entityInit() {
    }

    // NBT
    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        energy = tag.getLong("energy");
        targetX = tag.getDouble("targetX");
        targetY = tag.getDouble("targetY");
        targetZ = tag.getDouble("targetZ");
        hasTarget = tag.getBoolean("hasTarget");
        directionX = tag.getDouble("dirX");
        directionZ = tag.getDouble("dirZ");
        try {
            state = DroneState.valueOf(tag.getString("state"));
        } catch (Exception e) {
            state = DroneState.IDLE;
        }
    }
    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setLong("energy", energy);
        tag.setDouble("targetX", targetX);
        tag.setDouble("targetY", targetY);
        tag.setDouble("targetZ", targetZ);
        tag.setBoolean("hasTarget", hasTarget);
        tag.setString("state", state.name());
        tag.setDouble("dirX", directionX);
        tag.setDouble("dirZ", directionZ);
    }
}
