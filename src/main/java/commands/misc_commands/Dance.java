package commands.misc_commands;

import commands.GuildCommand;
import iw_bot.LogUtil;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Dance extends Thread implements GuildCommand {
	@Override
	public void runCommand(GuildMessageReceivedEvent event, String[] args) {
		Dance dance = new Dance(event);
		dance.setDance(Dance.ASCII.DANCE);
		dance.start();
	}

	@Override
	public String getHelp(GuildMessageReceivedEvent event) {
		return "Makes the bot dance";
	}

	private enum ASCII {
		DANCE
	}
	
	private GuildMessageReceivedEvent event;
	private Message message;
	private ASCII iAnimation;

	public Dance() {
		this.event = null;
	}

	private Dance(GuildMessageReceivedEvent event) {
		this.event = event;
	}

	public void run() {
		Thread.currentThread().setName("BOT - MISC - Dance");
		if (iAnimation == ASCII.DANCE) {
			for (int i = 0; i < 15; i++) {
				if (i == 0) {
					message  = event.getChannel().sendMessage("/o/").complete();
				}
				else if (i%2 == 0) {
					message.editMessage("/o/").complete();
				}
				else {
					message.editMessage("\\o\\").complete();
				}
				try {
					Thread.sleep(800);
				} catch (InterruptedException e) {
					LogUtil.logErr(e);
				}
			}
			message.editMessage("\\o/").queue();
		}
	}

	private void setDance(ASCII iAnimation) {
		this.iAnimation = iAnimation;
	}
}