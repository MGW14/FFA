package work.mgnet.ffa;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
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

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

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
	public static HashMap<Player, Inventory> inves = new HashMap<>();
	
	public static ArrayList<Player> players = new ArrayList<>();
	
	@Listener
	public void onServer(GameStartedServerEvent e) throws IOException, ObjectMappingException {
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
		configManager.save(node);
		
		String el = node.getNode("equipPos").getString();
		equipLocation = new Location<World>(Sponge.getServer().getWorlds().iterator().next(), Integer.parseInt(el.split(" ")[0]), Integer.parseInt(el.split(" ")[1]), Integer.parseInt(el.split(" ")[2]));
		
		String pl = node.getNode("pvpPos").getString();
		pvpLocation = new Location<World>(Sponge.getServer().getWorlds().iterator().next(), Integer.parseInt(pl.split(" ")[0]), Integer.parseInt(pl.split(" ")[1]), Integer.parseInt(pl.split(" ")[2]));
		
		String cl = node.getNode("chestPos").getString();
		chestLocation = new Location<World>(Sponge.getServer().getWorlds().iterator().next(), Integer.parseInt(cl.split(" ")[0]), Integer.parseInt(cl.split(" ")[1]), Integer.parseInt(cl.split(" ")[2]));
		CommandSpec ready = CommandSpec.builder().description(Text.of("Ready!!!!!!!")).executor(new CommandExecutor() {
			
			@Override
			public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
				if (!isRunning) return CommandResult.builder().successCount(1).build();
				if (!players.contains((Player) src)) {
					players.add((Player) src);
					for (Player player : Sponge.getGame().getServer().getOnlinePlayers()) {
						player.sendMessage(Text.of("§b»§a " + src.getName() + " §7 is now ready!"));
					}
				}
				if (players.size() == Sponge.getGame().getServer().getOnlinePlayers().size()) {
					for (Player player : Sponge.getGame().getServer().getOnlinePlayers()) {
						player.setLocation(pvpLocation);
						player.playSound(SoundTypes.ENTITY_WOLF_HOWL, equipLocation.getPosition(), 1);
						player.offer(Keys.GAME_MODE, GameModes.SURVIVAL);
						player.offer(Keys.INVISIBLE, false);
						player.sendMessage(Text.of("§b»§7 The Game has begun. Kill everyone to win"));
					}
				}
				return CommandResult.builder().successCount(1).build();
			}
		}).build();
		CommandSpec start = CommandSpec.builder().description(Text.of("Teleport everyone to Equip Cell")).permission("mgw.start").executor(new CommandExecutor() {
			
			@Override
			public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
				int items = 0;
				for (Player player : Sponge.getGame().getServer().getOnlinePlayers()) {
					items += 41;
					
					players.clear();
					player.setLocation(equipLocation);
					player.playSound(SoundTypes.ENTITY_CAT_PURR, equipLocation.getPosition(), 1);
					player.offer(Keys.GAME_MODE, GameModes.ADVENTURE);
					player.offer(Keys.INVISIBLE, true);
					player.getInventory().clear();
					player.sendMessage(Text.of("§b»§7 Type §a/items §7to see all the items you can get. When you are ready, type §a/ready§7."));
					isRunning = true;
					
				}
				return CommandResult.builder().successCount(1).affectedEntities(Sponge.getGame().getServer().getOnlinePlayers().size()).affectedItems(items).build();
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
		CommandSpec getInv = CommandSpec.builder().description(Text.of("Open the Inventory")).executor(new CommandExecutor() {
			
			@Override
			public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
				Player p = (Player) src;
				p.playSound(SoundTypes.BLOCK_NOTE_BELL, p.getLocation().getPosition(), 1.0);
				
				if (inves.containsKey(p)) {
					p.openInventory(inves.get(p));
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
				inves.put(p, dest);
				p.openInventory(dest);
				return CommandResult.builder().successCount(1).affectedItems(0).build();
			}
		}).build();
		Sponge.getCommandManager().register(this, start, "equip");
		Sponge.getCommandManager().register(this, getInv, "items");
		Sponge.getCommandManager().register(this, ready, "ready");
		Sponge.getCommandManager().register(this, editInv, "setitems");
	}
	
	@Listener
	public void onLogin(ClientConnectionEvent.Login e) {
		if (isRunning && !e.getTargetUser().hasPermission("mgw.bypass")) e.setCancelled(true);
	}
	
	ArrayList<Transaction<BlockSnapshot>> snapshots = new ArrayList<>();
	
	@Listener
	public void onBlock(ChangeBlockEvent.Place e) {
		for (Transaction<BlockSnapshot> s : e.getTransactions()) {
			snapshots.add(s);
		}
	}
	
	@Listener
	public void onBlock(ChangeBlockEvent.Break e) {
		for (Transaction<BlockSnapshot> s : e.getTransactions()) {
			snapshots.add(s);
		}
	}
	
	@Listener
	public void onDeath(DestructEntityEvent.Death e) {
		if (e.getTargetEntity().getType() == EntityTypes.PLAYER) {
			if (players.contains((Player) e.getTargetEntity())) {
				players.remove((Player) e.getTargetEntity());
				e.getTargetEntity().offer(Keys.GAME_MODE, GameModes.SPECTATOR);
				((Player) e.getTargetEntity()).getInventory().clear();
			}
			if (players.size() == 1) {
				Player winner = players.get(0);
				for (Player player : Sponge.getGame().getServer().getOnlinePlayers()) {
					player.sendTitle(Title.of(Text.of(winner.getName() + " won!")));
				}
				players.clear();
				inves.clear();
				Collections.reverse(snapshots);
				for (Transaction<BlockSnapshot> ts : new ArrayList<>(snapshots)) {	
					ts.getDefault().getLocation().get().copy().setBlock(ts.getOriginal().copy().getState().copy());
				}
				snapshots.clear();
			}
		}
	}
	
}
