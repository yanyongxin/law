package core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LexToken {
	private static final Logger log = LoggerFactory.getLogger(LexToken.class);
	static final Pattern ptStopWords = Pattern.compile("the|of|for|to|on|and|or|at|law|offices?|attorneys?", Pattern.CASE_INSENSITIVE);
	static final Pattern ptEntitySuffix = Pattern.compile("llp|llc|pc|pllc|ltd|plc", Pattern.CASE_INSENSITIVE);
	static final Pattern ptDate = Pattern.compile("\\d{1,2}\\/\\d{1,2}\\/(19|20)?\\d\\d", Pattern.CASE_INSENSITIVE);
	static final Pattern ptTime = Pattern.compile("(0|1)?\\d\\:\\d\\d\\s*(a|p)\\.?m\\.?", Pattern.CASE_INSENSITIVE);
	static final Pattern ptNumber = Pattern.compile("(\\d+(\\,\\d{3})*)(\\.\\d+)?", Pattern.CASE_INSENSITIVE);
	static final Pattern ptSerialNumber = Pattern.compile("\\d*(1st|2nd|3rd)|\\d+th", Pattern.CASE_INSENSITIVE);
	static final Pattern ptCaseNumber = Pattern.compile("\\d{0,2}\\:\\d\\d?-[a-z][a-z]-\\d{2,5}(-\\w{2,3})*", Pattern.CASE_INSENSITIVE);
	public static final int LEX_EMPTY = 0;
	public static final int LEX_WORD = 1;
	public static final int LEX_DIGIT = 2;
	public static final int LEX_PUNCT_OR_SYMBOL = 3;
	public static final int LEX_MINUS = 4;
	public static final int LEX_UNDERSCORE = 5;
	public static final int LEX_DATE = 6;
	public static final int LEX_NUMBER = 7;
	public static final int LEX_SERIALNUMBER = 8;
	public static final int LEX_TIME = 9;
	public static final int LEX_FED_CASENUMBER = 10;
	public static final int LEX_ENTITY = 11;
	public String parent; // parent string
	char beforeChar = 0; // the character before this token. 0 = not part of
							// sentence
	char afterChar = 0; // the character after this token
	public int start; // start position on parent
	public int end; // end position on parent
	String text; // parent.substring(start, end)
	int type;

	@Override
	public boolean equals(Object o) {
		if (o.getClass() != LexToken.class) {
			return false;
		}
		LexToken tk = (LexToken) o;
		if (text == null || tk.text == null) {
			return false;
		}
		return text.equalsIgnoreCase(tk.text);
	}

	public LexToken split(int splitPoint) {
		String s1 = parent.substring(start, splitPoint);
		String s2 = parent.substring(splitPoint, end);
		LexToken next = new LexToken(parent, s2, splitPoint, end, type);
		end = splitPoint;
		text = s1;
		return next;
	}

	public LexToken(String p, String t, int x1, int x2, int tp) {
		parent = p;
		text = t;
		start = x1;
		end = x2;
		type = tp;
		if (x1 <= 0) {
			beforeChar = 0;
		} else {
			beforeChar = p.charAt(x1 - 1);
		}
		if (x2 >= p.length()) {
			afterChar = 0;
		} else {
			afterChar = p.charAt(x2);
		}
	}

	public boolean isStopWord() {
		Matcher m = ptStopWords.matcher(text);
		return m.matches();
	}

	public boolean isEntitySuffix() {
		Matcher m = ptEntitySuffix.matcher(text);
		return m.matches();
	}

	public boolean isSymbol() {
		return type == LEX_PUNCT_OR_SYMBOL;
	}

	public int getStart() {
		return start;
	}

	public int getType() {
		return type;
	}

	public int getEnd() {
		return end;
	}

	@Override
	public String toString() {
		return text;
	}

	/**
	 * "American Airlines, Inc." ==> "AmericanAirlinesInc" "Amazon.com Inc."
	 * ==>"AmazoncomInc"
	 * 
	 * @param s
	 * @return
	 */
	public static String getSingleName(String s) {
		return s.replaceAll("\\W", "");
	}

	public void rebase(String _parent, int _offset) {
		parent = _parent;
		start += _offset;
		end += _offset;
	}

	public String getText() {
		return text;
	}

	/**
	 * tokenize() and tokenize2() differ in the handling of digit->letter
	 * boundary, and hyphen. Each has its merits. For now, official version is
	 * tokenize()
	 * 
	 * @param src
	 * @return
	 */
	public static List<LexToken> tokenize(String src) {
		if (src == null || src.trim().length() == 0) {
			return null;
		}
		List<LexToken> list = new ArrayList<LexToken>();

		char[] ch = src.toCharArray();
		int flag = LEX_EMPTY;
		StringBuilder sb = new StringBuilder();
		int xstart = 0;
		for (int i = 0; i < ch.length; i++) {
			if (Character.isLetter(ch[i])) {
				switch (flag) {
				case LEX_EMPTY:
					sb.append(ch[i]);
					xstart = i;
					flag = LEX_WORD;
					break;
				case LEX_WORD:
					sb.append(ch[i]);
					break;
				case LEX_DIGIT: // digit -> letter is a token boundary.
					// I changed my mind, now it is not a boundary, just change
					// to word flag.
					// the reason is I want to keep 1st, 2nd, 3rd, 5th, etc. as
					// a single word.
					// list.add(new LexToken(src, sb.toString(), xstart, i,
					// LEX_DIGIT));
					// sb = new StringBuilder();
					// xstart = i;
					sb.append(ch[i]);
					flag = LEX_WORD;
					break;
				}
			} else if (Character.isDigit(ch[i])) {
				switch (flag) {
				case LEX_EMPTY:
					// Check if it is date:
					Matcher m;
					m = ptCaseNumber.matcher(src);
					if (m.find(i)) {
						if (m.start() == i) {
							int end = m.end();
							if (end >= ch.length || !(Character.isDigit(ch[end]) || Character.isLetter(ch[end]))) {
								list.add(new LexToken(src, m.group(), i, end, LEX_FED_CASENUMBER));
								i = end - 1;
								continue;
							}
						}
					}
					m = ptDate.matcher(src);
					if (m.find(i)) {
						if (m.start() == i) {
							int end = m.end();
							if (end >= ch.length || !(Character.isDigit(ch[end]) || Character.isLetter(ch[end]))) {
								list.add(new LexToken(src, m.group(), i, end, LEX_DATE));
								i = end - 1;
								continue;
							}
						}
					}
					m = ptTime.matcher(src);
					if (m.find(i)) {
						if (m.start() == i) {
							int end = m.end();
							if (end >= ch.length || !(Character.isDigit(ch[end]) || Character.isLetter(ch[end]))) {
								list.add(new LexToken(src, m.group(), i, end, LEX_TIME));
								i = end - 1;
								continue;
							}
						}
					}
					m = ptSerialNumber.matcher(src);
					if (m.find(i)) {// this is guaranteed to succeed.
						if (m.start() == i) {
							int end = m.end();
							//							if (end >= ch.length || !(Character.isDigit(ch[end]) || Character.isLetter(ch[end]))) {
							list.add(new LexToken(src, m.group(), i, end, LEX_SERIALNUMBER));
							i = end - 1;
							continue;
							//							}
						}
					}
					m = ptNumber.matcher(src);
					if (m.find(i)) {// this is guaranteed to succeed.
						if (m.start() == i) {
							int end = m.end();
							//							if (end >= ch.length || !(Character.isDigit(ch[end]) || Character.isLetter(ch[end]))) {
							list.add(new LexToken(src, m.group(), i, end, LEX_NUMBER));
							i = end - 1;
							continue;
							//							}
						}
					}
					// this place is never reached:
					sb.append(ch[i]);
					xstart = i;
					flag = LEX_DIGIT;
					break;
				case LEX_WORD: // Letter -> digit is not a token boundary
					sb.append(ch[i]);
					break;
				case LEX_DIGIT:
					sb.append(ch[i]);
					break;
				}
			} else if (Character.isWhitespace(ch[i])) {
				switch (flag) {
				case LEX_EMPTY:
					break;
				default:
					list.add(new LexToken(src, sb.toString(), xstart, i, flag));
					sb = new StringBuilder();
					flag = LEX_EMPTY;
					break;
				}
			} else { // symbol or punctuation: each is a token by itself.
				// Check if it is O'Connor, O'Neill, etc.
				// if (ch[i] == '\'' && flag == LEX_WORD) {
				// if (i > 0 && ch[i - 1] == 'o' && i < ch.length - 3 || i <
				// ch.length - 1 && (ch[i + 1] == 's' || (i > 1 && ch[i - 1] ==
				// 's'))) {
				// sb.append(ch[i]);
				// continue;
				// }
				// }
				if (ch[i] == '-' && flag == LEX_WORD) {
					// if both side has two or more letters, do not break at hyphen
					if ((i < ch.length - 2 && Character.isLetter(ch[i + 1]) && Character.isLetter(ch[i + 2]))
							&& (i >= 2 && Character.isLetter(ch[i - 1]) && Character.isLetter(ch[i - 2]))) {
						sb.append(ch[i]);
						continue;
					}
					// else break at hyphen 
				}
				if (flag != LEX_EMPTY) {
					list.add(new LexToken(src, sb.toString(), xstart, i, flag));
					sb = new StringBuilder();
					xstart = i;
				}
				list.add(new LexToken(src, src.substring(i, i + 1), i, i + 1, LEX_PUNCT_OR_SYMBOL));
				flag = LEX_EMPTY;
			}
		}
		// last word of the sentence:
		if (flag != LEX_EMPTY) {
			list.add(new LexToken(src, sb.toString(), xstart, ch.length, flag));
		}
		return list;
	}

	public static List<LexToken> tokenize2(String src) {
		if (src == null || src.trim().length() == 0) {
			return null;
		}
		List<LexToken> list = new ArrayList<LexToken>();

		char[] ch = src.toCharArray();
		int flag = LEX_EMPTY;
		StringBuilder sb = new StringBuilder();
		int xstart = 0;
		for (int i = 0; i < ch.length; i++) {
			if (Character.isLetter(ch[i])) {
				switch (flag) {
				case LEX_EMPTY:
					sb.append(ch[i]);
					xstart = i;
					flag = LEX_WORD;
					break;
				case LEX_WORD:
					sb.append(ch[i]);
					break;
				case LEX_DIGIT: // digit -> letter is a token boundary.
					list.add(new LexToken(src, sb.toString(), xstart, i, LEX_DIGIT));
					sb = new StringBuilder();
					xstart = i;
					sb.append(ch[i]);
					flag = LEX_WORD;
					break;
				}
			} else if (Character.isDigit(ch[i])) {
				switch (flag) {
				case LEX_EMPTY:
					sb.append(ch[i]);
					xstart = i;
					flag = LEX_DIGIT;
					break;
				case LEX_WORD: // Letter -> digit is a token boundary
					list.add(new LexToken(src, sb.toString(), xstart, i, LEX_WORD));
					sb = new StringBuilder();
					xstart = i;
					sb.append(ch[i]);
					flag = LEX_DIGIT;
					break;
				case LEX_DIGIT:
					sb.append(ch[i]);
					break;
				}
			} else if (Character.isWhitespace(ch[i])) {
				switch (flag) {
				case LEX_EMPTY:
					break;
				default:
					list.add(new LexToken(src, sb.toString(), xstart, i, flag));
					sb = new StringBuilder();
					flag = LEX_EMPTY;
					break;
				}
			} else { // symbol or punctuation: each is a token by itself.
				// Check if it is O'Connor, O'Neill, etc.
				if (ch[i] == '\'' && flag == LEX_WORD) {
					if (ch[i - 1] == 'o' && i < ch.length - 3) {
						sb.append(ch[i]);
						continue;
					}
				}
				// if (ch[i] == '-' && flag == LEX_WORD) {
				// if (i < ch.length - 2) {
				// sb.append(ch[i]);
				// continue;
				// }
				// }
				if (flag != LEX_EMPTY) {
					list.add(new LexToken(src, sb.toString(), xstart, i, flag));
					sb = new StringBuilder();
				}
				list.add(new LexToken(src, src.substring(i, i + 1), i, i + 1, LEX_PUNCT_OR_SYMBOL));
				flag = LEX_EMPTY;
			}
		}
		if (flag != LEX_EMPTY) {
			list.add(new LexToken(src, sb.toString(), xstart, ch.length, flag));
		}
		return list;
	}

	public static boolean matchTokenList(List<LexToken> tk1, List<LexToken> tk2) {
		if (tk1 == null || tk2 == null) {
			return false;
		}
		if (tk1.size() != tk2.size()) {
			return false;
		}
		for (int i = 0; i < tk1.size(); i++) {
			LexToken t1 = tk1.get(i);
			LexToken t2 = tk2.get(i);
			if (!t1.equals(t2)) {
				return false;
			}
		}
		return true;
	}
}
