package work.mgnet.ffa;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.google.inject.Inject;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.schematic.MCEditSchematicFormat;
import com.sk89q.worldedit.sponge.SpongeWorldEdit;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

@SuppressWarnings("deprecation")
@Plugin(id = "ffa", name = "FFA", version = "1.0", description = "Adds FFA")
public class FFA {
	
	@Inject
	@DefaultConfig(sharedRoot = true)
	private Path defaultConfig;

	@Inject
	@DefaultConfig(sharedRoot = true)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;

	@Inject
	@ConfigDir(sharedRoot = false)
	private Path privateConfigDir;
	
	public static Path potentialFile;
	public static boolean isRunning = false;
	
	public static Location<World> equipLocation;
	public static Location<World> pvpLocation;
	public static Location<World> chestLocation;
	public static HashMap<String, Inventory> inves = new HashMap<>();
	
	public static ArrayList<String> players = new ArrayList<>();
	
	public ConfigurationNode loadConfig() throws IOException {
		potentialFile = Paths.get(privateConfigDir.toString(), "config.yml");
		ConfigurationNode node;
		if (!potentialFile.toFile().exists()) {
			privateConfigDir.toFile().mkdir();
			potentialFile.toFile().createNewFile();
			configManager = HoconConfigurationLoader.builder().setPath(potentialFile).build();
			node = configManager.createEmptyNode();
		} else {
			configManager = HoconConfigurationLoader.builder().setPath(potentialFile).build();
			node = configManager.load();
		}
		
		if (node.getNode("equipPos").getString() == null) node.getNode("equipPos").setValue("100 100 100");
		if (node.getNode("pvpPos").getString() == null) node.getNode("pvpPos").setValue("50 100 50");
		if (node.getNode("chestPos").getString() == null) node.getNode("chestPos").setValue("100 100 50");
		if (node.getNode("tickrate").getString() == null) node.getNode("tickrate").setValue(20);
		configManager.save(node);
		
		String el = node.getNode("equipPos").getString();
		equipLocation = new Location<World>(Sponge.getServer().getWorlds().iterator().next(), Integer.parseInt(el.split(" ")[0]), Integer.parseInt(el.split(" ")[1]), Integer.parseInt(el.split(" ")[2]));
		
		String pl = node.getNode("pvpPos").getString();
		pvpLocation = new Location<World>(Sponge.getServer().getWorlds().iterator().next(), Integer.parseInt(pl.split(" ")[0]), Integer.parseInt(pl.split(" ")[1]), Integer.parseInt(pl.split(" ")[2]));
		
		String cl = node.getNode("chestPos").getString();
		chestLocation = new Location<World>(Sponge.getServer().getWorlds().iterator().next(), Integer.parseInt(cl.split(" ")[0]), Integer.parseInt(cl.split(" ")[1]), Integer.parseInt(cl.split(" ")[2]));
		return node;
	}
	
	public void startGame(ConfigurationNode node) throws CommandException {
		isRunning = true;
		Sponge.getCommandManager().get("execute").get().getCallable().process(Sponge.getServer().getConsole(), "@p -40 8 -155 spreadplayers ~ ~ 50 150 false @a");
		Sponge.getCommandManager().get("tickrate").get().getCallable().process(Sponge.getServer().getConsole(), node.getNode("tickrate").getInt() + "");
		for (Player player : Sponge.getGame().getServer().getOnlinePlayers()) {
			player.offer(Keys.GAME_MODE, GameModes.SURVIVAL);
			player.health().set(20.0);
			player.sendMessage(Text.of("§b»§7 The Game has begun. Kill everyone to win"));
		}
	}
	
	@Listener
	public void onServer(GameStartedServerEvent e) throws IOException, ObjectMappingException {
		ConfigurationNode node = loadConfig();
		CommandSpec ready = CommandSpec.builder().description(Text.of("Ready!!!!!!!")).executor(new CommandExecutor() {
			
			@Override
			public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
				if (players.contains(src.getName())) return CommandResult.builder().successCount(1).build();
				players.add(src.getName());
				
				for (Player player : Sponge.getGame().getServer().getOnlinePlayers()) player.sendMessage(Text.of("§b»§a " + src.getName() + "§7 is now ready!"));
				
				if (players.size() == Sponge.getGame().getServer().getOnlinePlayers().size()) {	
					startGame(node);
				}
				return CommandResult.builder().successCount(1).build();
			}
		}).build();
		CommandSpec mapreload = CommandSpec.builder().description(Text.of("Reload the map")).permission("mgw.edit").executor(new CommandExecutor() {
			
			@Override
			public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
				try {
					loadMap();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				return CommandResult.builder().successCount(1).affectedEntities(Sponge.getGame().getServer().getOnlinePlayers().size()).build();
			}
		}).build();
		CommandSpec forceend = CommandSpec.builder().description(Text.of("Forceend the game")).permission("mgw.start").executor(new CommandExecutor() {
			
			@Override
			public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
				endGame();
				return CommandResult.builder().successCount(1).affectedEntities(Sponge.getGame().getServer().getOnlinePlayers().size()).build();
			}
		}).build();
		CommandSpec editInv = CommandSpec.builder().description(Text.of("Change the Inventory")).permission("mgw.edit").executor(new CommandExecutor() {
			
			@Override
			public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
				Player p = (Player) src;
				p.playSound(SoundTypes.BLOCK_NOTE_BELL, p.getLocation().getPosition(), 1.0);
				Inventory source = ((Chest) chestLocation.getTileEntity().get()).getDoubleChestInventory().get();
				p.openInventory(source);
				return CommandResult.builder().successCount(1).affectedItems(0).build();
			}
		}).build();
		CommandSpec forcestart = CommandSpec.builder().description(Text.of("Forcestart")).permission("mgw.start").executor(new CommandExecutor() {
			
			@Override
			public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
				players.clear();
				for (Player player : Sponge.getGame().getServer().getOnlinePlayers()) players.add(player.getName());
				startGame(node);
				return CommandResult.builder().successCount(1).build();
			}
		}).build();
		CommandSpec getInv = CommandSpec.builder().description(Text.of("Open the Inventory")).executor(new CommandExecutor() {
			
			@Override
			public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
				Player p = (Player) src;
				p.playSound(SoundTypes.BLOCK_NOTE_BELL, p.getLocation().getPosition(), 1.0);
				
				if (inves.containsKey(p.getName())) {
					p.openInventory(inves.get(p.getName()));
					return CommandResult.builder().successCount(1).affectedItems(0).build();
				}
				
				Inventory source = ((Chest) chestLocation.getTileEntity().get()).getDoubleChestInventory().get();
				Inventory dest = Inventory.builder().of(InventoryArchetypes.DOUBLE_CHEST).build(Sponge.getPluginManager().getPlugins().iterator().next());
				ArrayList<ItemStack> stacks = new ArrayList<>();
				for (int i = 0; i < 54; i++) {
					try {
						ItemStack is = source.poll().get();
						dest.offer(is.copy());
						stacks.add(is);
					} catch (Exception e) {
						
					}
				}
				for (ItemStack itemStack : stacks) {
					source.offer(itemStack);
				}
				inves.put(p.getName(), dest);
				p.openInventory(dest);
				return CommandResult.builder().successCount(1).affectedItems(0).build();
			}
		}).build();
		
		Sponge.getCommandManager().register(this, getInv, "items");
		Sponge.getCommandManager().register(this, forceend, "forceend");
		Sponge.getCommandManager().register(this, forcestart, "forcestart");
		Sponge.getCommandManager().register(this, mapreload, "reloadmap");
		Sponge.getCommandManager().register(this, ready, "ready");
		Sponge.getCommandManager().register(this, editInv, "setitems");
	}
	
	@Listener
	public void onLogin(ClientConnectionEvent.Join e) {
		e.setMessageCancelled(true);
		if (isRunning) {
			e.getTargetEntity().offer(Keys.GAME_MODE, GameModes.SPECTATOR);
			e.getTargetEntity().getInventory().clear();
		} else {
			e.getTargetEntity().setLocation(equipLocation);
			e.getTargetEntity().offer(Keys.GAME_MODE, GameModes.ADVENTURE);
			e.getTargetEntity().getInventory().clear();
			e.getTargetEntity().sendMessage(Text.of("§b»§7 Type §a/items §7to see all the items you can get. When you are ready, type §a/ready§7."));
		}
	}

	public void endGame() {
		if (!isRunning) return;
		players.clear();
		inves.clear();
		isRunning = false;
		for (Player player : Sponge.getGame().getServer().getOnlinePlayers()) {
			player.setLocation(equipLocation);
			player.getInventory().clear();
			player.sendMessage(Text.of("§b»§7 The Game has ended"));
			player.offer(Keys.GAME_MODE, GameModes.ADVENTURE);
		}
		try {
			Sponge.getCommandManager().get("tickrate").get().getCallable().process(Sponge.getServer().getConsole(), "20");
			loadMap();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	@Listener
	public void onDrop(DropItemEvent e) {
		if (!isRunning) e.setCancelled(true);
	}
	
	@Listener
	public void onPvP(DamageEntityEvent e) {
		if (!isRunning) e.setCancelled(true);
	}
	
	@Listener
	public void onLeave(ClientConnectionEvent.Disconnect e) throws CommandException {
		players.remove(e.getTargetEntity().getName());
		if (players.size() == 1) endGame();
	}
	
	@Listener
	public void onDeath(DestructEntityEvent.Death e) throws CommandException {
		if (e.getTargetEntity().getType() == EntityTypes.PLAYER) {
			if (players.contains(((Player) e.getTargetEntity()).getName())) {
				players.remove(((Player) e.getTargetEntity()).getName());
				e.getTargetEntity().offer(Keys.GAME_MODE, GameModes.SPECTATOR);
			}
			if (players.size() == 1) {
				Player winner = Sponge.getServer().getPlayer(players.get(0)).get();
				for (Player player : Sponge.getGame().getServer().getOnlinePlayers()) {
					player.sendTitle(Title.of(Text.of(winner.getName() + " won!")));
				}
				endGame();
			}
		}
	}
	
	public void loadMap() throws Exception {
		EditSession sess = WorldEdit.getInstance().getEditSessionFactory().getEditSession(SpongeWorldEdit.inst().getWorld(Sponge.getServer().getWorlds().iterator().next()), -1);
		
		File schem = new File(privateConfigDir.toString(), "map.schem");
		
		CuboidClipboard cl = MCEditSchematicFormat.getFormat(schem).load(schem);
		cl.paste(sess, new Vector(0, 0, 0), false, true);
	}
	
}
