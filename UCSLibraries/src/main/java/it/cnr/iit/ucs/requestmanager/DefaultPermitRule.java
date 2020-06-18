package it.cnr.iit.ucs.requestmanager;

import it.cnr.iit.utility.JAXBUtility;
import oasis.names.tc.xacml.core.schema.wd_17.RuleType;

public class DefaultPermitRule {

	// TODO add obligation in the rule
	private static final String defaultPermit = "<Rule Effect=\"Permit\" RuleId=\"def-permit\"></Rule>";

	public static final RuleType getInstance() {
		try {
			RuleType ruleType = JAXBUtility.unmarshalToObject(RuleType.class, defaultPermit);
			return ruleType;
		} catch (Exception exception) {
			return null;
		}
	}
}
