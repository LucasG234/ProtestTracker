package com.lucasg234.protesttracker.models;

import com.parse.ParseClassName;
import com.parse.ParseUser;

/**
 * Parse object which extends ParseUser
 * Defined and referenced to make adding additional fields to ParseUser possible
 */
@ParseClassName("_User")
public class User extends ParseUser {

}
