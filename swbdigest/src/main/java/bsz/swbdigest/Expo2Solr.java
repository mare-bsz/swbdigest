package bsz.swbdigest;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Expo2Solr {
	
	private final Pattern andPattern = Pattern.compile("^(.+)\\-and\\-(.+)$");
	private final Pattern orPattern = Pattern.compile("^(.+)\\-or\\-(.+)$");
	private final Pattern notPattern = Pattern.compile("^(.+)\\-not\\-(.+)$");	
	private final Pattern allPattern = Pattern.compile("^(\\w+)\\.all\\.(.+)$");
	private final Pattern anyPattern = Pattern.compile("^(\\w+)\\.any\\.(.+)$");
	private final Pattern adjPattern = Pattern.compile("^(\\w+)\\.adj\\.(.+)$");
	private final Pattern quotedPattern = Pattern.compile("^\"(.+)\"$");
	
	private final Map<String, String> indexe;
	
	public Expo2Solr(Map<String, String> indexe) {
		this.indexe = indexe;
	}
	
	public String parse(final String query) {
		Matcher m = quotedPattern.matcher(query);
		if (m.matches()) {
			return indexe.get("default") + ":" + quote(SolrUtil.prepareSolr(m.group(1)));
		} else {
			return parseAndOperation(query);
		}
	}
	
	private String parseAndOperation(final String query) {
		final String result = parseOperation(query, andPattern, "and");
		return (result != null) ? result : parseOrOperation(query);
	}
	
	private String parseOrOperation(final String query) {
		final String result = parseOperation(query, orPattern, "or");
		return (result != null) ? result : parseNotOperation(query);
	}
	
	private String parseNotOperation(final String query) {
		final String result = parseOperation(query, notPattern, "not");
		return (result != null) ? result : parseAllRelation(query);
	}
		
	private String parseAllRelation(final String query) {
		final String result = parseRelation(query, allPattern, "and");
		return (result != null) ? result : parseAnyRelation(query);		
	}
	
	private String parseAnyRelation(final String query) {
		final String result = parseRelation(query, anyPattern, "or");
		return (result != null) ? result : parseAdjRelation(query);		
	}
		
	private String parseAdjRelation(final String query) {
		final Matcher m = adjPattern.matcher(query);
		if (m.matches()) {
			final String index = parseIndex(m.group(1));
			final String phrase = parsePhrase(m.group(2));
			if (index != null && phrase != null) {
				return index + ":" + phrase;
			} 
		} 
		return parsePhrase(query);
	}
	
	private String parseOperation(final String query, final Pattern pattern, final String operator) {
		final Matcher m = pattern.matcher(query);
		if (m.matches()) {
			final String left = parse(m.group(1));
			final String right = parse(m.group(2));
			if (left != null && right != null) {
				return "(" + left + ") " + operator + " (" + right + ")";
			} 
		} 
		return null;
	}
	
	private String parseRelation(final String query, final Pattern pattern, final String operator) {	
		final Matcher m = pattern.matcher(query);
		if (m.matches()) {
			final String index = parseIndex(m.group(1));
			final String[] tokens = parseTokens(m.group(2));
			if (index != null && tokens != null) {
				return fold(index, tokens, operator);
			} else {
				return parsePhrase(query);
			}
		} 
		return null;
	}

	private String parseIndex(final String query) {
		if (indexe.get(query) != null) {
			return indexe.get(query);
		} else {
			return null;
		}
	}
	
	private String parsePhrase(final String query) {
		Matcher m = quotedPattern.matcher(query); 
		if (m.matches()) {
			return quote(SolrUtil.prepareSolr(m.group(1)));
		} 
		return parseString(query);
	}
	
	private String parseString(final String query) {
		if (! query.contains(" ")) {
			return quote(SolrUtil.prepareSolr(query));
		} else {
			return null;
		}
	}
	
	private String[] parseTokens(final String query) {
		Matcher m = quotedPattern.matcher(query);
		if (m.matches()) {
			return m.group(1).split(" ");
		} else {
			return query.split(" ");
		}
	}
	
	private String quote(final String query) {
		return "\"" + query + "\"";
	}
	
	private String fold(final String index, final String[] tokens, final String op) {
		final StringBuilder result = new StringBuilder(); 
		for (int i = 0; i < tokens.length; i++) {
			if (i != 0) {
				result.append(" " + op + " ");
			}
			result.append(index + ":" + quote(SolrUtil.prepareSolr(tokens[i])));
		}
		return result.toString();
	}	

}
