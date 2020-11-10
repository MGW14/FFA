package work.mgnet.ffa;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;


public class CommandFFAConfig implements CommandCallable{

	@Override
	public CommandResult process(CommandSource source, String arguments) throws CommandException {
		String[] args=arguments.split(" ");
		if(args[0].equalsIgnoreCase("pvpPos")) {
			FFA.pvpLocation=setCoordinates("pvpPos", args);
		}else if(args[0].equalsIgnoreCase("chestPos")) {
			FFA.chestLocation=setCoordinates("chestPos", args);
		}else if(args[0].equalsIgnoreCase("equipPos")) {
			FFA.equipLocation=setCoordinates("equipPos", args);
		}else if(args[0].equalsIgnoreCase("tickrate")) {
			FFA.tickrate=setFloat("tickrate", args);
		}else if(args[0].equalsIgnoreCase("spreadPlayerRadius")){
			FFA.spreadPlayerRadius=setInt("spreadPlayerRadius", args);
		}else if(args[0].equalsIgnoreCase("spreadPlayerDistance")) {
			FFA.spreadPlayerDistance=setInt("spreadPlayerDistance", args);
		}else if(args[0].equalsIgnoreCase("mapname")) {
			FFA.mapname=setString("mapname", args);
		}else {
			source.sendMessage(Text.of("§b» §7/ffa pvpPos | chestPos | equipPos | tickrate | spreadPlayerRadius | spreadPlayerDistance | mapname"));
			return CommandResult.builder().successCount(1).build();
		}
		source.sendMessage(Text.of("§b» §7Successfully changed config option "+args[0]));
		FFA.saveConfig();
		return CommandResult.builder().successCount(1).build();
	}

	public Location<World> setCoordinates(String nodename, String[] args) throws CommandException {
		int X;
		int Y;
		int Z;
		try {
		X=Integer.parseInt(args[1]);
		Y=Integer.parseInt(args[2]);
		Z=Integer.parseInt(args[3]);
		}catch(NumberFormatException ex) {
			throw new CommandException(Text.of("Could not process coordinates"));
		}catch(IndexOutOfBoundsException ev) {
			throw new CommandException(Text.of("Too few arguments"));
		}
		FFA.node.getNode(nodename).setValue(Integer.toString(X)+" "+Integer.toString(Y)+" "+Integer.toString(Z));
		return new Location<World>(Sponge.getServer().getWorlds().iterator().next(), X, Y, Z);
	}
	public float setFloat(String nodename, String[]args) throws CommandException{
		float out;
		try {
			out=Float.parseFloat(args[1]);
		}catch(NumberFormatException ex) {
			throw new CommandException(Text.of("Could not process coordinates"));
		}catch(IndexOutOfBoundsException ev) {
			throw new CommandException(Text.of("Too few arguments"));
		}
		FFA.node.getNode(nodename).setValue(out);
		return out;
	}
	public int setInt(String nodename, String[]args) throws CommandException{
		int out;
		try {
			out=Integer.parseInt(args[1]);
		}catch(NumberFormatException ex) {
			throw new CommandException(Text.of("Could not process coordinates"));
		}catch(IndexOutOfBoundsException ev) {
			throw new CommandException(Text.of("Too few arguments"));
		}
		FFA.node.getNode(nodename).setValue(out);
		return out;
	}
	public String setString(String nodename, String[]args) throws CommandException {
		String out;
		try {
			out=args[1];
		}catch(IndexOutOfBoundsException ex) {
			throw new CommandException(Text.of("Too few arguments"));
		}
		FFA.node.getNode(nodename).setValue(out);
		return out;
	}
	@Override
	public List<String> getSuggestions(CommandSource source, String arguments, Location<World> targetPosition)
			throws CommandException {
		List<String> liste= new ArrayList<String>();
		liste.add("pvpPos");
		liste.add("chestPos");
		liste.add("equipPos");
		liste.add("tickrate");
		liste.add("spreadPlayerRadius");
		liste.add("spreadPlayerDistance");
		liste.add("mapname");
		return liste;
	}

	@Override
	public boolean testPermission(CommandSource source) {
		return source.hasPermission("mgw.edit");
	}

	@Override
	public Optional<Text> getShortDescription(CommandSource source) {
		return Optional.of(Text.of("Changes configuration"));
	}

	@Override
	public Optional<Text> getHelp(CommandSource source) {
		return Optional.of(Text.of("No help is available atm, please ask your questions after the beep.... *BEEP*"));
	}

	@Override
	public Text getUsage(CommandSource source) {
		return Text.of("/ffa <configname> value");
	}
}
