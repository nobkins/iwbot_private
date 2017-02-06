package iw_bot;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.MessageEmbed;
import provider.DataProvider;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Scanner;

class AutoUpdate {
    class Author {
        String username;
    }
    class Commit {
        Author author;
        String message;
    }
    class Push {

        String ref;
        Commit commits[];
    }

    AutoUpdate() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(1701), 0);
            server.createContext("/update", new GitHookHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class GitHookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            JDA jda = Listener.jda;

            Gson gson = new Gson();
            JsonReader jReader = new JsonReader(new InputStreamReader(t.getRequestBody()));

            Push push = gson.fromJson(jReader, Push.class);

            String commits = "";
            for (Commit commit : push.commits) {
                commits += "Author: " + commit.author.username + "\n";
                commits += "Message: " + commit.message + "\n\n";
            }
            System.out.println(commits);

            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle("New push to repository")
                    .setColor(jda.getGuildById("142749481530556416").getMember(jda.getSelfUser()).getRoles().get(0).getColor())
                    .addField("Reference", push.ref, true)
                    .addField("Commits", commits, false);
            MessageEmbed embed = eb.build();

            jda.getGuildById("142749481530556416").getTextChannelById("217344072111620096").sendMessage(embed).queue();

            t.sendResponseHeaders(200, 0);
            OutputStream os = t.getResponseBody();
            os.write("".getBytes());
            os.close();


            URL jarurl = new URL("https://github.com/Bermos/iwbot_private/blob/feature_autoupdate/out/production/discordbot.jar?client_id=5e80efc3ed12cf8c1515&client_secret=" + DataProvider.getGithubToken());
            ReadableByteChannel rbc = Channels.newChannel(jarurl.openStream());
            FileOutputStream fos = new FileOutputStream("discordbot_new.jar");
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

        }
    }
}
