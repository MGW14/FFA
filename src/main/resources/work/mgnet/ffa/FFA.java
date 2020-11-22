package work.mgnet.ffa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSources;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
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
	private static ConfigurationLoader<CommentedConfigurationNode> configManager;

	@Inject
	@ConfigDir(sharedRoot = false)
	private Path privateConfigDir;

	public static int spreadPlayerDistance;
	
	public static Path potentialFile;
	public static boolean isRunning = false;
	
	public static Location<World> equipLocation;
	public static Location<World> pvpLocation;
	public static Location<World> chestLocation;
	public static float tickrate;
	public static int spreadPlayerRadius;
	public static HashMap<String, Inventory> inves = new HashMap<>();
	
	public static boolean resetMap=true;
	
	public static ArrayList<String> players = new ArrayList<>();
	
	public static StatisticsConfig config;
	
	public static ConfigurationNode node;
	
	public static String mapname;
	
	public void saveKit(String name, Inventory inventory) throws Exception {
		File kitFile = Paths.get(privateConfigDir.toString(), name + ".kit").toFile();
		if (!kitFile.exists()) kitFile.createNewFile();
		ArrayList<DataView> items = InventorySerializer.serializeInventory(inventory);
		ArrayList<String> itemsS = new ArrayList<>();
		
		for (DataView dataView : items) {
			
		}
		
		new PrintWriter(kitFile).close();
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(kitFile));
		oos.writeObject(items);
		oos.close();
	}
	
	@SuppressWarnings("unchecked")
	public Inventory loadKit(String name) throws Exception {
		File kitFile = Paths.get(privateConfigDir.toString(), name + ".kit").toFile();
		if (!kitFile.exists()) throw new Exception();
		List<DataView> items;
		ObjectInputStream oos = new ObjectInputStream(new FileInputStream(kitFile));
		items = (List<DataView>) oos.readObject();
		oos.close();
		Inventory inv = Inventory.builder().of(InventoryArchetypes.DOUBLE_CHEST).build(this);
		InventorySerializer.deserializeInventory(items, inv);
		return inv;
	}
	
	public ConfigurationNode loadConfig() throws IOException {
		potentialFile = Paths.get(privateConfigDir.toString(), "config.yml");
		if (!potentialFile.toFile().exists()) {
			privateConfigDir.toFile().mkdir();
			potentialFile.toFile().createNewFile();
			configManager = HoconConfigurationLoader.builder().setPath(potentialFile).build();
			node = configManager.createEmptyNode();
		} else {
			configManager = HoconConfigurationLoader.builder().setPath(potentialFile).build();
			node = configManager.load();
		}
		config = new StatisticsConfig();
		config.loadStats(privateConfigDir.toFile());
		if (node.getNode("equipPos").getString() == null) node.getNode("equipPos").setValue("100 100 100");
		if (node.getNode("pvpPos").getString() == null) node.getNode("pvpPos").setValue("50 100 50");
		if (node.getNode("chestPos").getString() == null) node.getNode("chestPos").setValue("100 100 50");
		if (node.getNode("tickrate").getString() == null) node.getNode("tickrate").setValue(20);
		if(node.getNode("spreadPlayerRadius").getString() == null) node.getNode("spreadPlayerRadius").setValue(130);
		if(node.getNode("spreadPlayerDistance").getString() == null) node.getNode("spreadPlayerDistance").setValue(25);
		if(node.getNode("mapname").getString() == null) node.getNode("mapname").setValue("map");
		configManager.save(node);
		
		String[] el = node.getNode("equipPos").getString().split(" ");
		equipLocation = new Location<World>(Sponge.getServer().getWorlds().iterator().next(), Integer.parseInt(el[0]), Integer.parseInt(el[1]), Integer.parseInt(el[2]));
		
		String[] pl = node.getNode("pvpPos").getString().split(" ");
		pvpLocation = new Location<World>(Sponge.getServer().getWorlds().iterator().next(), Integer.parseInt(pl[0]), Integer.parseInt(pl[1]), Integer.parseInt(pl[2]));
		
		String[] cl = node.getNode("chestPos").getString().split(" ");
		chestLocation = new Location<World>(Sponge.getServer().getWorlds().iterator().next(), Integer.parseInt(cl[0]), Integer.parseInt(cl[1]), Integer.parseInt(cl[2]));
		
		tickrate=Float.parseFloat(node.getNode("tickrate").getString());
		
		spreadPlayerRadius=Integer.parseInt(node.getNode("spreadPlayerRadius").getString());
		
		spreadPlayerDistance=Integer.parseInt(node.getNode("spreadPlayerDistance").getString());
		
		mapname=node.getNode("mapname").getString();
		return node;
	}
	public static void saveConfig() {
		try {
			configManager.save(node);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void startGame(ConfigurationNode node) throws CommandException {
		isRunning = true;
		Sponge.getCommandManager().get("spreadplayers").get().getCallable().process(Sponge.getServer().getConsole(), pvpLocation.getBlockX() + " " + pvpLocation.getBlockZ() + " "+spreadPlayerDistance+" " + spreadPlayerRadius + " false @a");
		Sponge.getCommandManager().get("tickrate").get().getCallable().process(Sponge.getServer().getConsole(), Float.toString(tickrate));
		Sponge.getCommandManager().get("difficulty").get().getCallable().process(Sponge.getServer().getConsole(), "1");
		Sponge.getCommandManager().get("effect").get().getCallable().process(Sponge.getServer().getConsole(), "@a clear");
		for (Player player : Sponge.getGame().getServer().getOnlinePlayers()) {
			player.offer(Keys.GAME_MODE, GameModes.SURVIVAL);
			player.offer(Keys.HEALTH, 20D);
			player.offer(Keys.FOOD_LEVEL, 20);
			player.offer(Keys.EXPERIENCE_LEVEL, 0);
			player.sendMessage(Text.of("§b»§7 The Game has begun. Kill everyone to win"));
		}
	}
	
	public ArrayList<String> edit = new ArrayList<>();
	
	@Listener
	public void onServer(GameStartedServerEvent e) throws IOException, ObjectMappingException {
		node = loadConfig();
		
		//Command ready
		CommandSpec ready = CommandSpec.builder().description(Text.of("Ready!!!!!!!")).executor(new CommandExecutor() {
			
			@Override
			public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
				if (Sponge.getGame().getServer().getOnlinePlayers().size()==1) {
					for (Player player : Sponge.getGame().getServer().getOnlinePlayers()) player.sendMessage(Text.of("§b»§c At least 2 players are required"));
					return CommandResult.builder().successCount(1).build();
				}
				if (players.contains(src.getName())||isRunning==true) return CommandResult.builder().successCount(1).build();
				players.add(src.getName());
				
				for (Player player : Sponge.getGame().getServer().getOnlinePlayers()) player.sendMessage(Text.of("§b»§a " + src.getName() + "§7 is now ready!"));
				
				if (players.size() == Sponge.getGame().getServer().getOnlinePlayers().size()) {	
					startGame(node);
				}
				return CommandResult.builder().successCount(1).build();
			}
		}).build();
		
		//Command mapreload
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
		
		//Command forceend
		CommandSpec forceend = CommandSpec.builder().description(Text.of("Forceend the game")).permission("mgw.start").executor(new CommandExecutor() {
			
			@Override
			public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
				endGame();
				return CommandResult.builder().successCount(1).affectedEntities(Sponge.getGame().getServer().getOnlinePlayers().size()).build();
			}
		}).build();
		
		//Command setitems
		CommandSpec editInv = CommandSpec.builder().description(Text.of("Change the Inventory")).permission("mgw.edit").executor(new CommandExecutor() {
			
			@Override
			public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
				Player p = (Player) src;
				p.playSound(SoundTypes.BLOCK_NOTE_BELL, p.getLocation().getPosition(), 1.0);
				
				/*Inventory source = ((Chest) chestLocation.getTileEntity().get()).getDoubleChestInventory().get();
				p.openInventory(source);*/
				
				try {
					p.openInventory(loadKit("test"));
				} catch (Exception e) {
					e.printStackTrace();
					p.openInventory(Inventory.builder().of(InventoryArchetypes.DOUBLE_CHEST).build(Sponge.getPluginManager().getPlugin("ffa").get()));
				}
				
				edit.add(p.getName());
				
				return CommandResult.builder().successCount(1).affectedItems(0).build();
			}
		}).build();
		
		//Command forcestart
		CommandSpec forcestart = CommandSpec.builder().description(Text.of("Forcestart")).permission("mgw.start").executor(new CommandExecutor() {
			
			@Override
			public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
				players.clear();
				for (Player player : Sponge.getGame().getServer().getOnlinePlayers()) players.add(player.getName());
				startGame(node);
				return CommandResult.builder().successCount(1).build();
			}
		}).build();
		//Command stopmapreload
			CommandSpec stopmapreload = CommandSpec.builder().description(Text.of("Stops reloading the map after the round is over")).permission("mgw.edit").executor(new CommandExecutor() {
					
				@Override
				public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
					resetMap=!resetMap;
					return CommandResult.builder().successCount(1).affectedEntities(Sponge.getGame().getServer().getOnlinePlayers().size()).build();
				}
			}).build();
		
		//Command items
		CommandSpec getInv = CommandSpec.builder().description(Text.of("Open the Inventory")).executor(new CommandExecutor() {
			
			@Override
			public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
				Player p = (Player) src;
				p.playSound(SoundTypes.BLOCK_NOTE_BELL, p.getLocation().getPosition(), 1.0);
				
				if (inves.containsKey(p.getName())) {
					p.openInventory(inves.get(p.getName()));
					return CommandResult.builder().successCount(1).affectedItems(0).build();
				}
				
				try {
					p.openInventory(loadKit("test"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				/*Inventory source = ((Chest) chestLocation.getTileEntity().get()).getDoubleChestInventory().get();
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
				p.openInventory(dest);*/
				return CommandResult.builder().successCount(1).affectedItems(0).build();
			}
		}).build();
		
		
		Sponge.getCommandManager().register(this, getInv, "items"); // ScribbleLP
		Sponge.getCommandManager().register(this, forceend, "forceend");// Done
		Sponge.getCommandManager().register(this, forcestart, "forcestart"); // Done 
		Sponge.getCommandManager().register(this, mapreload, "reloadmap"); // Done
		
		Sponge.getCommandManager().register(this, ready, "ready"); // Done
		
		Sponge.getCommandManager().register(this, config, "statistics"); // Done
		
		Sponge.getCommandManager().register(this, editInv, "setitems"); // ScribbleLP
		
		Sponge.getCommandManager().register(this, stopmapreload, "reloadmapstop"); // Dunno
		Sponge.getCommandManager().register(this, new CommandFFAConfig(), "ffa");  // ScribbleLP
	}
	
	@Listener
	public void onLogin(ClientConnectionEvent.Join e) {
		e.setMessageCancelled(true);
		Cause cause=e.getCause();
		Player player= cause.first(Player.class).get();
		if (isRunning) {
			player.offer(Keys.GAME_MODE, GameModes.SPECTATOR);
			try {
				Sponge.getCommandManager().get("tickrate").get().getCallable().process(Sponge.getServer().getConsole(), Float.toString(tickrate)+" "+player.getName());
			} catch (CommandException e1) {
				e1.printStackTrace();
			}
			player.sendMessage(Text.of("§b»§7 A game is already running, after the round you will participate"));
		} else {
			player.offer(Keys.GAME_MODE, GameModes.ADVENTURE);
			try {
				Sponge.getCommandManager().get("tickrate").get().getCallable().process(Sponge.getServer().getConsole(), "20 "+player.getName());
			} catch (CommandException e1) {
				e1.printStackTrace();
			}
			player.sendMessage(Text.of("§b»§7 Type §a/items §7to see all the items you can get. When you are ready, type §a/ready§7."));
		}
		player.getInventory().clear();
		player.setLocation(equipLocation);
		try {
			Sponge.getCommandManager().get("spawnpoint").get().getCallable().process(Sponge.getServer().getConsole(), player.getName());
		} catch (CommandException e1) {
			e1.printStackTrace();
		}
	}

	public void endGame() {
		if (!isRunning) return;
		players.clear();
		inves.clear();
		for (Player player : Sponge.getGame().getServer().getOnlinePlayers()) {
			player.setLocation(equipLocation);
			player.getInventory().clear();
			player.offer(Keys.HEALTH, 20D);
			player.offer(Keys.FOOD_LEVEL, 20);
			player.offer(Keys.EXPERIENCE_LEVEL, 0);
			player.sendMessage(Text.of("§b»§e The Game has ended"));
			player.sendMessage(Text.of("§b»§7 Type §a/items §7to see all the items you can get. When you are ready, type §a/ready§7."));
			player.offer(Keys.GAME_MODE, GameModes.ADVENTURE);
		}
		try {
			Sponge.getCommandManager().get("difficulty").get().getCallable().process(Sponge.getServer().getConsole(), "0");
		} catch (CommandException e2) {
			e2.printStackTrace();
		}
		try {
			Sponge.getCommandManager().get("effect").get().getCallable().process(Sponge.getServer().getConsole(), "@a clear");
		} catch (CommandException e2) {
			e2.printStackTrace();
		}
		try {
			Sponge.getCommandManager().get("tickrate").get().getCallable().process(Sponge.getServer().getConsole(), "20");
		} catch (CommandException e) {
			e.printStackTrace();
		}
		try {
			Sponge.getCommandManager().get("kill").get().getCallable().process(Sponge.getServer().getConsole(), "@e[type=!player]");
		} catch (CommandException e) {
			e.printStackTrace();
		}
		if(resetMap==true) {
			try {
				loadMap();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		isRunning = false;
	}
	
	@Listener
	public void onDrop(DropItemEvent e) {
		try {Cause cause=e.getCause();
		Optional<Player> player= cause.first(Player.class);
		if (!isRunning&&!player.get().hasPermission("mgw.bypasslobby")) e.setCancelled(true);} catch (Exception e3) {
			
		}
	}
	
	@Listener
	public void onPvP(DamageEntityEvent e) {
		Cause cause=e.getCause();
		
		try {if (isRunning && e.willCauseDeath() && e.getTargetEntity().getType() == EntityTypes.PLAYER) {
			for (Player p : Sponge.getServer().getOnlinePlayers()) {
				if (e.getCause().getContext().toString().contains(p.getName())) {
					config.updateStats(p, 1, 0, 0, 0);
				}
			}
			try {
				config.updateStats((Player) e.getTargetEntity(), 0, 1, 1, 0);
			} catch (Exception e2) {
				
			}
		}
		
		Optional<DamageSource> source=cause.first(DamageSource.class);
		if (!isRunning&&source.get()!=DamageSources.VOID) e.setCancelled(true);} catch (Exception e3) {
			
		}
	}
	
	@Listener
	public void onLeave(ClientConnectionEvent.Disconnect e) throws CommandException {
		players.remove(e.getTargetEntity().getName());
		if (players.size() <= 1&&isRunning==true) endGame();
	}
	
	@Listener
	public void onDeath(DestructEntityEvent.Death e) {
		if (e.getTargetEntity().getType() == EntityTypes.PLAYER) {
			if (players.contains(((Player) e.getTargetEntity()).getName())) {
				players.remove(((Player) e.getTargetEntity()).getName());
				e.getTargetEntity().offer(Keys.GAME_MODE, GameModes.SPECTATOR);
			}
			if (players.size() == 1) {
				Player winner = Sponge.getServer().getPlayer(players.get(0)).get();
				config.updateStats(winner, 0, 0, 1, 1);
				for (Player player : Sponge.getGame().getServer().getOnlinePlayers()) {
					player.sendTitle(Title.of(Text.of(winner.getName() + " won!")));
				}
				endGame();
			}
		}
	}
	
	public void loadMap() throws Exception {
		EditSession sess = WorldEdit.getInstance().getEditSessionFactory().getEditSession(SpongeWorldEdit.inst().getWorld(Sponge.getServer().getWorlds().iterator().next()), -1);
		
		File schem = new File(privateConfigDir.toString(), mapname+".schem");
		if(schem.exists()) {
			CuboidClipboard cl = MCEditSchematicFormat.getFormat(schem).load(schem);
			cl.paste(sess, new Vector(0, 0, 0), false, true);
		}else {
			System.out.println("No schematic file found!");
		}
	}
	
	@Listener
	public void onInv(InteractInventoryEvent.Close e) throws Exception {
		if (edit.contains(((Player) e.getSource()).getName())) {
			saveKit("test", e.getTargetInventory());
			System.out.println("wtf");
			edit.remove(((Player) e.getSource()).getName());
		}
	}
	
}
