package bsz.swbdigest;

public class SolrUtil {
	
	public static String prepareSolr(String term) {
		StringBuilder result = new StringBuilder();
		for (char c : term.toCharArray()) {
			switch (c) {
			case '+':
				result.append("\\+");
				break;
			case '-':
				result.append("\\-");
				break;
			case '&':
				result.append("\\&");
				break;
			case '|':
				result.append("\\|");
				break;
			case '!':
				result.append("\\!");
				break;
			case '(':
				result.append("\\(");
				break;
			case ')':
				result.append("\\)");
				break;
			case '{':
				result.append("\\{");
				break;
			case '}':
				result.append("\\}");
				break;
			case '[':
				result.append("\\[");
				break;
			case ']':
				result.append("\\]");
				break;
			case '^':
				result.append("\\^");
				break;
			case '"':
				result.append("\\\"");
				break;
			case '~':
				result.append("\\~");
				break;
			case '*':
				result.append("\\*");
				break;
			case '?':
				result.append("\\?");
				break;
			case ':':
				result.append("\\:");
				break;
			case '\\':
				result.append("\\\\");
				break;
			default:
				result.append(c);
			}
		}
		return result.toString();		
	}

}
