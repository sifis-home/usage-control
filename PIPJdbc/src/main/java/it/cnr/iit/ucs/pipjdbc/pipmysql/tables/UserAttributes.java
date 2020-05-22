package it.cnr.iit.ucs.pipjdbc.pipmysql.tables;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(
    tableName = "users")
public class UserAttributes {

  @DatabaseField(
      id = true,
      columnName = "username")
  private String username;

  @DatabaseField(
      id = false,
      columnName = "name")
  private String name;

  @DatabaseField(
      id = false,
      columnName = "surname")
  private String surname;

  @DatabaseField(
      id = false,
      columnName = "orgname")
  private String organizationName;
  
  @DatabaseField(
	      id = false,
	      columnName = "member")
	  private String group;

  @DatabaseField(
      id = false,
      columnName = "role")
  private String role;
  

  @DatabaseField(
      id = false,
      columnName = "email")
  private String email;

  @DatabaseField(
      id = false,
      columnName = "password")
  private String password;

  @DatabaseField(
      id = false,
      columnName = "salt")
  private String salt;
  
  @DatabaseField(
	      id = false,
	      columnName = "country")
  private String country;

  @DatabaseField(
      id = false,
      columnName = "userscol")
  private String col;

  public String getUsername() {
    return username;
  }

  
  public void setUsername(String username) {
    this.username = username;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }

  public String getRole() {
    return role;
  }
  

  public void setRole(String role) {
    this.role = role;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getSalt() {
    return salt;
  }

  public void setSalt(String salt) {
    this.salt = salt;
  }

  public String getCol() {
    return col;
  }

  public void setCol(String col) {
    this.col = col;
  }
  
  public String getCountry() {
	  return country;
  }

  public void setCountry(String country) {
	  this.country = country;
  }
  
  public String getGroup() {
	  return group;
  }

  public void setGroup(String group) {
	  this.group = group;
  }

  @Override
  public String toString() {
    try {
      return new ObjectMapper().writeValueAsString(this);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

  }

}
