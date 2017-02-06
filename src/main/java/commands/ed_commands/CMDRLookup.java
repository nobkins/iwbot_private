package commands.ed_commands;

import commands.GuildCommand;
import commands.PMCommand;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import provider.DataProvider;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

class CombatLogger {
    public String name;
    String no_of_logs;
    String type_of_log;
    String platform;
}

public class CMDRLookup implements PMCommand, GuildCommand {

    private static long last_lookup;
    private static List<CombatLogger> combat_loggers = new ArrayList<>();

    @Override
    public void runCommand(PrivateMessageReceivedEvent event, String[] args) {
        event.getChannel().sendMessage(lookup(args)).queue();
    }

    @Override
    public void runCommand(GuildMessageReceivedEvent event, String[] args) {
        event.getChannel().sendMessage(lookup(args)).queue();
    }

    @Override
    public String getHelp(GuildMessageReceivedEvent event) {
        return "Finds all the information available on inara.cz and r/EliteCombatLoggers";
    }

    private String lookup(String[] args) {
        if (args.length == 1) {
            return whois(args[0], false);
        }
        else if (args.length == 2) {
            return whois(args[0], args[1].equalsIgnoreCase("update"));
        }

        //Should not happen but just in case
        return "[Error] Something went really, really wrong. Try again later or something";
    }

    private static String whois(String username, boolean force_update) {
        String info;
        String inara   = inara(username);
        String loggers = loggers(username, force_update);
        //String reddit  = reddit(username);

        info  = inara + "\n\n";
        info += loggers + "\n\n";
        //info += reddit + "\n";

        return info;
    }

    private static String inara(String username) {
        String info = "__INARA__\nNothing found";

        String url = "http://inara.cz/search?location=search&searchglobal=" + username.replaceAll(" ", "+");
        try {
            Connection.Response loginResponse = Jsoup.connect("http://inara.cz/login")
                    .header("User-Agent", "Mozilla")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Host", "inara.cz")
                    .header("Origin", "http://inara.cz")
                    .header("Upgrade-Insecure-Requests", "1")
                    .data("loginid", "Bermos")
                    .data("loginpass", DataProvider.getInaraPW())
                    .data("formact", "ENT_LOGIN")
                    .data("location", "intro")
                    .followRedirects(false)
                    .method(Connection.Method.POST)
                    .execute();

            //Use the inara search function to get candidates
            Document doc = Jsoup.connect(url).userAgent("Mozilla").cookie("elitesheet", "21111").cookie("esid", loginResponse.cookie("esid")).ignoreContentType(true).get();

            //Find closest match from search results
            double closestScore = 0.0;
            Element closest = null;

            for (Element link : doc.getElementsByTag("a")) {
                if (link.attr("href").contains("cmdr/") && !link.text().equals("CMDR's log")) {
                    double score = StringUtils.getJaroWinklerDistance(username, link.text());
                    if (score > closestScore && score > 0.8) {
                        closest = link;
                    }
                }
            }

            //If there is a closest (maybe noone was found) format the findings for output
            if (closest != null) {
                info = "__INARA__\n";
                url = "http://inara.cz" + closest.attr("href");
                doc = Jsoup.connect(url).userAgent("Mozilla").cookie("elitesheet", "21111").cookie("esid", loginResponse.cookie("esid")).ignoreContentType(true).get();

                info += "**" + doc.select("span.pflheadersmall").get(0).parent().text() + "**\n";
                for (Element image : doc.select("img")) {
                    if (image.parent().hasClass("profileimage"))
                        info += image.absUrl("src") + "\n";
                }

                Elements cells = doc.select("span.pflcellname");
                for (Element cell : cells) {
                    if (!(cell.parent().text().replaceFirst(cell.text(), "").trim().length() < 3))
                        info += ( cell.text() + ": " + cell.parent().text().replaceFirst(cell.text(), "").trim() + "\n");
                }
                info += "This information was obtained using Inara.";
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        return info;
    }

    private static String loggers(String username, boolean force_update)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm zz");
        String info = "__r/EliteCombatLoggers__\nNothing found. Last updated: " + sdf.format(last_lookup);
        String url = "https://www.googleapis.com/drive/v3/files/16A8s5WFXI2sjOEIlZhcz_KAlO3jI7RWXZlbsOYxzF7E/export?mimeType=text%2Fcsv&key=" + DataProvider.getGoogleToken();

        //If the last lookup was done over 3h ago or the user forces it we update
        //This is to increase performance and don't hit google api rate limits
        if ((last_lookup + (3*60*60*1000) < System.currentTimeMillis()) || force_update)
        {
            try
            {
                //Get doc from google
                String doc = Jsoup.connect(url).get().body().text();

                //Refresh new last lookup time
                last_lookup = System.currentTimeMillis();
                info = "__r/EliteCombatLoggers__\nNothing found. Last updated: " + sdf.format(last_lookup);

                //Update local data to new readings from google doc
                String[] rows = doc.split(", ");
                combat_loggers.clear();

                for (String row : rows) {
                    String[] values = row.split(",");

                    //There are some broken/empty entries, we don't want 'em
                    if (values.length == 4) {
                        //Here we found our candidate, output him
                        if (values[0].equalsIgnoreCase(username)) {
                            info = "__r/EliteCombatLoggers__" + "\n"
                                    + "Exact CMDR name: " + values[0] + "\n"
                                    + "No of logs: " + values[1] + "\n"
                                    + "Method: " + values[2] + "\n"
                                    + "Platform: " + values[3] + "\n"
                                    + "This information was updated on: " + sdf.format(last_lookup);
                        }

                        //Save all the loggers internally for later reuse
                        CombatLogger new_logger = new CombatLogger();
                        new_logger.name = values[0];
                        new_logger.no_of_logs = values[1];
                        new_logger.type_of_log = values[2];
                        new_logger.platform = values[3];
                        combat_loggers.add(new_logger);
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

        }
        else
        {
            for (CombatLogger logger : combat_loggers)
            {
                if (logger.name.equalsIgnoreCase(username)) {
                    info = "__r/EliteCombatLoggers__" + "\n"
                            + "Exact CMDR name: " + logger.name + "\n"
                            + "No of logs: " + logger.no_of_logs + "\n"
                            + "Method: " + logger.type_of_log + "\n"
                            + "Platform: " + logger.platform + "\n"
                            + "This information was updated on: " + sdf.format(last_lookup);
                    return info;
                }
            }
        }

        return info;
    }

    /*private static String reddit(String username) {
        //TODO
        return null;
    } */
}