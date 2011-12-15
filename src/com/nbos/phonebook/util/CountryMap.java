package com.nbos.phonebook.util;

import java.util.HashMap;
import java.util.Map;


public class CountryMap {

	Map<String, CountryIndex> countries = new HashMap<String, CountryIndex>();
	/**
	 * @param args
	 */
	
	public static void main(String[] args) {
		
	}
	
	public String getCallingCode(String alphaCode) {
		return countries.get(alphaCode).code;
	}
	
	public int getIndex(String alphaCode) {
		return countries.get(alphaCode).index;
	}

	public CountryMap() {
		int index = 0;
		countries.put("AF", new CountryIndex(index++, new String("93")));
		countries.put("AL", new CountryIndex(index++, new String("355")));
		countries.put("DZ", new CountryIndex(index++, new String("213")));
		countries.put("AS", new CountryIndex(index++, new String("1")));
		countries.put("AD", new CountryIndex(index++, new String("376")));
		countries.put("AO", new CountryIndex(index++, new String("244")));
		countries.put("AI", new CountryIndex(index++, new String("1")));
		countries.put("AG", new CountryIndex(index++, new String("1")));
		countries.put("AR", new CountryIndex(index++, new String("54")));
		countries.put("AM", new CountryIndex(index++, new String("374")));
		countries.put("AW", new CountryIndex(index++, new String("297")));
		countries.put("AU", new CountryIndex(index++, new String("61")));
		countries.put("AT", new CountryIndex(index++, new String("43")));
		countries.put("AZ", new CountryIndex(index++, new String("994")));
		countries.put("BH", new CountryIndex(index++, new String("973")));
		countries.put("BD", new CountryIndex(index++, new String("880")));
		countries.put("BB", new CountryIndex(index++, new String("1")));
		countries.put("BY", new CountryIndex(index++, new String("375")));
		countries.put("BE", new CountryIndex(index++, new String("32")));
		countries.put("BZ", new CountryIndex(index++, new String("501")));
		countries.put("BJ", new CountryIndex(index++, new String("229")));
		countries.put("BM", new CountryIndex(index++, new String("1")));
		countries.put("BT", new CountryIndex(index++, new String("975")));
		countries.put("BO", new CountryIndex(index++, new String("591")));
		countries.put("BQ", new CountryIndex(index++, new String("599")));
		countries.put("BA", new CountryIndex(index++, new String("387")));
		countries.put("BW", new CountryIndex(index++, new String("267")));
		countries.put("BR", new CountryIndex(index++, new String("55")));
		countries.put("IO", new CountryIndex(index++, new String("246")));
		countries.put("VG", new CountryIndex(index++, new String("1")));
		countries.put("BN", new CountryIndex(index++, new String("673")));
		countries.put("BG", new CountryIndex(index++, new String("359")));
		countries.put("BF", new CountryIndex(index++, new String("226")));
		countries.put("BI", new CountryIndex(index++, new String("257")));
		countries.put("KH", new CountryIndex(index++, new String("855")));
		countries.put("CM", new CountryIndex(index++, new String("237")));
		countries.put("CA", new CountryIndex(index++, new String("1")));
		countries.put("CV", new CountryIndex(index++, new String("238")));
		countries.put("KY", new CountryIndex(index++, new String("1")));
		countries.put("CF", new CountryIndex(index++, new String("236")));
		countries.put("TD", new CountryIndex(index++, new String("235")));
		countries.put("CL", new CountryIndex(index++, new String("56")));
		countries.put("CN", new CountryIndex(index++, new String("86")));
		countries.put("CO", new CountryIndex(index++, new String("57")));
		countries.put("KM", new CountryIndex(index++, new String("269")));
		countries.put("CK", new CountryIndex(index++, new String("682")));
		countries.put("CR", new CountryIndex(index++, new String("506")));
		countries.put("CI", new CountryIndex(index++, new String("225")));
		countries.put("HR", new CountryIndex(index++, new String("385")));
		countries.put("CU", new CountryIndex(index++, new String("53")));
		countries.put("CW", new CountryIndex(index++, new String("599")));
		countries.put("CY", new CountryIndex(index++, new String("357")));
		countries.put("CZ", new CountryIndex(index++, new String("420")));
		countries.put("CD", new CountryIndex(index++, new String("243")));
		countries.put("DK", new CountryIndex(index++, new String("45")));
		countries.put("DJ", new CountryIndex(index++, new String("253")));
		countries.put("DM", new CountryIndex(index++, new String("1")));
		countries.put("DO", new CountryIndex(index++, new String("1")));
		countries.put("EC", new CountryIndex(index++, new String("593")));
		countries.put("EG", new CountryIndex(index++, new String("20")));
		countries.put("SV", new CountryIndex(index++, new String("503")));
		countries.put("GQ", new CountryIndex(index++, new String("240")));
		countries.put("ER", new CountryIndex(index++, new String("291")));
		countries.put("EE", new CountryIndex(index++, new String("372")));
		countries.put("ET", new CountryIndex(index++, new String("251")));
		countries.put("FK", new CountryIndex(index++, new String("500")));
		countries.put("FO", new CountryIndex(index++, new String("298")));
		countries.put("FM", new CountryIndex(index++, new String("691")));
		countries.put("FJ", new CountryIndex(index++, new String("679")));
		countries.put("FI", new CountryIndex(index++, new String("358")));
		countries.put("FR", new CountryIndex(index++, new String("33")));
		countries.put("GF", new CountryIndex(index++, new String("594")));
		countries.put("PF", new CountryIndex(index++, new String("689")));
		countries.put("GA", new CountryIndex(index++, new String("241")));
		countries.put("GE", new CountryIndex(index++, new String("995")));
		countries.put("DE", new CountryIndex(index++, new String("49")));
		countries.put("GH", new CountryIndex(index++, new String("233")));
		countries.put("GI", new CountryIndex(index++, new String("350")));
		countries.put("GR", new CountryIndex(index++, new String("30")));
		countries.put("GL", new CountryIndex(index++, new String("299")));
		countries.put("GD", new CountryIndex(index++, new String("1")));
		countries.put("GP", new CountryIndex(index++, new String("590")));
		countries.put("GU", new CountryIndex(index++, new String("1")));
		countries.put("GT", new CountryIndex(index++, new String("502")));
		countries.put("GN", new CountryIndex(index++, new String("224")));
		countries.put("GW", new CountryIndex(index++, new String("245")));
		countries.put("GY", new CountryIndex(index++, new String("592")));
		countries.put("HT", new CountryIndex(index++, new String("509")));
		countries.put("HN", new CountryIndex(index++, new String("504")));
		countries.put("HK", new CountryIndex(index++, new String("852")));
		countries.put("HU", new CountryIndex(index++, new String("36")));
		countries.put("IS", new CountryIndex(index++, new String("354")));
		countries.put("IN", new CountryIndex(index++, new String("91")));
		countries.put("ID", new CountryIndex(index++, new String("62")));
		countries.put("IR", new CountryIndex(index++, new String("98")));
		countries.put("IQ", new CountryIndex(index++, new String("964")));
		countries.put("IE", new CountryIndex(index++, new String("353")));
		countries.put("IL", new CountryIndex(index++, new String("972")));
		countries.put("IT", new CountryIndex(index++, new String("39")));
		countries.put("JM", new CountryIndex(index++, new String("1")));
		countries.put("JP", new CountryIndex(index++, new String("81")));
		countries.put("JO", new CountryIndex(index++, new String("962")));
		countries.put("KZ", new CountryIndex(index++, new String("7")));
		countries.put("KE", new CountryIndex(index++, new String("254")));
		countries.put("KI", new CountryIndex(index++, new String("686")));
		countries.put("XK", new CountryIndex(index++, new String("381")));
		countries.put("KW", new CountryIndex(index++, new String("965")));
		countries.put("KG", new CountryIndex(index++, new String("996")));
		countries.put("LA", new CountryIndex(index++, new String("856")));
		countries.put("LV", new CountryIndex(index++, new String("371")));
		countries.put("LB", new CountryIndex(index++, new String("961")));
		countries.put("LS", new CountryIndex(index++, new String("266")));
		countries.put("LR", new CountryIndex(index++, new String("231")));
		countries.put("LY", new CountryIndex(index++, new String("218")));
		countries.put("LI", new CountryIndex(index++, new String("423")));
		countries.put("LT", new CountryIndex(index++, new String("370")));
		countries.put("LU", new CountryIndex(index++, new String("352")));
		countries.put("MO", new CountryIndex(index++, new String("853")));
		countries.put("MK", new CountryIndex(index++, new String("389")));
		countries.put("MG", new CountryIndex(index++, new String("261")));
		countries.put("MW", new CountryIndex(index++, new String("265")));
		countries.put("MY", new CountryIndex(index++, new String("60")));
		countries.put("MV", new CountryIndex(index++, new String("960")));
		countries.put("ML", new CountryIndex(index++, new String("223")));
		countries.put("MT", new CountryIndex(index++, new String("356")));
		countries.put("MH", new CountryIndex(index++, new String("692")));
		countries.put("MQ", new CountryIndex(index++, new String("596")));
		countries.put("MR", new CountryIndex(index++, new String("222")));
		countries.put("MU", new CountryIndex(index++, new String("230")));
		countries.put("YT", new CountryIndex(index++, new String("262")));
		countries.put("MX", new CountryIndex(index++, new String("52")));
		countries.put("MD", new CountryIndex(index++, new String("373")));
		countries.put("MC", new CountryIndex(index++, new String("377")));
		countries.put("MN", new CountryIndex(index++, new String("976")));
		countries.put("ME", new CountryIndex(index++, new String("382")));
		countries.put("MS", new CountryIndex(index++, new String("1")));
		countries.put("MA", new CountryIndex(index++, new String("212")));
		countries.put("MZ", new CountryIndex(index++, new String("258")));
		countries.put("MM", new CountryIndex(index++, new String("95")));
		countries.put("NA", new CountryIndex(index++, new String("264")));
		countries.put("NR", new CountryIndex(index++, new String("674")));
		countries.put("NP", new CountryIndex(index++, new String("977")));
		countries.put("NL", new CountryIndex(index++, new String("31")));
		countries.put("NC", new CountryIndex(index++, new String("687")));
		countries.put("NZ", new CountryIndex(index++, new String("64")));
		countries.put("NI", new CountryIndex(index++, new String("505")));
		countries.put("NE", new CountryIndex(index++, new String("227")));
		countries.put("NG", new CountryIndex(index++, new String("234")));
		countries.put("NU", new CountryIndex(index++, new String("683")));
		countries.put("NF", new CountryIndex(index++, new String("672")));
		countries.put("KP", new CountryIndex(index++, new String("850")));
		countries.put("MP", new CountryIndex(index++, new String("1")));
		countries.put("NO", new CountryIndex(index++, new String("47")));
		countries.put("OM", new CountryIndex(index++, new String("968")));
		countries.put("PK", new CountryIndex(index++, new String("92")));
		countries.put("PW", new CountryIndex(index++, new String("680")));
		countries.put("PS", new CountryIndex(index++, new String("970")));
		countries.put("PA", new CountryIndex(index++, new String("507")));
		countries.put("PG", new CountryIndex(index++, new String("675")));
		countries.put("PY", new CountryIndex(index++, new String("595")));
		countries.put("PE", new CountryIndex(index++, new String("51")));
		countries.put("PH", new CountryIndex(index++, new String("63")));
		countries.put("PL", new CountryIndex(index++, new String("48")));
		countries.put("PT", new CountryIndex(index++, new String("351")));
		countries.put("PR", new CountryIndex(index++, new String("1")));
		countries.put("QA", new CountryIndex(index++, new String("974")));
		countries.put("CG", new CountryIndex(index++, new String("242")));
		countries.put("RE", new CountryIndex(index++, new String("262")));
		countries.put("RO", new CountryIndex(index++, new String("40")));
		countries.put("RU", new CountryIndex(index++, new String("7")));
		countries.put("RW", new CountryIndex(index++, new String("250")));
		countries.put("BL", new CountryIndex(index++, new String("590")));
		countries.put("SH", new CountryIndex(index++, new String("290")));
		countries.put("KN", new CountryIndex(index++, new String("1")));
		countries.put("MF", new CountryIndex(index++, new String("590")));
		countries.put("PM", new CountryIndex(index++, new String("508")));
		countries.put("VC", new CountryIndex(index++, new String("1")));
		countries.put("WS", new CountryIndex(index++, new String("685")));
		countries.put("SM", new CountryIndex(index++, new String("378")));
		countries.put("ST", new CountryIndex(index++, new String("239")));
		countries.put("SA", new CountryIndex(index++, new String("966")));
		countries.put("SN", new CountryIndex(index++, new String("221")));
		countries.put("RS", new CountryIndex(index++, new String("381")));
		countries.put("SC", new CountryIndex(index++, new String("248")));
		countries.put("SL", new CountryIndex(index++, new String("232")));
		countries.put("SG", new CountryIndex(index++, new String("65")));
		countries.put("SX", new CountryIndex(index++, new String("599")));
		countries.put("SK", new CountryIndex(index++, new String("421")));
		countries.put("SI", new CountryIndex(index++, new String("386")));
		countries.put("SB", new CountryIndex(index++, new String("677")));
		countries.put("SO", new CountryIndex(index++, new String("252")));
		countries.put("ZA", new CountryIndex(index++, new String("27")));
		countries.put("KR", new CountryIndex(index++, new String("82")));
		countries.put("SS", new CountryIndex(index++, new String("211")));
		countries.put("ES", new CountryIndex(index++, new String("34")));
		countries.put("LK", new CountryIndex(index++, new String("94")));
		countries.put("LC", new CountryIndex(index++, new String("1")));
		countries.put("SD", new CountryIndex(index++, new String("249")));
		countries.put("SR", new CountryIndex(index++, new String("597")));
		countries.put("SZ", new CountryIndex(index++, new String("268")));
		countries.put("SE", new CountryIndex(index++, new String("46")));
		countries.put("CH", new CountryIndex(index++, new String("41")));
		countries.put("SY", new CountryIndex(index++, new String("963")));
		countries.put("TW", new CountryIndex(index++, new String("886")));
		countries.put("TJ", new CountryIndex(index++, new String("992")));
		countries.put("TZ", new CountryIndex(index++, new String("255")));
		countries.put("TH", new CountryIndex(index++, new String("66")));
		countries.put("BS", new CountryIndex(index++, new String("1")));
		countries.put("GM", new CountryIndex(index++, new String("220")));
		countries.put("TL", new CountryIndex(index++, new String("670")));
		countries.put("TG", new CountryIndex(index++, new String("228")));
		countries.put("TK", new CountryIndex(index++, new String("690")));
		countries.put("TO", new CountryIndex(index++, new String("676")));
		countries.put("TT", new CountryIndex(index++, new String("1")));
		countries.put("TN", new CountryIndex(index++, new String("216")));
		countries.put("TR", new CountryIndex(index++, new String("90")));
		countries.put("TM", new CountryIndex(index++, new String("993")));
		countries.put("TC", new CountryIndex(index++, new String("1")));
		countries.put("TV", new CountryIndex(index++, new String("688")));
		countries.put("UG", new CountryIndex(index++, new String("256")));
		countries.put("UA", new CountryIndex(index++, new String("380")));
		countries.put("AE", new CountryIndex(index++, new String("971")));
		countries.put("GB", new CountryIndex(index++, new String("44")));
		countries.put("US", new CountryIndex(index++, new String("1")));
		countries.put("UY", new CountryIndex(index++, new String("598")));
		countries.put("VI", new CountryIndex(index++, new String("1")));
		countries.put("UZ", new CountryIndex(index++, new String("998")));
		countries.put("VU", new CountryIndex(index++, new String("678")));
		countries.put("VA", new CountryIndex(index++, new String("39")));
		countries.put("VE", new CountryIndex(index++, new String("58")));
		countries.put("VN", new CountryIndex(index++, new String("84")));
		countries.put("WF", new CountryIndex(index++, new String("681")));
		countries.put("YE", new CountryIndex(index++, new String("967")));
		countries.put("ZM", new CountryIndex(index++, new String("260")));
		countries.put("ZW", new CountryIndex(index++, new String("263")));	
		System.out.println("countries: "+countries);
	}
	
	class CountryIndex {
		int index;
		String code;
		public CountryIndex(int index, String code) {
			super();
			this.index = index;
			this.code = code;
		}
		
	}
}
