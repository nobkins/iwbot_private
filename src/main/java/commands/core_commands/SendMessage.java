package commands.core_commands;

import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import provider.DataProvider;

public class SendMessage implements commands.PMCommand {
    @Override
    public void runCommand(PrivateMessageReceivedEvent event, String[] args) {
        if (!DataProvider.isOwner(event)){
            event.getChannel().sendMessage("You ain't allowed to do that").queue();
            return;
        }

        if (args.length == 3) {
            event.getJDA().getGuildById(args[0]).getTextChannelById(args[1]).sendMessage(args[2]).queue();
            event.getChannel().sendMessage("Sent").queue();
        }
    }
}
