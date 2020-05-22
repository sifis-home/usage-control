package it.cnr.iit.ucs.pipjdbc.pipmysql.tables;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(
    tableName = "user")
public class OrganizationTable {
  @DatabaseField(
      id = true,
      columnName = "orgname")
  private String name;

  @DatabaseField(
      id = false,
      columnName = "address")
  private String address;

  @DatabaseField(
      id = false,
      columnName = "website")
  private String website;

  @DatabaseField(
      id = false,
      columnName = "vatnumber")
  private String vatnumber;

  @DatabaseField(
      id = false,
      columnName = "organizationcol")
  private String col;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public String getVatnumber() {
    return vatnumber;
  }

  public void setVatnumber(String vatnumber) {
    this.vatnumber = vatnumber;
  }

  public String getCol() {
    return col;
  }

  public void setCol(String col) {
    this.col = col;
  }

  @Override
  public String toString() {
    try {
      return new ObjectMapper().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

}
