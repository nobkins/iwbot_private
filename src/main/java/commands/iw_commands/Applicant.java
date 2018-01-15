package commands.iw_commands;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import commands.GuildCommand;
import iw_bot.LogUtil;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import provider.Connections;
import provider.DataProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static iw_bot.Constants.SQL_SDF;
import static iw_bot.Constants.USER_SDF;

/**
 * This command class is for keeping track of
 * applicants
 *
 * TODO
 */
public class Applicant implements GuildCommand {
    private final Connections con = new Connections();

    @Override
    public void runCommand(GuildMessageReceivedEvent event, String[] args) {
        //Permission check
        if (!(DataProvider.isOwner(event) || DataProvider.isAdmin(event) || DataProvider.isAdvisor(event))) {
            event.getChannel().sendMessage("[Error] You aren't authorized to do this").queue();
            return;
        }

        if (args.length == 0) {
            event.getChannel().sendMessage("[Error] Please use at least one argument for this command").queue();
            return;
        }
        Arrays.sort(args);

        if (Arrays.binarySearch(args, "help") > -1) {
            help(event);
            return;
        } else if (Arrays.binarySearch(args, "list") > -1) {
            applicantlist(event);
            return;
        } else if (event.getMessage().getMentionedUsers().isEmpty()) {
            event.getChannel().sendMessage("[Error] Please mention a user").queue();
            return;
        }

        if (Arrays.binarySearch(args, "status") > -1) {
            status(event);
            return;
        }

        if (Arrays.binarySearch(args, "failed") > -1 || Arrays.binarySearch(args, "fail") > -1) {
            delete(event, false);
            return;
        }

        if (Arrays.binarySearch(args, "passed") > -1 || Arrays.binarySearch(args, "pass") > -1) {
            delete(event, true);
            return;
        }

        if (Arrays.binarySearch(args, "new") > -1) {
            if(args.length < 3) {
                event.getChannel().sendMessage("You need to specify the type of applicant (PC,XBOX,PS4)\neg.\n/applicant new,@CMDR,PS4").queue();
            } else {
                newApplicant(event, args);
            }
        }

        if (Arrays.binarySearch(args, "combat") > -1)
            combat(event);

        if (Arrays.binarySearch(args, "mission") > -1)
            mission(event);

    }

    private void delete(GuildMessageReceivedEvent event,boolean pass) {
        User uApplicant = event.getMessage().getMentionedUsers().get(0);

        try {
            PreparedStatement ps = con.getConnection().prepareStatement("DELETE FROM applicants WHERE id = ?");
            ps.setString(1, uApplicant.getId());
            String out = new String();

            if (ps.executeUpdate() == 1) {
                List<Role> memberRoles = event.getMember().getRoles();
                List newMemberRoles = new ArrayList<Role>(memberRoles);
                Role pc = event.getGuild().getRolesByName("PC_applicant", true).get(0);
                Role pcPilot = event.getGuild().getRolesByName("PC Pilots", true).get(0);
                Role xbox = event.getGuild().getRolesByName("XBOX_applicant", true).get(0);
                Role xboxPilot = event.getGuild().getRolesByName("XBOX Pilots", true).get(0);
                Role ps4 = event.getGuild().getRolesByName("PS4_applicant", true).get(0);
                Role ps4Pilot = event.getGuild().getRolesByName("PS4 Pilots", true).get(0);
                Role appl = event.getGuild().getRolesByName("Applicant", true).get(0);
                Role needsCE = event.getGuild().getRolesByName("Needs CE", true).get(0);
                Role IridiumWing = event.getGuild().getRolesByName("Iridium Wing", true).get(0);
                Role escortPilots = event.getGuild().getRolesByName("Escort Pilots", true).get(0);

                if (pass) {
                    if (!memberRoles.contains(escortPilots)) {
                        newMemberRoles.add(escortPilots);
                        out += "Upgrded to \"Escort Pilot\"\n";
                    }
                } else if (memberRoles.contains(IridiumWing)) {
                    newMemberRoles.remove(IridiumWing);
                    out += "\"Iridium Wing\" role removed\n";
                }
                if (memberRoles.contains(pc)) {
                    newMemberRoles.remove(pc);
                    if (pass) {
                        newMemberRoles.add(pcPilot);
                        out += "Upgraded to \"PC Pilot\"\n";
                    } else {
                        out += "\"PC_applicant\" role removed\n";
                    }
                }
                if (memberRoles.contains(xbox)) {
                    newMemberRoles.remove(xbox);
                    if (pass) {
                        newMemberRoles.add(xboxPilot);
                        out += "Upgraded to \"XBOX Pilot\"\n";
                    } else {
                        out += "\"XBOX_applicant\" role removed\n";
                    }
                }
                if (memberRoles.contains(ps4)) {
                    newMemberRoles.remove(ps4);
                    if (pass) {
                        newMemberRoles.add(ps4Pilot);
                        out += "Upgraded to \"PS4 Pilot\"\n";
                    } else {
                        out += "\"PS4_applicant\" role removed\n";
                    }
                }
                if (memberRoles.contains(appl)) {
                    newMemberRoles.remove(appl);
                    out += "\"Applicant\" role removed\n";
                }
                if (memberRoles.contains(needsCE)) {
                    newMemberRoles.remove(needsCE);
                    out += "\"Needs CE\" role removed\n";
                }
                Iterator iterator = memberRoles.iterator();
                while(iterator.hasNext()) {
                    Role element = (Role) iterator.next();
                    System.out.print("memberRoles: "+element.getName() + "\n");
                }
                iterator = newMemberRoles.iterator();
                while(iterator.hasNext()) {
                    Role element = (Role) iterator.next();

                    System.out.print("newMemberRoles: "+element.getName() + "\n");
                }
                event.getGuild().getController().modifyMemberRoles(event.getMember(), newMemberRoles).queue();
                if (pass) {
                    out += "Applicant Succesfully promoted!\n";
                } else {
                    out += "Applicant removed from database\n";
                }
                event.getChannel().sendMessage(out).queue();
                TextChannel channel = event.getGuild().getTextChannelsByName("iridium_Lounge",true).get(0);
                String congrats = new String();
                congrats += "**Congratulations** to " + uApplicant.getAsMention() + " for being promoted to an **Escort Pilot!**\n\n";
                if (memberRoles.contains(pc))
                    congrats += pcPilot.getAsMention() + " ";
                if (memberRoles.contains(xbox))
                    congrats += xboxPilot.getAsMention() + " ";
                if (memberRoles.contains(ps4))
                    congrats += ps4Pilot.getAsMention() + " ";
                congrats += " please welcome a new pilot to your ranks.";

                channel.sendMessage(congrats).queue();
            } else {
                event.getChannel().sendMessage("Applicant not found. Has he been registered via 'applicant new, ...' ?").queue();
            }

        } catch (SQLException e) {
            event.getChannel().sendMessage("Something went wrong. Couldn't find applicant to delete").queue();
            LogUtil.logErr(e);
        }
    }


    private void status(GuildMessageReceivedEvent event) {
        User uApplicant = event.getMessage().getMentionedUsers().get(0);
        Member mApplicant = event.getGuild().getMember(uApplicant);

        try {
            PreparedStatement ps = con.getConnection().prepareStatement("SELECT * FROM applicants WHERE id = ?");
            ps.setString(1, uApplicant.getId());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String out = mApplicant.getEffectiveName() + " progress:\n";
                out += "Eval: " + rs.getInt("eval") + "\n";
                out += "Missions: " + rs.getInt("missions") + "\n";

                event.getChannel().sendMessage(out).queue();
            } else {
                event.getChannel().sendMessage("Applicant not found. Has he been registered via 'applicant new, ...' ?").queue();
            }

        } catch (SQLException e) {
            event.getChannel().sendMessage("Something went wrong. Couldn't get status of applicant").queue();
            LogUtil.logErr(e);
        }
    }

    private void mission(GuildMessageReceivedEvent event) {
        User uApplicant = event.getMessage().getMentionedUsers().get(0);

        try {
            PreparedStatement ps = con.getConnection().prepareStatement("UPDATE applicants SET missions = missions + 1 WHERE id = ? AND missions < 2");
            ps.setString(1, uApplicant.getId());

            if (ps.executeUpdate() == 1) {
                event.getChannel().sendMessage("Added mission done").queue();
            } else {
                event.getChannel().sendMessage("No mission added. Either applicant is already at 2 or he wasn't found.").queue();
            }

        } catch (SQLException e) {
            LogUtil.logErr(e);
        }
    }

    private void combat(GuildMessageReceivedEvent event) {
        User uApplicant = event.getMessage().getMentionedUsers().get(0);
        Role needsCE = event.getGuild().getRolesByName("Needs CE",true).get(0);
        try {
            PreparedStatement ps = con.getConnection().prepareStatement("UPDATE applicants SET eval = eval + 1 WHERE id = ? AND eval < 1");
            ps.setString(1, uApplicant.getId());

            if (ps.executeUpdate() == 1) {
                event.getChannel().sendMessage("Combat eval complete \"Needs CE\" role removed.").queue();
                event.getGuild().getController().removeRolesFromMember(event.getMember(), needsCE).queue();
            } else {
                event.getChannel().sendMessage("No combat eval added. Either applicant already had his or he wasn't found.").queue();
            }
        } catch (SQLException e) {
            LogUtil.logErr(e);
        }
    }

    private void newApplicant(GuildMessageReceivedEvent event, String[] args) {
        User applicant = event.getMessage().getMentionedUsers().get(0);

        try {
            PreparedStatement ps = con.getConnection().prepareStatement("INSERT INTO applicants (id) VALUES (?)");
            ps.setString(1, applicant.getId());
            ps.executeUpdate();

            Role pc = event.getGuild().getRolesByName("PC_applicant",true).get(0);
            Role xbox = event.getGuild().getRolesByName("XBOX_applicant",true).get(0);
            Role ps4 = event.getGuild().getRolesByName("PS4_applicant",true).get(0);
            Role appl = event.getGuild().getRolesByName("Applicant",true).get(0);
            Role IridiumWing = event.getGuild().getRolesByName("Iridium Wing",true).get(0);
            Role needsCE = event.getGuild().getRolesByName("Needs CE",true).get(0);
            Member applicantMem = event.getGuild().getMember(applicant);
            ArrayList<Role> addRoles = new ArrayList();
            addRoles.add(appl);
            addRoles.add(IridiumWing);
            addRoles.add(needsCE);
            Arrays.sort(args);
            String out = new String();
            out +="Added \"Applicant\" role\nAdded \"Iridium Wing\" role\nAdded \"Needs CE\" role\n";
            if (containsCaseInsensitive("pc",Arrays.asList(args))) {
                addRoles.add(pc);
                out +="Added \"PC_Applicant\" role\n";
            }
            if (containsCaseInsensitive("xbox",Arrays.asList(args))) {
                addRoles.add(xbox);
                out +="Added \"XBOX_Applicant\" role\n";
            }
            if (containsCaseInsensitive("ps4",Arrays.asList(args))) {
                addRoles.add(ps4);
                out +="Added \"PS4_Applicant\" role\n";
            }

            event.getGuild().getController().addRolesToMember(applicantMem, addRoles).queue();
            out +="Added new applicant\n";
            event.getChannel().sendMessage(out).queue();

        } catch (MySQLIntegrityConstraintViolationException e) {
            event.getChannel().sendMessage("This applicant is already registered").queue();
        } catch (SQLException e) {
            event.getChannel().sendMessage("Something went wrong. No new applicant saved.").queue();
            LogUtil.logErr(e);
        }
    }

//    TODO: fix this shit
    private void applicantlist (GuildMessageReceivedEvent event) {
        List Applicants = event.getGuild().getMembersWithRoles(event.getGuild().getRolesByName("Applicant",true));
        List PC_applicant = event.getGuild().getMembersWithRoles(event.getGuild().getRolesByName("PC_applicant",true));
        List XBOX_applicant = event.getGuild().getMembersWithRoles(event.getGuild().getRolesByName("XBOX_applicant",true));
        List PS4_applicant = event.getGuild().getMembersWithRoles(event.getGuild().getRolesByName("PS4_applicant",true));

        Iterator iterator = Applicants.iterator();

        ArrayList<String> applicantStats = new ArrayList();
        while(iterator.hasNext()){
            Member element = (Member) iterator.next();
            String applicantStat= new String();
            Boolean found = false;
            applicantStat += "["+element.getEffectiveName()+"]\n"
                +"Type =";
            if(PC_applicant.contains(element)) {
                applicantStat+=" PC";
                found = true;
            }
            if(XBOX_applicant.contains(element)) {
                if(found) {
                    applicantStat+=",";
                }
                applicantStat+=" XBOX";
                found = true;
            }
            if(PS4_applicant.contains(element)) {
                if(found) {
                    applicantStat+=",";
                }
                applicantStat+=" PS4";
                found = true;
            }
            if(!found) {
                applicantStat+= "None! You need to allocate a PC, XBox or PS4 role";
            }
            applicantStat+="\n";
            try {
                PreparedStatement ps = con.getConnection().prepareStatement("SELECT * FROM applicants WHERE id = ?");
                ps.setString(1, element.getUser().getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    applicantStat+= "Application_Date = " + USER_SDF.format(SQL_SDF.parse(rs.getString("timestamp")))+ " UTC\n"+
                            "Combat_Evals = " +rs.getInt("eval") + "\n"+
                            "Mock_Escorts = " + rs.getInt("missions") + "\n";

                } else {
                    applicantStat+="Warning = Not in DB! You need to use /applicant new {discordname}\n";
                }
            } catch (SQLException e) {
                event.getChannel().sendMessage("Something went wrong. Couldn't get applcant!").queue();
                LogUtil.logErr(e);
            } catch (ParseException e) {
                event.getChannel().sendMessage("**SQL Date Failed!**").queue();
                LogUtil.logErr(e);
            }
            applicantStats.add(applicantStat);
        }
        Collections.sort(applicantStats, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.toLowerCase().compareTo(o2.toLowerCase());
            }
        });

        iterator = applicantStats.iterator();
        String out = new String();
        event.getChannel().sendMessage("**Applicants (" + Applicants.size() + ")**\n").queue();
        while(iterator.hasNext()){
            if( out.length()>1800) {
                event.getChannel().sendMessage("```ini\n"+out+"```").queue();
                out = "";
            }
            out += iterator.next() + "\n";
        }
        event.getChannel().sendMessage("```ini\n"+out+"```").queue();

    }

    private void help (GuildMessageReceivedEvent event) {

        String Help = "Applicant Commands and format:\n\n";
        Help += "**Commands**\n" +
                "***new:*** registers new applicant and adds roles\n"+
                "eg. /applicant new,@CMDR,xbox,ps4\n"+
                "***pass:*** Removes applicant and coverts them to a fully fledged escort pilot!\n" +
                "eg. /applicant pass,@CMDR\n"+
                "***fail:*** Removes applicant!\n" +
                "eg. /applicant pass,@CMDR\n"+
                "***combat:*** adds in that combat eval is complete\n" +
                "***mission:*** adds one mission on to completed mock missions\n" +
                "***status:*** displays named applicants current progress\n" +
                "***list:*** displays a list of all applicants who have the \"Applicant\" role and their progress.\n\n";
        Help += "Format is: `/applicant (command), (@applicant name)`\n" + "you can also add more than one command after the applicant name\n" + "ex: `/applicant (new), (@applicantname), (mission)`\n";
        event.getChannel().sendMessage(Help).queue();
    }

    @Override
    public String getHelp(GuildMessageReceivedEvent event) {
        return "";
    }

    public boolean containsCaseInsensitive(String s, List<String> l){
        for (String string : l){
            if (string.equalsIgnoreCase(s)){
                return true;
            }
        }
        return false;
    }
}
