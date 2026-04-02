package wfcore.common.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import wfcore.common.world.Retrofitter;

import java.io.File;

public class CommandRetrofit extends CommandBase {

    @Override
    public String getName() {
        return "wfcore";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/wfcore retrofit";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length > 0 && args[0].equalsIgnoreCase("retrofit")) {

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

        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Usage: " + getUsage(sender)));
        }
    }
}


