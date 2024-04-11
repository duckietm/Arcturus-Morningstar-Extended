package com.skeletor.plugin.javascript.utils;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtility {

  public static String getYouTubeId (String youTubeUrl) {
    String pattern = "(?<=youtu.be/|watch\\?v=|/videos/|embed\\/)[^#\\&\\?]*";
    Pattern compiledPattern = Pattern.compile(pattern);
    Matcher matcher = compiledPattern.matcher(youTubeUrl);
    if(matcher.find()){
      return matcher.group();
    } else {
      return "";
    }
  }

  /**
   * Sanitizes a string by removing any potentially harmful HTML elements.
   *
   * @param str The string to be sanitized.
   * @return The sanitized string.
   */
  public static String sanitize(String str) {
    PolicyFactory policy = new HtmlPolicyBuilder().toFactory();
    return policy.sanitize(str);
  }
}