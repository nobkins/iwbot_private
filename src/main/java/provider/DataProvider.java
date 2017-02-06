package provider;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

public class DataProvider {
	private static Info info;
	class Info {
		class Discord {
			String token;
			List<String> idOwner;
			List<String> idRoles;
			String newMember;
			String adminChanID;
		}

		class ConData {
			String IP;
			String DB;
			String US;
			String PW;
		}

		Discord discord;
		Map<String, ConData> connections;
		String inaraPW;
		String googleToken;
		String githubToken;
		boolean dev;
	}
	
	private static void getInfo() {
		try {
			Gson gson = new Gson();
			JsonReader jReader = new JsonReader(new FileReader("./data.json"));
			info = gson.fromJson(jReader, Info.class);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private static void setInfo() {
		try {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonWriter jWriter = new JsonWriter(new FileWriter("./data.json"));
			jWriter.setHtmlSafe(false);
			jWriter.setIndent("  ");
			gson.toJson(info, Info.class, jWriter);
			jWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the token necessary to login to Discord
	 * 
	 * @return the token to login to Discord
	 */
	public static String getToken() {
		if (info == null)
			getInfo();
		return info.discord.token;
	}
	
	/**
	 * Returns a list of all owners that are allowed
	 * to perform critical changes
	 * 
	 * @return list of ID strings
	 */
	private static List<String> getOwnerIDs() {
		if (info == null)
			getInfo();
		return info.discord.idOwner;
	}
	
	/**
	 * 
	 * @param id of the owner to add
	 */
	public static void addOwner(String id) {
		if (info == null)
			getInfo();
		info.discord.idOwner.add(id);
		setInfo();
	}
	
	/**
	 * 
	 * @param id of the owner to remove
	 */
	public static boolean removeOwner(String id) {
		if (info == null)
			getInfo();
		boolean success = !info.discord.idOwner.remove(info.discord.idOwner.indexOf(id)).isEmpty();
		setInfo();
		return success;
	}
	
	/**
	 * Get the saved message for new members.
	 * 
	 * @return message as string
	 */
	public static String getNewMemberInfo() {
		if (info == null)
			getInfo();
		return info.discord.newMember;
	}
	
	/**
	 * Save a new message for the new members.
	 * 
	 * @param message as string
	 */
	public static void setNewMemberInfo(String message) {
		if (info == null)
			getInfo();
		info.discord.newMember = message;
		setInfo();
	}

	/**
	 * 
	 * @return the admin channel id as string
	 */
	public static String getAdminChanID() {
		if (info == null)
			getInfo();
		return info.discord.adminChanID;
	}
	
	/**
	 * 
	 * @param id of the channel used for admin
	 */
	public static void setAdminChanID(String id) {
		if (info == null)
			getInfo();
		info.discord.adminChanID = id;
		setInfo();
	}
	
	/**
	 * 
	 * @return the ids of all admin roles
	 */
	public static List<String> getAdminRoleIDs() {
		if (info == null)
			getInfo();
		return info.discord.idRoles;
	}
	
	/**
	 * 
	 * @param id of the admin role
	 */
	public static void addAdminRoleID(String id) {
		if (info == null)
			getInfo();
		info.discord.idRoles.add(id);
		setInfo();
	}
	
	public static void removeAdminRoleID(String id) {
		if (info == null)
			getInfo();
		info.discord.idRoles.remove(id);
		setInfo();
	}

	public static boolean isOwner(GuildMessageReceivedEvent event) {
		return getOwnerIDs().contains(event.getAuthor().getId());
	}

	public static boolean isOwner(PrivateMessageReceivedEvent event) {
		return getOwnerIDs().contains(event.getAuthor().getId());
	}

	public static boolean isOwner(String id) {
		return getOwnerIDs().contains(id);
	}

	public static boolean isAdmin(GuildMessageReceivedEvent event) {
		boolean isAdmin = false;
		for (Role role : event.getMember().getRoles()) {
			if (getAdminRoleIDs().contains(role.getId()))
				isAdmin = true;
		}
		return isAdmin;
	}

	public static boolean isAdmin(List<Role> roles) {
		boolean isAdmin = false;
		for (Role role : roles) {
			if (getAdminRoleIDs().contains(role.getId()))
				isAdmin = true;
		}
		return isAdmin;
	}

	public static String getInaraPW() {
		if (info == null)
			getInfo();
		return info.inaraPW;
	}

	public static String getGoogleToken() {
		if (info == null)
			getInfo();
		return info.googleToken;
	}

	/**
	 *
	 * @return if the bot runs in development
	 */
	public static boolean isDev () {
		if (info == null)
			getInfo();
		return info.dev;
	}

	public static Info.ConData getConData(String conName) {
		if (info == null)
			getInfo();
		return info.connections.get(conName);
	}

	public static String getGithubToken() {
	    if (info == null)
	        getInfo();
	    return info.githubToken;
    }
}
