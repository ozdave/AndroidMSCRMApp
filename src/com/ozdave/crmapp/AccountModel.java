package com.ozdave.crmapp;




public class AccountModel {

	
	
	/**
	 * Build a typed account from a CRM Entity
	 * @param ent
	 */
	public AccountModel(CRMEntity ent) {
		
		if (ent.getName().equals("account")) {
			setId(ent.getId());
			setName(ent.getAttributes().get("name"));
			setPhone1(ent.getAttributes().get("telephone1"));
			setEmail1(ent.getAttributes().get("emailaddress1"));
		}
	}
	
	
	/**
	 * Override toString() to show what should appear in the list view
	 */
	public String toString() {	
		return getName() + " : " + getPhone1();
	}
	
	
	

	private String m_name;
	
	private String m_phone1;
	
	private String m_email1;
	
	private String m_id;
	
	
	public String getName() {
		return m_name;
	}

	public void setName(String m_name) {
		this.m_name = m_name;
	}


	public String getPhone1() {
		return m_phone1;
	}


	public void setPhone1(String m_phone1) {
		this.m_phone1 = m_phone1;
	}


	public String getEmail1() {
		return m_email1;
	}


	public void setEmail1(String m_email1) {
		this.m_email1 = m_email1;
	}


	public String getId() {
		return m_id;
	}


	public void setId(String m_id) {
		this.m_id = m_id;
	}


	
	

	
}
