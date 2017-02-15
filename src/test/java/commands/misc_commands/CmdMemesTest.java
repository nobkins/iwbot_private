package commands.misc_commands;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.impl.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.apache.http.HttpHost;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CmdMemesTest {
    @org.junit.Before
    public void setUp() throws Exception {

    }

    @Test
    public void getHelp() {
        Memes memes = new Memes(); // Class to be tested

        JDAImpl jda = new JDAImpl(AccountType.BOT, new HttpHost("0"), false, false, false, false);
        GuildImpl guildimpl = new GuildImpl(jda,"0");
        TextChannelImpl textimpl = new TextChannelImpl("0", guildimpl);

        User user = new UserImpl("135891021048315904", jda);

        Message mess = new MessageImpl("0", textimpl, true).setAuthor(user).setContent("");
        GuildMessageReceivedEvent event = new GuildMessageReceivedEvent(jda, 0, mess);

        String[] args = {"update"};
        assertEquals("Memes updated from file.", memes.memes(args));

        args[0] = "upgrade"; //typo
        assertEquals("[Error] Wrong arguments", memes.memes(args));
    }

}