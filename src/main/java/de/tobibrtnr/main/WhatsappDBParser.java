package de.tobibrtnr.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tobibrtnr.classes.Person;

/*
 * Entrypoint for creating the network graph.
 */
public class WhatsappDBParser {

    // Enter user data using BufferReader
    private static BufferedReader reader = new BufferedReader(
            new InputStreamReader(System.in));

    // SQLite connection object
    private static Connection connection;

    // Own number that needs to be filtered out
    private static String ownNumber;

    // FirstLine for the edges csv file.
    private static final String FL_EDGEMAP = "source,target,weight";
    // RegEx pattern that matches any character that is not a letter, digit or
    // separator. Used to remove all other chars from group names.
    private static final Pattern GROUP_PATTERN = Pattern.compile("[^\\p{L}\\p{N}\\p{Z}]",
            Pattern.UNICODE_CHARACTER_CLASS);

    // Maps a List of group members to the unique raw_strings of the groups
    private static HashMap<String, ArrayList<Person>> participantMap = new HashMap<>();
    // All person edges with the strength of their connection.
    private static Map<Set<String>, Integer> personEdgeMap = new HashMap<>();
    // All Persons, identified by their phone number, together with
    // their respective person object
    private static HashMap<String, Person> allPersonsMap = new HashMap<>();
    // All groups, identified by their unique raw_string, together with the group
    // name
    private static HashMap<String, String> groupNameMap = new HashMap<>();

    /**
     * Main method, running the whole process
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) {
        try {
            // Create a connection to the specified sqlite database file.
            setSqlConnection();
            System.out.println("2/9 | Connection to SQLite database established.");

            ownNumber = getUserInput(
                    "3/9 | Please submit your own number without the + or any spaces (e.g. 491604593939):");

            // Add all group names to map
            setGroupNames();

            // Add all participants
            addParticipants(Statements.PARTICIPANT_STATEMENT);
            addParticipants(Statements.MESSAGE_STATEMENT);

            setWADB();

        } catch (SQLException | IOException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            e.printStackTrace();
            return;
        }

        boolean nameMode = getUserInput(
                "5/9 | Please choose name mode: 0 for names / numbers included, anything else for anonymous ids")
                .equals("0");

        String excludedGroupsInput = getUserInput(
                "6/9 | You can optionally add groups that should be left out from the graph, seperated by semicolons. A part of the name is sufficient.");

        String[] excludedGroups = Arrays.stream(excludedGroupsInput.split(";")).map(String::trim)
                .toArray(String[]::new);

        participantMap.forEach((k, list) -> {

            // Skip if a group name contains one of the the specified names
            if (excludedGroupsInput.length() > 0
                    && Arrays.stream(excludedGroups).anyMatch(getRealGroupName(k)::contains)) {
                System.out.println(String.format("Skipped Group \"%s\"", getRealGroupName(k)));
                return;
            }

            for (int i = list.size() - 1; i > 0; i--) {
                // Go through every combination of 2 persons
                for (int j = i - 1; j >= 0; j--) {
                    if (!(list.get(i).equals(list.get(j))) &&
                            !list.get(i).getNumber().contains(ownNumber) &&
                            !list.get(j).getNumber().contains(ownNumber)) {
                        // Create Set with the 2 persons
                        Set<String> tmpSet = new HashSet<>();
                        if (nameMode) {
                            tmpSet.add(String.valueOf(list.get(j).toString()));
                            tmpSet.add(String.valueOf(list.get(i).toString()));
                        } else {
                            tmpSet.add(String.valueOf(list.get(j).getId()));
                            tmpSet.add(String.valueOf(list.get(i).getId()));
                        }

                        // Add combination or increase weight of edge
                        if (tmpSet.size() == 2) {
                            if (personEdgeMap.containsKey(tmpSet)) {
                                personEdgeMap.put(tmpSet, personEdgeMap.get(tmpSet) + 1);
                            } else {
                                personEdgeMap.put(tmpSet, 1);
                            }
                        }
                    }
                }
            }
        });

        String exportFileName = getUserInput("7/9 | Please submit the name of the export file.");
        boolean fileMode = getUserInput(
                "8/9 | Please choose file mode: 0 for gephi csv output, anything else for json output").equals("0");
        exportEdges(FL_EDGEMAP, String.format("%s.%s", exportFileName, fileMode ? "csv" : "json"), fileMode, nameMode);

        try {
            if (connection != null)
                connection.close();
        } catch (SQLException e) {
            // connection close failed.
            e.printStackTrace();
        }

        System.out.println("9/9 | Successfully generated and exported the edges file.");
        System.out.println(String.format("Your whole Network contains %s persons and %s edges.", allPersonsMap.size(),
                personEdgeMap.size()));
    }

    /**
     * Creates a SQLite connection to a file that will be specified by the user
     * using the console
     * 
     * @throws IOException
     */
    private static void setSqlConnection() throws SQLException, IOException {
        String fileName = getUserInput("1/9 | Please enter the name of the decrypted database file:");
        if (!new File(fileName).exists()) {
            throw new IOException("The specified database file does not exist.");
        }
        connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s", fileName));
    }

    /**
     * Reads a user-entered line from the console.
     * 
     * @param question A prompt that will be displayed.
     * @return The string entered by the user.
     */
    private static String getUserInput(String question) {
        System.out.println(question);
        try {
            return reader.readLine();
        } catch (IOException e) {
            System.err.println("Error while getting user input.");
            return "";
        }
    }

    /**
     * Gets all groups and adds them to their respective hash map.
     */
    private static void setGroupNames() throws SQLException {
        Statement statement = connection.createStatement();
        statement.setQueryTimeout(30); // set timeout to 30 sec.
        ResultSet rs = statement.executeQuery(Statements.GROUP_STATMENT);
        while (rs.next()) {
            String groupstring = rs.getString("groupstring");
            String subject = rs.getString("subject");
            Matcher matcher = GROUP_PATTERN.matcher(subject);
            subject = matcher.replaceAll("").trim();

            groupNameMap.put(groupstring, subject);
        }
    }

    /**
     * Adds all persons that were returned from the sql statement to the suiting
     * group in the groupNameMap. If necessary, the person will also be added to the
     * allPersonsMap.
     * 
     * @param participantStatement The sql statement returning persons with their
     *                             groups.
     */
    private static void addParticipants(String participantStatement) throws SQLException {
        Statement statement = connection.createStatement();
        statement.setQueryTimeout(30); // set timeout to 30 sec.
        ResultSet rs = statement.executeQuery(participantStatement);
        while (rs.next()) {
            // Gets the userstring and groupstring, status_me equals the user themself.
            String userstring = rs.getString("userstring");
            String groupstring = rs.getString("groupstring");
            String name = null;

            if (userstring.equals("status_me")) {
                userstring = String.format("%s@s.whatsapp.net", ownNumber);
                name = "Me";
            }

            // Adds the person to the allPersonsMap if it does not yet exist
            // or adds the current group of the person.
            Person pers;
            if (allPersonsMap.containsKey(userstring)) {
                pers = allPersonsMap.get(userstring);
                pers.addGroup(groupstring);
            } else {
                pers = new Person(userstring, name);
                pers.addGroup(groupstring);
                allPersonsMap.put(userstring, pers);
            }

            // Adds the group to the participant map if it does not yet exist
            // and adds the person as a member.
            if (!participantMap.containsKey(groupstring)) {
                participantMap.put(groupstring, new ArrayList<Person>());
                participantMap.get(groupstring).add(pers);
            } else {
                List<Person> tmpList = participantMap.get(groupstring);
                if (!tmpList.contains(pers)) {
                    tmpList.add(pers);
                }
            }
        }
    }

    /**
     * Reads the contact names from wa.db and
     * sets them for the respective numbers.
     */
    private static void setWADB() {
        String dbName = getUserInput("4/9 If you have a wa.db file (optional), please submit down its file name:");
        Connection wadb_connection = null;
        try {
            // create a database connection
            wadb_connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s", dbName));

            Statement statement = wadb_connection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.
            ResultSet rs = statement.executeQuery(Statements.CONTACT_STATEMENT);
            while (rs.next()) {
                String raw = rs.getString("jid");
                String name = rs.getString("name");

                if (allPersonsMap.containsKey(raw)) {
                    allPersonsMap.get(raw).setName(name);
                }
            }

            if (wadb_connection != null)
                wadb_connection.close();
        } catch (SQLException e) {
            System.out.println("Error while getting contact names, skipping.");
        }
    }

    /**
     * Exports the personEdgeMap as .csv or .json file.
     * 
     * @param firstline  First Line of the csv file
     * @param targetfile Name of the target file
     * @param csvMode    Specifies csv or json mode
     */
    private static void exportEdges(String firstline, String targetfile, boolean csvMode, boolean nameMode) {
        List<String> dataEdgeLines = new ArrayList<>();

        if (csvMode) {
            dataEdgeLines.add(firstline);
        } else {
            dataEdgeLines.add("[");
        }

        // Edges
        personEdgeMap.forEach((k, v) -> {
            Object[] tmp = k.toArray();
            double d = v;
            if (csvMode) {
                dataEdgeLines.add(
                        new StringBuilder().append(tmp[0]).append(',').append(tmp[1]).append(',').append(d).toString());
            } else {
                StringBuilder sb = new StringBuilder().append('[');
                if (nameMode) {
                    sb.append('"').append(tmp[0]).append('"').append(",")
                            .append('"').append(tmp[1]).append('"');
                } else {
                    sb.append(tmp[0])
                        .append(',').append(tmp[1]);
                }
                dataEdgeLines.add(sb
                        .append(',').append(d).append("],").toString());
            }
        });

        if (!csvMode) {
            // remove comma of last node
            dataEdgeLines.set(dataEdgeLines.size() - 1, dataEdgeLines.get(dataEdgeLines.size() - 1).substring(0,
                    dataEdgeLines.get(dataEdgeLines.size() - 1).length() - 1));

            dataEdgeLines.add("]");
        }

        // Set Edges File
        File edgeOutputFile = new File(targetfile);
        try (PrintWriter pw = new PrintWriter(edgeOutputFile)) {
            dataEdgeLines.forEach(pw::println);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns if a given raw_string available in the groupNameMap.
     * 
     * @param name The raw_string of the group
     * @return If the group is available
     */
    public static boolean doesContainGroupName(String name) {
        return false;
    }

    /**
     * Returns the real group name for the given raw string
     * 
     * @param raw The raw_string of the group
     * @return The real group name
     */
    public static String getRealGroupName(String raw) {
        String name = groupNameMap.get(raw);
        return name != null ? name : raw;
    }
}
