package olbot;

import com.mysql.jdbc.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Timer;
import java.util.TimerTask;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionRemoveEvent;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;

public class Handler extends MainBot {

	public static final String PREFIX = "<"; // command prefix
	public static final String REACTION = ":ballot_box_with_check:";
	public static boolean gReady = false;

	@EventSubscriber
	public void onReady(ReadyEvent event) {
		String[] queries = {
			"CREATE TABLE IF NOT EXISTS servers("
			+ "id INT AUTO_INCREMENT, server_id BIGINT NOT NULL, channel_id BIGINT "
			+ "NOT NULL, manager_role VARCHAR(32) NOT NULL,"
			+ "participant_role VARCHAR(32) NOT NULL, host_role VARCHAR(32) NOT NULL,"
			+ "start_time BIGINT NOT NULL, lobby_count INT NOT NULL, lobbies_per_day INT,"
			+ "limit_lobbies BIT(1) NOT NULL,"
			+ "PRIMARY KEY(id))",
			"CREATE TABLE IF NOT EXISTS blacklist("
			+ "id INT AUTO_INCREMENT, user_id BIGINT NOT NULL, PRIMARY KEY(id))",
			"CREATE TABLE IF NOT EXISTS lobbies("
			+ "id INT AUTO_INCREMENT, author_id BIGINT NOT NULL, server_id BIGINT NOT "
			+ "NULL, link VARCHAR(256) NOT NULL,"
			+ "description VARCHAR(200) NOT NULL, message_id BIGINT NOT NULL, PRIMARY KEY(id))",
		};
		
		for (String query : queries) {
			Handler.executeUpdate(query);
		}
		System.out.println("Ready!");
		client.changePlayingText(PREFIX + "help");
		schedule(10000);
		gReady = true;
	}

	@EventSubscriber
	public void messageReceived(MessageReceivedEvent event) {
		try {
			if (!gReady && client.isReady()) {
				if (!client.getOurUser().getPresence().getPlayingText().isPresent()) {
					client.changePlayingText(PREFIX + "help");
				}
				System.out.println("Ready!");
				schedule(10000);
				gReady = true;
			}
			String query = "SELECT 1";
			try {
				executeQuery(query);
			} catch (Exception ex) {
				try {
					MainBot.disconnect();
					System.gc();
					MainBot.connect();
				} catch (Exception ex2) {
				}
			}
			ImprovedString message = new ImprovedString(event.getMessage().getContent());
			String command = message.getWord(0);
			query = String.format("SELECT channel_id, manager_role, participant_role, host_role "
					+ "FROM servers WHERE server_id=%d",
					event.getGuild().getLongID());
			ResultSet s = executeQuery(query);
			if (s.next()) {
				long channelId = s.getLong(1);
				String managerRoleName = s.getString(2);
				String participantRoleName = s.getString(3);
				String hostRoleName = s.getString(4);
				boolean managerRoleMissing = false;
				IRole managerRole = null;
				IRole participantRole = null;
				IRole hostRole = null;
				try {
					managerRole = event.getGuild().getRolesByName(managerRoleName).get(0);
				} catch (IndexOutOfBoundsException ex) {
					managerRoleMissing = true;
				}try {
					participantRole = event.getGuild().getRolesByName(participantRoleName).get(0);
				} catch (IndexOutOfBoundsException ex) {
				}
				try {
					hostRole = event.getGuild().getRolesByName(hostRoleName).get(0);
				} catch (IndexOutOfBoundsException ex) {
				}
				if (command.equalsIgnoreCase(PREFIX + "start") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot()) {
					Functions.start(event, message, channelId, managerRoleMissing, managerRole, hostRole);
				} else if (command.equalsIgnoreCase(PREFIX + "join") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot()) {
					Functions.join(event, participantRole);
				} else if (command.equalsIgnoreCase(PREFIX + "leave") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot()) {
					Functions.leave(event, participantRole, hostRole);
				} else if (command.equalsIgnoreCase(PREFIX + "stop") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot()) {
					Functions.stop(event, managerRoleMissing, managerRole, participantRole, hostRole);
				} else if (command.equalsIgnoreCase(PREFIX + "setup") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot()) {
					Functions.resetup(event, message);
				} else if (command.equalsIgnoreCase(PREFIX + "help") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot()) {
					Functions.help(event);
				} else if (command.equalsIgnoreCase(PREFIX + "tos") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot()) {
					Functions.tos(event);
				} else if (command.equalsIgnoreCase(PREFIX + "test") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot()) {
					Functions.test(event);
				} else if (command.equalsIgnoreCase(PREFIX + "blacklist") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot()) {
					Functions.blacklist(event, message);
				} else if (command.equalsIgnoreCase(PREFIX + "addbot") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot()) {
					Functions.addbot(event);
				} else if (command.equals(PREFIX + "disconnect") && event.getAuthor().getLongID()
						== client.getApplicationOwner().getLongID()) {
					MainBot.disconnect();
					event.getChannel().sendMessage(Utils.generateSuccess("Disconnected! Bot will "
							+ "be unusable until next restart."));
					MainBot.client.logout();
					client.getDispatcher().unregisterListener(this);
				}

			} else {
				if (command.equalsIgnoreCase(PREFIX + "setup")
						&& !event.getAuthor().isBot()) {
					try {
						Functions.setup(event, message);
					} catch (Exception ex) {
						event.getChannel().sendMessage(Utils.INVALID_FORMAT);
					}
				} else if (command.equalsIgnoreCase(PREFIX + "help") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot()) {
					Functions.help(event);
				} else if (command.equalsIgnoreCase(PREFIX + "tos") && !event.getAuthor().isBot()) {
					Functions.tos(event);
				} else if (command.equalsIgnoreCase(PREFIX + "test") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot()) {
					Functions.test(event);
				} else if (command.equalsIgnoreCase(PREFIX + "blacklist") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot()) {
					Functions.blacklist(event, message);
				} else if (command.equalsIgnoreCase(PREFIX + "addbot") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot()) {
					Functions.addbot(event);
				} else if ((command.equalsIgnoreCase(PREFIX + "start") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot())
						|| (command.equalsIgnoreCase(PREFIX + "stop") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot())
						|| (command.equalsIgnoreCase(PREFIX + "join") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot())
						|| (command.equalsIgnoreCase(PREFIX + "leave") && !isBlacklisted(event.getAuthor())
						&& !event.getAuthor().isBot())) {
					event.getChannel().sendMessage(Utils.generateWarning(String.format("Those commands are only"
							+ " available for registered servers. If you are an admin, you can "
							+ "do that by executing `%ssetup` explained more in depth using "
							+ "`%shelp`.", PREFIX, PREFIX)));
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.gc();
		}
		System.gc();
	}

	@EventSubscriber
	public void reactionAdded(ReactionAddEvent event) {
		try {
			String query = String.format("SELECT participant_role FROM servers WHERE server_id"
					+ "=%d", event.getGuild().getLongID());
			ResultSet s = executeQuery(query);
			IRole pr = null;
			if (s.next()) {
				pr = event.getGuild().getRolesByName(s.getString(1)).get(0);
			}
			if (pr != null) {
				query = String.format("SELECT message_id FROM lobbies WHERE server_id"
						+ "=%d", event.getGuild().getLongID());
				s = executeQuery(query);
				if (s.next()) {
					long messageId = s.getLong(1);
					if (event.getMessage().getLongID() == messageId && !event.getUser().isBot()) {
						if (!isBlacklisted(event.getUser())) {
							if (event.getReaction().getUnicodeEmoji().getAliases()
									.contains(REACTION.replace(":", ""))) {
								Functions.join(event, pr);
							}
						}
					}
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.gc();
	}
	
	@EventSubscriber
	public void reactionRemoved (ReactionRemoveEvent event) {
		try {
			String query = String.format("SELECT participant_role, host_role FROM servers WHERE server_id"
					+ "=%d", event.getGuild().getLongID());
			ResultSet s = executeQuery(query);
			IRole pr = null;
			IRole hr = null;
			if (s.next()) {
				pr = event.getGuild().getRolesByName(s.getString(1)).get(0);
				hr = event.getGuild().getRolesByName(s.getString(2)).get(0);
			}
			if (pr != null && hr != null) {
				query = String.format("SELECT message_id FROM lobbies WHERE server_id"
						+ "=%d", event.getGuild().getLongID());
				s = executeQuery(query);
				if (s.next()) {
					long messageId = s.getLong(1);
					if (event.getMessage().getLongID() == messageId && !event.getUser().isBot()) {
						if (!isBlacklisted(event.getUser())) {
							if (event.getReaction().getUnicodeEmoji().getAliases()
									.contains(REACTION.replace(":", ""))) {
								
								Functions.leave(event, pr, hr);
							}
						}
					}
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.gc();
	}

	// methods for easy use
	public static ResultSet executeQuery(String query) {
		Statement statement = null;
		ResultSet rs = null;
		try {
			statement = MainBot.connection.createStatement();
			rs = statement.executeQuery(query);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		finally {
			return rs;
		}
	}

	public static int executeUpdate(String query) {
		Statement statement = null;
		int n = 0;
		try {
			statement = MainBot.connection.createStatement();
			n = statement.executeUpdate(query);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		finally {
			return n;
		}
	}
	

	public static boolean execute(String query) {
		Statement statement = null;
		boolean executed = false;
		try {
			statement = MainBot.connection.createStatement();
			executed = statement.execute(query);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		finally {
			return executed;
		}
	}

	public static boolean isBlacklisted(IUser user) {
		try {
			String query = String.format("SELECT id FROM blacklist WHERE user_id=%d",
					user.getLongID());
			return executeQuery(query).next();
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
	public void schedule (long millis) {
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run () {
				Functions.update();
			}
		}, 0, millis);
	}
	
	public static void quietClose(Connection c) {
		try {
			c.close();
		} catch (Exception ex) {
		}
	}
	public static void quietClose(Statement s) {
		try {
			s.close();
		} catch (Exception ex) {
		}
	}
	public static void quietClose(ResultSet rs) {
		try {
			rs.close();
		} catch (Exception ex) {
		}
	}
}
