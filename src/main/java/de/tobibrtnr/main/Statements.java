package de.tobibrtnr.main;

/**
 * This class contains the sql statements as constants.
 */
public class Statements {
    // Return all known members of all groups.
    public static final String PARTICIPANT_STATEMENT = "" + 
    "select j_g.raw_string as groupstring, j_u.raw_string as userstring " +
    "from group_participant_user gpu " +
    "join jid j_g on (j_g._id = group_jid_row_id) " +
    "join jid j_u on (j_u._id = user_jid_row_id) " +
    "where groupstring like '%@g.us' and userstring not like 'status_me'";

    // Returns all known group members by sent messages.
    public static final String MESSAGE_STATEMENT = "" + 
    "select distinct j_g.raw_string as groupstring, j_u.raw_string as userstring " +
    "from message " +
    "join jid j_u on (j_u._id = sender_jid_row_id)  " +
    "join chat c on (chat_row_id = c._id) " +
    "join jid j_g on (j_g._id = c.jid_row_id)";

    // Returns all group names.
    public static final String GROUP_STATMENT = "" + 
    "select j.raw_string as groupstring, c.subject " +
    "from chat c " +
    "join jid j on c.jid_row_id = j._id " + 
    "where j.raw_string like '%@g.us' and subject is not null";

    // Returns all saved contact names.
    public static final String CONTACT_STATEMENT = "" +
    "select jid, wa_name || ' (' || _id || ')' as name " +
    "from wa_contacts " +
    "where jid like '%@s.whatsapp.net' and wa_name is not null and display_name is null " +
    "union " +
    "select jid, display_name as name " +
    "from wa_contacts " +
    "where jid like '%@s.whatsapp.net' and display_name is not null";
}
