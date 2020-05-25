package it.cnr.iit.ucs.pipjdbc.pipmysql.tables;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "users")
public class UserAttributes {
	public static final String USERNAME_FIELD = "username";
	public static final String NAME_FIELD = "name";
	public static final String SURNAME_FIELD = "surname";
	public static final String COUNTRY_FIELD = "country";
	public static final String ORGNAME_FIELD = "orgname";
	public static final String MEMBER_FIELD = "member";
	public static final String ROLE_FIELD = "role";
	public static final String EMAIL_FIELD = "email";
	public static final String PASSWORD_FIELD = "password";
	public static final String USERSCOL_FIELD = "userscol";
	public static final String SALT_FIELD = "salt";

	@DatabaseField(generatedId = true, columnName = USERNAME_FIELD, canBeNull = false)
	private String username;

	@DatabaseField(columnName = NAME_FIELD)
	private String name;

	@DatabaseField(columnName = SURNAME_FIELD)
	private String surname;

	@DatabaseField(columnName = COUNTRY_FIELD, canBeNull = false)
	private String country;

	@DatabaseField(columnName = ORGNAME_FIELD, canBeNull = false)
	private String orgname;

	@DatabaseField(columnName = MEMBER_FIELD, canBeNull = false)
	private String member;

	@DatabaseField(columnName = ROLE_FIELD, canBeNull = false)
	private String role;

	@DatabaseField(columnName = EMAIL_FIELD)
	private String email;

	@DatabaseField(columnName = PASSWORD_FIELD, canBeNull = false)
	private String password;

	@DatabaseField(columnName = USERSCOL_FIELD)
	private String userscol;

	@DatabaseField(columnName = SALT_FIELD)
	private String salt;

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

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getOrgname() {
		return orgname;
	}

	public void setOrgname(String orgname) {
		this.orgname = orgname;
	}

	public String getMember() {
		return member;
	}

	public void setMember(String member) {
		this.member = member;
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

	public String getUserscol() {
		return userscol;
	}

	public void setUserscol(String userscol) {
		this.userscol = userscol;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

}
