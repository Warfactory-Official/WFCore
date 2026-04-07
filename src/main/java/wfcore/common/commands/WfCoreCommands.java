package wfcore.common.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import wfcore.api.radar.MultiblockRadarLogic;
import wfcore.api.util.math.ClusterData;
import wfcore.api.util.math.IntCoord2;
import wfcore.common.world.Retrofitter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WfCoreCommands extends CommandBase {

    @Override
    public String getName() {
        return "wfcore";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/wfcore <retrofit>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "retrofit" -> executeRetrofit(server, sender);
            case "radartest" -> executeRadarTest(server, sender);
            default -> sendUsage(sender);
        }
    }

    private void executeRadarTest(MinecraftServer server, ICommandSender sender) {
        World world = sender.getEntityWorld();
        
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Collecting valid entities for radar test..."));
        Map<IntCoord2, MultiblockRadarLogic.DataPoint> mockWorld = MultiblockRadarLogic.collectValidEntites(world);
        
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Calculating DBSCAN clusters..."));
        CompletableFuture<List<ClusterData>> clusterFuture = MultiblockRadarLogic.calculateDBSCAN(mockWorld, MultiblockRadarLogic.EPS, MultiblockRadarLogic.MIN_PTS);
        
        clusterFuture.thenAccept(clusters -> {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            try (FileWriter writer = new FileWriter(Paths.get("./mock.json").toFile())) {
                gson.toJson(mockWorld, writer);
                
                // Write clusters out to another file for testing
                try (FileWriter clusterWriter = new FileWriter(Paths.get("./clusters.json").toFile())) {
                    gson.toJson(clusters, clusterWriter);
                }
                
                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Radar test complete! Wrote " + mockWorld.size() + " points and " + clusters.size() + " clusters to JSON files."));
            } catch (IOException e) {
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "I/O error during radar test. Check console."));
                e.printStackTrace();
            }
        }).exceptionally(ex -> {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Error calculating clusters: " + ex.getMessage()));
            ex.printStackTrace();
            return null;
        });
    }

    private void executeRetrofit(MinecraftServer server, ICommandSender sender) {
        if (Retrofitter.active) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "A retrofit scan is already in progress!"));
            return;
        }

        File worldDir = server.getWorld(sender.getEntityWorld().provider.getDimension()).getChunkSaveLocation();
        File regionDir = new File(worldDir, "region");

        if (!regionDir.exists()) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Could not locate region folder!"));
            return;
        }

        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Starting background MCA scan... Check console for progress."));
        Retrofitter.INSTANCE.startGlobalScan(regionDir);
    }

    private void sendUsage(ICommandSender sender) {
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Usage: " + getUsage(sender)));
    }
}