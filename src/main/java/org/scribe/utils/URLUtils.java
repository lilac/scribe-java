package org.scribe.utils;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * Utils to deal with URL and url-encodings
 *
 * @author Pablo Fernandez
 */
public class URLUtils
{
  private static final String EMPTY_STRING = "";
  private static final String UTF_8 = "UTF-8";
  private static final String PAIR_SEPARATOR = "=";
  private static final String PARAM_SEPARATOR = "&";
  private static final char QUERY_STRING_SEPARATOR = '?';

  private static final String ERROR_MSG = String.format("Cannot find specified encoding: %s", UTF_8);

  private static final Set<EncodingRule> ENCODING_RULES;

  static
  {
    Set<EncodingRule> rules = new HashSet<EncodingRule>();
    rules.add(new EncodingRule("*","%2A"));
    rules.add(new EncodingRule("+","%20"));
    rules.add(new EncodingRule("%7E", "~"));
    ENCODING_RULES = Collections.unmodifiableSet(rules);
  }

  /**
   * Turns a map into a form-urlencoded string
   * 
   * @param map any map
   * @return form-url-encoded string
   */
  public static String formURLEncodeMap(Map<String, String> map)
  {
    Preconditions.checkNotNull(map, "Cannot url-encode a null object");
    return (map.size() <= 0) ? EMPTY_STRING : doFormUrlEncode(map);
  }

  private static String doFormUrlEncode(Map<String, String> map)
  {
    StringBuffer encodedString = new StringBuffer(map.size() * 20);
    for (String key : map.keySet())
    {
      encodedString.append(PARAM_SEPARATOR).append(formURLEncode(key));
      if(map.get(key) != null)
      {
        encodedString.append(PAIR_SEPARATOR).append(formURLEncode(map.get(key)));
      }
    }
    return encodedString.toString().substring(1);
  }

  public static ByteArrayOutputStream doFormDataEncode(Map<String, String> map, Map<String, String> files, String boundary) throws IOException {
	  String charset = UTF_8;
	  String CRLF = "\r\n"; // Line separator required by multipart/form-data.
	  
	  PrintWriter writer = null;
	  ByteArrayOutputStream output = new ByteArrayOutputStream();
	  try {
		  writer = new PrintWriter(new OutputStreamWriter(output, charset), true); // true = autoFlush, important!

		  for (Entry<String, String> v : map.entrySet()) {
			  // Send normal param.
			  writer.append("--" + boundary).append(CRLF);
			  writer.append("Content-Disposition: form-data; name=\"" + v.getKey() + "\"").append(CRLF);
			  writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
			  writer.append(CRLF);
			  writer.append(v.getValue()).append(CRLF).flush();
		  }
		  // Send text file.
		  /*
		  writer.append("--" + boundary).append(CRLF);
		  writer.append("Content-Disposition: form-data; name=\"textFile\"; filename=\"" + textFile.getName() + "\"").append(CRLF);
		  writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
		  writer.append(CRLF).flush();
		  BufferedReader reader = null;
		  try {
			  reader = new BufferedReader(new InputStreamReader(new FileInputStream(textFile), charset));
			  for (String line; (line = reader.readLine()) != null;) {
				  writer.append(line).append(CRLF);
			  }
		  } finally {
			  if (reader != null) try { reader.close(); } catch (IOException logOrIgnore) {}
		  }
		  writer.flush();
		  */
		  for (Entry<String, String> file : files.entrySet()) {
			  // Send binary file.
			  String fn = file.getValue();
			  writer.append("--" + boundary).append(CRLF);
			  writer.append("Content-Disposition: form-data; name=\"" + file.getKey() + "\"; filename=\"" + fn + "\"").append(CRLF);
			  writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(fn)).append(CRLF);
			  writer.append("Content-Transfer-Encoding: binary").append(CRLF);
			  writer.append(CRLF).flush();
			  InputStream input = null;
			  try {
				  input = new FileInputStream(fn);
				  byte[] buffer = new byte[1024];
				  for (int length = 0; (length = input.read(buffer)) > 0;) {
					  output.write(buffer, 0, length);
				  }
				  output.flush(); // Important! Output cannot be closed. Close of writer will close output as well.
			  } finally {
				  if (input != null) {
					  input.close();
				  }
			  }
			  writer.append(CRLF).flush(); // CRLF is important! It indicates end of binary boundary.
		  }
		  // End of multipart/form-data.
		  writer.append("--" + boundary + "--").append(CRLF);
	  } finally {
		  if (writer != null) writer.close();
	  }
	  return output;
  }
  /**
   * Percent encodes a string
   * 
   * @param string plain string
   * @return percent encoded string
   */
  public static String percentEncode(String string)
  {
    String encoded = formURLEncode(string);
    for (EncodingRule rule : ENCODING_RULES)
    {
      encoded = rule.apply(encoded);
    }
    return encoded;
  }

  /**
   * Translates a string into application/x-www-form-urlencoded format
   *
   * @param plain
   * @return form-urlencoded string
   */
  public static String formURLEncode(String string)
  {
    Preconditions.checkNotNull(string, "Cannot encode null string");
    try
    {
      return URLEncoder.encode(string, UTF_8);
    } 
    catch (UnsupportedEncodingException uee)
    {
      throw new IllegalStateException(ERROR_MSG, uee);
    }
  }

  /**
   * Decodes a application/x-www-form-urlencoded string
   * 
   * @param string form-urlencoded string
   * @return plain string
   */
  public static String formURLDecode(String string)
  {
    Preconditions.checkNotNull(string, "Cannot decode null string");
    try
    {
      return URLDecoder.decode(string, UTF_8);
    }
    catch (UnsupportedEncodingException uee)
    {
      throw new IllegalStateException(ERROR_MSG, uee);
    }
  }

  /**
   * Append given parameters to the query string of the url
   *
   * @param url the url to append parameters to
   * @param params any map
   * @return new url with parameters on query string
   */
  public static String appendParametersToQueryString(String url, Map<String, String> params)
  {
    Preconditions.checkNotNull(url, "Cannot append to null URL");
    String queryString = URLUtils.formURLEncodeMap(params);
    if (queryString.equals(EMPTY_STRING))
    {
      return url;
    }
    else
    {
      url += url.indexOf(QUERY_STRING_SEPARATOR) != -1 ? PARAM_SEPARATOR : QUERY_STRING_SEPARATOR;
      url += queryString;
      return url;
    }
  }

  private static final class EncodingRule
  {
    private final String ch;
    private final String toCh;

    EncodingRule(String ch, String toCh)
    {
      this.ch = ch;
      this.toCh = toCh;
    }

    String apply(String string) {
      return string.replace(ch, toCh);
    }
  }
}
