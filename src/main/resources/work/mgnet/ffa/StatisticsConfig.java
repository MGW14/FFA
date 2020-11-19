package work.mgnet.ffa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class StatisticsConfig implements CommandCallable {

	public static File statsFile;
	
	public static class Stats {
		
		public Stats(UUID uuid) {
			this.uuid = uuid;
		}
		
		public UUID uuid;
		public int kills = 0;
		public int deaths = 0;
		public int games = 0;
		public int gamesWon = 0;
		
		@Override
		public String toString() {
			return uuid.toString() + ":" + kills + ":" + deaths + ":" + games + ":" + gamesWon;
		}
		
		public static Stats fromString(String obj) {
			String[] segmentedObj = obj.split(":");
			Stats stats = new Stats(UUID.fromString(segmentedObj[0]));
			stats.kills = Integer.parseInt(segmentedObj[1]);
			stats.deaths = Integer.parseInt(segmentedObj[2]);
			stats.games = Integer.parseInt(segmentedObj[3]);
			stats.gamesWon = Integer.parseInt(segmentedObj[4]);
			return stats;
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof Stats ? ((Stats) obj).uuid.equals(uuid) : false; 
		}
		
	}
	
	public List<Stats> stats = new ArrayList<>();
	
	public void updateStats(Player uuid, int kills, int deaths, int games, int gamesWon) {
		updateStats(uuid.getUniqueId(), kills, deaths, games, gamesWon);
		
	}
	
	public Stats getStats(Player uuid) {
		return getStats(uuid.getUniqueId());
	}
	
	public Stats getStats(UUID uuid) {
		for (Stats statO : stats) {
			if (statO.equals(new Stats(uuid))) return statO;
		}
		return null;
	}
	
	public void updateRank(String rank, UUID uuid) {
		Sponge.getServer().getPlayer(uuid).get().sendMessage(Text.of("§b» §aYou advanced to " + rank));
		Sponge.getServer().getServerScoreboard().get().getTeam(rank).get().addMember(Sponge.getServer().getPlayer(uuid).get().getTeamRepresentation());
		try {
			Sponge.getServer().getServerScoreboard().get().getTeam(rank).get().setPrefix(Text.of("§b" + Sponge.getServer().getServerScoreboard().get().getTeam(rank).get().getDisplayName().toPlain() + " §f"));
		} catch (Exception e) {
			
		}
	}
	
	public void updateStats(UUID uuid, int kills, int deaths, int games, int gamesWon) {
		if (stats.contains(new Stats(uuid))) {
			Stats stat = null;
			for (Stats statO : stats) {
				if (statO.equals(new Stats(uuid))) stat = statO;
			}
			stat.kills += kills;
			stat.deaths += deaths;
			stat.games += games;
			stat.gamesWon += gamesWon;
			stats.remove(stat);
			stats.add(stat);
			
			if (stat.games == 2) {
				updateRank("beginner", uuid);
			} else if (stat.games == 20) {
				if (stat.kills > 4) {
					updateRank("fighter", uuid);
				} else {
					updateRank("noob", uuid);
				}
			} else if (stat.games == 50) {
				if (stat.kills > 35) {
					updateRank("advanced", uuid);
				} else {
					updateRank("intermediate", uuid);
				}
			} else if (stat.games == 100) {
				if (stat.kills > 150) {
					updateRank("taslegend", uuid);
				} else {
					updateRank("pro", uuid);
				}
			}
			
		} else {
			stats.add(new Stats(uuid));
			updateStats(uuid, kills, deaths, games, gamesWon);
		}
		
		try {
			saveStats();
		} catch (FileNotFoundException e) {
			System.out.println("Couldn't save stats");
		}
		
	}
	
	public void saveStats() throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(new FileOutputStream(statsFile));
		writer.print("");
		writer.close();
		writer = new PrintWriter(new FileOutputStream(statsFile));
		String string = "";
		for (Stats stat : stats) {
			string = string + ";" + stat.toString();
		}
		string = string.replaceFirst(";", "");
		writer.write(string + "\r\n");
		writer.flush();
		writer.close();
		
	}
	
	public void loadStats(File configDir) throws IOException {
		statsFile = new File(configDir, "stats.yml");
		if (statsFile.exists()) {
			BufferedReader reader = new BufferedReader(new FileReader(statsFile));
			String[] serializedStats;
			try {
				serializedStats = reader.readLine().split(";");
			}catch(NullPointerException e) {
				System.err.println("Stats file is empty");
				reader.close();
				return;
			}
			for (String serializedStat : serializedStats) {
				stats.add(Stats.fromString(serializedStat));
			}
			reader.close();
		} else {
			statsFile.createNewFile();
		}
	}

	@Override
	public CommandResult process(CommandSource source, String arguments) throws CommandException {
		Player player = (Player) source;
		if (arguments.length() != 0) {
			try {
				if (Sponge.getServer().getPlayer(arguments).get() == null) throw new Exception();
				player = Sponge.getServer().getPlayer(arguments).get();
			} catch (Exception e) {
				source.sendMessage(Text.of("§b» §7That Player is not online!"));
			}
		}
		try {
			Stats stats = getStats(player);
			source.sendMessage(Text.of("§b» §7Showing Stats of " + player.getName()));
			source.sendMessage(Text.of("§b» §7Kills: §b" + stats.kills));
			source.sendMessage(Text.of("§b» §7Deaths: §b" + stats.deaths));
			source.sendMessage(Text.of("§b» §7K/D: §b" + Math.round((double) stats.kills / ((stats.deaths == 0) ? 1 : stats.deaths))));
			source.sendMessage(Text.of("§b» §7Games played: §b" + stats.games));
			source.sendMessage(Text.of("§b» §7Games won: §b" + stats.gamesWon));
			source.sendMessage(Text.of("§b» §7Win chance: §b" + ((((double) stats.gamesWon) / ((double) stats.games)) * 100) + "%"));
		} catch (ArithmeticException e1) {
			source.sendMessage(Text.of("§b» §7Win chance: Not enough data!"));
		} catch (Exception e) {
			source.sendMessage(Text.of("§b» §7Couldn't show stats!"));
		}
		return CommandResult.builder().successCount(1).build();
		
	}

	@Override
	public List<String> getSuggestions(CommandSource source, String arguments, Location<World> targetPosition)
			throws CommandException {
		List<String> liste= new ArrayList<String>();
		String[] args=arguments.split(" ");
		if(args.length==1) {
			Sponge.getGame().getServer().getOnlinePlayers().forEach(player->{
				liste.add(player.getName());
			});
		}
		return CommandUtil.getListOfStringsMatchingLastWord(args, liste);
	}

	@Override
	public boolean testPermission(CommandSource source) {
		return true;
	}

	@Override
	public Optional<Text> getShortDescription(CommandSource source) {
		return Optional.of(Text.of("Get Stats"));
	}

	@Override
	public Optional<Text> getHelp(CommandSource source) {
		return Optional.of(Text.of("No help is available atm, please ask your questions after the beep.... *BEEP*"));
	}

	@Override
	public Text getUsage(CommandSource source) {
		return Text.of("/statistics <Player>");
	}
	
}
