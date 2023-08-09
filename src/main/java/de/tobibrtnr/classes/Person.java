package de.tobibrtnr.classes;

import java.util.ArrayList;
import java.util.List;

import de.tobibrtnr.main.WhatsappDBParser;

public class Person {
   
    // A Person consists of their number, name and a unique ID.
    private String number;
    private String name;
    private int id;

    // All groups where the person is mentioned.
    private List<String> groupList = new ArrayList<>();

    /**
     * Constructor for a person. Sets number and name.
     * @param number Number of the Person. Required.
     * @param name Name of the Person. Optional.
     */
    public Person(String number, String name) {
        this.number = number;
        this.name = name;
        id = NumberID.get();
        NumberID.add();
    }

    /**
     * Sets the name of the person
     * @param name The new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Adds a new group to the list.
     * @param g The group to add
     */
    public void addGroup(String g) {
        groupList.add(g);
    }
     
    /**
     * Returns the number of the person.
     * @return The person's number
     */
    public String getNumber() {
        return number;
    }

    /**
     * Returns the id of the person.
     * @return The person's unique id.
     */
    public int getId() {
        return id;
    }

    /** Returns the groups of the person as 
     * string for debug purposes.
     */
    public String getGroups() {
        return groupList.toString();
    }

    /**
     * To-String method for a person. Returns the name, group name or the person's number.
     */
    @Override
    public String toString() {
        if(name != null) {
            return name;
        } else if(!groupList.isEmpty()) {
            if(WhatsappDBParser.doesContainGroupName(groupList.get(0))) 
                return new StringBuilder().append(WhatsappDBParser.getRealGroupName(groupList.get(0))).append('(').append(id).append(')').toString();
        }
        return number.split("@")[0];
    }

    /**
     * Equals-method for a person. Compares only the telephone numbers of the persons.
     */
    @Override
    public boolean equals(Object obj) {
        if(obj == null) return false;
        else if(obj.getClass() == String.class) {
            return number.equals(obj);
        } else if(obj.getClass() == Person.class) {
            return number.equals(((Person)obj).getNumber());
        }
        return false;
    }
}
